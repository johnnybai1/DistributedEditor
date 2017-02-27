package server;

import editor.Operation;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Defines how the Editor Server handles incoming Operation objects
 */
public class EditorServerHandler extends SimpleChannelInboundHandler<Operation> {

    static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private ConcurrentLinkedQueue<Operation> opLog;

    public EditorServerHandler(ConcurrentLinkedQueue<Operation> opLog) {
        this.opLog = opLog;
    }

    @Override
    /**
     * Called when a connection is established.
     */
    public void channelActive(final ChannelHandlerContext ctx) {
        channels.add(ctx.channel());
        ctx.writeAndFlush("Welcome to the editing\r\n");
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
        }
        else if (opLog.offer(op)) {
            System.out.println("Successfully offered!");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }



}
