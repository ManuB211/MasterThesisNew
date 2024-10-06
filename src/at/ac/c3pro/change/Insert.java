package at.ac.c3pro.change;

import java.util.Set;

import org.jbpt.algo.tree.rpst.IRPSTNode;
import org.jbpt.algo.tree.rpst.RPST;
import org.jbpt.graph.DirectedGraph;
import org.jbpt.graph.Fragment;

import at.ac.c3pro.changePropagation.ChangePropagationUtil.ChgOpType;
import at.ac.c3pro.chormodel.Choreography;
import at.ac.c3pro.chormodel.IRole;
import at.ac.c3pro.chormodel.IRpstModel;
import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.chormodel.RpstModel;
import at.ac.c3pro.node.Edge;

import at.ac.c3pro.node.INode;
import at.ac.c3pro.util.FragmentUtil;

enum InsertionType {
    Sequence,
    Parallel
};

public class Insert<E extends Edge<N>, N extends INode> extends ChangeOperation implements IChangeOperation{
	
	public RpstModel<E,N> f;
	public RpstModel<E,N> model;
	public N in;
	public N out;
    public InsertionType how;
	
	public Insert(Fragment<E,N> f, E position, DirectedGraph g){
		//g.addVertices(f);
		//update in with f.in
		//in.setVertices(in.getV1(), f.getGraph().)
        this.how = InsertionType.Sequence;
	}
	
	public Insert(IRPSTNode<E,N> a1, N in, N out){
        this.how = InsertionType.Sequence;
	}
	
	public Insert(RpstModel<E,N> f,
            RpstModel<E,N> model,
            N in,
            N out,
            Choreography c,
            Role currentRole
           ) {	
	super();
	this.f=f;
	this.model=model;
	this.in=in;
	this.out=out;
	this.c=c;
	this.currentRole=currentRole;
    this.how = InsertionType.Sequence;	
	}
	
	public Insert(IRpstModel<E,N> f,
            IRpstModel<E,N> model,
            Set<N> in,
            Set<N> out,
            Choreography c,
            Role currentRole
           ) {	
	super();
	this.f=(RpstModel<E, N>) f;
	this.model=(RpstModel<E, N>) model;
	this.in=in.iterator().next();
	this.out=out.iterator().next();
	this.c=c;
	this.currentRole=currentRole;
    this.how = InsertionType.Sequence;

	}
	
	  public int getNb_nodes(){
	    	if(this.f!=null)
	    		return f.getdigraph().countVertices()-2;
	    	else 
	    		return 0;
	    }
	
	 public String toString() {
	       return "<Insert: f:" + this.f+ " In: "+this.in + " Out: "+ this.out + ">";
	    }
	 
	 public ChgOpType getType(){
		 return ChgOpType.Insert;
	 }
	
	@Override
	public void Propagate() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void Decompose() {
		// TODO Auto-generated method stub
		
	}

    public void changeInsertionType(InsertionType t) {
        this.how = t;
    }

}
