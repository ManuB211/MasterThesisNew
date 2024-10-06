package at.ac.c3pro.changePropagation;

import java.util.Set;

import org.jbpt.algo.tree.rpst.IRPSTNode;

import at.ac.c3pro.changePropagation.ChangePropagationUtil.ChgOpType;
import at.ac.c3pro.chormodel.IRpstModel;
import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.node.AndGateway;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.INode;
import at.ac.c3pro.node.InteractionActivity;
import at.ac.c3pro.node.XorGateway;
import at.ac.c3pro.util.FragmentUtil;

public class Stats {

	// Metrics of the initial request
	public ChgOpType type;
	public Role role;
	public int Nb_Nodes_source = 0;
	public int nb_activ_source = 0;
	public int nb_AndGtw_source = 0;
	public int nb_XorGtw_source = 0;
	// Metrics of the propagation result
	public int Nb_Nodes_target = 0;
	public int nb_activ_target = 0;
	public int nb_AndGtw_target = 0;
	public int nb_XorGtw_target = 0;
	public int nb_affected_partners = 0;
	public int nb_Insert_generated = 0;
	public int nb_Replace_generated = 0;
	public int nb_Delete_generated = 0;
	public int nb_merges = 0;
	public int nb_nodes_to_merge = 0;

	public Stats(Role role, ChgOpType t) {
		this.role = role;
		this.type = t;
	}

	/**
	 * Statistics of the initial change request
	 */
	public Stats(Role role, ChgOpType t, int Nb_Nodes_source, int nb_activ_source, int nb_AndGtw_source,
			int nb_XorGtw_source) {
		this.type = t;
		this.role = role;
		this.Nb_Nodes_source = Nb_Nodes_source;
		this.nb_activ_source = nb_activ_source;
		this.nb_AndGtw_source = nb_AndGtw_source;
		this.nb_XorGtw_source = nb_XorGtw_source;
	}

	/**
	 * * Statistics of the initial change request this used is case of a DELETE or
	 * INSERT --> Nb affected nodes on the initiating partners
	 */
	public <E extends Edge<N>, N extends INode> Stats(Role role, ChgOpType t, IRPSTNode<E, N> f_source) {
		type = t;
		this.role = role;
		this.Nb_Nodes_source = FragmentUtil.collectNodes(f_source).size();
		this.nb_activ_source = FragmentUtil.collectActivities(f_source).size();
		this.nb_AndGtw_source = FragmentUtil.collectANDGateways(f_source).size();
		this.nb_XorGtw_source = FragmentUtil.collectXORGateways(f_source).size();
	}

	/**
	 * Statistics of the initial change request this used is case of a
	 * REPLACE(f1,f2) --> Nb affected nodes on the initiating partners = sum
	 */
	public <E extends Edge<N>, N extends INode> Stats(Role role, ChgOpType t, IRPSTNode<E, N> f_source1,
			IRPSTNode<E, N> f_source2) {

		type = t;
		this.role = role;
		if (f_source1 != null && f_source2 != null) {
			this.Nb_Nodes_source = FragmentUtil.collectNodes(f_source1).size()
					+ FragmentUtil.collectNodes(f_source2).size();
			this.nb_activ_source = FragmentUtil.collectActivities(f_source1).size()
					+ FragmentUtil.collectActivities(f_source2).size();
			this.nb_AndGtw_source = FragmentUtil.collectANDGateways(f_source1).size()
					+ FragmentUtil.collectANDGateways(f_source2).size();
			this.nb_XorGtw_source = FragmentUtil.collectXORGateways(f_source1).size()
					+ FragmentUtil.collectXORGateways(f_source2).size();
		}
	}

	/**
	 * Statistics of the change propagation algo this used is case of a DELETE or
	 * INSERT --> Nb affected nodes on the Target partners
	 */
	public <E extends Edge<N>, N extends INode> void updatePropagationStats(ChgOpType t, IRPSTNode<E, N> f_target,
			boolean isRootRequest) {

		if (f_target != null) {
			this.Nb_Nodes_target += FragmentUtil.collectNodes(f_target).size();
			this.nb_activ_target += FragmentUtil.collectActivities(f_target).size();
			this.nb_AndGtw_target += FragmentUtil.collectANDGateways(f_target).size();
			this.nb_XorGtw_target += FragmentUtil.collectXORGateways(f_target).size();
			this.nb_affected_partners++;
			if (t.equals(ChgOpType.Insert))
				this.nb_Insert_generated++;
			else if (t.equals(ChgOpType.Delete))
				this.nb_Delete_generated++;
		}
	}

