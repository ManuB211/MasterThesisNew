package at.ac.c3pro.chormodel;

import at.ac.c3pro.node.*;
import org.jbpt.graph.Fragment;
import org.jbpt.graph.abs.IDirectedGraph;

import java.util.LinkedList;
import java.util.List;

public class PrivateModel extends RpstModel<Edge<IPrivateNode>, IPrivateNode> implements IPrivateModel {

    public PrivateModel(MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode> g, String name) {
        super(g, name);
    }

    /**
     * @return this function returns the behavioral Interface of a given
     * PrivateModel Still to be done: graph reduction
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

    public IPublicModel BehavioralInterface2() {
        IPublicModel PuM = null;
        MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> graph = new MultiDirectedGraph<Edge<IPublicNode>, IPublicNode>();
        for (IPrivateNode node : this.diGraph.getVertices()) {

            for (IPrivateNode successor : this.getnextInteractionOrGateway2(node)) {
                try {
                    graph.addEdge((IPublicNode) node, (IPublicNode) successor);
                } catch (ClassCastException e) {
                    try {
                        graph.addEdge(((PrivateActivity) node).getPublicActivity(), (IPublicNode) successor);
                    } catch (ClassCastException e2) {
                        try {
                            graph.addEdge((IPublicNode) node, ((PrivateActivity) successor).getPublicActivity());
                        } catch (ClassCastException e3) {
                            try {
                                graph.addEdge(((PrivateActivity) node).getPublicActivity(), ((PrivateActivity) successor).getPublicActivity());
                            } catch (ClassCastException e4) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

            }

        }
        PuM = new PublicModel(graph, this.getName() + "Interface");
        return PuM;
    }

    /**
     * @return this function returns the list of next nodes of type gateway,
     * InteractionActivity or Event of a given node
     */

    public List<IPrivateNode> getnextInteractionOrGateway2(IPrivateNode node) {
        List<IPrivateNode> L = new LinkedList<IPrivateNode>();
        if (this.diGraph.getDirectSuccessors(node).size() > 0)
            for (IPrivateNode n : this.diGraph.getDirectSuccessors(node)) {
                L.add(n);
            }

        return L;
    }

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
