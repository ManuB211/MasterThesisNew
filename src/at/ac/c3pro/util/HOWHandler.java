package at.ac.c3pro.util;

import at.ac.c3pro.chormodel.IRpstModel;
import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.node.*;
import at.ac.c3pro.node.Interaction.InteractionType;
import org.jbpt.graph.abs.IDirectedGraph;
import org.jbpt.utils.IOUtils;

import java.util.*;
import java.util.stream.Collectors;

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

    private final IDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> graph;
    private final IRpstModel<Edge<IChoreographyNode>, IChoreographyNode> graphRpstModel;
    private final Role currentRole;
    private final Event start;
    private boolean cleanNecessary;
    private List<IChoreographyNode> xorsWithDirectConnectionToMerge;

    // For debug purposes
    private final boolean printDebugGraphs = false;

    //TODO: entferne weil deprecated
    public HOWHandler(IRpstModel<Edge<IChoreographyNode>, IChoreographyNode> pGraphRpstModel, Role pCurrentRole) {
        this.graphRpstModel = pGraphRpstModel;
        this.graph = pGraphRpstModel.getdigraph();
        this.currentRole = pCurrentRole;
        this.start = this.findStartNode();
        this.cleanNecessary = false;
        this.xorsWithDirectConnectionToMerge = pGraphRpstModel.getAllXORsWithDirectConnectionToMerge();
    }

    public void run() {
        if (printDebugGraphs)
            printGraphsDebug("BeforeCropOut");

        this.cropOutHandover();

        if (printDebugGraphs)
            printGraphsDebug("AfterCropOutBeforeClean");

        //Check for instances where XOR->XORm are only connections between the two
//        this.eliminateOnlyDirectXORtoMergeConnections(this.graph);

        //Setze den digraph zurück ins RPSTModel
        this.graphRpstModel.setDiGraph(this.graph);

        //We can't (only?) use the normal reduceGraph Method, as the HOW was only moved in the diGraph but is still reflected in the graphRpstModel.
        //In there mutation is hard due to the models structure. Hence we check the
        this.graphRpstModel.reduceGraph(this.xorsWithDirectConnectionToMerge);

        //Remove instances where gateways have only one child connection
        this.cleanSingleChildGateways();
        this.graphRpstModel.setDiGraph(this.graph);

    }

    private void cleanSingleChildGateways() {
        for (IChoreographyNode node : this.graph.getVertices()) {

            if (node instanceof Gateway && !node.getName().endsWith("_m")) {
                IChoreographyNode mergeNode = getMergeNode(node.getName());

                List<IChoreographyNode> children = new ArrayList<>(graph.getDirectSuccessors(node));

                //If only child is the merge node itself, connect pred of node with succ of mergeNode.
                //Remove connection pred of node -> node, node -> mergeNode, mergeNode -> succ of mergeNode
                if (children.size() == 1 && children.get(0).equals(mergeNode)) {

                    //There can only be one pred of node and one succ of mergeNode, otherwise the graph would be shit
                    IChoreographyNode predOfNode = new ArrayList<>(this.graph.getDirectPredecessors(node)).get(0);
                    IChoreographyNode succOfMergeNode = new ArrayList<>(this.graph.getDirectSuccessors(mergeNode)).get(0);

                    this.graph.removeEdge(this.graph.getEdge(predOfNode, node));
                    this.graph.removeEdge(this.graph.getEdge(node, mergeNode));
                    this.graph.removeEdge(this.graph.getEdge(mergeNode, succOfMergeNode));

                    this.graph.removeVertex(node);
                    this.graph.removeVertex(mergeNode);

                    this.graph.addEdge(predOfNode, succOfMergeNode);

                } else if (children.size() == 1) {
                    //Only child of the gateway fork is not the merge node.
                    //Remove nodePred -> node, node -> nodeSucc, add nodePred -> nodeSucc
                    //Remove mergeNodeSucc -> mergeNode, mergeNode -> mergeNodeSucc, add mergeNodePred -> mergeNodeSucc

                    IChoreographyNode predOfNode = new ArrayList<>(this.graph.getDirectPredecessors(node)).get(0);
                    IChoreographyNode succOfNode = new ArrayList<>(this.graph.getDirectSuccessors(node)).get(0);
                    IChoreographyNode predOfMergeNode = new ArrayList<>(this.graph.getDirectPredecessors(mergeNode)).get(0);
                    IChoreographyNode succOfMergeNode = new ArrayList<>(this.graph.getDirectSuccessors(mergeNode)).get(0);

                    this.graph.removeEdge(this.graph.getEdge(predOfNode, node));
                    this.graph.removeEdge(this.graph.getEdge(node, succOfNode));
                    this.graph.addEdge(predOfNode, succOfNode);
                    this.graph.removeVertex(node);

                    this.graph.removeEdge(this.graph.getEdge(predOfMergeNode, mergeNode));
                    this.graph.removeEdge(this.graph.getEdge(mergeNode, succOfMergeNode));
                    this.graph.addEdge(predOfMergeNode, succOfMergeNode);
                    this.graph.removeVertex(mergeNode);
                }

            }
        }
    }

    private void printGraphsDebug(String timeInfo) {
        IOUtils.toFile(GlobalTimestamp.timestamp + "/" + GlobalTimestamp.timestamp + "_" + timeInfo + "_" + currentRole.name + ".dot",
                graph.toDOT());
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
                    IChoreographyNode pred = new ArrayList<>(this.graph.getDirectPredecessors(Collections.singletonList(target)))
                            .get(0);
                    IChoreographyNode succ = new ArrayList<>(this.graph.getDirectSuccessors(Collections.singletonList(target)))
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
                            this.graph.getDirectSuccessors(Collections.singletonList(this.start))).get(0);

                    this.graph.removeEdge(this.graph.getEdge(this.start, startSucc));

                    // Add edges start -> target and target -> startSucc
                    this.graph.addEdge(this.start, target);
                    this.graph.addEdge(target, startSucc);

                    // handover of work (has to be first), break
                    this.cleanNecessary = true;
                    break;
                }
            }

        }

    }

    private IChoreographyNode getMergeNode(String name) {

        for (IChoreographyNode vertex : this.graph.getVertices()) {
            if (vertex.getName().equals(name + "_m")) {
                return vertex;
            }
        }

        throw new IllegalArgumentException(
                "Could not find corresponding merge node to " + name + ". Something has to have been wrong before");

    }

    /**
     * This function is another step of the graph reduction, that was not present in
     * the initial reduction algorithm of RpstModel. Said algorithm did produce
     * instances, in which there are orphaned parent gateway nodes, meaning that
     * e.g. an XOR pointed to another gateway merge, although the XOY could never be
     * reached. This function removes such instances
     */
    private void removeOrphanParents() {

        boolean anotherRunNeeded = true;

        while (anotherRunNeeded) {
            anotherRunNeeded = false;

            System.out.println("==================================================================");
            System.out.println("==================================================================");
            System.out.println("==================================================================");
            for (Edge<IChoreographyNode> e : graph.getEdges()) {
                System.out.println("Edge: " + e.getSource() + " --> " + e.getTarget());
            }
            System.out.println("==================================================================");
            System.out.println("==================================================================");
            System.out.println("==================================================================");

            for (IChoreographyNode node : graph.getVertices()) {

                List<IChoreographyNode> preds = graph.getDirectPredecessors(node).stream().collect(Collectors.toList());

                if (preds.isEmpty()) {

                    // If node is a gateway without parents, it needs to be removed
                    if (node instanceof Gateway) {
                        graph.removeVertex(node);

                        List<IChoreographyNode> succs = graph.getDirectSuccessors(node).stream()
                                .collect(Collectors.toList());

                        for (IChoreographyNode child : succs) {
                            graph.removeEdge(graph.getEdge(node, child));
                        }
                        anotherRunNeeded = true;

                    }
                    // If node is a interaction without parents something went wrong (for now throw
                    // exception, TODO restart generation?)
                    else if (node instanceof Interaction) {
                        throw new IllegalStateException("Unreachable interaction node found");
                    }

                    // If node i a event (start) nothing happens
                }
            }
        }

    }

    public Collection<Edge<IChoreographyNode>> getEdges() {
        return this.graph.getEdges();
    }
}
