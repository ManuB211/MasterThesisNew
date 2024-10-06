package at.ac.c3pro.chormodel;

import java.util.Set;

import org.jbpt.algo.tree.rpst.IRPSTNode;
import org.jbpt.graph.abs.IDirectedGraph;

import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.Event;
import at.ac.c3pro.node.IPublicNode;
import at.ac.c3pro.node.InteractionActivity;


public class PublicModel extends RpstModel<Edge<IPublicNode>,IPublicNode> implements IPublicModel{
	
	public PublicModel(MultiDirectedGraph<Edge<IPublicNode>,IPublicNode> g,String name) {
		super(g, name); 
	}
	
	public boolean isCompatibleWith(PublicModel p){
		//to be imlemented
		return true;
	}
	
	public boolean isActivity(IPublicNode n) {
		return n instanceof InteractionActivity;
	}

    public PublicModel reloadFromGraph(IDirectedGraph<Edge<IPublicNode>, IPublicNode> graph) {
        return new PublicModel((MultiDirectedGraph<Edge<IPublicNode>, IPublicNode>) graph, this.name);
    }
    
    public Event getStartEvent(){
    	for(IPublicNode pn : this.diGraph.getVertices())
    		if(pn instanceof Event)
    			if ( this.diGraph.getDirectPredecessors(pn).size()==0)
    				return (Event) pn;
    return null ;
    }
    
    public Event getEndEvent(){
    	for(IPublicNode pn : this.diGraph.getVertices())
    		if(pn instanceof Event)
    			if ( this.diGraph.getDirectSuccessors(pn).size()==0)
    				return (Event) pn;
    return null ;
    }
    
}
