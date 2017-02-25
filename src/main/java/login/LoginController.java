package login;

import chat.ChatClientInitializer;
import editor.EditorClientInitializer;
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
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import main.MainController;

import java.awt.event.KeyEvent;
import java.io.File;

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

    private Channel chatChannel;
    private EventLoopGroup chatGroup;

    private Channel editorChannel;
    private EventLoopGroup editorGroup;

    public LoginController() {
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        conField.setText("localhost:9000"); // default
        fileField.setText("/Users/Johnny/Documents/TEST.txt");
        loginBox.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                connect();
            }
        });
    }

    @FXML
    public void connect() {
        // if connected, do stuff, return true
        extractFields();
        chatGroup = new NioEventLoopGroup();
        editorGroup = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b
                    .group(chatGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChatClientInitializer(mainController.chatController));
            chatChannel = b.connect(host, port).sync().channel();
            mainController.chatController.setAlias(alias);
            mainController.chatController.setChannel(chatChannel);

            Bootstrap e = new Bootstrap();
            e
                    .group(editorGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new EditorClientInitializer(mainController.editorController));
            editorChannel = e.connect(host, port+1).sync().channel();
            mainController.editorController.setChannel(editorChannel);

            connectPressed.set(true);
            if (!filePath.isEmpty()) {
                File f = new File(filePath);
                if (f.exists() && f.isFile() && f.canRead()) {
                    mainController.editorController.populateEditor(f);
                }
            }
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

    public Channel getChatChannel() {
        return chatChannel;
    }

    public Channel getEditorChannel() {
        return editorChannel;
    }


}
