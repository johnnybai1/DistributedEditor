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

    @FXML VBox chatBox;
    @FXML TextArea messages;
    @FXML TextField input;

    private MainController mainController;
    private Channel channel;
    private String alias;

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

    @FXML
    public void initialize() {
        messages.setDisable(true);
        input.setDisable(true);
        input.setOnKeyPressed(event -> {
            if (event.getCode() == (KeyCode.ENTER)) {
                String msg = input.getText();
                // Send message
                send(msg);
                input.clear();
            }
        });
    }

    public void updateMessages(String msg) {
        messages.appendText(msg);
    }

    @FXML
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
