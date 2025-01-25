package at.ac.c3pro.chormodel.compliance;

import at.ac.c3pro.chormodel.generation.Branch;
import at.ac.c3pro.chormodel.generation.ChorModelGenerator.NodeType;
import at.ac.c3pro.chormodel.generation.Split;
import at.ac.c3pro.node.AndGateway;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.Interaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CoRequisite extends CompliancePattern {
    private final Interaction q;
    private final HashMap<Interaction, ArrayList<Interaction>> possibleAssignments = new HashMap<Interaction, ArrayList<Interaction>>();

    public CoRequisite(String label, Interaction p, Interaction q) {
        super(label, p);
        this.q = q;
        this.type = PatternType.OCCURRENCE;
    }

    @Override
    public void findPossibleAssignments() {
        for (Split split : splitTracking.getSplits()) {
            if (splitTracking.insideXorSplit(split)) {
                for (Branch branch : split.getBranches()) {
                    for (Interaction interaction : branch.getInteractions()) {
                        ArrayList<Interaction> possibleQs = new ArrayList<Interaction>();
                        possibleQs = this.getPossibleQs(interaction, possibleQs);
                        if (!possibleQs.isEmpty()) {
                            possibleAssignments.put(interaction, possibleQs);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void clearAssignments() {
        possibleAssignments.clear();
    }

    // TODO adjust algo (doublers)
    private ArrayList<Interaction> getPossibleQs(IChoreographyNode p, ArrayList<Interaction> possibleQs) {
        Branch currentBranch = splitTracking.getBranchByNode(p);
        Split split = currentBranch.getSplit();

        if (split.getNodeType() == NodeType.XOR) {
            possibleQs = insideBranch(p, currentBranch, possibleQs);
        } else if (split.getNodeType() == NodeType.AND) {
            while (split.getNodeType() != NodeType.XOR) {
                currentBranch = splitTracking.getBranchByNode(split.getSplitNode());
                split = currentBranch.getSplit();
            }
            possibleQs = insideBranch(p, currentBranch, possibleQs);
        }

        possibleQs = insideBranch(p, currentBranch, possibleQs);

        return possibleQs;
    }

    private ArrayList<Interaction> insideBranch(IChoreographyNode p, Branch branch, ArrayList<Interaction> possibleQs) {
        for (IChoreographyNode node : branch.getNodes()) {
            if (node instanceof Interaction && node != p && !possibleQs.contains(node))
                possibleQs.add((Interaction) node);
            if (node instanceof AndGateway && splitTracking.getSplitMap().containsKey(node)) {
                for (Branch splitBranch : splitTracking.getSplitMap().get(node).getBranches()) {
                    possibleQs = insideBranch(p, splitBranch, possibleQs);
                }
            }
        }
        return possibleQs;
    }

    @Override
    public void printAssignments() {
        for (Map.Entry<Interaction, ArrayList<Interaction>> entry : possibleAssignments.entrySet()) {
            System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
        }
    }

}
