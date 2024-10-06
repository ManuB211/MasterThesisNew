package at.ac.c3pro.chormodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.ac.c3pro.ImpactAnalysis.ImpactAnalysisUtil.Pair;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.IGateway;
import at.ac.c3pro.node.IPrivateNode;
import at.ac.c3pro.node.IPublicNode;

public class Choreography {

	public Collaboration collaboration = null;// choreography model + all public models and roles map
	public List<IPrivateModel> privateModels = new ArrayList<IPrivateModel>();
	public String id;
	public Map<IPrivateModel, IPublicModel> P2P = new HashMap<IPrivateModel, IPublicModel>();
	public Map<IPublicNode, IChoreographyNode> Pu2Ch = new HashMap<IPublicNode, IChoreographyNode>();
	public IChoreographyModel choreo = null;
	public Map<IPrivateNode, IPublicNode> Pr2Pu = new HashMap<IPrivateNode, IPublicNode>();
	public Map<IPublicNode, IPrivateNode> Pu2Pr = new HashMap<IPublicNode, IPrivateNode>();
	public Map<IChoreographyNode, Pair<IPublicNode, IPublicNode>> Ch2PuPair = new HashMap<IChoreographyNode, Pair<IPublicNode, IPublicNode>>();
	public Map<IGateway, Set<Pair<IRole, IGateway>>> ChGtw2PuGtws = new HashMap<IGateway, Set<Pair<IRole, IGateway>>>();
	public Map<Role, IPrivateModel> R2PrM = new HashMap<Role, IPrivateModel>();

	public Choreography() {

	}

	public Choreography(String id, Collaboration collaboration, List<IPrivateModel> privateModels,
			IChoreographyModel cm) {
		this.collaboration = collaboration;
		this.privateModels = privateModels;
		this.id = id;
		this.choreo = cm;
	}

	public IPrivateModel getPrivateModel(String name) {
		for (IPrivateModel p : privateModels)
			if (p.getName().equals(name))
				return p;

		return null;
	}

	public boolean addPrivateModel(PrivateModel p) {
		if (privateModels.contains(p))
			return false;
		privateModels.add(p);
		return true;
	}

	public boolean removePrivateModel(IPrivateModel p) {
		if (!privateModels.contains(p))
			return false;
		privateModels.remove(p);
		return true;
	}

	public boolean replacePrivateModel(PrivateModel pold, PrivateModel pnew) {
		if (!privateModels.contains(pold) || privateModels.contains(pnew))
			return false;
		privateModels.remove(pold);
		privateModels.add(pnew);
		return true;
	}

	public Role getRole(String role) {
		for (Role r : this.collaboration.roles)
			if (r.name.equals(role))
				return r;
		return null;

	}

}
