package at.ac.c3pro.ChangeNegotiation;

import java.util.ArrayList;

import at.ac.c3pro.chormodel.Role;

public class OpaqueNegotiation {

	public CombinedCost source;
	public ArrayList<PublicCost> targets = new ArrayList<PublicCost>();
	public CommonCost commoncost;
	double utility;
	
	public OpaqueNegotiation(Role partner, CombinedCost source, ArrayList<PublicCost> targets, CommonCost commoncost) {
		this.source = source;
		this.targets = targets;
		this.commoncost = commoncost;
	}

	public String toString(){
	
		return "OpaqueNego:  source= "+source + " targets= "+targets + "  CommonCost= " + commoncost;			
	}
	
	public double utility(double privateweight, double publicweight, double commonweight){	
		 utility = privateweight*this.source.privatecost.cost + publicweight*this.source.publiccost.cost + commonweight*this.commoncost.cost;
		 return utility;
	}
	
	
}
