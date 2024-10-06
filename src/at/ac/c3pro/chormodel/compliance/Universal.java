package at.ac.c3pro.chormodel.compliance;

import java.util.ArrayList;

import at.ac.c3pro.chormodel.generation.Branch;
import at.ac.c3pro.chormodel.generation.ChorModelGenerator.NodeType;
import at.ac.c3pro.chormodel.generation.Split;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.Interaction;

public class Universal extends OccurrencePattern {
	private ArrayList<Interaction> possibleAssignments = new ArrayList<Interaction>();

	public Universal(String label, Interaction p) {
		super(label, p);
		this.type = PatternType.OCCURRENCE;
	}

	/*
	 * possible P assignments: - any interaction not inside a XOR-Split
	 */
	@Override
	public void findPossibleAssignments() {
		for (Split split : splitTracking.getSplits()) {
			if (split.getNodeType() == NodeType.XOR || splitTracking.insideXorSplit(split))
				continue;
			for (Branch branch : split.getBranches()) {
				for (IChoreographyNode node : branch.getNodes()) {
					if (node instanceof Interaction) {
						possibleAssignments.add((Interaction) node);
					}
				}
			}
		}
	}

	@Override
	public void clearAssignments() {
		possibleAssignments.clear();
	}

	@Override
	public void printAssignments() {
		System.out.println(possibleAssignments);
	}

	public ArrayList<Interaction> getPossibleAssignments() {
		return possibleAssignments;
	}

}
