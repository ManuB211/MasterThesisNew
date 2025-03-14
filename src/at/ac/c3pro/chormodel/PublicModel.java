package at.ac.c3pro.chormodel;

import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.IPublicNode;
import at.ac.c3pro.node.InteractionActivity;
import org.jbpt.graph.abs.IDirectedGraph;

public class PublicModel extends RpstModel<Edge<IPublicNode>, IPublicNode> implements IPublicModel {

    public PublicModel(MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> g, String name) {
        super(g, name);
    }

    public boolean isActivity(IPublicNode n) {
        return n instanceof InteractionActivity;
    }

    public PublicModel reloadFromGraph(IDirectedGraph<Edge<IPublicNode>, IPublicNode> graph) {
        return new PublicModel((MultiDirectedGraph<Edge<IPublicNode>, IPublicNode>) graph, this.name);
    }

}
