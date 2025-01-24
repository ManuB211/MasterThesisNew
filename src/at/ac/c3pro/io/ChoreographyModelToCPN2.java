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
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.IPrivateNode;

public class ChoreographyModelToCPN2 {

	private enum PNMLElementEnum {
		PLACE, TRANSITION
	}

	private final Integer DIMENSION_PLACE = 20;
	private final Integer DIMENSION_TRANSITION = 32;

	// Spacing between siblings
	private final Integer X_OFFSET = 300;
	// Spacing between parent/child
	private final Integer Y_OFFSET = 300;

	private List<PrivateModel> privateModels;

	private File outputFolder;

	private Map<String, Position> positions;
	private Map<String, Integer> shifts;

	// The parent element of all the petri net tags
	private Element net;

	public ChoreographyModelToCPN2(List<PrivateModel> pPrivateModels, File pOutputFolder) {
		this.privateModels = pPrivateModels;
		this.outputFolder = pOutputFolder;
		this.positions = new HashMap<>();
		this.shifts = new HashMap<>();

		this.net = new Element("net");
		this.net.setAttribute("type", "http://www.yasper.org/specs/epnml-1.1");
		this.net.setAttribute("id", "CPN1"); // TODO: do i need something dynamic for the ID?

		// this.test();

		PrivateModel prModel1 = privateModels.get(1);

		this.buildPNMLForSingleParticipant(prModel1);

	}

	/**
	 * Sets up the basic structure of the document, meaning the xml, pnml and net
	 * tags
	 */
	private void test() {

		createPlace("p1", 530, 80);
		createPlace("p2", 530, 380);
		createTransition("tr1", 530, 230);
		createArc("p1", "tr1");
		createArc("tr1", "p2");

	}

	/**
	 * Builds the PNML model for a single participant
	 * 
	 * @param participantModel: the private model of the participant to build the
	 *                          PNML of
	 */
	private void buildPNMLForSingleParticipant(PrivateModel participantModel) {

		IDirectedGraph<Edge<IPrivateNode>, IPrivateNode> participantModelGraph = participantModel.getdigraph();

		IPrivateNode start = participantModel.getStartEvent();

		this.handlePositioning(participantModelGraph, start);

		// Create place for the start event
		createPlace(start.getName(), this.positions.get(start.getName()).x, this.positions.get(start.getName()).y);

		this.buildPetriNet(participantModelGraph, start, new ArrayList<>());

		for (Edge<IPrivateNode> e : participantModel.getdigraph().getEdges()) {
			System.out.println(e);
		}
	}

	private void buildPetriNet(IDirectedGraph<Edge<IPrivateNode>, IPrivateNode> participantModelGraph,
			IPrivateNode node, List<IPrivateNode> nodeChildren) {

		/**
		 * // Get all the children of currently visited node List<IPrivateNode>
		 * nodeChildren = participantModelGraph.getDirectSuccessors(node).stream()
		 * .collect(Collectors.toList());
		 * 
		 * // Iterate over children for (IPrivateNode childNode : nodeChildren) {
		 * 
		 * String childNodeName = childNode.getName(); Position childNodePosition =
		 * this.positions.get(childNodeName);
		 * 
		 * // Create place for node if (childNode instanceof Receive || childNode
		 * instanceof Send) {
		 * 
		 * createInteraction(childNodeName, childNodePosition.x, childNodePosition.y);
		 * 
		 * // Create arc from parent to children
		 * 
		 * if (node instanceof AndGateway) { createArc(node.getName(),
		 * childNode.getName() + "_in"); } else if (node instanceof XorGateway) {
		 * continue; } else if (node instanceof Send || node instanceof Receive) {
		 * createArc(node.getName() + "_out", childNode.getName() + "_in"); } else if
		 * (node instanceof Event) { createArc(node.getName(), childNode.getName() +
		 * "_in"); }
		 * 
		 * } else if (childNode instanceof AndGateway) {
		 * 
		 * createAnd(childNodeName, childNodePosition.x, childNodePosition.y);
		 * 
		 * if (node instanceof AndGateway) { createArc(node.getName(),
		 * childNode.getName()); } else if (node instanceof XorGateway) { continue; }
		 * else if (node instanceof Send || node instanceof Receive) {
		 * createArc(node.getName() + "_out", childNode.getName()); } else if (node
		 * instanceof Event) { createArc(node.getName(), childNode.getName()); }
		 * 
		 * } else if (childNode instanceof XorGateway) { List<IPrivateNode> xorChildren
		 * = participantModelGraph.getDirectSuccessors(childNode).stream()
		 * .collect(Collectors.toList());
		 * 
		 * boolean isMergeNode = childNode.getName().contains("_m");
		 * 
		 * if (!isMergeNode) { for (IPrivateNode xorChild : xorChildren) {
		 * createXor(xorChild, isMergeNode);
		 * 
		 * if (node instanceof AndGateway) { createArc(node.getName(),
		 * childNode.getName() + "_xor_in"); } else if (node instanceof XorGateway) {
		 * continue; } else if (node instanceof Send || node instanceof Receive) {
		 * createArc(node.getName() + "_out", childNode.getName()); } else if (node
		 * instanceof Event) { createArc(node.getName(), childNode.getName()); } } }
		 * 
		 * if (!isMergeNode) {
		 * 
		 * }
		 * 
		 * // createXOR(); }
		 * 
		 * // Child node is End-Event else if (childNode instanceof Event) {
		 * createPlace(childNodeName, childNodePosition.x, childNodePosition.y);
		 * 
		 * // Create arc from parent to children if (node instanceof AndGateway) {
		 * createArc(node.getName(), childNode.getName()); } else if (node instanceof
		 * XorGateway) { continue; } else if (node instanceof Send || node instanceof
		 * Receive) { createArc(node.getName() + "_out", childNode.getName()); }
		 * 
		 * }
		 * 
		 * buildPetriNet(participantModelGraph, childNode);
		 * 
		 * }
		 * 
		 **/

	}

