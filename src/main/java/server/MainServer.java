package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AttributeKey;

import java.util.ArrayList;
import java.util.List;


/**
 * The main server backend for the editor, chat, and file storage applications. The chatting, 
 * editor, and file storage features listen on separate ports (9000, 9001, and 9002
 * respectively by default).
 */
public class MainServer {

    // To allow us to route Operations to appropriate clients
    public static final AttributeKey<String> PATHKEY = AttributeKey.valueOf("filepath");
    public static final AttributeKey<Boolean> SAVEKEY = AttributeKey.valueOf("saving");

    private int chatPort; // Chat server port we are listening on
    private int editorPort; // Editor server port we are listening on
    private int filePort; // File server port we are listening on

    public static final String root = "root/";

    public MainServer(int chatPort, int editorPort, int filePort) throws Exception {
        this.chatPort = chatPort;
        this.editorPort = editorPort;
        this.filePort = filePort;
    }

    public MainServer() throws Exception {
        this(9000, 9001, 9002);
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
                    .childHandler(new EditorServerInitializer());
            futures.add(editor.bind(editorPort));
            // Start the file server
            ServerBootstrap fileServer = new ServerBootstrap();
            fileServer.group(bossGroup, workerGroup)
            		.channel(NioServerSocketChannel.class)
            		.handler(new LoggingHandler(LogLevel.INFO))
            		.childHandler(new FileServerInitializer());
            futures.add(fileServer.bind(filePort));
            
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
        MainServer server;
        if (args.length == 2) {
            int port = Integer.parseInt(args[0]);
            // Run on sequential ports
            server = new MainServer(port, port+1, port+2);
        }
        else {
            server = new MainServer(); // Default ports
        }
        server.run();
    }


}
