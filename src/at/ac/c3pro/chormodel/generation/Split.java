package at.ac.c3pro.chormodel.generation;

import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.chormodel.generation.ChorModelGenerator.NodeType;
import at.ac.c3pro.node.IChoreographyNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Split {

    private IChoreographyNode spiltNode = null;
    private IChoreographyNode mergeNode = null;
    private Role firstSender = null;
    private Role lastReceiver = null;
    private NodeType nodeType = null;
    private int numberBranches = 0;
    private ArrayList<Branch> branches = new ArrayList<Branch>();
    private Boolean closed = false;
    private SplitTracking splitTracking = null;

    public Split(IChoreographyNode spiltNode, NodeType nodeType) {
        super();
        this.spiltNode = spiltNode;
        this.nodeType = nodeType;
        this.splitTracking = SplitTracking.getInstance();
    }

    public Split(IChoreographyNode spiltNode, NodeType nodeType, int numberBranches) {
        super();
        this.spiltNode = spiltNode;
        this.nodeType = nodeType;
        this.numberBranches = numberBranches;
        this.splitTracking = SplitTracking.getInstance();
        this.addBranches(numberBranches);
    }

    private void addBranches(int numberBranches) {
        for (int i = 1; i < numberBranches + 1; i++) {
            branches.add(new Branch(this));
        }
    }

    public boolean isCloseable() {
        for (Branch branch : branches) {
            if (!branch.isClosed())
                return false;
        }
        return true;
    }

    public void close() {
        this.closed = true;
    }

    public ArrayList<Branch> getSortedBranches() {
        HashMap<Branch, Integer> branches = new HashMap<Branch, Integer>();
        for (Branch branch : getBranches()) {
            branches.put(branch, branch.getInteractionCount());
        }
        ArrayList<Branch> sortedBranches = new ArrayList<Branch>();
        Branch lastbranch = null;
        for (int i = 0; i < branches.size(); i++) {
            sortedBranches.add(getNextHighestBranch(branches, lastbranch));
        }

        return sortedBranches;
    }

    private Branch getNextHighestBranch(HashMap<Branch, Integer> branches, Branch lastBranch) {

        Map.Entry<Branch, Integer> maxEntry = null;

        for (Map.Entry<Branch, Integer> entry : branches.entrySet()) {
            if ((maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
                    && !entry.getKey().equals(lastBranch)) {
                maxEntry = entry;
            }
        }

        return maxEntry.getKey();
    }

    public IChoreographyNode getSplitNode() {
        return spiltNode;
    }

    public void setSpiltNode(IChoreographyNode spiltNode) {
        this.spiltNode = spiltNode;
    }

    public int getNumberBranches() {
        return numberBranches;
    }

    public void setNumberBranches(int numberBranches) {
        this.numberBranches = numberBranches;
    }

    public ArrayList<Branch> getBranches() {
        return branches;
    }

    public void setBranches(ArrayList<Branch> branches) {
        this.branches = branches;
    }

    public Boolean isClosed() {
        return closed;
    }

    public void setClosed(Boolean closed) {
        this.closed = closed;
    }

    public IChoreographyNode getMergeNode() {
        return mergeNode;
    }

    public void setMergeNode(IChoreographyNode mergeNode) {
        this.mergeNode = mergeNode;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public SplitTracking getSplitTracking() {
        return splitTracking;
    }

    public void setSplitTracking(SplitTracking splitTracking) {
        this.splitTracking = splitTracking;
    }

    public Role getLastReceiver() {
        return lastReceiver;
    }

    public void setLastReceiver(Role lastReceiver) {
        this.lastReceiver = lastReceiver;
    }

    public Boolean getClosed() {
        return closed;
    }

    public Role getFirstSender() {
        return firstSender;
    }

    public void setFirstSender(Role firstSender) {
        this.firstSender = firstSender;
    }

}
