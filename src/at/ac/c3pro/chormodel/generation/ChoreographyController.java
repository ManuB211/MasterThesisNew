package at.ac.c3pro.chormodel.generation;

import at.ac.c3pro.chormodel.*;
import at.ac.c3pro.chormodel.compliance.ComplianceController;
import at.ac.c3pro.chormodel.compliance.CompliancePattern;
import at.ac.c3pro.io.ChoreographyModel2Bpmn;
import at.ac.c3pro.io.ChoreographyModelToCPN;
import at.ac.c3pro.io.Collaboration2Bpmn;
import at.ac.c3pro.io.PrivateModel2Bpmn;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.Interaction;
import at.ac.c3pro.node.Interaction.InteractionType;
import at.ac.c3pro.util.*;
import at.ac.c3pro.util.VisualizationHandler.VisualizationType;
import org.jbpt.utils.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChoreographyController {

    private static final String lineSep = "----------------------------------------------------------\n";

    public static void main(String[] args) throws IOException, JSONException, InterruptedException {
        //Add timestamp as global attribute, so that it can be accessed anywhere without the need of giving it as a parameter everywhere
        GlobalTimestamp.timestamp = getTimestampFormatted();
        MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> graph;

        File dir = OutputHandler.createOutputFolder(null);

        // Read config file
        String configString = "";
        InputStream fileStream = Files.newInputStream(new File("config/config.json").toPath());

        int inputChar;
        while ((inputChar = fileStream.read()) != -1) {
            configString += (char) inputChar;
        }
        fileStream.close();

        JSONObject configObject = new JSONObject(configString);

        // MODEL GENERATOR PARAMETERS
        int participantCount = configObject.getInt("participantCount"); // number of participants
        int xorSplitCount = configObject.getInt("xorSplitCount"); // number of XOR gateways
        int andSplitCount = configObject.getInt("andSplitCount"); // number of AND gateways
        int loopCount = 0; // number of loops
        int maxBranching = configObject.getInt("maxBranching");

        int amountMessageExchange = configObject.getInt("amountMessageExchange");
        int amountHandoverOfWork = configObject.getInt("amountHandoverOfWork");
        int amountRessourceSharing = configObject.getInt("amountRessourceSharing");
        int amountSynchronousActivity = configObject.getInt("amountSynchronousActivity");

        Map<InteractionType, Integer> remainingInteractionTypes = new HashMap<>();

        remainingInteractionTypes.put(InteractionType.MESSAGE_EXCHANGE, amountMessageExchange);
        remainingInteractionTypes.put(InteractionType.HANDOVER_OF_WORK, amountHandoverOfWork);
        remainingInteractionTypes.put(InteractionType.SHARED_RESOURCE, amountRessourceSharing);
        remainingInteractionTypes.put(InteractionType.SYNCHRONOUS_ACTIVITY, amountSynchronousActivity);

        int interactionCount = amountHandoverOfWork + amountMessageExchange + amountRessourceSharing
                + amountSynchronousActivity;

        boolean printPetriNetVisualizationsSeparateParticipants = configObject.getBoolean("visualizeAllCPNs");
        boolean printVisualizationsForPrivModels = configObject.getBoolean("visualizePrivModels");
        boolean printVisualizationsForPubModels = configObject.getBoolean("visualizePubModels");
        boolean useEasySoundnessChecker = configObject.getBoolean("useEasySoundnessChecker");

        ChorModelGenerator modelGen;
        SplitTracking splitTracking = SplitTracking.getInstance();
        ComplianceController complianceController = new ComplianceController();

        complianceController.orderInteractions();
        complianceController.printInteractionOrderWithAffectedRules();

        if (amountHandoverOfWork >= participantCount) {
            throw new IllegalArgumentException(
                    "The amount of interactions of the type handover-of-work cannot be equal or greater to the amount of participants");
        }

        modelGen = new ChorModelGenerator(participantCount, interactionCount, xorSplitCount, andSplitCount,
                loopCount, maxBranching, remainingInteractionTypes);

        // TODO: Put in Constructor?
        modelGen.setEarlyBranchClosing(Boolean.valueOf(false));
        modelGen.setStartWithInteraction(Boolean.valueOf(false));

        // build choreography model
        graph = modelGen.build();
        IOUtils.toFile(GlobalTimestamp.timestamp + "/finished_graph_preCompliance.dot", graph.toDOT()); // first build
        IOUtils.toFile(GlobalTimestamp.timestamp + "/finished_graph_enriched.dot", modelGen.getEnrichedGraph().toDOT()); // enriched

        VisualizationHandler.visualize(VisualizationType.FINISHED_GRAPH_ENRICHED);

        String folder = dir.toString();

        IOUtils.toFile(GlobalTimestamp.timestamp + "/" + GlobalTimestamp.timestamp + "_choreo_model.dot",
                modelGen.getEnrichedGraph().toDOT()); // assigned with compliance rules interactions

        createChoreoInfo(complianceController, folder, modelGen.getInteractions(),
                splitTracking.getNumberOfInteractions());

        ChoreographyModel choreoModel = new ChoreographyModel(modelGen.getEnrichedGraph());
        ChoreographyModel2Bpmn choreo2bpmnIO = new ChoreographyModel2Bpmn(choreoModel,
                "autogen_choreo_model_" + GlobalTimestamp.timestamp, folder);

        // Generate Choreography (incl. all public models / private models)
        ChoreographyGenerator chorGen = new ChoreographyGenerator();
        Choreography choreo = ChoreographyGenerator.generateChoreographyFromModel(choreoModel);


        // Export Models
        exportPublicModels(choreo, printVisualizationsForPubModels);
        List<PrivateModel> privateModels = exportPrivateModels(choreo, printVisualizationsForPrivModels);

        if (useEasySoundnessChecker) {
            EasySoundnessChecker easySoundnessChecker = new EasySoundnessChecker(choreo);
            easySoundnessChecker.run();
        }


        // Transform Private Models to a PNML file
        try {
            ChoreographyModelToCPN choreoToCPN = new ChoreographyModelToCPN(privateModels);
            choreoToCPN.printXMLs(printPetriNetVisualizationsSeparateParticipants);
        } catch (IOException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Transform Models to bpmn
        Collaboration2Bpmn collab2bpmnIO = new Collaboration2Bpmn(choreo.collaboration,
                "autogen_collab_" + GlobalTimestamp.timestamp, folder);

        for (Role role : choreo.collaboration.roles) {
            IPrivateModel prModel = choreo.R2PrM.get(role);
            PrivateModel2Bpmn prModel2bpmn = new PrivateModel2Bpmn(prModel,
                    "autogen_prModel_" + role.name + "_" + GlobalTimestamp.timestamp + ".bpmn", folder);
            prModel2bpmn.buildXML();
        }

        choreo2bpmnIO.buildXML();
        collab2bpmnIO.buildXML();

        complianceController.printComplianceData();
        modelGen.printInteractions();

        splitTracking.terminate();
    }

    /**
     * Exports the public models to the target folder, named after the timestamp the
     * generation was started
     */
    private static void exportPublicModels(Choreography choreo, boolean visualize) throws IOException, InterruptedException {

        //Generate PublicModels folder
        OutputHandler.createOutputFolder("PublicModels");
        String path = GlobalTimestamp.timestamp + "/PublicModels/";

        // Export public model graphs
        for (Role role : choreo.collaboration.roles) {
            IPublicModel puModel = choreo.collaboration.R2PuM.get(role);

            String filename = "puModel_" + role.name + ".dot";

            IOUtils.toFile(path + filename,
                    puModel.getdigraph().toDOT()); // assigned with compliance rules interactions
        }

        if (visualize) {
            VisualizationHandler.visualize(VisualizationType.PUB_MODEL);
        }
    }

    /**
     * Exports the public models to the target folder, named after the timestamp the
     * generation was started
     */
    private static List<PrivateModel> exportPrivateModels(Choreography choreo, boolean visualize) throws IOException, InterruptedException {
        FragmentGenerator fragGen = null;

        List<PrivateModel> rst = new ArrayList<>();

        // Sort roles so we can be sure that the returned list of private models is in
        // order
        List<Role> rolesSorted = new ArrayList<>(choreo.collaboration.roles);
        rolesSorted.sort(Comparator.comparing(Role::getName));

        //Generate PublicModels folder
        OutputHandler.createOutputFolder("PrivateModels");
        String path = GlobalTimestamp.timestamp + "/PrivateModels/";

        // Export private model graphs
        for (Role role : rolesSorted) {
            IPrivateModel prModel = choreo.R2PrM.get(role);
            fragGen = new FragmentGenerator((PrivateModel) prModel);
            prModel = fragGen.enhance();
            rst.add((PrivateModel) prModel);

            String filename = "prModel_" + role.name + ".dot";

            IOUtils.toFile(path + filename,
                    prModel.getdigraph().toDOT()); // assigned with compliance rules interactions
        }

        if (visualize) {
            VisualizationHandler.visualize(VisualizationType.PRIV_MODEL);
        }

        return rst;
    }

    /**
     * Builds the ChoreoInfo, saved as autogen_choreo_info_[timestamp].txt
     */
    private static void createChoreoInfo(ComplianceController complianceController, String folder,
                                         ArrayList<Interaction> interactions, int numberOfInteractions) {
        try (BufferedWriter bw = new BufferedWriter(
                new FileWriter(folder + "/autogen_choreo_info_" + GlobalTimestamp.timestamp + ".txt"))) {
            bw.write(lineSep);
            bw.write("ADDED RULES:\n");
            bw.write(lineSep);
            for (CompliancePattern cr : complianceController.getComplianceRules()) {
                bw.write(complianceController.printRule(cr));
                bw.newLine();
            }
            bw.write(lineSep);
            bw.write("CONFLICTED RULES:\n");
            bw.write(lineSep);
            for (CompliancePattern cr : complianceController.getConflictedRules()) {
                bw.write(complianceController.printRule(cr));
                bw.newLine();
            }
            bw.write(lineSep);
            bw.write("ORDER DEPENDENCIES:\n");
            bw.write(lineSep);
            for (Map.Entry<Interaction, ArrayList<Interaction>> entry : complianceController.getOrderDependencies()
                    .entrySet()) {
                bw.write("Key : " + entry.getKey() + " Value : " + entry.getValue());
                bw.newLine();
            }
            bw.write(lineSep);
            bw.write("UNIVERSAL IAs:\n");
            bw.write(lineSep);
            for (Interaction universalIA : complianceController.getUniversalInteractions()) {
                bw.write("[" + universalIA.getName() + "] ");
            }
            bw.newLine();
            bw.write(lineSep);
            bw.write("EXISTS IAs:\n");
            bw.write(lineSep);
            for (Interaction existIA : complianceController.getExistInteractions()) {
                bw.write("[" + existIA.getName() + "] ");
            }
            bw.newLine();
            bw.write(lineSep);
            bw.write("INTERACTION ORDER:\n");
            bw.write(lineSep);
            for (Interaction ia : complianceController.getInteractionOrder()) {
                bw.write(ia + " - related rules: ");
                for (CompliancePattern cr : complianceController.getAffectedRules(ia)) {
                    bw.write(cr.getLabel() + " ");
                }
                bw.newLine();
            }
            bw.write(lineSep);
            bw.write("INTERACTIONS:\n");
            bw.write(lineSep);
            System.out.println(interactions.size());
            for (Interaction ia : interactions) {
                if (ia == null) {
                    System.out.println("asdasd");
                } else {
                    System.out.println(ia);
                    System.out.println(ia.getName());
                    System.out.println(ia.getParticipant1());
                    System.out.println(ia.getParticipant2());
                    System.out.println(ia.getMessage());
                    System.out.println(ia.getMessage().getId());

                    bw.write(ia.getName() + ": " + ia.getParticipant1().name + " -> " + ia.getParticipant2().name + " "
                            + ia.getMessage().name + " " + ia.getMessage() + " : " + ia.getInteractionType());
                    bw.newLine();
                }
            }
            bw.write(lineSep);
            bw.write("Number Of Interactions: " + numberOfInteractions);
            bw.newLine();
            bw.write(lineSep);
            System.out.println("Done");

        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Returns a string depicting the current timestamp as a string, applicable for
     * all OS. This is used to have a similar naming convention for all the output
     * files for a single run of the algorithm
     *
     * @return the current timestamp as a formatted String
     */
    private static String getTimestampFormatted() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Date date = new Date();
        date.setTime(timestamp.getTime());
        return new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(date);
    }
}
