package at.ac.c3pro.chormodel.compliance;

import java.util.ArrayList;

import javax.swing.DebugGraphics;

import at.ac.c3pro.chormodel.compliance.CompliancePattern.PatternType;
import at.ac.c3pro.chormodel.generation.Branch;
import at.ac.c3pro.chormodel.generation.Split;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.Interaction;

public class Exists extends OccurrencePattern {
	private ArrayList<Interaction> possibleAssignments = new ArrayList<Interaction>();
	
	public Exists(String label, Interaction p) {
		super(label, p);
		this.type = PatternType.OCCURRENCE;
	}
	
	
	/*
	 * possible P assignments:
	 * - any interaction
	 */
	@Override
	public void findPossibleAssignments() {
		for (Split split : splitTracking.getSplits()) {
			for (Branch branch : split.getBranches()) {
				for (IChoreographyNode node : branch.getNodes()) {
					if (node instanceof Interaction)
						possibleAssignments.add((Interaction) node); 
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
