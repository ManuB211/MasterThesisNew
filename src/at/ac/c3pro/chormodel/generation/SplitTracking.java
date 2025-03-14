package at.ac.c3pro.chormodel.generation;

import at.ac.c3pro.chormodel.generation.ChorModelGenerator.NodeType;
import at.ac.c3pro.node.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SplitTracking {

    private static SplitTracking instance = null;
    private Map<IChoreographyNode, Split> splitMap = new HashMap<IChoreographyNode, Split>();
    private List<Split> splits = new ArrayList<Split>();

    protected SplitTracking() {

    }

    public static SplitTracking getInstance() {
        if (instance == null) {
            instance = new SplitTracking();
        }
        return instance;
    }

    public void terminate() {
        instance = null;
    }

    /*
     * Determines the minimum amount of interactions reserved by current build. Each
     * branch needs min. one subsequent Interaction, by considering throughout
     * branching (nested gateways).
     */
    public int getResInteractions() {
        int count = 0;
        for (Split split : splits) {
            if (split.getNodeType() != NodeType.LOOP && split.getNodeType() != NodeType.XOR) {
                for (Branch branch : split.getBranches()) {
                    if (branch.isOpen()) {
                        if (branch.getNodes().isEmpty())
                            count++;
                    }
                }
            } else {
                int resSplit = split.getBranches().size();
                for (Branch branch : split.getBranches()) {
                    if (!branch.getNodes().isEmpty()) {
                        resSplit = resSplit - 1;
                    }
                }
                if (resSplit > 0) {
                    count = count + (resSplit - 1);
                }
            }
        }
        return count;
    }

    public ArrayList<Interaction> getInteractions() {
        ArrayList<Interaction> interactions = new ArrayList<Interaction>();
        for (Split split : splits) {
            for (Branch branch : split.getBranches()) {
                for (IChoreographyNode node : branch.getNodes()) {
                    if (node instanceof Interaction)
                        interactions.add((Interaction) node);
                }
            }
        }
        return interactions;
    }

    public int getNumberOfInteractions() {
        int interactions = 0;
        for (Split split : splits) {
            for (Branch branch : split.getBranches()) {
                for (IChoreographyNode node : branch.getNodes()) {
                    if (node instanceof Interaction)
                        interactions++;
                }
            }
        }
        return interactions;
    }

    public Map<IChoreographyNode, Split> getSplitMap() {
        return splitMap;
    }

    public void setSplitMap(Map<IChoreographyNode, Split> splitMap) {
        this.splitMap = splitMap;
    }

    public List<Split> getSplits() {
        return splits;
    }

    public void setSplits(List<Split> splits) {
        this.splits = splits;
    }

    public void addSplit(IChoreographyNode node, Split split) {
        this.splitMap.put(node, split);
        this.splits.add(split);
    }

    public Split getSplit(IChoreographyNode node) {
        return this.splitMap.get(node);
    }

    public Branch getBranchByNode(IChoreographyNode node) {
        for (Map.Entry<IChoreographyNode, Split> entry : splitMap.entrySet()) {
            for (Branch branch : entry.getValue().getBranches()) {
                for (IChoreographyNode bNode : branch.getNodes()) {
                    if (bNode.equals(node))
                        return branch;
                }
            }
        }
        return null;
    }

    // TODO remove
    public IChoreographyNode getSplitNodeBySplit(Split split) {
        for (Map.Entry<IChoreographyNode, Split> entry : splitMap.entrySet()) {
            if (entry.getValue() == split) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Split getSplitByBranchNode(IChoreographyNode node) {
        Branch branch = this.getBranchByNode(node);
        return branch.getSplit();
    }

    public Split getSplitByMergeNode(IChoreographyNode merge) {
        for (Split split : splits) {
            if (split.getMergeNode() == merge)
                return split;
        }
        return null;
    }

    public Split getPrecedingSplit(Split split) {
        IChoreographyNode splitNode = split.getSplitNode();
        Branch branch = this.getBranchByNode(splitNode);
        Split precedingSplit = branch.getSplit();

        return precedingSplit;
    }

    public Branch getMainBranch() {
        return splits.get(0).getBranches().get(0);
    }

    /*
     * Returns first interaction after XOR merge. (also if nested XOR)
     */
    public ArrayList<Interaction> getFirstInteractionsAfterXOR(ArrayList<Interaction> reachInteractions,
                                                               IChoreographyNode splitNode) {
        Split split = splitMap.get(splitNode);
        System.out.println("hereherhere");
        for (Branch branch : split.getBranches()) {
            IChoreographyNode firstBranchNode = branch.getNodes().get(0);
            if (firstBranchNode instanceof XorGateway) {
                reachInteractions = getFirstInteractionsAfterXOR(reachInteractions, firstBranchNode);
            } else if (firstBranchNode instanceof Interaction) {
                reachInteractions.add((Interaction) firstBranchNode);
            }
        }
        return reachInteractions;
    }

    public ArrayList<Interaction> getInteractionsAfterMerge(IChoreographyNode merge,
                                                            ArrayList<Interaction> succeedingInteractions) {
        ArrayList<IChoreographyNode> branchNodes = getBranchByNode(merge).getNodes();
        int pos = branchNodes.indexOf(merge);
        if (pos + 1 != branchNodes.size()) {
            IChoreographyNode succeedingNode = branchNodes.get(pos + 1);
            if (succeedingNode instanceof Interaction) {
                succeedingInteractions.add((Interaction) succeedingNode);
                return succeedingInteractions;
//			} else if (succeedingNode instanceof Gateway && !splitMap.containsKey(succeedingNode)) {
//				succeedingInteractions = getInteractionsAfterMerge(succeedingNode, succeedingInteractions);
            } else if (succeedingNode instanceof AndGateway && splitMap.containsKey(succeedingNode)) {
                succeedingInteractions = getInteractionsAfterAndSplit(succeedingNode, succeedingInteractions);
            }
        } else {
            merge = this.getSplitByBranchNode(merge).getMergeNode();
            succeedingInteractions = getInteractionsAfterMerge(merge, succeedingInteractions);
        }
        return succeedingInteractions;
    }

    public ArrayList<Interaction> getInteractionsAfterAndSplit(IChoreographyNode andSplit,
                                                               ArrayList<Interaction> succeedingInteractions) {
        for (Branch branch : getSplit(andSplit).getBranches()) {
            IChoreographyNode firstNode = branch.getNodes().get(0);
            if (firstNode instanceof Interaction) {
                succeedingInteractions.add((Interaction) firstNode);
            } else if (firstNode instanceof AndGateway) {
                succeedingInteractions = getInteractionsAfterAndSplit(firstNode, succeedingInteractions);
            }
        }
        return succeedingInteractions;
    }

    public boolean insideAndSplit(Split split) {
        System.out.println("--- Inside And Split Check 1 ");
        NodeType splitType = split.getNodeType();
        while (splitType != NodeType.START) {
            System.out.println("--- Inside And Split Check 2 ");
            if (splitType == NodeType.AND) {
                return true;
            } else {
                Split precedingSplit = this.getPrecedingSplit(split);
                split = precedingSplit;
                splitType = precedingSplit.getNodeType();
            }
        }

        return false;
    }

    public boolean insideXorSplit(Split split) {
        System.out.println("--- Inside XOR Split Check 1 ");
        NodeType splitType = split.getNodeType();
        while (splitType != NodeType.START) {
            System.out.println("--- Inside XOR Split Check 2 ");
            if (splitType == NodeType.XOR) {
                return true;
            } else {
                Split precedingSplit = this.getPrecedingSplit(split);
                split = precedingSplit;
                splitType = precedingSplit.getNodeType();
            }
        }
        return false;
    }

    public boolean isSplit(IChoreographyNode node) {
        return splitMap.containsKey(node);
    }

//	public int getBranchInteractionCount(Branch branch) {
//		int count = 0;
//		for (IChoreographyNode node : branch.getNodes()) {
//			if (node instanceof Interaction)
//				count++;
//			else if (node instanceof Gateway && isSplit(node)) {
//				for (Branch innerBranch : getSplit(node).getBranches()) {
//					count += getBranchInteractionCount(innerBranch);
//				}
//			}
//		}
//		return count;
//	}

    public HashMap<Interaction, ArrayList<Interaction>> getInteractionHierarchy() {
        HashMap<Interaction, ArrayList<Interaction>> interactionHierarchy = new HashMap<Interaction, ArrayList<Interaction>>();
        for (Split split : this.getSplits()) {
            for (Branch branch : split.getBranches()) {
                for (Interaction interaction : branch.getInteractions()) {
                    ArrayList<Interaction> succeedingIAs = new ArrayList<Interaction>();
                    IChoreographyNode nextNode = branch.getNextNode(interaction);
                    succeedingIAs = this.getSucceedingIAs(nextNode, branch, succeedingIAs);
                    interactionHierarchy.put(interaction, succeedingIAs);
                }
            }
        }
        return interactionHierarchy;
    }

    private ArrayList<Interaction> getSucceedingIAs(IChoreographyNode node, Branch currentBranch,
                                                    ArrayList<Interaction> succeedingIAs) {
        if (node != null)
            succeedingIAs = getSucceedingIAsInnerBranch(node, currentBranch, succeedingIAs);
        succeedingIAs = getSucceedingIAsOuterBranch(node, currentBranch, succeedingIAs);
        return succeedingIAs;
    }

    private ArrayList<Interaction> getSucceedingIAsInnerBranch(IChoreographyNode node, Branch currentBranch,
                                                               ArrayList<Interaction> succeedingIAs) {
        ArrayList<IChoreographyNode> branchNodes = currentBranch.getNodes();
        int pos = branchNodes.indexOf(node);
        int size = branchNodes.size();
        for (int i = pos; i < size; i++) {
            IChoreographyNode branchNode = branchNodes.get(i);
            if (branchNode instanceof Interaction) {
                succeedingIAs.add((Interaction) branchNode);
            } else if (branchNode instanceof AndGateway && this.getSplitMap().containsKey(branchNode)) {
                for (Branch innerBranch : this.getSplitMap().get(branchNode).getBranches()) {
                    if (!innerBranch.getNodes().isEmpty()) {
                        IChoreographyNode firstNode = innerBranch.getNodes().get(0);
                        succeedingIAs = getSucceedingIAsInnerBranch(firstNode, innerBranch, succeedingIAs);
                    }
                }
            }
        }
        return succeedingIAs;
    }

    private ArrayList<Interaction> getSucceedingIAsOuterBranch(IChoreographyNode node, Branch currentBranch,
                                                               ArrayList<Interaction> succeedingIAs) {
        IChoreographyNode merge = currentBranch.getSplit().getMergeNode(); // merge node of branch
        while (!(merge instanceof Event)) {
            currentBranch = this.getBranchByNode(merge);
            ArrayList<IChoreographyNode> branchNodes = currentBranch.getNodes();
            int pos = branchNodes.indexOf(merge);
            int size = branchNodes.size();

            for (int i = pos; i < size; i++) {
                IChoreographyNode branchNode = branchNodes.get(i);
                if (branchNode instanceof Interaction) {
                    succeedingIAs.add((Interaction) branchNode);
                } else if (branchNode instanceof AndGateway && this.getSplitMap().containsKey(branchNode)) {
                    for (Branch innerBranch : this.getSplitMap().get(branchNode).getBranches()) {
                        if (!innerBranch.getNodes().isEmpty()) {
                            IChoreographyNode firstNode = innerBranch.getNodes().get(0);
                            succeedingIAs = getSucceedingIAsInnerBranch(firstNode, innerBranch, succeedingIAs);
                        }
                    }
                }
            }
            merge = currentBranch.getSplit().getMergeNode();
        }
        return succeedingIAs;
    }

}
