package at.ac.c3pro.node;

import at.ac.c3pro.chormodel.MultiDirectedGraph;
import at.ac.c3pro.chormodel.RpstModel;

public abstract class Gateway extends Node implements IGateway {

    public Gateway() {
        super();
    }

    public Gateway(String name) {
        super(name);
    }

    public Gateway(String name, String id) {
        super(name);
        this.setId(id);
    }

    public boolean isJoin(RpstModel model) {
        return model != null && model.getdigraph().getDirectPredecessors(this).size() > 1
                && model.getdigraph().getDirectSuccessors(this).size() == 1;
    }

    public boolean isSplit(RpstModel model) {
        return model != null && model.getdigraph().getDirectPredecessors(this).size() == 1
                && model.getdigraph().getDirectSuccessors(this).size() > 1;
    }

    public <E extends Edge<N>, N extends INode> boolean isJoin(MultiDirectedGraph<E, N> digraph) {
        return digraph != null && digraph.getDirectPredecessors((N) this).size() > 1
                && digraph.getDirectSuccessors((N) this).size() == 1;
    }

    public <E extends Edge<N>, N extends INode> boolean isSplit(MultiDirectedGraph<E, N> digraph) {
        return digraph != null && digraph.getDirectPredecessors((N) this).size() == 1
                && digraph.getDirectSuccessors((N) this).size() > 1;
    }

    /*
     * public Gateway clone(){ if(this instanceof XorGateway) return new
     * XorGateway(this.getName()); if(this instanceof AndGateway) return new
     * AndGateway(this.getName()); return null; }
     */
}
