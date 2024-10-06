package at.ac.c3pro.chormodel.compliance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Formatter;

import at.ac.c3pro.chormodel.compliance.CompliancePattern.PatternType;
import at.ac.c3pro.chormodel.generation.Branch;
import at.ac.c3pro.chormodel.generation.ChorModelGenerator.NodeType;
import at.ac.c3pro.chormodel.generation.Split;
import at.ac.c3pro.node.AndGateway;
import at.ac.c3pro.node.Event;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.Interaction;
import at.ac.c3pro.node.Node;
import at.ac.c3pro.node.XorGateway;

public class CoExists extends CompliancePattern {
	private Interaction q;
	private HashMap<Interaction, ArrayList<Interaction>> possibleAssignments = new HashMap<Interaction, ArrayList<Interaction>>();
	
	public CoExists(String label, Interaction p, Interaction q) {
		super(label, p);
		this.q = q;
		this.type = PatternType.OCCURRENCE;
	}

	/*
	 * possible P assignments:
	 * - any interaction
	 * possible Q assignments:
	 * - any reachable interaction on path before and after P
	 */
	@Override
	public void findPossibleAssignments() {
		for (Split split : splitTracking.getSplits()) {
			for (Branch branch : split.getBranches()) {
				System.out.println("Nodes: " + branch.getNodes());
				System.out.println("IAs: " + branch.getInteractions());
				for (Interaction interaction : branch.getInteractions()) {
					ArrayList<Interaction> possibleQs = new ArrayList<Interaction>();
					System.out.println("Interaction" + interaction);
					possibleQs = this.getPossibleQs(interaction, possibleQs);
					if (!possibleQs.isEmpty()) {
						possibleQs.remove(interaction);
						possibleAssignments.put(interaction, possibleQs);
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
		possibleQs = preceedingPath(node, possibleQs);
		
		return possibleQs;
	}
	

	@Override
	public void printAssignments() {
		for (Map.Entry<Interaction, ArrayList<Interaction>> entry : possibleAssignments.entrySet()) {
			System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
		}
	}
	
	private ArrayList<Interaction> insideBranch(Branch branch, ArrayList<Interaction> possibleQs) {
		for (IChoreographyNode node : branch.getNodes()) {
			System.out.println("Print" + node);
			if (node instanceof Interaction)
				if (!possibleQs.contains(node))
					possibleQs.add((Interaction) node);
			if (node instanceof AndGateway && splitTracking.getSplitMap().containsKey(node)) {
				for (Branch splitBranch : splitTracking.getSplitMap().get(node).getBranches()) {
					possibleQs = insideBranch(splitBranch, possibleQs);
				}
			}
		}
		return possibleQs;
	}
	
	private ArrayList<Interaction> preceedingPath(IChoreographyNode node, ArrayList<Interaction> possibleQs) {
		Branch currentBranch = splitTracking.getBranchByNode(node);
		IChoreographyNode splitNode = currentBranch.getSplit().getSpiltNode();
		while (!(splitNode instanceof Event)) {
			if (splitNode instanceof XorGateway) {
				possibleQs = insideBranch(currentBranch, possibleQs);
			} else if (splitNode instanceof AndGateway) {
				Split split = currentBranch.getSplit();
				for (Branch branch : split.getBranches()) {
					possibleQs = insideBranch(branch, possibleQs);
				}
			}
			
			currentBranch = splitTracking.getBranchByNode(splitNode);
			splitNode = currentBranch.getSplit().getSpiltNode();
			System.out.println("currentBranch: " + currentBranch.getNodes());
			System.out.println("splitNode: " + splitNode);
		}
		
		currentBranch = splitTracking.getMainBranch();
		possibleQs = insideBranch(currentBranch, possibleQs);
		
		return possibleQs;
	}

	public Interaction getQ() {
		return q;
	}
		
	

}
