package at.ac.c3pro.chormodel.generation;

//import java.security.acl.LastOwnerException;
import java.util.ArrayList;
import java.util.UUID;

import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.chormodel.generation.ChorModelGenerator.NodeType;
import at.ac.c3pro.node.AndGateway;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.Interaction;
import at.ac.c3pro.node.Message;
import at.ac.c3pro.node.XorGateway;

public class Branch {

	public enum BranchState {
		OPEN, CLOSED, SPLIT
	}

	private Split split = null;
	private int number = 0;
	private BranchState state = BranchState.OPEN;
	private ArrayList<IChoreographyNode> nodes = new ArrayList<IChoreographyNode>();
	private SplitTracking splitTracking = null;
	private Role lastReceiver = null;
	private Boolean isClosable = false;
//	private List<Role> possibleReceivers = new ArrayList<Role>();
//	private List<Role> possibleSenders = new ArrayList<Role>();

	public Branch(int number) {
		this.number = number;
		this.splitTracking = SplitTracking.getInstance();
	}

	public Branch(int number, Split split) {
		this.number = number;
		this.split = split;
		this.splitTracking = SplitTracking.getInstance();
	}

	public IChoreographyNode getLastNode() {
		IChoreographyNode lastNode = null;
		if (!this.nodes.isEmpty()) {
			lastNode = this.nodes.get(this.nodes.size() - 1);
		} else {
			lastNode = this.getSplit().getSpiltNode();
		}

		return lastNode;
	}

	/*
	 * Checks if branch is closable. Therefore: - branch must not be empty - last
	 * node must be interaction or merge - and if parallel branch is already closed,
	 * last receiver must be the same
	 */
	public boolean isClosable() {
		IChoreographyNode lastNode = this.getLastNode();
//		Role sLastReceiver = null;

		if (this.split.getNodeType() != NodeType.XOR) {
			if (this.split.getNodeType() == NodeType.START || this.nodes.isEmpty()) {
				return false;
			} else if ((lastNode instanceof AndGateway || lastNode instanceof XorGateway)
					&& splitTracking.getSplit(lastNode) == null) { // lastNode is Merge
				return true;
			} else if (lastNode instanceof Interaction) { // lastNode is Interaction
				return true;
			}
		} else {
			if ((lastNode instanceof AndGateway || lastNode instanceof XorGateway)
					&& splitTracking.getSplit(lastNode) == null) { // lastNode is Merge
				return true;
			} else if (lastNode instanceof Interaction) { // lastNode is Interaction
				return true;
			} else {
				int numberBranches = this.split.getNumberBranches();
				int numberOfClosableBranches = 0;
				for (Branch branch : split.getBranches()) {
					if (!branch.getNodes().isEmpty()) {
						numberOfClosableBranches++;
					}
				}
				// If all other branches are closable then this branch is also closable even if
				// empty
				if (numberOfClosableBranches == numberBranches - 1) {
					return true;
				}
			}

		}

//		if (this.split.getNodeType() == NodeType.START || this.nodes.isEmpty()) {
//			return false;
//		} else if (((lastNode instanceof XorGateway || lastNode instanceof AndGateway) && splitTracking.getSplit(lastNode) == null) || lastNode instanceof Interaction) { // lastNode is Merge
//			sLastReceiver = this.split.getLastReceiver();
//			if (sLastReceiver == null) {
//				return true;
//			} else if (sLastReceiver.equals(this.lastReceiver)) {
//				return true;
//			}
//		}
		return false;
	}

