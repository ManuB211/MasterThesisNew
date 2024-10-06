package at.ac.c3pro.chormodel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.ac.c3pro.ImpactAnalysis.ImpactAnalysisUtil.Pair;
import at.ac.c3pro.node.IGateway;
import at.ac.c3pro.node.IPublicNode;

public class Collaboration implements ICollaboration{

	public String id;
	public Set<Role> roles = new HashSet<Role>();
	public Set<IPublicModel> puModels = new HashSet<IPublicModel>();
    public Map<Role,IPublicModel>  R2PuM = new HashMap<Role,IPublicModel>();
    public Map<IPublicModel, Role> PuM2R = new HashMap<IPublicModel, Role>();
    public Map<IPublicNode,IPublicNode> Pu2Pu = new HashMap<IPublicNode,IPublicNode>();
    //Name2PuGtws is a map between A gateway Name and all its instances in the public models. Name-->Set((role,Gtw)).
	public Map<String, Set<Pair<IRole,IGateway>>> Name2PuGtws = new HashMap<String, Set<Pair<IRole,IGateway>>>();
	public Map<IGateway, IGateway> PuGtw2chorGtw = new HashMap<IGateway, IGateway>();
	
	
	public Collaboration(Set<IPublicModel> puModels, String id, Set<Role> roles){
		this.puModels= puModels;
		this.id = id;
		this.roles = roles;
	}
	
	public Collaboration(String id){
		this.id = id;
	}
	
	//public void 
	public boolean addPublicModel(Role r, PublicModel pm){
		if(puModels.contains(pm))
			return false;
		puModels.add(pm);
		R2PuM.put(r, pm);
		PuM2R.put(pm, r);
		return true;
	}
	
	public boolean removePublicModel(PublicModel pm){
		if(!puModels.contains(pm))
			return false;
		puModels.remove(pm);//collab.indexOf(pm));
		
		return true;
	}
	
	
	public boolean replacePublicModel (PublicModel pmOld, PublicModel pmNew){	
		if(puModels.contains(pmOld) && !puModels.contains(pmNew)){
			puModels.remove(pmOld);
			puModels.add(pmNew);
			return true;
		}
		return false;
	}
	
	public boolean addRole(Role role){
		return this.roles.add(role);
	}
	
	public boolean removeRole(Role role){
		return this.roles.remove(role);
	}
	
	public IPublicModel getPublicModel(String pmName){
		for(IPublicModel pum : this.puModels)
			if(pum.getName().equals(pmName))
				return pum;
		return null;
	}
	
	Set<IPublicModel> Chor2PuM(){
		
		for(Role role: roles){
		//	collab.add(new PublicModel(choreo.cm_rpst.projection(role)));
			//projection method projects the choreography model on a given partner to compute the public model of the latter
			//projection should be in rpst utils
		}
		
		return puModels;
	}
	
}
