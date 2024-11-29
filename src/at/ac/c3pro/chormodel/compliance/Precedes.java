package at.ac.c3pro.chormodel.compliance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import at.ac.c3pro.chormodel.generation.Branch;
import at.ac.c3pro.chormodel.generation.ChorModelGenerator.NodeType;
import at.ac.c3pro.chormodel.generation.Split;
import at.ac.c3pro.node.AndGateway;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.Interaction;

public class Precedes extends OrderPattern {

	private HashMap<Interaction, ArrayList<Interaction>> possibleAssignments = new HashMap<Interaction, ArrayList<Interaction>>();
	private HashMap<Interaction, ArrayList<Interaction>> possibleAssignments2 = new HashMap<Interaction, ArrayList<Interaction>>();

	public Precedes(String label, Interaction p, Interaction q) {
		super(label, p, q);
		this.type = PatternType.ORDER;
	}

	/*
	 * possible Q assignments: - any interaction, except first one possible P
	 * assignments: - any interaction on preceding path of Q
	 */
	@Override
	public void findPossibleAssignments() {
		for (Split split : splitTracking.getSplits()) {
			for (Branch branch : split.getBranches()) {
				System.out.println("Nodes: " + branch.getNodes());
				System.out.println("IAs: " + branch.getInteractions());
				for (Interaction interaction : branch.getInteractions()) {
					ArrayList<Interaction> possiblePs = new ArrayList<Interaction>();
					System.out.println("Interaction" + interaction);
					possiblePs = this.getPossibleInteractions(interaction, branch);
					if (!possiblePs.isEmpty()) {
						possibleAssignments.put(interaction, possiblePs);
					}
				}
			}
		}
		ArrayList<Interaction> preceedingIAs = new ArrayList<Interaction>();
		for (Map.Entry<Interaction, ArrayList<Interaction>> entry : possibleAssignments.entrySet()) {
			// System.out.println("Key : " + entry.getKey() + " Value : " +
			// entry.getValue());
			for (Interaction ia : entry.getValue()) {
				preceedingIAs.add(ia);
			}
		}
		for (Interaction preceedingIA : preceedingIAs) {
			possibleAssignments2.put(preceedingIA, getSucceedingIAs(preceedingIA));
		}
	}

	@Override
	public void clearAssignments() {
		possibleAssignments.clear();
	}

	private ArrayList<Interaction> getSucceedingIAs(Interaction interaction) {
		ArrayList<Interaction> succeedingIAs = new ArrayList<Interaction>();
		for (Map.Entry<Interaction, ArrayList<Interaction>> entry : possibleAssignments.entrySet()) {
			// System.out.println("Key : " + entry.getKey() + " Value : " +
			// entry.getValue());
			for (Interaction ia : entry.getValue()) {
				if (ia == interaction)
					succeedingIAs.add(entry.getKey());
			}
		}
		return succeedingIAs;
	}

	private ArrayList<Interaction> getPossibleInteractions(IChoreographyNode node, Branch currentBranch) {
		ArrayList<Interaction> possiblePs = new ArrayList<Interaction>();
		NodeType splitType = null;
		while (splitType != NodeType.START) {
			splitType = currentBranch.getSplit().getNodeType();
			ArrayList<IChoreographyNode> branchNodes = currentBranch.getNodes();
			int pos = branchNodes.indexOf(node);
			if (pos > 0) {
				for (int i = pos; i != 0; i--) {
					IChoreographyNode precedingNode = branchNodes.get(i - 1);
					if (precedingNode instanceof Interaction) {
						possiblePs.add((Interaction) precedingNode);
					} else if (precedingNode instanceof AndGateway
							&& !splitTracking.getSplitMap().containsKey(precedingNode)) {
						Split split = splitTracking.getSplitByMergeNode(precedingNode);
						for (Branch innerBranch : split.getBranches()) {
							possiblePs = getPossibleInteractionsOfBranch(innerBranch, possiblePs);
						}
					}
				}
			}
			node = currentBranch.getSplit().getSplitNode();
			currentBranch = splitTracking.getBranchByNode(node);
		}
		return possiblePs;
	}

	private ArrayList<Interaction> getPossibleInteractionsOfBranch(Branch branch, ArrayList<Interaction> possibleQs) {
		ArrayList<IChoreographyNode> branchNodes = branch.getNodes();
		int size = branchNodes.size();
		for (int i = 0; i < size; i++) {
			IChoreographyNode branchNode = branchNodes.get(i);
			if (branchNode instanceof Interaction) {
				possibleQs.add((Interaction) branchNode);
			} else if (branchNode instanceof AndGateway && splitTracking.getSplitMap().containsKey(branchNode)) {
				for (Branch innerBranch : splitTracking.getSplitMap().get(branchNode).getBranches()) {
					if (!innerBranch.getNodes().isEmpty()) {
						IChoreographyNode firstNode = innerBranch.getNodes().get(0);
						System.out.println(innerBranch.getNodes());
						System.out.println(firstNode);
					}
					possibleQs = getPossibleInteractionsOfBranch(innerBranch, possibleQs);
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
		System.out.println("------");
		for (Map.Entry<Interaction, ArrayList<Interaction>> entry : possibleAssignments2.entrySet()) {
			System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
		}
	}

	public HashMap<Interaction, ArrayList<Interaction>> getPossibleAssignments() {
		return possibleAssignments2;
	}

}
