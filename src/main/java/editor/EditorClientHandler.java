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
        if (controller.op != null) {
            if (op.type == Operation.INSERT) {
                controller.op.startPos += op.content.length();
                controller.send(controller.op);
                controller.op = null;
            }
        }
        receiveOperation(op);
    }

    /**
     * Called when this client receives an operation from the server.
     * @param rcvdOp: Operation received from the server
     */
    private void receiveOperation(Operation rcvdOp) {
        ConcurrentLinkedQueue<Operation> outgoing = controller.outgoing;
        if (!outgoing.isEmpty()) {
            for (Operation localOp : outgoing) {
                if (localOp.opsGenerated < rcvdOp.opsReceived) {
                    if (outgoing.remove(localOp)) {
                        System.out.println("Removed: " + localOp.stringToSend());
                    }
                }
            }
//        Operation[] transformedOutgoing = new Operation[outgoing.size()];
            for (int i = 0; i < outgoing.size(); i++) {
                Operation client = new Operation(outgoing.remove()); // Copy the op
                Operation[] transformed = Operation.transform(client, rcvdOp);
                Operation forClient = transformed[0];
                Operation forServer = transformed[1];
                outgoing.add(forServer);
                rcvdOp = forClient;
            }
        }
        controller.apply(rcvdOp);
        System.out.println("Applying: " + rcvdOp.stringToSend());
        controller.opsReceived += 1;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
