package at.ac.c3pro.io;

import at.ac.c3pro.chormodel.PrivateModel;
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

public class ChoreographyModelToCPN {

    private final List<PrivateModel> privateModels;

    private final List<String> alreadyVisited;

    private final List<String> alreadyCreated;

    private final Map<String, Element> privateNets;

    //Needed for the creation of the global petri net
    private Element netComplete;

    private List<Element> netCompleteElementsWithoutRelevantSync;
    private List<Element> netCompleteElementsRelevantSync;

    private Map<InteractionType, List<String>> interactionTransitions;

    private int currentParticipant;
    private boolean globalStartCreated;
    private boolean globalEndCreated;


    public ChoreographyModelToCPN(List<PrivateModel> privateModels) throws IOException {
        this.privateModels = privateModels;
        this.alreadyVisited = new ArrayList<>();
        this.alreadyCreated = new ArrayList<>();
        this.privateNets = new HashMap<>();

        //Initialize the datastructure to save the interaction-transitions
        this.interactionTransitions = new HashMap<>();
        this.interactionTransitions.put(InteractionType.HANDOVER_OF_WORK, new ArrayList<>());
        this.interactionTransitions.put(InteractionType.MESSAGE_EXCHANGE, new ArrayList<>());
        this.interactionTransitions.put(InteractionType.SHARED_RESOURCE, new ArrayList<>());
        this.interactionTransitions.put(InteractionType.SYNCHRONOUS_ACTIVITY, new ArrayList<>());

        OutputHandler.createOutputFolder("CPNs_private");

        this.netComplete = new Element("net");
        this.netComplete.setAttribute("type", "http://www.yasper.org/specs/epnml-1.1"); // TODO
        this.netComplete.setAttribute("id", "CPN_complete");

        this.netCompleteElementsWithoutRelevantSync = new ArrayList<>();
        this.netCompleteElementsRelevantSync = new ArrayList<>();

        // Generate petri model for each private model of the participants
        for (int i = 0; i < this.privateModels.size(); i++) {

            this.currentParticipant = i;

            this.alreadyCreated.clear();
            this.alreadyVisited.clear();

            String cpnId = "CPN" + i;


            this.privateNets.putIfAbsent(cpnId, buildPNMLForSingleParticipant(this.privateModels.get(i), cpnId));
        }

        createPetriNetInteractions();

        buildGlobalPetriNet();

    }

    private void buildGlobalPetriNet() {

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

//        List<String> synchronousTask = this.interactionTransitions.get(InteractionType.SYNCHRONOUS_ACTIVITY);
//        Collections.sort(synchronousTask);
//
//        createPetriNetInteractionsSynchronousTask(synchronousTask);
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

        for (int i = 0; i < interactions.size() - 2; i += 2) {
            String participant1 = interactions.get(i), participant2 = interactions.get(i + 1);

            //Create the place
            String idGlobalPlace = participant1.split("\\(")[0];
            createPlaceGlobal(idGlobalPlace);

            //Create the arcs: p1 -> sr, sr -> p1, p2 -> sr, sr -> p2
            createArcGlobal(participant1, idGlobalPlace);
            createArcGlobal(idGlobalPlace, participant1);
            createArcGlobal(participant2, idGlobalPlace);
            createArcGlobal(idGlobalPlace, participant2);

        }
    }

//    private void createPetriNetInteractionsSynchronousTask(List<String> interactions) {
//
//        for (int i = 0; i < interactions.size() - 2; i += 2) {
//            String participant1 = interactions.get(i), participant2 = interactions.get(i+1);
//
//            this.netComplete.remove
//        }
//    }

    /**
     * Builds the PNML model for a single participant
     *
     * @param participantModel: the private model of the participant to build the
     *                          PNML of
     */
    private Element buildPNMLForSingleParticipant(PrivateModel participantModel, String cpnId) {

        Element netCurr = new Element("net");
        netCurr.setAttribute("type", "http://www.yasper.org/specs/epnml-1.1"); // TODO
        netCurr.setAttribute("id", cpnId);

        IDirectedGraph<Edge<IPrivateNode>, IPrivateNode> participantModelGraph = participantModel.getdigraph();

        IPrivateNode start = participantModel.getStartEvent();

        // Create place for the start event
        createPlace(start.getName(), netCurr);

//        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

        this.buildPetriNet(participantModelGraph, start, netCurr);

//        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

        return netCurr;
    }

