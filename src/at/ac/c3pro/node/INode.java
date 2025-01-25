package at.ac.c3pro.node;

import at.ac.c3pro.chormodel.Role;
import org.jbpt.hypergraph.abs.IVertex;

import java.util.Set;

public interface INode extends IVertex {
    boolean hasRole(Role role);

    Set<Role> getRoles();

    Message getMessage();

}
