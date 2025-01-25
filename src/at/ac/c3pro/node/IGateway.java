package at.ac.c3pro.node;

import at.ac.c3pro.chormodel.RpstModel;

public interface IGateway extends IPublicNode, IChoreographyNode, IPrivateNode {

    boolean isJoin(RpstModel<?, Node> model);

    boolean isSplit(RpstModel<?, Node> model);

}
