package at.ac.c3pro.chormodel.generation;

import at.ac.c3pro.chormodel.*;
import at.ac.c3pro.chormodel.exceptions.*;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ChoreographyController {

    public static void main(String[] args) throws IOException, JSONException, InterruptedException {
        //To establish a retry logic
        Map<CustomExceptionEnum, Integer> countCustomExceptions = new HashMap<>();

        //Add timestamp as global attribute, so that it can be accessed anywhere without the need of giving it as a parameter everywhere
        GlobalTimestamp.timestamp = getTimestampFormatted();
        MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> graph;

        File dir = OutputHandler.createOutputFolder(null);

        //randomizeConfig(6);

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

    //TODO remove

    static String template = "{\r\n" + //
            "  \"participantCount\": [p],\r\n" + //
            "  \"xorSplitCount\": [x],\r\n" + //
            "  \"andSplitCount\": [a],\r\n" + //
            "  \"maxBranching\": 2,\r\n" + //
            "  \"amountMessageExchange\": x1,\r\n" + //
            "  \"amountHandoverOfWork\": x2,\r\n" + //
            "  \"amountRessourceSharing\": x3,\r\n" + //
            "  \"amountSynchronousActivity\": x4,\r\n" + //
            "  \"visualizeAllCPNs\": false,\r\n" + //
            "  \"visualizePrivModels\": false,\r\n" + //
            "  \"visualizePubModels\": false,\r\n" + //
            "  \"useEasySoundnessChecker\": true,\r\n" + //
            "  \"easySoundnessCheckVisualization\": true,\r\n" + //
            "  \"exceptionRetries\": {\r\n" + //
            "    \"privateModelDisconnected\": 1,\r\n" + //
            "    \"noTraceToEndFound\": 1,\r\n" + //
            "    \"notEasySound\": 10,\r\n" + //
            "    \"twoHOWReceivesForOneParticipant\": 1\r\n" + //
            "  },\r\n" + //
            "  \"easySoundnessCheckDebugLevel\": 1,\r\n" + //
            "  \"doExportsBeforeEasySoundnessCheck\": false,\r\n" + //
            "  \"exportBPMN_UntouchedFromPreviousImplementation\": false,\r\n" + //
            "  \"continueUntilEasySoundModel\": false\r\n" + //
            "}";

    private static Random rand = new Random();

    private static void randomizeConfig(int p) throws IOException {

        int gwBound = p <= 7 ? p <= 4 ? 6 : 8 : 10;

        int xCount = rand.nextInt(gwBound), andCount = rand.nextInt(gwBound);

        int boundInteraction = (xCount + andCount) * 3 + 1;

        String newConfig = template;

        newConfig = newConfig.replace("[a]", andCount + "");
        newConfig = newConfig.replace("[p]", p + "");
        newConfig = newConfig.replace("[x]", xCount + "");

        newConfig = newConfig.replace("x1", rand.nextInt(boundInteraction) + "");
        newConfig = newConfig.replace("x2", (rand.nextInt(p - 1) + 1) + "");
        newConfig = newConfig.replace("x3", rand.nextInt(boundInteraction) + "");
        newConfig = newConfig.replace("x4", rand.nextInt(boundInteraction) + "");

        File configFile = new File("config/config.json");
        FileOutputStream fo = new FileOutputStream(configFile, false);

        fo.write(newConfig.getBytes());
        fo.close();
    }

    private static void runWrapper(File dir, JSONObject configObject, Map<CustomExceptionEnum, Integer> countCustomExceptions) throws IOException, InterruptedException {

        int retriesDisconnected = configObject.getJSONObject("exceptionRetries").getInt("privateModelDisconnected");
        int retriesNoTraceToEnd = configObject.getJSONObject("exceptionRetries").getInt("noTraceToEndFound");
        int retriesTwoHOWReceivesForOneParticipant = configObject.getJSONObject("exceptionRetries").getInt("twoHOWReceivesForOneParticipant");
        int retriesNotEasySound = configObject.getJSONObject("exceptionRetries").getInt("twoHOWReceivesForOneParticipant");

        //Compute if we are on the first try
        boolean firstTry = countCustomExceptions.values().stream().anyMatch(x -> x == 0);

        try {
            run(dir, configObject, firstTry);
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

        } catch (NotEasySoundException e) {
            countCustomExceptions.putIfAbsent(CustomExceptionEnum.NOT_EASY_SOUND, 0);
            countCustomExceptions.put(CustomExceptionEnum.NOT_EASY_SOUND, countCustomExceptions.get(CustomExceptionEnum.NOT_EASY_SOUND) + 1);

            if (countCustomExceptions.get(CustomExceptionEnum.NOT_EASY_SOUND) <= retriesNotEasySound) {
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
    private static void removeFileDir(File dir) throws IOException {
        Path root = Paths.get(dir.getAbsolutePath());

        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });


    }

    private static void run(File dir, JSONObject configObject, boolean firstTry) throws IOException, InterruptedException, PrivateModelDisconnectedException, NoTracesToEndFoundException, TwoHOWReceiveForOneParticipantException, NotEasySoundException {

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

        boolean doExportsBeforeEasySoundnessCheck = configObject.getBoolean("doExportsBeforeEasySoundnessCheck");
        boolean exportBPMN = configObject.getBoolean("exportBPMN_UntouchedFromPreviousImplementation");
        boolean continueGenerationProcessUntilEasySoundModelFound = configObject.getBoolean("continueUntilEasySoundModel");

        if (amountHandoverOfWork >= participantCount) {
            throw new IllegalArgumentException(
                    "The amount of interactions of the type handover-of-work cannot be equal or greater to the amount of participants");
        }

        ChorModelGenerator modelGen;
        SplitTracking splitTracking = SplitTracking.getInstance();

        //If at that point a instance is present that means its from a previous try and still holds information about it
        //Thus we terminate and redo it.
        if (!firstTry) {
            splitTracking.terminate();
            splitTracking = SplitTracking.getInstance();
        }

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
        Map<Role, PrivateModel> privateModelsByRole = exportPrivateModels(choreo, printVisualizationsForPrivModels);

        //Additional Check if any of the private models is disconnected. If yes an exception is thrown and the generation is started again.
        //If the error with disconnectedness still persists (noticed by Janik in the scope of the paper) then it has have to do with the translation to CPN
        checkDisconnectedNess(new ArrayList<>(privateModelsByRole.values()));

        //Specify option to do ADDITIONAL CPEE Export before Easy-Soundness-Check
        if (doExportsBeforeEasySoundnessCheck) {
            Map<Role, IDirectedGraph<Edge<IPublicNode>, IPublicNode>> puMbyRole = new HashMap<>();
            for (Role role : choreo.collaboration.roles) {
                puMbyRole.put(role, choreo.collaboration.R2PuM.get(role).getdigraph());
            }

            // Transform Private Models to a PNML file
            try {
                ChoreographyModelToPNML choreoToCPN = new ChoreographyModelToPNML(privateModelsByRole, true);
                choreoToCPN.printXMLs(printPetriNetVisualizationsSeparateParticipants);
            } catch (IOException | InterruptedException e) {
                System.err.println("Something went wrong during the generation of the PNML representation of the private models");
            }

            ChoreographyModelToCPEE cpeeGenerator = new ChoreographyModelToCPEE(puMbyRole, true);
            cpeeGenerator.run();
        }

        EasySoundnessChecker2 easySoundnessChecker2 = new EasySoundnessChecker2(choreo, easySoundnessCheckVisualization, debugLevelEasySoundnessChecker);
        Map<Role, IPublicModel> easySoundSubgraphs = easySoundnessChecker2.run();

        //Remove null-elements (graphs that are not executable at all)
        Map<Role, IPublicModel> easySoundSubgraphsFiltered = easySoundSubgraphs.entrySet().stream()
                .filter(x -> x.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (!easySoundSubgraphsFiltered.isEmpty()) {
            // Transform Private Models to a PNML file

            //Cast Public Model to private one (downwards compatibility pls)
            Map<Role, PrivateModel> publicModelsCastedToPrivate = new HashMap<>();

            for (Map.Entry<Role, IPublicModel> entry : easySoundSubgraphsFiltered.entrySet()) {

                PublicModel puMToRole = (PublicModel) entry.getValue();

                publicModelsCastedToPrivate.put(entry.getKey(), puMToRole.convertToPrivateModel());
            }


            try {
                ChoreographyModelToPNML choreoToCPN = new ChoreographyModelToPNML(publicModelsCastedToPrivate, false);
                choreoToCPN.printXMLs(printPetriNetVisualizationsSeparateParticipants);
            } catch (IOException | InterruptedException e) {
                System.err.println("Something went wrong during the generation of the PNML representation of the private models");
            }

            //Transform to a CPEE representation again
            ChoreographyModelToCPEE cpeeGenerator = new ChoreographyModelToCPEE(
                    easySoundSubgraphsFiltered.entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry -> entry.getValue().getdigraph())
                            ),
                    false
            );
            cpeeGenerator.run();
        } else if (continueGenerationProcessUntilEasySoundModelFound) {
            throw new NotEasySoundException();
        }


        if (exportBPMN) {
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
        }

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
    private static Map<Role, PrivateModel> exportPrivateModels(Choreography choreo, boolean visualize) throws IOException, InterruptedException {
        FragmentGenerator fragGen = null;

        Map<Role, PrivateModel> rst = new HashMap<>();

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
            rst.put(role, (PrivateModel) prModel);

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

            if (!prModel.getDisconnectedVertices().isEmpty()) {
                throw new PrivateModelDisconnectedException(roleName, "Disconnected Nodes found");
            }
        }
    }
}
