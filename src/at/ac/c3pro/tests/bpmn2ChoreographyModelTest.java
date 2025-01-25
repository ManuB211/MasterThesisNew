package at.ac.c3pro.tests;

import at.ac.c3pro.ImpactAnalysis.ImpactAnalysisUtil;
import at.ac.c3pro.ImpactAnalysis.ImpactAnalysisUtil.IPartnerNode;
import at.ac.c3pro.ImpactAnalysis.ImpactAnalysisUtil.Pair;
import at.ac.c3pro.chormodel.IRole;
import at.ac.c3pro.io.Bpmn2ChoreographyModel;
import org.jbpt.utils.IOUtils;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class bpmn2ChoreographyModelTest {

    @Test
    public void test() throws Exception {
        Bpmn2ChoreographyModel example = new Bpmn2ChoreographyModel("target/BookTripOperation.xml",
                "BookTripOperation");
        IOUtils.toFile("BookTripBpmn2chorTest.dot", example.choreoModel.getdigraph().toDOT());
        // IOUtils.toFile("BookTripBpmn2chorTestRpst.dot", example.choreoModel.toDOT());

        Map<Pair<IRole, IRole>, Double> ConnectivityMap = new HashMap<Pair<IRole, IRole>, Double>();
        ConnectivityMap = ImpactAnalysisUtil.ComputeConnectivity(example.choreoModel);

        /*
         * for(Pair<IRole, IRole> p : ConnectivityMap.keySet()){
         * System.out.println("< "+p.first+ ","+p.second + " > -->  " +
         * ConnectivityMap.get(p)); }
         */

        Map<IPartnerNode, Double> centrality = new HashMap<IPartnerNode, Double>();
        centrality = ImpactAnalysisUtil.ComputeCentrality(example.choreoModel, ConnectivityMap);

        Map<Pair<IRole, IRole>, Double> staticImpact = new HashMap<Pair<IRole, IRole>, Double>();
        staticImpact = ImpactAnalysisUtil.StaticImpact(ConnectivityMap, centrality);
        // System.out.println(ImpactAnalysisUtil.ComputeConnectivity(example.choreoModel));
    }
}
