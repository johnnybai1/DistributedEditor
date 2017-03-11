package editor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Handles incoming Operation objects from the server.
 */
public class EditorClientHandler extends SimpleChannelInboundHandler<Operation> {

    private EditorController controller;
    private String filePath;

    public EditorClientHandler(EditorController controller, String filePath) {
        this.controller = controller;
        this.filePath = filePath;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(new Operation(Operation.CONNECT, filePath));
    }

    @Override
    /**
     * Called when an Operation object arrives in this channel.
     */
    public void channelRead0(ChannelHandlerContext ctx, Operation op) throws Exception {
        System.err.println("FROM SERVER: " + op);
        receiveOperation(op);
    }

    private void receiveOperation(Operation rcvdOp) {
        ConcurrentLinkedQueue<Operation> outgoing = controller.outgoing;
        Operation fromServer = new Operation(rcvdOp);
        // Discard acknowledged messages
        if (!outgoing.isEmpty()) {
            for (Operation o : outgoing) {
                if (o.opsGenerated < fromServer.opsReceived) {
                    outgoing.remove(o);
                }
            }
        }
        Operation[] ops;
        Operation cPrime;
        Operation sPrime;
        for (int i = 0; i < outgoing.size(); i++) {
            // Transform incoming op with ones in outgoing queue
            Operation C = new Operation(outgoing.remove());
            if (C.opsGenerated + C.opsReceived == fromServer.opsGenerated +
                    fromServer.opsReceived &&
                    C.clientId < fromServer.clientId) {
                // our Id is lower, we have priority!
                ops = Operation.transform(fromServer, C);
                cPrime = ops[1];
                sPrime = ops[0];
            }
            else {
                ops = Operation.transform(C, fromServer);
                cPrime = ops[0]; // transformed CLIENT op
                sPrime = ops[1]; // transformed SERVER op
            }
            fromServer = sPrime;
            outgoing.add(cPrime);
        }
        controller.apply(fromServer);
        controller.opsReceived += 1;
    }

    /**
     * Called when this client receives an operation from the server.
     * @param rcvdOp: Operation received from the server
     */
    private void OLDreceiveOperation(Operation rcvdOp) {
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
