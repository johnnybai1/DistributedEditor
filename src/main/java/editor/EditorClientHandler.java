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
        receiveOperation(op);
    }

    /**
     * Called when this client receives an operation from the server.
     * @param rcvdOp: Operation received from the server
     */
    private void receiveOperation(Operation rcvdOp) {
        if (rcvdOp.type == Operation.ACK) {
            controller.opsReceived += 1;
            return;
        }
        Operation fromServer = new Operation(rcvdOp);
        ConcurrentLinkedQueue<Operation> outgoing = controller.outgoing;
        // Discard acknowledged operations
        if (!outgoing.isEmpty()) {
            for (Operation localOp : outgoing) {
                if (localOp.opsGenerated < fromServer.opsReceived) {
                    if (outgoing.remove(localOp)) {
                        System.out.println("Removed: " + localOp);
                    }
                }
            }
            for (int i = 0; i < outgoing.size(); i++) {
                // Transform incoming op with ones in outgoing queue
                Operation C = new Operation(outgoing.remove()); // Copy the op
                Operation[] transformed = Operation.transform(C, fromServer);
                Operation cPrime = transformed[0];
                Operation sPrime = transformed[1];
                fromServer = sPrime;
                outgoing.add(cPrime);
            }
        }
        controller.apply(fromServer);
        System.out.println("Applying: " + fromServer);
        controller.opsReceived += 1;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
