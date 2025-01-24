package at.ac.c3pro.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jbpt.graph.abs.IDirectedGraph;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import at.ac.c3pro.chormodel.PrivateModel;
import at.ac.c3pro.node.AndGateway;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.Event;
import at.ac.c3pro.node.IPrivateNode;
import at.ac.c3pro.node.PrivateActivity;
import at.ac.c3pro.node.Receive;
import at.ac.c3pro.node.Send;
import at.ac.c3pro.node.XorGateway;

public class ChoreographyModelToCPN {

	private List<PrivateModel> privateModels;

	private List<String> alreadyVisited;

	private List<String> alreadyCreated;

	private File outputFolder;
	private static String formattedDate;

	// The parent element of all the petri net tags
//	private Element net;

	private Map<String, Element> privateNets;

	public ChoreographyModelToCPN(List<PrivateModel> pPrivateModels, String pFormattedDate) throws IOException {
		this.privateModels = pPrivateModels;
		this.formattedDate = pFormattedDate;
		this.alreadyVisited = new ArrayList<>();
		this.alreadyCreated = new ArrayList<>();
		this.privateNets = new HashMap<>();

		this.outputFolder = createOutputFolder();

//		this.net = new Element("net");
//		this.net.setAttribute("type", "http://www.yasper.org/specs/epnml-1.1");
//		this.net.setAttribute("id", "CPN1"); // TODO: do i need something dynamic for the ID?

		// Generate petri model for each private model of the participants
		for (int i = 0; i < privateModels.size(); i++) {
			String cpnId = "CPN" + i;
			this.privateNets.putIfAbsent(cpnId, this.buildPNMLForSingleParticipant(privateModels.get(i), cpnId));
		}

	}

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

//		System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

		this.buildPetriNet(participantModelGraph, start, netCurr);

//		System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

//		for (Edge<IPrivateNode> e : participantModel.getdigraph().getEdges()) {
//			System.out.println(e);
//		}
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
			} else if (node instanceof AndGateway) {
				createAnd(node.getName(), nodeChildren, nodeParents, netCurr);
			} else if (node instanceof XorGateway) {
				createXor(node.getName(), nodeChildren, nodeParents, netCurr);
			} else {
				throw new IllegalArgumentException("Received node that is of no valid type");
			}

//			System.out.println("Current Node:");
//			System.out.println(node);
//			System.out.println("Child Nodes:");
//			System.out.println(nodeChildren);
//			System.out.println("Parent Nodes:");
//			System.out.println(nodeParents);
//			System.out.println("-------------------------------------------------------------------");

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
			Document doc = new Document();
			Element pnml = new Element("pnml");

			pnml.setContent(privModelCPN.getValue());
			doc.setRootElement(pnml);

			XMLOutputter xmlOutput = new XMLOutputter();

			// Pretty Print
			xmlOutput.setFormat(Format.getPrettyFormat());

			String cpnName = "/" + privModelCPN.getKey() + ".pnml";
			xmlOutput.output(doc, new FileWriter(outputFolder + cpnName));
		}

		if (visualRepresentation) {
			ProcessBuilder processBuilder = new ProcessBuilder("python", "resources/generatePetrinetVisualization.py",
					outputFolder.toString().substring(7, 26));
			processBuilder.redirectErrorStream(true);

			Process process = processBuilder.start();
			process.waitFor();
		}

	}

	/**
	 * @throws Exception
	 * 
	 */
	private static File createOutputFolder() throws IOException {
		File dir = new File("target/" + formattedDate + "/CPNs_private");

		if (!dir.exists()) {
			boolean created = dir.mkdir();
			if (created) {
				System.out.println("Directory created successfully!");
			} else {
				throw new IOException("Failed to create the directory.");
			}
		} else {
			System.out.println("Directory already exists.");
		}

		return dir;
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
		createPlace("start", net);
		createTransition("start_out", net);

		createArc("start", "start_out", net);

		this.alreadyCreated.add("start");
		this.alreadyCreated.add("start_out");
	}

	private void createEnd(Element net) {
		createPlace("end", net);
		createTransition("end_in", net);

		createArc("end_in", "end", net);

		this.alreadyCreated.add("end");
		this.alreadyCreated.add("end_in");
	}

	/**
	 * ===================================================TAG-CREATION========================================================================
	 */

	/**
	 * Creates an arc given the source and target element
	 * 
	 * @param id:     The id of the arc
	 * @param source: The source of the arc
	 * @param target: The target of the arc
	 * 
	 * @return an <arc>-tag
	 */
	private void createArc(String sourceId, String targetId, Element net) {
		String id = sourceId + "_to_" + targetId;

		if (!this.alreadyCreated.contains(id)) {
			Element arcElem = new Element("arc");
			arcElem.setAttribute("id", id);

			arcElem.setAttribute("source", sourceId);
			arcElem.setAttribute("target", targetId);

			net.addContent(arcElem);

			this.alreadyCreated.add(id);
		}

	}

	/**
	 * Creates a transition element with given dimensions (synchronous task)
	 * 
	 * @param id:   the id of the element
	 * @param posX: the x-coordinate of the element
	 * @param posY: the y-coordinate of the element
	 * @param dimX: the width
	 * @param dimY: the height
	 * 
	 * @return a <transition>-tag
	 */
	private void createTransition(String id, Element net) {

		if (!this.alreadyCreated.contains(id)) {
			Element transitionElem = new Element("transition");
			transitionElem.setAttribute("id", id);
			net.addContent(transitionElem);
			this.alreadyCreated.add(id);
		}

	}

	/**
	 * Creates a place element
	 * 
	 * @param id:   The id of the place (will also be the name)
	 * @param posX: The x-coord of the position
	 * @param posY: The y-coord of the position
	 * 
	 * @return a <place>-element
	 */

	private void createPlace(String id, Element net) {

		if (!this.alreadyCreated.contains(id)) {
			Element placeElem = new Element("place");

			placeElem.setAttribute("id", id);
			placeElem.addContent(getNameElement(id));
			net.addContent(placeElem);
			this.alreadyCreated.add(id);
		}

	}

	/**
	 * Gets a name-tag
	 * 
	 * @param name: the name to be given
	 * 
	 * @return: a <name>-element
	 */
	private Element getNameElement(String name) {
		Element nameElem = new Element("name");

		Element textElem = new Element("text");
		textElem.addContent(name);

		nameElem.addContent(textElem);

		return nameElem;

	}

}
