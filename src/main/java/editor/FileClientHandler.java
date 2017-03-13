package editor;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import javafx.application.Platform;
public class FileClientHandler extends SimpleChannelInboundHandler<String> {

    private EditorController controller;
    private String filePath;
    private int received = 1000;

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
        if (received == 1000 && !msg.isEmpty()) {
            received = -1 * Integer.parseInt(msg);
            controller.setFilePath(filePath);
        }
        else {
            received += msg.length();
            if (received < 0) {
                msg += "\n";
                received += 1;
            }
            final String toApply = msg;
            Platform.runLater(() -> controller.populateEditor(toApply));
        }
        if (received == 0) {
            received = 1000;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
