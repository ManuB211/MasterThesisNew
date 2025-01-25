package at.ac.c3pro.ChangeNegotiation;

import at.ac.c3pro.change.ChangeOperation;
import at.ac.c3pro.chormodel.PrivateModel;
import at.ac.c3pro.chormodel.Role;

public class PrivateCost extends Cost {
    Role partner;

    public PrivateCost(Role partner) {
        // TODO Auto-generated constructor stub
        super();
        this.partner = partner;
    }


    public void calculatecost(PrivateBO po, PrivateModel pm, ChangeOperation op) {

    }

    public String toString() {

        //return "("+partner +","+super.toString()+")";
        return super.toString();
    }

}

