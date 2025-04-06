package at.ac.c3pro.util;

import at.ac.c3pro.chormodel.Choreography;
import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.node.*;
import org.jbpt.graph.abs.IDirectedGraph;
import org.jbpt.hypergraph.abs.IGObject;
import org.jbpt.utils.IOUtils;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EasySoundnessChecker {

    boolean visualize;

    Map<Role, IDirectedGraph<Edge<IPublicNode>, IPublicNode>> publicModelsByRole;

    List<IPublicNode> visited;
    Map<String, Role> participantsByName;
    Map<Role, IPublicNode> endNodes;

    Map<Role, Integer> eliminationStepsForParticipantsGraph;

    List<List<IPublicNode>> traces;

    //Stores all the interactions from which we try to find traces back to a start node
    List<IPublicNode> interactionsToCheck;

    /*Maps a node to an interaction it is conditioned by meaning
     * that for an entry x->y: x can only be executed if y has been executed before
     * */
//    Map<IPublicNode, List<IPublicNode>> conditionMap;

    Pattern regEx = Pattern.compile("P_\\d+>P_\\d+");

    OutputHandler outputHandler;

    public EasySoundnessChecker(Choreography choreography, boolean visualize) throws IOException {

        outputHandler = new OutputHandler(OutputHandler.OutputType.EASY_SOUNDNESS);

        visited = new ArrayList<>();
        participantsByName = new HashMap<>();
        endNodes = new HashMap<>();
        traces = new ArrayList<>();
        publicModelsByRole = new HashMap<>();
//        conditionMap = new HashMap<>();
        interactionsToCheck = new ArrayList<>();
        this.visualize = visualize;
        eliminationStepsForParticipantsGraph = new HashMap<>();
        setupDeconstructionOfPuModels(choreography);
    }

    public void run() throws IOException, InterruptedException {
        try {
            //Step 1: Check for cyclic waits
            this.checkForCyclicWaits();
            //Step 2: Check other property
        } catch (Exception e) {
            outputHandler.closePrintWriter();
            throw e;
        }
        outputHandler.closePrintWriter();
    }

    private void setupDeconstructionOfPuModels(Choreography choreo) {

        for (Role role : choreo.collaboration.roles) {
            //Save role
            participantsByName.put(role.getName(), role);

            //Save mapping Role -> Digraph of public model
            IDirectedGraph<Edge<IPublicNode>, IPublicNode> puModelGraph = choreo.collaboration.R2PuM.get(role).getdigraph();
            publicModelsByRole.put(role, puModelGraph);

            //Initialize the tracking map for elimination step as 1
            eliminationStepsForParticipantsGraph.put(role, 1);

            //Save end-nodes of a roles public model
            List<IPublicNode> nodes = new ArrayList<>(puModelGraph.getVertices());

            for (IPublicNode node : nodes) {
                if (node.getName().equals("end")) {
                    endNodes.put(role, node);
                    break;
                }
            }

            //Save all the interactions to the list of interactions to find a path from
            //That means all interactions (Send and Receive) and the end events
            List<IPublicNode> interactionsToCheckInOrder = getInteractionsToCheckInOrderBFS(puModelGraph, endNodes.get(role));

            if (interactionsToCheckInOrder != null) {
                outputHandler.printEasySoundness("For role " + role.getName() + " the following order of consideration has been computed: ");
                outputHandler.printEasySoundness(interactionsToCheckInOrder.stream().map(IGObject::getName).collect(Collectors.joining(", \n")));
                interactionsToCheck.addAll(interactionsToCheckInOrder);
            } else {
                System.out.println("What the fuck has happened here");
            }

        }
    }

    private void checkForCyclicWaits() throws IOException, InterruptedException {


        outputHandler.printEasySoundness(OutputHandler.EasySoundnessAnalyisBlocks.START);

        //TODO: Mechanism to dynamically stop when either all participants are eliminated or it could been shown that there are no more cycles and there are executable paths
        //--> check mechanism with visited again to ensure

        while (!interactionsToCheck.isEmpty()) {

            outputHandler.printEasySoundness(OutputHandler.EasySoundnessAnalyisBlocks.INTERACTIONS_TO_CHECK_DELIM);
            outputHandler.printEasySoundness("Current Set of Interactions to find trace to start from");
            outputHandler.printEasySoundness(interactionsToCheck.stream().map(IGObject::getName).collect(Collectors.joining(", ")));
            outputHandler.printEasySoundness(OutputHandler.EasySoundnessAnalyisBlocks.INTERACTIONS_TO_CHECK_DELIM);

            //Search one trace at a time, either a cycle or a trace from end to some start
            List<IPublicNode> trace = new ArrayList<>();

            IPublicNode nodeToCheckNext = interactionsToCheck.remove(0);
            IDirectedGraph<Edge<IPublicNode>, IPublicNode> graphForNodeToCheckNext = getGraphToNode(nodeToCheckNext);

            //We set the attribute to true initially. Once the trace cannot be executed further as for a node the graph of the other participant
            // has already been eliminated from consideration, we set it to false TODO
            boolean traceValid = true;

            searchTrace(graphForNodeToCheckNext, nodeToCheckNext, trace, traceValid);


            if (traceValid) {
                outputHandler.printEasySoundness("Trace found: ");
                outputHandler.printEasySoundness(trace.stream().map(IGObject::getName).collect(Collectors.joining("\n --> \n")));
                outputHandler.printEasySoundness(OutputHandler.EasySoundnessAnalyisBlocks.TRACE_DELIM);

                traces.add(trace);

                //Filter out all gateways
                List<IPublicNode> traceOnlyInteractions = trace.stream().filter(node -> !(node instanceof Gateway)).collect(Collectors.toList());

                //If there are duplicates in the traceOnlyInteractions, that means that we found a cycle
                Set<IPublicNode> traceOnlyInteractionsSet = new HashSet<>(traceOnlyInteractions);

                if (traceOnlyInteractionsSet.size() < traceOnlyInteractions.size()) {
                    eliminateCyclesFromConsideration(traceOnlyInteractions);
                }
            } else {
                outputHandler.printEasySoundness("The part of a trace: ");
                outputHandler.printEasySoundness(trace.stream().map(IGObject::getName).collect(Collectors.joining("\n --> \n")));
                outputHandler.printEasySoundness("that was found was not valid and is therefore not feasible to find a path");
                outputHandler.printEasySoundness(OutputHandler.EasySoundnessAnalyisBlocks.TRACE_DELIM);
            }
        }
        outputHandler.printEasySoundness(OutputHandler.EasySoundnessAnalyisBlocks.STOP);


        //After we have iterated all interactions and tried to find a way from them to a start node, while eliminating those that are contained in a cycle,
        //the graphs remaining are all the cyclefree paths
        for (Map.Entry<Role, IDirectedGraph<Edge<IPublicNode>, IPublicNode>> entry : publicModelsByRole.entrySet()) {
            IOUtils.toFile(
                    GlobalTimestamp.timestamp + "/EasySoundness/CycleFreePaths_" + entry.getKey().getName() + ".dot",
                    entry.getValue() != null ? entry.getValue().toDOT() : "");
        }

        if (visualize) {
            VisualizationHandler.visualize(VisualizationHandler.VisualizationType.EASY_SOUNDNESS);
        }

    }

    private void searchTrace(IDirectedGraph<Edge<IPublicNode>, IPublicNode> graph, IPublicNode startingNode, List<IPublicNode> trace, boolean isTraceValid) throws IOException, InterruptedException {

        if (!(startingNode instanceof Gateway || startingNode instanceof Event)) {
            visited.add(startingNode);
        }

        boolean continueWithParents = true;

        if (startingNode.getName().equals("start")) {
            trace.add(startingNode);
            return;
        }

        if (!(startingNode instanceof Gateway) && trace.contains(startingNode)) {
            trace.add(startingNode);
            return;
        }

        trace.add(startingNode);

        /* When the current node that is looked at is an instance of a Receive, it means that the corresponding Send
         * has to already have been performed. Therefore the corresponding Send conditions
         * the execution of the nodeToCheckConditionsFor
         * This also holds for the case, that currNode is a synchronous task
         */
        boolean isSynchronousTask = startingNode.getName().startsWith("S: ");

        if (startingNode instanceof Receive || isSynchronousTask) {

            boolean isMessageExchangeOrHOW = startingNode.getName().startsWith("M: ") || startingNode.getName().startsWith("H: ");

            String[] participantsOfNode = getParticipantsOfNode(startingNode);
            String participant1 = participantsOfNode[0], participant2 = participantsOfNode[1];

            //Get the corresponding node that conditions the execution
            if (isSynchronousTask && startingNode instanceof Receive) {
                //CurrNode is Receive of a synchronous task -> consider corresponding Send next
                IDirectedGraph<Edge<IPublicNode>, IPublicNode> graphOfOtherParticipant = publicModelsByRole.get(participantsByName.get(participant1));

                //If the graph of the other participant has already been eliminated, the interaction cannot be executed and we need to eliminate its context
                if (graphOfOtherParticipant == null) {

                    outputHandler.printEasySoundness("Interaction " + startingNode.getName() + " can never be executed because the graph of the other participant involved in this interaction is not executable at all.");
                    outputHandler.printEasySoundness("Therefore, the Interaction " + startingNode.getName() + " needs to be eliminated as well as its context");

                    eliminateContext(startingNode);

                    isTraceValid = false;
                    return;
                } else {
                    IPublicNode counterpartNode = getCounterpartNode(startingNode, graphOfOtherParticipant);

                    if (!visited.contains(counterpartNode)) {
                        searchTrace(graphOfOtherParticipant, counterpartNode, trace, isTraceValid);
                        continueWithParents = false;
                    }
                }
            } else if (isSynchronousTask) {
                //CurrNode is Send of a synchronous task -> consider corresponding Receive next
                IDirectedGraph<Edge<IPublicNode>, IPublicNode> graphOfOtherParticipant = publicModelsByRole.get(participantsByName.get(participant2));

                //If the graph of the other participant has already been eliminated, the interaction cannot be executed and we need to eliminate its context
                if (graphOfOtherParticipant == null) {

                    outputHandler.printEasySoundness("Interaction " + startingNode.getName() + " can never be executed because the graph of the other participant involved in this interaction is not executable at all.");
                    outputHandler.printEasySoundness("Therefore, the Interaction " + startingNode.getName() + " needs to be eliminated as well as its context");

                    eliminateContext(startingNode);

                    isTraceValid = false;
                    return;
                } else {
                    IPublicNode counterpartNode = getCounterpartNode(startingNode, graphOfOtherParticipant);

                    if (!visited.contains(counterpartNode)) {
                        searchTrace(graphOfOtherParticipant, counterpartNode, trace, isTraceValid);
                        continueWithParents = false;
                    }
                }

            } else {
                //CurrNode is Receive of a MX or HOW -> Conditioned by corresponding Send
                if (isMessageExchangeOrHOW) {
                    IDirectedGraph<Edge<IPublicNode>, IPublicNode> graphOfOtherParticipant = publicModelsByRole.get(participantsByName.get(participant1));

                    //If the graph of the other participant has already been eliminated, the interaction cannot be executed and we need to eliminate its context
                    if (graphOfOtherParticipant == null) {

                        outputHandler.printEasySoundness("Interaction " + startingNode.getName() + " can never be executed because the graph of the other participant involved in this interaction is not executable at all.");
                        outputHandler.printEasySoundness("Therefore, the Interaction " + startingNode.getName() + " needs to be eliminated as well as its context");

                        eliminateContext(startingNode);

                        isTraceValid = false;
                        return;
                    } else {
                        IPublicNode counterpartNode = getCounterpartNode(startingNode, graphOfOtherParticipant);

                        searchTrace(graphOfOtherParticipant, counterpartNode, trace, isTraceValid);
                        continueWithParents = false;
                    }


                }

            }
        }

        if (continueWithParents) {
            //Get one parent of the current graph to continue execution with
            List<IPublicNode> parents = new ArrayList<>(graph.getDirectPredecessors(startingNode));

            IPublicNode randomPossibleParent = getNextInTracePreferUnvisited(parents);

            searchTrace(graph, randomPossibleParent, trace, isTraceValid);
        }
    }

    private void eliminateCyclesFromConsideration(List<IPublicNode> trace) throws IOException, InterruptedException {
        Set<IPublicNode> helperSetDups = new HashSet<>();

        //There can only be one cycle node at a time, as the searchTrace method returns at the first occurence of a cycle
        IPublicNode cycleNode = trace.stream().filter(node -> !helperSetDups.add(node)).collect(Collectors.toList()).get(0);
        //Get index of the first occurence of the cycle node (cycle begin)
        int cycleStartIndex = trace.indexOf(cycleNode);

        List<IPublicNode> cycleTrace = trace.subList(cycleStartIndex, trace.size());

        outputHandler.printEasySoundness("Cycle found: ");
        outputHandler.printEasySoundness(cycleTrace.stream().map(IGObject::getName).collect(Collectors.joining("\n --> \n")) + "\n");


        //For types HOW, MX and RS collect all interactions in the cycleTrace and their counterpart nodes. Ressource Share can be ignored, as the non-executableness of it for one participant does not mean the counterpart cannot fire
        //TODO: Janik fragen, ob das wirklich der Fall ist

        Set<IPublicNode> eliminateBecauseOfCycle = new HashSet<>();

        for (IPublicNode node : cycleTrace) {
            eliminateBecauseOfCycle.add(node);
            if (!node.getName().startsWith("R: ")) {
                IPublicNode counterpartNode = getCounterpartNode(node);
                if (counterpartNode != null) {
                    eliminateBecauseOfCycle.add(counterpartNode);
                } else {
                    outputHandler.printEasySoundness("Counterpart Graph was already null, TODO check if that was supposed to ever happen");
                }
            }
        }

        for (IPublicNode nodeToBeEliminated : eliminateBecauseOfCycle) {
            outputHandler.printEasySoundness(OutputHandler.EasySoundnessAnalyisBlocks.NODE_DELIM);
            outputHandler.printEasySoundness("Checking implications of interaction " + nodeToBeEliminated + " being eliminated\n");
            eliminateContext(nodeToBeEliminated);
        }


    }

    /**
     * If a node is eliminated from the consideration, because its not executable, that means that its "context" also has to be eliminated.
     * This means, that an eliminated node is on an AND-branch, the whole and branch can not be executed and thus also needs to be eliminated.
     * If the eliminated node has a non-branched connection to the start event, the whole participant cannot work
     * If the eliminated node is on an XOR-Branch, all other nodes on that branch cannot executed and therefore need to be eliminated
     * <p>
     * This needs to be done recursively; e.g. when eliminated node is on an AND-branch, and the and branch has a non-branching connection to the startnode
     * then the whole participant cannot work again
     */
    private void eliminateContext(IPublicNode node) throws IOException, InterruptedException {

        IDirectedGraph<Edge<IPublicNode>, IPublicNode> graphOfNode = getGraphToNode(node);

        //Graph has already been eliminated -> can not execute at all
        if (graphOfNode == null) {
            outputHandler.printEasySoundness("Graph of the participant has already been eliminated, as its not exeuctable. Skipping validation for the node\n");
            return;
        }

        //Check if the current node to eliminate with its context is still present, because it might have been eliminated in a previous elimination step
        if (!graphOfNode.getVertices().contains(node)) {
            outputHandler.printEasySoundness("Node has already been eliminated in a previous step, Continuing with the next one");
            return;
        }

        List<IPublicNode> toEliminate = new ArrayList<>();

        //Step 1: Traverse the graph backwards until you find either a XOR node or the start node
        //In case of XOR, the other branches of XOR can potentially be used to get from start to end
        //In case of start, the whole participant cannot execute
        eliminateContextUntilXOROrStart(graphOfNode, node, toEliminate);

//        Set<IPublicNode> toEliminateSet = new HashSet<>();

        IPublicNode stopNode = toEliminate.remove(toEliminate.size() - 1);

        Role participantOfNode = participantsByName.get(getParticipantOfNode(node));

        if (stopNode instanceof Event) {
            //Stopped at start

            outputHandler.printEasySoundness("All nodes of the participant " + participantOfNode.getName() + " are not executable");
            outputHandler.printEasySoundness("Remove the following nodes as starting points to find a path to a start node: ");
            outputHandler.printEasySoundness(publicModelsByRole.get(participantOfNode).getVertices().stream().map(IGObject::getName).collect(Collectors.joining(",")) + "\n");

            //As the whole graph will be eliminated, we can also eliminate all nodes from consideration for finding a path from the nodes to a start node
            interactionsToCheck.removeAll(publicModelsByRole.get(participantOfNode).getVertices());

            //As the participant is not executable, set its graph to null
            publicModelsByRole.put(participantOfNode, null);

            outputHandler.printEasySoundness("Added the new (empty) graph for participant " + participantOfNode + " in EasySoundness/");

            IOUtils.toFile("/EasySoundnessCycleChecks_" + participantOfNode.getName() + "_Change" + eliminationStepsForParticipantsGraph.get(participantOfNode), "");
            eliminationStepsForParticipantsGraph.put(participantOfNode, eliminationStepsForParticipantsGraph.get(participantOfNode) + 1);


            return;
        } else if (stopNode instanceof Gateway) {
            //Stopped at XOR

            //Get the first element on the XOR branch
            IPublicNode firstNodeOnXORbranch = toEliminate.get(toEliminate.size() - 1);
            String xorMergeName = stopNode.getName() + "_m";

            //Step 2 Eliminate all nodes on the path from the XOR through the first node on the branch to the XOR merge

            outputHandler.printEasySoundness("Eliminating nodes on the XOR branch that cannot be executed");
            eliminateContextXORBranch(graphOfNode, firstNodeOnXORbranch, xorMergeName);

            //TODO: Move into actual elimination to get all nodes (in case of branches, only one branch is eliminated currently)
            interactionsToCheck.removeAll(toEliminate);

            //TODO Fix toEliminate, also needs to add the nodes further down
            outputHandler.printEasySoundness("Nodes eliminated: " + toEliminate.stream().map(IGObject::getName).collect(Collectors.joining(",")));
            outputHandler.printEasySoundness("These nodes are also eliminated from consideration to find a path to a start node from");

            publicModelsByRole.put(participantOfNode, graphOfNode);

            outputHandler.printEasySoundness("Saved new graph for possible path finding to EasySoundness/");

            //TODO: hier kann es auch noch zur anzeige von traces kommen, die in teilen bereits eliminiert wurden

            //Export changed graph
            IOUtils.toFile(GlobalTimestamp.timestamp + "/EasySoundness/EasySoundnessCycleChecks_" + participantOfNode.getName() + "_Change" + eliminationStepsForParticipantsGraph.get(participantOfNode) + ".dot", graphOfNode.toDOT());
            eliminationStepsForParticipantsGraph.put(participantOfNode, eliminationStepsForParticipantsGraph.get(participantOfNode) + 1);


        }


    }

    /**
     * Iterates from the given XOR-gateway downwards, eliminating all the interactions on all paths to its merge node
     */
    private void eliminateContextXORBranch(IDirectedGraph<Edge<IPublicNode>, IPublicNode> graph, IPublicNode node, String gatewayMergeName) {

        //Remove the node and the connection to its parents
        List<IPublicNode> parents = new ArrayList<>(graph.getDirectPredecessors(node));
        List<IPublicNode> children = new ArrayList<>(graph.getDirectSuccessors(node));

        removeParentEdgesAndNode(graph, parents, node, true);

        if (node instanceof Receive || node instanceof Send) {
            interactionsToCheck.remove(node);
            addCounterpartNodeToPathConsiderationAgain(node);
        }

        outputHandler.printEasySoundness("Eliminating " + node.getName());

        for (IPublicNode child : children) {

            //If the child is the gateway merge node, we have to only remove the connection to it and can return after
            if (child.getName().equals(gatewayMergeName)) {
                removeParentEdgesAndNode(graph, Collections.singletonList(node), child, false);
                interactionsToCheck.remove(node);

//                addCounterpartNodeToPathConsiderationAgain(node);

                outputHandler.printEasySoundness("Eliminating " + node.getName());
                return;
            }

            eliminateContextXORBranch(graph, child, gatewayMergeName);
        }

    }

    /**
     * When removing a node, we need to add the counterpart node to the interactions to check again, because it might happen,
     * that a path was found before that uses an XOr that now gets eliminated, because of a cycle further down in the XOR branch.
     * In that case the previously found path becomes invalid.
     */
    private void addCounterpartNodeToPathConsiderationAgain(IPublicNode node) {
        IPublicNode counterpartNode = getCounterpartNode(node);
        if (counterpartNode != null && !interactionsToCheck.contains(counterpartNode)) {
            interactionsToCheck.add(counterpartNode);
        }
    }

    /**
     * Removes the edges from the parents to the node and the node itself
     */
    private void removeParentEdgesAndNode(IDirectedGraph<Edge<IPublicNode>, IPublicNode> graph, List<IPublicNode> parents, IPublicNode node, boolean removeNode) {
        for (IPublicNode parent : parents) {
            graph.removeEdge(graph.getEdge(parent, node));
        }
        if (removeNode)
            graph.removeVertex(node);
    }

    /**
     * Iterates from a node upwards, until it either finds a XOR fork or the start node
     */
    private void eliminateContextUntilXOROrStart(IDirectedGraph<Edge<IPublicNode>, IPublicNode> graph, IPublicNode node, List<IPublicNode> toEliminate) {

        //If we arrive at the start event we return
        if (node.getName().equals("start")) {
            toEliminate.add(node);
            return;
        }

        /**
         * If we arrive at an XOR_fork, we return iff the corresponding XOR merge is not present in the nodes to be eliminated.
         * That is, as the presence would mean, that the node we started the traversal at is not on an XOR branch, but after a complete XOR-subgraph
         * */
        if ((node.getName().startsWith("XOR") && !node.getName().endsWith("_m") && toEliminate.stream().noneMatch(n -> n.getName().equals(node.getName() + "_m")))) {
            toEliminate.add(node);
            return;
        }

        toEliminate.add(node);

        /**
         * It suffices to only look at one parent at a time.
         * If there are multiple parents that means we are looking at a gateway merge.
         * We are only interested in arriving at the start element, or a gateway fork, therefore we can ignore multiple parents
         * */
        IPublicNode singleParent = new ArrayList<>(graph.getDirectPredecessors(node)).get(0);

        eliminateContextUntilXOROrStart(graph, singleParent, toEliminate);
    }


    /**
     * Helper functions========================================================================================================================================================
     *
     * @return the graph the given node belongs to
     */

    private IDirectedGraph<Edge<IPublicNode>, IPublicNode> getGraphToNode(IPublicNode node) {

        if (node instanceof Receive || node instanceof Send) {
            String[] participants = getParticipantsOfNode(node);

            boolean isParticipant2OrReceiver = node.getName().contains("(p2)") || node.getName().contains("(r)");

            if (isParticipant2OrReceiver) {
                return publicModelsByRole.get(participantsByName.get(participants[1]));
            } else {
                return publicModelsByRole.get(participantsByName.get(participants[0]));
            }
        } else if (node instanceof Event) {
            return publicModelsByRole.get(((Event) node).getRole());
        } else {
            throw new IllegalArgumentException("Node to get graph to must either be an interaction or an event");
        }

    }


    private IPublicNode getCounterpartNode(IPublicNode node) {
        String[] counterpartNameAndRole = getCounterpartInteractionNameAndRole(node);

        String nameOfCounterpartInteraction = counterpartNameAndRole[0];
        String counterpartRole = counterpartNameAndRole[1];

        IDirectedGraph<Edge<IPublicNode>, IPublicNode> counterpartGraph = publicModelsByRole.get(participantsByName.get(counterpartRole));

        if (counterpartGraph == null) {
            outputHandler.printEasySoundness("Counterpart Graph has already been eliminated, cannot fetch the counterpart node anymore");
            return null;
        }

        //If counterpart node cannot be found in counterpartGraph it has been eliminated before
        List<IPublicNode> counterpartNode = counterpartGraph.getVertices().stream()
                .filter(n -> n.getName().equals(nameOfCounterpartInteraction)).collect(Collectors.toList());

        if (counterpartNode.isEmpty()) {
            return null;
        } else {
            return counterpartNode.get(0);
        }

    }

    private IPublicNode getCounterpartNode(IPublicNode node, IDirectedGraph<Edge<IPublicNode>, IPublicNode> counterpartGraph) {
        String[] counterpartNameAndRole = getCounterpartInteractionNameAndRole(node);

        String nameOfCounterpartInteraction = counterpartNameAndRole[0];

        return counterpartGraph.getVertices().stream()
                .filter(n -> n.getName().equals(nameOfCounterpartInteraction)).collect(Collectors.toList()).get(0);
    }

    private String[] getCounterpartInteractionNameAndRole(IPublicNode node) {
        String[] participants = getParticipantsOfNode(node);

        String nodeName = node.getName();

        boolean isParticipant2 = nodeName.contains("(p2)"), isReceiver = nodeName.contains("(r)"), isParticipant1 = nodeName.contains("(p1)"), isSender = nodeName.contains("(s)");

        //Name of the counterpart interaction
        String toReplace = isParticipant2 ? "(p2)" : isReceiver ? "(r)" : isParticipant1 ? "(p1)" : "(s)";
        String replaceThrough = isParticipant2 ? "(p1)" : isReceiver ? "(s)" : isParticipant1 ? "(p2)" : "(r)";

        String nameOfCounterpartInteraction = nodeName.replace(toReplace, replaceThrough);

        //Graph of the counterpart role
        String counterpartRole = (isParticipant2 || isReceiver) ? participants[0] : participants[1];

        return new String[]{nameOfCounterpartInteraction, counterpartRole};
    }

    /**
     * Returns the participant of the interaction node
     */
    private String getParticipantOfNode(IPublicNode node) {
        boolean isSenderOrParticipant1 = node.getName().contains("(s)") || node.getName().contains("(p1)");

        String[] participantsOfNode = getParticipantsOfNode(node);

        return participantsOfNode[isSenderOrParticipant1 ? 0 : 1];
    }

    /**
     * Returns both participants of the interaction node
     */
    private String[] getParticipantsOfNode(IPublicNode node) {
        String participant1 = "", participant2 = "";

        Matcher matcher = regEx.matcher(node.getName());
        boolean found = matcher.find();
        if (found) {
            String[] participantsSplit = matcher.group(0).split(">");
            participant1 = participantsSplit[0];
            participant2 = participantsSplit[1];
        } else {
            throw new IllegalStateException("For some reason the Node is fucked up");
        }

        return new String[]{participant1, participant2};
    }

    private IPublicNode getNextInTracePreferUnvisited(List<IPublicNode> parents) {

        List<IPublicNode> parentsWorkingSet = new ArrayList<>();

        for (IPublicNode parent : parents) {
            if (!visited.contains(parent)) {
                parentsWorkingSet.add(parent);
            }
        }

        if (parentsWorkingSet.isEmpty()) {
            parentsWorkingSet = new ArrayList<>(parents);
        }

        int randomIndex = new Random().nextInt(parentsWorkingSet.size());
        return parentsWorkingSet.get(randomIndex);

    }

    /**
     * The interactions to check are filled based on a reversed BFS (BFS from end to start)
     * This structures the rest of the process in a way where the deeper rooted nodes are considered first and
     * therefore the necessity of additional checks is minimized
     * Additional checks occur every time when a path is found from a node to a start node that goes through a graph/an XOR branch that gets eliminated afterwards
     */
    private List<IPublicNode> getInteractionsToCheckInOrderBFS(IDirectedGraph<Edge<IPublicNode>, IPublicNode> graph, IPublicNode endNode) {
        List<IPublicNode> queue = new ArrayList<>(Collections.singleton(endNode));
        List<IPublicNode> explored = new ArrayList<>(Collections.singletonList(endNode));

        List<IPublicNode> nodesToConsiderInOrder = new ArrayList<>(Collections.singletonList(endNode));
        while (!queue.isEmpty()) {

            IPublicNode currNode = queue.remove(0);

//            if (currNode.getName().equals("start") && queue.isEmpty()) {
//                return nodesToConsiderInOrder;
//            }

            if (currNode instanceof Receive || currNode instanceof Send) {
                nodesToConsiderInOrder.add(currNode);
            }

            List<IPublicNode> parentsOfCurr = new ArrayList<>(graph.getDirectPredecessors(currNode));

            for (IPublicNode parent : parentsOfCurr) {
                if (!explored.contains(parent)) {
                    explored.add(parent);
                    queue.add(parent);
                }
            }
        }
        return nodesToConsiderInOrder;
    }

}
