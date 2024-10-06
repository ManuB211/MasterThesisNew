package at.ac.c3pro.chormodel.compliance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import at.ac.c3pro.chormodel.generation.SplitTracking;
import at.ac.c3pro.node.Interaction;

public class ComplianceController {

	public enum PositionStatus {
		FREE, RESERVED
	}

	private SplitTracking splitTracking;
	private ArrayList<CompliancePattern> complianceRules = new ArrayList<CompliancePattern>();
	private ArrayList<Interaction> existInteractions = new ArrayList<Interaction>();
	private ArrayList<Interaction> universalInteractions = new ArrayList<Interaction>();
	private HashMap<Interaction, ArrayList<Interaction>> orderDependencies = new HashMap<Interaction, ArrayList<Interaction>>();
	private ArrayList<Interaction> interactionOrder = new ArrayList<Interaction>();
	private ArrayList<CompliancePattern> conflictedRules = new ArrayList<CompliancePattern>();
	private HashMap<Interaction, ArrayList<Interaction>> possibleAssignments = new HashMap<Interaction, ArrayList<Interaction>>();
	private HashMap<Interaction, ArrayList<Interaction>> interactionHierarchy = new HashMap<Interaction, ArrayList<Interaction>>();
	private HashMap<Interaction, Interaction> graphAssignments = new HashMap<Interaction, Interaction>();

	public ComplianceController() {
		this.splitTracking = SplitTracking.getInstance();
	}

	public boolean assign() {
		determinePossibleAssignments();
		orderInteractions();
		printPossibleIAAssignments();
		loadInteractionHierarchy();
		for (Interaction ia : interactionOrder) {
			if (!assignInteraction(ia))
				return false;
		}
		ArrayList<Interaction> occurrenceInteractions = universalInteractions;
		for (Interaction existInteraction : existInteractions) {
			if (!occurrenceInteractions.contains(existInteraction))
				occurrenceInteractions.add(existInteraction);
		}
		for (Interaction ia : occurrenceInteractions) {
			if (!graphAssignments.containsKey(ia)) {
				if (!assignInteraction(ia))
					return false;
			}
		}

		return true;
	}

	/*
	 * Add Rule - check if rule is in conflict with already added rules: -- is there
	 * a rule that defines that q -> p? (hence that p -> q not possible anymore)
	 */
//	public void addRule(CompliancePattern rule) {
//		Interaction p = rule.getP();
//		if (rule instanceof OrderPattern) {
//			Interaction q = ((OrderPattern) rule).getQ();
//			if (!conflictCheck(rule, p, q)) {
//				if (orderDependencies.containsKey(p)) {
//					if (!orderDependencies.get(p).contains(q)) {
//						ArrayList<Interaction> qs = orderDependencies.get(p);
//						qs.add(q);
//						orderDependencies.put(p, qs);
//						complianceRules.add(rule);
//					} else {
//						conflictedRules.add(rule);
//					}
//				} else {
//					ArrayList<Interaction> qs = new ArrayList<Interaction>();
//					qs.add(q);
//					orderDependencies.put(p, qs);
//					complianceRules.add(rule);
//				}
//			} else {
//				conflictedRules.add(rule);
//			}
//		} else if (rule instanceof Universal) {
//			if (!universalInteractions.contains(p)) {
//				universalInteractions.add(p);
//				complianceRules.add(rule);
//			} else {
//				conflictedRules.add(rule);
//			}
//		} else if (rule instanceof Exists) {
//			if (!existInteractions.contains(p)) {
//				existInteractions.add(p);
//				complianceRules.add(rule);
//			} else {
//				conflictedRules.add(rule);
//			}
//		}
//	}

//	private boolean conflictCheck(CompliancePattern rule, Interaction p, Interaction q) {
//		if (rule instanceof OrderPattern) {
//			if (orderConflictCheck(rule, p, q)) {
//				return true;
//			}
//		} else if (rule instanceof Exclusive)  {
//			if (occ)
//		}
//
//		return false;
//	}

