package at.ac.c3pro.tests;

import static org.junit.Assert.*;

import org.jbpt.utils.IOUtils;
import org.junit.Test;

import at.ac.c3pro.chormodel.ChoreographyModel;
import at.ac.c3pro.chormodel.MultiDirectedGraph;
import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.Event;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.Interaction;
import at.ac.c3pro.node.Message;
import at.ac.c3pro.node.XorGateway;

public class Rpsttest2 {

	@Test
	public void test() {
		MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> newGraph = new MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode>();
		
		Role r1 = new Role("participant 1", "123");
		Role r2 = new Role("participant 2", "124");
		Role r3 = new Role("participant 3", "125");
		
		Message m1 = new Message("message 1", "234");
		Message m2 = new Message("message 2", "235");
		Message m3 = new Message("message 3", "236");
		Message m4 = new Message("message 4", "237");
		
		Interaction i1 = new Interaction("interaction 1", "345", r1, r2, m1);
		Interaction i2 = new Interaction("interaction 2", "346", r2, r3, m2);
		Interaction i3 = new Interaction("interaction 3", "347", r3, r2, m3);
		Interaction i4 = new Interaction("interaction 4", "348", r2, r1, m4);
		Interaction i5 = new Interaction("interaction 5", "349", r2, r1, m4);
		Interaction i6 = new Interaction("interaction 6", "3410", r2, r1, m4);
		Interaction i7 = new Interaction("interaction 7", "3411", r2, r1, m4);
		
		XorGateway xor1 = new XorGateway("XOR1");
		XorGateway xor2 = new XorGateway("XOR2");
		XorGateway xor3 = new XorGateway("XOR3");
		XorGateway xor4 = new XorGateway("XOR4");
		XorGateway xor5 = new XorGateway("XOR5");
		XorGateway xor11 = new XorGateway("XOR11");
		XorGateway xor22 = new XorGateway("XOR22");
		XorGateway xor33 = new XorGateway("XOR33");
		XorGateway xor43 = new XorGateway("XOR44");
		
		Event start = new Event("start");
		Event end = new Event("end");
		
		newGraph.addEdge(start, xor1);
		newGraph.addEdge(xor1,xor2);
		newGraph.addEdge(xor1,xor3);
		newGraph.addEdge(xor2,xor4);
		
		newGraph.addEdge(xor3,xor5);
		newGraph.addEdge(xor5,end);
		
		ChoreographyModel chormodel = new ChoreographyModel(newGraph);
		
		IOUtils.toFile("rpsttest.dot", chormodel.getdigraph().toDOT());
		
	}

}
