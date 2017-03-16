import editor.Operation;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Testing the OT algorithm, step by step
 */
public class TestOTAlgorithm {

    public static void main(String[] args) {
        System.out.println("Testing batched inserts");
        testBatchedInserts();
        System.out.println("Testing batched deletes");
        testBatchedDeletes();
        System.out.println("testing batched insert-delete");
        testBatchedInsertDelete();

    }

    private static void testBatchedOT() {
        Client clientA = new Client("Client A");
        Client clientB = new Client("Client B");
        Server server = new Server();

        Operation a = Operation.insertOperation(0, "12345");
        Operation b = Operation.insertOperation(0, "67890");
        test(server, clientA, a, clientB, b);
        // a delete is entirely after b's delete: works
        // b delete is entirely after a's delete: works
        // a delete covers b's delete: works
        // a delete is left, b delete is right; partial overlap: works
        // b delete covers a's delete: works
        // b delete is left, a delete is right; partial overlap:
//        a = Operation.deleteOperation(5, 8); // Server
//        b = Operation.deleteOperation(2, 7); // Client

        a = Operation.insertOperation(5, "XXX");
        b = Operation.deleteOperation(2, 4);
        test(server, clientA, b, clientB, a);
    }

    private static void testBatchedInserts() {
        Client clientA = new Client("Client A");
        Client clientB = new Client("Client B");
        Server server = new Server();
        // Test1. Same position
        Operation a = Operation.insertOperation(0, "12345");
        Operation b = Operation.insertOperation(0, "67890");
        test(server, clientA, a, clientB, b);
        // Test2. A before B
        a = Operation.insertOperation(3, "AAA");
        b = Operation.insertOperation(6, "BBB");
        test(server, clientA, a, clientB, b);
        // Test3. B before A
        a = Operation.insertOperation(7, "XXX");
        b = Operation.insertOperation(5, "YYY");
        test(server, clientA, a, clientB, b);
    }

    private static void testBatchedDeletes() {
        Client clientA = new Client("Client A", true);
        Client clientB = new Client("Client B", true);
        Server server = new Server();
        // Test1. Same position
        Operation a = Operation.deleteOperation(3,6);
        Operation b = Operation.deleteOperation(3,6);
        test(server, clientA, a, clientB, b);
        clientA.resetText();
        clientB.resetText();
        // Test2. A before B
        a = Operation.deleteOperation(2,5);
        b = Operation.deleteOperation(6,8);
        test(server, clientA, a, clientB, b);
        clientA.resetText();
        clientB.resetText();
        // Test3. B before A
        a = Operation.deleteOperation(5, 9);
        b = Operation.deleteOperation(1, 5);
        test(server, clientA, a, clientB, b);
        clientA.resetText();
        clientB.resetText();
        // Test4. A covers B
        a = Operation.deleteOperation(4, 8);
        b = Operation.deleteOperation(5, 7);
        test(server, clientA, a, clientB, b);
        clientA.resetText();
        clientB.resetText();
        // Test5. B covers A
        a = Operation.deleteOperation(3, 8);
        b = Operation.deleteOperation(2, 8);
        test(server, clientA, a, clientB, b);
        clientA.resetText();
        clientB.resetText();
        // Test6. A and B partially overlap at A's left
        a = Operation.deleteOperation(2, 6);
        b = Operation.deleteOperation(4, 8);
        test(server, clientA, a, clientB, b);
        clientA.resetText();
        clientB.resetText();
        // Test7. A and B partially overlap at B's left
        a = Operation.deleteOperation(5, 8);
        b = Operation.deleteOperation(2, 7);
        test(server, clientA, a, clientB, b);
        clientA.resetText();
        clientB.resetText();

    }

    private static void testBatchedInsertDelete() {
        Client clientA = new Client("Client A", true);
        Client clientB = new Client("Client B", true);
        Server server = new Server();
        // Test1. A inserts after B deletes
        Operation a = Operation.insertOperation(5, "AAA");
        Operation b = Operation.deleteOperation(1, 4);
        test(server, clientA, a, clientB, b);
        clientA.resetText();
        clientB.resetText();
        // Test2. A inserts before B deletes
        a = Operation.insertOperation(3, "AAA");
        b = Operation.deleteOperation(4, 8);
        test(server, clientA, a, clientB, b);
        clientA.resetText();
        clientB.resetText();
        // Test3. A inserts where B deletes
        a = Operation.insertOperation(4, "AAA");
        b = Operation.deleteOperation(3, 6);
        test(server, clientA, a, clientB, b);
        clientA.resetText();
        clientB.resetText();
    }

