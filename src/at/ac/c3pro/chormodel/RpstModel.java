package at.ac.c3pro.chormodel;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jbpt.algo.tree.rpst.IRPSTNode;
import org.jbpt.algo.tree.rpst.RPST;
import org.jbpt.algo.tree.tctree.TCType;
import org.jbpt.graph.abs.IDirectedGraph;
import org.jbpt.graph.abs.IFragment;
import org.jbpt.utils.IOUtils;

import at.ac.c3pro.ImpactAnalysis.ImpactAnalysisUtil.Pair;
import at.ac.c3pro.node.AndGateway;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.Event;
import at.ac.c3pro.node.Gateway;
import at.ac.c3pro.node.INode;
import at.ac.c3pro.node.Interaction;
import at.ac.c3pro.node.InteractionActivity;
import at.ac.c3pro.node.Message;
import at.ac.c3pro.node.XorGateway;

/**
 * @author walidfdhila - 2013
 *
 * @param <E>
 * @param <N>
 */
/**
 */

public class RpstModel<E extends Edge<N>, N extends INode> extends RPST<E, N> implements IRpstModel<E, N> {

	// used in getFragmentWithSource, getFragmentWithTarget,
	// getFragmentWithSourceOrTarget
	public interface EdgeFilter<E> {
		boolean filter(E edge);
	};

	protected String name;
	public Map<E, E> original2RpstEdge = null;
	public Set<IRPSTNode<E, N>> Loops = new HashSet<IRPSTNode<E, N>>();

	public RpstModel(MultiDirectedGraph<E, N> graph, String name) {
		super(graph);
		this.name = name;
		original2RpstEdge = this.getOriginal2RpstMap(this.diGraph);
		this.IdentifyLoops();
	}

	public RpstModel(MultiDirectedGraph<E, N> graph) {
		super(graph);
		original2RpstEdge = this.getOriginal2RpstMap(this.diGraph);
		this.IdentifyLoops();
	}

	// public RpstModel<E,N> copy() {
	// return new RpstModel((MultiDirectedGraph<E,N>)this.diGraph, this.name);
	// }
	public String getName() {
		return this.name;
	}

	public void setDiGraph(IDirectedGraph<E, N> graph) {
		this.diGraph = graph;
	}

	public IDirectedGraph<E, N> getdigraph() {
		return this.diGraph;
	}

	public E addEdge(MultiDirectedGraph<E, N> g, N s, N t) {
		E e = (E) new Edge<N>(g, s, t);
		return e;
	}

	public E removeEdge(MultiDirectedGraph<E, N> g, E e) {
		return g.removeEdge(e);
	}

	public void Model2Dot() {
		IOUtils.toFile(this.name + "Rpst.dot", this.toDOT());
		IOUtils.toFile(this.name + "Graph.dot", this.diGraph.toDOT());
	}

	/**
	 * common base function for getFragmentWith*, filters out the fragment with the
	 * given node using the boolean function supplied by the callable
	 */
	private IRPSTNode<E, N> getFragmentWith(EdgeFilter<E> f) {
		List<IRPSTNode<E, N>> acc = new LinkedList<IRPSTNode<E, N>>();
		for (IRPSTNode<E, N> node : this.getRPSTNodes()) {
			for (E edge : node.getFragment()) {
				if (f.filter(edge) && node.getFragment().size() == 1) {
					acc.add(node);
				}
			}
		}
		if (acc.size() == 0)
			return null;
		if (acc.size() == 1)
			return acc.get(0);
		return this.getsmallestFragment(acc);
	}

	/**
	 * Retrieve the single fragment that starts from the node. Comparison is based
	 * on the node's unique id.
	 */
	public IRPSTNode<E, N> getFragmentWithSource(final N searchedNode) {
		return this.getFragmentWith(new EdgeFilter<E>() {
			public boolean filter(E edge) {
				return edge.getSource().getName() == searchedNode.getName();
			}
		});
	}

	/**
	 * Retrieve the single fragment that ends with the given node.
	 */
	public IRPSTNode<E, N> getFragmentWithTarget(final N searchedNode) {
		return this.getFragmentWith(new EdgeFilter<E>() {
			public boolean filter(E edge) {
				return edge.getTarget().getName() == searchedNode.getName();
			}
		});
	}

	/**
	 * return the common smallest fragment that holds the node in either source or
	 * target.
	 */
	public IRPSTNode<E, N> getFragmentWithSourceOrTarget(final N searchedNode) {
		return this.getFragmentWith(new EdgeFilter<E>() {
			public boolean filter(E edge) {
				return edge.getSource().getName().equals(searchedNode.getName())
						|| edge.getTarget().getName().equals(searchedNode.getName());
			}
		});
	}

	public IRPSTNode<E, N> getFragmentBoundedBy(final N sourceNode, final N targetNode) {
		return this.getFragmentWith(new EdgeFilter<E>() {
			public boolean filter(E edge) {
				return edge.getSource().getName() == sourceNode.getName()
						|| edge.getTarget().getName() == targetNode.getName();
			}
		});
	}

	/**
	 * return the trivial edge (fragment) which has sourceNode in getSource() and
	 * targetNode in getTarget()
	 */
	public IRPSTNode<E, N> getFragmentTrivialEdge(final N sourceNode, final N targetNode) {
		return this.getFragmentWith(new EdgeFilter<E>() {
			public boolean filter(E edge) {
				return edge.getSource().getName() == sourceNode.getName()
						&& edge.getTarget().getName() == targetNode.getName();
			}
		});
	}

