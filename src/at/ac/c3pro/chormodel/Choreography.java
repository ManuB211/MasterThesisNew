package at.ac.c3pro.chormodel;

import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.IGateway;
import at.ac.c3pro.node.IPublicNode;
import at.ac.c3pro.util.Pair;

import java.util.*;

public class Choreography {

    public Collaboration collaboration = null;// choreography model + all public models and roles map
    public List<IPrivateModel> privateModels = new ArrayList<IPrivateModel>();
    public Map<IPrivateModel, IPublicModel> P2P = new HashMap<IPrivateModel, IPublicModel>();
    public Map<IPublicNode, IChoreographyNode> Pu2Ch = new HashMap<IPublicNode, IChoreographyNode>();
    public Map<IChoreographyNode, Pair<IPublicNode, IPublicNode>> Ch2PuPair = new HashMap<IChoreographyNode, Pair<IPublicNode, IPublicNode>>();
    public Map<IGateway, Set<Pair<IRole, IGateway>>> ChGtw2PuGtws = new HashMap<IGateway, Set<Pair<IRole, IGateway>>>();
    public Map<Role, IPrivateModel> R2PrM = new HashMap<Role, IPrivateModel>();

    public Choreography() {

    }

}
