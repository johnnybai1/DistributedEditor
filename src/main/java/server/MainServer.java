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
 * The main server backend for the editor and chat applications. The chatting
 * and editor features listen on separate ports (9000 and 9001 respectively by
 * default).
 */
public class MainServer {

    // To store operations from clients
    private ConcurrentLinkedQueue<Operation> opLog = new ConcurrentLinkedQueue<>();
    private int chatPort; // Chat server port we are listening on
    private int editorPort; // Editor server port we are listening on

    public MainServer(int chatPort, int editorPort) throws Exception {
        this.chatPort = chatPort;
        this.editorPort = editorPort;
    }

    public MainServer() throws Exception {
        this(9000, 9001);
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
            futures.add(chat.bind(chatPort));
            // Start the editor server
            ServerBootstrap editor = new ServerBootstrap();
            editor.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new EditorServerInitializer(opLog));
            futures.add(editor.bind(editorPort));
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
        MainServer server = new MainServer();
        server.run();
    }


}
