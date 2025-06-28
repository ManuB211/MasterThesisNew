package at.ac.c3pro.chormodel;

import at.ac.c3pro.node.*;
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

    public PrivateModel convertToPrivateModel() {

        PrivateModel prM = null;

        MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode> graph = new MultiDirectedGraph<>();

        for (IPublicNode node : this.diGraph.getVertices()) {
            if ((node instanceof Event) || (node instanceof Gateway) || (node instanceof InteractionActivity)) {
                graph.addVertex((IPrivateNode) node);

                for (IPublicNode succ : this.diGraph.getDirectSuccessors(node)) {
                    graph.addEdge((IPrivateNode) node, (IPrivateNode) succ);
                }
            }
        }

        prM = new PrivateModel(graph, "privateModelParsedFromPublic");
        return prM;
    }


}
