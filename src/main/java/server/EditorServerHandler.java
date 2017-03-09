package server;

import editor.Operation;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Defines how the Editor Server handles incoming Operation objects. The main
 * role of this server is to simply pass along operations to other clients.
 */
public class EditorServerHandler extends SimpleChannelInboundHandler<Operation> {

    static ChannelGroup channels = null;
    private ConcurrentLinkedQueue<Operation> opLog; // TODO: probably won't need this anymore

    // TODO: Might not need the below if we use this server as message passer
    private ConcurrentLinkedQueue<Operation> outgoing; // queue of outgoing ops

    private int opsGenerated; // How many ops this server generated
    private int opsReceived; // How many ops this server received

    public EditorServerHandler(ChannelGroup cg, ConcurrentLinkedQueue<Operation> opLog) {
        this.channels = cg;
        this.opLog = opLog;
        this.opsGenerated = 0;
        this.opsReceived = 0;
        outgoing = new ConcurrentLinkedQueue<>();
    }

    @Override
    /**
     * Called when a connection is established.
     */
    public void channelActive(final ChannelHandlerContext ctx) {
        channels.add(ctx.channel());
    }


    @Override
    /**
     * Called when an Operation object arrives in the channel.
     */
    protected void channelRead0(ChannelHandlerContext ctx, Operation op) {
        if (op.type == Operation.PRINT) {
            System.err.println("=================================");
            System.err.println("Number of operations in log: " + opLog.size());
            for (Operation oper : opLog) {
                System.err.println(oper);
            }
        } else {
            System.out.println("RECEIVED: " + op);
            opLog.add(op);
            Operation toClients = receiveOperation(op);
            for (Channel c : channels) {
                if (c == ctx.channel()) {
                    c.writeAndFlush(new Operation(Operation.ACK));
                    continue;
                }
                c.writeAndFlush(toClients);
            }
        }
    }

    /**
     * Called when this server receives an operation from a client.
     *
     * @param rcvdOp: Operation received from the client.
     * @return an Operation to be sent to clients
     */
    private Operation receiveOperation(Operation rcvdOp) {
        Operation fromClient = new Operation(rcvdOp);
        // Discard acknowledged messages
        if (!outgoing.isEmpty()) {
            for (Operation localOp : outgoing) {
                if (localOp.opsGenerated < fromClient.opsReceived) {
                    if (outgoing.remove(localOp)) {
                        System.out.println("Removed: " + localOp);
                    }
                }
            }
        }
        for (int i = 0; i < outgoing.size(); i++) {
            // Transform incoming op with ones in outgoing queue
            Operation S = new Operation(outgoing.remove()); // Copy the op
            Operation[] transformed = Operation.transform(fromClient, S);
            Operation cPrime = transformed[0];
            Operation sPrime = transformed[1];
            System.out.println("cPrime: " + cPrime);
            System.out.println("sPrime: " + sPrime);
            cPrime.opsReceived = opsReceived;
            fromClient = cPrime;
            outgoing.add(sPrime);
        }
        outgoing.add(fromClient);
        System.out.println("Applying: " + fromClient);
        opsReceived += 1;
        return fromClient;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }


}
