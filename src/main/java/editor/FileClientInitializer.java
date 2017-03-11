package editor;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;


public class FileClientInitializer extends ChannelInitializer<SocketChannel> {

    private EditorController controller;
    private String filePath;

    public FileClientInitializer(EditorController controller, String filePath) {
        this.controller = controller;
        this.filePath = filePath;
    }

    @Override
    /**
     */
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast("line based decoder", new LineBasedFrameDecoder(8192));
        pipeline.addLast("string decoder", new StringDecoder());
        pipeline.addLast("string encoder", new StringEncoder());
        pipeline.addLast("file client logic", new FileClientHandler(controller, filePath));


    }

}