	private void handlePositioning(IDirectedGraph<Edge<IPrivateNode>, IPrivateNode> participantModelGraph,
			IPrivateNode start) {
		// Calculate Positioning of the nodes
		this.calculatePositionsForNodes(participantModelGraph, start, 0, 0, 0);
		// Resolve Overlaps locally
		this.resolveOverlaps(participantModelGraph, start);
		// Apply Global Shifts to resolve overlaps globally
		this.applyAdditionalShifts(participantModelGraph, start, 0);
	}

	/**
	 * Calculates the position of the nodes in the resulting petri net, so a
	 * balanced structure is created
	 * 
	 * @param participantModel: the graph behind the model to be created
	 * @param node:             The node that is currently looked at
	 * @param posX:             The x coordinate of the node
	 * @param posY:             The y coordinate of the node
	 * @param level:            The depth of the node
	 */
	private void calculatePositionsForNodes(IDirectedGraph<Edge<IPrivateNode>, IPrivateNode> participantModelGraph,
			IPrivateNode node, Integer posX, Integer posY, Integer level) {

		this.positions.put(node.getName(), new Position(posX, posY + level * Y_OFFSET));

		List<IPrivateNode> nodeChildren = participantModelGraph.getDirectSuccessors(node).stream()
				.collect(Collectors.toList());

		Integer posXChild = posX - (nodeChildren.size() - 1) * X_OFFSET / 2;

		for (IPrivateNode childNode : nodeChildren) {
			calculatePositionsForNodes(participantModelGraph, childNode, posXChild, posY, level + 1);
			posXChild += X_OFFSET;
		}
	}

	/**
	 * Resolves overlaps in the petri net structure by looking at each two subtrees
	 * of a node: 1. Calculate the overlap of the subtrees by comparing rightmost
	 * node of left subtree and leftmost node of right subtree 2. If overlap exists
	 * shift the node and all subtrees to "make space" 2.1 Store the shift that
	 * needs to be done for the right child 3. Recursively look in all subtrees if
	 * another overlap is found
	 */
	private void resolveOverlaps(IDirectedGraph<Edge<IPrivateNode>, IPrivateNode> participantModelGraph,
			IPrivateNode node) {

		List<IPrivateNode> nodeChildren = participantModelGraph.getDirectSuccessors(node).stream()
				.collect(Collectors.toList());

		for (int i = 0; i < nodeChildren.size() - 1; i++) {

			Integer rightmostOfLeftSubtree = getRightmost(participantModelGraph, nodeChildren.get(i));
			Integer leftmostOfRightSubtree = getLeftmost(participantModelGraph, nodeChildren.get(i + 1));

			Integer overlap = leftmostOfRightSubtree - rightmostOfLeftSubtree;

			// Also shift when they are perfectly over each other
			if (overlap >= 0) {
				Integer shift = ((overlap % X_OFFSET) + 1) * X_OFFSET;

				this.shifts.put(nodeChildren.get(i + 1).getName(), shift);

				this.shiftSubtree(participantModelGraph, node, shift);
			}
		}
	}

