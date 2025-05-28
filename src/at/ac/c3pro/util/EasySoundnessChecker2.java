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

    OutputHandler.DebugLevel DEBUG = OutputHandler.DebugLevel.DEBUG;
    OutputHandler.DebugLevel INFO = OutputHandler.DebugLevel.INFO;

    boolean visualize;

    //    Map<Role, IDirectedGraph<Edge<IPublicNode>, IPublicNode>> publicModelsByRole;
    Map<Role, IPublicModel> publicModelsByRole;

    List<IPublicNode> visited;
    Map<String, Role> participantsByName;

    Map<Role, Integer> eliminationStepsForParticipantsGraph;

    Map<IPublicNode, List<IPublicNode>> mapCommonSynchronousTasksToIndividualParts;
    //To track if the shared synchronous task for both participants was already added in the model
    Map<String, IPublicNode> mapSynchronousTasksToCommonOne;

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

    public EasySoundnessChecker2(Choreography choreography, boolean visualize, int debugLevel) throws IOException {

        outputHandler = new OutputHandler(OutputHandler.OutputType.EASY_SOUNDNESS, OutputHandler.DebugLevel.getLevelByValue(debugLevel));

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
        mapSynchronousTasksToCommonOne = new HashMap<>();
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

                outputHandler.printEasySoundness("Cycles found; can be found in /EasySoundness/CombinedPublicModelCycles.dot", INFO);
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

                outputHandler.printEasySoundness("The following interactions are all contained in a cycle and therefore need to be eliminated", INFO);
                outputHandler.printEasySoundness(interactionsToRemove.stream().map(IGObject::getName).collect(Collectors.joining(", ")), INFO);

                eliminateCyclesFromConsideration(interactionsToRemove);

            }

            //Step 2: Check for valid traces -> only if there are publicModels that are not null
            if (publicModelsByRole.values().stream().anyMatch(Objects::nonNull)) {
                searchForValidTraces();
            }

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
                    outputHandler.printEasySoundness("Counterpart Graph was already null, no eliminations needed", INFO);
                }
            }
        }

        Set<IPublicNode> furtherEliminations = new HashSet<>();
        for (IPublicNode nodeToBeEliminated : eliminateBecauseOfCycle) {
            outputHandler.printEasySoundness(OutputHandler.EasySoundnessAnalyisBlocks.NODE_DELIM);
            outputHandler.printEasySoundness("Checking implications of interaction " + nodeToBeEliminated + " being eliminated\n", INFO);
            furtherEliminations.addAll(eliminateContext(nodeToBeEliminated));
        }

        //If a graph was eliminated completely, the relevant counterpart nodes have to be eliminated as well
        //This is done recursively as in each elimination step there might be other nodes whose counterparts need to be eliminated as well
        while (!furtherEliminations.isEmpty()) {
            List<IPublicNode> furtherNodesToBeEliminatedWorkingSet = new ArrayList<>(furtherEliminations);
            furtherEliminations.clear();

            for (IPublicNode furtherNodeToBeEliminated : furtherNodesToBeEliminatedWorkingSet) {
                outputHandler.printEasySoundness(OutputHandler.EasySoundnessAnalyisBlocks.NODE_DELIM);
                outputHandler.printEasySoundness("Checking implications of interaction " + furtherNodeToBeEliminated + " being eliminated\n", INFO);
                furtherEliminations.addAll(eliminateContext(furtherNodeToBeEliminated));
            }
        }
    }


    private MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> buildCombinedPublicModel() {

        mapSynchronousTasksToCommonOne.clear();
        mapCommonSynchronousTasksToIndividualParts.clear();

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
                            String nameDescription = getNameWithRole(vertex, currRole);
                            XorGateway newXor = new XorGateway(nameDescription);
                            newXor.setDescription(nameDescription);
                            mappingPuMCombinedPuM.put(vertex, newXor);
                            rst.addVertex(newXor);
                        } else {
                            String nameDescription = getNameWithRole(vertex, currRole);
                            AndGateway newAnd = new AndGateway(nameDescription);
                            newAnd.setDescription(nameDescription);
                            mappingPuMCombinedPuM.put(vertex, newAnd);
                            rst.addVertex(newAnd);
                        }
                    }

                    //TODO: Synchronous Tasks werden als zwei verschiedene Objekte mit identischem Inhalt geadded -> Immernoch?
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

                    //Treat as a list, so that we can perform an empty check. If the graph is still present, thesize of the list will always be 1
                    List<IPublicNode> counterpartList = rstOnlyInteractions.stream().filter(v -> v.getName().equals(counterpartNodeName)).collect(Collectors.toList());

                    if (!counterpartList.isEmpty()) {

                        IPublicNode counterpart = counterpartList.get(0);
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
                        alreadyAdded.put(counterpart, true);
                    }
                    alreadyAdded.put(vertex, true);
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

        outputHandler.printEasySoundness("No Cycle has been found in the graph, topological order is:", INFO);
        outputHandler.printEasySoundness(rst.stream().map(IGObject::getName).collect(Collectors.joining(", ")), INFO);
        return new Pair<>(true, rst);
    }


    private void searchForValidTraces() throws NoTracesToEndFoundException {
        outputHandler.printEasySoundness(OutputHandler.EasySoundnessAnalyisBlocks.START_VALID_TRACES);

        //2.1 Build the combined public model again on the basis of the reduced public models
        MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> combinedPuM = buildCombinedPublicModel();

        //Step 2.2 Compute incompatibility map
        buildIncompatibilityMap(combinedPuM);

        //Step 2.3 Compute all traces from start to sink TODO while removing incompatible traces -> done??
        List<List<Edge<IPublicNode>>> tracesToEndGlobal = computeAllTracesStartToSink(combinedPuM);

        if (tracesToEndGlobal != null) {
            //Step 2.3 Eliminate all incompatible nodes
//            removeAllIncompatibleTraces(incompatibilityMap, tracesToEndGlobal);


            if (tracesToEndGlobal.isEmpty()) {
                outputHandler.printEasySoundness("\nAll traces to endGlobal are not valid. Therefore the model is not Easy-Sound", INFO);
            } else {
                outputHandler.printEasySoundness("==================================", INFO);
                outputHandler.printEasySoundness("\nThe valid traces are", INFO);
                outputHandler.printEasySoundness("==================================", INFO);

                tracesToEndGlobal.forEach(t -> {
                    outputHandler.printEasySoundness(t.stream().map(Edge::toString).collect(Collectors.joining(" \n ")), INFO);
                    outputHandler.printEasySoundness("--------------------------------------", INFO);
                });
            }

        } else {
            throw new NoTracesToEndFoundException();
        }

        outputHandler.printEasySoundness(OutputHandler.EasySoundnessAnalyisBlocks.STOP_VALID_TRACES);
    }


    private List<List<Edge<IPublicNode>>> computeAllTracesStartToSink(MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> combinedPuM) {

        //1. Compute topological order using Kahns algorithm
        List<IPublicNode> topologicalOrder = computeTopologicalOrder(combinedPuM, combinedPuM.getVertices().stream().filter(v -> v.getName().equals("startGlobal")).collect(Collectors.toList()).get(0), true, false).second;

        outputHandler.printEasySoundness("----------------------------------------------", INFO);
        outputHandler.printEasySoundness("Starting computation of traces to all nodes", INFO);
        outputHandler.printEasySoundness("----------------------------------------------", INFO);
        //2. For all nodes compute all paths that end at said node in topological order
        Map<IPublicNode, List<List<Edge<IPublicNode>>>> tracesToAllNodes = computeTracesToAllNodes2(combinedPuM, new ArrayList<>(topologicalOrder));


        //Print traces ending at node for all nodes in topological order
        for (IPublicNode nodeTopOrder : topologicalOrder) {
            List<List<Edge<IPublicNode>>> tracesToNode = tracesToAllNodes.get(nodeTopOrder);

            //TODO: VALID traces that end at:
            outputHandler.printEasySoundness("Traces that end at: " + nodeTopOrder.getName() + " (" + (tracesToNode != null ? tracesToNode.size() : 0) + ")", INFO);

            //For startGlobal its gonna be null
            if (tracesToNode != null) {
                for (List<Edge<IPublicNode>> trace : tracesToAllNodes.get(nodeTopOrder)) {
                    outputHandler.printEasySoundness(trace.stream().map(Edge::toString).collect(Collectors.joining(" \n ")), INFO);
                    outputHandler.printEasySoundness("---------------------------------------------------", INFO);
                }
                outputHandler.printEasySoundness("\n", INFO);
            }
        }


        IPublicNode endGlobal = tracesToAllNodes.keySet().stream().filter(t -> t.getName().equals("endGlobal")).collect(Collectors.toList()).get(0);

        return tracesToAllNodes.get(endGlobal);

    }

    private void removeAllIncompatibleTraces2(List<List<Edge<IPublicNode>>> traces, MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> graph) {
        Iterator<List<Edge<IPublicNode>>> tracesIterator = traces.iterator();

        //Iterate over traces
        while (tracesIterator.hasNext()) {
            List<Edge<IPublicNode>> traceToCheck = tracesIterator.next();

            //Used for the break if an iterator is removed
            boolean iterRemoved = false;

            //Iterate over edges in trace
            for (Edge<IPublicNode> currEdge : traceToCheck) {

                //Edges that contain an event can be ignored, as edges from/to start nor edges from/to end can ever be in an XOR branch
                //and will thus never have incompatible edges
                if (!(currEdge.getSource() instanceof Event || currEdge.getTarget() instanceof Event)) {
                    Edge<IPublicNode> edgeMappedBack = getEdgeMappedFromCPuMToPuM(currEdge, graph);

                    Set<Edge<IPublicNode>> incompatibleEdges = incompatibilityMap.get(edgeMappedBack);

                    //If there are any incompatible edges, iterate over them
                    if (incompatibleEdges != null) {
                        for (Edge<IPublicNode> incomp : incompatibleEdges) {

                            Edge<IPublicNode> incompMappedBack = getEdgeMappedFromPuMToCPuM(incomp, graph);
                            if (traceToCheck.contains(incompMappedBack)) {
                                tracesIterator.remove();
                                iterRemoved = true;
                                break;
                            }
                        }
                    }
                    if (iterRemoved)
                        break;
                }

            }

        }
    }

    //TODO: Was wenn es zwei aufeinanderfolgende Sync-Tasks sind? Wie korrekte Edge herausfinden? -> gefixt durch edge aus CPuM im falle von Synch task?
    //Beide betrachten? Iterieren zurück über Parents lässt beide Participants zu
    private Edge<IPublicNode> getEdgeMappedFromCPuMToPuM(Edge<IPublicNode> edge, MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> graph) {

        IPublicNode src = edge.getSource(), tgt = edge.getTarget();

        //If at least one of the nodes is a synchronous task, the edge needs to be taken from the combined model, as the incompatibility map was built that way
        if (src.getName().startsWith("S: ") || tgt.getName().startsWith("S: ")) {
            return graph.getEdge(src, tgt);
        }

        IDirectedGraph<Edge<IPublicNode>, IPublicNode> graphOfParticipant = getGraphToNode(src);

        Edge<IPublicNode> edgeRst = graphOfParticipant.getEdge(getNodeMappedFromCPuMToPuM(src), getNodeMappedFromCPuMToPuM(tgt));

        return edgeRst;
    }

    private IPublicNode getNodeMappedFromCPuMToPuM(IPublicNode node) {
        //Events can be ignored, as neither start nor end can ever be on an XOR branch and thus never have incopmpatible edges.

        if (node instanceof Gateway) {
            //TODO: Reverse Map or is the other direction needed elsewhere
            for (Map.Entry<IPublicNode, IPublicNode> gatewayMap : mappingPuMCombinedPuM.entrySet()) {
                if (gatewayMap.getValue().equals(node))
                    return gatewayMap.getKey();
            }
            throw new IllegalStateException("The given gateway " + node.getName() + " could not be mapped back");
        } else {
            return node;
        }
    }

    private Edge<IPublicNode> getEdgeMappedFromPuMToCPuM(Edge<IPublicNode> edge, MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> graph) {

        //In case either source or target is a synchronous task, no mapping is necessary, as these instances are stored
        //as edges from the CPuM in the incompatibility map.
        if (edge.getSource().getName().startsWith("S: ") || edge.getTarget().getName().startsWith("S: ")) {
            return edge;
        }

        Edge<IPublicNode> edgeRst = graph.getEdge(getNodeMappedFromPuMToCPuM(edge.getSource()), getNodeMappedFromPuMToCPuM(edge.getTarget()));

        if (edgeRst == null) {
            throw new IllegalStateException("Something went wrong while mapping nodes from the public models to the combined");
        } else {
            return edgeRst;
        }
    }


    private IPublicNode getNodeMappedFromPuMToCPuM(IPublicNode node) {

        if (node instanceof Gateway) {
            return mappingPuMCombinedPuM.getOrDefault(node, null);
        } else {
            return node;
        }
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

            outputHandler.printEasySoundness("Computation of traces that end at " + currNode.getName(), INFO);
            outputHandler.printEasySoundness("--------------------------------------\n", INFO);

            if (currNode instanceof XorGateway) {

                for (IPublicNode parent : parents) {
                    List<List<Edge<IPublicNode>>> tracesEndingAtParent = rst.get(parent);

                    outputHandler.printEasySoundness("For parent " + parent.getName() + " the branches ending at it are:", DEBUG);
                    outputHandler.printEasySoundness(
                            tracesEndingAtParent.stream()
                                    .map(traceOneParent ->
                                            traceOneParent.stream()
                                                    .map(Edge::toString)
                                                    .collect(Collectors.joining(", ")))
                                    .collect(Collectors.joining("\n")), DEBUG);

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

                        outputHandler.printEasySoundness("Edge ending at " + currNode.getName() + ":", DEBUG);
                        outputHandler.printEasySoundness(newTraceEndingAtCurr.stream().map(Edge::toString).collect(Collectors.joining(",")), DEBUG);
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

            removeAllIncompatibleTraces2(tracesEndingAtCurrNode, graph);

            rst.put(currNode, tracesEndingAtCurrNode);

            outputHandler.printEasySoundness("The traces ending at " + currNode.getName() + " are:", INFO);
            outputHandler.printEasySoundness(tracesEndingAtCurrNode.stream()
                    .map(trace -> trace.stream()
                            .map(Edge::toString)
                            .collect(Collectors.joining(",", "{", "}")))
                    .collect(Collectors.joining(", \n")), INFO);


            outputHandler.printEasySoundness("\n", INFO);
        }

        return rst;
    }

    private List<List<Edge<IPublicNode>>> getTracesEndingAtNode2(List<List<List<Edge<IPublicNode>>>> allTraces, IPublicNode node, List<Edge<IPublicNode>> edgesToNode) {
        List<List<Edge<IPublicNode>>> rst = new ArrayList<>();

        outputHandler.printEasySoundness("Combining parents traces to get traces ending at " + node.getName() + "\n", DEBUG);

        getTracesEndingAtNodeRec2(allTraces, node, edgesToNode, 0, new ArrayList<>(), rst);

        return rst;
    }

    private int getTracesEndingAtNodeRec2(List<List<List<Edge<IPublicNode>>>> allTraces, IPublicNode node, List<Edge<IPublicNode>> edgesToNode, int ctr, List<Edge<IPublicNode>> currTrace, List<List<Edge<IPublicNode>>> rst) {

        if (ctr == allTraces.size() || allTraces.stream().allMatch(Objects::isNull)) {
            currTrace.addAll(edgesToNode);
            rst.add(new ArrayList<>(currTrace));

            outputHandler.printEasySoundness("\nTraces combined! Adding new trace:", DEBUG);
            outputHandler.printEasySoundness(currTrace.stream().map(Edge::toString).collect(Collectors.joining(", ")), DEBUG);
            outputHandler.printEasySoundness("=============================", DEBUG);
            //For the backtracking we need to consider the elements that are added in the recursive call as well
            return edgesToNode.size();
        }

        List<List<Edge<IPublicNode>>> tracesOfCurrParticipant = allTraces.get(ctr);
        for (List<Edge<IPublicNode>> singleTraceOfCurrParticipant : tracesOfCurrParticipant) {

            outputHandler.printEasySoundness("\nElement to add to combination:", DEBUG);
            outputHandler.printEasySoundness(singleTraceOfCurrParticipant.stream().map(Edge::toString).collect(Collectors.joining(", ")) + "\n", DEBUG);
            //Needed for the backtracking
            int amountAddedElements = 0;

            //Add all nodes that are not already in the trace
            for (Edge<IPublicNode> nodeOfTrace : singleTraceOfCurrParticipant) {
                if (!currTrace.contains(nodeOfTrace)) {
                    currTrace.add(nodeOfTrace);
                    amountAddedElements++;
                }
            }

            outputHandler.printEasySoundness("Amount of added elements before rec call: " + amountAddedElements, DEBUG);

            outputHandler.printEasySoundness("\nCurrent Working Trace:", DEBUG);
            outputHandler.printEasySoundness(currTrace.stream().map(Edge::toString).collect(Collectors.joining(", ")), DEBUG);

            amountAddedElements += getTracesEndingAtNodeRec2(allTraces, node, edgesToNode, ctr + 1, currTrace, rst);

            outputHandler.printEasySoundness("Amount of added elements after rec call: " + amountAddedElements, DEBUG);

            outputHandler.printEasySoundness("Start backtracking\n", DEBUG);

            //Remove the added trace of the current participant from the end of the current trace again
            for (int i = 0; i < amountAddedElements; i++) {
                outputHandler.printEasySoundness("Remove: " + currTrace.get(currTrace.size() - 1), DEBUG);
                currTrace.remove(currTrace.size() - 1);
            }

            outputHandler.printEasySoundness("\nBacktracking finished, Working Trace backtracked to:", DEBUG);
            outputHandler.printEasySoundness(currTrace.stream().map(Edge::toString).collect(Collectors.joining(", ")), DEBUG);
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


    private void buildIncompatibilityMap(MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> combinedPuM) {

        for (Map.Entry<Role, List<XorGateway>> xors : xors.entrySet()) {

            IPublicModel puM = publicModelsByRole.get(xors.getKey());

            if (puM != null && puM.getdigraph() != null) {
                //Get graph of the corresponding role
                IDirectedGraph<Edge<IPublicNode>, IPublicNode> graph = puM.getdigraph();

                //Iterate over the XOR Gateways from that graph
                for (XorGateway xor : xors.getValue()) {
                    List<Set<Edge<IPublicNode>>> branchInteractions = findCompatibleEdgesForOneXOR(graph, xor, combinedPuM);
                    fillIncompatibilityMap(branchInteractions);
                }
            } else {
                outputHandler.printEasySoundness("Graph for participant " + xors.getKey().getName() + " does not exist anymore, therefore no incompatibility check needs to be performed", INFO);
            }
        }

        outputHandler.printEasySoundness(OutputHandler.EasySoundnessAnalyisBlocks.TRACE_DELIM);
        outputHandler.printEasySoundness("Computed the incompatibility map (interactions that can never be executed in the same trace)", INFO);
        for (Map.Entry<Edge<IPublicNode>, Set<Edge<IPublicNode>>> incomp : incompatibilityMap.entrySet()) {
            outputHandler.printEasySoundness("For " + incomp.getKey() + " incompatible operations are " + incomp.getValue().stream().map(Edge::toString).collect(Collectors.joining(", ")), INFO);
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
     * e.g. a return value of [{IA1 -> IA2}, {IA3 -> IA4}] means that IA1 -> IA2 are on one branch of the XOR, IA3 -> IA4 on another
     */
    private List<Set<Edge<IPublicNode>>> findCompatibleEdgesForOneXOR(IDirectedGraph<Edge<IPublicNode>, IPublicNode> graph, XorGateway xor, MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> combinedPuM) {
        List<Set<Edge<IPublicNode>>> rst = new ArrayList<>();

        List<IPublicNode> succ = new ArrayList<>(graph.getDirectSuccessors(xor));

        for (IPublicNode successorOfInit : succ) {
            Set<Edge<IPublicNode>> edgesOfBranch = new HashSet<>();

            Edge<IPublicNode> firstToAdd = getEdgeConsiderSynchronousTasks(xor, successorOfInit, graph, combinedPuM);
            edgesOfBranch.add(firstToAdd);
//            edgesOfBranch.add(graph.getEdge(xor, successorOfInit));

            getAllNodesOfBranch(graph, xor, successorOfInit, edgesOfBranch, combinedPuM);
            rst.add(new HashSet<>(edgesOfBranch));
            edgesOfBranch.clear();
        }

        return rst;
    }

    /**
     * Gets the edge of two given nodes under consideration of synchronous tasks.
     * If at least one of the nodes is a synchronous task, the edge needs to be taken out of the combined public model
     */
    private Edge<IPublicNode> getEdgeConsiderSynchronousTasks(IPublicNode src, IPublicNode tgt, IDirectedGraph<Edge<IPublicNode>, IPublicNode> graph, MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> combinedPuM) {

        Edge<IPublicNode> edge = graph.getEdge(src, tgt);

        boolean isSrcSyncTask = src.getName().startsWith("S: "), isTgtSyncTask = tgt.getName().startsWith("S: ");

        if (isSrcSyncTask && isTgtSyncTask) {

            IPublicNode srcRst = null, tgtRst = null;

            for (Map.Entry<IPublicNode, List<IPublicNode>> mappingSyncTasks : mapCommonSynchronousTasksToIndividualParts.entrySet()) {
                List<IPublicNode> mappingSyncTaskValue = mappingSyncTasks.getValue();

                if (mappingSyncTaskValue.contains(src)) {
                    srcRst = mappingSyncTasks.getKey();
                }

                if (mappingSyncTaskValue.contains(tgt)) {
                    tgtRst = mappingSyncTasks.getKey();
                }
            }

            if (srcRst == null || tgtRst == null) {
                throw new IllegalStateException("No mapping to a common synchronous task for either source (" + src.getName() + ") or target (" + tgt.getName() + " could be found. There is something wrong with the mapping");
            } else {
                return combinedPuM.getEdge(srcRst, tgtRst);
            }

        } else if (isSrcSyncTask) {
            IPublicNode srcRst = null;

            for (Map.Entry<IPublicNode, List<IPublicNode>> mappingSyncTasks : mapCommonSynchronousTasksToIndividualParts.entrySet()) {
                List<IPublicNode> mappingSyncTaskValue = mappingSyncTasks.getValue();

                if (mappingSyncTaskValue.contains(src)) {
                    srcRst = mappingSyncTasks.getKey();
                }
            }

            if (srcRst == null) {
                throw new IllegalStateException("No mapping to a common synchronous task for source (" + src.getName() + ") could be found. There is something wrong with the mapping");
            } else {

                IPublicNode tgtOfEdge = mappingPuMCombinedPuM.getOrDefault(tgt, tgt);
                Edge<IPublicNode> edgeToReturn = combinedPuM.getEdge(srcRst, tgtOfEdge);
                return edgeToReturn;
            }
        } else if (isTgtSyncTask) {
            IPublicNode tgtRst = null;

            for (Map.Entry<IPublicNode, List<IPublicNode>> mappingSyncTasks : mapCommonSynchronousTasksToIndividualParts.entrySet()) {
                List<IPublicNode> mappingSyncTaskValue = mappingSyncTasks.getValue();

                if (mappingSyncTaskValue.contains(tgt)) {
                    tgtRst = mappingSyncTasks.getKey();
                }
            }
            if (tgtRst == null) {
                throw new IllegalStateException("No mapping to a common synchronous task for source (" + tgt.getName() + ") could be found. There is something wrong with the mapping");
            } else {
                IPublicNode srcOfEdge = mappingPuMCombinedPuM.getOrDefault(src, src);
                Edge<IPublicNode> edgeToReturn = combinedPuM.getEdge(srcOfEdge, tgtRst);
                return edgeToReturn;
            }

        }

        return edge;
    }

    private void getAllNodesOfBranch(IDirectedGraph<Edge<IPublicNode>, IPublicNode> graph, XorGateway xor, IPublicNode start, Set<Edge<IPublicNode>> workingSet, MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> combinedPuM) {

        if (start.getName().equals(xor.getName() + "_m")) {
            return;
        }

        for (IPublicNode child : new ArrayList<>(graph.getDirectSuccessors(start))) {

            Edge<IPublicNode> toAdd = getEdgeConsiderSynchronousTasks(start, child, graph, combinedPuM);
            workingSet.add(toAdd);
//            workingSet.add(graph.getEdge(start, child));
            if (child.getName().equals(xor.getName() + "_m")) {
                return;
            }

            getAllNodesOfBranch(graph, xor, child, workingSet, combinedPuM);
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
            outputHandler.printEasySoundness("Graph of the participant has already been eliminated, as its not executable. Skipping validation for the node\n", INFO);
            return Collections.emptyList();
        }

        //Check if the current node to eliminate with its context is still present, because it might have been eliminated in a previous elimination step
        if (!graphOfNode.getVertices().contains(node)) {
            outputHandler.printEasySoundness("Node has already been eliminated in a previous step, Continuing with the next one", INFO);
            return Collections.emptyList();
        }

        List<IPublicNode> toEliminate = new ArrayList<>();

        //Step 1: Traverse the graph backwards until you find either a XOR node or the start node
        //In case of XOR, the other branches of XOR can potentially be used to get from start to end
        //In case of start, the whole participant cannot execute
        eliminateContextUntilXOROrStart(graphOfNode, node, toEliminate, new ArrayList<>());

        IPublicNode stopNode = toEliminate.remove(toEliminate.size() - 1);

        Role participantOfNode = participantsByName.get(getParticipantOfNode(node));

        if (stopNode instanceof Event) {
            //Stopped at start node
            return handleEliminateCompletePublicModel(participantOfNode);
        } else if (stopNode instanceof Gateway) {
            //Stopped at XOR
            return handleEliminateXORBranch(toEliminate, stopNode, graphOfNode, participantOfNode);
        }
        return Collections.emptyList();
    }


    private List<IPublicNode> handleEliminateCompletePublicModel(Role participantOfNode) {
        outputHandler.printEasySoundness("All nodes of the participant " + participantOfNode.getName() + " are not executable", INFO);
        outputHandler.printEasySoundness(publicModelsByRole.get(participantOfNode).getdigraph().getVertices().stream().map(IGObject::getName).collect(Collectors.joining(",")) + "\n", INFO);

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

        outputHandler.printEasySoundness("Added the new (empty) graph for participant " + participantOfNode + " in EasySoundness/", INFO);

        IOUtils.toFile(GlobalTimestamp.timestamp + "/EasySoundness/EasySoundnessCycleChecks_" + participantOfNode.getName() + "_Change" + eliminationStepsForParticipantsGraph.get(participantOfNode), "");
        eliminationStepsForParticipantsGraph.put(participantOfNode, eliminationStepsForParticipantsGraph.get(participantOfNode) + 1);

        outputHandler.printEasySoundness("The relevant counterpart nodes of that graph therefore need to be eliminated as well: ", INFO);
        outputHandler.printEasySoundness(furtherEliminate.stream().map(IGObject::getName).collect(Collectors.joining(", ")), INFO);

        return furtherEliminate;
    }

    private List<IPublicNode> handleEliminateXORBranch(List<IPublicNode> toEliminate, IPublicNode stopNode, IDirectedGraph<Edge<IPublicNode>, IPublicNode> graphOfNode, Role participantOfNode) {
        //Get the first element on the XOR branch
        IPublicNode firstNodeOnXORbranch = toEliminate.get(toEliminate.size() - 1);
        String xorMergeName = stopNode.getName() + "_m";

        //Step 2 Eliminate all nodes on the path from the XOR through the first node on the branch to the XOR merge
        //if the node on the XOR branch is the last remaining connection, XOR becomes disconnected from XOr merge.
        //Thus, the context of the first node BEFORE the XOR gateway needs to be eliminated.
        List<IPublicNode> xorSucc = new ArrayList<>(graphOfNode.getDirectSuccessors(stopNode));

        if (xorSucc.size() == 1 && toEliminate.contains(firstNodeOnXORbranch)) {
            outputHandler.printEasySoundness("Eliminating the XOR branch containing " + firstNodeOnXORbranch.getName() + " would lead to the XOR being disconnected", INFO);
            outputHandler.printEasySoundness("The branch on which the XOR is present needs to be eliminated", INFO);

            //Only one parent possible, otherwise somethings fucked up
            //IPublicNode xorParent = new ArrayList<>(graphOfNode.getDirectPredecessors(stopNode)).get(0);

            ArrayList<IPublicNode> skip = new ArrayList<>();
            skip.add(stopNode);

            eliminateContextUntilXOROrStart(graphOfNode, stopNode, toEliminate, skip);

//                return eliminateContext(xorParent);

        }

        outputHandler.printEasySoundness("Eliminating nodes on the XOR branch that cannot be executed", INFO);

        List<IPublicNode> furtherEliminate = new ArrayList<>();

        eliminateContextXORBranch(graphOfNode, firstNodeOnXORbranch, xorMergeName, furtherEliminate);

        //Do reduction of the graph
        IPublicModel rpstModel = publicModelsByRole.get(participantOfNode);
        rpstModel.setDiGraph(graphOfNode);
        rpstModel.reduceGraph(xorWithDirectEdgeToMerge);

        publicModelsByRole.put(participantOfNode, rpstModel);

        if (!furtherEliminate.isEmpty()) {
            outputHandler.printEasySoundness("By eliminating the nodes from the XOR branch, the following relevant counterpart nodes also need to be eliminated", INFO);
            outputHandler.printEasySoundness(furtherEliminate.stream().map(IGObject::getName).collect(Collectors.joining(", ")), INFO);
        }

        outputHandler.printEasySoundness("Saved new graph for possible path finding to EasySoundness/", INFO);

        //Export changed graph
        IOUtils.toFile(GlobalTimestamp.timestamp + "/EasySoundness/EasySoundnessCycleChecks_" + participantOfNode.getName() + "_Change" + eliminationStepsForParticipantsGraph.get(participantOfNode) + ".dot", graphOfNode.toDOT());
        eliminationStepsForParticipantsGraph.put(participantOfNode, eliminationStepsForParticipantsGraph.get(participantOfNode) + 1);

        return furtherEliminate;
    }

    /**
     * Iterates from the given XOR-gateway downwards, eliminating all the interactions on all paths to its merge node
     */
    private void eliminateContextXORBranch(IDirectedGraph<Edge<IPublicNode>, IPublicNode> graph, IPublicNode node, String gatewayMergeName, List<IPublicNode> furtherEliminate) {

        //Remove the node and the connection to its parents
        List<IPublicNode> parents = new ArrayList<>(graph.getDirectPredecessors(node));
        List<IPublicNode> children = new ArrayList<>(graph.getDirectSuccessors(node));

        //If the nodes counterpart becomes unexecutable, add it to the nodes to further eliminate

        if (isCounterpartReliantOnExecution(node)) {
            IPublicNode counterpartNode = getCounterpartNode(node);

            if (counterpartNode != null) {
                furtherEliminate.add(counterpartNode);
            }

        }
        removeParentEdgesAndNode(graph, parents, node, true);

        outputHandler.printEasySoundness("Eliminating " + node.getName(), INFO);

        for (IPublicNode child : children) {

            //If the child is the gateway merge node, we have to only remove the connection to it and can return after
            if (child.getName().equals(gatewayMergeName)) {
                removeParentEdgesAndNode(graph, Collections.singletonList(node), child, false);
                return;
            }

            eliminateContextXORBranch(graph, child, gatewayMergeName, furtherEliminate);
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
     * Skip is a list with gateways that need to be skipped during the traversal.
     * This is for instances where an XOR gateway becomes disconnected and thus the traversal needs to continue.
     */
    private void eliminateContextUntilXOROrStart(IDirectedGraph<Edge<IPublicNode>, IPublicNode> graph, IPublicNode node, List<IPublicNode> toEliminate, List<IPublicNode> skip) {

        //If we arrive at the start event we return
        if (node.getName().equals("start")) {
            toEliminate.add(node);
            return;
        }

        /**
         * If we arrive at an XOR_fork, we return iff the corresponding XOR merge is not present in the nodes to be eliminated.
         * That is, as the presence would mean, that the node we started the traversal at is not on an XOR branch, but after a complete XOR-subgraph
         * */
        if (!skip.contains(node) && (node.getName().startsWith("XOR") && !node.getName().endsWith("_m") && toEliminate.stream().noneMatch(n -> n.getName().equals(node.getName() + "_m")))) {
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

        eliminateContextUntilXOROrStart(graph, singleParent, toEliminate, new ArrayList<>());
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
        } else if (node instanceof XorGateway || node instanceof AndGateway) {
            //For gateways description will be e.g. XOR1 P_2
            String roleName = node.getDescription().split(" ")[1];
            Role role = participantsByName.get(roleName);

            if (role != null) {
                IPublicModel puM = publicModelsByRole.get(role);
                return puM != null ? puM.getdigraph() : null;
            } else {
                throw new IllegalArgumentException("In the gateway, no valid participant was associated.");
            }
        } else {
            throw new IllegalArgumentException("Node to get graph to is of unknown type");
        }

    }

    private IPublicNode getCounterpartNode(IPublicNode node) {
        String[] counterpartNameAndRole = getCounterpartInteractionNameAndRole(node);

        String nameOfCounterpartInteraction = counterpartNameAndRole[0];
        String counterpartRole = counterpartNameAndRole[1];

        IPublicModel puM = publicModelsByRole.get(participantsByName.get(counterpartRole));
        IDirectedGraph<Edge<IPublicNode>, IPublicNode> counterpartGraph = puM != null ? puM.getdigraph() : null;

        if (counterpartGraph == null) {
            outputHandler.printEasySoundness("Counterpart Graph has already been eliminated, cannot fetch the counterpart node for " + node.getName() + " anymore", INFO);
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

        if (node instanceof Receive || node instanceof Send) {

            boolean isSenderOrParticipant1 = node.getName().contains("(s)") || node.getName().contains("(p1)");

            String[] participantsOfNode = getParticipantsOfNode(node);

            return participantsOfNode[isSenderOrParticipant1 ? 0 : 1];
        } else if (node instanceof AndGateway || node instanceof XorGateway) {
            return node.getDescription().split(" ")[1];
        }
        throw new IllegalArgumentException("getParticipantOfNode was called with invalid node type");
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

    //Returns true if the counterpart of the given node is reliant on the execution of said node
    //That is, if the given node is a sending part of a Message Exchange or a Handover of Work or if it is a Synchronous Task
    private boolean isCounterpartReliantOnExecution(IPublicNode node) {
        return (node instanceof Send && (node.getName().startsWith("M: ") || node.getName().startsWith("H: "))) || node.getName().startsWith("S: ");
    }

}
