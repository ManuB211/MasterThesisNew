package at.ac.c3pro.node;

import java.util.HashSet;
import java.util.Set;

import org.jbpt.hypergraph.abs.Vertex;

import at.ac.c3pro.chormodel.Role;

public abstract class Node extends Vertex implements INode {

	public Node() {
		super();
	}

	public Node(String name) {
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

	/**
	 * Helper function to get the name of the in-transition for CPN
	 */
	public String getNameIn() {
		return this.getName() + "_in";
	}

	/**
	 * Helper function to get the name of the in-transition for CPN
	 */
	public String getNameOut() {
		return this.getName() + "_out";
	}

	public Node clone() {
		return this.clone();
	}
}
