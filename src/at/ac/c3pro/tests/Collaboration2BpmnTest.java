package at.ac.c3pro.tests;

import org.jbpt.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;

import at.ac.c3pro.chormodel.IPublicModel;
import at.ac.c3pro.io.Bpmn2Collaboration;
import at.ac.c3pro.io.Collaboration2Bpmn;

public class Collaboration2BpmnTest {

	private Bpmn2Collaboration BookTripCollab;

	@Before
	public void initChoreography() throws Exception {
		BookTripCollab = new Bpmn2Collaboration("target/collaborationV1.xml", "CollaborationBookTrip");
	}

	@Test
	public void test() throws Exception {

		Collaboration2Bpmn collab2bpmnIO = new Collaboration2Bpmn(BookTripCollab.collab, "test", "target/");
		collab2bpmnIO.buildXML();

		for (IPublicModel puModel : BookTripCollab.collab.puModels) {
			System.out.println(puModel.getName());
			IOUtils.toFile("autogen_pum_" + puModel.getName() + "_original.dot", puModel.getdigraph().toDOT());
		}

		Bpmn2Collaboration reversedCollab = new Bpmn2Collaboration("target/collab2BPMNtest.xml", "ReversedCollab");

		for (IPublicModel puModel : reversedCollab.collab.puModels) {
			System.out.println(puModel.getName());
			IOUtils.toFile("autogen_pum_" + puModel.getName() + "_generated.dot", puModel.getdigraph().toDOT());
		}

	}

}
