package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;

public class FileServerHandler extends SimpleChannelInboundHandler<String> {

	ChannelGroup channels = null;
	
	public FileServerHandler(ChannelGroup cg) {
		this.channels = cg;
	}

    @Override
    /**
     * Called when a connection is established.
     */
    public void channelActive(final ChannelHandlerContext ctx) {
        channels.add(ctx.channel());
    }

    @Override
    /**
     * Called when a message arrives in the file server channel.
     */
    public void channelRead0(ChannelHandlerContext ctx, String msg) throws FileNotFoundException {
        File file = new File(msg);
        if (file.exists()) {
            if (!file.isFile()) {
            	for (Channel c: channels)
            		ctx.writeAndFlush("Not a file: " + file + '\n');
                return;
            }
            for (Channel c: channels)
            {
	            // echo file contents
	            FileRegion region = new DefaultFileRegion(new FileInputStream(file).getChannel(), 0, file.length());
	            ctx.write(region);
	            ctx.writeAndFlush("\n");
            }
        } else {
        	for (Channel c : channels)
        		ctx.writeAndFlush("File not found: " + file + '\n');
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}
