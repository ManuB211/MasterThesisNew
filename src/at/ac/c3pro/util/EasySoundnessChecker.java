package at.ac.c3pro.util;

import at.ac.c3pro.chormodel.*;
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

    //    Map<Role, IDirectedGraph<Edge<IPublicNode>, IPublicNode>> publicModelsByRole;
    Map<Role, IPublicModel> publicModelsByRole;

    List<IPublicNode> visited;
    Map<String, Role> participantsByName;
    Map<Role, IPublicNode> endNodes;

    Map<Role, Integer> eliminationStepsForParticipantsGraph;

    List<List<IPublicNode>> traces;

    //Stores all the interactions from which we try to find traces back to a start node
    List<IPublicNode> interactionsToCheck;

    /**
     * Stores for each edge x a set of edges {y1,...,yn} it is incompatible with;
     * meaning when x is executed, none of y1,...,yn can be executed in the same trace
     * this is due to the fact that x and y1,...,yn are on different branches of the same XOR
     */
    Map<Edge<IPublicNode>, Set<Edge<IPublicNode>>> incompatibilityMap;
    Map<Role, List<XorGateway>> xors;

    List<IChoreographyNode> xorWithDirectEdgeToMerge;


    Pattern regEx = Pattern.compile("P_\\d+>P_\\d+");

    OutputHandler outputHandler;

    public EasySoundnessChecker(Choreography choreography, boolean visualize) throws IOException {

        outputHandler = new OutputHandler(OutputHandler.OutputType.EASY_SOUNDNESS);

        visited = new ArrayList<>();
        participantsByName = new HashMap<>();
        endNodes = new HashMap<>();
        traces = new ArrayList<>();
        publicModelsByRole = new HashMap<>();
        interactionsToCheck = new ArrayList<>();
        this.visualize = visualize;
        eliminationStepsForParticipantsGraph = new HashMap<>();
        incompatibilityMap = new HashMap<>();
        xors = new HashMap<>();
        xorWithDirectEdgeToMerge = new ArrayList<>();
        setupDeconstructionOfPuModels(choreography);
    }

    public void run() throws IOException, InterruptedException {
        try {
            //Step 1: Check for cyclic waits
            checkForCyclicWaits();

            //Step 2: Check for valid traces
            searchForValidTraces();


        } catch (Exception e) {
            outputHandler.closePrintWriter();
            throw e;
        }
        outputHandler.closePrintWriter();
    }

    private void searchForValidTraces() {
        outputHandler.printEasySoundness(OutputHandler.EasySoundnessAnalyisBlocks.START_VALID_TRACES);

        //Step 2.1 Compute all traces from start to sink
        computeAllTracesStartToSink();

        //Step 2.2 Build incompatibilityMap
        buildIncompatibilityMap();


        outputHandler.printEasySoundness(OutputHandler.EasySoundnessAnalyisBlocks.STOP_VALID_TRACES);
    }

    private void computeAllTracesStartToSink() {
        //1. Build the complete graph, meaning a combination of all public models
        MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> completeGraph = buildCompleteGraph();

        if (completeGraph != null) {
            //2. Compute topological order using Kahns algorithm
            List<IPublicNode> topologicalOrder = computeTopologicalOrder(completeGraph, completeGraph.getVertices().stream().filter(v -> v.getName().equals("startGlobal")).collect(Collectors.toList()).get(0));

            outputHandler.printEasySoundness("----------------------------------------------");
            outputHandler.printEasySoundness("Starting computation of traces to all nodes");
            outputHandler.printEasySoundness("----------------------------------------------");
            //3. For all nodes compute all paths that end at said node in topological order
            Map<IPublicNode, List<List<Edge<IPublicNode>>>> tracesToAllNodes = computeTracesToAllNodes2(completeGraph, new ArrayList<>(topologicalOrder));

            for (Map.Entry<IPublicNode, List<List<Edge<IPublicNode>>>> entry : tracesToAllNodes.entrySet()) {
                outputHandler.printEasySoundness("Traces that end at: " + entry.getKey().getName());

                for (List<Edge<IPublicNode>> trace : entry.getValue()) {
                    outputHandler.printEasySoundness(trace.stream().map(Edge::toString).collect(Collectors.joining(" , ")));
                }
                outputHandler.printEasySoundness("\n");
            }

            IPublicNode endGlobal = tracesToAllNodes.keySet().stream().filter(t -> t.getName().equals("endGlobal")).collect(Collectors.toList()).get(0);

            //removeAllIncompatibleTraces(incompatibilityMap, tracesToAllNodes.get(endGlobal));

        } else {
            outputHandler.printEasySoundness("No topological order able to be computed for a graph that is null");
        }
    }

    /*private void removeAllIncompatibleTraces(Map<Edge<IPublicNode>, Set<Edge<IPublicNode>>> incomp, List<List<IPublicNode>> traces) {

        List<List<IPublicNode>> toRemove = new ArrayList<>();

        for (Map.Entry<Edge<IPublicNode>, Set<Edge<IPublicNode>>> incompEntry : incomp.entrySet()) {

            Edge<IPublicNode> currNode = incompEntry.getKey();

            for (List<IPublicNode> traceToCheck : traces) {
                if (traceToCheck.contains(currNode)) {

                    for (IPublicNode incompatibleWithCurr : incompEntry.getValue()) {
                        if (traceToCheck.contains(incompatibleWithCurr)) {

                            outputHandler.printEasySoundness(OutputHandler.EasySoundnessAnalyisBlocks.NODE_DELIM);
                            outputHandler.printEasySoundness("A trace has been found that contains incompatible nodes: " + currNode + " and " + incompatibleWithCurr);
                            outputHandler.printEasySoundness("Therefore the following trace has been eliminated");
                            outputHandler.printEasySoundness(traceToCheck.stream().map(IGObject::getName).collect(Collectors.joining(" -> ")));
                            toRemove.add(traceToCheck);
                            break;
                        }
                    }
                }
            }


        }

        for (List<IPublicNode> traceToRemove : toRemove) {
            traces.remove(traceToRemove);
        }
    }*/

    private Map<IPublicNode, List<List<Edge<IPublicNode>>>> computeTracesToAllNodes2(MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> graph, List<IPublicNode> topOrder) {
        Map<IPublicNode, List<List<Edge<IPublicNode>>>> rst = new HashMap<>();

        //Remove global start as it has no parent elements
        topOrder.remove(0);

        for (IPublicNode currNode : topOrder) {

            //Get all predecessors
            List<IPublicNode> parents = new ArrayList<>(graph.getDirectPredecessors(currNode));

            List<List<Edge<IPublicNode>>> tracesEndingAtCurrNode = new ArrayList<>();

            if (parents.isEmpty()) {
                throw new IllegalStateException("No parents found, there has to be something wrong with the topological order");
            }

            if (currNode instanceof XorGateway) {

                for (IPublicNode parent : parents) {
                    List<List<Edge<IPublicNode>>> tracesEndingAtParent = rst.get(parent);

                    //Children of global start
                    if (tracesEndingAtParent.isEmpty()) {
                        List<Edge<IPublicNode>> newTrace = new ArrayList<>();
                        newTrace.add(graph.getEdge(parent, currNode));
                        tracesEndingAtCurrNode.add(newTrace);
                        continue;
                    }

                    //Take the traces ending at parent, add the current node and add the new trace as trace ending at the current node
                    for (List<Edge<IPublicNode>> singleTraceEndingAtParent : tracesEndingAtParent) {
                        List<Edge<IPublicNode>> newTraceEndingAtCurr = new ArrayList<>(singleTraceEndingAtParent);
                        newTraceEndingAtCurr.add(graph.getEdge(parent, currNode));
                        tracesEndingAtCurrNode.add(newTraceEndingAtCurr);
                    }
                }

            } else {
                //Build list containing all traces ending at all parents
                List<List<List<Edge<IPublicNode>>>> allTraces = new ArrayList<>();

                for (IPublicNode parent : parents) {


                    //startGlobal -> children
                    if (rst.isEmpty()) {

//                        List<Edge<IPublicNode>> startEdge = new ArrayList<>();
//                        startEdge.add(graph.getEdge(parent, currNode));
//                        List<List<Edge<IPublicNode>>> startTrace = new ArrayList<>();
//                        startTrace.add(startEdge);
//                        rst.put(parent, startTrace);
//
//                        allTraces.add(startTrace);
                    } else {
                        allTraces.add(rst.get(parent));
                    }


                }


                //Get all edges to current node
                List<Edge<IPublicNode>> edgesToCurr = parents.stream().map(p -> graph.getEdge(p, currNode)).collect(Collectors.toList());

                //Build all combinations using a backtracking approach
                List<List<Edge<IPublicNode>>> tracesEndingAtCurr = getTracesEndingAtNode2(allTraces, currNode, edgesToCurr);

                //TODO: Sort elements according to topological order again or does this not matter at all?

                tracesEndingAtCurrNode.addAll(tracesEndingAtCurr);
            }

            rst.put(currNode, tracesEndingAtCurrNode);
        }

        return rst;
    }

    private List<List<Edge<IPublicNode>>> getTracesEndingAtNode2(List<List<List<Edge<IPublicNode>>>> allTraces, IPublicNode node, List<Edge<IPublicNode>> edgesToNode) {
        List<List<Edge<IPublicNode>>> rst = new ArrayList<>();

        getTracesEndingAtNodeRec2(allTraces, node, edgesToNode, 0, new ArrayList<>(), rst);

        return rst;
    }

    private void getTracesEndingAtNodeRec2(List<List<List<Edge<IPublicNode>>>> allTraces, IPublicNode node, List<Edge<IPublicNode>> edgesToNode, int ctr, List<Edge<IPublicNode>> currTrace, List<List<Edge<IPublicNode>>> rst) {

        if (ctr == allTraces.size() || allTraces.stream().allMatch(Objects::isNull)) {
            currTrace.addAll(edgesToNode);
            rst.add(new ArrayList<>(currTrace));
            return;
        }

        List<List<Edge<IPublicNode>>> tracesOfCurrParticipant = allTraces.get(ctr);
        for (List<Edge<IPublicNode>> singleTraceOfCurrParticipant : tracesOfCurrParticipant) {

            //Needed for the backtracking
            int amountAddedElements = 0;

            //Add all nodes that are not already in the trace
            for (Edge<IPublicNode> nodeOfTrace : singleTraceOfCurrParticipant) {
                if (!currTrace.contains(nodeOfTrace)) {
                    currTrace.add(nodeOfTrace);
                    amountAddedElements++;
                }
            }

            getTracesEndingAtNodeRec2(allTraces, node, edgesToNode, ctr + 1, currTrace, rst);

            //Remove the added trace of the current participant from the end of the current trace again
            for (int i = 0; i < amountAddedElements; i++) {
                currTrace.remove(currTrace.size() - 1);
            }
        }
    }


    /**
     * Computes for all nodes the paths that stop at this node.
     */
    private Map<IPublicNode, List<List<IPublicNode>>> computeTracesToAllNodes(MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> graph, List<IPublicNode> topOrder) {

        Map<IPublicNode, List<List<IPublicNode>>> rst = new HashMap<>();

        //Add the global start with a list containing only itself as the first element
        IPublicNode startGlobal = topOrder.remove(0);
        List<List<IPublicNode>> traces = new ArrayList<>();
        traces.add(Collections.singletonList(startGlobal));
        rst.put(startGlobal, traces);

        for (IPublicNode currNode : topOrder) {

            List<IPublicNode> parents = new ArrayList<>(graph.getDirectPredecessors(currNode));

            List<List<IPublicNode>> tracesEndingAtCurrNode = new ArrayList<>();

            if (parents.isEmpty()) {
                throw new IllegalStateException("No parents found, there has to be something wrong with the topological order");
            }

            //If the currently visited node is an XOR_merge, the traces ending at that node are the traces ending at both parents plus the current node
            //In all other cases, the traces ending at a node are the union of all traces ending at the parents plus the current node
            if (currNode instanceof XorGateway) {

                for (IPublicNode parent : parents) {

                    List<List<IPublicNode>> tracesEndingAtParent = rst.get(parent);

                    if (tracesEndingAtParent.isEmpty()) {
                        throw new IllegalStateException("WTF");
                    }

                    //Take the traces ending at parent, add the current node and add the new trace as trace ending at the current node
                    for (List<IPublicNode> singleTraceEndingAtParent : tracesEndingAtParent) {
                        List<IPublicNode> newTraceEndingAtCurr = new ArrayList<>(singleTraceEndingAtParent);
                        newTraceEndingAtCurr.add(currNode);
                        tracesEndingAtCurrNode.add(newTraceEndingAtCurr);
                    }
                }

            } else {

                //Build list containing all traces ending at all parents
                List<List<List<IPublicNode>>> allTraces = new ArrayList<>();

                for (IPublicNode parent : parents) {
                    allTraces.add(rst.get(parent));
                }

                //Build all combinations using a backtracking approach
                List<List<IPublicNode>> tracesEndingAtCurr = getTracesEndingAtNode(allTraces, currNode);

                //TODO: Sort elements according to topological order again or does this not matter at all?

                tracesEndingAtCurrNode.addAll(tracesEndingAtCurr);
            }

            rst.put(currNode, tracesEndingAtCurrNode);
        }

        return rst;
    }


    private List<List<IPublicNode>> getTracesEndingAtNode(List<List<List<IPublicNode>>> allTraces, IPublicNode node) {
        List<List<IPublicNode>> rst = new ArrayList<>();

        getTracesEndingAtNodeRec(allTraces, node, 0, new ArrayList<>(), rst);

        return rst;
    }

    private void getTracesEndingAtNodeRec(List<List<List<IPublicNode>>> allTraces, IPublicNode node, int ctr, List<IPublicNode> currTrace, List<List<IPublicNode>> rst) {

        if (ctr == allTraces.size()) {
            currTrace.add(node);
            rst.add(new ArrayList<>(currTrace));
            return;
        }

        List<List<IPublicNode>> tracesOfCurrParticipant = allTraces.get(ctr);
        for (List<IPublicNode> singleTraceOfCurrParticipant : tracesOfCurrParticipant) {

            //Needed for the backtracking
            int amountAddedElements = 0;

            //Add all nodes that are not already in the trace
            for (IPublicNode nodeOfTrace : singleTraceOfCurrParticipant) {
                if (!currTrace.contains(nodeOfTrace)) {
                    currTrace.add(nodeOfTrace);
                    amountAddedElements++;
                }
            }

            getTracesEndingAtNodeRec(allTraces, node, ctr + 1, currTrace, rst);

            //Remove the added trace of the current participant from the end of the current trace again
            for (int i = 0; i < amountAddedElements; i++) {
                currTrace.remove(currTrace.size() - 1);
            }
        }
    }


    /**
     * Computes the topological ordering from our complete graph using Kahns algorithm
     * The only node that has incoming degree of 0 is the globalStart
     */
    private List<IPublicNode> computeTopologicalOrder(MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> graph, IPublicNode startGlobal) {
        List<IPublicNode> rst = new ArrayList<>();

        //Set of Connections that were removed for Kahns algorithm to work but need to be added again afterwards
        Set<IPublicNode[]> toAddAfterTopOrder = new HashSet<>();

        List<IPublicNode> queue = new ArrayList<>();
        queue.add(startGlobal);

        //Compute and store the amount of incoming edges for each vertex
        Map<IPublicNode, Integer> amountInEdges = graph.getVertices().stream().collect(Collectors.toMap(v -> v, v -> graph.getDirectPredecessors(v).size()));

        while (!queue.isEmpty()) {

            IPublicNode currNode = queue.remove(0);
            rst.add(currNode);
            amountInEdges.remove(currNode);

            //Decrement for each predecessor of the current node the amount of incoming edges by 1
            for (IPublicNode child : graph.getDirectSuccessors(currNode)) {

                /*
                 * If child is of type resource share or synchronous task, that means that it basically has an undirected edge (two contrary directed edges)
                 * This would break Kahns algorithm because it can never be resolved.
                 * Therefore we modify it, so that once the sync. task or resource share is reached,
                 * it gets treated as the sender and the incoming edge from its counterpart is removed
                 */
                if (child.getName().startsWith("S: ") || child.getName().startsWith("R: ")) {

                    String counterpartNodeName = getCounterpartInteractionNameAndRole(child)[0];
                    IPublicNode counterpart = graph.getVertices().stream().filter(v -> v.getName().equals(counterpartNodeName)).collect(Collectors.toList()).get(0);

                    //If the edge from the counterpart to the currently visited child still exists it is removed and the in-degree is updated accordingly
                    if (graph.getEdge(counterpart, child) != null) {
                        toAddAfterTopOrder.add(new IPublicNode[]{counterpart, child});
                        graph.removeEdge(graph.getEdge(counterpart, child));
                        amountInEdges.computeIfPresent(child, (k, v) -> v - 1);
                    }
                }

                amountInEdges.computeIfPresent(child, (k, v) -> v - 1);
            }

            queue.addAll(amountInEdges.entrySet().stream().filter(v -> v.getValue() == 0).filter(v -> !queue.contains(v.getKey())).map(v -> v.getKey()).collect(Collectors.toList()));

            System.out.println("Current queue:");
            System.out.println(queue.stream().map(IGObject::getName).collect(Collectors.joining(", ")));
            System.out.println("---------------------------------------------------------");
            System.out.println("Current Topological Ordering: ");
            System.out.println(rst.stream().map(IGObject::getName).collect(Collectors.joining(", ")));
            System.out.println("---------------------------------------------------------");

        }

        if (amountInEdges.entrySet().stream().anyMatch(v -> v.getValue() != 0)) {
            throw new IllegalStateException("Congratulations, my cycle checking sucks because there still appears to be one");
        }

        outputHandler.printEasySoundness("The following topological order has been found for the complete graph:");
        outputHandler.printEasySoundness(rst.stream().map(IGObject::getName).collect(Collectors.joining(",\n")));

        for (IPublicNode[] toAdd : toAddAfterTopOrder) {
            graph.addEdge(toAdd[0], toAdd[1]);
        }


        return rst;
    }


    private MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> buildCompleteGraph() {

        Map<Role, IDirectedGraph<Edge<IPublicNode>, IPublicNode>> puMs = publicModelsByRole.entrySet().stream().filter(entry -> entry.getValue() != null).collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getdigraph()));

        if (!puMs.isEmpty()) {

            MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> rst = new MultiDirectedGraph<Edge<IPublicNode>, IPublicNode>();

            //Add global start and end
            Event startGlobal = new Event("startGlobal");
            Event endGlobal = new Event("endGlobal");

            rst.addVertex(startGlobal);
            rst.addVertex(endGlobal);

            for (Map.Entry<Role, IDirectedGraph<Edge<IPublicNode>, IPublicNode>> puM : puMs.entrySet()) {

                Role currRole = puM.getKey();

                for (IPublicNode vertex : puM.getValue().getVertices()) {

                    if (vertex instanceof Event) {
                        Event newEvent = new Event(getNameWithRole(vertex, currRole));
                        rst.addVertex(newEvent);

                        if (vertex.getName().startsWith("start")) {
                            rst.addEdge(startGlobal, newEvent);
                        } else {
                            rst.addEdge(newEvent, endGlobal);
                        }
                    }

                    if (vertex instanceof Gateway) {

                        if (vertex instanceof XorGateway) {
                            XorGateway newXor = new XorGateway(getNameWithRole(vertex, currRole));
                            rst.addVertex(newXor);
                        } else {
                            AndGateway newAnd = new AndGateway(getNameWithRole(vertex, currRole));
                            rst.addVertex(newAnd);
                        }
                    }

                    if (vertex instanceof Send || vertex instanceof Receive) {
                        rst.addVertex(vertex);
                    }
                }

                for (Edge<IPublicNode> edge : puM.getValue().getEdges()) {

                    IPublicNode source = edge.getSource(), target = edge.getTarget();

                    if ((source instanceof Receive || source instanceof Send) && (target instanceof Receive || target instanceof Send)) {
                        rst.addEdge(source, target);
                    } else if (source instanceof Receive || source instanceof Send) {
                        IPublicNode newTgt = rst.getVertices().stream().filter(vertex -> vertex.getName().equals(getNameWithRole(target, currRole))).collect(Collectors.toList()).get(0);
                        rst.addEdge(source, newTgt);
                    } else if (target instanceof Receive || target instanceof Send) {
                        IPublicNode newSrc = rst.getVertices().stream().filter(vertex -> vertex.getName().equals(getNameWithRole(source, currRole))).collect(Collectors.toList()).get(0);
                        rst.addEdge(newSrc, target);
                    } else {
                        IPublicNode newSrc = rst.getVertices().stream().filter(vertex -> vertex.getName().equals(getNameWithRole(source, currRole))).collect(Collectors.toList()).get(0);
                        IPublicNode newTgt = rst.getVertices().stream().filter(vertex -> vertex.getName().equals(getNameWithRole(target, currRole))).collect(Collectors.toList()).get(0);

                        rst.addEdge(newSrc, newTgt);
                    }
                }
            }


            /*
             * Add all the crossreferences between interactions. To avoid duplicates we track what connections we already added
             */

            //Filter out only the interactions
            List<IPublicNode> rstOnlyInteractions = rst.getVertices().stream().filter(v -> v instanceof Send || v instanceof Receive).collect(Collectors.toList());

            Map<IPublicNode, Boolean> alreadyAdded = rstOnlyInteractions.stream().collect(Collectors.toMap(e -> e, e -> false));

            //Iterate over all interactions
            for (IPublicNode vertex : rstOnlyInteractions) {

                //If the connection was not already added
                if (!alreadyAdded.get(vertex)) {

                    String counterpartNodeName = getCounterpartInteractionNameAndRole(vertex)[0];
                    IPublicNode counterpart = rstOnlyInteractions.stream().filter(v -> v.getName().equals(counterpartNodeName)).collect(Collectors.toList()).get(0);

                    //Find out what is source and what target
                    //In case Sync. Task or Res. Share, there are two arcs between both participants
                    if (vertex.getName().startsWith("S: ") || vertex.getName().startsWith("R: ")) {
                        rst.addEdge(vertex, counterpart);
                        rst.addEdge(counterpart, vertex);
                    } else {

                        //If Mess. Exch. or Handover, we check which participant it is
                        if (vertex.getName().contains(("p1")) || vertex.getName().contains("(s)")) {
                            rst.addEdge(vertex, counterpart);
                        } else {
                            rst.addEdge(counterpart, vertex);
                        }
                    }

                    alreadyAdded.put(vertex, true);
                    alreadyAdded.put(counterpart, true);

                }
            }


            boolean printCompleteGraph = true;

            if (printCompleteGraph) {
                IOUtils.toFile(
                        GlobalTimestamp.timestamp + "/EasySoundness/Test12345.dot",
                        rst != null ? rst.toDOT() : "");
            }

            return rst;
        }

        return null;
    }

    private String getNameWithRole(IPublicNode vertex, Role role) {
        return vertex.getName() + " " + role.getName();
    }


    private void buildIncompatibilityMap() {

        for (Map.Entry<Role, List<XorGateway>> xors : xors.entrySet()) {

            IPublicModel puM = publicModelsByRole.get(xors.getKey());

            if (puM != null && puM.getdigraph() != null) {
                //Get graph of the corresponding role
                IDirectedGraph<Edge<IPublicNode>, IPublicNode> graph = puM.getdigraph();

                //Iterate over the XOR Gateways from that graph
                for (XorGateway xor : xors.getValue()) {
                    List<Set<Edge<IPublicNode>>> branchInteractions = findCompatibleEdgesForOneXOR(graph, xor);
                    fillIncompatibilityMap(branchInteractions);
                }
            } else {
                outputHandler.printEasySoundness("Graph for participant " + xors.getKey().getName() + " does not exist anymore, therefore no incompatibility check needs to be performed");
            }
        }

        outputHandler.printEasySoundness(OutputHandler.EasySoundnessAnalyisBlocks.TRACE_DELIM);
        outputHandler.printEasySoundness("Computed the incompatibility map (interactions that can never be executed in the same trace)");
        for (Map.Entry<Edge<IPublicNode>, Set<Edge<IPublicNode>>> incomp : incompatibilityMap.entrySet()) {
            outputHandler.printEasySoundness("For " + incomp.getKey() + " incompatible operations are " + incomp.getValue().stream().map(Edge::toString).collect(Collectors.joining(", ")));
        }
    }


    /**
     * Takes the result of findCompatibleNodesForOneXOR and fills the incompatibility map with it
     * e.g. an input of [{IA1->IA2,IA3->IA4}, {IA5->IA6, IA7->IA8}] leads to the following entries:
     * {IA1->IA2} -> {IA5->IA6, IA7->IA8}
     * {IA3->IA4} -> {IA5->IA6, IA7->IA8}
     * {IA5->IA6} -> {IA1->A2, IA3->IA4}
     * {IA7->IA8} -> {IA1->A2, IA3->IA4}
     * <p>
     * representing that if the key is executed, none of the entries in the value set can fire in the same execution
     */
    private void fillIncompatibilityMap(List<Set<Edge<IPublicNode>>> branchEdges) {

        for (int i = 0; i < branchEdges.size(); i++) {
            for (Edge<IPublicNode> currEdge : branchEdges.get(i)) {
                for (int j = 0; j < branchEdges.size(); j++) {
                    if (i == j)
                        continue;

                    incompatibilityMap.putIfAbsent(currEdge, new HashSet<>());
                    incompatibilityMap.get(currEdge).addAll(branchEdges.get(j));
                }
            }
        }
    }

    /**
     * Returns a list of sets, each set contains the interactions that are in one branch of the XOR gateway.
     * e.g. a return value of [{IA1, IA2}, {IA3,IA4}] means that IA1, IA2 are on one branch of the XOR, IA3, IA4 on another
     */
    private List<Set<Edge<IPublicNode>>> findCompatibleEdgesForOneXOR(IDirectedGraph<Edge<IPublicNode>, IPublicNode> graph, XorGateway xor) {
        List<Set<Edge<IPublicNode>>> rst = new ArrayList<>();

        List<IPublicNode> succ = new ArrayList<>(graph.getDirectSuccessors(xor));

        for (IPublicNode successorOfInit : succ) {
            Set<Edge<IPublicNode>> edgesOfBranch = new HashSet<>();
            edgesOfBranch.add(graph.getEdge(xor, successorOfInit));

            getAllNodesOfBranch(graph, xor, successorOfInit, edgesOfBranch);
            rst.add(new HashSet<>(edgesOfBranch));
            edgesOfBranch.clear();
        }

        return rst;
    }

    private void getAllNodesOfBranch(IDirectedGraph<Edge<IPublicNode>, IPublicNode> graph, XorGateway xor, IPublicNode start, Set<Edge<IPublicNode>> workingSet) {

        if (start.getName().equals(xor.getName() + "_m")) {
            return;
        }

        for (IPublicNode child : new ArrayList<>(graph.getDirectSuccessors(start))) {

            workingSet.add(graph.getEdge(start, child));
            if (child.getName().equals(xor.getName() + "_m")) {
                return;
            }

            getAllNodesOfBranch(graph, xor, child, workingSet);
        }
    }


    private void setupDeconstructionOfPuModels(Choreography choreo) {

        //To fill xorsWithDirectEdgeToMerge
        Set<IChoreographyNode> xorsWithDirectEdgeToMerge = new HashSet<>();

        for (Role role : choreo.collaboration.roles) {
            //Save role
            participantsByName.put(role.getName(), role);

            //Save mapping Role -> Digraph of public model
//            IDirectedGraph<Edge<IPublicNode>, IPublicNode> puModelGraph = choreo.collaboration.R2PuM.get(role).getdigraph();
//            publicModelsByRole.put(role, puModelGraph);

            IRpstModel<Edge<IPublicNode>, IPublicNode> rpstModel = choreo.collaboration.R2PuM.get(role);
            IDirectedGraph<Edge<IPublicNode>, IPublicNode> puModelGraph = rpstModel.getdigraph();

            publicModelsByRole.put(role, choreo.collaboration.R2PuM.get(role));

            //Initialize the tracking map for elimination step as 1
            eliminationStepsForParticipantsGraph.put(role, 1);

            //Save end-nodes of a roles public model
            List<IPublicNode> nodes = new ArrayList<>(puModelGraph.getVertices());

            for (IPublicNode node : nodes) {
                //Store XORs for later (incompatibility map)
                if (node instanceof XorGateway && !node.getName().endsWith("_m")) {
                    xors.putIfAbsent(role, new ArrayList<>());
                    xors.get(role).add((XorGateway) node);

                    //Check if direct edge to merge node is present
                    for (IPublicNode child : puModelGraph.getDirectSuccessors(node)) {
                        if (child.getName().equals(node.getName() + "_m")) {
                            xorWithDirectEdgeToMerge.add((IChoreographyNode) node);
                        }
                    }
                }

                if (node.getName().equals("end")) {
                    endNodes.put(role, node);
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

        outputHandler.printEasySoundness(OutputHandler.EasySoundnessAnalyisBlocks.START_CYCLIC_WAITS);

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
        outputHandler.printEasySoundness(OutputHandler.EasySoundnessAnalyisBlocks.STOP_CYCLIC_WAITS);


        //After we have iterated all interactions and tried to find a way from them to a start node, while eliminating those that are contained in a cycle,
        //the graphs remaining are all the cyclefree paths
//        for (Map.Entry<Role, IDirectedGraph<Edge<IPublicNode>, IPublicNode>> entry : publicModelsByRole.entrySet()) {
        for (Map.Entry<Role, IPublicModel> entry : publicModelsByRole.entrySet()) {
            IDirectedGraph<Edge<IPublicNode>, IPublicNode> model = entry.getValue() != null ? entry.getValue().getdigraph() : null;
            IOUtils.toFile(
                    GlobalTimestamp.timestamp + "/EasySoundness/CycleFreePaths_" + entry.getKey().getName() + ".dot",
                    model != null ? model.toDOT() : "");
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

                IPublicModel puM = publicModelsByRole.get(participantsByName.get(participant1));
                IDirectedGraph<Edge<IPublicNode>, IPublicNode> graphOfOtherParticipant = puM != null ? puM.getdigraph() : null;


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

                IPublicModel puM = publicModelsByRole.get(participantsByName.get(participant2));
                IDirectedGraph<Edge<IPublicNode>, IPublicNode> graphOfOtherParticipant = puM != null ? puM.getdigraph() : null;
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
                    IPublicModel puM = publicModelsByRole.get(participantsByName.get(participant1));
                    IDirectedGraph<Edge<IPublicNode>, IPublicNode> graphOfOtherParticipant = puM != null ? puM.getdigraph() : null;

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
//            outputHandler.printEasySoundness(publicModelsByRole.get(participantOfNode).getVertices().stream().map(IGObject::getName).collect(Collectors.joining(",")) + "\n");
            outputHandler.printEasySoundness(publicModelsByRole.get(participantOfNode).getdigraph().getVertices().stream().map(IGObject::getName).collect(Collectors.joining(",")) + "\n");

            //As the whole graph will be eliminated, we can also eliminate all nodes from consideration for finding a path from the nodes to a start node
            interactionsToCheck.removeAll(publicModelsByRole.get(participantOfNode).getdigraph().getVertices());

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

            //Do reduction of the graph
            IPublicModel rpstModel = publicModelsByRole.get(participantOfNode);
            rpstModel.setDiGraph(graphOfNode);
            rpstModel.reduceGraph(xorWithDirectEdgeToMerge);

//            publicModelsByRole.put(participantOfNode, graphOfNode);
            publicModelsByRole.put(participantOfNode, rpstModel);

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
                IPublicModel puM = publicModelsByRole.get(participantsByName.get(participants[1]));
                return puM != null ? puM.getdigraph() : null;

            } else {
                IPublicModel puM = publicModelsByRole.get(participantsByName.get(participants[0]));
                return puM != null ? puM.getdigraph() : null;
            }
        } else if (node instanceof Event) {
            IPublicModel puM = publicModelsByRole.get(((Event) node).getRole());
            return puM != null ? puM.getdigraph() : null;
        } else {
            throw new IllegalArgumentException("Node to get graph to must either be an interaction or an event");
        }

    }

    private IPublicNode getCounterpartNode(IPublicNode node) {
        String[] counterpartNameAndRole = getCounterpartInteractionNameAndRole(node);

        String nameOfCounterpartInteraction = counterpartNameAndRole[0];
        String counterpartRole = counterpartNameAndRole[1];

        IPublicModel puM = publicModelsByRole.get(participantsByName.get(counterpartRole));
        IDirectedGraph<Edge<IPublicNode>, IPublicNode> counterpartGraph = puM != null ? puM.getdigraph() : null;

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
        return GraphHelper.performBackwardsBFSPublic(graph, endNode, true);
    }

}
