package at.ac.c3pro.util;

import at.ac.c3pro.chormodel.Choreography;
import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.node.*;
import org.jbpt.graph.abs.IDirectedGraph;
import org.jbpt.hypergraph.abs.IGObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EasySoundnessChecker {

    Map<Role, IDirectedGraph<Edge<IPublicNode>, IPublicNode>> publicModelsByRole;

    List<IPublicNode> visited;
    Map<String, Role> participantsByName;
    Map<Role, IPublicNode> endNodes;

    List<List<IPublicNode>> traces;

    //Tracks nodes that are eliminated, meaning they are contained in a cycle
    Set<IPublicNode> eliminated;

    boolean continueSearchForCyclicWaits;

    /*Maps a node to an interaction it is conditioned by meaning
     * that for an entry x->y: x can only be executed if y has been executed before
     * */
    Map<IPublicNode, List<IPublicNode>> conditionMap;

    Pattern regEx = Pattern.compile("P_\\d+>P_\\d+");
    ;

    public EasySoundnessChecker(Choreography choreography) {
        visited = new ArrayList<>();
        participantsByName = new HashMap<>();
        endNodes = new HashMap<>();
        traces = new ArrayList<>();
        eliminated = new HashSet<>();
        publicModelsByRole = new HashMap<>();
        conditionMap = new HashMap<>();
        continueSearchForCyclicWaits = true;
        setupDeconstructionOfPuModels(choreography);
    }

    public void run() {
        //Step 1: Check for cyclic waits
        this.checkForCyclicWaits();
        //Step 2: Check other property
    }

    private void setupDeconstructionOfPuModels(Choreography choreo) {

        for (Role role : choreo.collaboration.roles) {
            //Save role
            participantsByName.put(role.getName(), role);

            //Save mapping Role -> Digraph of public model
            IDirectedGraph<Edge<IPublicNode>, IPublicNode> puModelGraph = choreo.collaboration.R2PuM.get(role).getdigraph();
            publicModelsByRole.put(role, puModelGraph);

            //Save end-nodes of a roles public model
            List<IPublicNode> nodes = new ArrayList<>(puModelGraph.getVertices());

            for (IPublicNode node : nodes) {
                if (node.getName().equals("end")) {
                    this.endNodes.put(role, node);
                    break;
                }
            }
        }
    }

    private void checkForCyclicWaits() {

        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println("Begin Check for cyclic waits");
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

        //Get one arbitrary end node and corresponding graph
        Role firstRole = new ArrayList<>(participantsByName.values()).get(0);
        IPublicNode firstEndNode = endNodes.get(firstRole);
        IDirectedGraph<Edge<IPublicNode>, IPublicNode> firstGraph = publicModelsByRole.get(firstRole);

        int ctr = 0;

        while (ctr < 7) {

            //Search one trace at a time, either a cycle or a trace from end to some start
            List<IPublicNode> trace = new ArrayList<>();

            searchTrace(firstGraph, firstEndNode, trace);

            System.out.println("Trace found: ");
            System.out.println(trace.stream().map(IGObject::getName).collect(Collectors.joining(" --> ")));
            System.out.println("---------------------------------------------------------------------------------------------------------");
            System.out.println();

            traces.add(trace);

            //Filter out all gateways
            List<IPublicNode> traceOnlyInteractions = trace.stream().filter(node -> !(node instanceof Gateway)).collect(Collectors.toList());

            //If there are duplicates in the traceOnlyInteractions, that means that we found a cycle
            Set<IPublicNode> traceOnlyInteractionsSet = new HashSet<>(traceOnlyInteractions);

            if (traceOnlyInteractionsSet.size() < traceOnlyInteractions.size()) {
                eliminateCyclesFromConsideration(traceOnlyInteractions);
            }

            ctr++;

        }
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println("End Check for cyclic waits");
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    }

    private void searchTrace(IDirectedGraph<Edge<IPublicNode>, IPublicNode> graph, IPublicNode startingNode, List<IPublicNode> trace) {

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
                IPublicNode counterpartNode = getCounterpartNode(startingNode, graphOfOtherParticipant);

                if (!visited.contains(counterpartNode)) {
                    searchTrace(graphOfOtherParticipant, counterpartNode, trace);
                    continueWithParents = false;
                }

            } else if (isSynchronousTask) {
                //CurrNode is Send of a synchronous task -> consider corresponding Receive next
                IDirectedGraph<Edge<IPublicNode>, IPublicNode> graphOfOtherParticipant = publicModelsByRole.get(participantsByName.get(participant2));
                IPublicNode counterpartNode = getCounterpartNode(startingNode, graphOfOtherParticipant);

                if (!visited.contains(counterpartNode)) {
                    searchTrace(graphOfOtherParticipant, counterpartNode, trace);
                    continueWithParents = false;
                }


            } else {
                //CurrNode is Receive of a MX or HOW -> Conditioned by corresponding Send
                if (isMessageExchangeOrHOW) {
                    IDirectedGraph<Edge<IPublicNode>, IPublicNode> graphOfOtherParticipant = publicModelsByRole.get(participantsByName.get(participant1));
                    IPublicNode counterpartNode = getCounterpartNode(startingNode, graphOfOtherParticipant);

                    searchTrace(graphOfOtherParticipant, counterpartNode, trace);
                    continueWithParents = false;
                }

            }
        }

        if (continueWithParents) {
            //Get one parent of the current graph to continue execution with
            List<IPublicNode> parents = new ArrayList<>(graph.getDirectPredecessors(startingNode));

            //TODO: integrate eliminated into consideration
            IPublicNode randomPossibleParent = getNextInTracePreferUnvisited(parents);
//            IPublicNode randomPossibleParent = parents.get(0);

            searchTrace(graph, randomPossibleParent, trace);
        }
    }

    private void eliminateCyclesFromConsideration(List<IPublicNode> trace) {
        Set<IPublicNode> helperSetDups = new HashSet<>();

        //There can only be one cycle node at a time, as the searchTrace method returns at the first occurence of a cycle
        IPublicNode cycleNode = trace.stream().filter(node -> !helperSetDups.add(node)).collect(Collectors.toList()).get(0);
        //Get index of the first occurence of the cycle node (cycle begin)
        int cycleStartIndex = trace.indexOf(cycleNode);

        List<IPublicNode> cycleTrace = trace.subList(cycleStartIndex, trace.size());

        System.out.println("Cycle found: ");
        System.out.println(cycleTrace.stream().map(IGObject::getName).collect(Collectors.joining(" --> ")));
        System.out.println();


        //For types HOW, MX and RS collect all interactions in the cycleTrace and their counterpart nodes. Ressource Share can be ignored, as the non-executableness of it for one participant does not mean the counterpart cannot fire
        //TODO: Janik fragen, ob das wirklich der Fall ist

        Set<IPublicNode> eliminateBecauseOfCycle = new HashSet<>();

        for (IPublicNode node : cycleTrace) {
            eliminateBecauseOfCycle.add(node);
            if (!node.getName().startsWith("R: "))
                eliminateBecauseOfCycle.add(getCounterpartNode(node));
        }

        for (IPublicNode nodeToBeEliminated : eliminateBecauseOfCycle) {
            System.out.println("Checking implications of interaction" + nodeToBeEliminated + " being eliminated");
            eliminateContext(nodeToBeEliminated);
        }

        System.out.println();
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
    private void eliminateContext(IPublicNode node) {

        IDirectedGraph<Edge<IPublicNode>, IPublicNode> graphOfNode = getGraphToNode(node);

        List<IPublicNode> toEliminate = new ArrayList<>();


        //Step 1: Traverse the graph backwards until you find either a XOR node or the start node
        //In case of XOR, the other branches of XOR can potentially be used to get from start to end
        //In case of start, the whole participant cannot execute
        eliminateContextUntilXOROrStart(graphOfNode, node, toEliminate);

        Set<IPublicNode> toEliminateSet = new HashSet<>();

        IPublicNode stopNode = toEliminate.remove(toEliminate.size() - 1);

        if (stopNode instanceof Event) {
            //Stopped at start
            System.out.println("All nodes of the participants are not executable");
            eliminated.addAll(graphOfNode.getVertices());
            return;
        } else if (stopNode instanceof Gateway) {
            //Stopped at XOR

            //Get the first element on the XOR branch
            IPublicNode firstNodeOnXORbranch = toEliminate.remove(toEliminate.size() - 1);
            String xorMergeName = stopNode.getName() + "_m";

            //Step 2 Eliminate all children of the uppermost AND. This is because as the node inside of the AND-Branch cannot execute, the AND branch is never able to conclude
            List<IPublicNode> toEliminateAND = new ArrayList<>();

            eliminateContextXORBranch(graphOfNode, firstNodeOnXORbranch, toEliminateAND, xorMergeName);

            System.out.println("Nodes eliminated: " + toEliminateAND.stream().map(IGObject::getName).collect(Collectors.joining(",")));

            eliminated.addAll(toEliminateAND);

        }


    }

    /**
     * Iterates from the given AND-gateway downwards, eliminating all the interactions on all paths to its merge node
     */
    private void eliminateContextXORBranch(IDirectedGraph<Edge<IPublicNode>, IPublicNode> graph, IPublicNode node, List<IPublicNode> toEliminate, String gatewayMergeName) {

        if (node.getName().equals(gatewayMergeName)) {
            toEliminate.add(node);
            return;
        }

        List<IPublicNode> children = new ArrayList<>(graph.getDirectSuccessors(node));

        for (IPublicNode child : children) {
            eliminateContextXORBranch(graph, child, toEliminate, gatewayMergeName);
        }

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

        String[] participants = getParticipantsOfNode(node);

        boolean isParticipant2OrReceiver = node.getName().contains("(p2)") || node.getName().contains("(r)");

        if (isParticipant2OrReceiver) {
            return publicModelsByRole.get(participantsByName.get(participants[1]));
        } else {
            return publicModelsByRole.get(participantsByName.get(participants[0]));
        }
    }


    private IPublicNode getCounterpartNode(IPublicNode node) {
        String[] counterpartNameAndRole = getCounterpartInteractionNameAndRole(node);

        String nameOfCounterpartInteraction = counterpartNameAndRole[0];
        String counterpartRole = counterpartNameAndRole[1];

        IDirectedGraph<Edge<IPublicNode>, IPublicNode> counterpartGraph = publicModelsByRole.get(participantsByName.get(counterpartRole));

        return counterpartGraph.getVertices().stream()
                .filter(n -> n.getName().equals(nameOfCounterpartInteraction)).collect(Collectors.toList()).get(0);
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


}
