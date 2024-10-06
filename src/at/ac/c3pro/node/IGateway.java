package at.ac.c3pro.node;

import at.ac.c3pro.chormodel.RpstModel;

public interface IGateway extends IPublicNode, IChoreographyNode, IPrivateNode{

	public boolean isJoin(RpstModel<?, Node>  model);
	public boolean isSplit(RpstModel<?, Node> model);
	
}
