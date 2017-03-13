package server;

import editor.Operation;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.Attribute;
import io.netty.util.concurrent.GlobalEventExecutor;
import sun.applet.Main;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Defines how the Editor Server handles incoming Operation objects. The main
 * role of this server is to simply pass along operations to other clients.
 */
public class EditorServerHandler extends SimpleChannelInboundHandler<Operation> {

    // Communication channels
    static ChannelGroup channels = null;
    // How many operations server has received for each file being edited
    static HashMap<String, Integer> fileStates = new HashMap<>(); // For loads
    static int clientCount = 0;


    public EditorServerHandler(ChannelGroup cg) {
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
     * Called when an Operation object arrives in the channel.
     */
    protected void channelRead0(ChannelHandlerContext ctx, Operation op) {
        if (op.type == Operation.CONNECT) {
            // Bind the file being edited to the channel
            ctx.channel().attr(MainServer.PATHKEY).setIfAbsent(op.content);
            Operation response = new Operation(Operation.CONNECT);
            response.content = String.valueOf(fileStates.getOrDefault(op.content, 0));
            response.clientId = clientCount++;
            ctx.channel().writeAndFlush(response);
        } else {
            String filePath = ctx.channel().attr(MainServer.PATHKEY).get();
            int curr = fileStates.getOrDefault(filePath, 0);
            fileStates.put(filePath, curr + 1);
            System.out.println("RECEIVED: " + op);
            Operation toClients = receiveOperation(op);
            for (Channel c : channels) {
                if (c == ctx.channel() ||
                        !filePath.equals(c.attr(MainServer.PATHKEY).get())) {
                    // Don't send to sender or clients editing other files
                    continue;
                }
                c.writeAndFlush(toClients);
            }
        }
    }

    /**
     * Called when this server receives an operation from a client.
     *
     * @param rcvdOp: Operation received from the client.
     * @return an Operation to be sent to clients
     */
    private Operation receiveOperation(Operation rcvdOp) {
        Operation fromClient = new Operation(rcvdOp);
        // Discard acknowledged messages
//        if (!outgoing.isEmpty()) {
//            for (Operation localOp : outgoing) {
//                if (localOp.opsGenerated < fromClient.opsReceived) {
//                    if (outgoing.remove(localOp)) {
//                        System.out.println("Removed: " + localOp);
//                    }
//                }
//            }
//        }
//        if (opsReceived > fromClient.opsGenerated + fromClient.opsReceived) {
//            for (int i = 0; i < outgoing.size(); i++) {
//                 Transform incoming op with ones in outgoing queue
//                Operation S = new Operation(outgoing.remove()); // Copy the op
//                Operation[] transformed = Operation.transform(fromClient, S);
//                Operation cPrime = transformed[0];
//                Operation sPrime = transformed[1];
//                cPrime.opsReceived = opsReceived;
//                fromClient = cPrime;
//                outgoing.add(sPrime);
//            }
//        }
//        outgoing.add(fromClient);
        return fromClient;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }


}
