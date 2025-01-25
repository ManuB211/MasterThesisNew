package at.ac.c3pro.tests;

import at.ac.c3pro.chormodel.MultiDirectedGraph;
import at.ac.c3pro.chormodel.PublicModel;
import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.node.*;
import org.jbpt.algo.tree.rpst.IRPSTNode;
import org.jbpt.utils.IOUtils;
import org.junit.Test;

public class PublicModelTest {

    @Test
    public void test() {
        MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> g = new MultiDirectedGraph<Edge<IPublicNode>, IPublicNode>();
        Role role1 = new Role("p1");
        Role role2 = new Role("p2");
        Role role3 = new Role("p3");
        Message m1 = new Message();
        Message m2 = new Message();
        Message m3 = new Message();
        Message m4 = new Message();

        IPublicNode s1 = new Send(role1, m1, "send1");
        IPublicNode r2 = new Receive(role2, m2, "receive1");
        IPublicNode r3 = new Receive(role3, m3, "receive2");
        IPublicNode s4 = new Send(role1, m4, "send2");
        IPublicNode g1 = new XorGateway("g1");
        IPublicNode g2 = new XorGateway("g2");
        IPublicNode e1 = new Event("start");
        IPublicNode e2 = new Event("end");

        g.addEdge(e1, s1);
        g.addEdge(s1, g1);
        g.addEdge(g1, r2);
        g.addEdge(g1, r3);
        g.addEdge(r2, g2);
        g.addEdge(r3, g2);
        g.addEdge(g2, s4);
        g.addEdge(s4, e2);

        PublicModel Pu1 = new PublicModel(g, "Pu1");
        // M1.addEdge(g, e1, i1);
        // RPST<Edge<IChoreographyNode>, IChoreographyNode> rpst = new
        // RPST<Edge<IChoreographyNode>,IChoreographyNode>(g);
        // IOUtils.toFile("rpst.dot", rpst.toDOT());
        IOUtils.toFile("Pu1.dot", Pu1.toDOT());

        for (IRPSTNode<Edge<IPublicNode>, IPublicNode> node : Pu1.getRPSTNodes()) {
            System.out.print(node.getName() + ": ");
            for (IRPSTNode<Edge<IPublicNode>, IPublicNode> child : Pu1.getPolygonChildren(node)) {
                System.out.print(child.getName() + " ");
            }
            System.out.println();
        }
    }

}