	/**
	 * return the fragment where the activity holds a specific message
	 */
	public IRPSTNode<E, N> getFragmentWithMessageInActivity(final Message m) {
		return this.getFragmentWith(new EdgeFilter<E>() {
			public boolean filter(E edge) {
				return edge.getSource().getMessage() == m || edge.getTarget().getMessage() == m;
			}
		});
	}

	/**
	 * Get all successors of a node
	 */
	public Set<N> getsuccessorsOfNode(N node, Set<N> resultSet) {
		if (!this.getdigraph().contains(node))
			return null;

		for (N n : this.getdigraph().getDirectSuccessors(node)) {
			resultSet.add(n);
			resultSet.addAll(this.getsuccessorsOfNode(n, resultSet));
		}
		return resultSet;
	}

	/**
	 * Get all predecessors of a node
	 */
	public Set<N> getpredecessorsOfNode(N node, Set<N> resultSet) {
		if (!this.getdigraph().contains(node))
			return null;

		for (N n : this.getdigraph().getDirectPredecessors(node)) {
			resultSet.add(n);
			resultSet.addAll(this.getpredecessorsOfNode(n, resultSet));
		}
		return resultSet;
	}

	/**
	 * returns the smallest fragment that encapsulates a list of rpstNodes.
	 */
	public IRPSTNode<E, N> getsmallestFragment(List<IRPSTNode<E, N>> L) {

		if (L.size() == 0)
			return null;
		if (L.size() == 1)
			return L.get(0);
		IRPSTNode<E, N> commonparent = L.get(0);
		for (IRPSTNode<E, N> node : L) {
			commonparent = this.getLCA(commonparent, node);

		}
		return commonparent;

	}

	/**
	 * deleting a singular node, use the the set-based delete
	 */
	public RpstModel<E, N> delete(N node) {
		Set<IRPSTNode<E, N>> s = new HashSet<IRPSTNode<E, N>>();
		s.add(this.getFragmentWithSource(node));
		s.add(this.getFragmentWithTarget(node));
		// System.out.println("s= "+s);
		return this.delete(s);
	}

	public RpstModel<E, N> delete(IRPSTNode<E, N> fragment) {
		return reloadFromGraph(this.innerDelete(fragment, this.cloneDiGraph())).reduceGraph();
	}

	public RpstModel<E, N> delete(Set<IRPSTNode<E, N>> sequenceFragment) {
		return reloadFromGraph(this.innerDelete(sequenceFragment, this.cloneDiGraph())).reduceGraph();
	}

	/**
	 * a fragment in the RPST is determined by its root element in the tree
	 * 
	 * @param <E>
	 * @param <N>
	 */
	public IDirectedGraph<E, N> innerDelete(IRPSTNode<E, N> fragment, IDirectedGraph<E, N> graph) {
		// IDirectedGraph<E, N> graph = this.cloneDiGraph();
		return removefragment(fragment.getFragment(), fragment.getEntry(), fragment.getExit(), graph);
		// return new RpstModel<E,N>((MultiDirectedGraph<E, N>) newDiGraph);
		// return newDiGraph;
	}

	public IDirectedGraph<E, N> innerDelete(Set<IRPSTNode<E, N>> Sequencefragment, IDirectedGraph<E, N> graph) {
		// we first check if the set contains anly trivial nodes and it corresponds to a
		// connected sequence of nodes
		Iterator<IRPSTNode<E, N>> I = Sequencefragment.iterator();
		if (Sequencefragment == null)
			return graph;
		if (Sequencefragment.size() == 1) {
			System.out.println("error: single edge"); // to be replaced by throw exception ...
			return graph;
		}

		IRPSTNode<E, N> parent = this.getParent(I.next());
		for (IRPSTNode<E, N> node : Sequencefragment) {
			if (node.getType() != TCType.TRIVIAL || this.getParent(node) != parent) {
				System.out.println("error: the fragment is not a sequence");
				return graph;
			}
		}

		// Checking if the edges are in sequence and finding the entry and the exit of
		// this sequence
		Map<N, Integer> Occurence = new HashMap<N, Integer>();
		for (IRPSTNode<E, N> node1 : Sequencefragment) {
			int nb1 = 1;
			int nb2 = -1;
			if (!Occurence.containsKey(node1.getEntry())) {
				for (IRPSTNode<E, N> node2 : Sequencefragment) {
					if (node1.getEntry() == node2.getExit())
						nb1--;
				}
				Occurence.put(node1.getEntry(), nb1);
			}

			if (!Occurence.containsKey(node1.getExit())) {
				for (IRPSTNode<E, N> node2 : Sequencefragment) {
					if (node1.getExit() == node2.getEntry())
						nb2++;
				}
				Occurence.put(node1.getExit(), nb2);
			}
		}
		int nbEntry = 0;
		int nbExit = 0;
		N Fentry = null;
		N Fexit = null;
		for (N node : Occurence.keySet()) {
			if (Occurence.get(node) == 1) {
				nbEntry++;
				Fentry = node;
			}
			if (Occurence.get(node) == -1) {
				nbExit++;
				Fexit = node;
			}
		}
		if (nbEntry > 1 || nbExit > 1) {
			System.out.println("error: the fragment is not a connected sequence");
			return graph;
		}
		// At this level it is sure that it is a correct sequence with Fentry and Fexit
		// as boundaries, so we proceed to the delete

		// TODO: extract this sequence checking as a method to be reused wherever we
		// need

		I = Sequencefragment.iterator();
		// IFragment<E,N> Fragment = new Fragment<E,N>(null);
		Set<E> Fragment = new HashSet<E>();
		while (I.hasNext()) {

			Iterator<E> it = I.next().getFragment().iterator();
			while (it.hasNext()) {
				E edge = it.next();
				Fragment.add(edge);
				// System.out.println("edge =="+edge + Sequencefragment.size());
			}
		}

		// IDirectedGraph<E, N> newDiGraph = removefragment(Fragment,Fentry, Fexit,
		// graph);
		// return new RpstModel<E,N>((MultiDirectedGraph<E, N>) newDiGraph);
		// return newDiGraph;

		return removefragment(Fragment, Fentry, Fexit, graph);
	}

