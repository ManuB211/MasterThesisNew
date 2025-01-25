package at.ac.c3pro.chormodel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Public2PrivateMap {

    protected Map<PrivateModel, PublicModel> p2p = new HashMap<PrivateModel, PublicModel>();

    Public2PrivateMap(Map<PrivateModel, PublicModel> p2p) {
        this.p2p.putAll(p2p);
    }

    public Set getPrivateModels() {
        return p2p.keySet();
    }

    public Collection getPublicModels() {
        return p2p.values();
    }

    public PublicModel getPublicModel(PrivateModel pr) {
        return p2p.get(pr);
    }

    public PrivateModel getPrivateModel(PublicModel pu) {
        PrivateModel p = null;
        if (p2p.containsValue(pu)) {
            for (PrivateModel pr : p2p.keySet())
                if (p2p.get(pr).equals(pu)) {
                    p = pr;
                    break;
                }
        }
        return p;
    }

    public boolean ConsistencyCheck() {
        for (PrivateModel p : p2p.keySet())
            if (!ConsistencyCheck(p, p2p.get(p)))
                return false;
        return true;
    }

    public boolean ConsistencyCheck(PrivateModel pr, PublicModel pu) {
        //To be imlemented...
        return true;
    }

}
