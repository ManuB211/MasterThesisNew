package at.ac.c3pro.chormodel.generation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import org.jbpt.utils.IOUtils;

import at.ac.c3pro.chormodel.MultiDirectedGraph;
import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.chormodel.generation.Branch.BranchState;
import at.ac.c3pro.node.AndGateway;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.Event;
import at.ac.c3pro.node.Gateway;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.Interaction;
import at.ac.c3pro.node.Message;
import at.ac.c3pro.node.XorGateway;

public class ChorModelGenerator {

	public enum NodeType {
		INTERACTION, XOR, AND, LOOP, START, END, MERGE
	}

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	private MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> buildGraph = new MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode>();
	private MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> enrichedGraph = new MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode>();

	private int participantCount;
	private SplitTracking splitTracking = null;

	// options
	private Boolean earlyBranchClosing = false;
	private Boolean startWithInteraction = true;
	private int maxBranching;

	HashMap<NodeType, Integer> initialNodeCounts = new HashMap<NodeType, Integer>();
	HashMap<NodeType, Integer> remainingNodeCounts;

	// lists
	private List<Role> participants = new ArrayList<Role>();
	private ArrayList<Interaction> interactions = new ArrayList<Interaction>();
	private List<XorGateway> xorGateways = new ArrayList<XorGateway>();
	private List<AndGateway> andGateways = new ArrayList<AndGateway>();
	private List<XorGateway> loops = new ArrayList<XorGateway>();

	// Formatted date for uniform naming schemes
	String formattedDate;

	public ChorModelGenerator() {
	}

	public ChorModelGenerator(int participantCount, int interactionCount, int xorSplitCount, int andSpiltCount,
			int loopCount, int maxBranching, String formattedDate) {
		super();
		this.participantCount = participantCount;
		this.initialNodeCounts.put(NodeType.INTERACTION, interactionCount);
		this.initialNodeCounts.put(NodeType.XOR, xorSplitCount);
		this.initialNodeCounts.put(NodeType.AND, andSpiltCount);
		this.initialNodeCounts.put(NodeType.LOOP, loopCount);
		this.maxBranching = maxBranching;
		this.remainingNodeCounts = this.initialNodeCounts;
		this.formattedDate = formattedDate;
		setupParticipants();
	}

