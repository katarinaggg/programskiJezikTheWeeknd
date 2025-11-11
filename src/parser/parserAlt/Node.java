package parser.parserAlt;

import java.util.ArrayList;
import java.util.List;

public class Node {
    private static int counter = 0;
    private final int id;
    private final String type;
    private final String value;
    private final List<Node> children = new ArrayList<>();

    public Node(String type) {
        this(type, null);
    }

    public Node(String type, String value) {
        this.id = counter++;
        this.type = type;
        this.value = value;
    }

    public void addChild(Node child) {
        if (child != null)
            children.add(child);
    }

    public void print(String indent) {
        System.out.println(indent + "[" + id + "] " + type +
                (value != null ? " (" + value + ")" : ""));
        for (Node child : children) {
            child.print(indent + "  ");
        }
    }

    public static int getCounter() {
        return counter;
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public List<Node> getChildren() {
        return children;
    }

    public void print() {
        print("");
    }
}