	/**
	 * Moves the x-coordinate of the node and all subtrees by shift to the right
	 */
	private void shiftSubtree(IDirectedGraph<Edge<IPrivateNode>, IPrivateNode> participantModelGraph, IPrivateNode node,
			Integer shift) {

		Position currentPositionOfNode = this.positions.get(node.getName());

		this.positions.put(node.getName(), new Position(currentPositionOfNode.x + shift, currentPositionOfNode.y));

		List<IPrivateNode> nodeChildren = participantModelGraph.getDirectSuccessors(node).stream()
				.collect(Collectors.toList());

		for (IPrivateNode childNode : nodeChildren) {
			shiftSubtree(participantModelGraph, childNode, shift);
		}
	}

	/**
	 * Gets the rightmost tree of the (sub)tree at the given node
	 * 
	 * @param node: The root node of the (sub)tree
	 * 
	 * @return: The x position of the rightmost node
	 */
	private Integer getRightmost(IDirectedGraph<Edge<IPrivateNode>, IPrivateNode> participantModelGraph,
			IPrivateNode node) {

		List<IPrivateNode> nodeChildren = participantModelGraph.getDirectSuccessors(node).stream()
				.collect(Collectors.toList());

		if (nodeChildren.isEmpty()) {
			return this.positions.get(node.getName()).x;
		}
		return getRightmost(participantModelGraph, nodeChildren.getLast());
	}

	/**
	 * Gets the leftmost tree of the (sub)tree at the given node
	 * 
	 * @param node: The root node of the (sub)tree
	 * 
	 * @return: The x position of the leftmost node
	 */
	private Integer getLeftmost(IDirectedGraph<Edge<IPrivateNode>, IPrivateNode> participantModelGraph,
			IPrivateNode node) {

		List<IPrivateNode> nodeChildren = participantModelGraph.getDirectSuccessors(node).stream()
				.collect(Collectors.toList());

		if (nodeChildren.isEmpty()) {
			return this.positions.get(node.getName()).x;
		}
		return getRightmost(participantModelGraph, nodeChildren.getFirst());
	}

	/**
	 * 
	 * */
	private void applyAdditionalShifts(IDirectedGraph<Edge<IPrivateNode>, IPrivateNode> participantModelGraph,
			IPrivateNode node, Integer acc) {

		Position currentPositionOfNode = this.positions.get(node.getName());

		this.positions.put(node.getName(), new Position(currentPositionOfNode.x + acc, currentPositionOfNode.y));

		List<IPrivateNode> nodeChildren = participantModelGraph.getDirectSuccessors(node).stream()
				.collect(Collectors.toList());

		for (IPrivateNode childNode : nodeChildren) {

			Integer addShiftOfNode = 0;

			if (this.shifts.containsKey(node.getName())) {
				addShiftOfNode = this.shifts.get(node.getName());
			}
			applyAdditionalShifts(participantModelGraph, childNode, acc + addShiftOfNode);

		}
	}

	/**
	 * Prints the CPN.xml file to the output folder
	 */
	public void printXML() throws IOException {

		Document doc = new Document();
		Element pnml = new Element("pnml");

		pnml.setContent(net);
		doc.setRootElement(pnml);

		XMLOutputter xmlOutput = new XMLOutputter();

		// Pretty Print
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(doc, new FileWriter(outputFolder + "/CPN.pnml"));
	}

	private void createInteraction(String id, Integer posX, Integer posY) {

		String idIn = id + "_in", idOut = id + "_out";

		createPlace(idIn, posX, posY - 50);
		createTransition(id, posX, posY);
		createPlace(idOut, posX, posY + 50);

		createArc(idIn, id);
		createArc(id, idOut);
	}

	private void createAnd(String id, Integer posX, Integer posY) {
		createTransition(id, posX, posY);
	}

