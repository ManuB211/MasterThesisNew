package at.ac.c3pro.util;

import at.ac.c3pro.chormodel.*;
import at.ac.c3pro.chormodel.exceptions.NoTracesToEndFoundException;
import at.ac.c3pro.node.*;
import org.jbpt.graph.abs.IDirectedGraph;
import org.jbpt.hypergraph.abs.IGObject;
import org.jbpt.utils.IOUtils;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EasySoundnessChecker2 {

    boolean visualize;

    //    Map<Role, IDirectedGraph<Edge<IPublicNode>, IPublicNode>> publicModelsByRole;
    Map<Role, IPublicModel> publicModelsByRole;

    List<IPublicNode> visited;
    Map<String, Role> participantsByName;

    Map<Role, Integer> eliminationStepsForParticipantsGraph;

    Map<IPublicNode, List<IPublicNode>> mapCommonSynchronousTasksToIndividualParts;

    /**
     * Stores for each edge x a set of edges {y1,...,yn} it is incompatible with;
     * meaning when x is executed, none of y1,...,yn can be executed in the same trace
     * this is due to the fact that x and y1,...,yn are on different branches of the same XOR
     */
    Map<Edge<IPublicNode>, Set<Edge<IPublicNode>>> incompatibilityMap;
    Map<Role, List<XorGateway>> xors;

    //Stores the mapping between gateways in the public models and the combined
    Map<IPublicNode, IPublicNode> mappingPuMCombinedPuM;

    List<IChoreographyNode> xorWithDirectEdgeToMerge;


    Pattern regEx = Pattern.compile("P_\\d+>P_\\d+");

    OutputHandler outputHandler;

    public EasySoundnessChecker2(Choreography choreography, boolean visualize) throws IOException {

        outputHandler = new OutputHandler(OutputHandler.OutputType.EASY_SOUNDNESS);

        visited = new ArrayList<>();
        participantsByName = new HashMap<>();
        publicModelsByRole = new HashMap<>();
        this.visualize = visualize;
        eliminationStepsForParticipantsGraph = new HashMap<>();
        incompatibilityMap = new HashMap<>();
        xors = new HashMap<>();
        mappingPuMCombinedPuM = new HashMap<>();
        xorWithDirectEdgeToMerge = new ArrayList<>();
        mapCommonSynchronousTasksToIndividualParts = new HashMap<>();
        setupDeconstructionOfPuModels(choreography);
    }

    private void setupDeconstructionOfPuModels(Choreography choreo) {

        //To fill xorsWithDirectEdgeToMerge
        Set<IChoreographyNode> xorsWithDirectEdgeToMerge = new HashSet<>();

        for (Role role : choreo.collaboration.roles) {
            //Save role
            participantsByName.put(role.getName(), role);

            //Save mapping Role -> Digraph of public model
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
            }
        }


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


    public void run() throws IOException, InterruptedException, NoTracesToEndFoundException {
        try {

            //Step 1: Build combined public model
            MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> combinedPuM = buildCombinedPublicModel();

            //Step 2: Find circles by computing topological order and inverse topological order, removing the nodes and edges from the graph

            IPublicNode endGlobal = combinedPuM.getVertices().stream().filter(t -> t.getName().equals("endGlobal")).collect(Collectors.toList()).get(0);
            IPublicNode startGlobal = combinedPuM.getVertices().stream().filter(t -> t.getName().equals("startGlobal")).collect(Collectors.toList()).get(0);

            Pair<Boolean, List<IPublicNode>> topOrderResult = computeTopologicalOrder(combinedPuM, startGlobal, true, true);

            //If a topological order has already been found, we dont need to check the backwards direction or eliminate any cycles (there are none)
            if (!topOrderResult.first) {

                //Compute the inverse topological order starting at endGlobal
                computeTopologicalOrder(combinedPuM, endGlobal, false, true);

                outputHandler.printEasySoundness("Cycles found; can be found in /EasySoundness/CombinedPublicModelCycles.dot");
                IOUtils.toFile(
                        GlobalTimestamp.timestamp + "/EasySoundness/CombinedPublicModelCycles.dot",
                        combinedPuM.toDOT());

                //Step 2.1: Eliminate interactions that are in the found cycles

                //The only remaining nodes in the combinedPuM are the ones contained in a cycle
                //filter out synchronous tasks in the beginning (need to be mapped back)
                List<IPublicNode> interactionsToRemove = combinedPuM.getVertices().stream()
                        .filter(v -> !(v instanceof Gateway))
                        .filter(v -> !v.getName().startsWith("S: "))
                        .collect(Collectors.toList());

                //Add the mapped back synchronous tasks again
                List<IPublicNode> interactionsToRemoveSyncTask = combinedPuM.getVertices().stream()
                        .filter(v -> v.getName().startsWith("S: "))
                        .collect(Collectors.toList());

                for (IPublicNode syncTask : interactionsToRemoveSyncTask) {
                    interactionsToRemove.addAll(mapCommonSynchronousTasksToIndividualParts.get(syncTask));
                }

                outputHandler.printEasySoundness("The following interactions are all contained in a cycle and therefore need to be eliminated");
                outputHandler.printEasySoundness(interactionsToRemove.stream().map(IGObject::getName).collect(Collectors.joining(", ")));

                eliminateCyclesFromConsideration(interactionsToRemove);

            }


//            outputHandler.printEasySoundness("The following topological order has been found for the complete graph:");
//            outputHandler.printEasySoundness(rst.stream().map(IGObject::getName).collect(Collectors.joining(",\n")));


            //Step 1: Check for cyclic waits
//            checkForCyclicWaits();

            //Step 2: Check for valid traces
//            searchForValidTraces();


            if (visualize) {
                VisualizationHandler.visualize(VisualizationHandler.VisualizationType.EASY_SOUNDNESS);
            }


        } catch (Exception e) {
            outputHandler.closePrintWriter();
            throw e;
        }
        outputHandler.closePrintWriter();
    }


    private void eliminateCyclesFromConsideration(List<IPublicNode> interactionsToRemove) throws IOException, InterruptedException {
        Set<IPublicNode> eliminateBecauseOfCycle = new HashSet<>();

        //If the node is a Resource Share we can ignore it, because counterpart can still be executed
        //Also we only need to add the counterparts of sending elements. This is, as although a message can never be received,
        //the sender can still executes (sending into nirvana) TODO: Implement and take into account ST
        for (IPublicNode node : interactionsToRemove) {
            eliminateBecauseOfCycle.add(node);
            if (!node.getName().startsWith("R: ") /*&& !(node instanceof Receive)*/) {
                IPublicNode counterpartNode = getCounterpartNode(node);
                if (counterpartNode != null) {
                    eliminateBecauseOfCycle.add(counterpartNode);
                } else {
                    outputHandler.printEasySoundness("Counterpart Graph was already null, TODO check if that was supposed to ever happen");
                }
            }
        }

        Set<IPublicNode> furtherEliminations = new HashSet<>();
        for (IPublicNode nodeToBeEliminated : eliminateBecauseOfCycle) {
            outputHandler.printEasySoundness(OutputHandler.EasySoundnessAnalyisBlocks.NODE_DELIM);
            outputHandler.printEasySoundness("Checking implications of interaction " + nodeToBeEliminated + " being eliminated\n");
            furtherEliminations.addAll(eliminateContext(nodeToBeEliminated));
        }

        //If a graph was eliminated completely, the relevant counterpart nodes have to be eliminated as well
        for (IPublicNode furtherNodeToBeEliminated : furtherEliminations) {
            outputHandler.printEasySoundness(OutputHandler.EasySoundnessAnalyisBlocks.NODE_DELIM);
            outputHandler.printEasySoundness("Checking implications of interaction " + furtherNodeToBeEliminated + " being eliminated\n");
            //TODO: Recursively -> Until no new nodes
            eliminateContext(furtherNodeToBeEliminated);
        }


    }


    private MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> buildCombinedPublicModel() {

        Map<Role, IDirectedGraph<Edge<IPublicNode>, IPublicNode>> puMs = publicModelsByRole.entrySet().stream().filter(entry -> entry.getValue() != null).collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getdigraph()));

        //To track if the shared synchronous task for both participants was already added in the model
        Map<String, IPublicNode> mapSynchronousTasksToCommonOne = new HashMap<>();

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
                            mappingPuMCombinedPuM.put(vertex, newXor);
                            rst.addVertex(newXor);
                        } else {
                            AndGateway newAnd = new AndGateway(getNameWithRole(vertex, currRole));
                            mappingPuMCombinedPuM.put(vertex, newAnd);
                            rst.addVertex(newAnd);
                        }
                    }

                    if (vertex instanceof Send || vertex instanceof Receive) {

                        //Special treatment of sync tasks. Create a shared one for both participants if not already present
                        if (vertex.getName().startsWith("S: ")) {
                            //Store using only the middle section of the interactions name (S: IA5(P_x>P_y) (r) -> IA5(P_x>P_y))
                            String[] vertexNameSplit = vertex.getName().split(" ");
                            IPublicNode syncTaskNode = mapSynchronousTasksToCommonOne.get(vertexNameSplit[1]);

                            //Sync Task has not been added yet
                            if (syncTaskNode == null) {
                                IPublicNode syncTaskNew = new PublicNode();
                                syncTaskNew.setName(vertexNameSplit[0] + " " + vertexNameSplit[1]);
                                mapSynchronousTasksToCommonOne.put(vertexNameSplit[1], syncTaskNew);

                                List<IPublicNode> toMap = new ArrayList<>();
                                toMap.add(vertex);
                                mapCommonSynchronousTasksToIndividualParts.put(syncTaskNew, toMap);

                                rst.addVertex(syncTaskNew);
                            } else {
                                mapCommonSynchronousTasksToIndividualParts.get(syncTaskNode).add(vertex);
                            }

                        } else {
                            rst.addVertex(vertex);

                        }

                    }
                }

                for (Edge<IPublicNode> edge : puM.getValue().getEdges()) {

                    IPublicNode source, target;

                    if (edge.getSource().getName().startsWith("S: ")) {
                        source = mapSynchronousTasksToCommonOne.get(edge.getSource().getName().split(" ")[1]);
                    } else {
                        source = edge.getSource();
                    }

                    if (edge.getTarget().getName().startsWith("S: ")) {
                        target = mapSynchronousTasksToCommonOne.get(edge.getTarget().getName().split(" ")[1]);
                    } else {
                        target = edge.getTarget();
                    }

                    if ((source instanceof Receive || source instanceof Send) && (target instanceof Receive || target instanceof Send)) {
                        rst.addEdge(source, target);
                    } else if (source instanceof Receive || source instanceof Send) {
                        IPublicNode newTgt = rst.getVertices().stream()
                                .filter(vertex -> vertex.getName().equals(getNameWithRole(target, currRole)))
                                .collect(Collectors.toList()).get(0);
                        rst.addEdge(source, newTgt);
                    } else if (target instanceof Receive || target instanceof Send) {
                        IPublicNode newSrc = rst.getVertices().stream()
                                .filter(vertex -> vertex.getName().equals(getNameWithRole(source, currRole)))
                                .collect(Collectors.toList()).get(0);
                        rst.addEdge(newSrc, target);
                    } else {
                        IPublicNode newSrc = rst.getVertices().stream()
                                .filter(vertex -> vertex.getName().equals(getNameWithRole(source, currRole)))
                                .collect(Collectors.toList()).get(0);
                        IPublicNode newTgt = rst.getVertices().stream()
                                .filter(vertex -> vertex.getName().equals(getNameWithRole(target, currRole)))
                                .collect(Collectors.toList()).get(0);

                        //Ensure edges between two adjacent sync tasks are not added twice
                        if (rst.getEdge(newSrc, newTgt) == null) {
                            rst.addEdge(newSrc, newTgt);
                        }
                    }
                }
            }


            /*
             * Add all the crossreferences between interactions. To avoid duplicates we track what connections we already added
             */

            /*
             * Filter out only the interactions, synchronous tasks and resource shares
             * -> sync tasks as they are treated as a shared node and therefore doesnt need cross references
             * -> resource shares as they are irrelevant for the computation order and can be treated from the participant they belong to only
             */
            List<IPublicNode> rstOnlyInteractions = rst.getVertices().stream()
                    .filter(v -> v instanceof Send || v instanceof Receive)
                    .filter(v -> v.getName().startsWith("M: ") || v.getName().startsWith("H: "))
                    .collect(Collectors.toList());

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


            IOUtils.toFile(
                    GlobalTimestamp.timestamp + "/EasySoundness/CombinedPublicModel2.dot",
                    rst != null ? rst.toDOT() : "");


            return rst;
        } else {
            throw new IllegalStateException("Public Models are null");
        }
    }


    /**
     * Does the computation of a topological order using Khans algorithm.
     * There are multiple configuration options:
     * <p>
     * forwardDirection -> Decides in which direction the topological order is built
     * -> true: "standard" topological order is built by traversing graph through child nodes
     * -> false: "inverse" topological order is built by traversing graph through parent nodes
     * <p>
     * remove -> Decides if the visited nodes and edges are removed or not
     * <p>
     * Returns a pair of Boolean and a list of public nodes, the boolean depicting if topological order could be found and the list containing said order
     */
    private Pair<Boolean, List<IPublicNode>> computeTopologicalOrder(MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> graph, IPublicNode beginNode, boolean forwardDirection, boolean remove) {
        List<IPublicNode> rst = new ArrayList<>();

        List<IPublicNode> queue = new ArrayList<>();
        queue.add(beginNode);

        //Compute and store the amount of incoming edges for each vertex
        Map<IPublicNode, Integer> amountNeighboringEdges = graph.getVertices().stream().collect(Collectors.toMap(v -> v, v -> forwardDirection ? graph.getDirectPredecessors(v).size() : graph.getDirectSuccessors(v).size()));

        while (!queue.isEmpty()) {

            IPublicNode currNode = queue.remove(0);
            rst.add(currNode);
            amountNeighboringEdges.remove(currNode);


            List<IPublicNode> neighbors = forwardDirection ? new ArrayList<>(graph.getDirectSuccessors(currNode)) : new ArrayList<>(graph.getDirectPredecessors(currNode));
            //Decrement for each neighbor of the current node the amount of connected edges by 1
            for (IPublicNode neighbor : neighbors) {
                //Remove edge from currNode to its neighbor
                if (remove)
                    graph.removeEdge(forwardDirection ? graph.getEdge(currNode, neighbor) : graph.getEdge(neighbor, currNode));

                amountNeighboringEdges.computeIfPresent(neighbor, (k, v) -> v - 1);
            }

            if (remove)
                graph.removeVertex(currNode);

            queue.addAll(amountNeighboringEdges.entrySet().stream().filter(v -> v.getValue() == 0).filter(v -> !queue.contains(v.getKey())).map(Map.Entry::getKey).collect(Collectors.toList()));

            System.out.println("Current queue:");
            System.out.println(queue.stream().map(IGObject::getName).collect(Collectors.joining(", ")));
            System.out.println("---------------------------------------------------------");
            System.out.println("Current Topological Ordering: ");
            System.out.println(rst.stream().map(IGObject::getName).collect(Collectors.joining(", ")));
            System.out.println("---------------------------------------------------------");

        }

        if (amountNeighboringEdges.entrySet().stream().anyMatch(v -> v.getValue() != 0)) {
            return new Pair<>(false, new ArrayList<>());
        }

        outputHandler.printEasySoundness("No Cycle has been found in the graph, topological order is:");
        outputHandler.printEasySoundness(rst.stream().map(IGObject::getName).collect(Collectors.joining(", ")));
        return new Pair<>(true, rst);
    }


    private void searchForValidTraces() throws NoTracesToEndFoundException {
        outputHandler.printEasySoundness(OutputHandler.EasySoundnessAnalyisBlocks.START_VALID_TRACES);

        //Step 2.1 Compute all traces from start to sink
        List<List<Edge<IPublicNode>>> tracesToEndGlobal = computeAllTracesStartToSink();

        if (tracesToEndGlobal != null) {

            //Step 2.2 Build incompatibilityMap
            buildIncompatibilityMap();

            //Step 2.3 Eliminate all incompatible nodes
            removeAllIncompatibleTraces(incompatibilityMap, tracesToEndGlobal);


            if (tracesToEndGlobal.isEmpty()) {
                outputHandler.printEasySoundness("\nAll traces to endGlobal are not valid. Therefore the model is not Easy-Sound");
            } else {
                outputHandler.printEasySoundness("\nThe valid traces are");

                tracesToEndGlobal.forEach(t -> {
                    outputHandler.printEasySoundness(t.stream().map(Edge::toString).collect(Collectors.joining(" \n ")));
                    outputHandler.printEasySoundness("--------------------------------------");
                });
            }

        } else {
            throw new NoTracesToEndFoundException();
        }

        outputHandler.printEasySoundness(OutputHandler.EasySoundnessAnalyisBlocks.STOP_VALID_TRACES);
    }


    private List<List<Edge<IPublicNode>>> computeAllTracesStartToSink() {
        //1. Build the complete graph, meaning a combination of all public models
        MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> completeGraph = buildCombinedPublicModel();

        if (completeGraph != null) {
            //2. Compute topological order using Kahns algorithm
            List<IPublicNode> topologicalOrder = computeTopologicalOrder(completeGraph, completeGraph.getVertices().stream().filter(v -> v.getName().equals("startGlobal")).collect(Collectors.toList()).get(0), true, false).second;

            outputHandler.printEasySoundness("----------------------------------------------");
            outputHandler.printEasySoundness("Starting computation of traces to all nodes");
            outputHandler.printEasySoundness("----------------------------------------------");
            //3. For all nodes compute all paths that end at said node in topological order
            Map<IPublicNode, List<List<Edge<IPublicNode>>>> tracesToAllNodes = computeTracesToAllNodes2(completeGraph, new ArrayList<>(topologicalOrder));


            //Print traces ending at node for all nodes in topological order
            for (IPublicNode nodeTopOrder : topologicalOrder) {
                List<List<Edge<IPublicNode>>> tracesToNode = tracesToAllNodes.get(nodeTopOrder);

                outputHandler.printEasySoundness("Traces that end at: " + nodeTopOrder.getName() + " (" + (tracesToNode != null ? tracesToNode.size() : 0) + ")");

                //For startGlobal its gonna be null
                if (tracesToNode != null) {
                    for (List<Edge<IPublicNode>> trace : tracesToAllNodes.get(nodeTopOrder)) {
                        outputHandler.printEasySoundness(trace.stream().map(Edge::toString).collect(Collectors.joining(" \n ")));
                        outputHandler.printEasySoundness("---------------------------------------------------");
                    }
                    outputHandler.printEasySoundness("\n");
                }
            }


            IPublicNode endGlobal = tracesToAllNodes.keySet().stream().filter(t -> t.getName().equals("endGlobal")).collect(Collectors.toList()).get(0);

            return tracesToAllNodes.get(endGlobal);


        } else {
            outputHandler.printEasySoundness("No topological order able to be computed for a graph that is null");
            return null;
        }
    }

    private void removeAllIncompatibleTraces(Map<Edge<IPublicNode>, Set<Edge<IPublicNode>>> incomp, List<List<Edge<IPublicNode>>> traces) {

        for (Map.Entry<Edge<IPublicNode>, Set<Edge<IPublicNode>>> incompEntry : incomp.entrySet()) {
            Edge<IPublicNode> currEdge = incompEntry.getKey();

            Iterator<List<Edge<IPublicNode>>> tracesIterator = traces.iterator();

            while (tracesIterator.hasNext()) {
                boolean foundIncompatibility = false;
                List<Edge<IPublicNode>> traceToCheck = tracesIterator.next();

                if (isEdgeContainedInTrace(traceToCheck, currEdge)) {

                    for (Edge<IPublicNode> incompatibleWithCurr : incompEntry.getValue()) {

                        IPublicNode incompatibleSource = mappingPuMCombinedPuM.getOrDefault(incompatibleWithCurr.getSource(), incompatibleWithCurr.getSource());
                        IPublicNode incompatibleTarget = mappingPuMCombinedPuM.getOrDefault(incompatibleWithCurr.getTarget(), incompatibleWithCurr.getTarget());


                        for (Edge<IPublicNode> incompatibleEdge : traceToCheck) {

                            if (incompatibleEdge.getSource().equals(incompatibleSource) && incompatibleEdge.getTarget().equals(incompatibleTarget)) {
                                outputHandler.printEasySoundness(OutputHandler.EasySoundnessAnalyisBlocks.NODE_DELIM);
                                outputHandler.printEasySoundness("A trace has been found that contains incompatible edges: " + currEdge + " and " + incompatibleWithCurr);
                                outputHandler.printEasySoundness("Therefore the following trace has been eliminated\n");
                                outputHandler.printEasySoundness(traceToCheck.stream().map(Edge::toString).collect(Collectors.joining("\n")));
                                foundIncompatibility = true;
                                break;
                            }
                        }

                        if (foundIncompatibility) {
                            tracesIterator.remove();
                            break;
                        }
                    }
                }
            }


        }


    }

    /**
     * Helper method, as list contains does not work due to differing IDs. Therefore we check over source and target, which are indeed the same if the edge is contained
     */
    private boolean isEdgeContainedInTrace(List<Edge<IPublicNode>> trace, Edge<IPublicNode> toCheck) {

        for (Edge<IPublicNode> edge : trace) {
            if (edge.getSource().getName().equals(toCheck.getSource().getDescription()) && edge.getTarget().getName().equals(toCheck.getTarget().getDescription())) {
                return true;
            }
        }
        return false;
    }

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

            outputHandler.printEasySoundness("Computation of branches that end at " + currNode.getName());
            outputHandler.printEasySoundness("--------------------------------------\n");

            if (currNode instanceof XorGateway) {

                for (IPublicNode parent : parents) {
                    List<List<Edge<IPublicNode>>> tracesEndingAtParent = rst.get(parent);

                    outputHandler.printEasySoundness("For parent " + parent.getName() + " the branches ending at it are:");
                    outputHandler.printEasySoundness(
                            tracesEndingAtParent.stream()
                                    .map(traceOneParent ->
                                            traceOneParent.stream()
                                                    .map(Edge::toString)
                                                    .collect(Collectors.joining(", ")))
                                    .collect(Collectors.joining("\n")));

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

                        outputHandler.printEasySoundness("Edge ending at " + currNode.getName() + ":");
                        outputHandler.printEasySoundness(newTraceEndingAtCurr.stream().map(Edge::toString).collect(Collectors.joining(",")));
                    }
                }

            } else {
                //Build list containing all traces ending at all parents
                List<List<List<Edge<IPublicNode>>>> allTraces = new ArrayList<>();

                for (IPublicNode parent : parents) {


                    //startGlobal -> children is empty
                    if (!rst.isEmpty()) {
                        allTraces.add(rst.get(parent));
                    }


                }

                //Get all edges to current node
                List<Edge<IPublicNode>> edgesToCurr = parents.stream().map(p -> graph.getEdge(p, currNode)).collect(Collectors.toList());

                //Build all combinations using a backtracking approach
                List<List<Edge<IPublicNode>>> tracesEndingAtCurr = getTracesEndingAtNode2(allTraces, currNode, edgesToCurr);

                tracesEndingAtCurrNode.addAll(tracesEndingAtCurr);
            }

            rst.put(currNode, tracesEndingAtCurrNode);

            outputHandler.printEasySoundness("\n");
        }

        return rst;
    }

    private List<List<Edge<IPublicNode>>> getTracesEndingAtNode2(List<List<List<Edge<IPublicNode>>>> allTraces, IPublicNode node, List<Edge<IPublicNode>> edgesToNode) {
        List<List<Edge<IPublicNode>>> rst = new ArrayList<>();

        outputHandler.printEasySoundness("Combining parents traces to get traces ending at " + node.getName() + "\n");

        getTracesEndingAtNodeRec2(allTraces, node, edgesToNode, 0, new ArrayList<>(), rst);

        return rst;
    }

    private int getTracesEndingAtNodeRec2(List<List<List<Edge<IPublicNode>>>> allTraces, IPublicNode node, List<Edge<IPublicNode>> edgesToNode, int ctr, List<Edge<IPublicNode>> currTrace, List<List<Edge<IPublicNode>>> rst) {

        if (ctr == allTraces.size() || allTraces.stream().allMatch(Objects::isNull)) {
            currTrace.addAll(edgesToNode);
            rst.add(new ArrayList<>(currTrace));

            outputHandler.printEasySoundness("\nTraces combined! Adding new trace:");
            outputHandler.printEasySoundness(currTrace.stream().map(Edge::toString).collect(Collectors.joining(", ")));
            outputHandler.printEasySoundness("=============================");
            //For the backtracking we need to consider the elements that are added in the recursive call as well
            return edgesToNode.size();
        }

        List<List<Edge<IPublicNode>>> tracesOfCurrParticipant = allTraces.get(ctr);
        for (List<Edge<IPublicNode>> singleTraceOfCurrParticipant : tracesOfCurrParticipant) {

            outputHandler.printEasySoundness("\nElement to add to combination:");
            outputHandler.printEasySoundness(singleTraceOfCurrParticipant.stream().map(Edge::toString).collect(Collectors.joining(", ")) + "\n");
            //Needed for the backtracking
            int amountAddedElements = 0;

            //Add all nodes that are not already in the trace
            for (Edge<IPublicNode> nodeOfTrace : singleTraceOfCurrParticipant) {
                if (!currTrace.contains(nodeOfTrace)) {
                    currTrace.add(nodeOfTrace);
                    amountAddedElements++;
                }
            }

            outputHandler.printEasySoundness("Amount of added elements before rec call: " + amountAddedElements);

            outputHandler.printEasySoundness("\nCurrent Working Trace:");
            outputHandler.printEasySoundness(currTrace.stream().map(Edge::toString).collect(Collectors.joining(", ")));

            amountAddedElements += getTracesEndingAtNodeRec2(allTraces, node, edgesToNode, ctr + 1, currTrace, rst);

            outputHandler.printEasySoundness("Amount of added elements after rec call: " + amountAddedElements);

            outputHandler.printEasySoundness("Start backtracking\n");

            //Remove the added trace of the current participant from the end of the current trace again
            for (int i = 0; i < amountAddedElements; i++) {
                outputHandler.printEasySoundness("Remove: " + currTrace.get(currTrace.size() - 1));
                currTrace.remove(currTrace.size() - 1);
            }

            outputHandler.printEasySoundness("\nBacktracking finished, Working Trace backtracked to:");
            outputHandler.printEasySoundness(currTrace.stream().map(Edge::toString).collect(Collectors.joining(", ")));
        }
        return 0;
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

    private String getNameWithRole(IPublicNode vertex, Role role) {
        if (vertex.getName().startsWith("S: ")) {
            return vertex.getName().split(" ")[0] + " " + vertex.getName().split(" ")[1];
        }
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

    /**
     * If a node is eliminated from the consideration, because its not executable, that means that its "context" also has to be eliminated.
     * This means, that an eliminated node is on an AND-branch, the whole and branch can not be executed and thus also needs to be eliminated.
     * If the eliminated node has a non-branched connection to the start event, the whole participant cannot work
     * If the eliminated node is on an XOR-Branch, all other nodes on that branch cannot executed and therefore need to be eliminated
     * <p>
     * This needs to be done recursively; e.g. when eliminated node is on an AND-branch, and the and branch has a non-branching connection to the startnode
     * then the whole participant cannot work again
     */
    private List<IPublicNode> eliminateContext(IPublicNode node) throws IOException, InterruptedException {

        IDirectedGraph<Edge<IPublicNode>, IPublicNode> graphOfNode = getGraphToNode(node);

        //Graph has already been eliminated -> can not execute at all
        if (graphOfNode == null) {
            outputHandler.printEasySoundness("Graph of the participant has already been eliminated, as its not executable. Skipping validation for the node\n");
            return Collections.emptyList();
        }

        //Check if the current node to eliminate with its context is still present, because it might have been eliminated in a previous elimination step
        if (!graphOfNode.getVertices().contains(node)) {
            outputHandler.printEasySoundness("Node has already been eliminated in a previous step, Continuing with the next one");
            return Collections.emptyList();
        }

        List<IPublicNode> toEliminate = new ArrayList<>();

        //Step 1: Traverse the graph backwards until you find either a XOR node or the start node
        //In case of XOR, the other branches of XOR can potentially be used to get from start to end
        //In case of start, the whole participant cannot execute
        eliminateContextUntilXOROrStart(graphOfNode, node, toEliminate);

        IPublicNode stopNode = toEliminate.remove(toEliminate.size() - 1);

        Role participantOfNode = participantsByName.get(getParticipantOfNode(node));

        if (stopNode instanceof Event) {
            //Stopped at start
            outputHandler.printEasySoundness("All nodes of the participant " + participantOfNode.getName() + " are not executable");
            outputHandler.printEasySoundness(publicModelsByRole.get(participantOfNode).getdigraph().getVertices().stream().map(IGObject::getName).collect(Collectors.joining(",")) + "\n");


            /**
             * The receiving counterparts (MX and HOW) as well as the synchronous task counterparts of the nodes in the
             * eliminated graph need to be eliminated as well
             * */
            List<IPublicNode> furtherEliminate = new ArrayList<>();
            for (IPublicNode eliminatedNode : publicModelsByRole.get(participantOfNode).getdigraph().getVertices()) {

                if (eliminatedNode.getName().startsWith("S: ") || (eliminatedNode instanceof Send && !eliminatedNode.getName().startsWith("R: "))) {
                    IPublicNode counterpartNode = getCounterpartNode(eliminatedNode);

                    if (counterpartNode != null)
                        furtherEliminate.add(counterpartNode);
                }
            }

            //As the participant is not executable, set its graph to null
            publicModelsByRole.put(participantOfNode, null);

            outputHandler.printEasySoundness("Added the new (empty) graph for participant " + participantOfNode + " in EasySoundness/");

            IOUtils.toFile(GlobalTimestamp.timestamp + "/EasySoundness/EasySoundnessCycleChecks_" + participantOfNode.getName() + "_Change" + eliminationStepsForParticipantsGraph.get(participantOfNode), "");
            eliminationStepsForParticipantsGraph.put(participantOfNode, eliminationStepsForParticipantsGraph.get(participantOfNode) + 1);

            outputHandler.printEasySoundness("The relevant counterpart nodes of that graph therefore need to be eliminated as well: ");
            outputHandler.printEasySoundness(furtherEliminate.stream().map(IGObject::getName).collect(Collectors.joining(", ")));

            return furtherEliminate;
        } else if (stopNode instanceof Gateway) {
            //Stopped at XOR
            //Get the first element on the XOR branch
            IPublicNode firstNodeOnXORbranch = toEliminate.get(toEliminate.size() - 1);
            String xorMergeName = stopNode.getName() + "_m";

            //Step 2 Eliminate all nodes on the path from the XOR through the first node on the branch to the XOR merge
            outputHandler.printEasySoundness("Eliminating nodes on the XOR branch that cannot be executed");
            eliminateContextXORBranch(graphOfNode, firstNodeOnXORbranch, xorMergeName);

            //Do reduction of the graph
            IPublicModel rpstModel = publicModelsByRole.get(participantOfNode);
            rpstModel.setDiGraph(graphOfNode);
            rpstModel.reduceGraph(xorWithDirectEdgeToMerge);

            publicModelsByRole.put(participantOfNode, rpstModel);

            outputHandler.printEasySoundness("Saved new graph for possible path finding to EasySoundness/");

            //Export changed graph
            IOUtils.toFile(GlobalTimestamp.timestamp + "/EasySoundness/EasySoundnessCycleChecks_" + participantOfNode.getName() + "_Change" + eliminationStepsForParticipantsGraph.get(participantOfNode) + ".dot", graphOfNode.toDOT());
            eliminationStepsForParticipantsGraph.put(participantOfNode, eliminationStepsForParticipantsGraph.get(participantOfNode) + 1);
        }
        return Collections.emptyList();
    }

    /**
     * Iterates from the given XOR-gateway downwards, eliminating all the interactions on all paths to its merge node
     */
    private void eliminateContextXORBranch(IDirectedGraph<Edge<IPublicNode>, IPublicNode> graph, IPublicNode node, String gatewayMergeName) {

        //Remove the node and the connection to its parents
        List<IPublicNode> parents = new ArrayList<>(graph.getDirectPredecessors(node));
        List<IPublicNode> children = new ArrayList<>(graph.getDirectSuccessors(node));

        removeParentEdgesAndNode(graph, parents, node, true);

        outputHandler.printEasySoundness("Eliminating " + node.getName());

        for (IPublicNode child : children) {

            //If the child is the gateway merge node, we have to only remove the connection to it and can return after
            if (child.getName().equals(gatewayMergeName)) {
                removeParentEdgesAndNode(graph, Collections.singletonList(node), child, false);
                return;
            }

            eliminateContextXORBranch(graph, child, gatewayMergeName);
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


}
