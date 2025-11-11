package parser.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class NodeBase {

    private static int cnt = 0;
    public final int id = cnt++;

    protected final List<NodeBase> children = new ArrayList<>();

    protected void add(NodeBase... ns) {
        if (ns!=null)
            for (var n: ns)
                if (n!=null)
                    children.add(n);
    }

    protected void addAll(List<? extends NodeBase> ns) {
        if(ns!=null)
            for(var n:ns)
                if(n!=null)
                    children.add(n);

    }
    public List<NodeBase> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void print(String indent) {
        String t = label(); // tip cvora
        String v = valueLabel(); // pravi naziv
        System.out.println(indent + "[" + id + "] " + t + (v.isEmpty() ? "" : " (" + v + ")"));
        for (var ch : children) ch.print(indent + "  ");
    }

    protected String label() { return getClass().getSimpleName(); }
    protected String valueLabel() { return ""; }

}
