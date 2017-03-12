package chat;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * Initializes a channel to the server for the chat feature.
 */
public class ChatClientInitializer extends ChannelInitializer<SocketChannel> {

    private ChatController controller;
    private String filePath;

    public ChatClientInitializer(ChatController controller, String filePath) {
        this.controller = controller;
        this.filePath = filePath;
    }

    @Override
    /**
     * Chatting relies on messages passed as String objects.
     */
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
        pipeline.addLast(new StringDecoder());
        pipeline.addLast(new StringEncoder());
        pipeline.addLast(new ChatClientHandler(controller, filePath));


    }

}
