package at.ac.c3pro.tests;

import java.util.Iterator;

import org.jbpt.algo.tree.rpst.IRPSTNode;
import org.jbpt.utils.IOUtils;
import org.junit.Test;

import at.ac.c3pro.chormodel.PrivateModel;
import at.ac.c3pro.io.Bpmn2PrivateModel;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.IPrivateNode;

public class Bpmn2PrivateModelTest {

	@Test
	public void test() throws Exception {
		Bpmn2PrivateModel example = new Bpmn2PrivateModel("target/AcquirerPrivateProcess.xml", "PrivateAcquirerModel");
		IOUtils.toFile(example.privateModel.getName() + "Bpmn2PrivateModelTest.dot",
				example.privateModel.getdigraph().toDOT());
		System.out.println(example.PrivateNode2PublicNodeIDMap);

		Bpmn2PrivateModel example2 = new Bpmn2PrivateModel("target/MultipleLoops-1.bpmn.xml", "LoopModel");
		IOUtils.toFile("Loopgraph.dot", example2.privateModel.getdigraph().toDOT());
		System.out.println(example2.privateModel);
		IOUtils.toFile("Looprpst.dot", (example2.privateModel).toDOT());
		Iterator<IRPSTNode<Edge<IPrivateNode>, IPrivateNode>> I = ((PrivateModel) example2.privateModel).Loops
				.iterator();

		// Testing the Loops and printing the nodes in each loop

		while (I.hasNext()) {
			System.out.println("Loops:   " + example2.privateModel.getNodes(I.next()));
		}

		for (IPrivateNode pnode : example2.privateModel.getdigraph().getVertices()) {
			System.out.println(pnode + "  " + example2.privateModel.getLoopsOfNode(pnode).size());
		}

		// System.out.println("Loops: "example2.privateModel.Loops); ;
		// System.out.println(example2.PrivateNode2PublicNodeIDMap);

	}
}
