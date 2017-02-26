package editor;


import java.io.Serializable;

/**
 * An object to encapsulate information about modifications being made to a text
 * file. Implements Serializable to allow sending through netty Channel.
 */
public class Operation implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int PRINT = -1; // Mostly for debuggin

    public static final int INSERT = 1; // Adding text to the file
    public static final int DELETE = 0; // Removing text from the file

    public int type; // "how" to execute this operation
    public int startPos; // Position where we started editing
    public int finalPos; // Position where we finished editing
    public String content; // Applies only to INSERT ops

    public Operation(int type) {
        this.type = type;
        content = "";
    }

    // TODO: May not need this method if we send the object directly
    /**
     * String representation of an Operation, understood by the Server
     */
    public String stringToSend() {
        StringBuilder sb = new StringBuilder();
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
        sb.append(content);
        sb.append(" ");
        sb.append(content.length());
        return sb.toString();
    }

    @Override
    /**
     * String representation of an Operation. We call .trim() on the content
     * to eliminate carriage returns / newlines. Not intended to be used to
     * parse instructions to use for applying an Operation to a text file.
     */
    public String toString() {
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
        sb.append(content.trim());
        sb.append(" (");
        sb.append(content.length());
        sb.append(")");
        sb.append("]");
        return sb.toString();
    }


}
