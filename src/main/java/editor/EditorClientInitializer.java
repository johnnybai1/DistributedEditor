package editor;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;


public class EditorClientInitializer extends ChannelInitializer<SocketChannel> {

    private EditorController controller;

    public EditorClientInitializer(EditorController controller) {
        this.controller = controller;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

//        pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
//        pipeline.addLast(new StringDecoder());
//        pipeline.addLast(new StringEncoder());
        pipeline.addLast(new ObjectDecoder(ClassResolvers.softCachingResolver(
                ClassLoader.getSystemClassLoader())));
        pipeline.addLast(new ObjectEncoder());
        pipeline.addLast(new EditorClientHandler(controller));


    }

}
