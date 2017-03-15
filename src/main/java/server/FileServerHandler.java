package server;

import java.io.*;
import java.util.HashMap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import sun.applet.Main;

public class FileServerHandler extends SimpleChannelInboundHandler<String> {

    ChannelGroup channels = null;
    static HashMap<Channel, FileWriter> updates = new HashMap<>();
    static HashMap<Channel, Integer> fileLengths = new HashMap<>();

    public FileServerHandler(ChannelGroup cg) {
        this.channels = cg;
    }

    @Override
    /**
     * Called when a connection is established.
     */
    public void channelActive(final ChannelHandlerContext ctx) {
        // Channel is not in SAVE mode
        ctx.channel().attr(MainServer.SAVEKEY).set(false);
        channels.add(ctx.channel());
    }

    @Override
    /**
     * Called when a message arrives in the file server channel.
     */
    public void channelRead0(ChannelHandlerContext ctx, String msg) throws IOException {
        if (msg.startsWith("savereq__")) {
            ctx.channel().attr(MainServer.SAVEKEY).set(true);
            String[] split = msg.split("__");
            File f = new File(MainServer.root + split[1]);
            FileWriter fw = new FileWriter(f, false);
            updates.put(ctx.channel(), fw);
            fileLengths.put(ctx.channel(), -1 * Integer.parseInt(split[2]));
            return;
        }
        if (ctx.channel().attr(MainServer.SAVEKEY).get()) {
            int received = fileLengths.get(ctx.channel()) + msg.length();
            if (received < 0) {
                received += 1;
                msg += "\n";
            }
            fileLengths.replace(ctx.channel(), received);
            updates.get(ctx.channel()).write(msg);
            if (received >= 0) {
                updates.get(ctx.channel()).close();
                updates.remove(ctx.channel());
                fileLengths.remove(ctx.channel());
                ctx.channel().attr(MainServer.SAVEKEY).set(false);
            }
        }
        else {
            File file = new File(MainServer.root + msg);
            if (file.exists()) {
                if (!file.isFile()) {
                    ctx.channel().writeAndFlush("Not a file: " + file + "\r\n");
                }
                FileRegion region = new DefaultFileRegion(new FileInputStream(file).getChannel(), 0, file.length());
                ctx.channel().writeAndFlush(file.length() + "\r\n"); // Tell client how long the file is
                ctx.channel().write(region);
                ctx.channel().writeAndFlush("\r\n");
            } else {
                if (file.createNewFile()) {
                    ctx.channel().writeAndFlush("0\r\n");
                }
            }
        }
    }




    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}
