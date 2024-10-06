package at.ac.c3pro.chormodel;

import org.jbpt.graph.abs.AbstractMultiDirectedGraph;

import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.INode;

public class MultiDirectedGraph<E extends Edge<N>, N extends INode> extends AbstractMultiDirectedGraph<E, N> {
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.hpi.bpt.hypergraph.abs.AbstractMultiDirectedHyperGraph#addEdge(de.hpi.bpt.
	 * hypergraph.abs.IVertex, de.hpi.bpt.hypergraph.abs.IVertex)
	 */
	@Override
	public E addEdge(N s, N t) {
		E e = (E) new Edge<N>(this, s, t);
		return e;
	}
}