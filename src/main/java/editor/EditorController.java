package editor;

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

    @FXML VBox editorBox;
    @FXML TextArea editor;

    private Stack<Operation> opLog; // Useful for REDO functionality
    private Operation op; // "current" operation we are building

    private File editingFile;
    private static final int IDLE_CHECK_TIME = 30;
    
    private MainController mainController;

    public EditorController() {
        opLog = new Stack<>();
        op = null;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
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
    	
        editor.setWrapText(true);
        editor.setDisable(true);
        editor.setOnKeyTyped(event -> {
            // getCode() will be undefined for typed characters
            String c = event.getCharacter();
            if (!c.isEmpty()) {
                if (op == null) {
                    // Create a new operation since none existed before
                    op = new Operation(Operation.INSERT);
                    // Set start position to where caret (cursor) currently is
                    op.startPos = editor.getCaretPosition();
                }
                // We had a series of deletes, but now we have an insert
                if (op.type == Operation.DELETE) {
                    // Set final position of caret
                    op.finalPos = editor.getCaretPosition();
                    // Push the delete op into logs
                    opLog.push(op);
                    // Create new Operation for these inserts
                    op = new Operation(Operation.INSERT);
                    // Set start position
                    op.startPos = editor.getCaretPosition();
                }
                // Append characters to track changes
                op.content += c;
                if (c.equals("\r") || c.equals(" ") ||
                        c.equals(".")) {
                    // Enter, space, or period triggers a push
                    // Final Position may not be needed for inserts, since we can
                    // simply get op.content.length()
                    op.finalPos = editor.getCaretPosition();
                    System.out.println("Space or Enter hit, adding: " + op);
                    // Push to logs
                    opLog.push(op);
                    // Make current op null again
                    op = null;
                }
            }
        });

        // TODO: track where the caret currently is BEFORE the click occurred
        // Currently just prints where the caret position is when you clicked
        editor.setOnMouseClicked(event -> {

//            if (op != null) {
//                opLog.push(op);
//                op = null;
//            }
            System.out.println(editor.getCaretPosition());
        });

        editor.setOnKeyPressed(event -> {
            // For special keys, e.g. backspace, command, shift, etc.
            // Can use this to introduce shortcuts (e.g. COMMAND+S to save)
            if (event.getCode() == KeyCode.BACK_SPACE) {
                if (op == null) {
                    op = new Operation(Operation.DELETE);
                    op.startPos = editor.getCaretPosition();
                }
                if (op.type == Operation.INSERT) {
                    opLog.push(op);
                    op = new Operation(Operation.DELETE);
                    op.startPos = editor.getCaretPosition();
                }
            }
        });
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

