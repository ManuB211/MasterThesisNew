package at.ac.c3pro.util;

import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.INode;
import org.jbpt.algo.tree.rpst.IRPSTNode;

import java.util.HashSet;
import java.util.Set;

public class FragmentUtil {

    /**
     * given a RPST node, return all the (graph) nodes' unique ids
     */
    public static <E extends Edge<N>, N extends INode> Set<String> collectNodeIds(IRPSTNode<E, N> node) {
        Set<String> s = new HashSet<String>();
        for (E edge : node.getFragment()) {
            s.add(edge.getSource().getId());
            s.add(edge.getTarget().getId());
        }
        return s;
    }

    /**
     * extract all roles found in a given fragment
     */
    public static <E extends Edge<N>, N extends INode> Set<Role> extractRoles(IRPSTNode<E, N> node) {
        Set<Role> roles = new HashSet<Role>();
        for (E edge : node.getFragment()) {
            roles.addAll(edge.getSource().getRoles());
            roles.addAll(edge.getTarget().getRoles());
        }
        return roles;
    }


}
