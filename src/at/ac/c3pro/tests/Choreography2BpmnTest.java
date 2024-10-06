package at.ac.c3pro.tests;

import org.jbpt.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;

import at.ac.c3pro.chormodel.ChoreographyModel;
import at.ac.c3pro.chormodel.MultiDirectedGraph;
import at.ac.c3pro.io.Bpmn2ChoreographyModel;
import at.ac.c3pro.io.ChoreographyModel2Bpmn;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.IChoreographyNode;

public class Choreography2BpmnTest {

	private Bpmn2ChoreographyModel BookTripChoreoModel;

	@Before
	public void initChoreography() throws Exception {

		BookTripChoreoModel = new Bpmn2ChoreographyModel("target/Book_Trip_OperationExtended.xml",
				"BookTripOperationExtended");

	}

	@Test
	public void test() throws Exception {

		MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> digraph = (MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode>) BookTripChoreoModel.choreoModel
				.getdigraph();
		ChoreographyModel choreoModel = new ChoreographyModel(digraph);
		ChoreographyModel2Bpmn choreo2bpmnIO = new ChoreographyModel2Bpmn(choreoModel,
				"BookTripOperationExtended_Reversed", "target/");

		choreo2bpmnIO.buildXML();

		IOUtils.toFile("chor_original.dot", digraph.toDOT());

		Bpmn2ChoreographyModel genModel = new Bpmn2ChoreographyModel("target/choreo2BPMNtest.xml", "GeneratedBpmnFile");

		MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> genGraph = (MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode>) genModel.choreoModel
				.getdigraph();

		IOUtils.toFile("chor_generated.dot", genGraph.toDOT());

		System.out.println(genGraph.compareTo(digraph));

	}

}
