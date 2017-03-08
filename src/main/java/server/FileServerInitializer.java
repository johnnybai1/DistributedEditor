package server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.concurrent.GlobalEventExecutor;


/**
 * Initializes server side channel socket for files
 *
 */
public class FileServerInitializer extends ChannelInitializer<SocketChannel> {	
	
	ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	
    @Override
    /**
     * Files are String-based
     */
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast("line based decoder", new LineBasedFrameDecoder(8192));
        pipeline.addLast("string decoder", new StringDecoder());
        pipeline.addLast("string encoder", new StringEncoder());
        pipeline.addLast("file server logic", new FileServerHandler(channels));
    }

}