    /**
     * @formatter:off
	 * Builds the elements for the distinct components of the petri net. With (t)
	 * transition and (p) place that means:
	 * 
	 * start event: start(p) -> start_out(t)
	 * end event: end_in(t) -> end(p)
	 * interaction, priv. act: ia_in(p) -> ia(t) -> ia_out(p)
	 * and gateway: and_in(t) -> and(p) -> and_out(t)
	 * xor gateway fork: xor_in (t) -> xor(p) -> xor_to_child1 (t), xor_to_child2(t),...
	 * xor gateway merge: parent1_to_xorm(t), parent2_to_xorm(t),... -> xorm(p) -> xorm_out(t)
	 * 
	 * @formatter:on
     */
    private void buildPetriNet(IDirectedGraph<Edge<IPrivateNode>, IPrivateNode> participantModelGraph,
                               IPrivateNode node, Element netCurr) {

        List<IPrivateNode> nodeChildren = participantModelGraph.getDirectSuccessors(node).stream()
                .collect(Collectors.toList());

        if (!this.alreadyVisited.contains(node.getId())) {

            List<IPrivateNode> nodeParents = participantModelGraph.getDirectPredecessors(node).stream()
                    .collect(Collectors.toList());

            if (node instanceof Event) {
                if (node.getName().equals("start")) {
                    createStart(netCurr);
                } else if (node.getName().equals("end")) {
                    createEnd(netCurr);
                } else {
                    throw new IllegalArgumentException("Received invalid event");
                }
            } else if (node instanceof Send || node instanceof Receive || node instanceof PrivateActivity) {
                createInteractionOrPrivateActivity(node.getName(), nodeChildren, nodeParents, netCurr);
                trackNodeForBuildingOverallPetriNet(node.getName());
            } else if (node instanceof AndGateway) {
                createAnd(node.getName(), nodeChildren, nodeParents, netCurr);
            } else if (node instanceof XorGateway) {
                createXor(node.getName(), nodeChildren, nodeParents, netCurr);
            } else {
                throw new IllegalArgumentException("Received node that is of no valid type");
            }

//            System.out.println("Current Node:");
//            System.out.println(node);
//            System.out.println("Child Nodes:");
//            System.out.println(nodeChildren);
//            System.out.println("Parent Nodes:");
//            System.out.println(nodeParents);
//            System.out.println("-------------------------------------------------------------------");

            this.alreadyVisited.add(node.getId());
        }

        for (IPrivateNode child : nodeChildren) {
            buildPetriNet(participantModelGraph, child, netCurr);
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

        xmlOutput.output(doc, new FileWriter(path + cpnName));
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
     * Transforms a interaction or private activity to its corresponding PNML
     * elements
     */
    private void createInteractionOrPrivateActivity(String id, List<IPrivateNode> children, List<IPrivateNode> parents,
                                                    Element net) {
        String idIn = id + "_in", idOut = id + "_out";

        // Exactly one parent and one child
        IPrivateNode parent = parents.get(0);
        IPrivateNode child = children.get(0);

        createTransition(id, net);

        // In case of a IA -> IA connection we connect them directly without
        // intermediate transaction
        if (!(parent instanceof Send || parent instanceof Receive || parent instanceof PrivateActivity)) {
            createPlace(idIn, net);
            createArc(idIn, id, net);
        }

        createPlace(idOut, net);
        createArc(id, idOut, net);

        // Handle Connection to Parent
        if (parent instanceof Event && parent.getName().equals("start")) {
            // start -> IA ( start_out (t) -> IA_in (p))
            createArc(parent.getNameOut(), idIn, net);
        } else if (parent instanceof XorGateway && parent.getName().contains("_m")) {
            // XOR_m -> IA (XOR_m_out (t) -> IA_in (p))
            createArc(parent.getNameOut(), idIn, net);
        } else if (parent instanceof XorGateway) {
            // XOR -> IA (XOR_to_IA (t) -> IA_in (p))
            String xorToIA = parent.getName() + "_to_" + id;

            createArc(xorToIA, idIn, net);
        } else if (parent instanceof AndGateway) {
            // AND_m -> IA (AND_m (t) -> IA_in (p))
            // AND -> IA (AND (t) -> IA_in)
            createArc(parent.getName(), idIn, net);
        } else if (parent instanceof Send || parent instanceof Receive || parent instanceof PrivateActivity) {
            // IA1 -> IA2 (IA1_out (p) -> IA2 (t) )
            createArc(parent.getNameOut(), id, net);
        }

        // Handle Connection to Child
        if (child instanceof Event && child.getName().equals("end")) {
            // IA -> end (IA_out (p) -> end_in (t))
            createArc(idOut, child.getNameIn(), net);
        } else if (child instanceof XorGateway && child.getName().contains("_m")) {
            // IA->XOR_m (IA_out (p) -> IA_to_XOR_m (t) -> XOR_m (p) )
            String IAtoXorMerge = id + "_to_" + child.getName();
            createArc(idOut, IAtoXorMerge, net);
        } else if (child instanceof XorGateway) {
            // IA -> XOR (IA_out (p) -> XOR_in (t))
            createArc(idOut, child.getNameIn(), net);
        } else if (child instanceof AndGateway) {
            // IA -> AND_m (IA_out (p) -> AND_m (t))
            // IA -> AND (IA_out (p) -> AND (t))
            createArc(idOut, child.getName(), net);
        } else if (child instanceof Send || child instanceof Receive || child instanceof PrivateActivity) {
            // IA1 -> IA2 (IA1 (t) -> IA2_in (p) )
            createArc(id, child.getNameIn(), net);

        }

    }

    private void createAnd(String id, List<IPrivateNode> children, List<IPrivateNode> parents, Element net) {

        boolean isMergeNode = id.contains("_m");

        // Creates the transition for the AND Gateway
        createTransition(id, net);

        if (isMergeNode) {

            for (IPrivateNode parent : parents) {

                if (parent instanceof XorGateway && parent.getName().contains("_m")) {
                    // XOR_m -> AND_m (XOR_m_out (t) -> XOR_m_to_AND_m (p) -> AND_m (t) )

                    String connectorPlace = parent.getName() + "_" + id;

                    createPlace(connectorPlace, net);

                    createArc(parent.getNameOut(), connectorPlace, net);
                    createArc(connectorPlace, id, net);

                } else if (parent instanceof AndGateway && parent.getName().contains("_m")) {
                    String connectorPlace = parent.getName() + "_" + id;

                    createPlace(connectorPlace, net);

                    createArc(parent.getName(), connectorPlace, net);
                    createArc(connectorPlace, id, net);

                } else if (parent instanceof Send || parent instanceof Receive || parent instanceof PrivateActivity) {
                    // IA -> AND_m ( IA_out (p) -> AND_m (t) )

                    createArc(parent.getName(), id, net);

                }
            }

            for (IPrivateNode child : children) {

                if (child instanceof XorGateway && child.getName().contains("_m")) {
                    // AND_m -> XOR_m (AND_m (t) -> XOR_m (p) )
                    createArc(id, child.getName(), net);
                } else if (child instanceof XorGateway) {
                    // AND_m -> XOR (AND_m (t) -> AND_m_XOR (p) -> XOR_in (t) )
                    String connectorPlace = id + "_" + child.getName();

                    createPlace(connectorPlace, net);

                    createArc(id, connectorPlace, net);
                    createArc(connectorPlace, child.getNameIn(), net);

                } else if (child instanceof AndGateway && child.getName().contains("_m")) {
                    // AND1_m -> AND0_m ( AND1_m (t) -> AND1_m_AND0_m (p) -> AND0_m (t) )
                    String connectorPlace = id + "_" + child.getName();

                    createPlace(connectorPlace, net);

                    createArc(id, connectorPlace, net);
                    createArc(connectorPlace, child.getName(), net);

                } else if (child instanceof AndGateway) {
                    // AND1_m -> AND2 (AND1_m (t) -> AND1_m_AND2 (p) -> AND2 (t))
                    String connectorPlace = id + "_" + child.getName();

                    createPlace(connectorPlace, net);

                    createArc(id, connectorPlace, net);
                    createArc(connectorPlace, child.getName(), net);
                } else if (child instanceof Send || child instanceof Receive || child instanceof PrivateActivity) {
                    // AND1_m -> IA (AND1_m (t) -> IA_in (p))
                    createArc(id, child.getNameIn(), net);
                } else if (child instanceof Event && child.getName().equals("end")) {
                    // AND_m -> end (AND_m (t) -> AND_m_end (p) -> end_in (t))
                    String connectorPlace = id + "_" + child.getName();

                    createPlace(connectorPlace, net);

                    createArc(id, connectorPlace, net);
                    createArc(connectorPlace, child.getNameIn(), net);
                }

            }

        } else {

            for (IPrivateNode parent : parents) {

                if (parent instanceof Event && parent.getName().equals("start")) {
                    // start -> AND (start_out (t) -> start_AND (p) -> AND (t)
                    String connectorPlace = parent.getName() + "_" + id;

                    createPlace(connectorPlace, net);

                    createArc(parent.getNameOut(), connectorPlace, net);
                    createArc(connectorPlace, id, net);

                } else if (parent instanceof XorGateway && parent.getName().contains("_m")) {
                    // XOR_m -> AND (XOR_m_out (t) -> XOR_m_AND (p) -> AND (t))
                    String connectorPlace = parent.getName() + "_" + id;

                    createPlace(connectorPlace, net);

                    createArc(parent.getNameOut(), connectorPlace, net);
                    createArc(connectorPlace, id, net);
                } else if (parent instanceof XorGateway) {
                    // XOR -> AND (XOR (p) -> AND (t))

                    createArc(parent.getName(), id, net);
                } else if (parent instanceof AndGateway) {
                    // AND0_m -> AND1 ( AND0_m (t) -> AND0_m_AND1 (p) -> AND1 (t) )
                    // AND0 -> AND1 ( AND0 (t) -> AND0_AND1 (p) -> AND1 (t) )
                    String connectorPlace = parent.getName() + "_" + id;

                    createPlace(connectorPlace, net);

                    createArc(parent.getName(), connectorPlace, net);
                    createArc(connectorPlace, id, net);
                } else if (parent instanceof Send || parent instanceof Receive || parent instanceof PrivateActivity) {
                    // IA -> AND (IA_out (p) -> AND (t))
                    createArc(parent.getNameOut(), id, net);
                }

            }

            for (IPrivateNode child : children) {

                // To be sure; there should never be AndGateway-merge or XorGateway-merge here
                if (child instanceof AndGateway && !child.getName().contains("_m")) {
                    // AND0 -> AND1 (AND0 (t) -> AND0_AND1 (p) -> AND1 (t) )
                    String connectorPlace = id + "_" + child.getName();

                    createPlace(connectorPlace, net);

                    createArc(id, connectorPlace, net);
                    createArc(connectorPlace, child.getName(), net);
                } else if (child instanceof XorGateway && !child.getName().contains("_m")) {
                    // AND -> XOR (AND (t) -> AND_XOR (p) -> XOR_in (t) )
                    String connectorPlace = id + "_" + child.getName();

                    createPlace(connectorPlace, net);

                    createArc(id, connectorPlace, net);
                    createArc(connectorPlace, child.getNameIn(), net);
                } else if (child instanceof Send || child instanceof Receive || child instanceof PrivateActivity) {
                    // AND -> IA (AND (t) -> IA_in (p))
                    createArc(id, child.getNameIn(), net);
                }
            }

        }

    }

    private void createXor(String id, List<IPrivateNode> children, List<IPrivateNode> parents, Element net) {

        boolean isMergeNode = id.contains("_m");

        if (isMergeNode) {
            String idOut = id + "_out";

            createTransition(idOut, net);

            createPlace(id, net);
            createArc(id, idOut, net);

            for (IPrivateNode parent : parents) {

                if (parent instanceof AndGateway && parent.getName().contains("_m")) {
                    // AND_m -> XOR_m ( AND_m (t) -> XOR_m (p) )
                    createArc(parent.getName(), id, net);
                } else if (parent instanceof XorGateway && parent.getName().contains("_m")) {
                    // XOR1_m -> XOR0_m ( XOR1_m_out (t) -> XOR0_m (p) )
                    createArc(parent.getNameOut(), id, net);
                } else if (parent instanceof XorGateway) {
                    // XOR0 -> XOR0_m

                    String transitionOptionalBranch = parent.getName() + "_to_" + id;

                    createTransition(transitionOptionalBranch, net);

                    createArc(parent.getName(), transitionOptionalBranch, net);
                    createArc(transitionOptionalBranch, id, net);
                } else if (parent instanceof Send || parent instanceof Receive || parent instanceof PrivateActivity) {
                    // IA -> XOR_m (IA_out (p) -> IA_to_XOR_m (t) -> XOR (p))
                    String idIn = parent.getName() + "_to_" + id;

                    createTransition(idIn, net);

                    createArc(parent.getNameOut(), idIn, net);
                    createArc(idIn, id, net);
                }

            }

            for (IPrivateNode child : children) {

                if (child instanceof Event && child.getName().equals("end")) {
                    // XOR_m -> end (XOR_m_out (t) -> XOR_m_end (p)-> end_in (t))
                    String connectorPlace = id + "_" + child.getName();

                    createPlace(connectorPlace, net);

                    createArc(idOut, connectorPlace, net);
                    createArc(connectorPlace, child.getNameIn(), net);
                } else if (child instanceof XorGateway && child.getName().contains("_m")) {
                    // XOR1_m -> XOR0_m (XOR1_m_out (t) -> XOR0 (p))
                    createArc(idOut, child.getName(), net);
                } else if (child instanceof XorGateway) {
                    // XOR1_m -> XOR2 (XOR1_m_out (t) -> XOR1_m_XOR2 (p) -> XOR2_in (t) )
                    String connectorPlace = id + "_" + child.getName();

                    createPlace(connectorPlace, net);

                    createArc(idOut, connectorPlace, net);
                    createArc(connectorPlace, child.getNameIn(), net);
                } else if (child instanceof AndGateway) {
                    // XOR_m -> AND_m (XOR_m_out (t) -> XOR_m_AND (p) -> AND_m (t) )
                    // XOR_m -> AND (XOR_m_out (t) -> XOR_m_AND (p) -> AND (t))
                    String connectorPlace = id + "_" + child.getName();

                    createPlace(connectorPlace, net);

                    createArc(idOut, connectorPlace, net);
                    createArc(connectorPlace, child.getName(), net);
                } else if (child instanceof Send || child instanceof Receive || child instanceof PrivateActivity) {
                    // XOR_m -> IA (XOR_m_out (t) -> IA_in (p))
                    createArc(idOut, child.getNameIn(), net);
                }
            }

        } else {

            createPlace(id, net);
            String idIn = id + "_in";

            createTransition(idIn, net);
            createArc(idIn, id, net);

            for (IPrivateNode child : children) {

                if (child instanceof AndGateway) {
                    // XOR -> AND (XOR (p) -> AND (t) )
                    createArc(id, child.getName(), net);

                } else if (child instanceof XorGateway && child.getName().contains("_m")) {
                    // Optional Branch: XOR0 -> XOR0_m (XOR0 (p) -> XOR0_to_XOR0_m (t) -> XOR0_m (p)
                    // )

                    String transitionOptionalBranch = id + "_to_" + child.getName();

                    createTransition(transitionOptionalBranch, net);

                    createArc(id, transitionOptionalBranch, net);
                    createArc(transitionOptionalBranch, child.getName(), net);

                } else if (child instanceof XorGateway) {
                    // XOR0 -> XOR1 ( XOR1_m_out (t) -> XOR1_m_to_XOR0_m (p) -> XOR_0_m_in (t)

                    createArc(id, child.getNameIn(), net);

                } else if (child instanceof Send || child instanceof Receive || child instanceof PrivateActivity) {
                    String idOut = id + "_to_" + child.getName();

                    createTransition(idOut, net);
                    createArc(id, idOut, net);
                }

            }

            for (IPrivateNode parent : parents) {
                if (parent instanceof Event && parent.getName().equals("start")) {
                    // start -> XOR (start_out (t) -> start_XOR (p) -> XOR_in (t) -> XOR
                    // (p) )
                    String connectorPlace = parent.getName() + "_" + id;
                    createPlace(connectorPlace, net);

                    // Start_out->Start_XOR
                    createArc(parent.getNameOut(), connectorPlace, net);
                    // Start_XOR -> XOR_in
                    createArc(connectorPlace, idIn, net);
                    // XOR_in -> XOR
                    createArc(idIn, id, net);
                } else if (parent instanceof XorGateway && parent.getName().contains("_m")) {
                    // XOR1_m -> XOR2 (XOR1_m_out -> XOR1_m_XOR2 (p) -> XOR2_in (t))
                    String connectorPlace = parent.getName() + "_" + id;

                    createPlace(connectorPlace, net);

                    createArc(parent.getNameOut(), connectorPlace, net);
                    createArc(connectorPlace, idIn, net);

                } else if (parent instanceof XorGateway) {
                    // XOR0 -> XOR1 (XOR0 (p) -> XOR1_in (t))
                    createArc(parent.getName(), idIn, net);

                } else if (parent instanceof AndGateway) {
                    // AND_m -> XOR ( AND_m (t) -> AND_m_XOR (p) -> XOR_in (t))
                    // AND -> XOR (AND (t) -> AND_XOR (p) -> XOR_in (t) )
                    String connectorPlace = parent.getName() + "_" + id;

                    createPlace(connectorPlace, net);

                    createArc(parent.getName(), connectorPlace, net);
                    createArc(connectorPlace, idIn, net);
                } else if (parent instanceof Send || parent instanceof Receive || parent instanceof PrivateActivity) {
                    // IA -> XOR (IA_out (p) -> XOR_in (t))
                    createArc(parent.getNameOut(), idIn, net);
                }

            }
        }

    }

    private void createStart(Element net) {
        if (!globalStartCreated) {
            createGlobalStart();
        }

        createPlace("start", net);
        createTransition("start_out", net);

        createArc("start", "start_out", net);

        //Connect start_global_out to the local start
        createArcGlobal("start_global_out", localIdToGlobalId("start"));

        this.alreadyCreated.add("start");
        this.alreadyCreated.add("start_out");
    }

    private void createGlobalStart() {
        //Create global start place
        String idPlace = "start_global";

        createPlaceGlobal(idPlace);

        //Create global start_out transition
        String idTransition = "start_global_out";
        createTransitionGlobal(idTransition);

        //Create arc  between start_global and start_global_out
        createArcGlobal(idPlace, idTransition);

        this.globalStartCreated = true;
    }

    private void createEnd(Element net) {
        if (!globalEndCreated) {
            createGlobalEnd();
        }

        createPlace("end", net);
        createTransition("end_in", net);

        createArc("end_in", "end", net);

        //Connect local end to the end_global_in
        createArcGlobal(localIdToGlobalId("end"), "end_global_in");

        this.alreadyCreated.add("end");
        this.alreadyCreated.add("end_in");
    }

    private void createGlobalEnd() {
        //Create global end_in transition

        String idTransition = "end_global_in";
        createTransitionGlobal(idTransition);

        //Create global end place
        String idPlace = "end_global";
        createPlaceGlobal(idPlace);

        //Create arc  between start_global and start_global_out
        createArcGlobal(idTransition, idPlace);

        this.globalEndCreated = true;
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

        if (!this.alreadyCreated.contains(id)) {

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

            boolean isSourceSyncTask = sourceId.startsWith("S: ");
            boolean isTargetSyncTask = targetId.startsWith("S: ");

            if (isSourceSyncTask && isTargetSyncTask)
                this.netCompleteElementsRelevantSync.add(arcElemGlobal);
            else
                this.netCompleteElementsWithoutRelevantSync.add(arcElemGlobal);
//            this.netComplete.addContent(arcElemGlobal);


            this.alreadyCreated.add(id);
        }

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


        if (!this.alreadyCreated.contains(id)) {

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

//            this.netComplete.addContent(transitionElemGlobal);

            this.alreadyCreated.add(id);
        }

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

        if (!this.alreadyCreated.contains(id)) {

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

//            if (isSyncTask)
//                this.netCompleteElementsRelevantSync.add(placeElemGlobal);
//            else
            this.netCompleteElementsWithoutRelevantSync.add(placeElemGlobal);

//            this.netComplete.addContent(placeElemGlobal);

            this.alreadyCreated.add(id);
        }

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
        return localId + "(P" + this.currentParticipant + ")";
    }

}
