package at.ac.c3pro.chormodel.compliance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import at.ac.c3pro.chormodel.generation.Branch;
import at.ac.c3pro.chormodel.generation.Split;
import at.ac.c3pro.node.AndGateway;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.Interaction;

public class PLeadsTo extends OrderPattern {

	private HashMap<Interaction, ArrayList<Interaction>> possibleAssignments = new HashMap<Interaction, ArrayList<Interaction>>();
	private boolean allowInsideAndSplit = false;

	public PLeadsTo(String label, Interaction p, Interaction q) {
		super(label, p, q);
		this.type = PatternType.ORDER;
	}

	/*
	 * possible P assignments: - every interaction on main branch (not inside any
	 * Split) possible Q assignments: - every interaction directly followed by P
	 */
//	@Override
//	public void findPossibleAssignments() {
//		ArrayList<IChoreographyNode> branchNodes = splitTracking.getMainBranch().getNodes();
//		for (int i = 0; i < branchNodes.size(); i++) {
//			IChoreographyNode node = branchNodes.get(i);
//			if (node instanceof Interaction) {
//				if (i + 1 != branchNodes.size()) {
//					IChoreographyNode succeedingNode = branchNodes.get(i + 1);
//					if (succeedingNode instanceof Interaction)
//						possibleAssignments.put((Interaction) node, (Interaction) succeedingNode);
//				}
//			}
//		}
//	}

	@Override
	public void findPossibleAssignments() {
		IChoreographyNode succeedingNode = null;
		for (Split split : splitTracking.getSplits()) {
			System.out.println(split.getSpiltNode());
			if ((!allowInsideAndSplit && splitTracking.insideAndSplit(split)) || splitTracking.insideXorSplit(split))
				continue;
			for (Branch branch : split.getBranches()) {
				ArrayList<IChoreographyNode> branchNodes = branch.getNodes();
				System.out.println("All Nodes: " + branch.getNodes());
				System.out.println("IAs: " + branch.getInteractions());
				for (int i = 0; i < branchNodes.size(); i++) {
					IChoreographyNode node = branchNodes.get(i);
					if (node instanceof Interaction) {
						if (i + 1 != branchNodes.size()) {
							succeedingNode = branchNodes.get(i + 1);
							if (succeedingNode instanceof Interaction) {
								ArrayList<Interaction> possibleQs = new ArrayList<Interaction>();
								possibleQs.add((Interaction) succeedingNode);
								possibleAssignments.put((Interaction) node, possibleQs);
							} else if (allowInsideAndSplit && succeedingNode instanceof AndGateway) {
								if (splitTracking.isSplit(succeedingNode)) {
									ArrayList<Interaction> possibleQs = new ArrayList<Interaction>();
									for (Branch andBranch : splitTracking.getSplit(succeedingNode).getBranches()) {
										possibleQs = getPossibleQs(andBranch, possibleQs);
									}
									if (!possibleQs.isEmpty()) {
										possibleAssignments.put((Interaction) node, possibleQs);
									}
								}
							}
						} else {
							IChoreographyNode mergeNode = split.getMergeNode();
							if (mergeNode instanceof AndGateway) {
								// get directly succeeding Interaction after merge
								ArrayList<Interaction> succeedingInteractions = new ArrayList<Interaction>();
								succeedingInteractions = splitTracking.getInteractionsAfterMerge(mergeNode,
										succeedingInteractions);
								if (!succeedingInteractions.isEmpty()) {
									possibleAssignments.put((Interaction) node, succeedingInteractions);
								}
							}
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

	private ArrayList<Interaction> getPossibleQs(Branch branch, ArrayList<Interaction> possibleQs) {
		IChoreographyNode firstNode = branch.getNodes().get(0);
		if (firstNode instanceof Interaction) {
			possibleQs.add((Interaction) firstNode);
			return possibleQs;
		} else if (firstNode instanceof AndGateway) {
			for (Branch innerBranch : splitTracking.getSplit(firstNode).getBranches()) {
				possibleQs = getPossibleQs(innerBranch, possibleQs);
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

	public HashMap<Interaction, ArrayList<Interaction>> getPossibleAssignments() {
		return possibleAssignments;
	}

}