	/**
	 * This Function reduces a graph for the delete
	 */
	private IDirectedGraph<E, N> removefragment(Set<E> Fragment, N predecessor, N successor,
			IDirectedGraph<E, N> graph) {
		Iterator<E> i = Fragment.iterator();
		// System.out.println("********"+Fragment);
		List<E> L = new LinkedList<E>();
		while (i.hasNext()) {
			E rpstEdge = i.next(); // rpst edges
			for (E graphEdge : original2RpstEdge.keySet())
				if (this.original2RpstEdge.get(graphEdge).getId().equals(rpstEdge.getId()))
					L.add(graphEdge);
		}

		// System.out.println("L: " + L.size());
		// System.out.println("L.size(): " + L.size());

		// TODO: merge the graph reduction logic in reduceGraph() and here

		if (L.size() > 0) {
			// graph reduction
			if (predecessor instanceof AndGateway && successor instanceof AndGateway) {
				Gateway source = (Gateway) predecessor;
				Gateway target = (Gateway) successor;
				// System.out.println("gateways to reduce "+ source+" "+target);
				if (source.isSplit(this) && target.isJoin(this)) {
					// System.out.println("removed = "+graph.removeEdges(L));
					Collection<E> deleted = graph.removeEdges(L);
					// System.out.println("(XOR) case 1 deleted: " + deleted);
					if (graph.getDirectSuccessors(predecessor).size() == 1
							&& graph.getDirectPredecessors(successor).size() == 1) {
						Iterator<N> It1 = graph.getDirectPredecessors(predecessor).iterator();
						Iterator<N> It2 = graph.getDirectSuccessors(predecessor).iterator();
						Iterator<N> It3 = graph.getDirectPredecessors(successor).iterator();
						Iterator<N> It4 = graph.getDirectSuccessors(successor).iterator();
						if (It1.hasNext() && It2.hasNext())
							graph.addEdge(It1.next(), It2.next());
						if (It3.hasNext() && It4.hasNext())
							graph.addEdge(It3.next(), It4.next());
						graph.removeVertex(predecessor);
						graph.removeVertex(successor);
						// System.out.println("(AND Case) Graph Reduction done !");
					}
				} else {
					Collection<E> deleted = graph.removeEdges(L);
					// System.out.println("(XOR) case 2 deleted: " + deleted);
				}
			} else { // xor, sequence, ..
						// System.out.println("XOR case");
				Collection<E> deleted = graph.removeEdges(L);
				// System.out.println("(XOR) deleted: " + deleted);
				graph.addEdge(predecessor, successor);
			}
		}

		// System.out.println("graph after reduction: " + graph);
		// System.out.println("this.diGraph after reduction: " + this.diGraph);

		graph.removeVertices(graph.getDisconnectedVertices());
		return graph;
	}

	/**
	 * Reduce the graph of the model by identifying fragments where reduction is
	 * possible.
	 */
	public RpstModel<E, N> reduceGraph() {
		RpstModel<E, N> model = this;
		// recursively reduce the graph until there are no more graph reductions
		// possible
		for (IRPSTNode<E, N> e : this.getFragmentsBottomUp()) {
			if (model.reduceGraph(e)) {
				// TODO: at this point we could export to ".dot" to see the reduction progress
				// System.out.println("the reduced graph: "+model.getdigraph());
				return model.reloadFromGraph(model.getdigraph()).reduceGraph();
			}
		}
		// no more graph reductions possible
		return model;
	}

	public RpstModel<E, N> reloadFromGraph(IDirectedGraph<E, N> graph) {
		return new RpstModel((MultiDirectedGraph<E, N>) graph);
	}

	public List<IRPSTNode<E, N>> getFragmentsBottomUp() {
		// System.out.println("#### getFragmentsBottomUp()");
		// perform DFS from the root, then reverse the accumulator
		List<IRPSTNode<E, N>> options = new LinkedList<IRPSTNode<E, N>>();
		options.add(this.getRoot());
		List<IRPSTNode<E, N>> accum = new LinkedList<IRPSTNode<E, N>>();
		// System.out.println("#### getFragmentsBottomUp()");
		return getFragmentsBottomUp(options, accum);
	}

	public List<IRPSTNode<E, N>> getFragmentsBottomUp(List<IRPSTNode<E, N>> options, List<IRPSTNode<E, N>> accum) {
		// System.out.println("options: " + options);
		// System.out.println("acuum: " + accum);
		// when there are no more options, return the reversed accumulator for the
		// correctly ordered list of fragments
		if (options.size() == 0 || options == null) {
			// reversing is not required, because we add(0, child) to the options
			// TODO: we need to test with a more complex graph if this works correctly
			// Collections.reverse(accum);
			return accum;
		}
		IRPSTNode<E, N> next = options.get(0);
		if (next == null)
			return accum;
		options.remove(0);

		if (next.getType() == TCType.BOND && !accum.contains(next)) {
			accum.add(next);
		}
		// get polygon children
		for (IRPSTNode<E, N> child : this.getallChildren(next)) {
			if (child.getType() == TCType.POLYGON || child.getType() == TCType.BOND) {
				options.add(0, child);
			}
		}
		return getFragmentsBottomUp(options, accum);
	}

