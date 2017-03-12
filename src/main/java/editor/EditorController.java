package editor;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import javafx.concurrent.Task;
import javafx.fxml.FXML;

import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.animation.KeyFrame;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.event.*;
import main.MainController;

import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EditorController {

    @FXML VBox editorBox; // Container holding the editor TextArea
    @FXML TextArea editor; // To display and edit contents of a text file

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
    ConcurrentLinkedQueue<Operation> outgoing; // queue of outgoing ops

    private String filePath; // file we are modifying
    private String copied; // copy paste mechanism
    private boolean commandPressed; // toggled when command key is pressed/released

    public EditorController() {
        this.opsGenerated = 0;
        this.opsReceived = 0;
        outgoing = new ConcurrentLinkedQueue<>();
        opLog = new Stack<>();
        op = null;
        commandPressed = false;
        this.clientId = clientCounter++;
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
            String c = event.getCharacter(); // what we typed
            if (!c.isEmpty() && !commandPressed) {
                op = new Operation(Operation.INSERT); // insert operation
                op.startPos = editor.getCaretPosition(); // start = cursor pos
                op.content = c; // content is what we typed
                send(op);
                op = null;
            }
            if (commandPressed) {
                if (c.equals("c")) {
                    // Copy
                    copied = editor.getSelectedText();
                }
                if (c.equals("v") && copied != null && copied.length() > 0) {
                    // Paste
                    op = new Operation(Operation.INSERT);
                    op.content = copied;
                    op.startPos = editor.getCaretPosition() - copied.length();
                    send(op);
                    op = null;
                }
            }
        });

        editor.setOnKeyPressed(event -> {
            // KeyPressed are keys that would not be displayed on the TextArea
            // For special keys, e.g. backspace, command, shift, etc.
            // Can use this to introduce shortcuts (e.g. COMMAND+S to save)
            if (event.getCode() == KeyCode.BACK_SPACE && editor.getCaretPosition() > 0) {
                // Backspace pressed, and not at beginning of text
                op = new Operation(Operation.DELETE);
                op.startPos = editor.getCaretPosition();
                send(op);
                op = null;
            }
            if (event.getCode() == KeyCode.COMMAND) {
                commandPressed = true;
            }
        });

        editor.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.COMMAND) {
                commandPressed = false;
            }
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
                        op.finalPos = editor.getCaretPosition();
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
                    op.startPos = editor.getCaretPosition();
                }
                // We had a series of deletes, but now we have an insert
                if (op.type == Operation.DELETE) {
                    send(op); // Send to server
                    op = new Operation(Operation.INSERT); // Create new Operation for these inserts
                    op.startPos = editor.getCaretPosition(); // Set start position
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
                    op.startPos = editor.getCaretPosition();
                    op.finalPos = op.startPos;
                }
                if (op.type == Operation.INSERT) {
                    // If we switched from inserting to deleting
                    send(op);
                    op = new Operation(Operation.DELETE);
                    op.startPos = editor.getCaretPosition();
                    op.finalPos = op.startPos;
                }
                if (op.type == Operation.DELETE) {
                    // Decrement our final position for a series of deletions
                    op.finalPos --;
                }
            }
        });
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
            @Override
            protected void succeeded() {
                System.out.println("SENT OPERATION TO SERVER: " + op);
            }
        };
        new Thread(task).start();
    }

    public void populateEditor(String string) {
        editor.appendText(string + "\n");
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
        int start = op.startPos;
        String content = op.content;
        if (content.equals("\r")) {
            content = "\r\n";
            start -= 1;
        }
        editor.insertText(start, content);
        editor.positionCaret(caret + 1);
    }

    private void doDelete(Operation op) {
        int caret = editor.getCaretPosition();
        int start = op.startPos;
        editor.deleteText(start-1, start);
        if (start <= caret) {
            // Deleted prior to our caret position
            editor.positionCaret(caret - 1);
        }
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
    }

    private void doBatchedInsert(Operation op) {
        int caret = editor.getCaretPosition();
        int start = op.startPos;
        String content = op.content;
        editor.insertText(start, content);
        editor.positionCaret(caret + content.length());
    }

    private void doBatchedDelete(Operation op) {
        int caret = editor.getCaretPosition();
        int start = op.startPos;
        int end = op.finalPos;
        editor.deleteText(end, start);
        editor.positionCaret(caret - start + end);
    }

    public void save() {
        String fileContent = editor.getText();
        fileChannel.writeAndFlush("savereq__" + filePath + "__" + fileContent.length() + "\n");
        // savereq__path/to/file.txt__length
        // contents
        fileChannel.writeAndFlush(fileContent + "\n");
    }

    public VBox getEditorBox() {
        return editorBox;
    }

    public TextArea getEditor() {
        return editor;
    }
}

