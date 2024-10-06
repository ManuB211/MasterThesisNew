package at.ac.c3pro.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import at.ac.c3pro.node.Gateway;
import at.ac.c3pro.node.Node;
import at.ac.c3pro.node.XorGateway;

public class NodeTests {

	@Test
	public void test() {
		Node n = new XorGateway();
		System.out.println(n.getClass().getSimpleName());
	}

}
