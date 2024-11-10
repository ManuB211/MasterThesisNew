package at.ac.c3pro.chormodel.generation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpt.utils.IOUtils;

import at.ac.c3pro.chormodel.Choreography;
import at.ac.c3pro.chormodel.ChoreographyModel;
import at.ac.c3pro.chormodel.IPrivateModel;
import at.ac.c3pro.chormodel.IPublicModel;
import at.ac.c3pro.chormodel.MultiDirectedGraph;
import at.ac.c3pro.chormodel.PrivateModel;
import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.chormodel.compliance.ComplianceController;
import at.ac.c3pro.chormodel.compliance.CompliancePattern;
import at.ac.c3pro.chormodel.compliance.Exists;
import at.ac.c3pro.chormodel.compliance.LeadsTo;
import at.ac.c3pro.chormodel.compliance.Precedes;
import at.ac.c3pro.chormodel.compliance.Universal;
import at.ac.c3pro.io.ChoreographyModel2Bpmn;
import at.ac.c3pro.io.Collaboration2Bpmn;
import at.ac.c3pro.io.PrivateModel2Bpmn;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.Interaction;
import at.ac.c3pro.node.Interaction.InteractionType;
import at.ac.c3pro.util.ChoreographyGenerator;

public class ChoreographyController {

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
	private static String lineSep = "----------------------------------------------------------\n";

	private static String formattedDate = getTimestampFormatted();
	private static ArrayList<CompliancePattern> complianceRules = new ArrayList<>();

	public static void main(String[] args) throws IOException {
		MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> graph = new MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode>();
		Boolean buildSuccess = false;
		int buildIterationCount = 0;
		BuildAnaylse buildAnaylse = null;

		File dir = createOutputFolder();

		// MODEL GENERATOR PARAMETERS
		int participantCount = 2; // number of participants
		int xorSplitCount = 1; // number of XOR gateways
		int andSplitCount = 2; // number of AND gateways
		int loopCount = 0; // number of loops
		int maxBranching = 3;

		int amountMessageExchange = 2;
		int amountHandoverOfWork = 1;
		int amountRessourceSharing = 1;
		int amountSynchronousActivity = 1;

		Map<InteractionType, Integer> remainingInteractionTypes = new HashMap<>();
		remainingInteractionTypes.put(InteractionType.MESSAGE_EXCHANGE, 2);
		remainingInteractionTypes.put(InteractionType.HANDOVER_OF_WORK, 1);
		remainingInteractionTypes.put(InteractionType.SHARED_RESOURCE, 1);
		remainingInteractionTypes.put(InteractionType.SYNCHRONOUS_ACTIVITY, 1);

		int interactionCount = amountHandoverOfWork + amountMessageExchange + amountRessourceSharing
				+ amountSynchronousActivity;

		// Here was the code from createInteractionList()

		ChorModelGenerator modelGen;
		SplitTracking splitTracking = SplitTracking.getInstance();
		ComplianceController complianceController = new ComplianceController();

//		for (CompliancePattern cr : complianceRules) {
//			complianceController.addRule(cr);
//		}

		complianceController.orderInteractions();
		complianceController.printInteractionOrderWithAffectedRules();

		if (amountHandoverOfWork >= participantCount) {
			throw new IllegalArgumentException(
					"The amount of interactions of the type handover-of-work cannot be equal or greater to the amount of participants");
		}

		while (!buildSuccess) {
			long startTime = System.currentTimeMillis();
			modelGen = new ChorModelGenerator(participantCount, interactionCount, xorSplitCount, andSplitCount,
					loopCount, maxBranching, formattedDate, remainingInteractionTypes);

			// TODO: Put in Constructor?
			modelGen.setEarlyBranchClosing(false);
			modelGen.setStartWithInteraction(false);

			buildIterationCount++;

			// build choreography model
			graph = modelGen.build(formattedDate);
			IOUtils.toFile(formattedDate + "/finished_graph_preCompliance.dot", graph.toDOT()); // first build
			IOUtils.toFile(formattedDate + "/finished_graph_enriched.dot", modelGen.getEnrichedGraph().toDOT()); // enriched
																													// with
																													// message
			// flow

			// if compliance rules are defined, do interaction assignment
			if (complianceRules.isEmpty()) {
				buildSuccess = true;
			} else {
				complianceController.reloadSplitTracking();
				buildSuccess = complianceController.assign();
			}

			if (buildSuccess) {
				long stopTime = System.currentTimeMillis();
				long elapsedTime = stopTime - startTime;

				String folder = dir.toString();

				IOUtils.toFile(formattedDate + "/" + formattedDate + "_choreo_model.dot",
						modelGen.getEnrichedGraph().toDOT()); // assigned with compliance rules interactions

				createChoreoInfo(complianceController, folder, modelGen.getInteractions(),
						splitTracking.getNumberOfInteractions(), buildIterationCount);

				ChoreographyModel choreoModel = new ChoreographyModel(modelGen.getEnrichedGraph());
				ChoreographyModel2Bpmn choreo2bpmnIO = new ChoreographyModel2Bpmn(choreoModel,
						"autogen_choreo_model_" + formattedDate, folder);

				// Generate Choreography (incl. all public models / private models)

				ChoreographyGenerator chorGen = new ChoreographyGenerator();
				Choreography choreo = chorGen.generateChoreographyFromModel(choreoModel);

				// Export Models
				exportPublicModels(choreo);
				exportPrivateModels(choreo);

				// Transform Models to bpmn
				Collaboration2Bpmn collab2bpmnIO = new Collaboration2Bpmn(choreo.collaboration,
						"autogen_collab_" + formattedDate, folder);

				for (Role role : choreo.collaboration.roles) {
					IPrivateModel prModel = choreo.R2PrM.get(role);
					PrivateModel2Bpmn prModel2bpmn = new PrivateModel2Bpmn(prModel,
							"autogen_prModel_" + role.name + "_" + formattedDate + ".bpmn", folder);
					prModel2bpmn.buildXML();
				}

				choreo2bpmnIO.buildXML();
				collab2bpmnIO.buildXML();

				// if interaction assignment failed, increase interactionCount by one every 10
				// iterations
			} else if (!buildSuccess && (buildIterationCount % 10 == 0)) {
				interactionCount = (int) (interactionCount * 1.1);
			}

			if (!buildSuccess && buildIterationCount > 4) {
				System.out.println("HIER DRIN");
				break;
			}

			complianceController.printComplianceData();
			System.out.println("Success?: " + buildSuccess);
			System.out.println("buildIterationCount: " + buildIterationCount);
			modelGen.printInteractions();

			splitTracking.terminate();
		}

	}

