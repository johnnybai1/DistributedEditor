package login;

import chat.ChatClientInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import main.MainController;

public class LoginController {

    private BooleanProperty connectPressed = new SimpleBooleanProperty(false);
    @FXML VBox loginBox;
    @FXML TextField conField;
    @FXML TextField fileField;
    @FXML TextField aliasField;
    @FXML Button conButton;

    private MainController mainController;

    private String alias;
    private String host;
    private int port;
    private String filePath;

    private Channel channel;
    private EventLoopGroup group;

    public LoginController() {
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
    }

    @FXML
    public void connect() {
        // if connected, do stuff, return true
        extractFields();
        group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChatClientInitializer(mainController.chatController));
            channel = b.connect(host, port).sync().channel();
            mainController.chatController.setAlias(alias);
            mainController.chatController.setChannel(channel);
            connectPressed.set(true);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void extractFields() {
        alias = aliasField.getText();
        String[] con = conField.getText().split(":");
        host = con[0];
        port = Integer.parseInt(con[1]);
        filePath = fileField.getText();
    }

    public VBox getLoginBox() {
        return loginBox;
    }

    public TextField getConField() {
        return conField;
    }

    public TextField getFileField() {
        return fileField;
    }

    public TextField getAliasField() {
        return aliasField;
    }

    public Button getConButton() {
        return conButton;
    }

    public BooleanProperty getConnectPressed() {
        return connectPressed;
    }

    public String getAlias() {
        return alias;
    }

    public String getFilePath() {
        return filePath;
    }

    public Channel getChannel() {
        return channel;
    }


}
