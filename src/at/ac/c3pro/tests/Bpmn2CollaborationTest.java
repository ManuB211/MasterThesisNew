package at.ac.c3pro.tests;

import static org.junit.Assert.*;

import org.jbpt.utils.IOUtils;
import org.junit.Test;

import at.ac.c3pro.chormodel.IPublicModel;
import at.ac.c3pro.io.Bpmn2ChoreographyModel;
import at.ac.c3pro.io.Bpmn2Collaboration;
import at.ac.c3pro.io.Bpmn2PrivateModel;

public class Bpmn2CollaborationTest {

	@Test
	public void test() throws Exception { // CollaborationBookTripV3
		Bpmn2Collaboration example = new Bpmn2Collaboration("target/CollaborationBookTripV3.xml","CollaborationBookTrip");
		//IPublicModel acquirer = null;
		/*Bpmn2PrivateModel example2 = new Bpmn2PrivateModel("target/AcquirerPrivateProcess.xml","PrivateAcquirerModel");
		IOUtils.toFile(example2.privateModel.getName()+"Bpmn2PrivateModelTest.dot", example2.privateModel.getdigraph().toDOT());
		Bpmn2ChoreographyModel example3 = new Bpmn2ChoreographyModel("target/BookTripOperation.xml", "BookTripOperation");
		IOUtils.toFile("BookTripBpmn2chorTest.dot", example3.choreoModel.getdigraph().toDOT()); */
		
		for(IPublicModel pum: example.collab.puModels){
			System.out.println(pum.getName());
			IOUtils.toFile(pum.getName()+"Bpmn2chollaborationTest.dot", pum.getdigraph().toDOT());
			System.out.println(pum.getName());
			if(pum.getName().equals("Acquirer")){
				//System.out.println("-----"+example2.PrivateNode2PublicNodeIDMap);
				//System.out.println("*****"+example2.getMapPr2puNode(pum));	
				//System.out.println("*****"+example2.getMapPu2prNode(pum));	
			}	
		}
		
		//System.out.println("@@@@@@@@"+example.getMapPu2ChoreoNode(example3.choreoModel));
		
		//simple tests
		assertEquals(9, example.collab.Pu2Pu.size());
		assertEquals(4, example.collab.R2PuM.size());
		
		System.out.println("*********Map node 2node : ");
		System.out.println(example.collab.Pu2Pu);
		System.out.println("*********  Role 2 PuM map");
		System.out.println("*********  List of Roles");
		System.out.println(example.collab.roles);
		
		
		
		
		
		
	}

}
