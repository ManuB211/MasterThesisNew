package at.ac.c3pro.chormodel;

import org.jbpt.graph.abs.IDirectedGraph;

import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.Event;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.Interaction;

public class ChoreographyModel extends RpstModel<Edge<IChoreographyNode>, IChoreographyNode>
		implements IChoreographyModel {

	public ChoreographyModel(MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> g, String name) {
		super(g, name);
	}

	public ChoreographyModel(MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> g) {
		super(g);
	}

	public boolean isActivity(IChoreographyNode n) {
		return n instanceof Interaction;
	}

	public ChoreographyModel reloadFromGraph(IDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> graph) {
		return new ChoreographyModel((MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode>) graph);
	}

	/*
	 * public Edge<IChoreographyNode>
	 * addEdge(MultiDirectedGraph<Edge<IChoreographyNode>,IChoreographyNode> g,
	 * IChoreographyNode s, IChoreographyNode t) { Edge<IChoreographyNode> e = new
	 * Edge<IChoreographyNode>(g,s,t); return e; }
	 */

	public Event getStartEvent() {
		for (IChoreographyNode pn : this.diGraph.getVertices())
			if (pn instanceof Event)
				if (this.diGraph.getDirectPredecessors(pn).size() == 0)
					return (Event) pn;
		return null;
	}

	public Event getEndEvent() {
		for (IChoreographyNode pn : this.diGraph.getVertices())
			if (pn instanceof Event)
				if (this.diGraph.getDirectSuccessors(pn).size() == 0)
					return (Event) pn;
		return null;
	}

}
