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

    private Timeline timeline; // For our robot
    private static int FREQUENCY = 300; // ms
    private FXRobot robot;
    private static String[] sequence = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};

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

    @FXML
    public void robotStart() {
        robot = FXRobotFactory.createRobot(root.getScene());
        timeline = new Timeline(new KeyFrame(Duration.millis(FREQUENCY), new EventHandler<ActionEvent>() {
            int track = 0;
            @Override
            public void handle(ActionEvent event) {
                doType(robot, sequence[track % 10]);
                robot.keyRelease(KeyCode.getKeyCode(sequence[track % 10]));
                track += 1;
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE); // keep looping while application is open
        timeline.play();
    }

    @FXML
    public void robotStop() {
        timeline.stop();
    }

    private void doType(FXRobot robot, String character) {
        switch (character) {
            case "0": robot.keyType(KeyCode.DIGIT0, character); break;
            case "1": robot.keyType(KeyCode.DIGIT1, character); break;
            case "2": robot.keyType(KeyCode.DIGIT2, character); break;
            case "3": robot.keyType(KeyCode.DIGIT3, character); break;
            case "4": robot.keyType(KeyCode.DIGIT4, character); break;
            case "5": robot.keyType(KeyCode.DIGIT5, character); break;
            case "6": robot.keyType(KeyCode.DIGIT6, character); break;
            case "7": robot.keyType(KeyCode.DIGIT7, character); break;
            case "8": robot.keyType(KeyCode.DIGIT8, character); break;
            case "9": robot.keyType(KeyCode.DIGIT9, character); break;
        }
    }

}