	/*
	 * Check Conflict: - check the succeeding interactions of q and their succeeding
	 * interactions: -- conflict: if one of them has p as succeeding interaction
	 */
	private boolean orderConflictCheck(CompliancePattern rule, Interaction p, Interaction q) {
		if (orderDependencies.containsKey(q)) {
			for (Interaction q2 : orderDependencies.get(q)) {
				if (q2 == p) {
					return true;
				} else if (orderConflictCheck(rule, p, q2)) {
					return true;
				}
			}
		}
		return false;
	}

//	private boolean occurenceConflictCheck();

	public ArrayList<CompliancePattern> getComplianceRules() {
		return complianceRules;
	}

	public void setComplianceRules(ArrayList<CompliancePattern> complianceRules) {
		this.complianceRules = complianceRules;
	}

	public void determinePossibleAssignments() {
		for (CompliancePattern cr : complianceRules) {
			System.out.println(cr);
			cr.findPossibleAssignments();
		}
	}

	public void orderInteractions() {
		int iaCount = getInteractionCount();
		interactionOrder.clear();
		while (interactionOrder.size() < iaCount) {
			interactionOrder.add(getNextInteraction());
		}
	}

	private Interaction getNextInteraction() {
		ArrayList<Interaction> possibleInteractions = new ArrayList<Interaction>();
		for (Map.Entry<Interaction, ArrayList<Interaction>> entry : orderDependencies.entrySet()) {
			if (!interactionOrder.contains(entry.getKey()) && !hasUnrankedPredecessors(entry.getKey())) {
				Interaction ia = entry.getKey();
				possibleInteractions.add(ia);
			}
			for (Interaction successor : entry.getValue()) {
				if (!interactionOrder.contains(successor) && !hasUnrankedPredecessors(successor)) {
					possibleInteractions.add(successor);
				}
			}
		}
		int index = ThreadLocalRandom.current().nextInt(possibleInteractions.size());
		return possibleInteractions.get(index);
	}

	/*
	 * (1) get all affected CRs (2)
	 */
	public boolean assignInteraction(Interaction ia) {
		System.out.println("INTERACTION: " + ia);
		// get affected CRs
		ArrayList<CompliancePattern> affectedCRs = new ArrayList<CompliancePattern>();
		for (CompliancePattern cr : complianceRules) {
			if (cr instanceof OrderPattern) {
				if (cr.getP() == ia)
					affectedCRs.add(cr);
				else if (((OrderPattern) cr).getQ() == ia) {
					affectedCRs.add(cr);
				}
			} else if (cr instanceof Universal) {
				if (cr.getP() == ia)
					affectedCRs.add(cr);
			} else if (cr instanceof Exists) {
				if (cr.getP() == ia)
					affectedCRs.add(cr);
			}
		}
		HashMap<CompliancePattern, ArrayList<Interaction>> crPossibleAssignments = new HashMap<CompliancePattern, ArrayList<Interaction>>();
		for (CompliancePattern cr : affectedCRs) {
			ArrayList<Interaction> tempPossibleAssignments = new ArrayList<Interaction>();
			System.out.println(cr.getLabel());
			if (cr.getP() == ia) {
				if (cr instanceof LeadsTo) {
					for (Map.Entry<Interaction, ArrayList<Interaction>> entry : ((LeadsTo) cr).getPossibleAssignments()
							.entrySet()) {
						tempPossibleAssignments.add(entry.getKey());
					}
				} else if (cr instanceof Precedes) {
					for (Map.Entry<Interaction, ArrayList<Interaction>> entry : ((Precedes) cr).getPossibleAssignments()
							.entrySet()) {
						tempPossibleAssignments.add(entry.getKey());
					}
				} else if (cr instanceof Universal) {
					tempPossibleAssignments.addAll(((Universal) cr).getPossibleAssignments());
				} else if (cr instanceof Exists) {
					tempPossibleAssignments.addAll(((Exists) cr).getPossibleAssignments());
				}
			} else {
				if (cr instanceof LeadsTo) {
					Interaction pAssignment = graphAssignments.get(cr.getP());
					tempPossibleAssignments.addAll(((LeadsTo) cr).getPossibleAssignments().get(pAssignment));
				} else if (cr instanceof Precedes) {
					Interaction pAssignment = graphAssignments.get(cr.getP());
					tempPossibleAssignments.addAll(((Precedes) cr).getPossibleAssignments().get(pAssignment));
				}
			}
			cr.printAssignments();
			System.out.println(tempPossibleAssignments);
			crPossibleAssignments.put(cr, tempPossibleAssignments);
		}
		ArrayList<Interaction> commonPossibleAssignments = getCommonAssignments(crPossibleAssignments);
		if (commonPossibleAssignments.isEmpty()) {
			System.out.println("NO COMMON POSSIBLE ASSIGNMENT: " + ia);
			return false;
		} else {
			Interaction graphInteraction = getInteractionWithMostSucceedingIAs(commonPossibleAssignments);
			if (graphInteraction == null)
				return false;
			graphAssignments.put(ia, graphInteraction);
			graphInteraction.setName(ia.getName());
		}
		possibleAssignments.put(ia, commonPossibleAssignments);
		return true;
	}

