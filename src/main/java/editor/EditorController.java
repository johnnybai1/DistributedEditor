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

    private MainController mainController; // To communicate with other controllers
    private Channel channel; // Connection to server
    private Channel fileChannel; // Connection to file server

    // Below are state info required for OT
    private Stack<Operation> opLog; // Useful for REDO functionality
    private volatile int opsGenerated; // How many ops this client generated
    private volatile int opsReceived; // How many ops this client received
    private int clientId; // Id of client
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
        commandPressed = false;
        Toolkit tk = Toolkit.getDefaultToolkit();
        clipboard = tk.getSystemClipboard();
        selected = null;
        selectedRange = null;
    }

    public int getOpsGenerated() {
        return opsGenerated;
    }

    public void setOpsGenerated(int opsGenerated) {
        this.opsGenerated = opsGenerated;
    }

    public int getOpsReceived() {
        return opsReceived;
    }

    public void setOpsReceived(int opsReceived) {
        this.opsReceived = opsReceived;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
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
                Operation op = new Operation(Operation.INSERT); // insert operation
                op.setLeftIdx(editor.getCaretPosition()); // leftIdx = cursor pos
                op.setContent(c);
                send(op);
            }
            if (commandPressed) {
                if (c.equals("v")) {
                    // Paste
                    try {
                        Operation op = new Operation(Operation.INSERT);
                        // Get from System's clipboard
                        String copied = (String) clipboard.getData(DataFlavor.stringFlavor);
                        // since we do not consume this, we adjust the insert position
                        // since caret position is AFTER the text has been pasted
                        op.setLeftIdx(editor.getCaretPosition() - copied.length());
                        op.setContent(copied);
                        send(op);
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
                    Operation op = new Operation(Operation.DELETE);
                    op.setLeftIdx(selectedRange.getStart());
                    op.setRightIdx(selectedRange.getEnd());
                    send(op);
                }
                else if (editor.getCaretPosition() > 0) {
                    // Backspace pressed, and not at beginning of text
                    Operation op = new Operation(Operation.DELETE);
                    op.setRightIdx(editor.getCaretPosition());
                    op.setLeftIdx(op.getRightIdx()-1);
                    send(op);
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

    @FXML
    /**
     * Sends an Operation object to the EditorServer
     */
    public synchronized void send(Operation op) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
            	// Set clientId into op
                op.setClientId(clientId);
                // Pack this client's state info into op
                op.setOpsGenerated(opsGenerated);
                op.setOpsReceived(opsReceived);
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
        };
        new Thread(task).start();
    }

    /**
     * Appends strings onto the editor text area.
     * @param string
     */
    void populateEditor(String string) {
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
        if (op.getType() == Operation.INSERT) {
            doInsert(op);
        }
        if (op.getType() == Operation.DELETE) {
            doDelete(op);
        }
    }

    private void doInsert(Operation op) {
        int caret = editor.getCaretPosition();
        int start = op.getLeftIdx();
        String content = op.getContent();
        if (content.equals("\r")) {
            content = "\n";
            start -= 1;
        }
        editor.insertText(start, content);
        editor.positionCaret(caret);
    }

    private void doDelete(Operation op) {
        int caret = editor.getCaretPosition();
        int start = op.getLeftIdx();
        editor.deleteText(start-1, start);
        if (start <= caret) {
            // Deleted prior to our caret position
            editor.positionCaret(caret - 1);
        }
    }

    /**
     * Updates the editor text area based on the Operation specified.
     */
    synchronized void applyBatched(Operation op) {
        if (op.getType() == Operation.INSERT) {
            doBatchedInsert(op);
        }
        if (op.getType() == Operation.DELETE) {
            doBatchedDelete(op);
        }
        if (op.getType() == Operation.REPLACE) {
            doReplace(op);
        }
    }

    private synchronized void doBatchedInsert(Operation op) {
        int caret = editor.getCaretPosition();
        int start = op.getLeftIdx();
        String content = op.getContent();
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

    private synchronized void doBatchedDelete(Operation op) {
        int caret = editor.getCaretPosition();
        int left = op.getLeftIdx();
        int right = op.getRightIdx();
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

    private synchronized void doReplace(Operation op) {
        int caret = editor.getCaretPosition();
        int left = op.getLeftIdx();
        int right = op.getRightIdx();
        if (caret >= left && caret <= right) {
            caret = left;
        }
        else if (right < caret) {
            caret += op.getContent().length() - (right - left);
        }
        editor.deleteText(left, right);
        editor.insertText(left, op.getContent());
        editor.positionCaret(caret);
    }

    /**
     * Sends a save request to the file server.
     */
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

