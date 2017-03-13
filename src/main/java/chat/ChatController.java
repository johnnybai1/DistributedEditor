package chat;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import main.MainController;

public class ChatController {

    @FXML VBox chatBox; // Container holding the chat section
    @FXML TextArea messages; // To display chat messages
    @FXML TextField input; // For user to input messages

    private MainController mainController; // To communicate with other controllers

    private Channel channel; // Connection to server
    private String alias; // Name used to identify the chat participant

    public ChatController() {
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }

    @FXML
    public void initialize() {
        messages.setDisable(true); // Will be enabled upon connect
        input.setDisable(true); // Will be enabled upon connect
        input.setOnKeyPressed(event -> {
            if (event.getCode() == (KeyCode.ENTER)) {
                // Send message when user hits "ENTER/RETURN"
                String msg = input.getText();
                send(msg);
                input.clear();
            }
        });
        messages.setWrapText(true);
        messages.setEditable(false);
    }

    /**
     * Updates the messages TextArea with the specified String.
     */
    public void updateMessages(String msg) {
        messages.appendText(msg);
    }

    @FXML
    /**
     * Sends a message to the Server
     */
    public void send(String msg) {
        Task<Void> task = new Task<Void>() {

            @Override
            protected Void call() throws Exception {
                ChannelFuture f = channel.writeAndFlush(alias + ": " + msg + "\r\n");
                f.sync();
                return null;
            }

            @Override
            protected void succeeded() {
                input.setText("");
            }
        };
        new Thread(task).start();
    }

    public VBox getChatBox() {
        return chatBox;
    }

    public TextArea getMessages() {
        return messages;
    }

    public TextField getInput() {
        return input;
    }

}
