package at.ac.c3pro.chormodel.compliance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import at.ac.c3pro.chormodel.compliance.CompliancePattern.PatternType;
import at.ac.c3pro.chormodel.generation.Branch;
import at.ac.c3pro.chormodel.generation.ChorModelGenerator.NodeType;
import at.ac.c3pro.chormodel.generation.Split;
import at.ac.c3pro.node.AndGateway;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.Interaction;
import at.ac.c3pro.node.XorGateway;

public class Exclusive extends CompliancePattern {
	private Interaction q;
	private HashMap<Interaction, ArrayList<Interaction>> possibleAssignments = new HashMap<Interaction, ArrayList<Interaction>>();

	public Exclusive(String label, Interaction p, Interaction q) {
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
	
	private ArrayList<Interaction> getPossibleQs(IChoreographyNode node, ArrayList<Interaction> possibleQs) {
		Branch currentBranch = splitTracking.getBranchByNode(node);
		IChoreographyNode splitNode = currentBranch.getSplit().getSpiltNode();
		Split preceedingSplit = currentBranch.getSplit();
		while (splitTracking.insideXorSplit(preceedingSplit)) {
			if (preceedingSplit.getNodeType() == NodeType.XOR) {
				for (Branch branch : currentBranch.getSplit().getBranches()) {
					if (branch != currentBranch) {
						possibleQs = insideBranch(branch, possibleQs);
					}
				}
			}
			
			currentBranch = splitTracking.getBranchByNode(preceedingSplit.getSpiltNode());
			preceedingSplit = currentBranch.getSplit();
		}
		
		return possibleQs;
	}
	
	private ArrayList<Interaction> insideBranch(Branch branch, ArrayList<Interaction> possibleQs) {
		for (IChoreographyNode node : branch.getNodes()) {
			if (node instanceof Interaction)
				possibleQs.add((Interaction) node);
			if ((node instanceof AndGateway || node instanceof XorGateway) && splitTracking.getSplitMap().containsKey(node)) {
				for (Branch splitBranch : splitTracking.getSplitMap().get(node).getBranches()) {
					possibleQs = insideBranch(splitBranch, possibleQs);
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

	public Interaction getQ() {
		return q;
	}
	
	
	
}
