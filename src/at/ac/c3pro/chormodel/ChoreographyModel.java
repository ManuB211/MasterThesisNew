package at.ac.c3pro.chormodel;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jbpt.algo.tree.rpst.IRPSTNode;
import  org.jbpt.algo.tree.rpst.RPST;
import org.jbpt.algo.tree.tctree.TCTree;
import org.jbpt.graph.DirectedEdge;
import org.jbpt.graph.Fragment;
//import org.jbpt.graph.MultiDirectedGraph;
import org.jbpt.graph.abs.AbstractMultiDirectedGraph;
import org.jbpt.graph.abs.IDirectedEdge;
import org.jbpt.graph.abs.IDirectedGraph;
import org.jbpt.hypergraph.abs.Vertex;
import org.jbpt.utils.IOUtils;

import at.ac.c3pro.chormodel.MultiDirectedGraph;
import at.ac.c3pro.node.ChoreographyNode;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.Event;
import at.ac.c3pro.node.Gateway;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.IPublicNode;
import at.ac.c3pro.node.Interaction;
import at.ac.c3pro.node.Node;
import at.ac.c3pro.node.IEdge;


public class ChoreographyModel extends RpstModel<Edge<IChoreographyNode>, IChoreographyNode> implements IChoreographyModel {

    public ChoreographyModel(MultiDirectedGraph<Edge<IChoreographyNode>,IChoreographyNode> g,String name) {
        super(g, name);
    }

    public ChoreographyModel(MultiDirectedGraph<Edge<IChoreographyNode>,IChoreographyNode> g) {
        super(g);
    }

    public boolean isActivity(IChoreographyNode n)  {
        return n instanceof Interaction;
    }

    public ChoreographyModel reloadFromGraph(IDirectedGraph<Edge<IChoreographyNode>,IChoreographyNode> graph) {
        return new ChoreographyModel((MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode>) graph);
    }

    /*  public Edge<IChoreographyNode> addEdge(MultiDirectedGraph<Edge<IChoreographyNode>,IChoreographyNode> g, IChoreographyNode s, IChoreographyNode t) {
        Edge<IChoreographyNode> e =  new Edge<IChoreographyNode>(g,s,t);
        return e;
    }*/
    
    
    public Event getStartEvent(){
    	for(IChoreographyNode pn : this.diGraph.getVertices())
    		if(pn instanceof Event)
    			if ( this.diGraph.getDirectPredecessors(pn).size()==0)
    				return (Event) pn;
    return null ;
    }
    
    public Event getEndEvent(){
    	for(IChoreographyNode pn : this.diGraph.getVertices())
    		if(pn instanceof Event)
    			if ( this.diGraph.getDirectSuccessors(pn).size()==0)
    				return (Event) pn;
    return null ;
    }

}