	public boolean isEmpty() {
		if (this.diGraph.getVertices().size() == 2)
			return true;
		else
			return false;
	}

	/**
	 * Reduce the given fragment specified by the entry and exit node
	 */
	public boolean reduceGraph(IRPSTNode<E, N> fragment) {
		N entry = fragment.getEntry();
		N exit = fragment.getExit();
		// System.out.println(fragment.getLabel());
		// System.out.println("\theight: " + fragment.getHeight());
		// System.out.println("\tentry: " + entry);
		// System.out.println("\texit: " + exit);
		List<IRPSTNode<E, N>> childfragments = this.getPolygonChildren(fragment);
		// System.out.println("child fragments: " + childfragments);
		// System.out.println("child fragment count: " + childfragments.size());
		// System.out.println("parent RPST node: " + this.getParent(fragment));
		// we count the direct connections between entry and exit
		List<E> directEntryExitEdges = new LinkedList<E>();
		for (E edge : fragment.getFragment()) {
			// System.out.println("\t -> edge: " + edge);
			if (entry == edge.getSource() && exit == edge.getTarget()) {
				// System.out.println("\t\t direct link between " + entry + " <-> " + exit);
				directEntryExitEdges.add(edge);
			}
		}
		int directConnection = directEntryExitEdges.size();
		// System.out.println("\tdirect connection count:" + directConnection);

		if (entry instanceof AndGateway && exit instanceof AndGateway) {
			// System.out.println("\tAND case");
			if (directConnection == 2) {
				// AND BOTH case
				// System.out.println("\tAND BOTH case -> reduction possible (remove this bond)
				// and reconnect the relevant nodes");
				// this is exactly the same as XOR BOTH case
				this.removeAndReconnect(entry, exit, directEntryExitEdges);
				// System.out.println("#### RETURN TRUE ####");
				return true;
			} else if (directConnection == 1) {
				// AND EITHER case

				// add edges
				List<Entry<N, N>> edgesToAdd = new LinkedList<Entry<N, N>>();
				// for predecessors of entry to successors of entry where successor != exit
				for (N predecessor : this.diGraph.getDirectPredecessors(entry)) {
					for (N successor : this.diGraph.getDirectSuccessors(entry)) {
						if (successor != exit) {
							edgesToAdd.add(new SimpleImmutableEntry<N, N>(predecessor, successor));
						}
					}
				}
				// System.out.println(""+this.diGraph.getDirectPredecessors(exit));
				// System.out.println("+++++++"+this.diGraph.getDirectSuccessors(exit));
				// for predecessors of exit to successors of exit
				for (N predecessor : this.diGraph.getDirectPredecessors(exit)) {
					for (N successor : this.diGraph.getDirectSuccessors(exit)) {
						if (predecessor != entry) {
							edgesToAdd.add(new SimpleImmutableEntry<N, N>(predecessor, successor));
						}
					}
				}

				// remove all direct edges from entry to exit
				this.diGraph.removeEdges(directEntryExitEdges);

				// remove incoming edges to entry
				this.diGraph.removeEdges(this.diGraph.getIncomingEdges(entry));

				// remove outgoing edges from entry
				this.diGraph.removeEdges(this.diGraph.getOutgoingEdges(entry));

				// remove incoming edges to exit
				this.diGraph.removeEdges(this.diGraph.getIncomingEdges(exit));

				// remove outgoing edges from exit
				this.diGraph.removeEdges(this.diGraph.getOutgoingEdges(exit));

				// System.out.println("Connection ==1");
				// adding edges
				// System.out.println("\t\tEDGES to add: " + edgesToAdd);
				for (Entry<N, N> edgeToAdd : edgesToAdd) {
					N predecessor = edgeToAdd.getKey();
					N successor = edgeToAdd.getValue();
					this.diGraph.addEdge(predecessor, successor);
				}

				this.diGraph.removeVertices(this.diGraph.getDisconnectedVertices());

				return true;
			}

			Gateway source = (Gateway) entry;
			Gateway target = (Gateway) exit;

			if (source.isSplit(this) && target.isJoin(this)) {
				if (directConnection == 2) {
					// System.out.println("\tINSIDE AND BOTH case -> reduction possible (remove this
					// bond) and reconnect the relevant nodes");
				} else if (directConnection == 1) {
					// System.out.println("\tINSIDE AND ONE case -> reduction possible");
				}
			}
		} else if (entry instanceof XorGateway && exit instanceof XorGateway) {
			// XOR BOTH?
			if (directConnection == 2) {
				// System.out.println("\tXOR BOTH case -> reduction possible (remove this bond)
				// and reconnect the relevant nodes");
				// remove this bond, connect the predecessors of entry to successors of exit
				this.removeAndReconnect(entry, exit, directEntryExitEdges);
				// System.out.println("#### RETURN TRUE ####");
				return true;
			} else {
				// System.out.println("\tXOR case (staying intact)");
			}
		} else {
			// sequence ?
			// System.out.println("\tSEQUENCE case");
		}

		// System.out.println("#### RETURN FALSE ####");
		return false;
	}

