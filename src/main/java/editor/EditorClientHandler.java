package editor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Handles incoming Operation objects from the server.
 */
public class EditorClientHandler extends SimpleChannelInboundHandler<Operation> {

    private EditorController controller;

    public EditorClientHandler(EditorController controller) {
        this.controller = controller;
    }

    @Override
    /**
     * Called when an Operation object arrives in this channel.
     */
    public void channelRead0(ChannelHandlerContext ctx, Operation op) throws Exception {
        System.err.println("FROM SERVER: " + op);
        // TODO: Apply the op to editor's text area
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
