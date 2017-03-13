package chat;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import javafx.application.Platform;

/**
 * Handles incoming messages from the server for the chat feature.
 */
public class ChatClientHandler extends SimpleChannelInboundHandler<String> {

    private ChatController controller;
    private String filePath;

    public ChatClientHandler(ChatController controller, String filePath) {
        this.controller = controller;
        this.filePath = filePath;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().write("CONNECTED::" + filePath + "\n");
        // Tell chat server which file we are editing
    }

    @Override
    /**
     * Called when a message arrives in this client's channel
     */
    public void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        // Update chat's text area
        Platform.runLater(() -> controller.updateMessages(msg +"\n"));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
