package at.ac.c3pro.ChangeNegotiation;

import java.util.ArrayList;

import at.ac.c3pro.chormodel.Role;

public class TransparentNegotiation {
	CombinedCost source;
	ArrayList<CombinedCost> targets = new ArrayList<CombinedCost>();
	CommonCost commoncost;
	double utilityscore;
	
	
	public TransparentNegotiation(Role partner, CombinedCost source, ArrayList<CombinedCost> targets, CommonCost commoncost) {
			this.source = source;
			this.targets = targets;
			this.commoncost = commoncost;
		}

		public void calculateUtility(){
			
			//TBD
			
		}
}