	/*
	 * Returns true when branch TODO check also if parallel branch is already closed
	 * and if last receivers are the same. if not there has to be one (maybe more??)
	 * res. interactions even if branch is not empty
	 */
	public boolean resInteraction() {
		if (this.isOpen()) {
			if (this.nodes.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	// TODO maybe implement a force close function, that adds needed interaction(s)
	// and then closes branch.
	public void forceClose() {
		Interaction interaction = new Interaction();
		interaction.setMessage(new Message("message"));
		this.addNode(interaction);

		if (split.getMergeNode() != null) {

		}
	}

	/*
	 * Closes this branch. Checking of mergeNode: == null means, that no other
	 * branch of split is closed yet: - create corresponding merge node, add it to
	 * initial (darüberliegendem) branch's nodes and set it's state to open. - set
	 * last receiver of initial branch to last receiver of branch - set last
	 * receiver of split to last receiver of this branch - set branch to closed !=
	 * null means, another branch of split is already closed: - nothing to do
	 * besides set branch to closed
	 */
	public void close() {
		if (this.split.getNodeType() == NodeType.START)
			System.out.println("WTF");

		if (this.split.getMergeNode() == null) {
			NodeType nodeType = this.split.getNodeType();
			Branch initialBranch = this.splitTracking.getBranchByNode(this.split.getSpiltNode());

			// set last receiver on split and initial branch
//			this.split.setLastReceiver(this.lastReceiver);
//			initialBranch.setLastReceiver(this.lastReceiver);

			switch (nodeType) {
			case XOR:
				XorGateway xorMerge = new XorGateway(this.split.getSpiltNode().getName() + "_m",
						UUID.randomUUID().toString());
				this.split.setMergeNode(xorMerge);
				initialBranch.addNode(xorMerge);
				initialBranch.setState(BranchState.OPEN);
				break;
			case AND:
				AndGateway andMerge = new AndGateway(this.split.getSpiltNode().getName() + "_m",
						UUID.randomUUID().toString());
				this.split.setMergeNode(andMerge);
				initialBranch.addNode(andMerge);
				initialBranch.setState(BranchState.OPEN);
				break;
			case START:
				// no closing needed
			case MERGE:
				// we will see
			default:
				// asdasd
			}
		}
		this.state = BranchState.CLOSED;
	}

	public IChoreographyNode getNextNode(IChoreographyNode node) {
		int pos = nodes.indexOf(node);
		if (pos + 1 < nodes.size())
			return nodes.get(pos + 1);
		return null;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public ArrayList<IChoreographyNode> getNodes() {
		return nodes;
	}

	public void setNodes(ArrayList<IChoreographyNode> nodes) {
		this.nodes = nodes;
	}

	public boolean isOpen() {
		if (state == BranchState.OPEN)
			return true;
		return false;
	}

	public boolean isClosed() {
		if (state == BranchState.CLOSED)
			return true;
		return false;
	}

	public void setClosability(Boolean isCloseable) {
		this.isClosable = isCloseable;
	}

	public void setState(BranchState state) {
		this.state = state;
	}

	public BranchState getState() {
		return this.state;
	}

	public Split getSplit() {
		return split;
	}

	public void setSplit(Split split) {
		this.split = split;
	}

	public void addNode(IChoreographyNode node) {
		this.nodes.add(node);
	}

	public Role getLastReceiver() {
		return lastReceiver;
	}

	public void setLastReceiver(Role lastReceiver) {
		this.lastReceiver = lastReceiver;
	}

	public int getInteractionCount() {
		int count = 0;
		for (IChoreographyNode node : nodes) {
			if (node instanceof Interaction)
				count++;
		}
		return count;
	}

	public ArrayList<Interaction> getInteractions() {
		ArrayList<Interaction> interactions = new ArrayList<Interaction>();
		for (IChoreographyNode node : nodes) {
			if (node instanceof Interaction)
				interactions.add((Interaction) node);
		}
		return interactions;
	}

	public boolean subsequentInteraction(IChoreographyNode merge) {
		boolean flag = false;
		for (IChoreographyNode node : this.getNodes()) {
			if (flag && node instanceof Interaction) {
				return true;
			} else if (node == merge) {
				flag = true;
			}
		}
		return false;
	}

}
