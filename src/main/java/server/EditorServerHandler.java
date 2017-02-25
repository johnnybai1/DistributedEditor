package server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.concurrent.ConcurrentLinkedQueue;


public class EditorServerHandler extends SimpleChannelInboundHandler<String> {

    final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private ConcurrentLinkedQueue<String> opLog;


    public EditorServerHandler(ConcurrentLinkedQueue<String> opLog) {
        this.opLog = opLog;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        channels.add(ctx.channel());
        ctx.writeAndFlush("Welcome to the editors' chat\r\n");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String op) {
        System.err.println("OPERATION RECEIVED: " + op);
        if (op.equals("PRINT")) {
            System.err.println("=================================");
            System.err.println("Number of operations in log: " + opLog.size());
            for (String oper : opLog) {
                System.err.println(oper);
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
