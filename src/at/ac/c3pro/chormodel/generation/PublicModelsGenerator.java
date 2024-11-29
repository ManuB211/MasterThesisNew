package at.ac.c3pro.chormodel.generation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import at.ac.c3pro.chormodel.Collaboration;
import at.ac.c3pro.chormodel.MultiDirectedGraph;
import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.Event;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.INode;
import at.ac.c3pro.node.IPublicNode;
import at.ac.c3pro.node.Interaction;
import at.ac.c3pro.node.Message;
import at.ac.c3pro.node.Receive;
import at.ac.c3pro.node.Send;

public class PublicModelsGenerator {

	private SplitTracking splitTracking = null;
	private Collaboration collab = null;
	private MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> chorModel = new MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode>();
	private HashMap<Role, MultiDirectedGraph<Edge<IPublicNode>, IPublicNode>> role2PuModel = new HashMap<Role, MultiDirectedGraph<Edge<IPublicNode>, IPublicNode>>();

	public PublicModelsGenerator(List<Role> roles,
			MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> chorModel) {
		this.splitTracking = SplitTracking.getInstance();
		this.collab = new Collaboration("test");
		this.chorModel = chorModel;
		collab.roles = new HashSet<Role>(roles);
	}

	public void generate() {
		for (Role role : collab.roles) {
			MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> diGraph = new MultiDirectedGraph<Edge<IPublicNode>, IPublicNode>();
			// IPublicModel puModel = new PublicModel(diGraph, role.name);
			// collab.puModels.add(puModel);
			// collab.R2PuM.put(role, puModel);
			// collab.PuM2R.put(puModel, role);
			Event start = new Event("start");
			diGraph.addVertex(start);
			role2PuModel.put(role, diGraph);
		}

		IChoreographyNode start = splitTracking.getMainBranch().getSplit().getSplitNode();
		Collection<IChoreographyNode> successors = chorModel.getDirectSuccessors(start);

		while (!successors.isEmpty()) {
			for (INode successor : successors) {
				if (successor instanceof Interaction) {
					addInteraction((Interaction) successor);

				}
			}
		}

	}

	private void addInteraction(Interaction ia) {
		Role sender = ia.getParticipant1();
		Role receiver = ia.getParticipant2();
		Message msg = ia.getMessage();

		Send sendNode = new Send(receiver, msg, ia.getName());
		Receive receiveNode = new Receive(sender, msg, ia.getName());
	}

}
