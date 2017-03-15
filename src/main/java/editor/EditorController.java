package editor;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;

import javafx.animation.Timeline;
import javafx.scene.control.IndexRange;
import javafx.util.Duration;
import javafx.animation.KeyFrame;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.event.*;
import main.MainController;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class EditorController {

    @FXML VBox editorBox; // Container holding the editor TextArea
    @FXML volatile TextArea editor; // To display and edit contents of a text file

    private static final int IDLE_CHECK_TIME = 3; // Timeout to auto push op
    
    private static int clientCounter = 0; // Keeps track of how many clients

    private MainController mainController; // To communicate with other controllers
    private Channel channel; // Connection to server
    private Channel fileChannel; // Connection to file server

    // Below are state info required for OT
    private Stack<Operation> opLog; // Useful for REDO functionality
    Operation op; // current operation we are building
    int opsGenerated; // How many ops this client generated
    int opsReceived; // How many ops this client received
    int clientId; // Id of client
    final ConcurrentLinkedQueue<Operation> outgoing; // queue of outgoing ops

    private String filePath; // file we are modifying
    private String selected; // text selection
    private IndexRange selectedRange; // range on text selection
    private boolean commandPressed; // toggled when command key is pressed/released

    AtomicBoolean editing = new AtomicBoolean(false);

    private Clipboard clipboard;

    public EditorController() {
        this.opsGenerated = 0;
        this.opsReceived = 0;
        outgoing = new ConcurrentLinkedQueue<>();
        opLog = new Stack<>();
        op = null;
        commandPressed = false;
        this.clientId = clientCounter++;
        Toolkit tk = Toolkit.getDefaultToolkit();
        clipboard = tk.getSystemClipboard();
        selected = null;
        selectedRange = null;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void setFileChannel(Channel channel) {
        this.fileChannel = channel;
    }


    @SuppressWarnings("restriction")
    @FXML
    /**
     * Each key stroke is a single operation
     */
    public void initialize() {
        editor.setWrapText(true); // Will be enabled upon connect
        editor.setDisable(true); // Will be enabled upon connect


        editor.setOnKeyTyped(event -> {
            // KeyTyped refers to keys pressed that can be displayed in the TextArea
            editing.set(true);
            String c = event.getCharacter(); // what we typed
            if (!c.isEmpty() && !commandPressed) {
                op = new Operation(Operation.INSERT); // insert operation
                op.leftIdx = editor.getCaretPosition(); // leftIdx = cursor pos
                op.content = c; // content is what we typed
                send(op);
                op = null;
            }
            if (commandPressed) {
                if (c.equals("v")) {
                    // Paste
                    try {
                        op = new Operation(Operation.INSERT);
                        // Get from System's clipboard
                        String copied = (String) clipboard.getData(DataFlavor.stringFlavor);
                        // since we do not consume this, we adjust the insert position
                        // since caret position is AFTER the text has been pasted
                        op.leftIdx = editor.getCaretPosition() - copied.length();
                        op.content = copied;
                        send(op);
                        op = null;
                    }
                    catch (Exception e) {
                        System.out.println(e);
                    }
                }
                if (c.equals("s")) {
                    save();
                }
                if (c.equals("q")) {
                    mainController.close();
                }
            }
        });

        editor.setOnKeyPressed(event -> {
            // KeyPressed are keys that would not be displayed on the TextArea
            // For special keys, e.g. backspace, command, shift, etc.
            // Can use this to introduce shortcuts (e.g. COMMAND+S to save)
            selected = editor.getSelectedText();
            selectedRange = editor.getSelection();
            if (event.getCode() == KeyCode.BACK_SPACE) {
                if (!selected.isEmpty()) {
                    op = new Operation(Operation.DELETE);
                    op.leftIdx = selectedRange.getStart();
                    op.rightIdx = selectedRange.getEnd();
                    send(op);
                    op = null;
                }
                else if (editor.getCaretPosition() > 0) {
                    // Backspace pressed, and not at beginning of text
                    op = new Operation(Operation.DELETE);
                    op.rightIdx = editor.getCaretPosition();
                    op.leftIdx = op.rightIdx - 1;
                    send(op);
                    op = null;
                }
            }
            if (event.getCode() == KeyCode.COMMAND) {
                commandPressed = true;
            }
            else editing.set(true);
        });

        editor.setOnKeyReleased(event -> {
            editing.set(false);
            if (event.getCode() == KeyCode.COMMAND) {
                commandPressed = false;
            }
        });

        editor.setOnMouseClicked(event -> {
            selected = null;
            selectedRange = null;
        });

    }

    @SuppressWarnings("restriction")
	@FXML
    /**
     * If we want to batch multiple key strokes into a single operation
     */
    public void initializeBatchedVersion() {
    	
    	// Timer to check if user is idle, runs every IDLE_CHECK_TIME seconds
		Timeline idleCheck = new Timeline(new KeyFrame(Duration.seconds(IDLE_CHECK_TIME), new EventHandler<ActionEvent>() {
			Operation previousOp = op; // what previous operation was IDLE_CHECK_TIME seconds ago
    	    @Override
    	    public void handle(ActionEvent event) {
    	    	if (op != null)
    	    	{
    	    		// check if previous operation equal to current operation
    	    		if (previousOp != null && previousOp.equals(op))
    	    		{
    	    			// user is idle
                        op.rightIdx = editor.getCaretPosition();
                        System.out.println("User is idle, adding: " + op);
                        // Push to logs
                        opLog.push(op);
                        // Send to server
                        send(op);
                        // Make current op null again
                        op = null;    	    		
                    }
    	    	}
    	    	previousOp = op; // update previous operation to current operation
    	    }
    	}));
    	idleCheck.setCycleCount(Timeline.INDEFINITE); // keep looping while application is open
    	idleCheck.play();
    	
        editor.setWrapText(true); // Will be enabled upon connect
        editor.setDisable(true); // Will be enabled upon connect

        // Prints out caret position when clicking
        editor.setOnMouseClicked(event -> {
            System.out.println(editor.getCaretPosition());
        });

        editor.setOnKeyTyped(event -> {
            // KeyTyped refers to keys pressed that can be displayed in the TextArea
            String c = event.getCharacter(); // what we typed
            if (!c.isEmpty()) {
                if (op == null) {
                    // Create a new operation since none existed before
                    op = new Operation(Operation.INSERT);
                    // Set start position to where caret (cursor) currently is
                    op.leftIdx = editor.getCaretPosition();
                }
                // We had a series of deletes, but now we have an insert
                if (op.type == Operation.DELETE) {
                    send(op); // Send to server
                    op = new Operation(Operation.INSERT); // Create new Operation for these inserts
                    op.leftIdx = editor.getCaretPosition(); // Set start position
                }
                op.content += c; // Append characters to track changes
                if (c.equals("\r") || c.equals(" ") ||
                        c.equals(".")) {
                    // Enter, space, or period triggers a push.
                    // Final Position may not be needed for inserts, since we
                    // can simply get op.content.length() for inserts.
                    send(op); // Send to server
                    op = null; // Make current op null again
                }
            }
        });

        // Pushes an existing op when clicking
        editor.setOnMouseClicked(event -> {
            if (op != null) {
                send(op);
                op = null;
            }
            System.out.println(editor.getCaretPosition());
        });

        editor.setOnKeyPressed(event -> {
            // KeyPressed are keys that would not be displayed on the TextArea
            // For special keys, e.g. backspace, command, shift, etc.
            // Can use this to introduce shortcuts (e.g. COMMAND+S to save)
            if (event.getCode() == KeyCode.BACK_SPACE && editor.getCaretPosition() > 0) {
                // Backspace pressed, and not at beginning of text
                if (op == null) {
                    // Create a new operation for deletions
                    op = new Operation(Operation.DELETE);
                    op.leftIdx = editor.getCaretPosition();
                    op.rightIdx = op.leftIdx;
                }
                if (op.type == Operation.INSERT) {
                    // If we switched from inserting to deleting
                    send(op);
                    op = new Operation(Operation.DELETE);
                    op.leftIdx = editor.getCaretPosition();
                    op.rightIdx = op.leftIdx;
                }
                if (op.type == Operation.DELETE) {
                    // Decrement our final position for a series of deletions
                    op.rightIdx--;
                }
            }
        });
    }

    public void generate(Operation op) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                applyBatched(op);
                // Set clientId into op
                op.clientId = clientId;
                // Pack this client's state info into op
                op.opsGenerated = opsGenerated;
                op.opsReceived = opsReceived;
                // Send the op
                ChannelFuture f = channel.writeAndFlush(op);
                // Append to queue
                outgoing.add(op);
                // Add to opLog
                opLog.push(op);
                // Increment this client's state info
                opsGenerated++;
                f.sync();
                return null;
            }
        };
        Platform.runLater(task);
    }

    @FXML
    /**
     * Sends an Operation object to the EditorServer
     */
    public void send(Operation op) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
            	// Set clientId into op
            	op.clientId = clientId;
                // Pack this client's state info into op
                op.opsGenerated = opsGenerated;
                op.opsReceived = opsReceived;
                // Send the op
                ChannelFuture f = channel.write(op);
                // Append to queue
                outgoing.add(op);
                // Add to opLog
                opLog.push(op);
                // Increment this client's state info
                opsGenerated++;
                f.sync();
                return null;
            }
            @Override
            protected void succeeded() {
                System.out.println("SENT OPERATION TO SERVER: " + op);
            }
        };
        new Thread(task).start();
