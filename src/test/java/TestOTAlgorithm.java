import editor.Operation;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Testing the OT algorithm, step by step
 */
public class TestOTAlgorithm {

    public static void main(String[] args) {
        Client clientA = new Client("Client A");
        Client clientB = new Client("Client B");
        Server server = new Server();

        Operation a = Operation.insertOperation(0, "a");
        Operation b = Operation.insertOperation(1, "b");
        a = clientA.generate(a);
        Operation toSend = server.receive(a);
        clientB.receive(toSend);
        b = clientA.generate(b);
        toSend = server.receive(b);
        clientB.receive(toSend);
        System.out.println(clientA);
        System.out.println(clientB);

//         ClientA and ClientB each generate an operation to insert
        Operation one = Operation.insertOperation(0, "A");
        Operation two = Operation.insertOperation(0, "B");
        one = clientA.generate(one);
        two = clientB.generate(two);
        System.out.println(clientA);
        System.out.println(clientB);

        // Suppose server receives operation from clientA first
        Operation first = server.receive(one);
        clientB.receive(first);
        System.out.println(clientB);

        Operation second = server.receive(two);
        clientA.receive(second);
        System.out.println(clientA);

        Operation three = Operation.insertOperation(2, "X");
        three = clientA.generate(three);

        Operation third = server.receive(three);
        clientB.receive(third);

        System.out.println(clientA);
        System.out.println(clientB);

    }

    static String apply(String s, Operation op) {
        if (op.type == Operation.INSERT) {
            return applyInsert(s, op);
        }
        if (op.type == Operation.DELETE) {
            return applyDelete(s, op);
        }
        return s;
    }

    static String applyInsert(String s, Operation op) {
        int idx = op.leftIdx;
        String left = s.substring(0, idx);
        String right = s.substring(idx);
        return left + op.content + right;
    }

    static String applyDelete(String s, Operation op) {
        int idx = op.leftIdx;
        String left = "";
        if (idx > 0) {
            left = s.substring(0, idx - 1);
        }
        String right = s.substring(idx);
        return left + right;
    }

}

class Server {
    public String text;
    public int opsRcv;
    public ConcurrentLinkedQueue<Operation> out;

    public Server() {
        text = "";
        opsRcv = 0;
        out = new ConcurrentLinkedQueue<>();
    }

    // Server receives an operation from the client
    public Operation receive(Operation C) {
        Operation fromClient = new Operation(C);
        // Discard acknowledged messages
//        if (!out.isEmpty()) {
//            for (Operation o : out) {
//                if (o.opsGenerated < fromClient.opsReceived) {
//                    out.remove(o);
//                }
//            }
//        }
//        if (opsRcv > fromClient.opsGenerated + fromClient.opsReceived) {
//            if (opsRcv > fromClient.opsReceived) {
//                for (int i = 0; i < out.size(); i++) {
//                     Transform incoming op with ones in outgoing queue
//                    Operation S = new Operation(out.remove());
//                    Operation[] ops = Operation.transform(fromClient, S);
//                    Operation cPrime = ops[0]; // transformed CLIENT op
//                    Operation sPrime = ops[1]; // transformed SERVER op
//                    cPrime.opsReceived = opsRcv;
//
//                    fromClient = cPrime;
//                    out.add(sPrime);
//                }
//            }
//        }
//        out.add(fromClient);
//        text = TestOTAlgorithm.apply(text, fromClient);
        opsRcv += 1;
        return fromClient;
    }

    @Override
    public String toString() {
        return "[Server: " + opsRcv + "]";
    }
}

class Client {

    static int numConnected = 0;

    public String name;
    public int id;
    public String text;
    public int opsGen;
    public int opsRcv;
    public ConcurrentLinkedQueue<Operation> out;

    public Client(String name) {
        this.name = name;
        text = "";
        opsGen = 0;
        opsRcv = 0;
        out = new ConcurrentLinkedQueue<>();
        this.id = numConnected + 1;
        numConnected += 1;
    }

    public Operation generate(Operation op) {
        // 1. Apply operation locally
        text = TestOTAlgorithm.apply(text, op);
        // 2. Update operation with state info
        op.clientId = id;
        op.opsGenerated = opsGen;
        op.opsReceived = opsRcv;
        // 3. Add op to outgoing queue
        out.add(op);
        // 4. Update state
        opsGen += 1;
        return op;
    }

    public Operation receive(Operation S) {
        Operation fromServer = new Operation(S);
        // Discard acknowledged messages
        if (!out.isEmpty()) {
            for (Operation o : out) {
                if (o.opsGenerated < fromServer.opsReceived) {
                    out.remove(o);
                }
            }
        }
        Operation[] ops;
        Operation cPrime;
        Operation sPrime;
        for (int i = 0; i < out.size(); i++) {
            // Transform incoming op with ones in outgoing queue
            Operation C = new Operation(out.remove());
            if (C.opsGenerated + C.opsReceived == fromServer.opsGenerated +
                    fromServer.opsReceived &&
                    C.clientId < fromServer.clientId) {
                // our Id is lower, we have priority!
                ops = Operation.transform(fromServer, C);
                cPrime = ops[1];
                sPrime = ops[0];
            }
            else {
                ops = Operation.transform(C, fromServer);
                cPrime = ops[0]; // transformed CLIENT op
                sPrime = ops[1]; // transformed SERVER op
            }
            fromServer = sPrime;
            out.add(cPrime);
        }
        text = TestOTAlgorithm.apply(text, fromServer);
        opsRcv += 1;
        return fromServer;
    }

    @Override
    public String toString() {
        return "[" + id + " " + name + " (" + opsGen + "," + opsRcv + ") " + text + "]";
    }

}

