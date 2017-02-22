package editor;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import main.MainController;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class EditorController {

    @FXML VBox editorBox;
    @FXML TextArea editor;

    private List<String> log;
    private String curr;

    private Stack<Operation> opLog;
    private Operation op;

    private File editingFile;

    private MainController mainController;

    public EditorController() {
        curr = "";
        log = new ArrayList<>();
        opLog = new Stack<>();
        op = null;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        editor.setWrapText(true);
        editor.setDisable(true);
        editor.setOnKeyTyped(event -> {
            // getCode() will be undefined for typed characters
            String c = event.getCharacter();
            if (!c.isEmpty()) {
                if (op == null) {
                    op = new Operation(Operation.INSERT);
                    op.startPos = editor.getCaretPosition();
                }
                // We had a series of deletes, but now we have an insert
                if (op.type == Operation.DELETE) {
                    // Push the delete op into logs
                    op.finalPos = editor.getCaretPosition();
                    opLog.push(op);
                    // Create new Operation for these inserts
                    op = new Operation(Operation.INSERT);
                    op.startPos = editor.getCaretPosition();
                }
                if (c.equals("\r")) {
                    op.content = op.content + "\r\n";
                }
                else op.content = op.content + c;
                if (c.equals("\r") || c.equals(" ")) {
                    // Enter pressed or Space pressed
                    System.out.println("Space or Enter hit, adding: " + op);
                    op.finalPos = editor.getCaretPosition();
                    opLog.push(op);
                    op = null;
                }
            }
        });

        editor.setOnMouseClicked(event -> {
            if (op != null) {
                opLog.push(op);
                op = null;
            }
            System.out.println(editor.getCaretPosition());
        });

        editor.setOnKeyPressed(event -> {
            // For special keys, e.g. enter, delete, shift, etc
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