	private void removeAndReconnect(N entry, N exit, List<E> directEntryExitEdges) {
		// add edges between the predecessors of entry to the successors of exit
		List<Entry<N, N>> edgesToAdd = new LinkedList<Entry<N, N>>();
		for (N predecessor : this.diGraph.getDirectPredecessors(entry)) {
			for (N successor : this.diGraph.getDirectSuccessors(exit)) {
				edgesToAdd.add(new SimpleImmutableEntry<N, N>(predecessor, successor));
			}
		}

		// remove all direct edges from entry to exit
		this.diGraph.removeEdges(directEntryExitEdges);
		// remove incoming edges to entry
		this.diGraph.removeEdges(this.diGraph.getIncomingEdges(entry));
		// remove outgoing edges from exit
		this.diGraph.removeEdges(this.diGraph.getOutgoingEdges(exit));

		// adding edges
		// System.out.println("\t\tEDGES to add: " + edgesToAdd);
		for (Entry<N, N> edgeToAdd : edgesToAdd) {
			N predecessor = edgeToAdd.getKey();
			N successor = edgeToAdd.getValue();
			this.diGraph.addEdge(predecessor, successor);
		}

		this.diGraph.removeVertices(this.diGraph.getDisconnectedVertices());
	}

	/**
	 * returns the list of all rpst nodes of the sub-rpst-tree with the root
	 * element: rpstNode
	 * 
	 * @param <E>
	 * @param <N>
	 */

	public List<IRPSTNode<E, N>> getallChildren(IRPSTNode<E, N> rpstNode) {
		List<IRPSTNode<E, N>> L = new LinkedList<IRPSTNode<E, N>>();
		{
			if (this.getChildren(rpstNode).size() > 0)
				// System.out.println("======="+this.getChildren(rpstNode));
				for (IRPSTNode<E, N> e : this.getChildren(rpstNode)) {
					L.add(e);
					L.addAll(getallChildren(e));
				}
		}

		return L;
	}

	/**
	 * @return This function returns a map between the edges of the original graph
	 *         and and the trivial nodes of the corresponding Rpst. A trivial node
	 *         in the rpst represents an edge.
	 */

	protected Map<E, E> getOriginal2RpstMap(IDirectedGraph<E, N> graph) {
		Map<E, E> M = new HashMap<E, E>();
		// System.out.println("this.diGraph: " + this.diGraph);
		if (graph.countEdges() <= 1)
			return null;

		for (E e : this.getRoot().getFragment())
			for (E e1 : graph.getEdges())
				if (e.getSource().getId() == e1.getSource().getId() && e.getTarget().getId() == e1.getTarget().getId())
					M.put(e1, e);

		// System.out.println("getOriginal2RpstMap M: " + M);
		return M;
	}

	/*
	 * this maps Edge - RpstNode (trivial nodes) public Map<E,IRPSTNode<E,N>>
	 * getOriginal2RpstMap(){ Map<E,IRPSTNode<E,N>> M = new
	 * HashMap<E,IRPSTNode<E,N>>();
	 * 
	 * for(IRPSTNode<E,N> e : this.getRPSTNodes(TCType.TRIVIAL)) for(E e1 :
	 * this.diGraph.getEdges()) if(e.getEntry() == e1.getSource() &&
	 * e.getExit()==e1.getTarget()) M.put(e1,e); return M; }
	 */

	protected void IdentifyLoops() {

		for (IRPSTNode<E, N> node : this.getRPSTNodes(TCType.BOND)) {
			N entry = node.getEntry();
			N exit = node.getExit();
			Set<N> predecessors = new HashSet<N>(this.diGraph.getDirectPredecessors(entry));
			// System.out.println(predecessors);
			Set<N> successors = new HashSet<N>(this.diGraph.getDirectSuccessors(exit));
			// System.out.println(successors);
			// predecessors.retainAll(vertices);

			// Single entry single exit fragments
			if (predecessors.size() == 1 && successors.size() == 1) {
				/// CHOICE
			} else {
				//// LOOP
				Loops.add(node);
			}
		}
	}

	// returns the set of loops (rppstnode root of the loop) that contains the given
	// node
	public Set<IRPSTNode<E, N>> getLoopsOfNode(N node) {
		Set<IRPSTNode<E, N>> loopsSet = new HashSet<IRPSTNode<E, N>>();
		for (IRPSTNode<E, N> n : Loops)
			if (this.getNodes(n).contains(node))
				loopsSet.add(n);

		return loopsSet;
	}

	// returns the set of loops that contains a given rpstNode
	public Set<IRPSTNode<E, N>> getLoopsOfRpstNode(IRPSTNode<E, N> node) {

		return getLoopsOfNode(node.getEntry());
	}

	// checks if a given node is included in a given loop
	public boolean LoopContainsNode(IRPSTNode<E, N> loop, N node) {
		if (this.getNodes(loop).contains(node))
			return true;
		return false;
	}

	// checks if the given node is encapsulated by any loop
	public boolean isNodeInLoop(N node) {

		for (IRPSTNode<E, N> loop : Loops)
			if (this.getNodes(loop).contains(node))
				return true;
		return false;
	}

