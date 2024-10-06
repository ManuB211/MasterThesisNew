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

public class Replace<E extends Edge<N>, N extends INode> extends ChangeOperation implements IChangeOperation {

	public IRPSTNode<E, N> f1;
	public IRpstModel<E, N> f2;
	public RpstModel<E, N> model;

	// TODO: need to merge

	public Replace(IRPSTNode<E, N> a1, IRPSTNode<E, N> a2) {
		// this.f1 = a1;
		// this.f2 = a2;
	}

	public Replace(IRPSTNode<E, N> f1, RpstModel<E, N> f2, RpstModel<E, N> model, Choreography c, Role currentRole) {

		super();
		this.f1 = f1;
		this.f2 = f2;
		this.model = model;
		this.c = c;
		this.currentRole = currentRole;
	}

	public String toString() {
		return "<Replace: f1:" + this.f1 + " f2:" + this.f2 + ">";
	}

	public int getNb_nodes() {
		if (f1 != null && f2 != null)
			return FragmentUtil.collectNodes(f1).size() + f2.getdigraph().countVertices() - 2;
		else
			return 0;
	}

	public ChgOpType getType() {
		return ChgOpType.Replace;
	}

	@Override
	public void Propagate() {
		// TODO Auto-generated method stub

	}

	@Override
	public void Decompose() {
		// TODO Auto-generated method stub

	}

}
