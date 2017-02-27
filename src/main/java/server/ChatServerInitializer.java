package server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * Initializes the server-side socket channel for chatting.
 */
public class ChatServerInitializer extends ChannelInitializer<SocketChannel> {

	ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	
    @Override
    /**
     * Messages are String-based for chat
     */
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast("delimiter decoder", new DelimiterBasedFrameDecoder(
                8192, Delimiters.lineDelimiter()));
        pipeline.addLast("string decoder", new StringDecoder());
        pipeline.addLast("string encoder", new StringEncoder());
        pipeline.addLast("chat logic", new ChatServerHandler(channels));
    }

}
