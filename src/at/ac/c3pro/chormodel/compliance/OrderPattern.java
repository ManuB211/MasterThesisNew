package at.ac.c3pro.chormodel.compliance;

import at.ac.c3pro.node.Interaction;

public abstract class OrderPattern extends CompliancePattern {

	protected Interaction q;

	public OrderPattern(String label, Interaction p, Interaction q) {
		super(label, p);
		this.q = q;
	}

	public Interaction getQ() {
		return this.q;
	}

}
