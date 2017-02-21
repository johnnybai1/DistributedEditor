package editor;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import main.MainController;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class EditorController {

    @FXML VBox editorBox;
    @FXML TextArea editor;

    private List<String> log;
    private String curr;

    private MainController mainController;

    public EditorController() {
        curr = "";
        log = new ArrayList<>();
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        editor.setWrapText(true);
        editor.setDisable(true);
        editor.setOnKeyTyped(event -> {
            if (curr.isEmpty()) {
                System.out.println("Curr is empty: (" + curr + ")");
                curr = curr + editor.getCaretPosition();
            }
            curr = curr + event.getCharacter() + "-";
        });

        editor.setOnKeyPressed(event -> {
           if (event.getCode() == KeyCode.ENTER) {
               log.add(curr);
           }
        });
    }

    public void printLog() {
        for (int i = 0; i < log.size(); i++) {
            System.out.println("Op" + i + ": " + log.get(i));
        }
    }

    public VBox getEditorBox() {
        return editorBox;
    }

    public TextArea getEditor() {
        return editor;
    }



}
