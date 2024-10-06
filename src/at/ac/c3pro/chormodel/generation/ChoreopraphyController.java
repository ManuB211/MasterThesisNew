package at.ac.c3pro.chormodel.generation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Map;
import at.ac.c3pro.util.*;

import org.jbpt.utils.IOUtils;

import at.ac.c3pro.chormodel.Choreography;
import at.ac.c3pro.chormodel.ChoreographyModel;
import at.ac.c3pro.chormodel.IPrivateModel;
import at.ac.c3pro.chormodel.IPublicModel;
import at.ac.c3pro.chormodel.MultiDirectedGraph;
import at.ac.c3pro.chormodel.PrivateModel;
import at.ac.c3pro.chormodel.PublicModel;
import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.chormodel.compliance.CoAbsent;
import at.ac.c3pro.chormodel.compliance.CoExists;
import at.ac.c3pro.chormodel.compliance.CoRequisite;
import at.ac.c3pro.chormodel.compliance.ComplianceController;
import at.ac.c3pro.chormodel.compliance.CompliancePattern;
import at.ac.c3pro.chormodel.compliance.ComplianceRulesGenerator;
import at.ac.c3pro.chormodel.compliance.Exclusive;
import at.ac.c3pro.chormodel.compliance.Exists;
import at.ac.c3pro.chormodel.compliance.LeadsTo;
import at.ac.c3pro.chormodel.compliance.PLeadsTo;
import at.ac.c3pro.chormodel.compliance.Precedes;
import at.ac.c3pro.chormodel.compliance.Universal;
import at.ac.c3pro.chormodel.compliance.XLeadsTo;
import at.ac.c3pro.io.ChoreographyModel2Bpmn;
import at.ac.c3pro.io.Collaboration2Bpmn;
import at.ac.c3pro.io.PrivateModel2Bpmn;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.Interaction;
import at.ac.c3pro.tests.PrivateModel2BpmnTest;
import at.ac.c3pro.util.ChoreographyGenerator;

public class ChoreopraphyController {
	
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
	private static String lineSep = "----------------------------------------------------------\n";


