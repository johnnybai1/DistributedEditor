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

    private int generatedOps; // How many ops this server generated
    private int receivedOps; // How many ops this server received

    public EditorServerHandler(ChannelGroup cg, ConcurrentLinkedQueue<Operation> opLog) {
    	this.channels = cg;
        this.opLog = opLog;
        this.generatedOps = 0;
        this.receivedOps = 0;
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
                System.err.println(oper.stringToSend());
            }
        } else {
            System.out.println("RECEIVED: " + op.stringToSend());
            opLog.add(op);

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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }



}
