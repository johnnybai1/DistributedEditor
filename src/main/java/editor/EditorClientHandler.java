package editor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import javafx.application.Platform;

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
        if (op.type == Operation.CONNECT) {
            controller.opsReceived = Integer.parseInt(op.content);
        }
        else {
            receiveOperation(op);
        }
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
        final Operation toApply = fromServer;

        Platform.runLater(() -> {
            controller.apply(toApply);
                });
        controller.opsReceived += 1;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
