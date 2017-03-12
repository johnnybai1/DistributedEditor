package main;

import chat.ChatController;
import editor.EditorController;
import editor.Operation;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.MenuBar;
import login.LoginController;

/**
 * Serves primarily as a communication point for controllers.
 */
public class MainController {

    @FXML public ChatController chatController;

    @FXML public EditorController editorController;

    @FXML public LoginController loginController;

    @FXML public MenuBar menu;

    public MainController() {
    }

    @FXML
    public void initialize() {
        // Register this MainController with sub controllers
        chatController.setMainController(this);
        editorController.setMainController(this);
        loginController.setMainController(this);
        // Activate certain fields if connection established
        chatController.getMessages().disableProperty().bind(loginController.getConnectPressed().not());
        chatController.getInput().disableProperty().bind(loginController.getConnectPressed().not());
        editorController.getEditor().disableProperty().bind(loginController.getConnectPressed().not());
        loginController.getLoginBox().disableProperty().bind(loginController.getConnectPressed());

    }

    @FXML
    public void close() {
        loginController.shutdown();
        Platform.exit();
    }

    @FXML
    public void save() {
        editorController.save();
    }
}