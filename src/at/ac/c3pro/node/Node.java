package at.ac.c3pro.node;


import java.util.HashSet;
import java.util.Set;

import org.jbpt.hypergraph.abs.Vertex;

import at.ac.c3pro.chormodel.Role;

public abstract class Node extends Vertex implements INode{
	
	public Node(){
		super();
	}
	
	public Node(String name){
		super(name);
	}
	
	public boolean hasRole(Role role) {
		return false;
	}

    public Set<Role> getRoles() {
        return new HashSet<Role>();
    }
    
    public Message getMessage() {
    	return null;
    }
 
   public Node clone(){
	  return this.clone();
   }
}