	// computes the probability of execution of a given fragment in the rpst
	public double ExecutionProbability(IRPSTNode<E, N> fragment, Map<Pair<N, N>, Double> branchingprobabilityMap) {
		double probability = 1;
		IRPSTNode<E, N> currentrpstnode = fragment;
		IRPSTNode<E, N> rpstnodeparent;
		while (!this.isRoot(currentrpstnode)) {
			rpstnodeparent = this.getParent(currentrpstnode);
			if (rpstnodeparent.getType() == TCType.BOND) {
				N currentgraphnode = currentrpstnode.getEntry();
				N graphnodeparent = rpstnodeparent.getEntry();
				for (Pair<N, N> pair : branchingprobabilityMap.keySet())
					if (pair.first == graphnodeparent && pair.second == currentgraphnode) {
						probability = probability * branchingprobabilityMap.get(pair);
					}
			}
			currentrpstnode = rpstnodeparent;
		}

		/*
		 * for(IRPSTNode<E,N> loop : getLoopsOfRpstNode(fragment)){ probability =
		 * probability * branchingprobabilityMap.get(loop.getExit());//to correct }
		 */

		return probability;
	}

	// TODO
	// returns the number of executions of a given activity

	public double ExecutionNumber(N node, Map<Pair<N, N>, Double> branchingprobabilityMap) {
		double nbExec = 0;
		double probability = 1;

		return nbExec;
	}

	// TODO
	// returns the number of execution of a given fragment : average of execution of
	// its nodes
	public double ExecutionNumber(IRPSTNode<E, N> fragment, Map<Pair<N, N>, Double> branchingprobabilityMap) {
		double nbExec = 0;

		for (N node : this.getNodes(fragment)) {
			nbExec += this.ExecutionNumber(node, branchingprobabilityMap);
		}

		nbExec = nbExec / this.getNodes(fragment).size();

		return nbExec;
	}

	public Set<N> getPostSet(IRPSTNode<E, N> n) {
		// find the first element of this fragment
		N lastElement = n.getExit();
		Set<N> finalSet = this.getPostSet(lastElement, new HashSet<N>());
		// System.out.println("PostSet of " + n + ": " + finalSet);
		return finalSet;
	}

	// TODO: possible refactor with getPreSet: difference getDirectPredecessors &
	// getPreSet calls
	public Set<N> getPostSet(N n, HashSet<N> set) {
		for (N next : this.diGraph.getDirectSuccessors(n)) {
			if (this.isActivity(next)) {
				set.add(next);
			}
			this.getPostSet(next, set);
		}
		return set;
	}

	public Set<N> getTransitivePostSet(IRPSTNode<E, N> n, Role role) {
		N lastElement = n.getExit();
		Set<N> finalSet = this.getTransitivePostSet(lastElement, role, new HashSet<N>());
		// System.out.println("Transitive postset of " + n + ": " + finalSet);
		return finalSet;
	}

	// TODO: refactor getPostSet, getPreSet, getTransitivePostSet,
	// getTransitivePreSet to use one common base function
	public Set<N> getTransitivePostSet(N n, Role role, HashSet<N> set) {
		for (N next : this.diGraph.getDirectSuccessors(n)) {
			if (this.isActivity(next) && next.hasRole(role)) {
				set.add(next);
			} else {
				this.getTransitivePostSet(next, role, set);
			}
		}
		return set;
	}

	// TODO: refactor common logic from getTransitivePreSetF (different: call to
	// getTransitivePostSet)
	public IRPSTNode<E, N> getTransitivePostSetF(IRPSTNode<E, N> n, Role role) {
		List<IRPSTNode<E, N>> finalList = new LinkedList<IRPSTNode<E, N>>();
		Set<N> postset = this.getTransitivePostSet(n, role);
		for (N node : postset) {
			IRPSTNode<E, N> x = this.getFragmentWithSourceOrTarget(node);
			finalList.add(x);
		}
		return this.getsmallestFragment(finalList);
	}

	public Set<N> getPreSet(IRPSTNode<E, N> n) {
		N firstElement = n.getEntry();
		Set<N> finalSet = this.getPreSet(firstElement, new HashSet<N>());
		// System.out.println("PreSet of " + n + ": " + finalSet);
		return finalSet;
	}

	public Set<N> getPreSet(N n, HashSet<N> set) {
		for (N next : this.diGraph.getDirectPredecessors(n)) {
			if (this.isActivity(next)) {
				set.add(next);
			}
			this.getPreSet(next, set);
		}
		return set;
	}

	public Set<N> getTransitivePreSet(IRPSTNode<E, N> n, Role role) {
		N firstElement = n.getEntry();
		Set<N> finalSet = this.getTransitivePreSet(firstElement, role, new HashSet<N>());
		return finalSet;
	}

	public Set<N> getTransitivePreSet(N n, Role role, HashSet<N> set) {
		for (N next : this.diGraph.getDirectPredecessors(n)) {
			if (this.isActivity(next) && next.hasRole(role)) {
				set.add(next);
			} else {
				this.getTransitivePreSet(next, role, set);
			}
		}
		return set;
	}

	// returns also gateways as tr-preset
	// excludes n from the t-preset
	public Set<N> getTransitivePreSetWithGateways(N n, Role role) {
		// System.out.println("Projection On Role "+role);
		// System.out.println("Model to be projected "+this.diGraph);
		RpstModel<E, N> projectedmodel = (RpstModel<E, N>) this.projectionRole(role, true);
		// System.out.println("Projection result "+projectedmodel.diGraph);
		if (projectedmodel.diGraph.contains(n))
			return (Set<N>) projectedmodel.diGraph.getDirectPredecessors(n);
		Set<N> set = new HashSet<N>();
		for (N previous : this.diGraph.getDirectPredecessors(n)) {
			if (projectedmodel.diGraph.contains(previous))
				set.add(previous);
			else
				set.addAll(getTransitivePreSetWithGateways(previous, role));
		}
		return set;
	}

