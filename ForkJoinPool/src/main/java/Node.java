// Node.java
import java.util.concurrent.ConcurrentHashMap;

public class Node {
    private final ConcurrentHashMap<String, Node> children;
    private final Node parent;
    private final String value;

    public Node(Node parent, String value) {
        this.parent = parent;
        this.value = value;
        this.children = new ConcurrentHashMap<>();
    }

    public Node addChildIfAbsent(String value) {
        return children.computeIfAbsent(value, v -> new Node(this, v));
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        print(sb, "", true);
        return sb.toString();
    }

    private void print(StringBuilder sb, String prefix, boolean isTail) {
        sb.append(prefix).append(isTail ? "└── " : "├── ").append(value).append('\n');
        var keys = children.keySet();
        int size = keys.size();
        int i = 0;
        for (String key : keys) {
            Node child = children.get(key);
            if (child != null) {
                child.print(sb, prefix + (isTail ? "    " : "│   "), ++i == size);
            }
        }
    }
}