	private Interaction getInteractionWithMostSucceedingIAs(ArrayList<Interaction> possibleAssignments) {
		Interaction selectedInteraction = null;
		int succeedingIACount = 0;
		for (Interaction ia : possibleAssignments) {
			System.out.println(ia);
			if (!graphAssignments.containsValue(ia)) {
				if (interactionHierarchy.get(ia) != null) {
					if (interactionHierarchy.get(ia).size() > succeedingIACount) {
						succeedingIACount = interactionHierarchy.get(ia).size();
						selectedInteraction = ia;
					}
				}
			}
		}
		return selectedInteraction;
	}

	private ArrayList<Interaction> getCommonAssignments(
			HashMap<CompliancePattern, ArrayList<Interaction>> crPossibleAssignments) {
		ArrayList<Interaction> commonAssignments = new ArrayList<Interaction>();
		for (Map.Entry<CompliancePattern, ArrayList<Interaction>> entry : crPossibleAssignments.entrySet()) {
			commonAssignments = entry.getValue();
		}
		for (Map.Entry<CompliancePattern, ArrayList<Interaction>> entry : crPossibleAssignments.entrySet()) {
			commonAssignments.retainAll(entry.getValue());
		}
		return commonAssignments;
	}

	private boolean hasUnrankedPredecessors(Interaction ia) {
		for (Map.Entry<Interaction, ArrayList<Interaction>> entry : orderDependencies.entrySet()) {
			for (Interaction interaction : entry.getValue()) {
				if (interaction == ia && !interactionOrder.contains(entry.getKey())) {
					return true;
				}
			}
		}
		return false;
	}

	private int getInteractionCount() {
		ArrayList<Interaction> interactions = new ArrayList<Interaction>();
		for (Map.Entry<Interaction, ArrayList<Interaction>> entry : orderDependencies.entrySet()) {
			if (!interactions.contains(entry.getKey()))
				interactions.add(entry.getKey());
			for (Interaction interaction : entry.getValue()) {
				if (!interactions.contains(interaction))
					interactions.add(interaction);
			}
		}
		return interactions.size();
	}

	public void printPossibleIAAssignments() {
		for (Map.Entry<Interaction, ArrayList<Interaction>> entry : possibleAssignments.entrySet()) {
			System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
		}
	}

	public ArrayList<CompliancePattern> getAffectedRules(Interaction ia) {
		ArrayList<CompliancePattern> affectedCRs = new ArrayList<CompliancePattern>();
		for (CompliancePattern cr : complianceRules) {
			if (cr instanceof OrderPattern) {
				if (cr.getP() == ia)
					affectedCRs.add(cr);
				else if (((OrderPattern) cr).getQ() == ia) {
					affectedCRs.add(cr);
				}
			} else if (cr instanceof Universal) {
				if (cr.getP() == ia)
					affectedCRs.add(cr);
			}
		}
		return affectedCRs;
	}

