package at.ac.c3pro.chormodel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jbpt.algo.tree.rpst.IRPST;
import org.jbpt.algo.tree.rpst.IRPSTNode;
import org.jbpt.graph.abs.IDirectedGraph;

import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.INode;

public interface IRpstModel<E extends Edge<N>, N extends INode> extends IRPST<E, N> {

	public IDirectedGraph<E, N> getdigraph();

	public E addEdge(MultiDirectedGraph<E, N> g, N s, N t);

	public E removeEdge(MultiDirectedGraph<E, N> g, E e);

	public void Model2Dot();

	public IRPSTNode<E, N> getsmallestFragment(List<IRPSTNode<E, N>> L);

	public RpstModel<E, N> delete(IRPSTNode<E, N> fragment);

	public List<IRPSTNode<E, N>> getallChildren(IRPSTNode<E, N> rpstNode);

	public IRpstModel<E, N> projectionRole(Role role);

	public IRpstModel<E, N> projectionRole(Role role, boolean doGraphReduce);

	public IRpstModel<E, N> insert(IRPSTNode<E, N> fragment, E position);

	public RpstModel<E, N> replace(IRPSTNode<E, N> oldfragment, IRPSTNode<E, N> newfragment);

	public N getRealEntry(IRPSTNode<E, N> rpstNode);

	public N getRealExit(IRPSTNode<E, N> rpstNode);

	public boolean isActivity(N node);

	public String getName();

	public RpstModel<E, N> reduceGraph(List<IChoreographyNode> xorsWithDirectEdgeToMerge);

	public boolean reduceGraph(List<IChoreographyNode> xorsWithDirectEdgeToMerge, IRPSTNode<E, N> fragment);

	// public void toDOT();
	public Set<N> getNodes(IRPSTNode<E, N> rpstNode);

	public boolean isNodeInLoop(N node);

	public boolean LoopContainsNode(IRPSTNode<E, N> loop, N node);

	public Set<IRPSTNode<E, N>> getLoopsOfNode(N node);

	public IRPSTNode<E, N> getFragmentBoundedBy(final N sourceNode, final N targetNode);

	public Set<N> getTransitivePreSet(N n, Role role, HashSet<N> set);

	public Set<N> getTransitivePostSet(N n, Role role, HashSet<N> set);

	public Set<N> getTransitivePreSetWithGateways(N n, Role role);

	public Set<N> getTransitivePostSetWithGateways(N n, Role role);

	public Set<N> getInclusiveTransitivePostSetWithGateways(N n, Role role);

	public Set<N> getInclusiveTransitivePreSetWithGateways(N n, Role role);

	public RpstModel<E, N> delete(N node);

	public boolean isEmpty();

	public Set<N> getsuccessorsOfNode(N node, Set<N> resultSet);

	public Set<N> getpredecessorsOfNode(N node, Set<N> resultSet);

}
