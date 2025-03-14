package at.ac.c3pro.tests;

import at.ac.c3pro.change.IChangeOperation;
import at.ac.c3pro.changePropagation.ChangePropagationUtil;
import at.ac.c3pro.changePropagation.ChangePropagationUtil.ChgOpType;
import at.ac.c3pro.changePropagation.Stats;
import at.ac.c3pro.chormodel.*;
import at.ac.c3pro.io.Bpmn2ChoreographyModel;
import at.ac.c3pro.io.Bpmn2Collaboration;
import at.ac.c3pro.io.Bpmn2PrivateModel;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.IPrivateNode;
import at.ac.c3pro.node.IPublicNode;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import org.jbpt.utils.IOUtils;
import org.junit.Test;

import java.io.File;
import java.util.*;

public class Bpmn2ChoreographyTest {

    // this class tries to test a whole choreography generation from xml files with
    // all mapping between different models and nodes
    private Choreography Booking;

    @Test
    public void test() throws Exception {

        Bpmn2ChoreographyModel BookTripChoreoModel1 = new Bpmn2ChoreographyModel("target/choreo2BPMNtest.xml",
                "BookTripOperationExtended");

        IOUtils.toFile("BookTripExtendedBpmn2chorTestGraph_2.dot",
                BookTripChoreoModel1.choreoModel.getdigraph().toDOT());
        IOUtils.toFile("BookTripExtendedBpmn2chorTestRPST.dot",
                ((ChoreographyModel) BookTripChoreoModel1.choreoModel).toDOT());

        // Generating the choreography mode
        Bpmn2ChoreographyModel BookTripChoreoModel = new Bpmn2ChoreographyModel("target/BookTripOperation.xml",
                "BookTripOperation");

        IOUtils.toFile("BookTripBpmn2chorTestGraph.dot", BookTripChoreoModel.choreoModel.getdigraph().toDOT());
        IOUtils.toFile("BookTripBpmn2chorTestRPST.dot", ((ChoreographyModel) BookTripChoreoModel.choreoModel).toDOT());

        // Generating a Collaboration Model : all public views
        Bpmn2Collaboration collaboration = new Bpmn2Collaboration("target/CollaborationBookTripV3.xml",
                "CollaborationBookTrip");
        for (IPublicModel pum : collaboration.collab.puModels) {
            IOUtils.toFile(pum.getName() + "Bpmn2chollaborationTestRPST.dot", ((PublicModel) pum).toDOT());
            IOUtils.toFile(pum.getName() + "Bpmn2chollaborationTestGraph.dot", pum.getdigraph().toDOT());
        }

        // Generating a private model for each public view
        List<IPrivateModel> privatemodels = new LinkedList<IPrivateModel>();
        List<Bpmn2PrivateModel> Bpmn2PrivateModels = new LinkedList<Bpmn2PrivateModel>();
        Bpmn2PrivateModel AcquirerPrivateModel = new Bpmn2PrivateModel("target/AcquirerPrivateProcess.xml",
                "PrivateAcquirerModel");
        Bpmn2PrivateModels.add(AcquirerPrivateModel);
        privatemodels.add(AcquirerPrivateModel.privateModel);
        IOUtils.toFile(AcquirerPrivateModel.privateModel.getName() + "Bpmn2PrivateModelTestGraph.dot",
                AcquirerPrivateModel.privateModel.getdigraph().toDOT());
        IOUtils.toFile(AcquirerPrivateModel.privateModel.getName() + "Bpmn2PrivateModelTestRPST.dot",
                ((PrivateModel) AcquirerPrivateModel.privateModel).toDOT());

        // building the Choreography
        Booking = new Choreography("111", collaboration.collab, privatemodels, BookTripChoreoModel.choreoModel);

        // making the mapping between all nodes, models and roles
        for (Bpmn2PrivateModel prm : Bpmn2PrivateModels) {
            for (IPublicModel pum : collaboration.collab.puModels)
                if (pum.getName().equals(prm.privateModel.getName())) {
                    Booking.Pr2Pu.putAll(AcquirerPrivateModel.getMapPr2puNode(pum));
                    Booking.Pu2Pr.putAll(AcquirerPrivateModel.getMapPu2prNode(pum));
                    Booking.P2P.put(prm.privateModel, pum);
                }

        }

        Booking.Pu2Ch = collaboration.getMapPu2ChoreoNode(BookTripChoreoModel.choreoModel);
        Booking.Ch2PuPair = collaboration.getChoreoNode2PuNPairMap(BookTripChoreoModel.choreoModel);
        Booking.ChGtw2PuGtws = collaboration.getchorGtw2PuGtws(BookTripChoreoModel.choreoModel);

        // Normalization of roles of diff models
        // for(Role puMrole: collaboration.collab.roles)
        // for(Role prMrole : )

        System.out.println();
        System.out.println("----------------------------");
        System.out.print("The roles =");
        for (Role r : Booking.collaboration.roles)
            System.out.print(r.name + "-->" + r.id + "      ");
        System.out.println();
        System.out.println("----------------------------");
        System.out.print("The Choreography Model Nodes = ");
        for (IChoreographyNode chn : Booking.choreo.getdigraph().getVertices())
            System.out.print(chn.getName() + "==" + chn.getId() + "     ");

        System.out.println();
        System.out.println("----------------------------");
        System.out.println("The public Modeles  =");
        for (IPublicModel pum : Booking.collaboration.puModels) {
            System.out.print(pum.getName() + " has the Role ID: " + Booking.collaboration.PuM2R.get(pum).id + " :   ");
            for (IPublicNode pun : pum.getdigraph().getVertices())
                System.out.print(pun.getName() + "==" + pun.getId() + "     ");
            System.out.println();
        }
        System.out.println();
        System.out.println("----------------------------");
        System.out.println("The private Modeles  =");
        for (IPrivateModel prm : Booking.privateModels) {
            System.out.print(
                    "privatemodel = " + prm.getName() + " corresponds to publicModel " + Booking.P2P.get(prm).getName()
                            + "=" + Booking.collaboration.PuM2R.get(Booking.P2P.get(prm)).id + " :   ");
            for (IPrivateNode prn : prm.getdigraph().getVertices())
                System.out.print(prn.getName() + "==" + prn.getId() + "     ");
            System.out.println();
        }

        System.out.println();
        System.out.println("----------------------------");
        System.out.println("Mapping Public Nodes to Choreography nodes ");
        for (IPublicNode pun : Booking.Pu2Ch.keySet()) {
            System.out
                    .println("publicNode " + pun.getName() + " with ID " + pun.getId() + "  corresponds to choreoNode "
                            + Booking.Pu2Ch.get(pun).getName() + " whith ID = " + Booking.Pu2Ch.get(pun).getId());

        }

        System.out.println();
        System.out.println("----------------------------");
        System.out.println("Mapping Choreography Nodes to Pair of Public nodes ");
        System.out.println("size=" + Booking.Ch2PuPair.size());
        for (IChoreographyNode cn : Booking.Ch2PuPair.keySet()) {
            System.out.println("ChoreographyNode " + cn.getName() + " with ID " + cn.getId()
                    + "  corresponds to publicNodes " + Booking.Ch2PuPair.get(cn).first.getName() + " whith ID = "
                    + Booking.Ch2PuPair.get(cn).first.getId() + "  and " + Booking.Ch2PuPair.get(cn).second.getName()
                    + " whith ID = " + Booking.Ch2PuPair.get(cn).second.getId());

        }

        System.out.println();
        System.out.println("----------------------------");
        System.out.println("Mapping Public Nodes of differents public Models ");
        for (IPublicNode pun : Booking.collaboration.Pu2Pu.keySet()) {
            System.out.println("publicNode " + pun.getName() + " with ID " + pun.getId()
                    + "  corresponds to public Node " + Booking.collaboration.Pu2Pu.get(pun).getName() + " whith ID = "
                    + Booking.collaboration.Pu2Pu.get(pun).getId());

        }

        System.out.println("--ChangePropagationStatisticsTest");
        // build collaboration
        // buildChoreography2();
        // generate the set of possible change operations organized by type and role.
        Map<IRole, Map<ChgOpType, Set<IChangeOperation>>> role2map = ChangePropagationUtil
                .generateChangeOperationsForPublicModels(Booking);
        Map<String, Stats> chgOpId2Stats = new HashMap<String, Stats>();
        ChangePropagationUtil.init(Booking);
        int nb = 0;
        // Change Prpagation for each generated change request
        for (IRole role : role2map.keySet()) {
            Map<ChgOpType, Set<IChangeOperation>> role2map1 = role2map.get(role);
            for (ChgOpType type : role2map1.keySet())
                for (IChangeOperation op : role2map.get(role).get(type)) {
                    nb++;
                    // ChangePropagationUtil.propagate(op, null, null, chgOpId2Stats);
                }
        }
        System.out.println("nb operations : " + nb + "   " + ChangePropagationUtil.nbOp);

        // Creating an excel file and storing all metrics
        WritableWorkbook workbook = Workbook.createWorkbook(new File("target/ChangePropagationStats.xls"));
        WritableSheet sheet = workbook.createSheet("propagationSheet", 0);

        Label ChgOpId = new Label(0, 0, "ChgOpId");
        Label type = new Label(1, 0, "Type");
        Label Nb_Nodes_source = new Label(2, 0, "Nb_Nodes_source");
        Label nb_activ_source = new Label(3, 0, "nb_activ_source");
        Label nb_AndGtw_source = new Label(4, 0, "nb_AndGtw_source");
        Label nb_XorGtw_source = new Label(5, 0, "nb_XorGtw_source");
        // Metrics of the propagation result
        Label Nb_Nodes_target = new Label(7, 0, "Nb_Nodes_target");
        Label nb_activ_target = new Label(8, 0, "nb_activ_target");
        Label nb_AndGtw_target = new Label(9, 0, "nb_AndGtw_target");
        Label nb_XorGtw_target = new Label(10, 0, "nb_XorGtw_target");
        Label nb_affected_partners = new Label(11, 0, "nb_affected_partners");
        Label nb_Insert_genreated = new Label(12, 0, "nb_Insert_genreated");
        Label nb_Replace_generated = new Label(13, 0, "nb_Replace_generated");
        Label nb_Delete_generated = new Label(14, 0, "nb_Delete_generated");

        sheet.addCell(nb_XorGtw_target);
        sheet.addCell(nb_AndGtw_target);
        sheet.addCell(nb_activ_target);
        sheet.addCell(Nb_Nodes_target);
        sheet.addCell(nb_Delete_generated);
        sheet.addCell(nb_Replace_generated);
        sheet.addCell(nb_Insert_genreated);
        sheet.addCell(nb_affected_partners);
        sheet.addCell(nb_XorGtw_source);
        sheet.addCell(nb_AndGtw_source);
        sheet.addCell(nb_activ_source);
        sheet.addCell(Nb_Nodes_source);
        sheet.addCell(type);
        sheet.addCell(ChgOpId);

        int i = 1;
        for (String opId : chgOpId2Stats.keySet()) {
            Stats stats = chgOpId2Stats.get(opId);
            sheet.addCell(new Label(0, i, opId));
            sheet.addCell(new Label(1, i, stats.type.toString()));
            sheet.addCell(new Label(2, i, Integer.toString(stats.Nb_Nodes_source)));
            sheet.addCell(new Label(3, i, Integer.toString(stats.nb_activ_source)));
            sheet.addCell(new Label(4, i, Integer.toString(stats.nb_AndGtw_source)));
            sheet.addCell(new Label(5, i, Integer.toString(stats.nb_XorGtw_source)));
            sheet.addCell(new Label(7, i, Integer.toString(stats.Nb_Nodes_target)));
            sheet.addCell(new Label(8, i, Integer.toString(stats.nb_activ_target)));
            sheet.addCell(new Label(9, i, Integer.toString(stats.nb_AndGtw_target)));
            sheet.addCell(new Label(10, i, Integer.toString(stats.nb_XorGtw_target)));
            sheet.addCell(new Label(11, i, Integer.toString(stats.nb_affected_partners)));
            sheet.addCell(new Label(12, i, Integer.toString(stats.nb_Insert_generated)));
            sheet.addCell(new Label(13, i, Integer.toString(stats.nb_Replace_generated)));
            sheet.addCell(new Label(14, i, Integer.toString(stats.nb_Delete_generated)));
            i++;
        }
        // Write and close the workbook
        workbook.write();
        workbook.close();
        

        System.out.println(collaboration.collab.Name2PuGtws);
        System.out.println(Booking.ChGtw2PuGtws);
    }


}
