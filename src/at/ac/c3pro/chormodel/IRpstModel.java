package at.ac.c3pro.chormodel;

import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.INode;
import org.jbpt.algo.tree.rpst.IRPST;
import org.jbpt.algo.tree.rpst.IRPSTNode;
import org.jbpt.graph.abs.IDirectedGraph;

import java.util.List;
import java.util.Set;

public interface IRpstModel<E extends Edge<N>, N extends INode> extends IRPST<E, N> {

    IDirectedGraph<E, N> getdigraph();

    void setDiGraph(IDirectedGraph<E, N> graph);

    E addEdge(MultiDirectedGraph<E, N> g, N s, N t);

    IRPSTNode<E, N> getsmallestFragment(List<IRPSTNode<E, N>> L);

    RpstModel<E, N> delete(IRPSTNode<E, N> fragment);

    List<IRPSTNode<E, N>> getallChildren(IRPSTNode<E, N> rpstNode);

    IRpstModel<E, N> projectionRole(Role role, boolean doGraphReduce, List<IChoreographyNode> xorsWithDirectConnectionToMerge);

    IRpstModel<E, N> insert(IRPSTNode<E, N> fragment, E position);

    RpstModel<E, N> replace(IRPSTNode<E, N> oldfragment, IRPSTNode<E, N> newfragment);

    boolean isActivity(N node);

    String getName();

    RpstModel<E, N> reduceGraph(List<IChoreographyNode> xorsWithDirectEdgeToMerge);

    RpstModel<E, N> reduceGraph(List<IChoreographyNode> xorsWithDirectEdgeToMerge, String roleName);

    boolean reduceGraph(List<IChoreographyNode> xorsWithDirectEdgeToMerge, IRPSTNode<E, N> fragment);

    Set<N> getNodes(IRPSTNode<E, N> rpstNode);

    RpstModel<E, N> delete(N node);

    boolean isEmpty();

    Set<N> getsuccessorsOfNode(N node, Set<N> resultSet);

    Set<N> getpredecessorsOfNode(N node, Set<N> resultSet);

    List<IChoreographyNode> getAllXORsWithDirectConnectionToMerge();

}
