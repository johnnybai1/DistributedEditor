package main;

import chat.ChatController;
import editor.EditorController;
import editor.Operation;
import javafx.fxml.FXML;
import javafx.scene.control.MenuBar;
import login.LoginController;

public class MainController {

    @FXML public ChatController chatController;

    @FXML public EditorController editorController;

    @FXML public LoginController loginController;

    @FXML public MenuBar menu;

    public MainController() {

    }

    @FXML
    public void initialize() {
        chatController.setMainController(this);
        editorController.setMainController(this);
        loginController.setMainController(this);

        chatController.getMessages().disableProperty().bind(loginController.getConnectPressed().not());
        chatController.getInput().disableProperty().bind(loginController.getConnectPressed().not());
        editorController.getEditor().disableProperty().bind(loginController.getConnectPressed().not());
        loginController.getLoginBox().disableProperty().bind(loginController.getConnectPressed());

    }

    @FXML
    public void printLog() {
        editorController.printLog();
    }

    @FXML
    public void apply() {
        Operation op1 = new Operation(Operation.INSERT);
//        Operation op2 = new Operation(Operation.DELETE);
        editorController.apply(op1);
//        editorController.apply(op2);

    }




}