package server;

import editor.Operation;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * Initializes the server-side socket channel for editing.
 */
public class EditorServerInitializer extends ChannelInitializer<SocketChannel> {

    private ConcurrentLinkedQueue<Operation> opLog;

    public EditorServerInitializer(ConcurrentLinkedQueue<Operation> opLog) {
        this.opLog = opLog;
    }

    @Override
    /**
     * Messages are Operation (object) based.
     */
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new ObjectDecoder(ClassResolvers.softCachingResolver(
                ClassLoader.getSystemClassLoader())));
        pipeline.addLast(new ObjectEncoder());
        pipeline.addLast("ot logic", new EditorServerHandler(opLog));
    }
}
