package at.ac.c3pro.ImpactAnalysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.MapTransformer;

import at.ac.c3pro.chormodel.IChoreographyModel;
import at.ac.c3pro.chormodel.IRole;
import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.INode;
import at.ac.c3pro.node.Interaction;
import at.ac.c3pro.node.Node;
import at.ac.c3pro.util.FragmentUtil;
import edu.uci.ics.jung.algorithms.scoring.EigenvectorCentrality;
import edu.uci.ics.jung.graph.Hypergraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class ImpactAnalysisUtil {

	public static Map<Pair<IRole, IRole>, Double> StaticImpact(Map<Pair<IRole, IRole>, Double> connectivityMap,
			Map<IPartnerNode, Double> centrality) {
		Map<Pair<IRole, IRole>, Double> staticImpact = new HashMap<Pair<IRole, IRole>, Double>();

		for (Pair<IRole, IRole> pair : connectivityMap.keySet()) {
			for (IPartnerNode pnode : centrality.keySet()) {
				if (pnode.getRole() == pair.second) {
					staticImpact.put(pair, connectivityMap.get(pair)); // *centrality.get(pnode)
					System.out.println(pair.first + "-->" + pair.second + ":  " + staticImpact.get(pair));
				}
				// to add wheights + risk compliance costraints...Nb of activities...
			}
		}
		return staticImpact;
	}

	/**
	 * This function returns the centrality of each partner in the choerography,
	 * according to the eivenVector algorithms
	 * 
	 * @param choreo
	 * @param connectivityMap
	 * @return
	 */
	public static Map<IPartnerNode, Double> ComputeCentrality(IChoreographyModel choreo,
			Map<Pair<IRole, IRole>, Double> connectivityMap) {

		Map<IPEdge, Double> edgeWeights = new HashMap<IPEdge, Double>();
		// Map<Pair<IRole, IRole>,Double> connectivityMap = ComputeConnectivity(choreo);
		Hypergraph<IPartnerNode, IPEdge> graph = new UndirectedSparseGraph<IPartnerNode, IPEdge>();
		Map<IPartnerNode, Double> centrality = new HashMap<IPartnerNode, Double>();

		Set<IRole> roles = new HashSet<IRole>();
		Set<PartnerNode> partnernodes = new HashSet<PartnerNode>();

		for (Pair<IRole, IRole> pair : connectivityMap.keySet()) {
			roles.add(pair.first);
			roles.add(pair.second);
		}
		for (IRole role : roles) {
			partnernodes.add(new PartnerNode(role));
		}
		for (Pair<IRole, IRole> pair : connectivityMap.keySet()) {
			PartnerNode p1 = null;
			PartnerNode p2 = null;
			for (PartnerNode pnode : partnernodes) {
				if (pnode.partner == pair.first)
					p1 = pnode;
				else if (pnode.partner == pair.second)
					p2 = pnode;
			}
			PEdge e = new PEdge(p1, p2);

			((UndirectedSparseGraph<IPartnerNode, IPEdge>) graph).addEdge(e, p1, p2);
			edgeWeights.put(e, connectivityMap.get(pair));
		}
		Transformer<IPEdge, Double> edge_weights = MapTransformer.getInstance(edgeWeights);
		EigenvectorCentrality<IPartnerNode, IPEdge> EVC = new EigenvectorCentrality<IPartnerNode, IPEdge>(graph,
				edge_weights);
		EVC.setMaxIterations(20);
		EVC.setTolerance(1);
		EVC.evaluate();

		for (PartnerNode pnode : partnernodes) {
			centrality.put(pnode, EVC.getVertexScore(pnode));
			System.out.println("score of " + pnode.partner.toString() + " = " + EVC.getVertexScore(pnode));
		}
		return centrality;

	}

	/**
	 * This method computes the dependency matrix between all partners. This is
	 * based on the number of interactions and normalized through the total number
	 * of interactions of each partner
	 * 
	 * @param choreo
	 * @return
	 */
	public static Map<Pair<IRole, IRole>, Double> ComputeConnectivity(IChoreographyModel choreo) {
		Map<Pair<IRole, IRole>, Double> ConnectivityMap = new HashMap<Pair<IRole, IRole>, Double>();
		Map<IRole, Integer> role2NbInteractions = new HashMap<IRole, Integer>();
		int NbInteractions = 0;
		// totalMessagesSize = 0;
		// int totalMessagesImportance = 0;

		Set<Role> roles = FragmentUtil.extractRoles(choreo.getRoot());
		for (IRole r : roles)
			role2NbInteractions.put(r, 0);

		for (IRole r1 : roles)
			for (IRole r2 : roles)
				if (r1 != r2)
					ConnectivityMap.put(new Pair<IRole, IRole>(r1, r2), 0.0);

		for (IChoreographyNode node : choreo.getdigraph().getVertices()) {
			if (choreo.isActivity(node)) { // is an Interaction
				NbInteractions++;
				role2NbInteractions.put(((Interaction) node).getParticipant1(),
						role2NbInteractions.get(((Interaction) node).getParticipant1()) + 1);
				role2NbInteractions.put(((Interaction) node).getParticipant2(),
						role2NbInteractions.get(((Interaction) node).getParticipant2()) + 1);
				// totalMessagesSize += ((Interaction) node).getMessage().size;
				// totalMessagesImportance += ((Interaction) node).getMessage().importanceLevel;

				for (Pair<IRole, IRole> p : ConnectivityMap.keySet()) {
					if ((p.first == ((Interaction) node).getParticipant1() && p.second == ((Interaction) node).getParticipant2())
							|| (p.second == ((Interaction) node).getParticipant1()
									&& p.first == ((Interaction) node).getParticipant2())) {
						double newValue = ConnectivityMap.get(p) + 1; // TODO weight by importance of message
						ConnectivityMap.put(p, newValue);
					}

				}
			}
		}

		System.out.println(role2NbInteractions);

		// Normalization method 1
		/*
		 * if(NbInteractions > 0){ for(Pair<IRole, IRole> p : ConnectivityMap.keySet()){
		 * double normalizedValue = ConnectivityMap.get(p) /NbInteractions;//
		 * NbInteractions; //TODO add messagesize..importance.. normalization
		 * ConnectivityMap.put(p, normalizedValue); } }
		 */

		/*
		 * for(Pair<IRole, IRole> p : ConnectivityMap.keySet()){
		 * System.out.println("< "+p.first+ ","+p.second + " > -->  " +
		 * ConnectivityMap.get(p)); }
		 */
		// Normalization method 2
		for (Pair<IRole, IRole> p : ConnectivityMap.keySet()) {
			double normalizedValue = ConnectivityMap.get(p) / role2NbInteractions.get(p.first);// NbInteractions; //TODO
																								// add
																								// messagesize..importance..
																								// normalization
			ConnectivityMap.put(p, normalizedValue);
		}

		return ConnectivityMap;
	}

	public interface IPartnerNode extends INode {
		public IRole getRole();

	}

	public interface IPEdge {
		public PartnerNode getsource();

		public PartnerNode gettarget();

	}

	public static class PEdge implements IPEdge {
		public PartnerNode source;
		public PartnerNode target;
		public Integer id;
		public double weight;

		public PEdge(PartnerNode source, PartnerNode target) {
			this.source = source;
			this.target = target;
			// this.id = EdgeID++;
		}

		public PEdge(PartnerNode source, PartnerNode target, double weight) {
			this.source = source;
			this.target = target;
			this.weight = weight;
		}

		public PartnerNode getsource() {
			return this.source;
		}

		public PartnerNode gettarget() {
			return this.target;
		}

	}

	public static class PartnerNode extends Node implements IPartnerNode {
		public IRole partner;

		public PartnerNode(IRole role) {
			super(role.toString());
			this.partner = role;
		}

		public IRole getRole() {
			return this.partner;
		}

	}

	public static class Pair<obj1 extends Object, obj2 extends Object> {
		public obj1 first;
		public obj2 second;

		public Pair(obj1 o1, obj2 o2) {
			this.first = o1;
			this.second = o2;
		}

		public Pair() {
		}

		public boolean equals(Pair<obj1, obj2> p) {
			return ((p.first == this.first) && (p.second == this.second));
		}

	}

}
