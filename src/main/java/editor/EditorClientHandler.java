package editor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.ConcurrentLinkedQueue;

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

    /**
     * Called when this client receives an operation from the server.
     * @param rcvdOp: Operation received from the server
     */
    private void receiveOperation(Operation rcvdOp) {
        ConcurrentLinkedQueue<Operation> outgoing = controller.outgoing;
        for (Operation localOp : outgoing) {
            if (localOp.opsGenerated < rcvdOp.opsReceived) {
                if (outgoing.remove(localOp)) {
                    System.out.println("Removed: " + localOp.stringToSend());
                }
            }
        }
        for (int i = 0; i < outgoing.size(); i++) {
            // Transform received Op with Ops in outgoing queue

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
