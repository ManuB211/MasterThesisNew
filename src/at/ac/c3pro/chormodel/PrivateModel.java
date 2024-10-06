package at.ac.c3pro.chormodel;

import java.util.LinkedList;
import java.util.List;

import org.jbpt.graph.Fragment;
import org.jbpt.graph.abs.IDirectedGraph;

import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.Event;
import at.ac.c3pro.node.Gateway;
import at.ac.c3pro.node.IPrivateNode;
import at.ac.c3pro.node.IPublicNode;
import at.ac.c3pro.node.InteractionActivity;
import at.ac.c3pro.node.PrivateActivity;

public class PrivateModel extends RpstModel<Edge<IPrivateNode>, IPrivateNode> implements IPrivateModel {

	public PrivateModel(MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode> g, String name) {
		super(g, name);
	}

	/**
	 * @return this function returns the behavioral Interface of a given
	 *         PrivateModel Still to be done: graph reduction
	 */

	public IPublicModel BehavioralInterface1() {
		IPublicModel PuM = null;
		MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> graph = new MultiDirectedGraph<Edge<IPublicNode>, IPublicNode>();
		for (IPrivateNode node : this.diGraph.getVertices()) {
			if ((node instanceof Event) || (node instanceof Gateway) || (node instanceof InteractionActivity)) {
				for (IPrivateNode successor : this.getnextInteractionOrGateway(node)) {
					graph.addEdge((IPublicNode) node, (IPublicNode) successor);
				}
			}
		}
		PuM = new PublicModel(graph, this.getName() + "Interface");
		return PuM;
	}

	/**
	 * @return this function returns the list of next nodes of type gateway,
	 *         InteractionActivity or Event of a given node
	 */

	public List<IPrivateNode> getnextInteractionOrGateway(IPrivateNode node) {
		List<IPrivateNode> L = new LinkedList<IPrivateNode>();
		if (this.diGraph.getDirectSuccessors(node).size() > 0)
			for (IPrivateNode n : this.diGraph.getDirectSuccessors(node)) {
				if (n instanceof InteractionActivity || n instanceof Gateway || n instanceof Event)
					L.add(n);
				else
					L.addAll(getnextInteractionOrGateway(n));

			}

		return L;
	}

	public boolean isConsistentWith(PublicModel p) {
		// to be implemented
		return true;
	}

	public boolean isActivity(IPrivateNode n) {
		return (n instanceof InteractionActivity || n instanceof PrivateActivity);
	}

	public PrivateModel reloadFromGraph(IDirectedGraph<Edge<IPrivateNode>, IPrivateNode> graph) {
		return new PrivateModel((MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode>) graph, this.name);
	}

	public void change(Fragment f) {
		// TODO Auto-generated method stub
		// if(f.getGraph().equals(prm)){ // or OriginalGraph

		// }

	}

	public Event getStartEvent() {
		for (IPrivateNode pn : this.diGraph.getVertices())
			if (pn instanceof Event)
				if (this.diGraph.getDirectPredecessors(pn).size() == 0)
					return (Event) pn;
		return null;
	}

	public Event getEndEvent() {
		for (IPrivateNode pn : this.diGraph.getVertices())
			if (pn instanceof Event)
				if (this.diGraph.getDirectSuccessors(pn).size() == 0)
					return (Event) pn;
		return null;
	}

}