	// if the projection includes n then return n itself
	public Set<N> getInclusiveTransitivePreSetWithGateways(N n, Role role) {
		// System.out.println("Projection On Role "+role);
		// System.out.println("Model to be projected "+this.diGraph);
		RpstModel<E, N> projectedmodel = (RpstModel<E, N>) this.projectionRole(role, true);
		// System.out.println("Projection result "+projectedmodel.diGraph);
		Set<N> set = new HashSet<N>();
		if (projectedmodel.diGraph.contains(n)) {
			set.add(n);
			return set;
		}
		for (N previous : this.diGraph.getDirectPredecessors(n)) {
			if (projectedmodel.diGraph.contains(previous))
				set.add(previous);
			else
				set.addAll(getInclusiveTransitivePreSetWithGateways(previous, role));
		}
		return set;
	}

	// includes n in the t-postset
	public Set<N> getInclusiveTransitivePostSetWithGateways(N n, Role role) {
		RpstModel<E, N> projectedmodel = (RpstModel<E, N>) this.projectionRole(role, true);
		Set<N> set = new HashSet<N>();
		if (projectedmodel.diGraph.contains(n)) {
			set.add(n);
			return set;
		}
		for (N next : this.diGraph.getDirectSuccessors(n)) {
			if (projectedmodel.diGraph.contains(next))
				set.add(next);
			else
				set.addAll(getInclusiveTransitivePostSetWithGateways(next, role));
		}
		return set;
	}

	// returns also gateways as tr-postset - excludes n from the t-preset
	public Set<N> getTransitivePostSetWithGateways(N n, Role role) {
		RpstModel<E, N> projectedmodel = (RpstModel<E, N>) this.projectionRole(role, true);
		if (projectedmodel.diGraph.contains(n))
			return (Set<N>) projectedmodel.diGraph.getDirectSuccessors(n);
		Set<N> set = new HashSet<N>();
		for (N next : this.diGraph.getDirectSuccessors(n)) {
			if (projectedmodel.diGraph.contains(next))
				set.add(next);
			else
				set.addAll(getTransitivePreSetWithGateways(next, role));
		}
		return set;
	}

	public IRPSTNode<E, N> getTransitivePreSetF(IRPSTNode<E, N> n, Role role) {
		List<IRPSTNode<E, N>> finalList = new LinkedList<IRPSTNode<E, N>>();
		Set<N> postset = this.getTransitivePreSet(n, role);
		for (N node : postset) {
			IRPSTNode<E, N> x = this.getFragmentWithSourceOrTarget(node);
			finalList.add(x);
		}
		return this.getsmallestFragment(finalList);
	}

	public IRpstModel<E, N> projectionRole(Role role) {
		return this.projectionRole(role, true);
	}

	/**
	 * @return this function projects a given Model on a role. It returns a
	 *         structured model where the nodes are all reltated to this role.
	 */
	public IRpstModel<E, N> projectionRole(Role role, boolean doGraphReduce) {
		IRpstModel<E, N> projectedModel = null;
		MultiDirectedGraph<E, N> graph = new MultiDirectedGraph<E, N>();
		boolean var = false;

		// System.out.println("-- (projectionRole) this.diGraph:" + this.diGraph);
		// System.out.println("-- (projectionRole) this.diGraph.getVertices():" +
		// this.diGraph.getVertices());
		for (N n : this.diGraph.getVertices()) {
			// System.out.println("n type:" + n.getClass());
			// System.out.println("this type:" + this.getClass());
			if ((n instanceof Event) || (n instanceof Gateway)) {
				// System.out.println("-- true: gateway or event");
				var = true;
			} else if ((n instanceof InteractionActivity && ((InteractionActivity) n).role.name.equals(role.name))) {
				// System.out.println("-- true: public model activity");
				var = true;
			} else if ((n instanceof Interaction) && (((Interaction) n).getParticipant1().name.equals(role.name)
					|| ((Interaction) n).getParticipant2().name.equals(role.name))) {
				// System.out.println("-- true: choreography model interaction");
				// System.out.println("n is " + n.getName());
				// System.out.println("n role sender" + ((Interaction) n).getSender());
				// System.out.println("n role receiver" + ((Interaction) n).getReceiver());
				// System.out.println("(matched) n is: " + n);
				var = true;
			}

			if (var == true) {
				var = false;
				for (N successor : this.getnextRoleActivities(n, role)) {
					// System.out.println("-- adding to graph: " + n + " -> " + successor);
					graph.addEdge(n, successor);
				}
			}
		}
		// System.out.println("-- graph to be passed to constructor" + graph);
		projectedModel = new RpstModel<E, N>(graph, this.getName() + "Projection");
		if (doGraphReduce) {
			return projectedModel.reduceGraph();
		} else {
			return projectedModel;
		}
	}

	/**
	 * @return this function returns the list of next nodes of type gateway, Event,
	 *         InteractionActivity or Interactions having the Role role of a given
	 *         node
	 */
	private List<N> getnextRoleActivities(N node, Role role) {
		List<N> L = new LinkedList<N>();

		if (this.diGraph.getDirectSuccessors(node).size() > 0) {
			if ((this instanceof PublicModel || this instanceof PrivateModel)) {
				for (N n : this.diGraph.getDirectSuccessors(node)) {
					if ((n instanceof Event) || (n instanceof Gateway)
							|| (n instanceof InteractionActivity && ((InteractionActivity) n).role.equals(role))) {
						L.add(n);
					} else
						L.addAll(getnextRoleActivities(n, role));
				}
			}

			if (this instanceof ChoreographyModel) {
				for (N n : this.diGraph.getDirectSuccessors(node)) {
					if (n instanceof Interaction && !((Interaction) n).getParticipant1().name.equals(role.name)
							&& !(((Interaction) n).getParticipant2().name.equals(role.name))) {
						L.addAll(getnextRoleActivities(n, role));
					} else
						L.add(n);
				}
			}
		}
		return L;
	}

