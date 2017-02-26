package server;

import editor.Operation;
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

import java.util.concurrent.ConcurrentLinkedQueue;


public class EditorServerInitializer extends ChannelInitializer<SocketChannel> {

    ConcurrentLinkedQueue<Operation> opLog;

    public EditorServerInitializer(ConcurrentLinkedQueue<Operation> opLog) {
        this.opLog = opLog;
    }


    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

//        pipeline.addLast("delimiter decoder", new DelimiterBasedFrameDecoder(
//                8192, Delimiters.lineDelimiter()));
//        pipeline.addLast("string decoder", new StringDecoder());
//        pipeline.addLast("string encoder", new StringEncoder());
        pipeline.addLast(new ObjectDecoder(ClassResolvers.softCachingResolver(
                ClassLoader.getSystemClassLoader())));
        pipeline.addLast(new ObjectEncoder());
        pipeline.addLast("ot logic", new EditorServerHandler(opLog));
    }
}
