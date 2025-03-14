package at.ac.c3pro.chormodel;

import at.ac.c3pro.node.IGateway;
import at.ac.c3pro.node.IPublicNode;
import at.ac.c3pro.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Collaboration implements ICollaboration {

    public String id;
    public Set<Role> roles = new HashSet<>();
    public Set<IPublicModel> puModels = new HashSet<>();
    public Map<Role, IPublicModel> R2PuM = new HashMap<>();
    public Map<IPublicModel, Role> PuM2R = new HashMap<>();
    public Map<IPublicNode, IPublicNode> Pu2Pu = new HashMap<>();
    // Name2PuGtws is a map between A gateway Name and all its instances in the
    // public models. Name-->Set((role,Gtw)).
    public Map<String, Set<Pair<IRole, IGateway>>> Name2PuGtws = new HashMap<>();
    public Map<IGateway, IGateway> PuGtw2chorGtw = new HashMap<>();

    public Collaboration(String id) {
        this.id = id;
    }

    public void addPublicModel(Role r, PublicModel pm) {
        if (puModels.contains(pm))
            return;
        puModels.add(pm);
        R2PuM.put(r, pm);
        PuM2R.put(pm, r);
    }

    public boolean addRole(Role role) {
        return this.roles.add(role);
    }


}
