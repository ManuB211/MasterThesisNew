package at.ac.c3pro.tests;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.jbpt.algo.tree.rpst.IRPSTNode;
import org.jbpt.algo.tree.rpst.RPST;
import org.jbpt.utils.IOUtils;
import org.junit.Test;

import at.ac.c3pro.chormodel.MultiDirectedGraph;
import at.ac.c3pro.chormodel.PrivateModel;
import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.Event;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.IPrivateNode;
import at.ac.c3pro.node.Message;
import at.ac.c3pro.node.PrivateActivity;
import at.ac.c3pro.node.PrivateNode;
import at.ac.c3pro.node.XorGateway;

public class PrivateModelTest {

	@Test
	public void test() {
		MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode> g = new MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode>();
		Role r1 = new Role("p1");
		Role r2 = new Role("p2");
		Message m1 = new Message();
		PrivateNode a1 = new PrivateActivity("a1");
		PrivateNode a2 = new PrivateActivity(("a2"));
		PrivateNode a3 = new PrivateActivity("a3");
		PrivateNode a4 = new PrivateActivity("a4");
		IPrivateNode g1 = new XorGateway("g1");
		IPrivateNode g2 = new XorGateway("g2");
		IPrivateNode e1 = new Event("start");
		IPrivateNode e2 = new Event("end");

		g.addEdge(e1, a1);
		g.addEdge(a1, g1);
		g.addEdge(g1, a2);
		g.addEdge(g1, a3);
		g.addEdge(a2, g2);
		g.addEdge(a3, g2);
		g.addEdge(g2, a4);
		g.addEdge(a4, e2);

		PrivateModel Pr1 = new PrivateModel(g, "Pr1");
		// M1.addEdge(g, e1, i1);
		// RPST<Edge<IChoreographyNode>, IChoreographyNode> rpst = new
		// RPST<Edge<IChoreographyNode>,IChoreographyNode>(g);
		// IOUtils.toFile("rpst.dot", rpst.toDOT());
		IOUtils.toFile("Pr1.dot", Pr1.toDOT());

		for (IRPSTNode<Edge<IPrivateNode>, IPrivateNode> node : Pr1.getRPSTNodes()) {
			System.out.print(node.getName() + ": ");
			for (IRPSTNode<Edge<IPrivateNode>, IPrivateNode> child : Pr1.getPolygonChildren(node)) {
				System.out.print(child.getName() + " ");
			}
			System.out.println();
		}

		/*
		 * performBasicChecks1(g,M1); assertEquals(12,M1.getRPSTNodes().size());
		 * assertEquals(8,M1.getRPSTNodes(TCType.TRIVIAL).size());
		 * assertEquals(1,M1.getRPSTNodes(TCType.BOND).size());
		 * assertEquals(3,M1.getRPSTNodes(TCType.POLYGON).size());
		 * assertEquals(0,M1.getRPSTNodes(TCType.RIGID).size());
		 * 
		 * //assertTrue(M1.isRoot(M1.getRPSTNodes(TCType.POLYGON).iterator().next()));
		 * assertEquals("g1",M1.getRPSTNodes(TCType.POLYGON).iterator().next().getEntry(
		 * ).getName());
		 * assertEquals("g2",M1.getRPSTNodes(TCType.POLYGON).iterator().next().getExit()
		 * .getName());
		 * assertEquals(2,M1.getRPSTNodes(TCType.POLYGON).iterator().next().getFragment(
		 * ).size());
		 * 
		 * assertEquals("start",M1.getRPSTNodes(TCType.RIGID).iterator().next().getEntry
		 * ().getName());
		 * assertEquals("end",M1.getRPSTNodes(TCType.RIGID).iterator().next().getExit().
		 * getName());
		 * assertEquals(5,M1.getRPSTNodes(TCType.RIGID).iterator().next().getFragment().
		 * size()); System.out.println(
		 * "-----------------------------------------------------------------------");
		 */
	}

	private void performBasicChecks1(MultiDirectedGraph g, RPST<Edge<IChoreographyNode>, IChoreographyNode> rpst) {
		for (IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode> node : rpst.getRPSTNodes()) {
			assertTrue(g.getEdges().containsAll(node.getFragment()));

			Collection<Edge<IChoreographyNode>> edges = new ArrayList<Edge<IChoreographyNode>>();
			for (IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode> child : rpst.getChildren(node)) {
				edges.addAll(child.getFragment());
			}

			assertTrue(node.getFragment().containsAll(edges));
		}
	}
}
