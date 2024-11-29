package at.ac.c3pro.chormodel.generation;

//import java.security.acl.LastOwnerException;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import org.jbpt.algo.tree.rpst.IRPSTNode;
import org.jbpt.graph.abs.IDirectedGraph;
import org.jbpt.utils.IOUtils;

import at.ac.c3pro.chormodel.MultiDirectedGraph;
import at.ac.c3pro.chormodel.PrivateModel;
import at.ac.c3pro.chormodel.RpstModel;
import at.ac.c3pro.node.AndGateway;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.Event;
import at.ac.c3pro.node.Gateway;
import at.ac.c3pro.node.INode;
import at.ac.c3pro.node.IPrivateNode;
import at.ac.c3pro.node.InteractionActivity;
import at.ac.c3pro.node.PrivateActivity;
import at.ac.c3pro.node.Receive;
import at.ac.c3pro.node.Send;
import at.ac.c3pro.node.XorGateway;

public class FragmentGenerator {

	private enum FragmentType {
		SEQ, XOR, AND;

		public static FragmentType getRandomFragmentType() {
			Random random = new Random();
			return values()[random.nextInt(values().length)];
		}
	}

	private PrivateModel privateModel;
	private int iaCount;
	private ArrayList<IPrivateNode> nodes = new ArrayList<IPrivateNode>();
	private static final int MAX_ACTIVITIES = 5;
	private static final int MAX_BRANCHES = 3;
	private ArrayList<PrivateActivity> prActivities = new ArrayList<PrivateActivity>();

	// For ensuring uniform naming scheme
	String formattedDate;

	public FragmentGenerator(PrivateModel prModel, String formattedDate) {
		this.privateModel = prModel;
		// iaCount = determineInteractionCount(prModel);
		this.formattedDate = formattedDate;
	}

	public PrivateModel enhance() {
		int i = 0;
		RpstModel<Edge<IPrivateNode>, IPrivateNode> afterModel = null;

		IDirectedGraph<Edge<IPrivateNode>, IPrivateNode> digraph = privateModel.getdigraph();

		// TODO: TF is this -> only logging, do i need this
		for (IRPSTNode<Edge<IPrivateNode>, IPrivateNode> v : privateModel.getVertices()) {
			if (v.getEntry() instanceof Send || v.getExit() instanceof Send) {
				System.out.println("Send:" + v.getEntry().getName());
			} else if (v instanceof Receive) {
				// TODO: TF is this -> not even possible?
				System.out.println("ay");
			}
//			System.out.println("Entry: " + v.getEntry().getName() + " - Exit: " + v.getExit().getName());
		}

		for (IPrivateNode node : privateModel.getdigraph().getVertices()) {
			System.out.println(node.getName());
			if (node instanceof Send || node instanceof Receive)
				nodes.add(node);
		}

		System.out.println(nodes.size());

		IPrivateNode randomNode = nodes.get(randomInteger(0, nodes.size() - 1));

		privateModel = addFragment(randomNode);

		IOUtils.toFile(formattedDate + "/Insert_autogen_" + i + ".dot", privateModel.getdigraph().toDOT());

		return privateModel;
	}

	/**
	 * Depending on the node given, a so called fragment is inserted.
	 * 
	 * If the given node is a Send, the fragment is inserted between the predecessor
	 * and the given node If the given node is a Receive, the fragment is inserted
	 * between the given node and its successor
	 * 
	 * Therefore it is already safe considering the Handover of Work operator, since
	 * the fragment can only be inserted between the receive of the HOW and its
	 * successor
	 * 
	 */

	private PrivateModel addFragment(IPrivateNode node) {
		PrivateModel enhancedModel = null;
		if (node instanceof Send) {
			ArrayList<IPrivateNode> predecessors = new ArrayList<IPrivateNode>(
					privateModel.getdigraph().getDirectPredecessors(node));
			if (predecessors.size() > 1) {
				System.out.println("something is fishy");
			}

			if (predecessors.get(0) instanceof Event || predecessors.get(0) instanceof Gateway
					|| predecessors.get(0) instanceof Receive) {
				Edge<IPrivateNode> position = privateModel.getdigraph().getEdge(node, predecessors.get(0));

				RpstModel<Edge<IPrivateNode>, IPrivateNode> fragment = getRandomFragment();

				enhancedModel = (PrivateModel) privateModel.insert(fragment.getRoot(), position);
				position = enhancedModel.getdigraph().getEdge(node, predecessors.get(0));

				enhancedModel.getdigraph().removeEdge(position);

			}

		} else if (node instanceof Receive) {
			ArrayList<IPrivateNode> successors = new ArrayList<IPrivateNode>(
					privateModel.getdigraph().getDirectSuccessors(node));
			if (successors.size() > 1) {
				System.out.println("something is fishy");
			}

			Edge<IPrivateNode> position = privateModel.getdigraph().getEdge(node, successors.get(0));

			RpstModel<Edge<IPrivateNode>, IPrivateNode> fragment = getRandomFragment();

			enhancedModel = (PrivateModel) privateModel.insert(fragment.getRoot(), position);

			position = enhancedModel.getdigraph().getEdge(node, successors.get(0));

			enhancedModel.getdigraph().removeEdge(position);

		}

		return enhancedModel;
	}

