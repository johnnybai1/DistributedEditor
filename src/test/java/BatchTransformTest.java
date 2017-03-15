import editor.Operation;

/**
 * Tests batched (more than one character edits) transformations.
 */
public class BatchTransformTest {

    /**
     * Applies an operation to a string and returns the modified string
     */
    private static String applyBatch(String s, Operation op) {
        if (op.getType() == Operation.INSERT) {
            return applyBatchInsert(s, op);
        }
        if (op.getType() == Operation.DELETE) {
            return applyBatchDelete(s, op);
        }
        if (op.getType() == Operation.NO_OP) {
            return s;
        }
        return s;
    }

    /**
     * Performs an insert operation on the string.
     */
    private static String applyBatchInsert(String s, Operation op) {
        int idx = op.getLeftIdx();
        String left = s.substring(0, idx);
        String right = s.substring(idx);
        return left + op.getContent() + right;
    }

    /**
     * Performs a delete operation on the string.
     */
    private static String applyBatchDelete(String s, Operation op) {
        String left = "";
        if (op.getRightIdx() > 0) {
            left = s.substring(0, op.getRightIdx());
        }
        String right = s.substring(op.getLeftIdx());
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
    private static boolean doBatchTransformApply(String start,
                                         Operation client, Operation server) {
        String clientLocal = applyBatch(start, client);
        String serverLocal = applyBatch(start, server);
        Operation[] operations = Operation.transform(client, server);
        Operation forClient = operations[0];
        Operation forServer = operations[1];
        String clientFinal = applyBatch(clientLocal, forClient);
        String serverFinal = applyBatch(serverLocal, forServer);
        if (clientFinal.equals(serverFinal)) {
            System.out.println("Success: " + clientFinal);
            return true;
        }
        else {
            System.out.println("Failed: " + start);
            System.out.println("Client applied: " + client);
            System.out.println("Local client: " + clientLocal);
            System.out.println("From server: " + forClient);
            System.out.println("Final client: " + clientFinal);
            System.out.println("Server applied: " + server);
            System.out.println("Local server: " + serverLocal);
            System.out.println("From client: " + forServer);
            System.out.println("Final server: " + clientFinal);
            return false;
        }
    }

    private static void testBatchTransform() {
        testBatchTransformInsert();
        testBatchTransformDeleteCase1();
        testBatchTransformDeleteCase2();
        testBatchTransformDeleteCase3();
        testBatchTransformDeleteCase4();
        testBatchTransformDeleteCase5();
        testBatchTransformDeleteCase6();
    }

    private static void testBatchTransformInsert() {
        String start = "ABCDEF";
        Operation client = Operation.insertOperation(3, "XXX");
        Operation server = Operation.insertOperation(4, "ZZZ");
        doBatchTransformApply(start, client, server);
    }

    private static void testBatchTransformDeleteCase1() {
        // Case1: Server deletes a chunk entirely past Client
        String start = "ABCDEF";
        Operation client = Operation.deleteOperation(3,1);
        Operation server = Operation.deleteOperation(5,3);
        doBatchTransformApply(start, client, server);

        start = "123456789";
        client = Operation.deleteOperation(7,5);
        server = Operation.deleteOperation(5,0);
        doBatchTransformApply(start, client, server);
    }

    private static void testBatchTransformDeleteCase2() {
        // Case2: Client deletes a chunk entirely past Server
        String start = "ABCDEF";
        Operation client = Operation.deleteOperation(5,4);
        Operation server = Operation.deleteOperation(3,1);
        doBatchTransformApply(start, client, server);

        start = "123456789";
        client = Operation.deleteOperation(6,3);
        server = Operation.deleteOperation(3,1);
        doBatchTransformApply(start, client, server);
    }

    private static void testBatchTransformDeleteCase3() {
        // Case3: Client delete on the right of Server delete, but overlaps
        String start = "ABCDEF";
        Operation client = Operation.deleteOperation(5,3);
        Operation server = Operation.deleteOperation(4,2);
        doBatchTransformApply(start, client, server);

        start = "123456789";
    }

    private static void testBatchTransformDeleteCase4() {
        // Case4: Server delete on the right of Client delete, but overlaps
        String start = "ABCDEF";
        Operation client = Operation.deleteOperation(4, 2);
        Operation server = Operation.deleteOperation(5, 3);
        doBatchTransformApply(start, client, server);
    }

    private static void testBatchTransformDeleteCase5() {
        // Case5: Server delete covers entirety of Client's delete.
        String start = "ABCDEF";
        Operation client = Operation.deleteOperation(4, 2);
        Operation server = Operation.deleteOperation(5,1);
        doBatchTransformApply(start, client, server);
    }

    private static void testBatchTransformDeleteCase6() {
        // Case5: Client delete covers entirety of Server's delete.
        String start = "ABCDEF";
        Operation client = Operation.deleteOperation(5, 1);
        Operation server = Operation.deleteOperation(4,2);
        doBatchTransformApply(start, client, server);
    }

    public static void main(String[] args) {
        String s = "12345";
        Operation op = new Operation(Operation.INSERT);
        op.setLeftIdx(3);
        op.setContent("ADDED");
        String mod = applyBatchInsert(s, op);
        System.out.println(mod);

        s = "012345";
        op = new Operation(Operation.DELETE);
        op.setLeftIdx(2);
        op.setRightIdx(3);
        mod = applyBatchDelete(s, op);
        System.out.println(mod);

        testBatchTransform();
    }

}
