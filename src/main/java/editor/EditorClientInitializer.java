package editor;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class EditorClientInitializer extends ChannelInitializer<SocketChannel> {

    private EditorController controller;
    private String filePath;

    public EditorClientInitializer(EditorController controller, String filePath) {
        this.controller = controller;
        this.filePath = filePath;
    }

    @Override
    /**
     * Concurrent editing relies on messages passed as Operation objects.
     */
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(new ObjectDecoder(ClassResolvers.softCachingResolver(
                ClassLoader.getSystemClassLoader())));
        pipeline.addLast(new ObjectEncoder());
        pipeline.addLast(new EditorClientHandler(controller, filePath));


    }

}