	public MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> build(String formattedDate) {

		int remSplits = getRemainingSplits();
		// check parameter
		if (getResInteractionsGateways() > remainingNodeCounts.get(NodeType.INTERACTION))
			return null;

		// Instantiate split tracking
		this.splitTracking = SplitTracking.getInstance();

		// start event
		Event start = new Event("start", UUID.randomUUID().toString());
		IChoreographyNode currentNode = start;
		// pseudo split for main branch node = start and branches = 1
		Split startSplit = new Split(start, NodeType.START, 1);
		this.splitTracking.addSplit(currentNode, startSplit);

		IChoreographyNode nextNode = null;
		NodeType selectedNodeType = null;
		Branch currentBranch = null;
		int loopCounter = 1;

		//@formatter:off
		/*
		 * Build algorithm: 
		 * 	(1) select random non closed branch 
		 * 	(2) close branch possible? 
		 * 		(2.1) true -> choose to close branch by random (maybe some parameterized solution) 
		 * 			(2.1.1) close branch? 
		 * 				(2.1.1.1) true -> close branch (add merge node) -> GOTO (1)
		 *				(2.1.1.2) false -> GOTO step (3) 
		 *		(2.2) false -> GOTO (3) 
		 *	(3) add new random node -> GOTO (1)
		 */
		//@formatter:on
		do {
			System.out.println("--------- LOOP: " + loopCounter + " ------------");

			if (loopCounter < 2) {
				if (startWithInteraction) {
					nextNode = getNextNode(NodeType.INTERACTION);
					selectedNodeType = NodeType.INTERACTION;
				} else {
					selectedNodeType = getRandomPossibleNodeType();
					if (selectedNodeType == null)
						System.out.println("STOP");
					nextNode = getNextNode(selectedNodeType);
				}
			} else {
				selectedNodeType = getRandomPossibleNodeType();
				if (selectedNodeType == null)
					System.out.println("STOP");
				nextNode = getNextNode(selectedNodeType);
			}

			// select random branch and get its last node
			currentBranch = getRandomBranch();
			currentNode = currentBranch.getLastNode();
			if (currentNode == null) {
				currentNode = currentBranch.getSplit().getSpiltNode();
			}

			if (currentBranch.isClosable()) {
				if (new Random().nextBoolean()) {
					currentBranch.close();
					this.buildGraph.addEdge(currentNode, currentBranch.getSplit().getMergeNode());
					// close all possible branches of split
					if (earlyBranchClosing) {
						for (Branch branch : currentBranch.getSplit().getBranches()) {
							if (branch.isOpen()) {
								if (branch.isClosable()) {
									branch.close();
									currentNode = branch.getLastNode();
									this.buildGraph.addEdge(currentNode, branch.getSplit().getMergeNode());
								}
							}
						}
					}
					// currentGraphToDot(loopCounter);
					loopCounter++;
					continue;
				}
			}

			this.buildGraph.addEdge(currentNode, nextNode);
			currentBranch.addNode(nextNode);
			printCurrentBranching();

			// currentGraphToDot(loopCounter);

			if (nextNode instanceof XorGateway) {
				currentBranch.setState(BranchState.SPLIT);
				printCurrentBranching();
				Split xorSplit = new Split(nextNode, NodeType.XOR, getRandomPossibleBranchCount(NodeType.XOR));
				remainingNodeCounts.put(NodeType.XOR, remainingNodeCounts.get(NodeType.XOR) - 1);
				splitTracking.addSplit(nextNode, xorSplit);
			} else if (nextNode instanceof AndGateway) {
				currentBranch.setState(BranchState.SPLIT);
				printCurrentBranching();
				Split andSplit = new Split(nextNode, NodeType.AND, getRandomPossibleBranchCount(NodeType.AND));
				remainingNodeCounts.put(NodeType.AND, remainingNodeCounts.get(NodeType.AND) - 1);
				splitTracking.addSplit(nextNode, andSplit);
			} else if (nextNode instanceof Interaction) {
				remainingNodeCounts.put(NodeType.INTERACTION, remainingNodeCounts.get(NodeType.INTERACTION) - 1);
				interactions.add((Interaction) nextNode);

			} else {
				throw new IllegalStateException("Node given, that is not any of the allowed types");
			}

			currentNode = nextNode;

			loopCounter++;

		} while (remainingNodeCounts.get(NodeType.INTERACTION) > 0);

		// close down graph
		closeDownBuildGraph();

		IChoreographyNode lastGraphNode = startSplit.getBranches().get(0).getLastNode();
		Event end = new Event("end", UUID.randomUUID().toString());
		startSplit.setMergeNode(end);

		this.buildGraph.addEdge(lastGraphNode, end);

		// currentGraphToDot(loopCounter++);

		HashMap<IChoreographyNode, IChoreographyNode> loops = getRandomLoops();
		int count = 0;
		// Insert loops in graph
		// remove edges - insert xor gateways - insert new edges
		for (Map.Entry<IChoreographyNode, IChoreographyNode> entry : loops.entrySet()) {
			System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());

			// loop merge
			IChoreographyNode preNodeMerge = this.buildGraph.getFirstDirectPredecessor(entry.getKey());
			Edge<IChoreographyNode> mergeEdge = this.buildGraph.getEdge(preNodeMerge, entry.getKey());
			this.buildGraph.removeEdge(mergeEdge);

			XorGateway loopMerge = new XorGateway();
			loopMerge.setName("LOOP" + count + "_m");
			loopMerge.setId(UUID.randomUUID().toString());

			this.buildGraph.addEdge(preNodeMerge, loopMerge);
			this.buildGraph.addEdge(loopMerge, entry.getKey());

			// loop split
			IChoreographyNode sucNodeSplit = this.buildGraph.getFirstDirectSuccessor(entry.getValue());
			Edge<IChoreographyNode> splitEdge = this.buildGraph.getEdge(entry.getValue(), sucNodeSplit);
			this.buildGraph.removeEdge(splitEdge);

			XorGateway loopSplit = new XorGateway();
			loopSplit.setName("LOOP" + count);
			loopSplit.setId(UUID.randomUUID().toString());

			this.buildGraph.addEdge(entry.getValue(), loopSplit);
			this.buildGraph.addEdge(loopSplit, sucNodeSplit);
			this.buildGraph.addEdge(loopSplit, loopMerge);

			count++;
		}

