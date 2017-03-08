package editor;

public class TransformTest {

    /**
     * Applies an operation to a string and returns the modified string
     */
    private static String apply(String s, Operation op) {
        if (op.type == Operation.INSERT) {
            return applyInsert(s, op);
        }
        if (op.type == Operation.DELETE) {
            return applyDelete(s, op);
        }
        if (op.type == Operation.NO_OP) {
            return s;
        }
        return s;
    }

    /**
     * Performs an insert operation on the string.
     */
    private static String applyInsert(String s, Operation op) {
        int idx = op.startPos;
        String left = s.substring(0, idx);
        String right = s.substring(idx);
        return left + op.content + right;
    }

    /**
     * Performs a delete operation on the string.
     */
    private static String applyDelete(String s, Operation op) {
        String left = "";
        if (op.finalPos > 0) {
            left = s.substring(0, op.finalPos);
        }
        String right = s.substring(op.startPos);
        return left + right;
    }

    /**
     * With client and server both seeing the same string, each performs a local
     * operation. The transformation algorithm is applied to return two
     * operations to be applied by the client and server to both end up in a
     * consistent document state
     * @param start: initial string seen by both client and server
     * @param client: operation performed by client
     * @param server: operation performed by server
     */
    private static boolean doTransformApply(String start,
                                         Operation client, Operation server) {
        String clientLocal = apply(start, client);
        String serverLocal = apply(start, server);
        Operation[] operations = Operation.transform(client, server);
        Operation forClient = operations[0];
        Operation forServer = operations[1];
        String clientFinal = apply(clientLocal, forClient);
        String serverFinal = apply(serverLocal, forServer);
        if (clientFinal.equals(serverFinal)) {
            System.out.println("Success: " + clientFinal);
            return true;
        }
        else {
            System.out.println("Failed: " + start);
            System.out.println("Client applied: " + client.stringToSend());
            System.out.println("Local client: " + clientLocal);
            System.out.println("From server: " + forClient.stringToSend());
            System.out.println("Final client: " + clientFinal);
            System.out.println("Server applied: " + server.stringToSend());
            System.out.println("Local server: " + serverLocal);
            System.out.println("From client: " + forServer.stringToSend());
            System.out.println("Final server: " + clientFinal);
            return false;
        }
    }

    private static void testTransformApply() {
        testTransformInsert();
        testTransformDeleteCase1();
        testTransformDeleteCase2();
        testTransformDeleteCase3();
        testTransformDeleteCase4();
        testTransformDeleteCase5();
        testTransformDeleteCase6();
    }

    private static void testTransformInsert() {
        String start = "ABCDEF";
        Operation client = Operation.insertOperation(3, "XXX");
        Operation server = Operation.insertOperation(4, "ZZZ");
        doTransformApply(start, client, server);
    }

    private static void testTransformDeleteCase1() {
        // Case1: Server deletes a chunk entirely past Client
        String start = "ABCDEF";
        Operation client = Operation.deleteOperation(3,1);
        Operation server = Operation.deleteOperation(5,3);
        doTransformApply(start, client, server);

        start = "123456789";
        client = Operation.deleteOperation(7,5);
        server = Operation.deleteOperation(5,0);
        doTransformApply(start, client, server);
    }

    private static void testTransformDeleteCase2() {
        // Case2: Client deletes a chunk entirely past Server
        String start = "ABCDEF";
        Operation client = Operation.deleteOperation(5,4);
        Operation server = Operation.deleteOperation(3,1);
        doTransformApply(start, client, server);

        start = "123456789";
        client = Operation.deleteOperation(6,3);
        server = Operation.deleteOperation(3,1);
        doTransformApply(start, client, server);
    }

    private static void testTransformDeleteCase3() {
        // Case3: Client delete on the right of Server delete, but overlaps
        String start = "ABCDEF";
        Operation client = Operation.deleteOperation(5,3);
        Operation server = Operation.deleteOperation(4,2);
        doTransformApply(start, client, server);

        start = "123456789";
        client = Operation.deleteOperation()
    }

    private static void testTransformDeleteCase4() {
        // Case4: Server delete on the right of Client delete, but overlaps
        String start = "ABCDEF";
        Operation client = Operation.deleteOperation(4, 2);
        Operation server = Operation.deleteOperation(5, 3);
        doTransformApply(start, client, server);
    }

    private static void testTransformDeleteCase5() {
        // Case5: Server delete covers entirety of Client's delete.
        String start = "ABCDEF";
        Operation client = Operation.deleteOperation(4, 2);
        Operation server = Operation.deleteOperation(5,1);
        doTransformApply(start, client, server);
    }

    private static void testTransformDeleteCase6() {
        // Case5: Client delete covers entirety of Server's delete.
        String start = "ABCDEF";
        Operation client = Operation.deleteOperation(5, 1);
        Operation server = Operation.deleteOperation(4,2);
        doTransformApply(start, client, server);
    }

    public static void main(String[] args) {
        String s = "12345";
        Operation op = new Operation(Operation.INSERT);
        op.startPos = 3;
        op.content = "ADDED";
        String mod = applyInsert(s, op);
        System.out.println(mod);

        s = "012345";
        op = new Operation(Operation.DELETE);
        op.startPos = 3;
        op.finalPos = 2;
        mod = applyDelete(s, op);
        System.out.println(mod);

        testTransformApply();
    }

}
