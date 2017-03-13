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
        // Tell chat server which file we are editing
        ctx.channel().writeAndFlush("CONNECTED::" + filePath + "\n");
        ctx.channel().writeAndFlush(controller.getAlias() + " has joined." + "\n");
    }

    @Override
    /**
     * Called when a message arrives in this client's channel
     */
    public void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        // Update chat's text area
        if (!msg.startsWith("CONNECTED::")) {
            Platform.runLater(() -> controller.updateMessages(msg + "\n"));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