	/**
	 * Exports the public models to the target folder, named after the timestamp the
	 * generation was started
	 */
	private static void exportPublicModels(Choreography choreo) {
		// Export public model graphs
		for (Role role : choreo.collaboration.roles) {
			IPublicModel puModel = choreo.collaboration.R2PuM.get(role);
			IOUtils.toFile(formattedDate + "/" + formattedDate + "_puModel_" + role.name + ".dot",
					puModel.getdigraph().toDOT()); // assigned with compliance rules interactions
		}
	}

	/**
	 * Exports the public models to the target folder, named after the timestamp the
	 * generation was started
	 */
	private static void exportPrivateModels(Choreography choreo) {
		FragmentGenerator fragGen = null;

		// Export private model graphs
		for (Role role : choreo.collaboration.roles) {
			IPrivateModel prModel = choreo.R2PrM.get(role);
			fragGen = new FragmentGenerator((PrivateModel) prModel, formattedDate);
			prModel = fragGen.enhance();

			IOUtils.toFile(formattedDate + "/" + formattedDate + "_prModel_" + role.name + ".dot",
					prModel.getdigraph().toDOT()); // assigned with compliance rules interactions
		}
	}

	/**
	 * Builds the ChoreoInfo, saved as autogen_choreo_info_[timestamp].txt
	 */
	private static void createChoreoInfo(ComplianceController complianceController, String folder,
			ArrayList<Interaction> interactions, int numberOfInteractions, int buildIterationCount) {
		try (BufferedWriter bw = new BufferedWriter(
				new FileWriter(folder + "/autogen_choreo_info_" + formattedDate + ".txt"))) {
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
				;
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
			bw.write("Number Of Iterations: " + buildIterationCount);
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

	/**
	 * @throws Exception
	 * 
	 */
	private static File createOutputFolder() throws IOException {
		File dir = new File("target/" + formattedDate);

		if (!dir.exists()) {
			boolean created = dir.mkdir();
			if (created) {
				System.out.println("Directory created successfully!");
			} else {
				throw new IOException("Failed to create the directory.");
			}
		} else {
			System.out.println("Directory already exists.");
		}

		return dir;
	}

	/**
	 * Defines Interaction List for Compliance Rules TODO: Needed?
	 * 
	 * @returns List of interactions
	 */
	private List<Interaction> createInteractionListAndDefineComplianceRules() {

		// DEFINE INTERACTIONS FOR COMPLIANCE RULES
		Interaction a = new Interaction();
		a.setName("IA A");

		Interaction b = new Interaction();
		b.setName("IA B");

		Interaction c = new Interaction();
		c.setName("IA C");

		Interaction d = new Interaction();
		d.setName("IA D");

		Interaction e = new Interaction();
		e.setName("IA E");

		Interaction f = new Interaction();
		f.setName("IA F");

		Interaction g = new Interaction();
		g.setName("IA G");

		Interaction h = new Interaction();
		h.setName("IA H");

		Interaction i = new Interaction();
		i.setName("IA I");

		ArrayList<Interaction> interactions = new ArrayList<Interaction>();
		for (int x = 0; x < 10; x++) {
			interactions.add(new Interaction());
		}

		// Define and add complianceRules
		complianceRules.add(new LeadsTo("r1", a, b));
		complianceRules.add(new LeadsTo("r2", a, c));
		complianceRules.add(new LeadsTo("r3", b, f));
		complianceRules.add(new Precedes("r4", d, h));
		complianceRules.add(new LeadsTo("r5", g, d));
		complianceRules.add(new Precedes("r6", b, c));
		complianceRules.add(new Universal("r7", b));
		complianceRules.add(new Precedes("r8", c, a));
		complianceRules.add(new Universal("r9", e));
		complianceRules.add(new Exists("r10", i));

		return interactions;
	}

	/**
	 * Defines the compliance rules, which are
	 */
	private void defineComplianceRules() {

	}

}
