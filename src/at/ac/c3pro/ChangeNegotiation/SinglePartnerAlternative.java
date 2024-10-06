package at.ac.c3pro.ChangeNegotiation;

import java.util.ArrayList;

import at.ac.c3pro.chormodel.Role;

public class SinglePartnerAlternative {

	public PublicCost source;
	public CombinedCost target;
	public CommonCost commoncost;
	public double utility;

			
	public SinglePartnerAlternative(PublicCost source, PublicCost target, Role partner) {
		// TODO Auto-generated constructor stub
		this.source = source;
		this.target = new CombinedCost(new PrivateCost(partner), target);		
	}
	
	public SinglePartnerAlternative(PublicCost source, CombinedCost target, CommonCost commoncost) {
		
		this.source = source;
		this.target = target;		
		this.commoncost=commoncost;
	}
	
	public void setcommoncost(CommonCost commoncost){
		this.commoncost = commoncost;		
	}
	
	public double utility(double privateweight, double publicweight, double commonweight){	
		utility = privateweight*this.target.privatecost.cost + publicweight*this.target.publiccost.cost + commonweight*this.commoncost.cost; 
		return utility;
	}
	
	public String toString(){
		return "source= (" +source.partner+", PuCost "+ source.toString() +")    target = ("+target.publiccost.partner+"," +target.toString()+")";
	}

}
