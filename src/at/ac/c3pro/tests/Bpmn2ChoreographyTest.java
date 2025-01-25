package at.ac.c3pro.tests;

import at.ac.c3pro.ImpactAnalysis.ImpactAnalysisUtil.PEdge;
import at.ac.c3pro.ImpactAnalysis.ImpactAnalysisUtil.Pair;
import at.ac.c3pro.ImpactAnalysis.ImpactAnalysisUtil.PartnerNode;
import at.ac.c3pro.change.IChangeOperation;
import at.ac.c3pro.changePropagation.ChangePropagationUtil;
import at.ac.c3pro.changePropagation.ChangePropagationUtil.ChgOpType;
import at.ac.c3pro.changePropagation.Stats;
import at.ac.c3pro.chormodel.*;
import at.ac.c3pro.io.Bpmn2ChoreographyModel;
import at.ac.c3pro.io.Bpmn2Collaboration;
import at.ac.c3pro.io.Bpmn2PrivateModel;
import at.ac.c3pro.node.*;
import at.ac.c3pro.util.ChoreographyGenerator;
import at.ac.c3pro.util.FragmentUtil;
import at.ac.c3pro.util.Util;
import au.com.bytecode.opencsv.CSVWriter;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;
import org.jbpt.utils.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

        // Impact Anlysis test
        // UNCOMMENT
        // System.out.println("---centrality");
        // System.out.println(ChangePropagationUtil.centrality);
        // System.out.println("---Propagation graph");
        // System.out.println(ChangePropagationUtil.PropagationGraphMetrics);
        // for(Pair<IRole,IRole> pair :
        // ChangePropagationUtil.PropagationGraphMetrics.keySet()){
        // System.out.println("<"+pair.first+" , "+pair.second+">
        // -->"+ChangePropagationUtil.PropagationGraphMetrics.get(pair));
        // }

        System.out.println(collaboration.collab.Name2PuGtws);
        System.out.println(Booking.ChGtw2PuGtws);
    }

    @Test
    public void bpmn2chortest() throws Exception {

        String chorePath = "target/Book_Trip_OperationExtended5.xml";
        Bpmn2ChoreographyModel BookTripChoreoModel1 = new Bpmn2ChoreographyModel(chorePath,
                "BookTripOperationExtended");
        Choreography Booking = ChoreographyGenerator.generateChoreographyFromModel(BookTripChoreoModel1.choreoModel);

        IOUtils.toFile("BookTripExtendedBpmn2chorTestGraph.dot", BookTripChoreoModel1.choreoModel.getdigraph().toDOT());
        IOUtils.toFile("BookTripExtendedBpmn2chorTestRPST.dot",
                ((ChoreographyModel) BookTripChoreoModel1.choreoModel).toDOT());
        System.out.println("role=  " + FragmentUtil.extractRoles(BookTripChoreoModel1.choreoModel.getRoot()));
        for (Role role : BookTripChoreoModel1.roles) {
            IOUtils.toFile("projection/projection_" + role.name + ".dot",
                    BookTripChoreoModel1.choreoModel.projectionRole(role, true).getdigraph().toDOT());
            System.out
                    .println(
                            "PuM" + role + " = "
                                    + (ChoreographyGenerator.transformChorModel2PubModel(
                                    BookTripChoreoModel1.choreoModel.projectionRole(role, true), role))
                                    .getdigraph());
            IOUtils.toFile("PublicModels/PuM_" + role.name + ".dot", (ChoreographyGenerator
                    .transformChorModel2PubModel(BookTripChoreoModel1.choreoModel.projectionRole(role, true), role))
                    .getdigraph().toDOT());
        }
        System.out.println("messages=" + FragmentUtil.collectMessages(BookTripChoreoModel1.choreoModel.getRoot()));

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

        // TODO: limit the number of threads here
        int num_threads = 1;
        List<Role> roles = new LinkedList<Role>();
        roles.addAll(Booking.collaboration.roles);

        String filename = "target/ChangePropagationStats.xls";
        WritableWorkbook workbook = createXLS(filename);

        ExecutorService executor = Executors.newFixedThreadPool(num_threads);
        List<Callable<Object>> threads = new LinkedList<Callable<Object>>();
        List<PropagationThread> threads_props = new LinkedList<PropagationThread>();

        for (List<Role> roleList : Util.splitOperations(roles, num_threads)) {
            List<String> roleNames = new LinkedList<String>();
            for (Role r : roleList)
                roleNames.add(r.name);
            PropagationThread worker = new PropagationThread(chorePath, roleNames);
            // executor.execute(worker);
            threads.add(Executors.callable(worker));
            threads_props.add(worker);
        }

        System.out.println("--ChangePropagationStatisticsTest");
        // build collaboration
        // buildChoreography2();
        // generate the set of possible change operations organized by type and role.
        /*
         * Map<IRole, Map<ChgOpType, Set<IChangeOperation>>> role2map =
         * ChangePropagationUtil.generateChangeOperationsForPublicModels(Booking);
         *
         * // sum the total # of operations int nb_total = 0; List<IChangeOperation>
         * all_ops = new LinkedList<IChangeOperation>(); for(IRole role :
         * role2map.keySet()) { Map<ChgOpType, Set<IChangeOperation>> role2map1 =
         * role2map.get(role); for(ChgOpType type : role2map1.keySet())
         * for(IChangeOperation op :role2map.get(role).get(type)){ all_ops.add(op); } }
         * nb_total = all_ops.size();
         *
         * Map<String, Long> executionTimeMap = new HashMap<String, Long>();
         * ChangePropagationUtil.init(Booking); //Change Prpagation for each generated
         * change request int nb = 0; //
         *
         * String filename = "target/ChangePropagationStats.xls"; WritableSheet sheet =
         * createXLS(filename);
         *
         * ExecutorService executor = Executors.newFixedThreadPool(num_threads);
         * List<PropagationThread> threads = new LinkedList<PropagationThread>(); for
         * (List<IChangeOperation> ops : Util.splitOperations(all_ops, num_threads)) {
         * Runnable worker = new PropagationThread(ops, Booking);
         * executor.execute(worker); }
         *
         */

        executor.invokeAll(threads);
        executor.shutdown();

        // wait until threadpool is finished
        // while (!executor.isTerminated()) {}

        System.out.println("-- EXECUTOR TERMINATED...CONTINUING WITH WRITING");
        int maxResponseTime = 36000; // seconds
        int minResponseTime = 100; // seconds

        String logfilename = "target/ChangePropagationLog.csv";
        String centralityfilename = "target/CentralityMatrix.csv";

        CSVWriter writer = new CSVWriter(new FileWriter(logfilename), ',');
        CSVWriter writercentrality = new CSVWriter(new FileWriter(centralityfilename), ',');
        List<String[]> logStrings = new ArrayList<String[]>();
        List<String[]> centralityStrings = new ArrayList<String[]>();

        String[] Heading = new String[]{"TimeStamp", "InitialChgId", "InitialChgType", "ChgRequestor",
                "Nb-Source_Nodes", "AffectedPartner", "DerivedChgID", "DerivedChgType", "derived_Nb_nodes",
                "NegotiationResult", "ResponseTime (seconds)"};
        String[] centralityHeading = new String[]{"SourcePartner", "TargetPartner", "Frequency"};

        centralityStrings.add(centralityHeading);
        logStrings.add(Heading);
        int xlsRow = 1;
        for (PropagationThread thread : threads_props) {
            /*
             * xlsRow = appendToXLS(xlsRow, workbook, thread.executionTimeMap,
             * thread.chgOpId2Stats); for(IChangeOperation initialChange :
             * thread.ChangePropagationLogMap.keySet()){ Timestamp ts =
             * this.getRandomTS("2013-08-03 00:00:00", "2013-12-18 00:00:00"); Map<Role,
             * List<IChangeOperation>> r2ops =
             * thread.ChangePropagationLogMap.get(initialChange); int negotiationResult = 0;
             * double acceptanceRate =0.5; if(Math.random()>acceptanceRate)
             * negotiationResult = 1; int[] nb_reject = new int[r2ops.size()];
             * if(negotiationResult == 0){ nb_reject[0] = 0; for(int i = 1; i<r2ops.size();
             * i++){ if(Math.random()>acceptanceRate) nb_reject[i] = 1; else nb_reject[i] =
             * 0; } } int i=0; for(Role r : r2ops.keySet()){ for(IChangeOperation
             * derivedChange : r2ops.get(r)){ System.out.println("i = "+ i +
             * "    nb_reject = "+ nb_reject + "   size = "+r2ops.size() ); String[]
             * oneRowData = (ts + "#" + initialChange.getId()+"#"+initialChange.getType() +
             * "#" + initialChange.getCurrentRole() + "#" +
             * thread.chgOpId2Stats.get(initialChange.getId()).Nb_Nodes_source + "#" +
             * r.name + "#" + derivedChange.getId() + "#" + derivedChange.getType() + "#" +
             * derivedChange.getNb_nodes() + "#" + nb_reject[i] + "#" +
             * getRandomRange(minResponseTime, maxResponseTime) ).split("#");
             *
             * logStrings.add(oneRowData); } i++; }
             *
             * }
             */

            // Impact Anlysis test
            // UNCOMMENT

            System.out.println("---Propagation graph");
            // System.out.println(thread.PropagationGraphMetrics);
            Map<IRole, Double> sumOutMap = new HashMap<IRole, Double>();
            Set<IRole> setRoles = new HashSet<IRole>();
            for (Pair<IRole, IRole> pair : PropagationThread.PropagationGraphMetrics.keySet())
                setRoles.add(pair.first);

            for (IRole r : setRoles) {
                double sumRole = 0.0;
                for (Pair<IRole, IRole> pair : PropagationThread.PropagationGraphMetrics.keySet()) {
                    if (((Role) r).id.equals(((Role) pair.first).id))
                        sumRole = sumRole + PropagationThread.PropagationGraphMetrics.get(pair);
                }
                sumOutMap.put(r, sumRole);
            }

            /*
             * for(Pair<IRole,IRole> pair : thread.PropagationGraphMetrics.keySet()){ double
             * sum = 0; sum = sum + thread.PropagationGraphMetrics.get(pair) ; }
             */

            for (Pair<IRole, IRole> pair : PropagationThread.PropagationGraphMetrics.keySet()) {
                for (IRole r : sumOutMap.keySet()) {
                    if (pair.first.equals(r)) {
                        double frequence = (PropagationThread.PropagationGraphMetrics.get(pair) / sumOutMap.get(r));
                        double x = 1000.0;
                        double frequency = (Math.round(frequence * x)) / x;
                        String[] oneRowData = (pair.first + "#" + pair.second + "#" + frequency).split("#");
                        System.out.println("<" + pair.first + " , " + pair.second + "> -->" + frequency);
                        centralityStrings.add(oneRowData);
                    }
                }

            }
            List<PEdge> ListEdges = new LinkedList<PEdge>();
            MultiDirectedGraph<Edge<PartnerNode>, PartnerNode> PropagationGraph = new MultiDirectedGraph<Edge<PartnerNode>, PartnerNode>();
            Map<IRole, PartnerNode> role2NodeMap = new HashMap<IRole, PartnerNode>();
            for (Pair<IRole, IRole> pair : PropagationThread.PropagationGraphMetrics.keySet()) {
                if (PropagationThread.PropagationGraphMetrics.get(pair) > 0.0) {
                    if (!role2NodeMap.containsKey(pair.first)) {
                        PartnerNode pnode = new PartnerNode(pair.first);
                        role2NodeMap.put(pair.first, pnode);
                    }
                    if (!role2NodeMap.containsKey(pair.second)) {
                        PartnerNode pnode = new PartnerNode(pair.second);
                        role2NodeMap.put(pair.second, pnode);
                    }
                }
            }

            for (Pair<IRole, IRole> pair : PropagationThread.PropagationGraphMetrics.keySet()) {
                if (PropagationThread.PropagationGraphMetrics.get(pair) > 0.0) {
                    PEdge e = new PEdge(role2NodeMap.get(pair.first), role2NodeMap.get(pair.second),
                            PropagationThread.PropagationGraphMetrics.get(pair));
                    ListEdges.add(e);
                    PropagationGraph.addEdge(role2NodeMap.get(pair.first), role2NodeMap.get(pair.second));
                }
            }
            IOUtils.toFile("PropagationGraph.dot", PropagationGraph.toDOT());
            System.out.println("---centrality");
        }
        writer.writeAll(logStrings);
        writer.close();
        workbook.write();
        workbook.close();

        writercentrality.writeAll(centralityStrings);
        writercentrality.close();

        // Stats
        int nb_roles = Booking.collaboration.roles.size();
        int nbTNodes = 0;
        int maxNodes = 0;
        int minNodes = 10000;
        int nbTActivities = 0;
        int minActivities = 10000;
        int maxActivities = 0;
        int nbTXor = 0;
        int maxXor = 0;
        int minXor = 10000;
        int nbTAnd = 0;
        int maxAnd = 0;
        int minAnd = 10000;

        for (IPublicModel pum : Booking.collaboration.puModels) {
            int aux = pum.getdigraph().countVertices();
            nbTNodes = nbTNodes + aux;
            if (aux > maxNodes)
                maxNodes = aux;
            if (aux < minNodes)
                minNodes = aux;
        }

        double AvgNodesByRole = (double) nbTNodes / (double) nb_roles;
        for (IPublicModel pum : Booking.collaboration.puModels) {
            int auxAct = 0;
            int auxXor = 0;
            int auxAnd = 0;
            for (IPublicNode n : pum.getdigraph().getVertices()) {

                if (n instanceof XorGateway) {
                    nbTXor++;
                    auxXor++;
                } else if (n instanceof AndGateway) {
                    nbTAnd++;
                    auxAnd++;
                } else {
                    nbTActivities++;
                    auxAct++;
                }
            }
            nbTActivities = nbTActivities - 2; // 2 events
            if (auxAct > maxActivities)
                maxActivities = auxAct;
            if (auxAct < minActivities)
                minActivities = auxAct;
            if (auxXor > maxXor)
                maxXor = auxXor;
            if (auxXor < minXor)
                minXor = auxXor;
            if (auxAnd > maxAnd)
                maxAnd = auxAnd;
            if (auxAnd < minAnd)
                minAnd = auxAnd;
        }

        int nbTGateways = nbTXor + nbTAnd;
        double avgActivities = (double) nbTActivities / (double) nb_roles;
        double avgXor = (double) nbTXor / (double) nb_roles;
        double avgAnd = (double) nbTAnd / (double) nb_roles;

        int nbTchgRequests = 0;
        double Avg_chgReqByRole = 0;
        int nb_delete = 0;
        int nb_insert = 0;
        int nb_replace = 0;
        int maxChgRequets = 0;
        int minChgRequests = 0;
        int maxDelete = 0;
        int minDelete = 0;
        int maxReplace = 0;
        int minReplace = 0;
        int maxInsert = 0;
        int minInsert = 0;

        for (PropagationThread thread : threads_props) {
            nbTchgRequests = nbTchgRequests + thread.all_ops.size();
            Avg_chgReqByRole = Avg_chgReqByRole + thread.all_ops.size();
            minChgRequests = thread.minRequests;
            maxChgRequets = thread.maxRequests;
            maxDelete = thread.maxDelete;
            minDelete = thread.minDelete;
            maxReplace = thread.maxReplace;
            minReplace = thread.minReplace;
            maxInsert = thread.maxInsert;
            minInsert = thread.minInsert;

            for (IChangeOperation op : thread.all_ops) {
                if (op.getType().equals(ChgOpType.Delete))
                    nb_delete++;
                if (op.getType().equals(ChgOpType.Replace))
                    nb_replace++;
                if (op.getType().equals(ChgOpType.Insert))
                    nb_insert++;
            }
        }

        Avg_chgReqByRole = Avg_chgReqByRole / (double) nb_roles;

        System.out.println("nb_roles = " + nb_roles);
        System.out.println("nbTNodes  = " + nbTNodes);
        System.out.println("maxNodes  = " + maxNodes);
        System.out.println("minNodes  = " + minNodes);
        System.out.println("nbTActivities  = " + nbTActivities);
        System.out.println("minActivities  = " + minActivities);
        System.out.println("maxActivities  = " + maxActivities);
        System.out.println("nbTXor  = " + nbTXor);
        System.out.println("maxXor  = " + maxXor);
        System.out.println("minXor  = " + minXor);
        System.out.println("nbTAnd = " + nbTAnd);
        System.out.println("maxAnd  = " + maxAnd);
        System.out.println("minAnd  = " + minAnd);
        System.out.println("nbTchgRequests  = " + nbTchgRequests);
        System.out.println("Avg_chgReqByRole  = " + Avg_chgReqByRole);
        System.out.println("maxChgRequets  = " + maxChgRequets);
        System.out.println("minChgRequests  = " + minChgRequests);
        System.out.println("nb_delete  =" + nb_delete);
        System.out.println("nb_replace   =" + nb_replace);
        System.out.println("nb_insert   =" + nb_insert);
        System.out.println("maxDelete  =" + maxDelete);
        System.out.println("minDelete   =" + minDelete);
        System.out.println(" maxReplace  =" + maxReplace);
        System.out.println(" minReplace  =" + minReplace);
        System.out.println("maxInsert   =" + maxInsert);
        System.out.println("minInsert   =" + minInsert);

    }

    private WritableWorkbook createXLS(String filename) throws IOException, WriteException {
        // Creating an excel file and storing all metrics
        WritableWorkbook workbook = Workbook.createWorkbook(new File(filename));
        WritableSheet sheet = workbook.createSheet("propagationSheet", 0);

        Label ChgOpId = new Label(0, 0, "ChgOpId");
        Label partner = new Label(1, 0, "Partner");
        Label type = new Label(2, 0, "Type");
        Label Nb_Nodes_source = new Label(3, 0, "Nb_Nodes_source");
        Label nb_activ_source = new Label(4, 0, "nb_activ_source");
        Label nb_AndGtw_source = new Label(5, 0, "nb_AndGtw_source");
        Label nb_XorGtw_source = new Label(6, 0, "nb_XorGtw_source");
        // Metrics of the propagation result
        Label Nb_Nodes_target = new Label(7, 0, "Nb_Nodes_target");
        Label nb_activ_target = new Label(8, 0, "nb_activ_target");
        Label nb_AndGtw_target = new Label(9, 0, "nb_AndGtw_target");
        Label nb_XorGtw_target = new Label(10, 0, "nb_XorGtw_target");
        Label nb_affected_partners = new Label(11, 0, "nb_affected_partners");
        Label nb_Insert_genreated = new Label(12, 0, "nb_Insert_genreated");
        Label nb_Replace_generated = new Label(13, 0, "nb_Replace_generated");
        Label nb_Delete_generated = new Label(14, 0, "nb_Delete_generated");
        Label exec_time = new Label(15, 0, "exec_time");

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
        sheet.addCell(exec_time);
        sheet.addCell(partner);

        return workbook;
    }

    private int appendToXLS(int currRow, WritableWorkbook workbook, Map<String, Long> executionTimeMap,
                            Map<String, Stats> chgOpId2Stats) throws WriteException {
        WritableSheet sheet = workbook.getSheet("propagationSheet"); // TODO: remove hardcoded name
        System.out.println("\tappending to XLS." + " executionTimeMap.size():" + executionTimeMap.size()
                + " chgOpId2Stats.size():" + chgOpId2Stats.size());
        int i = currRow;
        for (String opId : chgOpId2Stats.keySet()) {
            Stats stats = chgOpId2Stats.get(opId);
            long time = executionTimeMap.get(opId);
            sheet.addCell(new Label(0, i, opId));
            sheet.addCell(new Label(1, i, stats.role.toString()));
            sheet.addCell(new Label(2, i, stats.type.toString()));
            sheet.addCell(new Label(3, i, Integer.toString(stats.Nb_Nodes_source)));
            sheet.addCell(new Label(4, i, Integer.toString(stats.nb_activ_source)));
            sheet.addCell(new Label(5, i, Integer.toString(stats.nb_AndGtw_source)));
            sheet.addCell(new Label(6, i, Integer.toString(stats.nb_XorGtw_source)));
            sheet.addCell(new Label(7, i, Integer.toString(stats.Nb_Nodes_target)));
            sheet.addCell(new Label(8, i, Integer.toString(stats.nb_activ_target)));
            sheet.addCell(new Label(9, i, Integer.toString(stats.nb_AndGtw_target)));
            sheet.addCell(new Label(10, i, Integer.toString(stats.nb_XorGtw_target)));
            sheet.addCell(new Label(11, i, Integer.toString(stats.nb_affected_partners)));
            sheet.addCell(new Label(12, i, Integer.toString(stats.nb_Insert_generated)));
            sheet.addCell(new Label(13, i, Integer.toString(stats.nb_Replace_generated)));
            sheet.addCell(new Label(14, i, Integer.toString(stats.nb_Delete_generated)));
            sheet.addCell(new Label(15, i, Long.toString(time)));
            i++;
        }
        return i;
    }

    private int getRandomRange(int min, int max) {
        return min + (int) (Math.random() * ((max - min) + 1));
    }

    // start is structured as 2012-01-01 00:00:00 and and as 2013-01-01 00:00:00
    private Timestamp getRandomTS(String startTs, String endTs) {
        long offset = Timestamp.valueOf(startTs).getTime();
        long end = Timestamp.valueOf(endTs).getTime();
        long diff = end - offset + 1;
        return new Timestamp(offset + (long) (Math.random() * diff));
    }
}