		currentGraphToDot(loopCounter++);
		addMessageFlow();
		buildFinishedGraph();
		IOUtils.toFile(formattedDate + "/enriched_graph" + ".dot", enrichedGraph.toDOT());
		insertLoops(loops);

		return this.buildGraph;
	}

	/*
	 * Close down whole graph recursively (depth first)
	 */
	private void closeSplit(Split split) {
		for (Branch branch : split.getBranches()) {
			System.out.println(branch.getNodes());
			int listSize = branch.getNodes().size();
			for (int i = 0; i < listSize; i++) {
				IChoreographyNode node = branch.getNodes().get(i);
				System.out.println(node.getName());
				if ((node instanceof XorGateway || node instanceof AndGateway)
						&& splitTracking.getSplit(node) != null) { // node is split
					closeSplit(splitTracking.getSplit(node));
				}
			}
			if (branch.isOpen()) {
				if (!branch.isClosable()) {
					System.out.println("BRANCH NOT CLOSABLE: " + branch.getSplit().getSpiltNode() + " Nodes: "
							+ branch.getNodes());
					Interaction interaction = new Interaction();
					interaction.setName(String.valueOf("IA" + interactions.size()));
					interaction.setId(UUID.randomUUID().toString());
					interactions.add(interaction);
					branch.addNode(interaction);
				} else {

				}
				branch.close();
				IChoreographyNode lastNode = branch.getLastNode();
				IChoreographyNode mergeNode = split.getMergeNode();
				System.out.println("LastNode: " + lastNode + " MergeNode: " + mergeNode);
				this.buildGraph.addEdge(branch.getLastNode(), split.getMergeNode());
			}
		}
	}

	private void closeDownBuildGraph() {
		Split startSplit = splitTracking.getMainBranch().getSplit();
		closeSplit(startSplit);
	}

	/*
	 * Choose random possible NodeType, based on current build and remaining nodes.
	 */
	private NodeType getRandomPossibleNodeType() { // branch reserved interactions
		System.out.println("--- Random NodeType Selection");
		NodeType selectedNodeType = null;
		List<NodeType> possibleNodeTypes = new ArrayList<NodeType>();

		int freeInteractions = determineFreeInteractions();

		printCurrentInteractionState();

		if (freeInteractions > 0 || this.getResInteractionsGateways() == 0)
			possibleNodeTypes.add(NodeType.INTERACTION);
		if (remainingNodeCounts.get(NodeType.XOR) > 0)
			possibleNodeTypes.add(NodeType.XOR);
		if (remainingNodeCounts.get(NodeType.AND) > 0)
			possibleNodeTypes.add(NodeType.AND);

		try {
			int index = ThreadLocalRandom.current().nextInt(possibleNodeTypes.size());
			selectedNodeType = possibleNodeTypes.get(index);
		} catch (Exception e) {
			System.out.println(e);
		}
		System.out.println("Selected NodeType: " + selectedNodeType.toString());
		return selectedNodeType;
	}

	/**
	 * Creates a new node of the given node type and returns it
	 * 
	 * @param nodeType: The node type that the created node will have
	 * 
	 * @return the created node
	 */
	private IChoreographyNode getNextNode(NodeType nodeType) {
		IChoreographyNode node = null;
		switch (nodeType) {
		case INTERACTION:
			node = new Interaction();
			node.setName(String.valueOf("IA" + interactions.size()));
			node.setId(UUID.randomUUID().toString());
			break;
		case XOR:
			node = new XorGateway();
			node.setName(String.valueOf("XOR" + xorGateways.size()));
			node.setId(UUID.randomUUID().toString());
			xorGateways.add((XorGateway) node);
			break;
		case AND:
			node = new AndGateway();
			node.setName(String.valueOf("AND" + andGateways.size()));
			node.setId(UUID.randomUUID().toString());

			andGateways.add((AndGateway) node);
			break;
		case LOOP:
			node = new XorGateway();
			node.setName(String.valueOf("LOOP" + loops.size()));
			node.setId(UUID.randomUUID().toString());
			loops.add((XorGateway) node);
			break;
		}
		return node;
	}

	private Branch getRandomBranch() { // considers number of remaining interactions / branches without interactions
		// System.out.println("» Random branch selection: [ " + nodeType + " ]");
		Branch ranBranch = null;
		List<Branch> possibleBranches = new ArrayList<Branch>();

		if (this.determineFreeInteractions() > 0) { // all open branches
			for (Split split : splitTracking.getSplits()) {
				for (Branch branch : split.getBranches()) {
					if (branch.isOpen()) {
						possibleBranches.add(branch);
					}
				}
			}
		} else { // only not closable branches
			for (Split split : splitTracking.getSplits()) {
				for (Branch branch : split.getBranches()) {
					if (!branch.isClosable() && branch.getState() != BranchState.SPLIT) {
						possibleBranches.add(branch);
					}
				}
			}
		}

		int index = ThreadLocalRandom.current().nextInt(possibleBranches.size());
		ranBranch = possibleBranches.get(index);

		return ranBranch;
	}

	private void setupParticipants() {
		for (int i = 0; i < participantCount; i++) {
			Role role = new Role("P_" + i, UUID.randomUUID().toString());
			participants.add(role);
		}
	}

	/*
	 * Determines the number of already reserved interactions, considering remaining
	 * splits (not yet in build) and not yet closed splits (splitTracking), that
	 * have branches without interactions. The minimum of reserved interactions for
	 * remaining splits is calculated by "remainingSplitNodes(XOR/AND) + 1" which is
	 * the number of interactions needed, if all remaining splits are nested inside
	 * one branch and max branching is 2.
	 */
	private int determineResInteractions() {
		int resInteractionsGateways = 0;
		int resInteractionsBuild = 0;

		int remAndGateways = this.getRemainingAND();
		int remXorGateways = this.getRemainingXOR();

		if (remAndGateways > 0) {
			resInteractionsGateways = remAndGateways + 1;
		} else if (remXorGateways > 0) {
			resInteractionsGateways = 1;
		}

		resInteractionsBuild = splitTracking.getResInteractions();

		return resInteractionsGateways + resInteractionsBuild;
	}

	private int getResInteractionsGateways() {
		int resInteractionsGateways = 0;
		int remAndGateways = this.getRemainingAND();
		int remXorGateways = this.getRemainingXOR();

		if (remAndGateways > 0) {
			resInteractionsGateways = remAndGateways + 1;
		} else if (remXorGateways > 0) {
			resInteractionsGateways = 1;
		}

		return resInteractionsGateways;
	}

	private int determineResInteractionsBuild() {
		return this.splitTracking.getResInteractions();
	}

	/*
	 * Returns random branch count out of range. Considering res. interactions -
	 * min: 2 - max: x (parameter)
	 */
	private int getRandomPossibleBranchCount(NodeType nodeType) {
		int min = 2;
		int freeInteractions = determineFreeInteractions();
		int currentMax = 0;
		System.out.println("--- Random Possible Branching");
		printCurrentInteractionState();

		if (freeInteractions >= 0) {
			if (nodeType == NodeType.XOR) {
				currentMax = min + freeInteractions; // +1 because XOR can have one empty branch
			} else {
				currentMax = min + freeInteractions;
			}
			if (currentMax > this.maxBranching)
				currentMax = this.maxBranching;
		} else {
			currentMax = min;
		}

		int branchCount = new Random().nextInt((currentMax - min) + 1) + min;

		if (branchCount < 2)
			LOGGER.warning("branchCount < 2");
		return branchCount;
	}

	private int getRemainingSplits() {
		return remainingNodeCounts.get(NodeType.XOR) + remainingNodeCounts.get(NodeType.AND);
	}

	private int getRemainingXOR() {
		return remainingNodeCounts.get(NodeType.XOR);
	}

	private int getRemainingAND() {
		return remainingNodeCounts.get(NodeType.AND);
	}

	private int determineFreeInteractions() {
		return remainingNodeCounts.get(NodeType.INTERACTION) - determineResInteractions();
	}

	private Role getRandomReceiver(Role sender) {
		Role receiver;
		int randomIndex;
		do {
			randomIndex = new Random().nextInt(participants.size());
			receiver = participants.get(randomIndex);
		} while (sender.equals(receiver));

		return receiver;
	}

	private Role getRandomParticipant() {
		int randomIndex = new Random().nextInt(participants.size());
		return participants.get(randomIndex);
	}

	private HashMap<IChoreographyNode, IChoreographyNode> getRandomLoops() {
		HashMap<IChoreographyNode, ArrayList<IChoreographyNode>> possibleLoops = new HashMap<IChoreographyNode, ArrayList<IChoreographyNode>>();
		for (Split split : splitTracking.getSplits()) {
			for (Branch branch : split.getBranches()) {
				for (IChoreographyNode node : branch.getNodes()) {
					if (node instanceof Gateway && splitTracking.getSplit(node) == null)
						continue;
					ArrayList<IChoreographyNode> nodes = new ArrayList<IChoreographyNode>();
					int index = branch.getNodes().indexOf(node);
					for (int i = index; i < branch.getNodes().size(); i++) {
						IChoreographyNode currentNode = branch.getNodes().get(i);
						if (currentNode instanceof Interaction) {
							nodes.add(currentNode);
						} else if (currentNode instanceof Gateway) {
							if (splitTracking.getSplit(currentNode) == null) {
								nodes.add(currentNode);
							}
						}
					}
					possibleLoops.put(node, nodes);
				}
			}
		}

		ArrayList<IChoreographyNode> possibleNodes = new ArrayList<IChoreographyNode>();

		for (Map.Entry<IChoreographyNode, ArrayList<IChoreographyNode>> entry : possibleLoops.entrySet()) {
			System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
			possibleNodes.add(entry.getKey());
		}

		HashMap<IChoreographyNode, IChoreographyNode> selectedLoops = new HashMap<IChoreographyNode, IChoreographyNode>();
		while (selectedLoops.size() < initialNodeCounts.get(NodeType.LOOP)) {
			int index = ThreadLocalRandom.current().nextInt(possibleNodes.size());
			IChoreographyNode firstNode = possibleNodes.get(index);

			if (possibleLoops.get(firstNode).size() > 0) {
				int index2 = ThreadLocalRandom.current().nextInt(possibleLoops.get(firstNode).size());
				IChoreographyNode secondNode = possibleLoops.get(firstNode).get(index2);
				possibleLoops.get(firstNode).remove(secondNode);
				selectedLoops.put(firstNode, secondNode);
			}
		}

		for (Map.Entry<IChoreographyNode, IChoreographyNode> entry : selectedLoops.entrySet()) {
			System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
		}

		return selectedLoops;

	}

	private void currentGraphToDot(int loopNo) {
		IOUtils.toFile(formattedDate + "/autogen_" + loopNo + ".dot", buildGraph.toDOT());
	}

	public void setEarlyBranchClosing(Boolean earlyBranchClosing) {
		this.earlyBranchClosing = earlyBranchClosing;
	}

	public void setStartWithInteraction(Boolean startWithInteraction) {
		this.startWithInteraction = startWithInteraction;
	}

	public void setMaxBranching(int maxBranching) {
		this.maxBranching = maxBranching;
	}

	/**
	 * Prints the current interaction state, containing, i.e. remaining interaction
	 * nodes, reserved amounts, free amounts
	 */
	private void printCurrentInteractionState() {
		System.out.println("-------------------------------");
		System.out.println("Printing Current Interaction State");
		System.out.println("-------------------------------");
		System.out.println("Remaining: " + remainingNodeCounts.get(NodeType.INTERACTION));
		System.out.println("ResTotal: " + determineResInteractions() + " ResGateways: " + getResInteractionsGateways()
				+ " ResBuild: " + determineResInteractionsBuild());
		System.out.println("Free: " + determineFreeInteractions());
		System.out.println("-------------------------------");
		System.out.println();
	}

	/**
	 * Prints the current branching, containing the following information for all
	 * splits: Split-Node, Merge-Node, State, Nodes, Reserved Interactions
	 * 
	 */
	private void printCurrentBranching() {
		System.out.println("-------------------------------");
		System.out.println("Printing Current Branches");
		System.out.println("-------------------------------");

		for (Split split : this.splitTracking.getSplits()) {
			System.out.println("SplitNode: " + split.getSpiltNode() + " MergeNode: " + split.getMergeNode());
			for (Branch branch : split.getBranches()) {
				System.out.println("State: " + branch.getState().toString() + " Nodes: " + branch.getNodes()
						+ " Res. IA: " + branch.resInteraction());
			}
		}

		System.out.println("-------------------------------");
		System.out.println();
	}

	/**
	 * Prints the interactions
	 */
	public void printInteractions() {

		System.out.println("-------------------------------");
		System.out.println("Printing Interactions, Size: " + interactions.size());
		System.out.println("-------------------------------");

		for (Interaction ia : interactions) {
			System.out.println(ia.toString());
		}
		System.out.println("-------------------------------");
		System.out.println();
	}

	/**
	 * Adds the message flow to the generatedModel
	 */
	private void addMessageFlow() {
		System.out.println("----------------------------------------------------------------------");
		System.out.println("Start adding message flow");
		System.out.println("----------------------------------------------------------------------");
		Split startSplit = splitTracking.getMainBranch().getSplit();
		addMessageFlow(startSplit);

	}

	/**
	 * Enriches Interactions with Sender & Receiver & Message
	 * 
	 * @param split: The split to use as an entry point of the addition of the
	 *               message flow
	 */
	private void addMessageFlow(Split split) {
		if (split.getSpiltNode() instanceof Event) {
			split.setFirstSender(getRandomParticipant());
		}

		System.out.println("SPLIT: " + split.getSpiltNode());
		System.out.println("Number of Branches: " + split.getNumberBranches());
		System.out.println("-------------------");
		System.out.println("");

		System.out.println("Iterate over the Branches");
		System.out.println("-------------------");
		System.out.println("");
		// Iterate over branches of the split
		for (Branch branch : split.getBranches()) {
			branch.setLastReceiver(split.getFirstSender());

			System.out.println("Branch: " + branch.getNodes());

			if (branch.getNodes().isEmpty()) {
				System.out.println(" Branch state: " + branch.getState());
				System.out.println("Branch closable: " + branch.isClosable());
			}

			System.out.println("-------------------");
			System.out.println("");
			System.out.println("Last Receiver: " + branch.getLastReceiver());
			System.out.println("-------------------");
			System.out.println("");

			System.out.println("Iterate over nodes in the branch");
			System.out.println("-------------------");
			System.out.println("");
			// Iterate over nodes of the branch
			int listsize = branch.getNodes().size();
			for (int i = 0; i < listsize; i++) {

				IChoreographyNode currentNode = branch.getNodes().get(i);
				Role sender = branch.getLastReceiver();
				System.out.println("Node: " + currentNode);
				Role receiver;

				if (currentNode instanceof Interaction) {

					// Current node is the last node of the branch
					if (i + 1 == listsize) {

						// If Split has no last receiver (), first worked branch of split determines it
						// Else another branch ofthe split already determined receiver for the
						// interaction
						if (split.getLastReceiver() == null) {

							receiver = setSenderAndRandomReceiver(((Interaction) currentNode), sender);
							createMessageAndSetToCurrNode(sender, receiver, ((Interaction) currentNode));

							branch.setLastReceiver(receiver);
							split.setLastReceiver(receiver); // set binding receiver for last interaction of all
																// branches
							System.out.println("(Interaction) set last receiver: " + split.getLastReceiver());

						} else {
							receiver = split.getLastReceiver();
							System.out.println("(Interaction) got last receiver: " + split.getLastReceiver());

							// one additional interaction needed -> 1st splitLastReceiver to random Part ;
							// 2nd lastReceiver to splitLastReceiver
							if (sender.equals(receiver)) {
								receiver = setSenderAndRandomReceiver(((Interaction) currentNode), sender);
								createMessageAndSetToCurrNode(sender, receiver, ((Interaction) currentNode));

								branch.setLastReceiver(receiver);
								sender = branch.getLastReceiver();
								receiver = split.getLastReceiver();

								Interaction ia = createInteraction(interactions.size(), sender, receiver);
								createMessageAndSetToCurrNode(sender, receiver, ia);

								interactions.add(ia);
								branch.addNode(ia);
								branch.setLastReceiver(receiver);

							} else {
								((Interaction) currentNode).setSender(sender);
								((Interaction) currentNode).setReceiver(receiver);
								createMessageAndSetToCurrNode(sender, receiver, ((Interaction) currentNode));
							}
						}
					} else {
						receiver = setSenderAndRandomReceiver(((Interaction) currentNode), sender);

						createMessageAndSetToCurrNode(sender, receiver, ((Interaction) currentNode));

						branch.setLastReceiver(receiver);
					}
					System.out.println("Sender: " + ((Interaction) currentNode).getSender());
					System.out.println("Receiver: " + ((Interaction) currentNode).getReceiver());

				} else if (currentNode instanceof Gateway) {

					// If gateway is split -> set first sender of that split as last receiver of
					// corresp. branch
					// Else (gateway is merge) -> set last receiver of that split as last receiver
					// of corresp. branch
					if (splitTracking.isSplit(currentNode)) {

						Split innerSplit = splitTracking.getSplit(currentNode);
						innerSplit.setFirstSender(branch.getLastReceiver());

						// Recursive Call
						addMessageFlow(innerSplit);

					} else {
						Split splitOfMerge = splitTracking.getSplitByMergeNode(currentNode);
						branch.setLastReceiver(splitOfMerge.getLastReceiver());

						System.out.println("(Merge): set last receiver branch " + branch.getLastReceiver());

						// Merge is last node of branch
						if (i + 1 == listsize) {
							if (split.getLastReceiver() == null) {
								split.setLastReceiver(branch.getLastReceiver());

								System.out.println("(Merge) set last receiver: " + split.getLastReceiver());
							} else {
								if (!branch.getLastReceiver().equals(split.getLastReceiver())) {
									sender = branch.getLastReceiver();
									receiver = split.getLastReceiver();

									Interaction ia = createInteraction(interactions.size(), sender, receiver);
									createMessageAndSetToCurrNode(sender, receiver, ia);

									interactions.add(ia);
									branch.addNode(ia);
									branch.setLastReceiver(receiver);
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Creates a new Interaction
	 * 
	 * @param interactionId: The next free id to identify the IA
	 * @param sender:        The sending part of the interaction
	 * @param receiver:      The receiving part of the interaction
	 * 
	 * @returns the newly created interaction
	 */
	private Interaction createInteraction(int interactionId, Role sender, Role receiver) {
		Interaction ia = new Interaction();
		ia.setName(String.valueOf("IA" + interactions.size()));
		ia.setId(UUID.randomUUID().toString());
		ia.setSender(sender);
		ia.setReceiver(receiver);

		return ia;
	}

	/**
	 * Creates a new message and sets it as the message for the current node
	 * 
	 * @param sender:   the sender of the message
	 * @param receiver: the receiver of the message
	 * @param node:     the node
	 */
	private void createMessageAndSetToCurrNode(Role sender, Role receiver, Interaction node) {
		Message message = new Message("Message: " + sender.name + " to " + receiver.name, UUID.randomUUID().toString());
		(node).setMessage(message);
	}

	/**
	 * Sets the (given) sender and the (random receiver)
	 * 
	 * @param node:   The node the sender and receiver are added to
	 * @param sender: The sender that is added to the node
	 * 
	 * @return The randomly selected receiver, that was added to the node
	 */
	private Role setSenderAndRandomReceiver(Interaction node, Role sender) {
		node.setSender(sender);
		Role receiver = getRandomReceiver(sender);
		node.setReceiver(receiver);

		return receiver;
	}

	private void buildFinishedGraph() {
		Split startSplit = splitTracking.getMainBranch().getSplit();
		buildFinishedGraph(startSplit);
	}

	private void buildFinishedGraph(Split split) {
		for (Branch branch : split.getBranches()) {
			IChoreographyNode splitNode = split.getSpiltNode();
			ArrayList<IChoreographyNode> branchNodes = branch.getNodes();
			if (branchNodes.isEmpty()) {
				enrichedGraph.addEdge(splitNode, split.getMergeNode());
			} else {
				enrichedGraph.addEdge(splitNode, branchNodes.get(0)); // add edge splitNode -> first Node branch
			}
			for (int i = 0; i < branchNodes.size(); i++) {
				IChoreographyNode currentNode = branchNodes.get(i);
				IChoreographyNode nextNode;
				if (splitTracking.isSplit(currentNode)) {
					buildFinishedGraph(splitTracking.getSplit(currentNode));
				} else {
					if (i + 1 != branchNodes.size()) {
						nextNode = branchNodes.get(i + 1);
					} else { // get mergeNode
						nextNode = split.getMergeNode();
					}
					enrichedGraph.addEdge(currentNode, nextNode);
				}
			}
		}
	}

	public SplitTracking getSplitTracking() {
		return this.splitTracking;
	}

	public void terminateSplitTracking() {
		this.splitTracking.terminate();
	}

	public ArrayList<Interaction> getInteractions() {
		return this.interactions;
	}

	public MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> getEnrichedGraph() {
		return enrichedGraph;
	}

	public List<Role> getParticipants() {
		return this.participants;
	}

	private void insertLoops(HashMap<IChoreographyNode, IChoreographyNode> loops) {
		int count = 0;
		for (Map.Entry<IChoreographyNode, IChoreographyNode> entry : loops.entrySet()) {
			System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());

			// loop merge
			IChoreographyNode preNodeMerge = this.enrichedGraph.getFirstDirectPredecessor(entry.getKey());
			Edge<IChoreographyNode> mergeEdge = this.enrichedGraph.getEdge(preNodeMerge, entry.getKey());
			this.enrichedGraph.removeEdge(mergeEdge);

			XorGateway loopMerge = new XorGateway();
			loopMerge.setName("LOOP" + count + "_m");
			loopMerge.setId(UUID.randomUUID().toString());

			this.enrichedGraph.addEdge(preNodeMerge, loopMerge);
			this.enrichedGraph.addEdge(loopMerge, entry.getKey());

			// loop split
			IChoreographyNode sucNodeSplit = this.enrichedGraph.getFirstDirectSuccessor(entry.getValue());
			Edge<IChoreographyNode> splitEdge = this.enrichedGraph.getEdge(entry.getValue(), sucNodeSplit);
			this.enrichedGraph.removeEdge(splitEdge);

			XorGateway loopSplit = new XorGateway();
			loopSplit.setName("LOOP" + count);
			loopSplit.setId(UUID.randomUUID().toString());

			this.enrichedGraph.addEdge(entry.getValue(), loopSplit);
			this.enrichedGraph.addEdge(loopSplit, sucNodeSplit);
			this.enrichedGraph.addEdge(loopSplit, loopMerge);

			count++;
		}

	}

}