	protected IDirectedGraph<E, N> cloneDiGraph() {
		// create a new multigraph
		MultiDirectedGraph<E, N> diGraph = new MultiDirectedGraph<E, N>();
		// iterate over all edges copy them over
		for (E edge : this.diGraph.getEdges()) {
			diGraph.addEdge(edge.getSource(), edge.getTarget());
		}

		original2RpstEdge = this.getOriginal2RpstMap(diGraph);
		return diGraph;
	}

	public RpstModel<E, N> insert(IRPSTNode<E, N> fragment, E position) {
		return this.reloadFromGraph(this.innerInsert(fragment, position, this.cloneDiGraph()));
	}

	/**
	 * Insert a fragment between the nodes specified in `position`. The inserted
	 * fragment should be bounded by a start and end events
	 */
	public IDirectedGraph<E, N> innerInsert(IRPSTNode<E, N> fragment, E position, IDirectedGraph<E, N> graph) {
		IFragment<E, N> actualFragment = fragment.getFragment();
		IDirectedGraph<E, N> theGraph = (IDirectedGraph<E, N>) actualFragment.getGraph();

		// we skip (fragment.getEntry()) and use its direct successors as starting nodes
		for (N sourceNode : theGraph.getDirectSuccessors(fragment.getEntry())) {
			graph.addEdge(position.getSource(), sourceNode);
		}

		// we skip (fragment.getExit())) and use its direct predecessors as ending nodes
		for (N targetNode : theGraph.getDirectPredecessors(fragment.getExit())) {
			graph.addEdge(targetNode, position.getTarget());
		}

		graph.removeEdge(position);

		// actually connect up the nodes stored in fragment
		if (fragment.getFragment().getGraph().countEdges() > 2)
			for (E edge : fragment.getFragment()) {
				if (edge.getSource() != fragment.getEntry() && edge.getTarget() != fragment.getExit())
					graph.addEdge(edge.getSource(), edge.getTarget());
			}

		// return new RpstModel((MultiDirectedGraph<E,N>) graph);
		return graph;
	}

	public RpstModel<E, N> replace(IRPSTNode<E, N> oldfragment, IRPSTNode<E, N> newfragment) {
		return this.reloadFromGraph(this.innerReplace(oldfragment, newfragment, this.cloneDiGraph()));
	}

	/**
	 * Replace an existing fragment by a new fragment. The inserted fragment should
	 * be bounded by a start and end events
	 */

	public IDirectedGraph<E, N> innerReplace(IRPSTNode<E, N> oldfragment, IRPSTNode<E, N> newfragment,
			IDirectedGraph<E, N> graph) {

		N oldFsourceNode = oldfragment.getEntry();
		N oldFtargetNode = oldfragment.getExit();

		// Deleting the old fragment (we can not use the delete function since it
		// includes graph reduction that we dont need here)
		Iterator<E> i = oldfragment.getFragment().iterator();
		List<E> L = new LinkedList<E>();

		while (i.hasNext()) {
			E rpstEdge = i.next(); // rpst edges
			for (E graphEdge : original2RpstEdge.keySet())
				if (this.original2RpstEdge.get(graphEdge).getId().equals(rpstEdge.getId()))
					L.add(graphEdge);
		}

		graph.removeEdges(L);
		graph.removeVertices(graph.getDisconnectedVertices());
		graph.addEdge(oldFsourceNode, oldFtargetNode);

		// inserting the new fragment
		return this.innerInsert(newfragment, graph.getEdge(oldFsourceNode, oldFtargetNode), graph);
	}

	/**
	 * Returns the set of nodes of a given fragment
	 */

	public Set<N> getNodes(IRPSTNode<E, N> rpstNode) {
		Set<N> Nodes = new HashSet<N>();
		for (E edge : rpstNode.getFragment()) {
			if (!Nodes.contains(edge.getSource()))
				Nodes.add(edge.getSource());
			if (!Nodes.contains(edge.getTarget()))
				Nodes.add(edge.getTarget());
		}
		return Nodes;
	}

	/**
	 * Returns the the real entry of a fragment
	 */

	public N getRealEntry(IRPSTNode<E, N> rpstNode) {
		N entry = null;
		Collection<N> FragmentNodes = this.getNodes(rpstNode);
		for (N n : this.diGraph.getDirectSuccessors(rpstNode.getEntry()))
			if (FragmentNodes.contains(n))
				entry = n;
		return entry;
	}

	/**
	 * Returns the real exit of a given fragment
	 */

	public N getRealExit(IRPSTNode<E, N> rpstNode) {
		N exit = null;
		Collection<N> FragmentNodes = this.getNodes(rpstNode);
		for (N n : this.diGraph.getDirectPredecessors(rpstNode.getExit()))
			if (FragmentNodes.contains(n))
				exit = n;
		return exit;
	}

	public boolean isActivity(N node) {
		return false; // should be overriden by subclasses: PrivateModel, PublicModel and
						// ChoreographyModel
	}

}
