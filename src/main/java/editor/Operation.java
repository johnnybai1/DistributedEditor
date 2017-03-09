package editor;


import java.io.Serializable;

/**
 * An object to encapsulate information about modifications being made to a text
 * file. Implements Serializable to allow sending through netty Channel.
 */
public class Operation implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int PRINT = -1; // Mostly for debugging

    public static final int INSERT = 2; // Adding text to the file
    public static final int DELETE = 1; // Removing text from the file
    public static final int NO_OP = 0; // No operation, do nothing
    public static final int ACK = 10; // Acknowledge op was applied

    public int opsGenerated; // Number of ops generated by the op creator
    public int opsReceived; // Number of ops received by the op creator
    public int type; // "how" to execute this operation
    public int startPos; // Position where we started editing
    public int finalPos; // Position where we finished editing
    public String content; // Applies only to INSERT ops

    public Operation(int type) {
        this.type = type;
        content = "";
    }

    public Operation(int opsGenerated, int opsReceived, int type, int startPos,
                     int finalPos) {
        this.opsGenerated = opsGenerated;
        this.opsReceived = opsReceived;
        this.type = type;
        this.startPos = startPos;
        this.finalPos = finalPos;
    }

    /**
     * Copy constructor
     */
    public Operation(Operation copy) {
        this.opsGenerated = copy.opsGenerated;
        this.opsReceived = copy.opsReceived;
        this.type = copy.type;
        this.startPos = copy.startPos;
        this.finalPos = copy.finalPos;
        this.content = copy.content;
    }

    /**
     * Returns an insert operation; intended to be used for testing.
     */
    public static Operation insertOperation(int startPos, String content) {
        Operation op = new Operation(INSERT);
        op.startPos = startPos;
        op.content = content;
        return op;
    }

    /**
     * Returns a delete operation; intended to be used for testing.
     */
    public static Operation deleteOperation(int startPos, int finalPos) {
        Operation op = new Operation(DELETE);
        op.startPos = startPos;
        op.finalPos = finalPos;
        return op;
    }

    public static Operation deleteOperation(int startPos) {
        return deleteOperation(startPos, startPos-1);
    }


    /**
     * The Ops generated field will be set during transmission.
     */
    public void setOpsGenerated(int opsGenerated) {
        this.opsGenerated = opsGenerated;
    }

    /**
     * The Ops received field will be set during transmission.
     */
    public void setOpsReceived(int opsReceived) {
        this.opsReceived = opsReceived;
    }

    public static Operation[] transform(Operation A, Operation B) {
        Operation[] ops = new Operation[2];
        if (A.type == INSERT && B.type == INSERT) {
            return transformInsert(A, B);
        }
        if (A.type == DELETE && B.type == DELETE) {
            return transformDelete(A, B);
        }
        if (A.type == INSERT && B.type == DELETE) {
            return transformInsertDelete(A, B);
        }
        if (A.type == DELETE && B.type == INSERT) {
            return transformDeleteInsert(A, B);
        }
        return ops;
    }

    /**
     * Transforms two insert operations against each other
     * @param C is the client's operation
     * @param S is the server's operation
     */
    public static Operation[] transformInsert(Operation C, Operation S) {
        Operation[] ops = new Operation[2];
        int idxC = C.startPos;
        int idxS = S.startPos;
        Operation cPrime = new Operation(C); // to be executed by S
        Operation sPrime = new Operation(S); // to be executed by C

        // S inserts at a position after C's insert
        if (idxS > idxC) {
            // C must insert S's operation at a later position
            sPrime.startPos += 1;
        }
        // S inserts at a position before C's insert
        if (idxS < idxC) {
            // S must insert C's operation at a later position
            cPrime.startPos += 1;
        }
        // C inserts at the same position as S's insert
        if (idxC == idxS) {
            // Let S win, S must insert C's operation at a later position
            cPrime.startPos += 1;
        }
        ops[0] = cPrime;
        ops[1] = sPrime;
        return ops;
    }

    /**
     * Transforms two deletions against each other.
     * @param C is the client's operation.
     * @param S is the server's operation.
     */
    public static Operation[] transformDelete(Operation C, Operation S) {
        Operation[] ops = new Operation[2];
        int idxC = C.startPos;
        int idxS = S.startPos;
        Operation cPrime = new Operation(C); // to be executed by C
        Operation sPrime = new Operation(S); // to be executed by S
        // S deleted at a position after C's deletion
        if (idxS > idxC) {
            sPrime.startPos -= 1;
        }
        // S deleted at a position before C's deletion
        if (idxS < idxC) {
            cPrime.startPos -= 1;
        }
        // S and C deleted at the same position
        if (idxS == idxC) {
            cPrime.type = Operation.NO_OP;
            sPrime.type = Operation.NO_OP;
        }
        ops[0] = cPrime;
        ops[1] = sPrime;
        return ops;
    }

    /**
     * Client made an insertion, server made a deletion.
     */
    public static Operation[] transformInsertDelete(Operation C, Operation S) {
        Operation[] ops = new Operation[2];
        int idxC = C.startPos;
        int idxS = S.startPos;
        Operation cPrime = new Operation(C);
        Operation sPrime = new Operation(S);
        // S deletes at a position after C's insertion
        if (idxS > idxC) {
            sPrime.startPos += 1;
        }
        // S deletes at a position before C's insertion
        if (idxS < idxC) {
            cPrime.startPos -= 1;
        }
        // S deletes at the same position of C's insertion
        if (idxS == idxC) {
            cPrime.startPos -= 1;
        }
        ops[0] = cPrime;
        ops[1] = sPrime;
        return ops;
    }

    /**
     * Client made a deletion, server made an insertion.
     */
    public static Operation[] transformDeleteInsert(Operation C, Operation S) {
        Operation[] ops = new Operation[2];
        int idxC = C.startPos;
        int idxS = S.startPos;
        Operation cPrime = new Operation(C);
        Operation sPrime = new Operation(S);
        if (idxS > idxC) {
            // Server inserts at a later position than client deletion
            sPrime.startPos -= 1;
        }
        if (idxS < idxC) {
            // Server inserts at an earlier position than client deletion
            cPrime.startPos += 1;
        }
        if (idxS == idxC) {
            sPrime.startPos -= 1;
        }
        ops[0] = cPrime;
        ops[1] = sPrime;
        return ops;
    }

    /**
     * Transforms client operation against server operation.  Server operation
     * takes precedence. This means we always transform our operations under the
     * assumption that the server operation is applied first.
     */
    public static Operation[] transformBatch(Operation client, Operation server) {
        Operation[] ops = new Operation[2];
        if (client.type == INSERT && server.type == INSERT) {
            return transformBatchedInserts(client, server);
        }
        if (client.type == DELETE && server.type == DELETE) {
            return transformBatchedDeletes(client, server);
        }
        return ops;
    }

    /**
     * Transforms two insert operations. Returns two operations, one the client
     * should apply and one the server should apply to reach a consistent state.
     */
    private static Operation[] transformBatchedInserts(Operation client, Operation server) {
        Operation[] ops = new Operation[2]; // [c'][s']
        // Transformed operation for client to execute
        Operation forClient = new Operation(server); // s'
        // Transformed operation for server to execute
        Operation forServer = new Operation(client); // c'

        int serverIndex = server.startPos;
        int clientIndex = client.startPos;

        if (serverIndex <= clientIndex) {
            // Transform as if server's insert occurred first
            // Insert from client has insert position shifted by length of
            // server's content inserted
            forServer.startPos += client.content.length();
        }
        else if (clientIndex <= serverIndex) {
            forClient.startPos += server.content.length();
        }
        ops[0] = forClient;
        ops[1] = forServer;
        return ops;
    }


    /**
     * Transforms two delete operations.
     */
    private static Operation[] transformBatchedDeletes(Operation client, Operation server) {
        Operation[] ops = new Operation[2];
        Operation forClient = new Operation(server); // s', client executes
        Operation forServer = new Operation(client); // c', server executes

        int serverRight = server.startPos; // Right index for delete
        int serverLeft = server.finalPos; // Left index for delete
        int serverNumDeleted = serverRight - serverLeft; // Num chars deleted
        int clientRight = client.startPos; // Right index for delete
        int clientLeft = client.finalPos; // Left index for delete
        int clientNumDeleted = clientRight - clientLeft; // Num chars deleted

        // Both server and client deleted the same frame
        if (serverLeft == clientLeft && serverRight == clientRight) {
            forClient = new Operation(Operation.NO_OP);
            forServer = new Operation(Operation.NO_OP);
        }
        // Deletion windows do not intersect
        else if (serverLeft >= clientRight) {
            // xxx[CCC]yyy[SSS]
//            System.err.println("CASE1");
            // Shift deletion indices by number of chars client deleted
            forClient.startPos -= clientNumDeleted;
            forClient.finalPos -= clientNumDeleted;
        }
        else if (clientLeft >= serverRight) {
            // xxx[SSS]yyy[CCC]
//            System.err.println("CASE2");
            // Shift deletion indices by number of chars server deleted
            forServer.startPos -= serverNumDeleted;
            forServer.finalPos -= serverNumDeleted;
        }
        // Deletion window intersects
        else if (serverLeft <= clientLeft) {
            if (serverRight > clientRight) {
                // Server's deletes covers client's delete
//                System.err.println("CASE5");
                int numRight = serverRight - clientRight;
                int numLeft = clientLeft - serverLeft;
                forClient.startPos = client.startPos - clientNumDeleted + numRight;
                forClient.finalPos = client.finalPos - clientNumDeleted + numLeft;
                forServer = new Operation(Operation.NO_OP);
            }
            else {
                // Client on right, Server on left, partial overlap
//                System.err.println("CASE3");
                int overlap = serverRight - clientLeft;
                forClient.startPos -= overlap;
                forServer.startPos -= serverNumDeleted;
                forServer.finalPos -= overlap;
            }
        }
        else if (clientLeft <= serverLeft) {
            if (clientRight > serverRight) {
                // Client's deletes covers server's delete
//                System.err.println("CASE6");
                int numRight = clientRight - serverRight;
                int numLeft = serverLeft - clientLeft;
                forServer.startPos = server.startPos - serverNumDeleted + numRight;
                forServer.finalPos = server.finalPos - serverNumDeleted + numLeft;
                forClient = new Operation(Operation.NO_OP);
            }
            else {
                // Server on right, Client on left, partial overlap
//                System.err.println("CASE4");
                int overlap = clientRight - serverLeft;
                forServer.startPos -= overlap;
                forClient.startPos -= clientNumDeleted;
                forClient.finalPos -= overlap;
            }
        }
        ops[0] = forClient;
        ops[1] = forServer;
        return ops;
    }

    @Override
    /**
     * String representation of an Operation. We call .trim() on the content
     * to eliminate carriage returns / newlines. Not intended to be used to
     * parse instructions to use for applying an Operation to a text file.
     */
    public String toString() {
        String character = content;
        if (character.equals("\r")) {
            character = "RETURN";
        }
        if (character.equals("\n")) {
            character = "NEWLINE";
        }
        if (character.equals("\r\n")) {
            character = "CARRIAGE RETURN";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        if (type == INSERT) {
            sb.append("INS ");
        }
        if (type == DELETE) {
            sb.append("DEL ");
        }
        sb.append(startPos);
        sb.append(":");
        sb.append(finalPos);
        sb.append(" ");
        sb.append(character);
        sb.append(" ");
        sb.append(" (");
        sb.append(content.length());
        sb.append(")");
        sb.append("]");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Operation) {
            Operation other = (Operation) obj;
            return opsGenerated == other.opsGenerated &&
                    opsReceived == other.opsReceived &&
                    type == other.type && startPos == other.startPos &&
                    finalPos == other.finalPos && content.equals(other.content);
        }
        return false;
    }
}
