package server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;


public class EditorServerInitializer extends ChannelInitializer<SocketChannel> {

    public EditorServerInitializer() {
    }


    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast("delimiter decoder", new DelimiterBasedFrameDecoder(
                8192, Delimiters.lineDelimiter()));
        pipeline.addLast("string decoder", new StringDecoder());
        pipeline.addLast("string encoder", new StringEncoder());

        pipeline.addLast("ot logic", new EditorServerHandler());
    }
}
