package at.ac.c3pro.chormodel.generation;

import at.ac.c3pro.chormodel.*;
import at.ac.c3pro.chormodel.exceptions.CustomExceptionEnum;
import at.ac.c3pro.chormodel.exceptions.NoTracesToEndFoundException;
import at.ac.c3pro.chormodel.exceptions.PrivateModelDisconnectedException;
import at.ac.c3pro.chormodel.exceptions.TwoHOWReceiveForOneParticipantException;
import at.ac.c3pro.io.*;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.IPublicNode;
import at.ac.c3pro.node.Interaction.InteractionType;
import at.ac.c3pro.util.*;
import at.ac.c3pro.util.VisualizationHandler.VisualizationType;
import org.jbpt.graph.abs.IDirectedGraph;
import org.jbpt.utils.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChoreographyController {

    public static void main(String[] args) throws IOException, JSONException, InterruptedException {
        //To establish a retry logic
        Map<CustomExceptionEnum, Integer> countCustomExceptions = new HashMap<>();

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

        runWrapper(dir, configObject, countCustomExceptions);
    }

    private static void runWrapper(File dir, JSONObject configObject, Map<CustomExceptionEnum, Integer> countCustomExceptions) throws IOException, InterruptedException {

        int retriesDisconnected = configObject.getJSONObject("exceptionRetries").getInt("privateModelDisconnected");
        int retriesNoTraceToEnd = configObject.getJSONObject("exceptionRetries").getInt("noTraceToEndFound");

        //Hardcoded as this rarely happens
        int retriesTwoHOWReceivesForOneParticipant = 5;

        try {
            run(dir, configObject);
        } catch (PrivateModelDisconnectedException e) {
            countCustomExceptions.putIfAbsent(CustomExceptionEnum.PRIVATE_MODEL_DISCONNECTED, 0);
            countCustomExceptions.put(CustomExceptionEnum.PRIVATE_MODEL_DISCONNECTED, countCustomExceptions.get(CustomExceptionEnum.PRIVATE_MODEL_DISCONNECTED) + 1);

            if (countCustomExceptions.get(CustomExceptionEnum.PRIVATE_MODEL_DISCONNECTED) <= retriesDisconnected) {
                System.err.println("Attempting another try");
                removeFileDir(dir);
                runWrapper(dir, configObject, countCustomExceptions);
            }
        } catch (NoTracesToEndFoundException e) {
            countCustomExceptions.putIfAbsent(CustomExceptionEnum.NO_TRACES_TO_END_FOUND, 0);
            countCustomExceptions.put(CustomExceptionEnum.NO_TRACES_TO_END_FOUND, countCustomExceptions.get(CustomExceptionEnum.NO_TRACES_TO_END_FOUND) + 1);

            if (countCustomExceptions.get(CustomExceptionEnum.NO_TRACES_TO_END_FOUND) <= retriesNoTraceToEnd) {
                removeFileDir(dir);
                System.err.println("Attempting another try");
                runWrapper(dir, configObject, countCustomExceptions);
            }
        } catch (TwoHOWReceiveForOneParticipantException e) {
            countCustomExceptions.putIfAbsent(CustomExceptionEnum.TWO_HOW_RECEIVE_ONE_PARTICIPANT, 0);
            countCustomExceptions.put(CustomExceptionEnum.TWO_HOW_RECEIVE_ONE_PARTICIPANT, countCustomExceptions.get(CustomExceptionEnum.TWO_HOW_RECEIVE_ONE_PARTICIPANT) + 1);

            if (countCustomExceptions.get(CustomExceptionEnum.TWO_HOW_RECEIVE_ONE_PARTICIPANT) <= retriesTwoHOWReceivesForOneParticipant) {
                removeFileDir(dir);
                System.err.println("Attempting another try");
                dir = OutputHandler.createOutputFolder(null);
                runWrapper(dir, configObject, countCustomExceptions);
            }

        } catch (StackOverflowError e) {
            System.err.println("Attempting another try");
            removeFileDir(dir);
            runWrapper(dir, configObject, countCustomExceptions);
        }
    }

    //When exception is thrown the directory needs to be cleared so that on a retry there is no exception
    private static void removeFileDir(File dir) {
        File[] content = dir.listFiles();

        if (content != null) {
            for (File file : content) {
                removeFileDir(file);
            }
        }
        dir.delete();
    }

    private static void run(File dir, JSONObject configObject) throws IOException, InterruptedException, PrivateModelDisconnectedException, NoTracesToEndFoundException, TwoHOWReceiveForOneParticipantException {

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
        boolean easySoundnessCheckVisualization = configObject.getBoolean("easySoundnessCheckVisualization");
        int debugLevelEasySoundnessChecker = configObject.getInt("easySoundnessCheckDebugLevel");

        boolean doCPEEExportBeforeEasySoundnessCheck = configObject.getBoolean("doCPEEExportBeforeEasySoundnessCheck");

        if (amountHandoverOfWork >= participantCount) {
            throw new IllegalArgumentException(
                    "The amount of interactions of the type handover-of-work cannot be equal or greater to the amount of participants");
        }

        ChorModelGenerator modelGen;
        SplitTracking splitTracking = SplitTracking.getInstance();

        modelGen = new ChorModelGenerator(participantCount, interactionCount, xorSplitCount, andSplitCount,
                loopCount, maxBranching, remainingInteractionTypes);

        // TODO: Put in Constructor?
        modelGen.setEarlyBranchClosing(Boolean.valueOf(false));
        modelGen.setStartWithInteraction(Boolean.valueOf(false));

        // build choreography model
        MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> graph = modelGen.build();
        IOUtils.toFile(GlobalTimestamp.timestamp + "/finished_graph_preCompliance.dot", graph.toDOT()); // first build
        IOUtils.toFile(GlobalTimestamp.timestamp + "/finished_graph_enriched.dot", modelGen.getEnrichedGraph().toDOT()); // enriched

        /**
         * We need to save the XOR nodes in the choreography model, that have a direct
         * connection to its merge. Otherwise they will be filtered out during the graph
         * reduction, resulting in graphs that are technically different from the
         * original workflow
         */
        RpstModel<Edge<IChoreographyNode>, IChoreographyNode> tempRPSTModelForDirectXORconnections = new RpstModel<>(graph, "tempRPSTModelForDirectXORConnections");
        List<IChoreographyNode> xorNodeWithDirectConnectionToMerge = tempRPSTModelForDirectXORconnections.getAllXORsWithDirectConnectionToMerge();

        VisualizationHandler.visualize(VisualizationType.FINISHED_GRAPH_ENRICHED);

        String folder = dir.toString();

        IOUtils.toFile(GlobalTimestamp.timestamp + "/" + GlobalTimestamp.timestamp + "_choreo_model.dot",
                modelGen.getEnrichedGraph().toDOT()); // assigned with compliance rules interactions

        ChoreographyModel choreoModel = new ChoreographyModel(modelGen.getEnrichedGraph());
        ChoreographyModel2Bpmn choreo2bpmnIO = new ChoreographyModel2Bpmn(choreoModel,
                "autogen_choreo_model_" + GlobalTimestamp.timestamp, folder);

        // Generate Choreography (incl. all public models / private models)
        Choreography choreo = ChoreographyGenerator.generateChoreographyFromModel(choreoModel, xorNodeWithDirectConnectionToMerge);


        // Export Models
        exportPublicModels(choreo, printVisualizationsForPubModels);
        List<PrivateModel> privateModels = exportPrivateModels(choreo, printVisualizationsForPrivModels);

        //Additional Check if any of the private models is disconnected. If yes an exception is thrown and the generation is started again.
        //If the error with disconnectedness still persists (noticed by Janik in the scope of the paper) then it has have to do with the translation to CPN
        checkDisconnectedNess(privateModels);

        //Specify option to do ADDITIONAL CPEE Export before Easy-Soundness-Check
        if (doCPEEExportBeforeEasySoundnessCheck) {
            Map<Role, IDirectedGraph<Edge<IPublicNode>, IPublicNode>> puMbyRole = new HashMap<>();
            for (Role role : choreo.collaboration.roles) {
                puMbyRole.put(role, choreo.collaboration.R2PuM.get(role).getdigraph());
            }

            ChoreographyModelToCPEE cpeeGenerator = new ChoreographyModelToCPEE(puMbyRole);
            cpeeGenerator.run();
        }

        EasySoundnessChecker2 easySoundnessChecker2 = new EasySoundnessChecker2(choreo, easySoundnessCheckVisualization, debugLevelEasySoundnessChecker);
        easySoundnessChecker2.run();


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

//        complianceController.printComplianceData();
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

    /**
     * Checks a list of private models for disconnectedness.
     * This is done using a BFS and comparing the nodes from the BFS-result to the nodes of the model
     * If a disconnected model is found the methos throws a PrivateModelDisconnectedException
     */
    private static void checkDisconnectedNess(List<PrivateModel> prModels) throws PrivateModelDisconnectedException {
        for (PrivateModel prModel : prModels) {

            //Get Role name from prModelName (Name is always P_x_prModel)
            String[] prModelNameSplit = prModel.getName().split("_");
            String roleName = prModelNameSplit[0] + "_" + prModelNameSplit[1];

            //TODO Check if trustworthy
            if (!prModel.getDisconnectedVertices().isEmpty()) {
                throw new PrivateModelDisconnectedException(roleName, "Disconnected Nodes found");
            }

            /*
            IDirectedGraph<Edge<IPrivateNode>, IPrivateNode> graph = prModel.getdigraph();
            List<IPrivateNode> nodes = new ArrayList<>(graph.getVertices());

            IPrivateNode endNode = nodes.stream().filter(node -> node instanceof Event && node.getName().equals("end")).collect(Collectors.toList()).get(0);

            List<IPrivateNode> bfsResult = GraphHelper.performBackwardsBFSPrivate(graph, endNode, false);
            */


        }
    }
}
