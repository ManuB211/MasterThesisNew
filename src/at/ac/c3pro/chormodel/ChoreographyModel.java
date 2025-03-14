package at.ac.c3pro.chormodel;

import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.Interaction;
import org.jbpt.graph.abs.IDirectedGraph;

public class ChoreographyModel extends RpstModel<Edge<IChoreographyNode>, IChoreographyNode>
        implements IChoreographyModel {

    public ChoreographyModel(MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> g) {
        super(g);
    }

    public boolean isActivity(IChoreographyNode n) {
        return n instanceof Interaction;
    }

    public ChoreographyModel reloadFromGraph(IDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> graph) {
        return new ChoreographyModel((MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode>) graph);
    }
}
