package at.ac.c3pro.change;

import org.jbpt.algo.tree.rpst.IRPSTNode;

import at.ac.c3pro.changePropagation.ChangePropagationUtil.ChgOpType;
import at.ac.c3pro.chormodel.Choreography;
import at.ac.c3pro.chormodel.IRpstModel;
import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.chormodel.RpstModel;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.INode;
import at.ac.c3pro.util.FragmentUtil;

public class Delete<E extends Edge<N>, N extends INode> extends ChangeOperation implements IChangeOperation {

	public IRPSTNode<E, N> f1;
	public RpstModel<E, N> model;
	public N activity;

	// public Delete(IRPSTNode<E,N> f, N activity) {
	// this.f = f;
	// this.activity = activity;
	// }

	public Delete(IRPSTNode<E, N> f1, RpstModel<E, N> model, Choreography c, Role currentRole) {

		super();
		this.f1 = f1;
		this.model = model;
		this.c = c;
		this.currentRole = currentRole;
		this.activity = null;
	}

	public Delete(N activity, IRpstModel<E, N> model, Choreography c, Role currentRole) {

		super();
		this.f1 = null;
		this.activity = activity;
		this.model = (RpstModel<E, N>) model;
		this.c = c;
		this.currentRole = currentRole;
	}

	public int getNb_nodes() {
		if (f1 != null)
			return FragmentUtil.collectNodes(f1).size();
		else
			return 1;
	}

	public String toString() {
		// return "<Delete: f:" + this.f1+ " activity:" + this.activity + ">";
		if (this.activity == null)
			return "<Delete: f:" + this.f1 + ">";
		else
			return "<Delete: activity:" + this.activity + ">";
	}

	public ChgOpType getType() {
		return ChgOpType.Delete;
	}

	@Override
	public void Propagate() {
		// TODO Auto-generated method stub
		//
	}

	@Override
	public void Decompose() {
		// TODO Auto-generated method stub
		//
	}

}
