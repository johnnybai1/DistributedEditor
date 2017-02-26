package main;

import chat.ChatController;
import editor.EditorController;
import editor.Operation;
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
    /**
     * Debug tool: selecting print log in menu will print the local operation
     * log
     */
    public void printLog() {
        editorController.printLog();
    }

    @FXML
    /**
     * Debug tool: selecting print server log in menu will print the operations
     * stored in the server queue
     */
    public void printServerLog() {
        editorController.send(new Operation(Operation.PRINT));
    }

    @FXML
    // TODO: Debug tool: selecting apply() will apply some operation to the editor
    public void apply() {
        Operation op1 = new Operation(Operation.INSERT);
//        Operation op2 = new Operation(Operation.DELETE);
        editorController.apply(op1);
//        editorController.apply(op2);

    }




}