	private RpstModel<Edge<IPrivateNode>, IPrivateNode> getRandomFragment() {
		MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode> fragment = new MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode>();

		FragmentType fragType = FragmentType.getRandomFragmentType();

		switch (fragType) {
		case SEQ:
			fragment = generateActivityFragment();
			break;
		case XOR:
			fragment = generateXorFragment();
			break;
		case AND:
			fragment = generateAndFragment();
			break;

		default:
			break;
		}

		RpstModel<Edge<IPrivateNode>, IPrivateNode> rpstFragment = new PrivateModel(fragment, "test");

		return rpstFragment;
	}

	private MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode> generateXorFragment() {
		MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode> newFragmentGraph = new MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode>();

		Event start = new Event("start");
		Event end = new Event("end");

		ArrayList<PrivateActivity> fragActivities = new ArrayList<PrivateActivity>();

		int activityCount = randomInteger(2, MAX_ACTIVITIES);

		XorGateway split = new XorGateway("PXOR", UUID.randomUUID().toString());
		XorGateway merge = new XorGateway("PXOR_m", UUID.randomUUID().toString());

		PrivateActivity nextActivity = null;
		PrivateActivity preActivity = null;

		int remainingAct = activityCount;

		for (int i = 0; i < activityCount; i++) {
			nextActivity = new PrivateActivity("PA_" + i, UUID.randomUUID().toString());
			fragActivities.add(nextActivity);
			if (i == 0) {
				newFragmentGraph.addEdge(split, nextActivity);
			} else if (remainingAct == 1) { // branch switch
				newFragmentGraph.addEdge(preActivity, merge);
				newFragmentGraph.addEdge(split, nextActivity);
			} else {
				newFragmentGraph.addEdge(preActivity, nextActivity);
			}

			preActivity = nextActivity;
			remainingAct--;
		}

		newFragmentGraph.addEdge(preActivity, merge);

		newFragmentGraph.addEdge(start, split);
		newFragmentGraph.addEdge(merge, end);

		return newFragmentGraph;

	}

	private MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode> generateAndFragment() {
		MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode> newFragmentGraph = new MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode>();

		Event start = new Event("start");
		Event end = new Event("end");

		ArrayList<PrivateActivity> fragActivities = new ArrayList<PrivateActivity>();

		int activityCount = randomInteger(2, MAX_ACTIVITIES);

		AndGateway split = new AndGateway("PAND", UUID.randomUUID().toString());
		AndGateway merge = new AndGateway("PAND_m", UUID.randomUUID().toString());

		PrivateActivity nextActivity = null;
		PrivateActivity preActivity = null;

		int remainingAct = activityCount;

		for (int i = 0; i < activityCount; i++) {
			nextActivity = new PrivateActivity("PA_" + i, UUID.randomUUID().toString());
			fragActivities.add(nextActivity);
			if (i == 0) {
				newFragmentGraph.addEdge(split, nextActivity);
			} else if (remainingAct == 1) { // branch switch
				newFragmentGraph.addEdge(preActivity, merge);
				newFragmentGraph.addEdge(split, nextActivity);
			} else {
				newFragmentGraph.addEdge(preActivity, nextActivity);
			}

			preActivity = nextActivity;
			remainingAct--;
		}

		newFragmentGraph.addEdge(preActivity, merge);

		newFragmentGraph.addEdge(start, split);
		newFragmentGraph.addEdge(merge, end);

		return newFragmentGraph;
	}

	private MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode> generateActivityFragment() {
		MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode> newFragmentGraph = new MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode>();

		Event start = new Event("start");
		Event end = new Event("end");

		ArrayList<PrivateActivity> fragActivities = new ArrayList<PrivateActivity>();

		int activityCount = randomInteger(1, MAX_ACTIVITIES);

		PrivateActivity nextActivity = null;
		PrivateActivity preActivity = null;

		for (int i = 0; i < activityCount; i++) {
			nextActivity = new PrivateActivity("PA_" + i, UUID.randomUUID().toString());
			fragActivities.add(nextActivity);
			if (i != 0) {
				newFragmentGraph.addEdge(preActivity, nextActivity);
			}
			preActivity = nextActivity;
		}

		newFragmentGraph.addEdge(start, fragActivities.get(0));
		newFragmentGraph.addEdge(nextActivity, end);

		return newFragmentGraph;

	}

	private int determineInteractionCount(PrivateModel prModel) {
		int interactionCount = 0;

		for (INode node : prModel.getdigraph().getVertices()) {
			if (node instanceof InteractionActivity) {
				interactionCount++;
			}
		}

		return interactionCount;
	}

	private int randomInteger(int min, int max) {

		Random r = new Random();
		return r.nextInt((max - min) + 1) + min;
	}

}
