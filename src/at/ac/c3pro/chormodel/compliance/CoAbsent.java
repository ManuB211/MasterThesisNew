package at.ac.c3pro.chormodel.compliance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import at.ac.c3pro.chormodel.generation.Branch;
import at.ac.c3pro.chormodel.generation.Split;
import at.ac.c3pro.node.AndGateway;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.Interaction;
import at.ac.c3pro.node.XorGateway;

public class CoAbsent extends CompliancePattern {
	private Interaction q;
	private HashMap<Interaction, ArrayList<Interaction>> possibleAssignments = new HashMap<Interaction, ArrayList<Interaction>>();

	public CoAbsent(String label, Interaction p, Interaction q) {
		super(label, p);
		this.q = q;
		this.type = PatternType.OCCURRENCE;
	}

	/*
	 * possible P assignments: - any interaction inside a XOR-Split possible Q
	 * assignments: - any interaction which is also not reached if P is not reached
	 */
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

		if (splitNode instanceof XorGateway) {
			possibleQs = insideBranch(node, currentBranch, possibleQs);
		} else if (splitNode instanceof AndGateway) {
			while (!(splitNode instanceof XorGateway)) {
				currentBranch = splitTracking.getBranchByNode(splitNode);
				splitNode = currentBranch.getSplit().getSpiltNode();
			}
			possibleQs = insideBranch(node, currentBranch, possibleQs);
		}
		return possibleQs;
	}

	private ArrayList<Interaction> insideBranch(IChoreographyNode p, Branch branch, ArrayList<Interaction> possibleQs) {
		for (IChoreographyNode node : branch.getNodes()) {
			System.out.println("Print" + node);
			if (node instanceof Interaction)
				if (node != p)
					possibleQs.add((Interaction) node);
			if ((node instanceof AndGateway || node instanceof XorGateway)
					&& splitTracking.getSplitMap().containsKey(node)) {
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
