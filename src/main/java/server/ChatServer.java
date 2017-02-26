package server;

import editor.Operation;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * Receives string messages from clients and broadcasts them back to clients
 */
public class ChatServer {

    private ConcurrentLinkedQueue<Operation> opLog = new ConcurrentLinkedQueue<>();

    private int port;

    public ChatServer(int port) throws Exception {
        this.port = port;
    }

    public ChatServer() throws Exception {
        this(9000);
    }

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        List<ChannelFuture> futures = new ArrayList<>();
        try {
            // Start the chat server
            ServerBootstrap chat = new ServerBootstrap();
            chat.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChatServerInitializer());
            futures.add(chat.bind(port));
//            b.bind(port).sync().channel().closeFuture().sync();
            // Start the editor server
            ServerBootstrap editor = new ServerBootstrap();
            editor.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new EditorServerInitializer(opLog));
            futures.add(editor.bind(port+1));
            for (ChannelFuture cf : futures) {
                cf.sync().channel().closeFuture().sync();
            }
        }
        finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        ChatServer server = new ChatServer();
        server.run();
    }


}