    private static void test(Server server, Client one, Operation a, Client two, Operation b) {
        a = one.generate(a);
        b = two.generate(b);
        Operation toSend = server.receive(a);
        two.receive(toSend);
        toSend = server.receive(b);
        one.receive(toSend);
        compareClients(one, two);
    }

    private static void compareClients(Client a, Client b) {
        if (a.text.equals(b.text)) {
            System.out.println("PASS");
        }
        else {
            System.out.println("ClientA: " + a);
            System.out.println("ClientB: " + b);
        }
    }

    private static void testSingleOT() {
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
        if (op.getType() == Operation.INSERT) {
            return applyInsert(s, op);
        }
        if (op.getType() == Operation.DELETE) {
            return applyDelete(s, op);
        }
        return s;
    }

    static String applyInsert(String s, Operation op) {
        int idx = op.getLeftIdx();
        String left = s.substring(0, idx);
        String right = s.substring(idx);
        return left + op.getContent() + right;
    }

    static String applyDelete(String s, Operation op) {
        int idx = op.getLeftIdx();
        String left = "";
        if (idx > 0) {
            left = s.substring(0, idx - 1);
        }
        String right = s.substring(idx);
        return left + right;
    }


    /**
     * Updates the editor text area based on the Operation specified.
     */
    public static String applyBatched(String s, Operation op) {
        if (op.getType() == Operation.INSERT) {
            return doBatchedInsert(s, op);
        }
        if (op.getType() == Operation.DELETE) {
            return doBatchedDelete(s, op);
        }
        if (op.getType() == Operation.REPLACE) {
            return doReplace(s, op);
        }
        return s;
    }

    private static String doBatchedInsert(String s, Operation op) {
        int idx = op.getLeftIdx();
        String left = s.substring(0, idx);
        String right = s.substring(idx);
        return left + op.getContent() + right;
    }

    private static String doBatchedDelete(String s, Operation op) {
        int leftIdx = op.getLeftIdx();
        int rightIdx = op.getRightIdx();
        String left = "";
        if (leftIdx > 0) {
            left = s.substring(0, leftIdx);
        }

        String right = s.substring(rightIdx);
        return left + right;
    }

    private static String doReplace(String s, Operation op) {
        int leftIdx = op.getLeftIdx();
        int rightIdx = op.getRightIdx();
        String left = s.substring(0, leftIdx);
        String right = s.substring(rightIdx);
        return left + op.getContent() + right;
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

    public Client(String name, boolean initialText) {
        this(name);
        if (initialText) {
            this.text = "01234567890";
        }
    }

    public void resetText() {
        text = "01234567890";
    }

    public Operation generate(Operation op) {
        // 1. Apply operation locally
        text = TestOTAlgorithm.applyBatched(text, op);
        // 2. Update operation with state info
        op.setClientId(id);
        op.setOpsGenerated(opsGen);
        op.setOpsReceived(opsRcv);
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
                if (o.getOpsGenerated() < fromServer.getOpsReceived()) {
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
            if (C.getOpsGenerated() + C.getOpsReceived() ==
                    fromServer.getOpsGenerated() + fromServer.getOpsReceived()
                    && C.getClientId()< fromServer.getClientId()) {
                // our Id is lower, we have priority!
                ops = Operation.transformBatch(fromServer, C);
                cPrime = ops[1];
                sPrime = ops[0];
            }
            else {
                ops = Operation.transformBatch(C, fromServer);
                cPrime = ops[0]; // transformed CLIENT op
                sPrime = ops[1]; // transformed SERVER op
            }
            fromServer = sPrime;
            out.add(cPrime);
        }
        text = TestOTAlgorithm.applyBatched(text, fromServer);
        opsRcv += 1;
        return fromServer;
    }

    @Override
    public String toString() {
        return "[" + id + " " + name + " (" + opsGen + "," + opsRcv + ") " + text + "]";
    }

}

