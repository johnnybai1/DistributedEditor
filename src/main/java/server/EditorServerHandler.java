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

    private int state;
    // TODO: Might not need the below if we use this server as message passer
    private ConcurrentLinkedQueue<Operation> outgoing; // queue of outgoing ops

    private int opsGenerated; // How many ops this server generated
    private int opsReceived; // How many ops this server received

    public EditorServerHandler(ChannelGroup cg, ConcurrentLinkedQueue<Operation> opLog) {
    	this.channels = cg;
        this.opLog = opLog;
        this.opsGenerated = 0;
        this.opsReceived = 0;
        this.state = 0;
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
            // Set incoming operation's received state to server's state
            op.opsReceived = state;
            // Increment server's state
            state += 1;
            // Send to every client, let client handle the operation
            for (Channel c : channels) {
                if (c == ctx.channel()) {
                    // Let sender know we processed this message
                    // TODO: not sure if this is actually needed. Placeholder.
//                    Operation ack = new Operation(op);
//                    ack.type += Operation.ACK;
//                    c.writeAndFlush(ack);
                    continue;
                }
                c.writeAndFlush(op);
            }
        }
    }

    /**
     * Called when this server receives an operation from a client.
     * @param rcvdOp: Operation received from the client.
     * @return: an Operation to be sent to clients
     */
    private Operation receiveOperation(Operation rcvdOp) {
        if (!outgoing.isEmpty()) {
            for (Operation localOp : outgoing) {
                if (localOp.opsGenerated < rcvdOp.opsReceived) {
                    if (outgoing.remove(localOp)) {
                        System.out.println("Removed: " + localOp);
                    }
                }
            }
//        Operation[] transformedOutgoing = new Operation[outgoing.size()];
            for (int i = 0; i < outgoing.size(); i++) {
                Operation server = new Operation(outgoing.remove()); // Copy the op
                Operation[] transformed = Operation.transform(rcvdOp, server);
                Operation forClient = transformed[1];
                Operation forServer = transformed[0];
                outgoing.add(forServer);
                rcvdOp = forClient;
            }
        }
        System.out.println("Applying: " + rcvdOp);
        opsReceived += 1;
        return rcvdOp;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }



}
