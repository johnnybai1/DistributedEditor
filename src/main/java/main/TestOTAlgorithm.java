package main;

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

        // ClientA and ClientB each generate an operation to insert
        Operation one = Operation.insertOperation(0, "A");
        Operation two = Operation.insertOperation(0, "B");
        clientA.generate(one);
        clientB.generate(two);
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
        clientA.generate(three);

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
        int idx = op.startPos;
        String left = s.substring(0, idx);
        String right = s.substring(idx);
        return left + op.content + right;
    }

    static String applyDelete(String s, Operation op) {
        int idx = op.startPos;
        String left = "";
        if (idx > 0) {
            left = s.substring(0, idx-1);
        }
        String right = s.substring(idx);
        return left + right;
    }

    static boolean doTransformApply(String s,
                                            Operation client, Operation server) {
        String clientLocal = apply(s, client); // What client sees
        String serverLocal = apply(s, server); // What server sees
        Operation[] ops = Operation.transform(client, server);
        Operation forClient = ops[0]; // transformed server op
        Operation forServer = ops[1]; // transformed client op
        String clientFinal = apply(clientLocal, forClient);
        String serverFinal = apply(serverLocal, forServer);
        if (clientFinal.equals(serverFinal)) {
            System.out.println("Success!");
            return true;
        }
        else {
            System.out.println("Failed!");
            System.out.println("Started with: " + s);
            System.out.println("Server applied " + server + ": " + serverLocal);
            System.out.println("Client applied " + client + ": " + clientLocal);
            System.out.println("ServerOp transformed into: " + forClient);
            System.out.println("ClientOp transformed into: " + forServer);
            System.out.println("Server final: " + serverFinal);
            System.out.println("Client final: " + clientFinal);
            return false;
        }
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
        if (!out.isEmpty()) {
            for (Operation o : out) {
                if (o.opsGenerated < fromClient.opsReceived) {
                    out.remove(o);
                }
            }
        }
        for (int i = 0; i < out.size(); i++) {
            // Transform incoming op with ones in outgoing queue
            Operation S = new Operation(out.remove());
            Operation[] ops = Operation.transform(fromClient, S);
            Operation cPrime = ops[0]; // transformed CLIENT op
            Operation sPrime = ops[1]; // transformed SERVER op
            cPrime.opsReceived = opsRcv;
            fromClient = cPrime;
            out.add(sPrime);
        }
        out.add(fromClient);
        text = TestOTAlgorithm.apply(text, fromClient);
        opsRcv += 1;
        return fromClient;
    }

    @Override
    public String toString() {
        return "[Server: " + opsRcv + "]";
    }
}

class Client {

    public String name;
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
    }

    public Operation generate(Operation op) {
        // 1. Apply operation locally
        text = TestOTAlgorithm.apply(text, op);
        // 2. Update operation with state info
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
        for (int i = 0; i < out.size(); i++) {
            // Transform incoming op with ones in outgoing queue
            Operation C = new Operation(out.remove());
            Operation[] ops = Operation.transform(C, fromServer);
            Operation cPrime = ops[0]; // transformed CLIENT op
            Operation sPrime = ops[1]; // transformed SERVER op
            fromServer = sPrime;
            out.add(cPrime);
        }
        text = TestOTAlgorithm.apply(text, fromServer);
        opsRcv += 1;
        return fromServer;
    }

    @Override
    public String toString() {
        return "[" + name + " (" + opsGen + "," + opsRcv + ") " + text + "]";
    }

}

