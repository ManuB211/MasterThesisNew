package at.ac.c3pro.chormodel.compliance;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import at.ac.c3pro.chormodel.generation.Branch;
import at.ac.c3pro.chormodel.generation.ChorModelGenerator.NodeType;
import at.ac.c3pro.chormodel.generation.Split;
import at.ac.c3pro.node.AndGateway;
import at.ac.c3pro.node.Event;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.Interaction;
import at.ac.c3pro.node.Node;
import at.ac.c3pro.node.XorGateway;


public class LeadsTo extends OrderPattern {
	
	private HashMap<Interaction, ArrayList<Interaction>> possibleAssignments = new HashMap<Interaction, ArrayList<Interaction>>();
	
	
	public LeadsTo(String label, Interaction p, Interaction q) {
		super(label, p, q);
		this.type = PatternType.ORDER;
	}
	
	/*
	 * possible p assignments:
	 * - every interaction, except the last one 
	 * possible q assignments:
	 * - every interaction on path after p (not within XOR)
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
					IChoreographyNode nextNode = branch.getNextNode(interaction);
					System.out.println("Next Node: " + nextNode);
					possibleQs = this.getPossibleQs(nextNode, branch, possibleQs);
					if (!possibleQs.isEmpty()) {
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
	
	
	
	private ArrayList<Interaction> getPossibleQs(IChoreographyNode node, Branch currentBranch, ArrayList<Interaction> possibleQs) {
		if (node != null)
			possibleQs = getPossibleQsInnerBranch(node, currentBranch, possibleQs);
		possibleQs = getPossibleQsOuterBranch(node, currentBranch, possibleQs);
		return possibleQs;
	}

	private ArrayList<Interaction> getPossibleQsInnerBranch(IChoreographyNode node, Branch currentBranch, ArrayList<Interaction> possibleQs) {
		ArrayList<IChoreographyNode> branchNodes = currentBranch.getNodes();
		System.out.println(branchNodes);
		int pos = branchNodes.indexOf(node);
		System.out.println("Pos: " + pos);
		int size = branchNodes.size();
		System.out.println("Size: " + size);
		for (int i = pos; i < size; i++) {
			IChoreographyNode branchNode = branchNodes.get(i);
			if (branchNode instanceof Interaction) {
				possibleQs.add((Interaction) branchNode); 
			} else if (branchNode instanceof AndGateway && splitTracking.getSplitMap().containsKey(branchNode)) {
				for (Branch innerBranch : splitTracking.getSplitMap().get(branchNode).getBranches()) {
					if (!innerBranch.getNodes().isEmpty()) {
						IChoreographyNode firstNode = innerBranch.getNodes().get(0);
						System.out.println(innerBranch.getNodes());
						System.out.println(firstNode);
						possibleQs = getPossibleQsInnerBranch(firstNode, innerBranch, possibleQs);
					}
				}
			}
		}
		return possibleQs;
	}
	
	private ArrayList<Interaction> getPossibleQsOuterBranch(IChoreographyNode node, Branch currentBranch, ArrayList<Interaction> possibleQs) {
		IChoreographyNode merge = currentBranch.getSplit().getMergeNode(); //merge node of branch
		while (!(merge instanceof Event)) {
			currentBranch = splitTracking.getBranchByNode(merge);
			ArrayList<IChoreographyNode> branchNodes = currentBranch.getNodes();
			int pos = branchNodes.indexOf(merge);
			int size = branchNodes.size();
			
			for (int i = pos; i < size; i++) {
				IChoreographyNode branchNode = branchNodes.get(i);
				if (branchNode instanceof Interaction) {
					possibleQs.add((Interaction) branchNode);
				} else if (branchNode instanceof AndGateway && splitTracking.getSplitMap().containsKey(branchNode)) {
					for (Branch innerBranch : splitTracking.getSplitMap().get(branchNode).getBranches()) {
						if (!innerBranch.getNodes().isEmpty()) {
							IChoreographyNode firstNode = innerBranch.getNodes().get(0);
							possibleQs = getPossibleQsInnerBranch(firstNode, innerBranch, possibleQs);
						}
					}
				}
			}
			merge = currentBranch.getSplit().getMergeNode();
		}		
		return possibleQs;
	}
	
	@Override
	public void printAssignments() {
		for (Map.Entry<Interaction, ArrayList<Interaction>> entry : possibleAssignments.entrySet()) {
			System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
		}
	}
	

	public HashMap<Interaction, ArrayList<Interaction>> getPossibleAssignments() {
		return possibleAssignments;
	}

}
