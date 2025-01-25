package at.ac.c3pro.ChangeNegotiation;

import at.ac.c3pro.chormodel.Role;

public class CombinedCost {

    public PrivateCost privatecost;
    public PublicCost publiccost;

    public CombinedCost(Role partner) {

        privatecost = new PrivateCost(partner);
        publiccost = new PublicCost(partner);
    }

    public CombinedCost(PrivateCost privatecost, PublicCost publiccost) {

        this.privatecost = privatecost;
        this.publiccost = publiccost;
    }

    public String toString() {
        return privatecost.partner + "<PrCost" + privatecost + ",PuCost" + publiccost + ">";
    }

}
