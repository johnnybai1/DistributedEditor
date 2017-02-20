package editor;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import main.MainController;

public class EditorController {

    @FXML VBox editorBox;
    @FXML TextArea editor;

    private MainController mainController;

    public EditorController() {
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        editor.setWrapText(true);
        editor.setDisable(true);
    }

    public VBox getEditorBox() {
        return editorBox;
    }

    public TextArea getEditor() {
        return editor;
    }



}
