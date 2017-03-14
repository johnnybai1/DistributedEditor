package main;

import chat.ChatController;
import com.sun.javafx.robot.FXRobot;
import com.sun.javafx.robot.FXRobotFactory;
import com.sun.javafx.robot.FXRobotImage;
import com.sun.javafx.robot.impl.BaseFXRobot;
import editor.EditorController;
import editor.Operation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.MenuBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import login.LoginController;

import java.awt.event.KeyEvent;

/**
 * Serves primarily as a communication point for controllers.
 */
public class MainController {

    @FXML public ChatController chatController;

    @FXML public EditorController editorController;

    @FXML public LoginController loginController;

    @FXML public MenuBar menu;

    @FXML public BorderPane root;

    Timeline idleCheck;
    int IDLE_CHECK_TIME = 700;

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

    public void robotStart() {
        FXRobot robot = FXRobotFactory.createRobot(root.getScene());
        idleCheck = new Timeline(new KeyFrame(Duration.millis(IDLE_CHECK_TIME), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                robot.keyType(KeyCode.C, "C");
            }
        }));
        idleCheck.setCycleCount(Timeline.INDEFINITE); // keep looping while application is open
        idleCheck.play();
    }

    @FXML
    public void robotStop() {
        idleCheck.stop();
        idleCheck.pause();
    }

}