	public static void main(String[] args) throws IOException {
		MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> graph = new MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode>();
		Boolean buildSuccess = false;
		int buildIterationCount = 0;
		BuildAnaylse buildAnaylse = null;
		ArrayList<CompliancePattern> complianceRules = new ArrayList<>();

		// MODEL GENERATOR PARAMETERS
		int interactionCount = 28;		// number of interactions
		int participantCount = 3;		// number of participants
		int xorSplitCount = 3;			// number of XOR gateways
		int andSplitCount = 5;			// number of AND gateways
		int loopCount = 0;				// number of loops
		int maxBranching = 3;

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
		for (int x = 0; x < 10; x++ ) {
			interactions.add(new Interaction());
		}



		/*
		 * DEFINE COMPLIANCE RULES:
		 * - P leadsTo Q
		 * - P precedes Q
		 * - P Exists
		 * - P Universal
		 */
//		complianceRules.add(new LeadsTo("r1", a, b));
		//complianceRules.add(new LeadsTo("r2", a, c));
//		complianceRules.add(new LeadsTo("r3", b, f));
		//complianceRules.add(new Precedes("r4", d, h));
//		complianceRules.add(new LeadsTo("r5", g, d));
		//complianceRules.add(new Precedes("r6", b, c));
		//complianceRules.add(new Universal("r7", b));
//		complianceRules.add(new Precedes("r8", c, a));
//		complianceRules.add(new Universal("r9", e));
		//complianceRules.add(new Exists("r10", i));


		ChorModelGenerator modelGen;
		SplitTracking splitTracking = SplitTracking.getInstance();
		ComplianceController complianceController = new ComplianceController();

//		for (CompliancePattern cr : complianceRules) {
//			complianceController.addRule(cr);
//		}

		complianceController.orderInteractions();
		complianceController.printInteractionOrderWithAffectedRules();

		while (!buildSuccess) {
			long startTime = System.currentTimeMillis();
			modelGen = new ChorModelGenerator(
					participantCount,
					interactionCount,
					xorSplitCount,
					andSplitCount,
					loopCount,
					maxBranching);
			modelGen.setEarlyBranchClosing(false);
			modelGen.setStartWithInteraction(false);


			buildIterationCount++;

			// build choreography model
			graph = modelGen.build();
			IOUtils.toFile("finished_graph_preCompliance.dot", graph.toDOT()); // first build
			IOUtils.toFile("finished_graph_enriched.dot", modelGen.getEnrichedGraph().toDOT()); // enriched with message flow

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

				Timestamp timestamp = new Timestamp(System.currentTimeMillis());
				String timeString = timestamp.toString();

				//create output folder
				File dir = new File("target/" + timeString);
				dir.mkdir();
				String folder = dir.toString();


				IOUtils.toFile(timeString + "/" + sdf.format(timestamp) + "_choreo_model.dot", modelGen.getEnrichedGraph().toDOT()); // assigned with compliance rules interactions

				try (BufferedWriter bw = new BufferedWriter(new FileWriter(folder + "/autogen_choreo_info_" + timeString + ".txt"))) {
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
					for (Map.Entry<Interaction, ArrayList<Interaction>> entry : complianceController.getOrderDependencies().entrySet()) {
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
						bw.newLine();;
					}
					bw.write(lineSep);
					bw.write("INTERACTIONS:\n");
					bw.write(lineSep);
					System.out.println(modelGen.getInteractions().size());
					for (Interaction ia : modelGen.getInteractions()) {
						if (ia == null) {
							System.out.println("asdasd");
						} else {
							System.out.println(ia);
							System.out.println(ia.getName());
							System.out.println(ia.getSender());
							System.out.println(ia.getReceiver());
							System.out.println(ia.getMessage());
							System.out.println(ia.getMessage().getId());

							bw.write(ia.getName() + ": " + ia.getSender().name + " -> " + ia.getReceiver().name + " " + ia.getMessage().name + " " + ia.getMessage());
							bw.newLine();
						}
					}
					bw.write(lineSep);
					bw.write("Number Of Interactions: " + splitTracking.getNumberOfInteractions());
					bw.newLine();
					bw.write(lineSep);
					bw.write("Number Of Iterations: " + buildIterationCount);
					bw.newLine();
					bw.write(lineSep);
					System.out.println("Done");



				} catch (IOException e1) {
					e1.printStackTrace();
				}

				ChoreographyModel choreoModel = new ChoreographyModel(modelGen.getEnrichedGraph());
				ChoreographyModel2Bpmn choreo2bpmnIO = new ChoreographyModel2Bpmn(choreoModel, "autogen_choreo_model_" + sdf.format(timestamp), folder);

				// Generate Choreography (incl. all public models / private models)

				ChoreographyGenerator chorGen = new ChoreographyGenerator();
				Choreography choreo = chorGen.generateChoreographyFromModel(choreoModel);

				// Export public model graphs
				for (Role role : choreo.collaboration.roles) {
					IPublicModel puModel = choreo.collaboration.R2PuM.get(role);
					IOUtils.toFile(timeString + "/" + sdf.format(timestamp) + "_puModel_" + role.name + ".dot", puModel.getdigraph().toDOT()); // assigned with compliance rules interactions
				}

				FragmentGenerator fragGen = null;

				// Export private model graps
				for (Role role : choreo.collaboration.roles) {
					IPrivateModel prModel = choreo.R2PrM.get(role);
					fragGen = new FragmentGenerator((PrivateModel) prModel);
					prModel = fragGen.enhance();

					IOUtils.toFile(timeString + "/" + sdf.format(timestamp) + "_prModel_" + role.name + ".dot", prModel.getdigraph().toDOT()); // assigned with compliance rules interactions
				}

				//
				Collaboration2Bpmn collab2bpmnIO = new Collaboration2Bpmn(choreo.collaboration, "autogen_collab_" + sdf.format(timestamp), folder);



				for (Role role : choreo.collaboration.roles) {
					IPrivateModel prModel = choreo.R2PrM.get(role);
					PrivateModel2Bpmn prModel2bpmn = new PrivateModel2Bpmn(prModel, "autogen_prModel_" + role.name + "_" + sdf.format(timestamp) + ".bpmn", folder);
					prModel2bpmn.buildXML();
				}


				choreo2bpmnIO.buildXML();
				collab2bpmnIO.buildXML();


				// if interaction assignment failed, increase interactionCount by one every 10 iterations
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


	private static void writeAnalysis(BuildAnaylse buildAnaylse) {
		BufferedWriter bw = null;
		FileWriter fw = null;

		try {

			fw = new FileWriter("/Users/fb/Documents/test.txt", true);
			bw = new BufferedWriter(fw);
			//bw.write("iterations,interactions,actinteractions,xors,ands,maxBranching,cr_ias,crs,success,duration\n");
			bw.write(buildAnaylse.iterations + "," + buildAnaylse.interactions + "," + buildAnaylse.actInteractions + "," + buildAnaylse.xors + "," + buildAnaylse.ands + "," + buildAnaylse.maxBranching + "," + buildAnaylse.crIas + "," + buildAnaylse.crs + "," + buildAnaylse.success + "," + buildAnaylse.duration + "\n");

			System.out.println("Done");

		} catch (IOException e) {

			e.printStackTrace();

		} finally {

			try {

				if (bw != null)
					bw.close();

				if (fw != null)
					fw.close();

			} catch (IOException ex) {

				ex.printStackTrace();

			}
		}

	}

//	private static BuildAnaylse genAnalyse(int xors, int ands, int branching, int cr_ias, int cr_number) {
//
//		MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> graph = new MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode>();
//		Boolean buildSuccess = false;
//		int buildIterationCount = 0;
//		BuildAnaylse buildAnaylse = null;
//		ArrayList<CompliancePattern> complianceRules = new ArrayList<>();
//
//		// MODEL GENERATOR PARAMETERS
//		int interactionCount = 10;		// number of interactions
//		int participantCount = 2;		// number of participants
//		int xorSplitCount = xors;			// number of XOR gateways
//		int andSplitCount = ands;			// number of AND gateways
//		int loopCount = 0;				// number of loops
//		int maxBranching = branching;
//
//		// DEFINE INTERACTIONS FOR COMPLIANCE RULES
//		Interaction a = new Interaction();
//		a.setName("IA A");
//
//		Interaction b = new Interaction();
//		b.setName("IA B");
//
//		Interaction c = new Interaction();
//		c.setName("IA C");
//
//		Interaction d = new Interaction();
//		d.setName("IA D");
//
//		Interaction e = new Interaction();
//		e.setName("IA E");
//
//		Interaction f = new Interaction();
//		f.setName("IA F");
//
//		Interaction g = new Interaction();
//		g.setName("IA G");
//
//		Interaction h = new Interaction();
//		h.setName("IA H");
//
//		Interaction i = new Interaction();
//		i.setName("IA I");
//
//		ArrayList<Interaction> interactions = new ArrayList<Interaction>();
//		for (int x = 0; x < 10; x++ ) {
//			interactions.add(new Interaction());
//		}
//
//
//
//		/*
//		 * DEFINE COMPLIANCE RULES:
//		 * - P leadsTo Q
//		 * - P precedes Q
//		 * - P Exists
//		 * - P Universal
//		 */
////		complianceRules.add(new LeadsTo("r1", a, b));
////		complianceRules.add(new LeadsTo("r2", a, c));
////		complianceRules.add(new LeadsTo("r3", b, f));
////		complianceRules.add(new Precedes("r4", d, h));
////		complianceRules.add(new LeadsTo("r5", g, d));
////		complianceRules.add(new Precedes("r6", b, c));
////		complianceRules.add(new Universal("r7", b));
////		complianceRules.add(new Precedes("r8", c, a));
////		complianceRules.add(new Universal("r9", e));
////		complianceRules.add(new Exists("r10", i));
//
//
////		// CRG can be used for generating a set of random compliance rules
////		ComplianceRulesGenerator crg = new ComplianceRulesGenerator(cr_ias, cr_number);
////		complianceRules = crg.generate();
////
////
////		ChorModelGenerator modelGen;
////		SplitTracking splitTracking = SplitTracking.getInstance();
////		ComplianceController complianceController = new ComplianceController();
////
//////		for (CompliancePattern cr : complianceRules) {
//////			complianceController.addRule(cr);
//////		}
////
////		complianceController.orderInteractions();
////		complianceController.printInteractionOrderWithAffectedRules();
//
//		while (!buildSuccess) {
//			long startTime = System.currentTimeMillis();
//			modelGen = new ChorModelGenerator(
//					participantCount,
//					interactionCount,
//					xorSplitCount,
//					andSplitCount,
//					loopCount,
//					maxBranching);
//			modelGen.setEarlyBranchClosing(false);
//			modelGen.setStartWithInteraction(false);
//
//
//			buildIterationCount++;
//
//			// build choreography model
//			graph = modelGen.build();
//			IOUtils.toFile("finished_graph_preCompliance.dot", graph.toDOT()); // first build
//			IOUtils.toFile("finished_graph_enriched.dot", modelGen.getEnrichedGraph().toDOT()); // enriched with message flow
//
//			// if compliance rules are defined, do interaction assignment
//			if (complianceRules.isEmpty()) {
//				buildSuccess = true;
//			} else {
//				complianceController.reloadSplitTracking();
//				buildSuccess = complianceController.assign();
//			}
//
//			if (buildSuccess) {
//				long stopTime = System.currentTimeMillis();
//				long elapsedTime = stopTime - startTime;
//				Timestamp timestamp = new Timestamp(System.currentTimeMillis());
//				IOUtils.toFile(sdf.format(timestamp) + "_choreo_model" + ".dot", modelGen.getEnrichedGraph().toDOT()); // assigned with compliance rules interactions
//				try (BufferedWriter bw = new BufferedWriter(new FileWriter("target/autogen_choreo_info_" + sdf.format(timestamp) + ".txt"))) {
//					bw.write(lineSep);
//					bw.write("ADDED RULES:\n");
//					bw.write(lineSep);
//					for (CompliancePattern cr : complianceController.getComplianceRules()) {
//						bw.write(complianceController.printRule(cr));
//						bw.newLine();
//					}
//					bw.write(lineSep);
//					bw.write("CONFLICTED RULES:\n");
//					bw.write(lineSep);
//					for (CompliancePattern cr : complianceController.getConflictedRules()) {
//						bw.write(complianceController.printRule(cr));
//						bw.newLine();
//					}
//					bw.write(lineSep);
//					bw.write("ORDER DEPENDENCIES:\n");
//					bw.write(lineSep);
//					for (Map.Entry<Interaction, ArrayList<Interaction>> entry : complianceController.getOrderDependencies().entrySet()) {
//						bw.write("Key : " + entry.getKey() + " Value : " + entry.getValue());
//						bw.newLine();
//					}
//					bw.write(lineSep);
//					bw.write("UNIVERSAL IAs:\n");
//					bw.write(lineSep);
//					for (Interaction universalIA : complianceController.getUniversalInteractions()) {
//						bw.write("[" + universalIA.getName() + "] ");
//					}
//					bw.newLine();
//					bw.write(lineSep);
//					bw.write("EXISTS IAs:\n");
//					bw.write(lineSep);
//					for (Interaction existIA : complianceController.getExistInteractions()) {
//						bw.write("[" + existIA.getName() + "] ");
//					}
//					bw.newLine();
//					bw.write(lineSep);
//					bw.write("INTERACTION ORDER:\n");
//					bw.write(lineSep);
//					for (Interaction ia : complianceController.getInteractionOrder()) {
//						bw.write(ia + " - related rules: ");
//						for (CompliancePattern cr : complianceController.getAffectedRules(ia)) {
//							bw.write(cr.getLabel() + " ");
//						}
//						bw.newLine();;
//					}
//					bw.write(lineSep);
//					bw.write("INTERACTIONS:\n");
//					bw.write(lineSep);
//					System.out.println(modelGen.getInteractions().size());
//					for (Interaction ia : modelGen.getInteractions()) {
//						if (ia == null) {
//							System.out.println("asdasd");
//						} else {
//							System.out.println(ia);
//							System.out.println(ia.getName());
//							System.out.println(ia.getSender());
//							System.out.println(ia.getReceiver());
//							System.out.println(ia.getMessage());
//							//System.out.println(ia.getMessage().getId());
//
//							//bw.write(ia.getName() + ": " + ia.getSender().name + " -> " + ia.getReceiver().name + " " + ia.getMessage().name + " " + ia.getMessage());
//							bw.newLine();
//						}
//					}
//					bw.write(lineSep);
//					bw.write("Number Of Interactions: " + splitTracking.getNumberOfInteractions());
//					bw.newLine();
//					bw.write(lineSep);
//					bw.write("Number Of Iterations: " + buildIterationCount);
//					bw.newLine();
//					bw.write(lineSep);
//					System.out.println("Done");
//
//
//
//				} catch (IOException e1) {
//					e1.printStackTrace();
//				}
//
//				ChoreographyModel choreoModel = new ChoreographyModel(modelGen.getEnrichedGraph());
//				ChoreographyModel2Bpmn choreo2bpmnIO = new ChoreographyModel2Bpmn(choreoModel, "autogen_choreo_model_" + sdf.format(timestamp));
//
//				// Generate Choreography (incl. all public models / private models)
//				/*
//				ChoreographyGenerator chorGen = new ChoreographyGenerator();
//				Choreography choreo = chorGen.generateChoreographyFromModel(choreoModel);
//
//				// Export public model graphs
//				for (Role role : choreo.collaboration.roles) {
//					IPublicModel puModel = choreo.collaboration.R2PuM.get(role);
//					IOUtils.toFile(sdf.format(timestamp) + "_puModel_" + role.name + ".dot", puModel.getdigraph().toDOT()); // assigned with compliance rules interactions
//				}
//
//				FragmentGenerator fragGen = null;
//
//				// Export private model graps
//				for (Role role : choreo.collaboration.roles) {
//					IPrivateModel prModel = choreo.R2PrM.get(role);
//					fragGen = new FragmentGenerator((PrivateModel) prModel);
//					prModel = fragGen.enhance();
//
//					IOUtils.toFile(sdf.format(timestamp) + "_prModel_" + role.name + ".dot", prModel.getdigraph().toDOT()); // assigned with compliance rules interactions
//				}
//
//				//
//				Collaboration2Bpmn collab2bpmnIO = new Collaboration2Bpmn(choreo.collaboration, "autogen_collab_" + sdf.format(timestamp));
//
//
//
//				for (Role role : choreo.collaboration.roles) {
//					IPrivateModel prModel = choreo.R2PrM.get(role);
//					PrivateModel2Bpmn prModel2bpmn = new PrivateModel2Bpmn(prModel, "autogen_prModel_" + role.name + "_" + sdf.format(timestamp) + ".bpmn");
//					prModel2bpmn.buildXML();
//				}
//
//
//				choreo2bpmnIO.buildXML();
//				collab2bpmnIO.buildXML(); */
//
//				buildAnaylse = new BuildAnaylse(buildIterationCount, interactionCount, splitTracking.getNumberOfInteractions(),true, elapsedTime, xorSplitCount, andSplitCount, maxBranching, cr_ias, cr_number);
//
//			// if interaction assignment failed, increase interactionCount by one every 10 iterations
//			} else if (!buildSuccess && (buildIterationCount % 10 == 0)) {
//				interactionCount = (int) (interactionCount * 1.1);
//			}
//
//			if (!buildSuccess && buildIterationCount > 4) {
//				System.out.println("HIER DRIN");
//				buildAnaylse = new BuildAnaylse(buildIterationCount, interactionCount, splitTracking.getNumberOfInteractions(), false, 0, xorSplitCount, andSplitCount, maxBranching, cr_ias, cr_number);
//				break;
//			}
//
//			complianceController.printComplianceData();
//			System.out.println("Success?: " + buildSuccess);
//			System.out.println("buildIterationCount: " + buildIterationCount);
//			modelGen.printInteractions();
//
//
//			splitTracking.terminate();
//		}
//
//
//		return buildAnaylse;
//	}
}


