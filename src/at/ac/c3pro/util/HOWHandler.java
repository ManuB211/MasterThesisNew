package at.ac.c3pro.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jbpt.graph.abs.IDirectedGraph;

import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.Event;
import at.ac.c3pro.node.Gateway;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.Interaction;
import at.ac.c3pro.node.Interaction.InteractionType;

/**
 * This class handles the adaptation of the generated choreography model for the
 * introduction of the Handover-of-Work interaction type.
 * 
 * This is due to the fact, that contrary to the other interaction types,
 * handover-of-work cannot be on the same level for the sender and receiver.
 * 
 * * For the receiving participant it always has to be the first interaction, and
	 * therefore needs to be "cropped" out of the initial graph and set as the first..
	 * 
	 * This might also have implications for the graph structure itself, as branches or gateways can become unneccessary as a whole.
	 * E.g. consider the following situation where IA2 is a HOW and the participant is the receiving end:
	 * 
	 * @formatter:off
	 * 				 		  XOR - [...] - XOR_merge	
	 * 					/							      \
	 * 	Start - 	AND 								    AND-merge - Stop
	 * 					\							      /
	 * 					 \			IA2	    			 /		
	 * 					  \		/		  \				/
	 * 						XOR				XOR_merge
	 * 							\ 	___   / 	
	 * 
	 * @formatter:on
	 * 
	 * The resulting graph would be:
	 * 
	 * @formatter:off
	 * 
	 * 
	 * Start - IA2 - XOR - [...] - XOR_merge - Stop
	 * 
	 * 
	 * @formatter:on
	 * 
	 * As the movement of IA2 to the front would result in an XOR-gateway without any children, 
	 * which would further lead to an AND-gateway with only one child, making it redundant was well
	 * 
	 * Therefore the following functions are introduced: 
	 * 
	 * cropOutHandover -> moves the Handover-of-Work node to the front (if participant is the receiving end)
	 * cleanGraph -> removes the structures, that became unnecessary
 */
public class HOWHandler {

	private IDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> graph;
	private Role currentRole;
	private Event start;
	private boolean cleanNecessary;

	public HOWHandler(IDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> pGraph, Role pCurrentRole) {
		this.graph = pGraph;
		this.currentRole = pCurrentRole;
		this.start = this.findStartNode();
		this.cleanNecessary = false;
	}

	public void run() {
		this.cropOutHandover();
		while (this.cleanNecessary) {
			this.cleanGraph();
		}
	}

	private Event findStartNode() {
		for (Object vertex : graph.getVertices()) {
			if (vertex instanceof Event && ((Event) vertex).getName().equals("start")) {
				return ((Event) vertex);
			}
		}
		return null;
	}

	/**
	 * First only the cropping out; If a HOW-node is found where the recipient is
	 * the currentRole (1) Find predecessor and successor (2) Remove edges
	 * predecessor -> node and node -> successor (3) Add edge predecessor ->
	 * successor (if not creating an edge from a fork to the merge) (4) Remove edge
	 * start -> successorOfStart (5) Add edges start -> node and node ->
	 * successorOfStart
	 */
	private void cropOutHandover() {

		Iterator<Edge<IChoreographyNode>> iter = this.graph.getEdges().iterator();

		while (iter.hasNext()) {

			Edge<IChoreographyNode> edge = iter.next();

			if (edge.getTarget() instanceof Interaction) {
				Interaction target = (Interaction) edge.getTarget();

				if (InteractionType.HANDOVER_OF_WORK.equals(target.getInteractionType())
						&& currentRole.equals(target.getParticipant2())) {

					// Get Predecessor and Successor (there is always only one node, only gateways
					// can have multiple
					IChoreographyNode pred = new ArrayList<>(this.graph.getDirectPredecessors(Arrays.asList(target)))
							.get(0);
					IChoreographyNode succ = new ArrayList<>(this.graph.getDirectSuccessors(Arrays.asList(target)))
							.get(0);

					// Remove Edges Predecessor -> target and target -> Successor
					this.graph.removeEdge(this.graph.getEdge(pred, target));
					this.graph.removeEdge(this.graph.getEdge(target, succ));

					// Add edge Pred -> Succ, but only if its not from fork to merge
					// Otherwise in case of XOR, it would be a different logic
					if (!((pred.getName().startsWith("AND") && succ.getName().startsWith("AND")
							|| pred.getName().startsWith("XOR") && succ.getName().startsWith("XOR"))
							&& pred.getName().substring(0, 4).equals(succ.getName().substring(0, 4)))) {
						this.graph.addEdge(pred, succ);
					}

					// Remove edge start -> startSucc (again only one successor possible)
					IChoreographyNode startSucc = new ArrayList<>(
							this.graph.getDirectSuccessors(Arrays.asList(this.start))).get(0);

					this.graph.removeEdge(this.graph.getEdge(this.start, startSucc));

					// Add edges start -> target and target -> startSucc
					this.graph.addEdge(this.start, target);
					this.graph.addEdge(target, startSucc);

					// (TODO: Add to MessageFlow )As a participant can only be receiver of one
					// handover of work (has to be first), break
					this.cleanNecessary = true;
					break;
				}
			}

		}

	}