	public <E extends Edge<N>, N extends INode> void updatePropagationStats(ChgOpType t, Set<N> nodes,
			boolean isRootRequest) {

		if (nodes != null) {
			this.Nb_Nodes_target += nodes.size();
			for (N node : nodes) {
				if (node instanceof InteractionActivity)
					this.nb_activ_target++;
				else if (node instanceof AndGateway)
					this.nb_AndGtw_target++;
				else if (node instanceof XorGateway)
					this.nb_XorGtw_target++;
			}
		}
	}

	/**
	 * Statistics of the change propagation algo this used is case of a DELETE -->
	 * Nb affected nodes on the Target partners
	 */
	public <E extends Edge<N>, N extends INode> void updatePropagationStats(ChgOpType t, IRpstModel<E, N> OriginalModel,
			IRpstModel<E, N> NewModel, boolean isRootRequest) {

		if (OriginalModel != null && NewModel != null) {
			if (!NewModel.isEmpty()) {
				this.Nb_Nodes_target += Math.abs(FragmentUtil.collectNodes(OriginalModel.getRoot()).size()
						- FragmentUtil.collectNodes(NewModel.getRoot()).size());
				this.nb_activ_target += Math.abs(FragmentUtil.collectActivities(OriginalModel.getRoot()).size()
						- FragmentUtil.collectActivities(NewModel.getRoot()).size());
				this.nb_AndGtw_target += Math.abs(FragmentUtil.collectANDGateways(OriginalModel.getRoot()).size()
						- FragmentUtil.collectANDGateways(NewModel.getRoot()).size());
				this.nb_XorGtw_target += Math.abs(FragmentUtil.collectXORGateways(OriginalModel.getRoot()).size()
						- FragmentUtil.collectXORGateways(NewModel.getRoot()).size());
			} else {
				this.Nb_Nodes_target += FragmentUtil.collectNodes(OriginalModel.getRoot()).size() - 2;
				this.nb_activ_target += FragmentUtil.collectActivities(OriginalModel.getRoot()).size();
				this.nb_AndGtw_target += FragmentUtil.collectANDGateways(OriginalModel.getRoot()).size();
				this.nb_XorGtw_target += FragmentUtil.collectXORGateways(OriginalModel.getRoot()).size();
			}
			this.nb_affected_partners++;
			if (t.equals(ChgOpType.Insert))
				this.nb_Insert_generated++;
			else if (t.equals(ChgOpType.Delete))
				this.nb_Delete_generated++;
		}
	}

	/**
	 * Statistics of the change propagation algo this used is case of a
	 * REPLACE(f1,f2) or Merge --> Nb affected nodes on the Target partners = sum
	 */
	public <E extends Edge<N>, N extends INode> void updatePropagationStats(IRPSTNode<E, N> f_target1,
			IRPSTNode<E, N> f_target2, boolean isRootRequest) {

		if (f_target1 != null && f_target2 != null) {
			this.Nb_Nodes_target += FragmentUtil.collectNodes(f_target1).size()
					+ FragmentUtil.collectNodes(f_target2).size() - 2;
			this.nb_activ_target += FragmentUtil.collectActivities(f_target1).size()
					+ FragmentUtil.collectActivities(f_target2).size();
			this.nb_AndGtw_target += FragmentUtil.collectANDGateways(f_target1).size()
					+ FragmentUtil.collectANDGateways(f_target2).size();
			this.nb_XorGtw_target += FragmentUtil.collectXORGateways(f_target1).size()
					+ FragmentUtil.collectXORGateways(f_target2).size();
			this.nb_affected_partners++;
			if (isRootRequest == false)
				this.nb_Replace_generated++;
		}
	}

	public String toString() {
		return "[" + type + " , " + Nb_Nodes_source + " , " + nb_activ_source + " , " + nb_AndGtw_source + " , "
				+ nb_XorGtw_source + " , " + Nb_Nodes_target + " , " + nb_activ_target + " , " + nb_AndGtw_target
				+ " , " + nb_XorGtw_target + " , " + nb_affected_partners + " , " + nb_Insert_generated + " , "
				+ nb_Replace_generated + " , " + nb_Delete_generated + "]";
	}

}
