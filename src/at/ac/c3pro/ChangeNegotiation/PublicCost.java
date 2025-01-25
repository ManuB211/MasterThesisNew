package at.ac.c3pro.ChangeNegotiation;

import at.ac.c3pro.change.ChangeOperation;
import at.ac.c3pro.chormodel.PublicModel;
import at.ac.c3pro.chormodel.Role;

public class PublicCost extends Cost {

    Role partner;

    public PublicCost(Role partner) {
        super();
        this.partner = partner;
    }

    public void calculatecost(PublicBO po, PublicModel pm, ChangeOperation op) {

    }

    public String toString() {

        // return "("+partner +","+super.toString()+")";
        return super.toString();
    }

}
