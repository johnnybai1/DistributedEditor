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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Stack;

public class EditorController {

    @FXML VBox editorBox; // Container holding the editor TextArea
    @FXML TextArea editor; // To display and edit contents of a text file

    private Stack<Operation> opLog; // Useful for REDO functionality
    private Operation op; // current operation we are building

    private File editingFile; // current file we are modifying
    private static final int IDLE_CHECK_TIME = 30;
    
    private MainController mainController; // To communicate with other controllers
    private Channel channel; // Connection to server

    public EditorController() {
        opLog = new Stack<>();
        op = null;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    @SuppressWarnings("restriction")
	@FXML
    public void initialize() {    	
    	
    	// Timer to check if user is idle, runs every 30 seconds
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
                    op.finalPos = editor.getCaretPosition(); // Set final position of caret
                    opLog.push(op); // Push the delete op into logs
                    op = new Operation(Operation.INSERT); // Create new Operation for these inserts
                    op.startPos = editor.getCaretPosition(); // Set start position
                }
                op.content += c; // Append characters to track changes
                if (c.equals("\r") || c.equals(" ") ||
                        c.equals(".")) {
                    // Enter, space, or period triggers a push.
                    // Final Position may not be needed for inserts, since we
                    // can simply get op.content.length() for inserts.
                    op.finalPos = editor.getCaretPosition();
                    opLog.push(op); // Push to logs
                    send(op); // To test send to serverLog
                    op = null; // Make current op null again
                }
            }
        });

        // TODO: track where the caret currently is BEFORE the click occurred.
        // TODO: changing cursor position forces an op push
        // Currently just prints where the caret position is when you clicked
        editor.setOnMouseClicked(event -> {
//            if (op != null) {
//                opLog.push(op);
//                op = null;
//            }
            System.out.println(editor.getCaretPosition());
        });

        editor.setOnKeyPressed(event -> {
            // KeyPressed are keys that would not be displayed on the TextArea
            // For special keys, e.g. backspace, command, shift, etc.
            // Can use this to introduce shortcuts (e.g. COMMAND+S to save)
            if (event.getCode() == KeyCode.BACK_SPACE) {
                if (op == null) {
                    // Create a new operation for deletions
                    op = new Operation(Operation.DELETE);
                    op.startPos = editor.getCaretPosition();
                }
                if (op.type == Operation.INSERT) {
                    // If we switched from inserting to deleting
                    opLog.push(op);
                    op = new Operation(Operation.DELETE);
                    op.startPos = editor.getCaretPosition();
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
                ChannelFuture f = channel.writeAndFlush(op);
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

    @FXML
    // TODO: Remove if we choose to communicate via Operation objects
    /**
     * Sends a String message to the EditorServer
     */
    public void send(String msg) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                ChannelFuture f = channel.writeAndFlush(msg + "\r\n");
                f.sync();
                return null;
            }
            @Override
            protected void succeeded() {
                System.out.println("SENT OPERATION TO SERVER: " + msg);
            }
        };
        new Thread(task).start();
    }

    /**
     * Intended to be called by the LoginController. User specifies a file to
     * populate the editor (TextArea) with.
     * @param file
     */
    public void populateEditor(File file) {
        this.editingFile = file;
        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file)));
            String line;
            while ((line = br.readLine()) != null) {
                editor.appendText(line + "\r\n");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the editor text area based on the Operation specified.
     */
    public void apply(Operation op) {

    }

    /**
     * Prints the current log of operations
     */
    public void printLog() {
        for (int i = 0; i < opLog.size(); i++) {
            System.out.println("Op" + i + ": " + opLog.get(i));
        }
    }

    public VBox getEditorBox() {
        return editorBox;
    }

    public TextArea getEditor() {
        return editor;
    }




}

