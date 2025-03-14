package at.ac.c3pro.tests;

import at.ac.c3pro.chormodel.IPublicModel;
import at.ac.c3pro.io.Bpmn2Collaboration;
import org.jbpt.utils.IOUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Bpmn2CollaborationTest {

    @Test
    public void test() throws Exception { // CollaborationBookTripV3
        Bpmn2Collaboration example = new Bpmn2Collaboration("target/CollaborationBookTripV3.xml",
                "CollaborationBookTrip");

        for (IPublicModel pum : example.collab.puModels) {
            System.out.println(pum.getName());
            IOUtils.toFile(pum.getName() + "Bpmn2chollaborationTest.dot", pum.getdigraph().toDOT());
            System.out.println(pum.getName());
        }

        // simple tests
        assertEquals(9, example.collab.Pu2Pu.size());
        assertEquals(4, example.collab.R2PuM.size());

        System.out.println("*********Map node 2node : ");
        System.out.println(example.collab.Pu2Pu);
        System.out.println("*********  Role 2 PuM map");
        System.out.println("*********  List of Roles");
        System.out.println(example.collab.roles);

    }

}
