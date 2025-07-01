package at.ac.c3pro.io;

import at.ac.c3pro.chormodel.PrivateModel;
import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.node.*;
import at.ac.c3pro.node.Interaction.InteractionType;
import at.ac.c3pro.util.GlobalTimestamp;
import at.ac.c3pro.util.OutputHandler;
import at.ac.c3pro.util.VisualizationHandler;
import org.jbpt.graph.abs.IDirectedGraph;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ChoreographyModelToPNML {

    private final Map<Role, PrivateModel> privateModels;
    private final Map<String, Element> privateNets;

    //Needed for the creation of the global petri net
    private Element netComplete;

    private List<Element> netCompleteElementsWithoutRelevantSync;
    private List<Element> netCompleteElementsRelevantSync;

    private Map<InteractionType, List<String>> interactionTransitions;
    private String currentParticipant;

    private List<String> localStartIDs;
    private List<String> localEndIDs;

    public ChoreographyModelToPNML(Map<Role, PrivateModel> privateModelsByRole) throws IOException {
        this.privateModels = privateModelsByRole;
        this.privateNets = new HashMap<>();

        localStartIDs = new ArrayList<>();
        localEndIDs = new ArrayList<>();

        //Initialize the datastructure to save the interaction-transitions
        this.interactionTransitions = new HashMap<>();
        this.interactionTransitions.put(InteractionType.HANDOVER_OF_WORK, new ArrayList<>());
        this.interactionTransitions.put(InteractionType.MESSAGE_EXCHANGE, new ArrayList<>());
        this.interactionTransitions.put(InteractionType.SHARED_RESOURCE, new ArrayList<>());
        this.interactionTransitions.put(InteractionType.SYNCHRONOUS_ACTIVITY, new ArrayList<>());

        OutputHandler.createOutputFolder("CPNs_private");

        this.netComplete = new Element("net");
        this.netComplete.setAttribute("id", "CPN_complete");

        this.netCompleteElementsWithoutRelevantSync = new ArrayList<>();
        this.netCompleteElementsRelevantSync = new ArrayList<>();

        // Generate petri model for each private model of the participants
        for (Map.Entry<Role, PrivateModel> entry : this.privateModels.entrySet()) {

            this.currentParticipant = entry.getKey().getName();

            String cpnID = "CPN_" + currentParticipant;

            this.privateNets.putIfAbsent(cpnID, createPNMLRepresentationForSingleParticipant(entry.getValue().getdigraph(), cpnID));
        }

        createPetriNetInteractions();
        buildGlobalPetriNet();
    }


    /**
     * Do an adapted DFS to traverse the graph in the order needed to insert CPEE elements "on the fly"
     */
    private Element createPNMLRepresentationForSingleParticipant(IDirectedGraph<Edge<IPrivateNode>, IPrivateNode> graph, String cpnId) {

        Element rstNet = new Element("net");
        rstNet.setAttribute("id", cpnId);

        Map<IPrivateNode, String> inputPointsLocal = new HashMap<>();
        Map<IPrivateNode, String> outputPointsLocal = new HashMap<>();


        IPrivateNode beginNode = graph.getVertices().stream().filter(v -> v.getName().equals("start")).collect(Collectors.toList()).get(0);
        Map<IPrivateNode, Boolean> visited = graph.getVertices().stream().collect(Collectors.toMap(v -> v, v -> false));

        visited.put(beginNode, Boolean.TRUE);

        List<IPrivateNode> queue = new ArrayList<>();
        queue.add(beginNode);

        while (!queue.isEmpty()) {

            IPrivateNode curr = queue.remove(queue.size() - 1);
            visited.put(curr, Boolean.TRUE);

            List<IPrivateNode> predecessors = new ArrayList<>(graph.getDirectPredecessors(curr));

            if (curr instanceof Receive || curr instanceof Send || curr instanceof PrivateActivity) {

                trackNodeForBuildingOverallPetriNet(curr.getName());

                //Only one pred possible
                IPrivateNode pred = predecessors.get(0);

                //Only start possible
                if (pred instanceof Event) {
                    createTransition(curr.getName(), rstNet);
                    createArc(outputPointsLocal.get(pred), curr.getName(), rstNet);
                } else if (pred instanceof Send || pred instanceof Receive || pred instanceof PrivateActivity || pred instanceof AndGateway) {

                    String placeId = outputPointsLocal.get(pred) + "_to_" + curr.getName();

                    createPlace(placeId, rstNet);
                    createTransition(curr.getName(), rstNet);
                    createArc(outputPointsLocal.get(pred), placeId, rstNet);
                    createArc(placeId, curr.getName(), rstNet);

                } else if (pred instanceof XorGateway) {

                    createTransition(curr.getName(), rstNet);
                    createArc(outputPointsLocal.get(pred), curr.getName(), rstNet);

                    outputPointsLocal.put(curr, curr.getName());

                }
                outputPointsLocal.put(curr, curr.getName());

                System.out.println("Interaction " + curr.getName());
                queue.addAll(graph.getDirectSuccessors(curr));
            } else if (curr instanceof AndGateway) {

                if (curr.getName().endsWith("_m")) {
                    System.out.println("And merge " + curr.getName());

                    //Only continue if all branches of AND have already been traversed/added
                    if (predecessors.stream().allMatch(visited::get)) {

                        createTransition(curr.getName(), rstNet);
                        //P = Event not possible, P = AndGateway -> only merge possible
                        for (IPrivateNode p : predecessors) {

                            if (p instanceof Send || p instanceof Receive || p instanceof PrivateActivity || p instanceof AndGateway) {
                                String placeId = outputPointsLocal.get(p) + "_to_" + curr.getName();

                                createPlace(placeId, rstNet);
                                createArc(outputPointsLocal.get(p), placeId, rstNet);
                                createArc(placeId, curr.getName(), rstNet);

                            } else if (p instanceof XorGateway) {
                                //Only XOR Merge possible
                                createArc(outputPointsLocal.get(p), curr.getName(), rstNet);
                            }
                        }
                        outputPointsLocal.put(curr, curr.getName());

                        queue.addAll(graph.getDirectSuccessors(curr));
                    }

                } else {
                    System.out.println("And fork " + curr.getName());

                    //Only one pred possible
                    IPrivateNode pred = predecessors.get(0);


                    if (pred instanceof Event) {
                        //pred is start node
                        String placeId = outputPointsLocal.get(pred) + "_to_" + curr.getName();

                        createTransition(curr.getName(), rstNet);
                        createArc(outputPointsLocal.get(pred), curr.getName(), rstNet);

                        outputPointsLocal.put(curr, curr.getName());
                    } else if (pred instanceof AndGateway) {
                        String placeId = outputPointsLocal.get(pred) + "_to_" + curr.getName();

                        createPlace(placeId, rstNet);
                        createTransition(curr.getName(), rstNet);

                        createArc(outputPointsLocal.get(pred), placeId, rstNet);
                        createArc(placeId, curr.getName(), rstNet);

                        outputPointsLocal.put(curr, curr.getName());

                    } else if (pred instanceof XorGateway) {
                        //Only fork possible

                        createTransition(curr.getName(), rstNet);
                        createArc(outputPointsLocal.get(pred), curr.getName(), rstNet);

                        outputPointsLocal.put(curr, curr.getName());
                    } else if (pred instanceof Send || pred instanceof Receive || pred instanceof PrivateActivity) {
                        //No additional elements needed, as the child connections will go out of the Send/Receive/PA
                        outputPointsLocal.put(curr, pred.getName());
                    }

                    queue.addAll(graph.getDirectSuccessors(curr));
                }

            } else if (curr instanceof XorGateway) {

                if (curr.getName().endsWith("_m")) {
                    System.out.println("XOR merge " + curr.getName());


                    //Only continue if all branches of AND have already been traversed/added
                    List<IPrivateNode> pred = new ArrayList<>(graph.getDirectPredecessors(curr));

                    if (pred.stream().allMatch(visited::get)) {

                        createPlace(curr.getName(), rstNet);

                        for (IPrivateNode p : pred) {

                            if (p instanceof AndGateway || p instanceof Send || p instanceof Receive || p instanceof PrivateActivity) {
                                createArc(outputPointsLocal.get(p), curr.getName(), rstNet);
                            } else if (p instanceof XorGateway) {

                                String transId = outputPointsLocal.get(p) + "_to_" + curr.getName();

                                createTransition(transId, rstNet);
                                createArc(outputPointsLocal.get(p), transId, rstNet);
                                createArc(transId, curr.getName(), rstNet);


                            }

                        }

                        outputPointsLocal.put(curr, curr.getName());

                        queue.addAll(graph.getDirectSuccessors(curr));
                    } else {
//                        sb.append(CPEETemplates.EXCLUSIVE_BRANCH_START);
                    }
                } else {
                    System.out.println("XOR fork " + curr.getName());

                    //Only one pred possible
                    IPrivateNode pred = predecessors.get(0);

                    //Only start possible
                    if (pred instanceof Event) {
                        //No additional place needed, the successors will be connected to start directly
                        outputPointsLocal.put(curr, pred.getName());
                    } else if (pred instanceof Send || pred instanceof Receive || pred instanceof PrivateActivity || pred instanceof AndGateway) {

                        createPlace(curr.getName(), rstNet);
                        createArc(outputPointsLocal.get(pred), curr.getName(), rstNet);

                        outputPointsLocal.put(curr, curr.getName());
                    } else if (pred instanceof XorGateway) {

                        String transId = outputPointsLocal.get(pred) + "_to_" + curr.getName();

                        createTransition(transId, rstNet);
                        createPlace(curr.getName(), rstNet);

                        createArc(outputPointsLocal.get(pred), transId, rstNet);
                        createArc(transId, curr.getName(), rstNet);

                        outputPointsLocal.put(curr, curr.getName());

                    }


                    //always put the XOR_m if present last to ensure the optional step being present in the generated model
                    List<IPrivateNode> succWorkingSet = new ArrayList<>(graph.getDirectSuccessors(curr));
                    IPrivateNode merge = null;
                    List<IPrivateNode> succ = new ArrayList<>();
                    for (IPrivateNode s : succWorkingSet) {
                        if (s.getName().equals(curr.getName() + "_m")) {
                            merge = s;
                        } else {
                            succ.add(s);
                        }
                    }

                    if (merge != null) {
                        succ.add(merge);
                    }

                    queue.addAll(succ);
                }

            } else {
                //Event
                if (curr.getName().equals("start")) {

                    createPlace("start", rstNet);
                    outputPointsLocal.put(curr, "start");

                    localStartIDs.add(localIdToGlobalId("start"));

                    //Add all successors to queue
                    queue.addAll(graph.getDirectSuccessors(curr));
                } else {
                    //end

                    //Only one pred possible
                    IPrivateNode pred = predecessors.get(0);

                    if (pred instanceof Send || pred instanceof Receive || pred instanceof PrivateActivity || pred instanceof AndGateway) {
                        createPlace("end", rstNet);
                        createArc(outputPointsLocal.get(pred), "end", rstNet);

                        outputPointsLocal.put(curr, "end");
                        localEndIDs.add(localIdToGlobalId("end"));
                    } else if (pred instanceof XorGateway) {

                        //Only merge possible
                        String transId = outputPointsLocal.get(pred) + "_to_end";

                        createTransition(transId, rstNet);
                        createPlace("end", rstNet);

                        createArc(outputPointsLocal.get(pred), transId, rstNet);
                        createArc(transId, "end", rstNet);

                        outputPointsLocal.put(curr, "end");
                        localEndIDs.add(localIdToGlobalId("end"));
                    }

                    return rstNet;
                }

            }
        }

        return null;
    }

    private void trackNodeForBuildingOverallPetriNet(String name) {
        String messageType = name.split(":")[0];

        switch (messageType) {
            case "H":
                this.interactionTransitions.get(InteractionType.HANDOVER_OF_WORK).add(localIdToGlobalId(name));
                break;
            case "M":
                this.interactionTransitions.get(InteractionType.MESSAGE_EXCHANGE).add(localIdToGlobalId(name));
                break;
            case "R":
                this.interactionTransitions.get(InteractionType.SHARED_RESOURCE).add(localIdToGlobalId(name));
                break;
            case "S":
                this.interactionTransitions.get(InteractionType.SYNCHRONOUS_ACTIVITY).add(localIdToGlobalId(name));
                break;
        }
    }


    /**
     * Prints the CPN.xml file to the output folder
     *
     * @throws InterruptedException
     */
    public void printXMLs(boolean visualRepresentation) throws IOException, InterruptedException {

        for (Map.Entry<String, Element> privModelCPN : this.privateNets.entrySet()) {
            printOneXML(privModelCPN.getKey(), privModelCPN.getValue());
        }

        printOneXML("CPN_complete", this.netComplete);

        if (visualRepresentation) {
            VisualizationHandler.visualize(VisualizationHandler.VisualizationType.PETRI_NET);
        }

    }

    private void printOneXML(String name, Element element) throws IOException {
        Document doc = new Document();
        Element pnml = new Element("pnml");

        pnml.setContent(element);
        doc.setRootElement(pnml);

        XMLOutputter xmlOutput = new XMLOutputter();

        // Pretty Print
        xmlOutput.setFormat(Format.getPrettyFormat());

        String path = "target/" + GlobalTimestamp.timestamp + "/CPNs_private/";
        String cpnName = "/" + name + ".pnml";


        try (FileWriter writer = new FileWriter(path + cpnName)) {
            xmlOutput.output(doc, writer);
        }
    }

    private void buildGlobalPetriNet() {

        //Build global start and end
        createPlaceGlobal("startGlobal");
        createTransitionGlobal("startGlobal_out");
        createArcGlobal("startGlobal", "startGlobal_out");
        this.localStartIDs.forEach(st -> createArcGlobal("startGlobal_out", st));

        createPlaceGlobal("endGlobal");
        createTransitionGlobal("endGlobal_in");
        createArcGlobal("endGlobal_in", "endGlobal");
        this.localEndIDs.forEach(e -> createArcGlobal(e, "endGlobal_in"));


        //Build a map in which "wrongly" named transitions map to the correct ones
        Map<String, String> replacementTransitions = new HashMap<>();
        Set<String> transitionsToCreate = new HashSet<>();

        Iterator<Element> iter = this.netCompleteElementsRelevantSync.iterator();
        while (iter.hasNext()) {
            Element currElem = iter.next();

            if (currElem.getName().equals("transition")) {

                String transId = currElem.getAttributeValue("id");

                String correctTransId = transId.split(" ")[0] + " " + transId.split(" ")[1];

                replacementTransitions.putIfAbsent(transId, correctTransId);
                transitionsToCreate.add(correctTransId);
                iter.remove();
            }
        }

        //Create new transitions
        for (String transitionId : transitionsToCreate) {
            createTransitionGlobal(transitionId);
        }

        //Add all the elements that dont need special handling + newly created transitions (sync tasks)
        for (Element elem : this.netCompleteElementsWithoutRelevantSync) {
            this.netComplete.addContent(elem);
        }

        //iterate over the remaining elements (arcs)
        for (Element arcElem : this.netCompleteElementsRelevantSync) {

            String sourceReplacement = replacementTransitions.getOrDefault(arcElem.getAttributeValue("source"), null);
            String targetReplacement = replacementTransitions.getOrDefault(arcElem.getAttributeValue("target"), null);

            if (sourceReplacement != null) {
                arcElem.setAttribute("source", sourceReplacement);
            }

            if (targetReplacement != null) {
                arcElem.setAttribute("target", targetReplacement);
            }
        }

        //Add the corrected sync tasks to the global petri net
        for (Element elem : this.netCompleteElementsRelevantSync) {
            this.netComplete.addContent(elem);
        }

    }

    /**
     * Builds the connections in the global petri net on the basis of the generated transactions representing the interactions
     */
    private void createPetriNetInteractions() {

        List<String> msgAndHow = this.interactionTransitions.get(InteractionType.MESSAGE_EXCHANGE);
        msgAndHow.addAll(this.interactionTransitions.get(InteractionType.HANDOVER_OF_WORK));
        Collections.sort(msgAndHow);

        createPetriNetInteractionsMsgAndHow(msgAndHow);

        List<String> resourceShare = this.interactionTransitions.get(InteractionType.SHARED_RESOURCE);
        Collections.sort(resourceShare);

        createPetriNetInteractionsSharedResource(resourceShare);

    }

    /**
     * Creates the connections in the global petri net for the types Hanover-of-Work and Message Exchange
     * Both cases are a connection from the sending participants transaction to a message channel (place) and from there to the
     * to the receiving participants transaction
     */
    private void createPetriNetInteractionsMsgAndHow(List<String> interactions) {

        //As the list is lexicographically ordered we can be sure at that point that (if every HOW/MSG has a corresponding part)
        //that they will be at subsequent indices in the order receiver, sender.

        for (int i = 0; i < interactions.size() - 1; i += 2) {

            String sender = interactions.get(i + 1), receiver = interactions.get(i);

            //Create the place
            String idGlobalPlace = sender.split("\\(")[0];
            createPlaceGlobal(idGlobalPlace);

            //Create Arcs from and to the place
            createArcGlobal(sender, idGlobalPlace);
            createArcGlobal(idGlobalPlace, receiver);
        }
    }

    /**
     * Creates the connections in the global petri net of the type Shared Resource.
     * For such interactions, there are two connections from the senders as well as the receivers transition
     * to the shared resource (place); One going to the place, one coming from the place
     */
    private void createPetriNetInteractionsSharedResource(List<String> interactions) {

        while (!interactions.isEmpty()) {

            String p1 = interactions.remove(0);

            //Two adjacent belong together
            if (interactions.get(0).split("\\(")[0].equals(p1.split("\\(")[0])) {

                String p2 = interactions.remove(0);

                //Create the place
                String idGlobalPlace = p1.split("\\(")[0];
                createPlaceGlobal(idGlobalPlace);

                //Create the arcs: p1 -> sr, sr -> p1, p2 -> sr, sr -> p2
                createArcGlobal(p1, idGlobalPlace);
                createArcGlobal(idGlobalPlace, p1);
                createArcGlobal(p2, idGlobalPlace);
                createArcGlobal(idGlobalPlace, p2);
            }

        }
    }

    /**
     * ===================================================TAG-CREATION========================================================================
     */

    /***
     *  Wrapper method to generate arc that needs to be in the local and global petri net
     */
    private void createArc(String sourceId, String targetId, Element net) {
        createArcImpl(sourceId, targetId, net, false);
    }

    /**
     * Wrapper method to generate arc that only needs to be in the global petri net
     */
    private void createArcGlobal(String sourceId, String targetId) {
        createArcImpl(sourceId, targetId, this.netComplete, true);
    }

    /**
     * Creates an arc given the source and target element
     *
     * @param sourceId: The source of the arc
     * @param targetId: The target of the arc
     * @param net:      The net to add the arc in
     * @return an <arc>-tag
     */
    private void createArcImpl(String sourceId, String targetId, Element net, boolean onlyGlobal) {
        String id = sourceId + "_to_" + targetId;


        if (!onlyGlobal) {
            //Add to local petri net
            Element arcElem = new Element("arc");

            arcElem.setAttribute("id", id);

            arcElem.setAttribute("source", sourceId);
            arcElem.setAttribute("target", targetId);

            net.addContent(arcElem);
        }

        //add to global petri net
        Element arcElemGlobal = new Element("arc");

        arcElemGlobal.setAttribute("id", onlyGlobal ? id : localIdToGlobalId(id));

        arcElemGlobal.setAttribute("source", onlyGlobal ? sourceId : localIdToGlobalId(sourceId));
        arcElemGlobal.setAttribute("target", onlyGlobal ? targetId : localIdToGlobalId(targetId));

        boolean isSourceSyncTask = sourceId.startsWith("S: ") || sourceId.contains("_to_S:");
        boolean isTargetSyncTask = targetId.startsWith("S: ") || targetId.contains("_to_S:");

        if (isSourceSyncTask || isTargetSyncTask)
            this.netCompleteElementsRelevantSync.add(arcElemGlobal);
        else
            this.netCompleteElementsWithoutRelevantSync.add(arcElemGlobal);
    }

    /***
     *  Wrapper method to generate transition that needs to be in the local and global petri net
     */
    private void createTransition(String id, Element net) {
        createTransitionImpl(id, net, false);
    }

    /**
     * Wrapper method to generate transition that only needs to be in the global petri net
     */
    private void createTransitionGlobal(String id) {
        createTransitionImpl(id, this.netComplete, true);
    }

    /**
     * Creates a transition element with given dimensions (synchronous task)
     *
     * @param id:  the id of the element
     * @param net: the net to add the transition in
     * @return a <transition>-tag
     */
    private void createTransitionImpl(String id, Element net, boolean onlyGlobal) {


        if (!onlyGlobal) {
            //add to local petri net
            Element transitionElem = new Element("transition");

            transitionElem.setAttribute("id", id);
            net.addContent(transitionElem);
        }

        //add to global petri net
        Element transitionElemGlobal = new Element("transition");

        transitionElemGlobal.setAttribute("id", onlyGlobal ? id : localIdToGlobalId(id));

        boolean isRelevantSyncTask = id.startsWith("S: ") && !id.contains("_to_");

        if (isRelevantSyncTask)
            this.netCompleteElementsRelevantSync.add(transitionElemGlobal);
        else
            this.netCompleteElementsWithoutRelevantSync.add(transitionElemGlobal);
    }

    /***
     *  Wrapper method to generate place that needs to be in the local and global petri net
     */
    private void createPlace(String id, Element net) {
        createPlaceImpl(id, net, false);
    }

    /**
     * Wrapper method to generate place that only needs to be in the global petri net
     */
    private void createPlaceGlobal(String id) {
        createPlaceImpl(id, this.netComplete, true);
    }

    /**
     * Creates a place element
     *
     * @param id:  The id of the place (will also be the name)
     * @param net: the net to add the place in
     * @return a <place>-element
     */

    private void createPlaceImpl(String id, Element net, boolean onlyGlobal) {


        if (!onlyGlobal) {
            //add to local petri net
            Element placeElem = new Element("place");

            placeElem.setAttribute("id", id);
            placeElem.addContent(getNameElement(id));
            net.addContent(placeElem);
        }

        //add to global petri net
        Element placeElemGlobal = new Element("place");

        String globalId = onlyGlobal ? id : localIdToGlobalId(id);
        placeElemGlobal.setAttribute("id", globalId);
        placeElemGlobal.addContent(getNameElement(globalId));

        boolean isSyncTask = id.startsWith("S: ");

        this.netCompleteElementsWithoutRelevantSync.add(placeElemGlobal);

    }

    /**
     * Gets a name-tag
     *
     * @param name: the name to be given
     * @return: a <name>-element
     */
    private Element getNameElement(String name) {
        Element nameElem = new Element("name");

        Element textElem = new Element("text");
        textElem.addContent(name);

        nameElem.addContent(textElem);

        return nameElem;

    }

    private String localIdToGlobalId(String localId) {

        return localId + "(" + this.currentParticipant + ")";
    }
}
