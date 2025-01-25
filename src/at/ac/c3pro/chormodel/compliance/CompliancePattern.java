package at.ac.c3pro.chormodel.compliance;

import at.ac.c3pro.chormodel.generation.SplitTracking;
import at.ac.c3pro.node.Interaction;

public abstract class CompliancePattern {

    public enum PatternType {
        ORDER, OCCURRENCE
    }

    protected Interaction p;
    protected SplitTracking splitTracking;
    protected PatternType type;
    protected String label;


    public CompliancePattern(String label, Interaction p) {
        this.label = label;
        this.p = p;
        this.splitTracking = SplitTracking.getInstance();
    }

    public abstract void findPossibleAssignments();

    public abstract void printAssignments();

    public abstract void clearAssignments();

    //public abstract boolean conflictCheck();

    public Interaction getP() {
        return p;
    }

    public void setP(Interaction p) {
        this.p = p;
    }

    public PatternType getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void reloadSplitTracking() {
        this.splitTracking = SplitTracking.getInstance();
    }


}
