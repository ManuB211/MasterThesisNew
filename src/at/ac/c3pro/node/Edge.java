package at.ac.c3pro.node;


import org.jbpt.graph.abs.AbstractDirectedEdge;
import org.jbpt.graph.abs.AbstractMultiDirectedGraph;
import org.jbpt.graph.abs.IDirectedEdge;


public class Edge<N extends INode> extends AbstractDirectedEdge<N> implements IDirectedEdge<N> {

    public Edge(AbstractMultiDirectedGraph<?, N> g, N source, N target) {
        super(g, source, target);
    }

    public String toString() {
        return this.getSource().getName() + "-->" + this.getTarget().getName();
    }
}
