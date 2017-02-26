package server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.InetAddress;

/**
 * Defines how the ChatServer handles messages
 */
public class ChatServerHandler extends SimpleChannelInboundHandler<String> {

    final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    /**
     * Called when a connection is established.
     */
    public void channelActive(final ChannelHandlerContext ctx) {
        ctx.writeAndFlush("Welcome to the collaborators' chat\r\n");
        channels.add(ctx.channel());
    }

    @Override
    /**
     * Called when a message arrives in the chat server channel.
     */
    public void channelRead0(ChannelHandlerContext ctx, String msg) {
        // Send received message to all clients
        System.err.println("CHAT RECEIVED: " + msg);
        for (Channel c: channels) {
                c.writeAndFlush(msg + "\n");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}
