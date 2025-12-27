package semantic;

import java.util.HashMap;
import java.util.Map;

public class Scope {

    private final Scope parent;
    private final Map<String, Symbol> symbols = new HashMap<>();

    public Scope(Scope parent) {
        this.parent = parent;
    }


    public Symbol resolve(String name) {
        Symbol s = symbols.get(name);
        if (s != null) {
            return s;
        }
        if (parent != null) {
            return parent.resolve(name);
        }
        return null;
    }

    public boolean containsLocal(String name) {
        return symbols.containsKey(name);
    }

    public void define(Symbol symbol) {
        symbols.put(symbol.name, symbol);
    }

    public Scope getParent() {
        return parent;
    }
}
