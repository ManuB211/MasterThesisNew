package at.ac.c3pro.tests;

import at.ac.c3pro.chormodel.ChoreographyModel;
import at.ac.c3pro.chormodel.MultiDirectedGraph;
import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.node.*;
import org.jbpt.algo.tree.rpst.IRPSTNode;
import org.jbpt.algo.tree.rpst.RPST;
import org.jbpt.graph.DirectedEdge;
import org.jbpt.hypergraph.abs.Vertex;
import org.jbpt.utils.IOUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertTrue;

public class ChoreographyModelTest {
    enum Name {start, end}

    @Test
    public void test() {
        MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> g = new MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode>();
        Role r1 = new Role("p1");
        Role r2 = new Role("p2");
        Message m1 = new Message();
        ChoreographyNode i1 = new Interaction("I1", r1, r2, m1);
        ChoreographyNode i2 = new Interaction("I2", r1, r2, m1);
        ChoreographyNode i3 = new Interaction("I3", r1, r2, m1);
        ChoreographyNode i4 = new Interaction("I4", r1, r2, m1);
        IChoreographyNode g1 = new XorGateway("g1");
        IChoreographyNode g2 = new XorGateway("g2");
        IChoreographyNode e1 = new Event("start");
        IChoreographyNode e2 = new Event("end");

        g.addEdge(e1, i1);
        g.addEdge(i1, g1);
        g.addEdge(g1, i2);
        g.addEdge(g1, i3);
        g.addEdge(i2, g2);
        g.addEdge(i3, g2);
        g.addEdge(g2, i4);
        g.addEdge(i4, e2);
        //a comment test

        ChoreographyModel M1 = new ChoreographyModel(g);// g is the original graph and M1 is the RPST of g -- Also to access the original graph
        //you can use M1.diGraph

        // M1.addEdge(g, e1, i1);
        //RPST<Edge<IChoreographyNode>, IChoreographyNode> rpst = new RPST<Edge<IChoreographyNode>,IChoreographyNode>(g);
        //IOUtils.toFile("rpst.dot", rpst.toDOT());
        IOUtils.toFile("M11.dot", M1.toDOT());
		
		
		/*for (IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode> node : M1.getRPSTNodes()) {
			System.out.print(node.getName() + ": ");			
			for (IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode> child : M1.getPolygonChildren(node)) {
				System.out.print(child.getName() + " ");	
			}
			System.out.println();
		}*/

        //M1.getMap();
		
		/*performBasicChecks1(g,M1);
		assertEquals(12,M1.getRPSTNodes().size());
		assertEquals(8,M1.getRPSTNodes(TCType.TRIVIAL).size());
		assertEquals(1,M1.getRPSTNodes(TCType.BOND).size());
		assertEquals(3,M1.getRPSTNodes(TCType.POLYGON).size());
		assertEquals(0,M1.getRPSTNodes(TCType.RIGID).size());*/

        //assertTrue(M1.isRoot(M1.getRPSTNodes(TCType.POLYGON).iterator().next()));
		/*assertEquals("g1",M1.getRPSTNodes(TCType.POLYGON).iterator().next().getEntry().getName());
		assertEquals("g2",M1.getRPSTNodes(TCType.POLYGON).iterator().next().getExit().getName());
		assertEquals(2,M1.getRPSTNodes(TCType.POLYGON).iterator().next().getFragment().size());
		*/
		/*assertEquals("start",M1.getRPSTNodes(TCType.RIGID).iterator().next().getEntry().getName());
		assertEquals("end",M1.getRPSTNodes(TCType.RIGID).iterator().next().getExit().getName());
		assertEquals(5,M1.getRPSTNodes(TCType.RIGID).iterator().next().getFragment().size());*/
        System.out.println("-----------------------------------------------------------------------");
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

    private void performBasicChecks(MultiDirectedGraph g, RPST<DirectedEdge, Vertex> rpst) {
        for (IRPSTNode<DirectedEdge, Vertex> node : rpst.getRPSTNodes()) {
            assertTrue(g.getEdges().containsAll(node.getFragment()));

            Collection<DirectedEdge> edges = new ArrayList<DirectedEdge>();
            for (IRPSTNode<DirectedEdge, Vertex> child : rpst.getChildren(node)) {
                edges.addAll(child.getFragment());
            }

            assertTrue(node.getFragment().containsAll(edges));
        }
    }
}
