package at.ac.c3pro.chormodel;

import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.INode;
import org.jbpt.algo.tree.rpst.IRPST;
import org.jbpt.algo.tree.rpst.IRPSTNode;
import org.jbpt.graph.abs.IDirectedGraph;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface IRpstModel<E extends Edge<N>, N extends INode> extends IRPST<E, N> {

    IDirectedGraph<E, N> getdigraph();

    E addEdge(MultiDirectedGraph<E, N> g, N s, N t);

    E removeEdge(MultiDirectedGraph<E, N> g, E e);

    void Model2Dot();

    IRPSTNode<E, N> getsmallestFragment(List<IRPSTNode<E, N>> L);

    RpstModel<E, N> delete(IRPSTNode<E, N> fragment);

    List<IRPSTNode<E, N>> getallChildren(IRPSTNode<E, N> rpstNode);

    IRpstModel<E, N> projectionRole(Role role);

    IRpstModel<E, N> projectionRole(Role role, boolean doGraphReduce);

    IRpstModel<E, N> insert(IRPSTNode<E, N> fragment, E position);

    RpstModel<E, N> replace(IRPSTNode<E, N> oldfragment, IRPSTNode<E, N> newfragment);

    N getRealEntry(IRPSTNode<E, N> rpstNode);

    N getRealExit(IRPSTNode<E, N> rpstNode);

    boolean isActivity(N node);

    String getName();

    RpstModel<E, N> reduceGraph(List<IChoreographyNode> xorsWithDirectEdgeToMerge);

    boolean reduceGraph(List<IChoreographyNode> xorsWithDirectEdgeToMerge, IRPSTNode<E, N> fragment);

    // public void toDOT();
    Set<N> getNodes(IRPSTNode<E, N> rpstNode);

    boolean isNodeInLoop(N node);

    boolean LoopContainsNode(IRPSTNode<E, N> loop, N node);

    Set<IRPSTNode<E, N>> getLoopsOfNode(N node);

    IRPSTNode<E, N> getFragmentBoundedBy(final N sourceNode, final N targetNode);

    Set<N> getTransitivePreSet(N n, Role role, HashSet<N> set);

    Set<N> getTransitivePostSet(N n, Role role, HashSet<N> set);

    Set<N> getTransitivePreSetWithGateways(N n, Role role);

    Set<N> getTransitivePostSetWithGateways(N n, Role role);

    Set<N> getInclusiveTransitivePostSetWithGateways(N n, Role role);

    Set<N> getInclusiveTransitivePreSetWithGateways(N n, Role role);

    RpstModel<E, N> delete(N node);

    boolean isEmpty();

    Set<N> getsuccessorsOfNode(N node, Set<N> resultSet);

    Set<N> getpredecessorsOfNode(N node, Set<N> resultSet);

}
