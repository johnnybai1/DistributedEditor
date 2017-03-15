import editor.Operation;

/**
 * Tests single character edits.
 */
public class TransformTest {

    private static String apply(String s, Operation op) {
        if (op.getType() == Operation.INSERT) {
            return applyInsert(s, op);
        }
        if (op.getType() == Operation.DELETE) {
            return applyDelete(s, op);
        }
        return s;
    }

    private static String applyInsert(String s, Operation op) {
        int idx = op.getLeftIdx();
        String left = s.substring(0, idx);
        String right = s.substring(idx);
        return left + op.getContent() + right;
    }

    private static String applyDelete(String s, Operation op) {
        int idx = op.getLeftIdx();
        String left = "";
        if (idx > 0) {
            left = s.substring(0, idx-1);
        }
        String right = s.substring(idx);
        return left + right;
    }

    private static boolean doTransformApply(String s,
                                    Operation client, Operation server) {
        String clientLocal = apply(s, client); // What client sees
        String serverLocal = apply(s, server); // What server sees
        Operation[] ops = Operation.transform(client, server);
        Operation cPrime = ops[0]; // transformed client op
        Operation sPrime = ops[1]; // transformed server op
        String clientFinal = apply(clientLocal, sPrime);
        String serverFinal = apply(serverLocal, cPrime);
        if (clientFinal.equals(serverFinal)) {
            System.out.println("Success!");
            return true;
        }
        else {
            System.out.println("Failed!");
            System.out.println("Started with: " + s);
            System.out.println("Server applied " + server + ": " + serverLocal);
            System.out.println("Client applied " + client + ": " + clientLocal);
            System.out.println("ServerOp transformed into: " + sPrime);
            System.out.println("ClientOp transformed into: " + cPrime);
            System.out.println("Server final: " + serverFinal);
            System.out.println("Client final: " + clientFinal);
            return false;
        }
    }

    /**
     * Tests various insert cases
     */
    private static void testInsert() {
        String s = "";
        Operation server = Operation.insertOperation(0, "S");
        Operation client = Operation.insertOperation(0, "C");
        doTransformApply(s, client, server);
        s = "012345";
        doTransformApply(s, client, server);
        server = Operation.insertOperation(4, "S");
        client = Operation.insertOperation(2, "C");
        doTransformApply(s, client, server);
        server = Operation.insertOperation(1, "S");
        client = Operation.insertOperation(5, "C");
        doTransformApply(s, client, server);


    }

    /**
     * Tests various delete cases
     */
    private static void testDelete() {
        String s = "012";
        Operation server = Operation.deleteOperation(2);
        Operation client = Operation.deleteOperation(1);
        doTransformApply(s, client, server);
        server = Operation.deleteOperation(1);
        client = Operation.deleteOperation(3);
        doTransformApply(s, client, server);
        server = Operation.deleteOperation(2);
        client = Operation.deleteOperation(2);
        doTransformApply(s, client, server);
    }

    /**
     * Tests various insert/delete cases
     */
    private static void testInsertDelete() {
        String s = "0";
        Operation server = Operation.deleteOperation(1);
        Operation client = Operation.insertOperation(0, "C");
        doTransformApply(s, client, server);
        doTransformApply(s, server, client);
        client = Operation.insertOperation(1, "C");
        doTransformApply(s, client, server);
        doTransformApply(s, server, client);
    }

    public static void main(String[] args) {
        testInsert();
        testDelete();
        testInsertDelete();
    }

}