	public ArrayList<CompliancePattern> getAffectedRulesSucceeding(Interaction ia) {
		ArrayList<CompliancePattern> affectedCRs = new ArrayList<CompliancePattern>();
		for (CompliancePattern cr : complianceRules) {
			if (cr instanceof OrderPattern) {
				if (((OrderPattern) cr).getQ() == ia) {
					affectedCRs.add(cr);
				}
			}
		}
		return affectedCRs;
	}

	public void printInteractionOrderWithAffectedRules() {
		for (Interaction ia : interactionOrder) {
			System.out.print("Interaction: " + ia + " - related rules: ");
			for (CompliancePattern cr : getAffectedRules(ia)) {
				System.out.print(cr.getLabel() + " ");
			}
			System.out.print("\n");
		}
	}

	public void printComplianceData() {
		System.out.println("ADDED RULES:");
		for (CompliancePattern cr : complianceRules) {
			System.out.println(cr.getLabel());
		}
		System.out.println("CONFLICTED RULES:");
		for (CompliancePattern cr : conflictedRules) {
			System.out.println(cr.getLabel());
		}
		System.out.println("ORDER DEPENDENCIES:");
		for (Map.Entry<Interaction, ArrayList<Interaction>> entry : orderDependencies.entrySet()) {
			System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
		}
		System.out.println("UNIVERSAL IAs:");
		System.out.println(universalInteractions);
		printInteractionOrderWithAffectedRules();
//		System.out.println("INTERACTION HIERARCHY:");
//		for (Map.Entry<Interaction, ArrayList<Interaction>> entry : interactionHierarchy.entrySet()) {
//			System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
//		} 
		System.out.println("INTERACTION ASSIGNMENT:");
		for (Map.Entry<Interaction, Interaction> entry : graphAssignments.entrySet()) {
			System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
		}
	}

	public void clearPossibleAssignments() {
		for (CompliancePattern cr : complianceRules) {
			System.out.println(cr);
			cr.clearAssignments();
		}
	}

	public void loadInteractionHierarchy() {
		interactionHierarchy = splitTracking.getInteractionHierarchy();
	}

	public HashMap<Interaction, ArrayList<Interaction>> getInteractionHierarchy() {
		return interactionHierarchy;
	}

	public ArrayList<Interaction> getUniversalInteractions() {
		return universalInteractions;
	}

	public HashMap<Interaction, ArrayList<Interaction>> getOrderDependencies() {
		return orderDependencies;
	}

	public ArrayList<Interaction> getInteractionOrder() {
		return interactionOrder;
	}

	public ArrayList<CompliancePattern> getConflictedRules() {
		return conflictedRules;
	}

	public void reloadSplitTracking() {
		this.splitTracking = SplitTracking.getInstance();
		for (CompliancePattern cr : complianceRules) {
			cr.reloadSplitTracking();
		}
	}

	public void printInteractionAssignment() {
		System.out.println("INTERACTION ASSIGNMENT:");
		for (Map.Entry<Interaction, Interaction> entry : graphAssignments.entrySet()) {
			System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
		}
	}

	public HashMap<Interaction, Interaction> getGraphAssignments() {
		return graphAssignments;
	}

	public String printRule(CompliancePattern cr) {
		if (cr instanceof LeadsTo) {
			return cr.getLabel() + ": " + cr.getP().getName() + " LeadsTo " + ((OrderPattern) cr).getQ();
		} else if (cr instanceof Precedes) {
			return cr.getLabel() + ": " + cr.getP().getName() + " Precedes " + ((OrderPattern) cr).getQ();
		} else if (cr instanceof Universal) {
			return cr.getLabel() + ": " + cr.getP().getName() + " Universal";
		} else if (cr instanceof Exists) {
			return cr.getLabel() + ": " + cr.getP().getName() + " Exists";
		}
		return "fishiy";
	}

	public ArrayList<Interaction> getExistInteractions() {
		return existInteractions;
	}

}
