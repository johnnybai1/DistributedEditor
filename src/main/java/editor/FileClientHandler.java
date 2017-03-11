package editor;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import javafx.application.Platform;
public class FileClientHandler extends SimpleChannelInboundHandler<String> {

    private EditorController controller;
    private String filePath;
    private int received = -1;

    public FileClientHandler(EditorController controller, String filePath) {
        this.controller = controller;
        this.filePath = filePath;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        if (!filePath.isEmpty()) {
            ctx.channel().writeAndFlush(filePath + '\n');
        }
    }

    @Override
    /**
     */
    public void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        if (received == -1) {
            received = -1 * Integer.parseInt(msg);
        }
        else {
            received += msg.length();
            final String toApply = msg;
            if (received < 0) {
                Platform.runLater(() -> controller.populateEditor(toApply));
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
