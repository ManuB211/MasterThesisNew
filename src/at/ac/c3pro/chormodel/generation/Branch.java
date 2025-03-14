package at.ac.c3pro.chormodel.generation;

//import java.security.acl.LastOwnerException;

import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.chormodel.generation.ChorModelGenerator.NodeType;
import at.ac.c3pro.node.AndGateway;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.Interaction;
import at.ac.c3pro.node.XorGateway;

import java.util.ArrayList;
import java.util.UUID;

public class Branch {

    public enum BranchState {
        OPEN, CLOSED, SPLIT
    }

    private Split split = null;
    private BranchState state = BranchState.OPEN;
    private ArrayList<IChoreographyNode> nodes = new ArrayList<IChoreographyNode>();
    private SplitTracking splitTracking = null;
    private Role lastReceiver = null;

    public Branch(Split split) {
        this.split = split;
        this.splitTracking = SplitTracking.getInstance();
    }

    public IChoreographyNode getLastNode() {
        IChoreographyNode lastNode = null;
        if (!this.nodes.isEmpty()) {
            lastNode = this.nodes.get(this.nodes.size() - 1);
        } else {
            lastNode = this.getSplit().getSplitNode();
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
            } else // lastNode is Interaction
                if ((lastNode instanceof AndGateway || lastNode instanceof XorGateway)
                        && splitTracking.getSplit(lastNode) == null) { // lastNode is Merge
                    return true;
                } else return lastNode instanceof Interaction;
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
                return numberOfClosableBranches == numberBranches - 1;
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
    }

    /*
     * Returns true when branch TODO check also if parallel branch is already closed
     * and if last receivers are the same. if not there has to be one (maybe more??)
     * res. interactions even if branch is not empty
     */
    public boolean resInteraction() {
        if (this.isOpen()) {
            return this.nodes.isEmpty();
        }
        return false;
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
            Branch initialBranch = this.splitTracking.getBranchByNode(this.split.getSplitNode());

            // set last receiver on split and initial branch
//			this.split.setLastReceiver(this.lastReceiver);
//			initialBranch.setLastReceiver(this.lastReceiver);

            switch (nodeType) {
                case XOR:
                    XorGateway xorMerge = new XorGateway(this.split.getSplitNode().getName() + "_m",
                            UUID.randomUUID().toString());
                    this.split.setMergeNode(xorMerge);
                    initialBranch.addNode(xorMerge);
                    initialBranch.setState(BranchState.OPEN);
                    break;
                case AND:
                    AndGateway andMerge = new AndGateway(this.split.getSplitNode().getName() + "_m",
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

    public ArrayList<IChoreographyNode> getNodes() {
        return nodes;
    }

    public boolean isOpen() {
        return state == BranchState.OPEN;
    }

    public boolean isClosed() {
        return state == BranchState.CLOSED;
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

}