//        Platform.runLater(task);
    }

    public void populateEditor(String string) {
        if (!string.isEmpty()) {
            editor.appendText(string);
        }
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Updates the editor text area based on operations with single character
     * changes
     */
    public void apply(Operation op) {
        if (op.type == Operation.INSERT) {
            doInsert(op);
        }
        if (op.type == Operation.DELETE) {
            doDelete(op);
        }
    }

    private void doInsert(Operation op) {
        int caret = editor.getCaretPosition();
        int start = op.leftIdx;
        String content = op.content;
        if (content.equals("\r")) {
            content = "\n";
            start -= 1;
        }
        editor.insertText(start, content);
        editor.positionCaret(caret);
    }

    private void doDelete(Operation op) {
        int caret = editor.getCaretPosition();
        int start = op.leftIdx;
        editor.deleteText(start-1, start);
        if (start <= caret) {
            // Deleted prior to our caret position
            editor.positionCaret(caret - 1);
        }
    }

    private void doReplace(Operation op) {
        int caret = editor.getCaretPosition();
        int left = op.leftIdx;
        int right = op.rightIdx;
        if (caret >= left && caret <= right) {
            caret = left;
        }
        else if (right < caret) {
            caret += op.content.length() - (right - left);
        }
        editor.deleteText(left, right);
        editor.insertText(left, op.content);
        editor.positionCaret(caret);
    }

    /**
     * Updates the editor text area based on the Operation specified.
     */
    public void applyBatched(Operation op) {
        if (op.type == Operation.INSERT) {
            doBatchedInsert(op);
        }
        if (op.type == Operation.DELETE) {
            doBatchedDelete(op);
        }
        if (op.type == Operation.REPLACE) {
            doReplace(op);
        }
    }

    private void doBatchedInsert(Operation op) {
        int caret = editor.getCaretPosition();
        int start = op.leftIdx;
        String content = op.content;
        if (content.equals("\r")) {
            content = "\n";
            start -= 1;
        }
        if (start < caret) {
            caret += content.length();
        }
        editor.insertText(start, content);
        editor.positionCaret(caret);
    }

    private void doBatchedDelete(Operation op) {
        int caret = editor.getCaretPosition();
        int left = op.leftIdx;
        int right = op.rightIdx;
        int deleted = right - left;
        if (caret >= left && caret <= right) {
            caret = left;
        }
        else if (caret >= left) {
            caret -= deleted;
        }
        editor.deleteText(left, right);
        editor.positionCaret(caret);
    }


    public void save() {
        String fileContent = editor.getText();
        // savereq__path/to/file.txt__length
        fileChannel.writeAndFlush("savereq__" + filePath + "__" + fileContent.length() + "\n");
        fileChannel.writeAndFlush(fileContent + "\n");
    }

    public VBox getEditorBox() {
        return editorBox;
    }

    public TextArea getEditor() {
        return editor;
    }
}

