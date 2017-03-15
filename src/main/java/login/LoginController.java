package login;

import chat.ChatClientInitializer;
import editor.EditorClientInitializer;
import editor.FileClientInitializer;
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

/**
 * Responsible for establishing a connection to the chat and editor servers as
 * well as loading the specified text file.
 */
public class LoginController {

    // To track whether or not we have connected yet
    private BooleanProperty connectPressed = new SimpleBooleanProperty(false);
    @FXML VBox loginBox; // Container holding the login feature
    @FXML TextField conField; // Connection information field
    @FXML TextField fileField; // File path field
    @FXML TextField aliasField; // Chatting alias field
    @FXML Button conButton; // Clickable button to execute the connect operation

    private MainController mainController; // To communicate with other controllers

    private String alias; // parsed from alias field
    private String host; // parsed from connection field
    private int port; // parsed from connection field
    private String filePath; // parsed for file path field

    private Channel chatChannel; // connection to server handling chat messages
    private EventLoopGroup chatGroup;

    private Channel editorChannel; // connection to server handling editing operations
    private EventLoopGroup editorGroup;

    private Channel fileChannel; // connection to server handling files
    private EventLoopGroup fileGroup;

    public LoginController() {
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        loginBox.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                connect();
            }
        });
    }

    @FXML
    /**
     * Based off on the information entered in the login box fields, attempt
     * to establish a connection.
     */
    public void connect() {
        extractFields();
        if (!host.isEmpty() && port > 0) {
            chatGroup = new NioEventLoopGroup();
            editorGroup = new NioEventLoopGroup();
            fileGroup = new NioEventLoopGroup();
            try {
                // Establish connection to MainServer
                Bootstrap bsChat = new Bootstrap();
                bsChat
                        .group(chatGroup)
                        .channel(NioSocketChannel.class)
                        .handler(new ChatClientInitializer(mainController.chatController, filePath));
                chatChannel = bsChat.connect(host, port).sync().channel();
                if (alias.isEmpty()) {
                    alias = "COLLAB" + mainController.editorController.getClientId();
                }
                mainController.chatController.setAlias(alias);
                mainController.chatController.setChannel(chatChannel);

                // Establish connection to EditorServer
                Bootstrap bsEditor = new Bootstrap();
                bsEditor
                        .group(editorGroup)
                        .channel(NioSocketChannel.class)
                        .handler(new EditorClientInitializer(mainController.editorController, filePath));
                editorChannel = bsEditor.connect(host, port + 1).sync().channel();
                mainController.editorController.setChannel(editorChannel);

                // Establish connection to FileServer
                Bootstrap bsFile = new Bootstrap();
                bsFile
                        .group(fileGroup)
                        .channel(NioSocketChannel.class)
                        .handler(new FileClientInitializer(mainController.editorController, filePath));
                fileChannel = bsFile.connect(host, port + 2).sync().channel();
                mainController.editorController.setFileChannel(fileChannel);

                connectPressed.set(true); // Connection established
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void extractFields() {
        alias = aliasField.getText();
        String[] con = conField.getText().split(":");
        host = con[0];
        port = Integer.parseInt(con[1]);
        // Use server's root as path
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

    public void shutdown() {
        chatGroup.shutdownGracefully();
        editorGroup.shutdownGracefully();
        fileGroup.shutdownGracefully();
    }


}
