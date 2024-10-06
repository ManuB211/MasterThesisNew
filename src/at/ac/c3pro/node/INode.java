package at.ac.c3pro.node;

import java.util.Set;

import org.jbpt.hypergraph.abs.IVertex;

import at.ac.c3pro.chormodel.Role;

public interface INode extends IVertex{
	public boolean hasRole(Role role);
    public Set<Role> getRoles();
    public Message getMessage();
 
}
