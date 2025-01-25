package at.ac.c3pro.tests;

import at.ac.c3pro.chormodel.MultiDirectedGraph;
import at.ac.c3pro.io.Bpmn2PrivateModel;
import at.ac.c3pro.io.PrivateModel2Bpmn;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.IPrivateNode;
import org.jbpt.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;

public class PrivateModel2BpmnTest {

    Bpmn2PrivateModel privateModelIO = null;

    @Before
    public void initChoreography() throws Exception {

        privateModelIO = new Bpmn2PrivateModel("target/AcquirerPrivateProcess.xml", "privatemodel");

    }

    @Test
    public void test() throws Exception {
        PrivateModel2Bpmn privateModel2bpmnIO = new PrivateModel2Bpmn(privateModelIO.privateModel, "test", "target");
        privateModel2bpmnIO.buildXML();

        MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode> digraph = (MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode>) privateModelIO.privateModel
                .getdigraph();

        IOUtils.toFile("pm_original.dot", digraph.toDOT());

        Bpmn2PrivateModel genModel = new Bpmn2PrivateModel("target/privateModel2BPMNtest.xml", "GeneratedBPMN");

        MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode> genGraph = (MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode>) genModel.privateModel
                .getdigraph();
        IOUtils.toFile("pm_generated.dot", genGraph.toDOT());

        System.out.println(digraph.compareTo(genGraph));

    }

}
