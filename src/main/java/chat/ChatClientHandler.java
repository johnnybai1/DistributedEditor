package chat;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import javafx.application.Platform;

public class ChatClientHandler extends SimpleChannelInboundHandler<String> {

    private ChatController controller;

    public ChatClientHandler(ChatController controller) {
        this.controller = controller;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        Platform.runLater(() -> controller.updateMessages(msg +"\n"));
        System.err.println(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
