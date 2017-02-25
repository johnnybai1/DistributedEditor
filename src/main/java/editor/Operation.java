package editor;


public class Operation {

    public static final int INSERT = 1;
    public static final int DELETE = 0;

    public int type;
    public int startPos; // Position where we started editing
    public int finalPos; // Position where we finished editing
    public String content; // Applies only to INSERT ops

    public Operation(int type) {
        this.type = type;
        content = "";
    }

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
        sb.append(content.length());
        sb.append(" ");
        sb.append(content);
        return sb.toString();
    }

    @Override
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
