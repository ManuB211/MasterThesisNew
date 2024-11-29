package at.ac.c3pro.util;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.jbpt.algo.tree.tctree.TCType;
import org.jbpt.graph.Fragment;
import org.jbpt.graph.abs.IDirectedGraph;
import org.jbpt.graph.abs.IFragment;

import at.ac.c3pro.chormodel.ChoreographyModel;
import at.ac.c3pro.chormodel.IRpstModel;
import at.ac.c3pro.chormodel.MultiDirectedGraph;
import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.chormodel.RpstModel;
import at.ac.c3pro.node.AndGateway;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.Event;
import at.ac.c3pro.node.Gateway;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.INode;
import at.ac.c3pro.node.Interaction;
import at.ac.c3pro.node.InteractionActivity;
import at.ac.c3pro.node.Message;
import at.ac.c3pro.node.XorGateway;

/*import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apromore.common.Constants;
import org.apromore.dao.model.FragmentVersion;
import org.apromore.dao.model.FragmentVersionDag;
import org.apromore.exception.PocketMappingException;
import org.apromore.cpf;
import org.apromore.graph.JBPT.CpfAndGateway;
import org.apromore.graph.JBPT.CpfEvent;
import org.apromore.graph.JBPT.CpfGateway;
import org.apromore.graph.JBPT.CpfNode;
import org.apromore.graph.JBPT.CpfOrGateway;
import org.apromore.graph.JBPT.CpfTask;
import org.apromore.graph.JBPT.CpfXorGateway;
import org.apromore.graph.JBPT.ICpfNode;
import org.jbpt.graph.abs.AbstractDirectedEdge;
import org.jbpt.algo.tree.rpst.RPST;
import org.jbpt.algo.tree.rpst.RPSTNode;
import org.jbpt.algo.tree.tctree.TCType;
import org.jbpt.hypergraph.abs.IVertex;
import org.jbpt.pm.FlowNode;
import org.jbpt.pm.IFlowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;*/

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

	public static <E extends Edge<N>, N extends INode> HashMap<String, N> collectNodeIdMap(IRPSTNode<E, N> node) {
		HashMap<String, N> uuid2N = new HashMap<String, N>();
		for (E edge : node.getFragment()) {
			N source = edge.getSource();
			N target = edge.getTarget();
			uuid2N.put(source.getId(), source);
			uuid2N.put(target.getId(), target);
		}
		return uuid2N;
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

	/**
	 * extract all roles that are stored in all the fragments in nodes.
	 */
	public static <E extends Edge<N>, N extends INode> Set<Role> extractRoles(List<IRPSTNode<E, N>> nodes) {
		Set<Role> roles = new HashSet<Role>();
		for (IRPSTNode<E, N> node : nodes) {
			roles.addAll(FragmentUtil.extractRoles(node));
		}
		return roles;
	}

	/**
	 * return the corresponding global fragment of another (public) fragment
	 */
	// TODO: use "collaboration.getMapPu2ChoreoNode(g)"
	public static <E extends Edge<N>, N extends INode> IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode> getCorrespondingGlobalFragment(
			IRPSTNode<E, N> f, ChoreographyModel g) {
		Set<Message> f1Messages = FragmentUtil.collectMessages(f);
		List<IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode>> f_G_list = new LinkedList<IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode>>();
		for (Message m : f1Messages) {
			f_G_list.add(g.getFragmentWithMessageInActivity(m));
		}
		return g.getsmallestFragment(f_G_list);
	}

	/**
	 * return the corresponding local fragment of the global fragment
	 */
	// TODO: this is exactly the same logic as getCorrespondingGlobalFragment but
	// with different types set....refactor
	public static <E extends Edge<N>, N extends INode> IRPSTNode<E, N> getCorrespondingLocalFragment(
			IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode> f, RpstModel<E, N> localModel) {
		Set<Message> messages = FragmentUtil.collectMessages(f);
		List<IRPSTNode<E, N>> f_list = new LinkedList<IRPSTNode<E, N>>();
		for (Message m : messages) {
			IRPSTNode<E, N> f_t = localModel.getFragmentWithMessageInActivity(m);
			if (f_t != null && !f_list.contains(f_t)) {
				f_list.add(f_t);
			}
		}
		return localModel.getsmallestFragment(f_list);
	}

	/**
	 * collect all messages stored in the interactions/activities of a fragment
	 */
	public static <E extends Edge<N>, N extends INode> Set<Message> collectMessages(IRPSTNode<E, N> f) {
		Set<Message> messages = new HashSet<Message>();
		for (E edge : f.getFragment()) {
			messages.add(edge.getSource().getMessage());
			messages.add(edge.getTarget().getMessage());
		}
		// TODO: i'm pretty sure there are null messages here too......should we delete
		// them?
		messages.remove(null);
		return messages;
	}

	/**
	 * returns a set of activities, which are associated with the role inside a
	 * fragment
	 */
	public static <E extends Edge<N>, N extends INode> Set<N> projectRoleOld(IRPSTNode<E, N> f, Role r) {
		Set<N> result = new HashSet<N>();
		N start = f.getEntry();
		N end = f.getExit();
		for (E edge : f.getFragment()) {
			if (edge.getSource().getRoles().contains(r) && start != edge.getSource()) {
				result.add(edge.getSource());
			}
			if (edge.getTarget().getRoles().contains(r) && end != edge.getTarget()) {
				result.add(edge.getTarget());
			}
		}
		return result;
	}

	public static <E extends Edge<N>, N extends INode> IRpstModel<E, N> projectRole(IRPSTNode<E, N> f, Role r,
			IDirectedGraph<E, N> g, boolean doGraphReduce) {
		N start = f.getEntry();
		N end = f.getExit();

		// System.out.println("FragmentUtil.projectRole on Fragment:" + f + " role:" + r
		// + " graph:" + g + " doGraphReduce:" + doGraphReduce);

		IRpstModel<E, N> projectedModel = null;
		MultiDirectedGraph<E, N> graph = new MultiDirectedGraph<E, N>();
		boolean sourceAdd = false;

		Map<N, Set<N>> edgeMapSet = new HashMap<N, Set<N>>();

		// TODO: I might have to add to a set first, from which we build the graph
		for (E edge : f.getFragment()) {
			N source = edge.getSource();
			N target = edge.getTarget();

			for (Object x : new LinkedList(Arrays.asList(edge.getSource(), edge.getTarget()))) {
				N n = (N) x;

				if (!edgeMapSet.containsKey(n)) {
					edgeMapSet.put(n, new HashSet<N>());
				}

				Set<N> edgeTargets = (Set<N>) edgeMapSet.get(n);

				if ((n instanceof Event) || (n instanceof Gateway)) {
					sourceAdd = true;
				} else if ((n instanceof InteractionActivity) && ((InteractionActivity) n).role.equals(r)) {
					sourceAdd = true;
				} else if (n instanceof Interaction && (((Interaction) n).getParticipant1().equals(r)
						|| ((Interaction) n).getParticipant2().equals(r))) {
					sourceAdd = true;
				}

				if (sourceAdd) {
					sourceAdd = false;
					for (N successor : FragmentUtil.getNextRoleActivities(n, r, g, end)) {
						// System.out.println("(FragmentUtil.projectRole) Adding edge to graph: " + n +
						// " <-> " + successor);
						// graph.addEdge(n, successor);
						edgeTargets.add(successor);
					}
				}
			}
		}

		// actually build the edges from edgeMapSet
		for (Entry<N, Set<N>> e : edgeMapSet.entrySet()) {
			N source = e.getKey();
			int size = e.getValue().size();
			for (N target : e.getValue()) {
				MultiDirectedGraph<E, N> g2 = (MultiDirectedGraph<E, N>) g;
				if (source instanceof XorGateway && target instanceof XorGateway && size == 1) {
					if (((XorGateway) source).isSplit(g2) && ((XorGateway) target).isJoin(g2)) {
						// if (!(((XorGateway)source).isJoin(g2) && ((XorGateway)target).isJoin(g2)) &&
						// !(((XorGateway)source).isSplit(g2) && ((XorGateway)target).isSplit(g2))) {
						graph.addEdge(source, target);
					}
				} else if (source instanceof AndGateway && target instanceof AndGateway && size == 1) {
					if (((AndGateway) source).isSplit(g2) && ((AndGateway) target).isJoin(g2)) {
						graph.addEdge(source, target);
					}
				}
				graph.addEdge(source, target);
			}
		}

		// System.out.println("(FragmentUtil.projectRole) building RpstModel using
		// graph: " + graph);

		// sometimes the graph is empty; this occurs when the projection returns
		// nothing.
		// Also sometimes there is only one edge (start <-> end)
		if (graph.getEdges().size() == 0) {
			return null;
		} else if (graph.getEdges().size() == 1) {
			// TODO: need to check that these two nodes are indeed Events
			return null;
		}
		// if(graph.getVertices().contains(o))
		Event e1 = new Event("start");
		Event e2 = new Event("end");
		if (!FragmentUtil.hasStartEvent(graph))
			graph.addEdge((N) e1, start);
		if (!FragmentUtil.hasEndEvent(graph))
			graph.addEdge(end, (N) e2);
		projectedModel = new RpstModel<E, N>(graph, "fragment projection");
		if (doGraphReduce) {
			// System.out.println(projectedModel.getdigraph());
			return projectedModel.reduceGraph(new ArrayList<>());
		} else {
			return projectedModel;
		}
	}

	public static <E extends Edge<N>, N extends INode> List<N> getNextRoleActivities(N node, Role r, IDirectedGraph g,
			N endNode) {
		List<N> L = new LinkedList<N>();

		if (endNode == node)
			return L;

		if (g.getDirectSuccessors(node).size() > 0) {
			for (Object x : g.getDirectSuccessors(node)) {
				N n = (N) x;
				if ((n instanceof Event) || (n instanceof Gateway)) {
					L.add(n);
				} else if ((n instanceof InteractionActivity) && ((InteractionActivity) n).role.equals(r)) {
					L.add(n);
				} else if (n instanceof Interaction && (((Interaction) n).getParticipant1().equals(r)
						|| ((Interaction) n).getParticipant2().equals(r))) {
					L.add(n);
				} else {
					L.addAll(FragmentUtil.getNextRoleActivities(n, r, g, endNode));
				}
			}
		}

		return L;
	}

	public static <E extends Edge<N>, N extends INode> Set<N> collectNodes(IRPSTNode<E, N> f) {
		Set<N> result = new HashSet<N>();
		for (E edge : f.getFragment()) {
			result.add(edge.getSource());
			result.add(edge.getTarget());
		}
		return result;
	}

	public static <E extends Edge<N>, N extends INode> Set<N> collectGateways(IRPSTNode<E, N> f) {
		Set<N> result = new HashSet<N>();
		for (E edge : f.getFragment()) {
			if (edge.getSource() instanceof Gateway)
				result.add(edge.getSource());
			if (edge.getTarget() instanceof Gateway)
				result.add(edge.getTarget());
		}
		return result;
	}

	public static <E extends Edge<N>, N extends INode> Set<N> collectXORGateways(IRPSTNode<E, N> f) {
		Set<N> result = new HashSet<N>();
		for (E edge : f.getFragment()) {
			if (edge.getSource() instanceof XorGateway)
				result.add(edge.getSource());
			if (edge.getTarget() instanceof XorGateway)
				result.add(edge.getTarget());
		}
		return result;
	}

	public static <E extends Edge<N>, N extends INode> Set<N> collectANDGateways(IRPSTNode<E, N> f) {
		Set<N> result = new HashSet<N>();
		for (E edge : f.getFragment()) {
			if (edge.getSource() instanceof AndGateway)
				result.add(edge.getSource());
			if (edge.getTarget() instanceof AndGateway)
				result.add(edge.getTarget());
		}
		return result;
	}

	public static <E extends Edge<N>, N extends INode> Set<N> collectActivities(IRPSTNode<E, N> f) {
		Set<N> result = new HashSet<N>();
		for (E edge : f.getFragment()) {
			if (!(edge.getSource() instanceof Gateway) && !(edge.getSource() instanceof Event))
				result.add(edge.getSource());
			if (!(edge.getTarget() instanceof Gateway) && !(edge.getTarget() instanceof Event))
				result.add(edge.getTarget());
		}
		return result;
	}

	public static <E extends Edge<N>, N extends INode> boolean fragmentIsEqual(IRPSTNode<E, N> f1, IRPSTNode<E, N> f2) {
		if (f1 == null && f2 == null) {
			// both are null fragments
			return true;
		} else if (f1 == null || f2 == null) {
			// we are comparing a valid fragment agains a null: not equal
			return false;
		}

		MultiDirectedGraph<E, N> g1 = (MultiDirectedGraph<E, N>) f1.getFragment().getGraph();
		MultiDirectedGraph<E, N> g2 = (MultiDirectedGraph<E, N>) f2.getFragment().getGraph();
		N start1 = f1.getEntry();
		N start2 = f2.getEntry();
		return FragmentUtil.matchingPaths(g1, start1, g2, start2);
	}

	public static <E extends Edge<N>, N extends INode> boolean matchingPaths(MultiDirectedGraph<E, N> g1, N n1,
			MultiDirectedGraph<E, N> g2, N n2) {

		// System.out.println("---- matchingPaths:");
		// System.out.println("n1: " + n1 + " " + n1.getId());
		// System.out.println("n2: " + n2 + " " + n2.getId());

		// if we are comparing activity nodes, use their ids
		if (!(n1 instanceof Gateway) && !(n2 instanceof Gateway)) {
			if (n1.getId() == n2.getId()) {
				System.out.println("n1.getId() == n2.getId(): n1.getId() =" + n1.getId());
				return n1.getId() == n2.getId();
			} else { // short circuit if not equal
				// System.out.println("### (short circuit) Activity Ids don't match");
				return false;
			}
		}

		// TODO: are these ordered correctly?
		List<N> children1 = (List<N>) g1.getDirectSuccessors(n1);
		List<N> children2 = (List<N>) g2.getDirectSuccessors(n2);
		// System.out.println("children1.size(): " + children1.size());
		// System.out.println("children2.size(): " + children2.size());
		/*
		 * for (N child1 : children1) { System.out.println("child1: " + child1); }
		 * 
		 * for (N child2 : children2) { System.out.println("child2: " + child2); }
		 */

		// Break off with children size criteria? If the number of children are
		// different the graph is obviously different.
		if (children1.size() != children2.size()) {
			// System.out.println("### (short circuit) Size of childrens don't match: " +
			// children1.size() + " vs " + children2.size());
			return false;
		}

		// the number of children are the same at this point

		// check XOR gateway, AND gateway and activity counts
		List<N> XORgateways1 = new LinkedList<N>();
		List<N> XORgateways2 = new LinkedList<N>();
		List<N> ANDgateways1 = new LinkedList<N>();
		List<N> ANDgateways2 = new LinkedList<N>();
		List<N> activities1 = new LinkedList<N>();
		List<N> activities2 = new LinkedList<N>();

		for (N x : children1) {
			if (x instanceof XorGateway) {
				XORgateways1.add(x);
			} else if (x instanceof AndGateway) {
				ANDgateways1.add(x);
			} else {
				activities1.add(x);
			}
		}

		for (N x : children2) {
			if (x instanceof XorGateway) {
				XORgateways2.add(x);
			} else if (x instanceof AndGateway) {
				ANDgateways2.add(x);
			} else {
				activities2.add(x);
			}
		}

		/*
		 * System.out.println("XORgateways1: " + XORgateways1 + " " +
		 * XORgateways1.size()); System.out.println("XORgateways2: " + XORgateways2 +
		 * " " + XORgateways2.size()); System.out.println("ANDgateways1: " +
		 * ANDgateways1 + " " + ANDgateways1.size());
		 * System.out.println("ANDgateways2: " + ANDgateways2 + " " +
		 * ANDgateways2.size()); System.out.println("activities1: " + activities1 + " "
		 * + activities1.size()); System.out.println("activities2: " + activities2 + " "
		 * + activities2.size());
		 */
		// if the node type counts are different, we can break off
		if (XORgateways1.size() != XORgateways2.size() && ANDgateways1.size() != ANDgateways2.size()
				&& activities1.size() != activities2.size()) {
			// System.out.println("### (short circuit) Size of children types don't
			// match.");
			return false;
		}

		// The number of nodes and their type counts are equal.
		// Are the

		// if any of the XOR gateway comparisons fail, so do we
		boolean anyXORMatch = (XORgateways1.size() == 0 && XORgateways2.size() == 0) ? true : false;
		for (N x1 : XORgateways1) {
			for (N x2 : XORgateways2) {
				anyXORMatch |= matchingPaths(g1, x1, g2, x2);
			}
		}
		if (anyXORMatch == false) {
			// System.out.println("### (short circuit) Non of the XOR children match");
			return false;
		}

		// if any of the AND gateway comparisons fail, so do we
		boolean anyANDMatch = (ANDgateways1.size() == 0 && ANDgateways2.size() == 0) ? true : false;
		for (N x1 : ANDgateways1) {
			for (N x2 : ANDgateways2) {
				anyANDMatch |= matchingPaths(g1, x1, g2, x2);
			}
		}
		if (anyANDMatch == false) {
			// System.out.println("### (short circuit) Non of the AND children match");
			return false;
		}

		// if all the activitiy comparisons fail, so do we
		Set<String> activityIds1 = new HashSet<String>();
		Set<String> activityIds2 = new HashSet<String>();

		for (N x1 : activities1) {
			activityIds1.add(x1.getId());
		}
		for (N x2 : activities2) {
			activityIds2.add(x2.getId());
		}

		// System.out.println("activityIds1:" + activityIds1);
		// System.out.println("activityIds2:" + activityIds2);

		if (!activityIds1.equals(activityIds2)) {
			// System.out.println("### (short circuit) ACTIVITY children don't match");
			return false;
		}

		return true;
	}

	/*
	 * public static <E extends Edge<N>,N extends INode> RpstModel<E,N>
	 * generateModelFromFragment(IRPSTNode<E,N> f){
	 * 
	 * MultiDirectedGraph<E, N> copyFragmentGraph = new MultiDirectedGraph<E, N>();
	 * Event e1 = new Event("start"); Event e2 = new Event("end"); Map<N,N>
	 * node2nodecopy = new HashMap<N,N>(); // Map between original nodes and cloned
	 * nodes
	 * 
	 * }
	 */

	public static <E extends Edge<N>, N extends INode> boolean hasStartEvent(MultiDirectedGraph<E, N> f) {
		for (E edge : f.getEdges())
			if (edge.getSource() instanceof Event)
				return true;
		return false;
	}

	public static <E extends Edge<N>, N extends INode> boolean hasEndEvent(MultiDirectedGraph<E, N> f) {
		for (E edge : f.getEdges())
			if (edge.getTarget() instanceof Event)
				return true;
		return false;
	}

	/**
	 * This function generates an rpst model from a fragment. It clones the elements
	 * of a fragment, generates the graph and encapsulates it with start and end
	 * events. This function is used to generate correct models to test insert or
	 * replace change operations.
	 */
	public static <E extends Edge<N>, N extends INode> RpstModel<E, N> generateCloneModelFromFragment(IRPSTNode<E, N> f,
			RpstModel<E, N> m) {

		MultiDirectedGraph<E, N> copyFragmentGraph = new MultiDirectedGraph<E, N>();
		Event e1 = new Event("start");
		Event e2 = new Event("end");
		Map<N, N> node2nodecopy = new HashMap<N, N>(); // Map between original nodes and cloned nodes

		// cloning nodes
		for (N node : FragmentUtil.collectNodes(f)) {
			// we have to maintin the original id to make every comparison work (on nodes)
			N clonedNode = (N) node.clone();
			clonedNode.setId(node.getId());
			node2nodecopy.put(node, clonedNode);
		}

		// creating the new fragment (clone)
		boolean containsStartEvent = false;
		boolean containsEndEvent = false;

		for (E edge : f.getFragment()) {
			copyFragmentGraph.addEdge(node2nodecopy.get(edge.getSource()), node2nodecopy.get(edge.getTarget()));
			if (edge.getSource() instanceof Event)
				containsStartEvent = true;
			if (edge.getTarget() instanceof Event)
				containsEndEvent = true;
		}

		// add the start and end events
		if (containsStartEvent == false) {
			if (f.getType().equals(TCType.POLYGON)) {
				Collection<N> set = m.getdigraph().getDirectSuccessors(f.getEntry());
				for (N n : set) {
					if (n != f.getExit() && node2nodecopy.containsKey(n)) {
						copyFragmentGraph.addEdge((N) e1, node2nodecopy.get(n));
						copyFragmentGraph.removeVertex(node2nodecopy.get(f.getEntry()));
						break;
					}
				}
			} else
				copyFragmentGraph.addEdge((N) e1, node2nodecopy.get(f.getEntry()));
		}
		if (containsEndEvent == false) {
			if (f.getType().equals(TCType.POLYGON)) {
				Collection<N> set = m.getdigraph().getDirectPredecessors(f.getExit());
				for (N n : set) {
					if (n != f.getEntry() && node2nodecopy.containsKey(n)) {
						copyFragmentGraph.addEdge(node2nodecopy.get(n), (N) e2);
						copyFragmentGraph.removeVertex(node2nodecopy.get(f.getExit()));
						break;
					}
				}
			} else
				copyFragmentGraph.addEdge(node2nodecopy.get(f.getExit()), (N) e2);
		}
		RpstModel<E, N> model_copy = new RpstModel<E, N>(copyFragmentGraph);
		return model_copy;
	}

	public static <E extends Edge<N>, N extends INode> RpstModel<E, N> generateCloneModelFromNode(N node) {

		if (node instanceof Event || node instanceof Gateway)
			return null;
		MultiDirectedGraph<E, N> copyFragmentGraph = new MultiDirectedGraph<E, N>();
		Event e1 = new Event("start");
		Event e2 = new Event("end");
		// maintain id and clone
		N nodeClone = (N) node.clone();
		nodeClone.setId(node.getId());
		copyFragmentGraph.addEdge((N) e1, nodeClone);
		copyFragmentGraph.addEdge(nodeClone, (N) e2);

		return new RpstModel<E, N>(copyFragmentGraph);
	}

	/**
	 * Returns the set of models corresponding to all the fragments of a given model
	 * (by adding end and start events)
	 * 
	 * @param m
	 * @return
	 */
	public static <E extends Edge<N>, N extends INode> Set<RpstModel<E, N>> getAllCloneFragmentModels(
			RpstModel<E, N> m) {
		Set<RpstModel<E, N>> modelsSet = new HashSet<RpstModel<E, N>>();
		for (IRPSTNode<E, N> node : m.getRPSTNodes(TCType.BOND))
			modelsSet.add(generateCloneModelFromFragment(node, m));
		for (IRPSTNode<E, N> node : m.getRPSTNodes(TCType.POLYGON))
			modelsSet.add(generateCloneModelFromFragment(node, m));
		for (N node : FragmentUtil.collectActivities(m.getRoot())) {
			modelsSet.add((RpstModel<E, N>) generateCloneModelFromNode(node));
		}

		return modelsSet;
	}

	/**
	 * This Function computes the complement of a given fragment in the current
	 * model
	 * 
	 * @param IFragment<E,N> fragment is the fragment to complement
	 * @param Role           role is the role of the current Model
	 */
	public static <E extends Edge<N>, N extends INode> IFragment<E, N> complement(IRPSTNode<E, N> fragment, Role role) {
		MultiDirectedGraph<E, N> graph = new MultiDirectedGraph<E, N>();
		Iterator<E> I = fragment.getFragment().iterator();
		E edge = null;
		Map<N, N> Node2Complement = new HashMap<N, N>();

		while (I.hasNext()) {
			edge = I.next();
			N source = edge.getSource();
			N target = edge.getTarget();
			// System.out.println("Complement N. Source: " + source + " Target: " + target);

			if (source instanceof InteractionActivity) {
				if (Node2Complement.containsKey(source))
					source = Node2Complement.get(source);
				else {
					InteractionActivity aux1 = (InteractionActivity) source;
					// System.out.println("Source is InteractionActivity" + aux1);
					source = (N) aux1.Complement(role);
					Node2Complement.put(edge.getSource(), source);
				}
			}

			if (target instanceof InteractionActivity) {
				if (Node2Complement.containsKey(target))
					target = Node2Complement.get(target);
				else {
					InteractionActivity aux2 = (InteractionActivity) target;
					// System.out.println("Target is InteractionActivity" + aux2);
					target = (N) aux2.Complement(role);
					Node2Complement.put(edge.getTarget(), target);
				}
			}

			// System.out.println("Adding Edge: " + source + " <-> " + target);
			graph.addEdge(source, target);
		}
		// System.out.println("Final graph: " + graph);
		IFragment<E, N> complement_fragment = new Fragment<E, N>(graph);
		// System.out.println("Final fragment: " + complement_fragment);
		return complement_fragment;
	}

	/*
	 * private static final Logger LOGGER =
	 * LoggerFactory.getLogger(FragmentUtil.class);
	 * 
	 * public static String getFragmentType(final RPSTNode f) { String nodeType =
	 * "UNKNOWN"; if (TCType.P.equals(f.getType())) { nodeType = "P"; } else if
	 * (TCType.B.equals(f.getType())) { nodeType = "B"; } else if
	 * (TCType.R.equals(f.getType())) { nodeType = "R"; } else if
	 * (TCType.T.equals(f.getType())) { nodeType = "T"; } return nodeType; }
	 * 
	 * @SuppressWarnings("unchecked") public static void removeEdges(final RPSTNode
	 * f, final RPSTNode cf) { Collection<AbstractDirectedEdge> fEdges =
	 * f.getFragmentEdges(); Collection<AbstractDirectedEdge> cfEdges =
	 * cf.getFragmentEdges();
	 * 
	 * for (AbstractDirectedEdge fe : fEdges) { for (AbstractDirectedEdge cfe :
	 * cfEdges) { if (fe.getSource().getId().equals(cfe.getSource().getId()) &&
	 * fe.getTarget().getId().equals(cfe.getTarget().getId())) {
	 * f.getFragment().removeEdge(fe); } } }
	 * 
	 * }
	 * 
	 * public static Collection<AbstractDirectedEdge> getIncomingEdges(final
	 * IFlowNode v, final Collection<AbstractDirectedEdge> es) {
	 * Collection<AbstractDirectedEdge> incomingEdges = new
	 * ArrayList<AbstractDirectedEdge>(0); for (AbstractDirectedEdge e : es) { if
	 * (e.getTarget().getId().equals(v.getId())) { incomingEdges.add(e); } } return
	 * incomingEdges; }
	 * 
	 * public static Collection<AbstractDirectedEdge> getOutgoingEdges(final
	 * IFlowNode v, final Collection<AbstractDirectedEdge> es) {
	 * Collection<AbstractDirectedEdge> outgoingEdges = new
	 * ArrayList<AbstractDirectedEdge>(0); for (AbstractDirectedEdge e : es) { if
	 * (e.getSource().getId().equals(v.getId())) { outgoingEdges.add(e); } } return
	 * outgoingEdges; }
	 * 
	 * public static List<IFlowNode> getPreset(final IFlowNode v, final
	 * Collection<AbstractDirectedEdge> es) { List<IFlowNode> preset = new
	 * ArrayList<IFlowNode>(0); for (AbstractDirectedEdge e : es) { if
	 * (e.getTarget().getId().equals(v.getId())) { preset.add((IFlowNode)
	 * e.getSource()); } } return preset; }
	 * 
	 * public static List<IFlowNode> getPostset(final IFlowNode v, final
	 * Collection<AbstractDirectedEdge> es) { List<IFlowNode> postset = new
	 * ArrayList<IFlowNode>(0); for (AbstractDirectedEdge e : es) { if
	 * (e.getSource().getId().equals(v.getId())) { postset.add((IFlowNode)
	 * e.getTarget()); } } return postset; }
	 * 
	 * public static FlowNode getFirstVertex(final Collection<FlowNode> vertices) {
	 * return vertices.iterator().next(); }
	 * 
	 * public static AbstractDirectedEdge getFirstEdge(final
	 * Collection<AbstractDirectedEdge> c) { return c.iterator().next(); }
	 * 
	 *//**
		 * Creates a new child mapping by replacing pocket ids of fragment by their
		 * corresponding pockets ids of content.
		 *
		 * @param childMappings  map pocketId -> childId
		 * @param pocketMappings map fragment pocket Id -> content pocket Id
		 */
	/*
	 * public static Map<String, String> remapChildren(final Map<String, String>
	 * childMappings, final Map<String, String> pocketMappings) throws
	 * PocketMappingException { Map<String, String> newChildMapping = new
	 * HashMap<String, String>(0); for (Entry<String, String> stringStringEntry :
	 * childMappings.entrySet()) { String o =
	 * pocketMappings.get(stringStringEntry.getKey()); if (o != null) { String
	 * mappedPocketId = pocketMappings.get(stringStringEntry.getKey()); String
	 * childId = stringStringEntry.getValue(); newChildMapping.put(mappedPocketId,
	 * childId); } else { String msg = "Mapping of pocket " +
	 * stringStringEntry.getKey() + " is null."; LOGGER.error(msg); throw new
	 * PocketMappingException(msg); } } return newChildMapping; }
	 * 
	 *//**
		 * Creates a new child mapping by replacing pocket ids of fragment by their
		 * corresponding pockets ids of content.
		 *
		 * @param childMappings  map pocketId -> childId
		 * @param pocketMappings map fragment pocket Id -> content pocket Id
		 *//*
			 * public static Map<String, FragmentVersion> remapChildren(final
			 * List<FragmentVersionDag> childMappings, final Map<String, String>
			 * pocketMappings) throws PocketMappingException { Map<String, FragmentVersion>
			 * newChildMapping = new HashMap<String, FragmentVersion>(0); for
			 * (FragmentVersionDag fvd : childMappings) { String o =
			 * pocketMappings.get(fvd.getPocketId()); if (o != null) { String mappedPocketId
			 * = pocketMappings.get(fvd.getPocketId()); newChildMapping.put(mappedPocketId,
			 * fvd.getChildFragmentVersionId()); } else { String msg = "Mapping of pocket "
			 * + fvd.getPocketId() + " is null."; LOGGER.error(msg); throw new
			 * PocketMappingException(msg); } } return newChildMapping; }
			 * 
			 * @SuppressWarnings("unchecked") public static void cleanFragment(final
			 * RPSTNode f) { Collection<AbstractDirectedEdge> es = f.getFragmentEdges();
			 * Collection<FlowNode> vs = f.getFragment().getVertices();
			 * Collection<AbstractDirectedEdge> removableEdges = new
			 * ArrayList<AbstractDirectedEdge>(0);
			 * 
			 * for (AbstractDirectedEdge e : es) { if (!vs.contains(e.getSource()) ||
			 * !vs.contains(e.getTarget())) { removableEdges.add(e); } }
			 * 
			 * f.getFragment().removeEdges(removableEdges); }
			 * 
			 * @SuppressWarnings("unchecked") public static void reconnectBoundary1(final
			 * RPSTNode f, final FlowNode oldB1, final FlowNode newB1, final RPST rpst) {
			 * FlowNode b1 = (FlowNode) f.getEntry(); FlowNode b2 = (FlowNode) f.getExit();
			 * 
			 * if (b1.getId().equals(oldB1.getId())) { f.setEntry(newB1);
			 * reconnectVertices(b1, newB1, f); f.getFragment().addVertex(newB1);
			 * f.getFragment().removeVertex(b1);
			 * 
			 * Collection<RPSTNode> childFragments = rpst.getChildren(f); for (RPSTNode
			 * childFragment : childFragments) { if (childFragment.getType() != TCType.T) {
			 * reconnectBoundary1(childFragment, oldB1, newB1, rpst); } } }
			 * 
			 * if (b2.equals(oldB1)) { LOGGER.debug("b2 = oldB1 in fragment: " +
			 * fragmentToString(f)); } }
			 * 
			 * @SuppressWarnings("unchecked") public static void reconnectBoundary2(final
			 * RPSTNode f, final FlowNode oldB2, final FlowNode newB2, final RPST rpst) {
			 * FlowNode b2 = (FlowNode) f.getExit();
			 * 
			 * if (b2.equals(oldB2)) { f.setExit(newB2); reconnectVertices(b2, newB2, f);
			 * f.getFragment().addVertex(newB2); f.getFragment().removeVertex(b2);
			 * 
			 * Collection<RPSTNode> childFragments = rpst.getChildren(f); for (RPSTNode
			 * childFragment : childFragments) { if (childFragment.getType() != TCType.T) {
			 * reconnectBoundary2(childFragment, oldB2, newB2, rpst); } } } }
			 * 
			 * @SuppressWarnings("unchecked") public static void reconnectVertices(final
			 * FlowNode oldVertex, final FlowNode newVertex, final RPSTNode f) {
			 * Collection<AbstractDirectedEdge> edges = f.getFragmentEdges(); for
			 * (AbstractDirectedEdge edge : edges) { if
			 * (edge.getSource().getId().equals(oldVertex.getId())) {
			 * edge.setSource(newVertex); } else if
			 * (edge.getTarget().getId().equals(oldVertex.getId())) {
			 * edge.setTarget(newVertex); } } }
			 * 
			 * public static FlowNode duplicateVertex(final IVertex v, final CPF og) {
			 * String label = v.getName(); String type = og.getVertexProperty(v.getId(),
			 * Constants.TYPE);
			 * 
			 * FlowNode newV; if (label.equals("XOR")) { newV = new CpfXorGateway(label); }
			 * else if (label.equals("AND")) { newV = new CpfAndGateway(label); } else if
			 * (label.equals("OR")) { newV = new CpfOrGateway(label); } else { newV = new
			 * CpfNode(label); } og.addVertex(newV); og.setVertexProperty(newV.getId(),
			 * Constants.TYPE, type);
			 * 
			 * return newV; }
			 * 
			 * @SuppressWarnings("unchecked") public static String fragmentToString(final
			 * RPSTNode f, final CPF g) { StringBuilder fs = new StringBuilder();
			 * Collection<FlowNode> vs = f.getFragment().getVertices(); for (FlowNode v :
			 * vs) { String label = v.getName();
			 * fs.append('\n').append(v).append(" : ").append(label); }
			 * fs.append("\nBoundary 1: ").append(f.getEntry().getId());
			 * fs.append("\nBoundary 2: ").append(f.getExit().getId()); return
			 * fs.toString(); }
			 * 
			 * @SuppressWarnings("unchecked") public static String fragmentToString(final
			 * RPSTNode f) { StringBuilder fs = new StringBuilder(); Collection<FlowNode> vs
			 * = f.getFragment().getVertices(); for (FlowNode v : vs) {
			 * fs.append(v).append(", "); } return fs.toString(); }
			 * 
			 * public static String getType(final FlowNode node) { String type = null; if
			 * (node instanceof CpfTask) { type = Constants.FUNCTION; } else if (node
			 * instanceof CpfEvent) { type = Constants.EVENT; } else { if (node instanceof
			 * CpfGateway) { type = Constants.CONNECTOR; } else { String nodeName =
			 * node.getName(); if ("OR".equals(nodeName) || "XOR".equals(nodeName) ||
			 * "AND".equals(nodeName)) { type = Constants.CONNECTOR; } } } return type; }
			 * 
			 * public static String getType(final ICpfNode node) { return getType((FlowNode)
			 * node); }
			 */
}
