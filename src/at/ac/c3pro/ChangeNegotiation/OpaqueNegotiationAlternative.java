package at.ac.c3pro.ChangeNegotiation;

import java.util.ArrayList;


public class OpaqueNegotiationAlternative {
    public PublicCost source;
    public ArrayList<CombinedCost> targets = new ArrayList<CombinedCost>();
    public CommonCost commoncost;


    public OpaqueNegotiationAlternative(PublicCost source, ArrayList<CombinedCost> targets) {
        this.source = source;
        this.targets = targets;
    }

    public OpaqueNegotiationAlternative(PublicCost source) {
        this.source = source;
    }

    public OpaqueNegotiationAlternative() {
    }

    public void setcommoncost(CommonCost commoncost) {
        this.commoncost = commoncost;
    }

    public void addtarget(CombinedCost target) {
        targets.add(target);
    }

    public void addtargets(ArrayList<CombinedCost> targets) {
        this.targets.addAll(targets);

    }

    public ArrayList<SinglePartnerAlternative> decompose() {
        ArrayList<SinglePartnerAlternative> alternatives = new ArrayList<SinglePartnerAlternative>();
        for (CombinedCost comc : targets) {
            SinglePartnerAlternative spa = new SinglePartnerAlternative(source, comc, this.commoncost);
            alternatives.add(spa);
        }
        return alternatives;
    }

    public String toString() {
        String output = "";
        if (source != null)
            output = "source = (" + source.partner + ", PuCost " + source + ")   targets= ";
        if (targets != null)
            for (CombinedCost target : targets)
                output = output + "(" + target.publiccost.partner + "," + target + ")   ";
        return output;
    }
}