	private void createXor(IPrivateNode node, boolean isMergeNode) {
		Position pos = this.positions.get(node.getName());

		createTransition(node.getName() + "_xor_in", pos.x, pos.y - 100);

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
	private void createArc(String sourceId, String targetId) {
		Element arcElem = new Element("arc");

		String id = sourceId + "_to_" + targetId;

		arcElem.setAttribute("id", id);

		arcElem.setAttribute("source", sourceId);
		arcElem.setAttribute("target", targetId);

		net.addContent(arcElem);
	}

	/**
	 * Creates a transition element with automatic dimensions (normal transition)
	 * 
	 * @param id:   the id of the element
	 * @param posX: the x-coordinate of the element
	 * @param posY: the y-coordinate of the element
	 * 
	 * @return a <transition>-tag
	 */
	private void createTransition(String id, Integer posX, Integer posY) {
		Element transitionElem = new Element("transition");

		transitionElem.setAttribute("id", id);
//		transitionElem.addContent(getGraphicsElement(PNMLElementEnum.TRANSITION, posX, posY));

		net.addContent(transitionElem);
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
	private void createTransition(String id, Integer posX, Integer posY, Integer dimX, Integer dimY) {
		Element transitionElem = new Element("transition");

		transitionElem.setAttribute("id", id);
//		transitionElem.addContent(getGraphicsElement(posX, posY, dimX, dimY));

		net.addContent(transitionElem);
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

	private void createPlace(String id, Integer posX, Integer posY) {
		Element placeElem = new Element("place");

		placeElem.setAttribute("id", id);

//		placeElem.addContent(getGraphicsElement(PNMLElementEnum.PLACE, posX, posY));
		placeElem.addContent(getNameElement(id));

		net.addContent(placeElem);
	}

	/**
	 * Gets a graphics-tag containing the dimension and position information for a
	 * place or transition
	 * 
	 * @param type: the type of the element for the graphics tag
	 * @param posX: x-coordinate of the Position
	 * @param posy: Y-coordinate of the Position
	 * 
	 * @return a <graphics>-element
	 */
//	private Element getGraphicsElement(PNMLElementEnum type, Integer posX, Integer posY) {
//
//		if (PNMLElementEnum.PLACE.equals(type)) {
//			return getGraphicsElement(posX, posY, DIMENSION_PLACE, DIMENSION_PLACE);
//		} else if (PNMLElementEnum.TRANSITION.equals(type)) {
//			return getGraphicsElement(posX, posY, DIMENSION_TRANSITION, DIMENSION_TRANSITION);
//		} else {
//			return getGraphicsElement(posX, posY, -1, -1);
//		}
//
//	}

	/**
	 * Gets a graphics-tag containing the dimension and position information for a
	 * place or transition
	 * 
	 * @param posX: x-coordinate of the Position
	 * @param posY: Y-coordinate of the Position
	 * @param dimX: width of the element
	 * @param dimY: height of the element
	 * 
	 * @return a <graphics>-element
	 */
//	private Element getGraphicsElement(Integer posX, Integer posY, Integer dimX, Integer dimY) {
//
//		Element graphicsElem = new Element("graphics");
//
//		Element posElem = getPositionElement(posX, posY);
//		Element dimElem = getDimensionElement(dimX, dimY);
//
//		graphicsElem.addContent(posElem);
//		graphicsElem.addContent(dimElem);
//
//		return graphicsElem;
//	}

	/**
	 * Gets a position-tag
	 * 
	 * @param x: The x coordinate
	 * @param y: The y coordinate
	 * 
	 * @return: a <position>-Element
	 */
	private Element getPositionElement(Integer x, Integer y) {
		Element posElem = new Element("position");
		posElem.setAttribute("x", x.toString());
		posElem.setAttribute("y", y.toString());

		return posElem;
	}

	/**
	 * Gets a dimension-element based on the given values
	 * 
	 * @param x: The width
	 * @param y: The height
	 * 
	 * @return a <dimension>-element with the respective height
	 */
	private Element getDimensionElement(Integer x, Integer y) {

		if (x < 0 || y < 0) {
			throw new IllegalStateException(
					"The creation of the dimension element failed, because neither input dimensions are positive, nor was a valid element-type given from which it could have been deducted");
		}

		Element dimElem = new Element("dimension");
		dimElem.setAttribute("x", x.toString());
		dimElem.setAttribute("y", y.toString());

		return dimElem;

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

	/**
	 * Tracks the position the nodes need to have in the resulting Petri model
	 */
	class Position {

		Integer x;
		Integer y;

		Position(Integer x, Integer y) {
			this.x = x;
			this.y = y;
		}

		public String toString() {
			return "(" + x + "," + y + ")";
		}
	}
}
