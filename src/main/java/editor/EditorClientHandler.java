package editor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles incoming Operation objects from the server.
 */
public class EditorClientHandler extends SimpleChannelInboundHandler<Operation> {

    private EditorController controller;
    private String filePath;

    public EditorClientHandler(EditorController controller, String filePath) {
        this.controller = controller;
        this.filePath = filePath;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(new Operation(Operation.CONNECT, filePath));
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(1000), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Platform.runLater(() -> {
                    if (!controller.editing.get()) {
                        ctx.channel().flush();
                    }
                });
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE); // keep looping while application is open
        timeline.play();
    }

    @Override
    /**
     * Called when an Operation object arrives in this channel.
     */
    public void channelRead0(ChannelHandlerContext ctx, Operation op) throws Exception {
        if (op.getType() == Operation.CONNECT) {
            controller.setOpsReceived(Integer.parseInt(op.getContent()));
            controller.setClientId(op.getClientId());
        } else {
            Platform.runLater(() -> {
                synchronized(controller.outgoing) {
                    receiveOperation(op);
                }
            });
        }
    }

    private void receiveOperation(Operation rcvdOp) {
        ConcurrentLinkedQueue<Operation> outgoing = controller.outgoing;
        Operation fromServer = new Operation(rcvdOp);
        // Discard acknowledged messages
        if (!outgoing.isEmpty()) {
            for (Operation o : outgoing) {
                if (o.getOpsGenerated() < fromServer.getOpsReceived()) {
                    outgoing.remove(o);
                }
            }
        }
        Operation[] ops;
        Operation cPrime;
        Operation sPrime;
        for (int i = 0; i < outgoing.size(); i++) {
            // Transform incoming op with ones in outgoing queue
            Operation C = new Operation(outgoing.remove());
            if (C.getOpsGenerated() + C.getOpsReceived() ==
                    fromServer.getOpsGenerated() +
                    fromServer.getOpsReceived() &&
                    C.getClientId() < fromServer.getClientId()) {
                // our Id is lower, we have priority!
                ops = Operation.transformBatch(fromServer, C);
                cPrime = ops[1];
                sPrime = ops[0];
            } else {
                ops = Operation.transformBatch(C, fromServer);
                cPrime = ops[0]; // transformed CLIENT op
                sPrime = ops[1]; // transformed SERVER op
            }
            fromServer = sPrime;
            outgoing.add(cPrime);
        }
        final Operation toApply = fromServer;

        Platform.runLater(() -> {
            controller.applyBatched(toApply);
            controller.setOpsReceived(controller.getOpsReceived() + 1);
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