	private void cleanGraph() {

		ArrayList<IChoreographyNode> queue = new ArrayList<>();
		queue.addAll(this.graph.getDirectSuccessors(this.start));

		boolean foundSomethingToClean = false;

		while (queue.size() > 0) {

			if (queue.size() == 1 && queue.get(0).getName().equals("end")) {
				break;
			}

			IChoreographyNode currentNode = queue.remove(0);

			// Check if node is a fork and if it has only one child (cf HOW was taken off
			// here)
			if (currentNode instanceof Gateway && !((Gateway) currentNode).getName().endsWith("_m")) {

				List<IChoreographyNode> currentNodeSuccessors = new ArrayList<>(
						this.graph.getDirectSuccessors(currentNode));

				// Only one child present
				if (currentNodeSuccessors.size() == 1) {

					IChoreographyNode forkSuccessor = currentNodeSuccessors.get(0);
					IChoreographyNode forkPredecessor = new ArrayList<>(this.graph.getDirectPredecessors(currentNode))
							.get(0);

					// Get Merge node dynamically, as we could have multiple interactions on the
					// only child branch of fork
					IChoreographyNode mergeNode = this.getMergeNode(currentNode.getName());

					IChoreographyNode mergePredecessor = new ArrayList<>(this.graph.getDirectPredecessors(mergeNode))
							.get(0);
					IChoreographyNode mergeSuccessor = new ArrayList<>(this.graph.getDirectSuccessors(mergeNode))
							.get(0);

					// Update edges accordingly
					this.graph.removeEdge(this.graph.getEdge(forkSuccessor, currentNode));
					this.graph.removeEdge(this.graph.getEdge(currentNode, forkSuccessor));
					this.graph.addEdge(forkPredecessor, forkSuccessor);

					this.graph.removeVertex(currentNode);

					this.graph.removeEdge(this.graph.getEdge(mergePredecessor, mergeNode));
					this.graph.removeEdge(this.graph.getEdge(mergeNode, mergeSuccessor));
					this.graph.addEdge(mergePredecessor, mergeSuccessor);

					this.graph.removeVertex(mergeNode);

					// Ensure that another run of clean is made until graph is completely clean
					foundSomethingToClean = true;
				}
			}

			queue.addAll(this.graph.getDirectSuccessors(currentNode));
		}

		if (foundSomethingToClean) {
			this.cleanNecessary = true;
		} else {
			this.cleanNecessary = false;
		}

	}

	private IChoreographyNode getMergeNode(String name) {

		for (IChoreographyNode vertex : this.graph.getVertices()) {
			if (vertex.getName().startsWith(name) && vertex.getName().endsWith("_m")) {
				return vertex;
			}
		}

		throw new IllegalArgumentException(
				"Could not find corresponding merge node. Something has to have been wrong before");

	}

	public Collection<Edge<IChoreographyNode>> getEdges() {
		return this.graph.getEdges();
	}
}
