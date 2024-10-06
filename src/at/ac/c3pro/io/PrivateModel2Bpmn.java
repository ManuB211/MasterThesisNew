package at.ac.c3pro.io;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import at.ac.c3pro.chormodel.IPrivateModel;
import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.node.AndGateway;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.Event;
import at.ac.c3pro.node.IPrivateNode;
import at.ac.c3pro.node.PrivateActivity;
import at.ac.c3pro.node.Receive;
import at.ac.c3pro.node.Send;
import at.ac.c3pro.node.XorGateway;

public class PrivateModel2Bpmn {
	
static Namespace BPMN2NS = Namespace.getNamespace("http://www.omg.org/spec/BPMN/20100524/MODEL");
	
	private IPrivateModel privateModel = null;
	private Collection<Edge<IPrivateNode>> edges;
	private String modelName;
	private Set<IPrivateNode> nodes = new HashSet<IPrivateNode>();
	private Set<Role> roles = new HashSet<Role>();
	private Role processOwner = null;
	
	private List<Element> flowNodeRefs = new ArrayList<Element>();
	private List<Element> seqFlows = new ArrayList<Element>();
	private List<Element> processNodes = new ArrayList<Element>();
	private List<Element> processes = new ArrayList<Element>();
	private Set<Element> messages = new HashSet<Element>();
	private Set<Element> participants = new HashSet<Element>(); 
	private Set<Element> messageFlows = new HashSet<Element>();

	private String outputfolder;
	
	
	public enum GatewayDirection {
		Diverging, Converging;
	}
	
	
	public PrivateModel2Bpmn(IPrivateModel privateModel, String modelName, String folder) {
		this.privateModel = privateModel;
		this.modelName = modelName;
		this.edges = privateModel.getdigraph().getEdges();
		this.outputfolder = folder;
	}
	
	public void buildXML() throws IOException {
		
		Element definitions = new Element("definitions", BPMN2NS);
		//definitions.setAttribute(new Attribute("typeLanguage", "http://www.w3.org/2001/XMLSchema"));
		Namespace bpmndi = Namespace.getNamespace("bpmndi", "http://www.omg.org/spec/BPMN/20100524/DI");
		Namespace omgdc = Namespace.getNamespace("omgdc", "http://www.omg.org/spec/DD/20100524/DC");
		Namespace omgdi = Namespace.getNamespace("omgdi", "http://www.omg.org/spec/DD/20100524/DI");
		Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
		
		Document doc = new Document();
		doc.setRootElement(definitions);
		
		definitions.addNamespaceDeclaration(bpmndi);
		definitions.addNamespaceDeclaration(omgdc);
		definitions.addNamespaceDeclaration(omgdi);
		definitions.addNamespaceDeclaration(xsi);
		definitions.setAttribute("targetNamespace", "http://www.signavio.com/bpmn20");
		definitions.setAttribute("typeLanguage", "http://www.w3.org/2001/XMLSchema");
		definitions.setAttribute("schemaLocation", "http://www.omg.org/spec/BPMN/20100524/MODEL http://www.omg.org/spec/BPMN/2.0/20100501/BPMN20.xsd", xsi);
		
		// XML:collaboration
		Element collaboration = new Element("collaboration", BPMN2NS);
		UUID collabId = UUID.randomUUID();
		collaboration.setAttribute(new Attribute("id", "sid-" + collabId));		
		
		// XML:sequenceFlow
		for (Edge<IPrivateNode> edge : edges) {
			nodes.add(edge.getSource());
			nodes.add(edge.getTarget());
			
			Element seqFlow = new Element("sequenceFlow", BPMN2NS);
			seqFlow.setAttribute(new Attribute("id", "sid-" + edge.getId()));
			seqFlow.setAttribute(new Attribute("name", edge.getName()));
			seqFlow.setAttribute(new Attribute("sourceRef", edge.getSource().getId()));
			seqFlow.setAttribute(new Attribute("targetRef", edge.getTarget().getId()));
			seqFlows.add(seqFlow);
		}
		
		for (IPrivateNode node : nodes) {
			
			// XML:process - lane -> flowNode
			Element flowNodeRef = new Element("flowNodeRef", BPMN2NS);
			flowNodeRef.setText(node.getId());
			flowNodeRefs.add(flowNodeRef);
			
			if (node instanceof Send) {
				// XML:sendTask
				Element sendTask = new Element("sendTask", BPMN2NS);
				
				sendTask.setAttribute(new Attribute("id", node.getId()));
				sendTask.setAttribute(new Attribute("name", node.getName()));
				Set<Element> incoming = this.getIncomingEdges(node);
				Set<Element> outgoing = this.getOutgoingEdges(node);
				sendTask.addContent(incoming);
				sendTask.addContent(outgoing);
				
				
				if (node.getMessage() != null) {					
					UUID msgFlowId = UUID.randomUUID();
					System.out.println(msgFlowId.toString());
					
					Role target = ((Send) node).role;
					
					for (Role role : node.getRoles()) {
						if (processOwner == null) {
							if (target != role)
								processOwner = role;
						}
						roles.add(role);
					}
					
					Element msgFlow = new Element("messageFlow", BPMN2NS);
					msgFlow.setAttribute(new Attribute("id","sid-" + msgFlowId.toString()));
					msgFlow.setAttribute(new Attribute("messageRef", node.getMessage().id));
					msgFlow.setAttribute(new Attribute("sourceRef", node.getId()));
					msgFlow.setAttribute(new Attribute("targetRef", ((Send) node).role.id));
					messageFlows.add(msgFlow);
					
					// XML:message
					Element msg = new Element("message", BPMN2NS);
					msg.setAttribute(new Attribute("id", node.getMessage().id));
					msg.setAttribute(new Attribute("name", node.getMessage().name));
					messages.add(msg);
				}
				
				
				
				processNodes.add(sendTask);
				
			} else if (node instanceof Receive) {
				Element receiveTask = new Element("receiveTask", BPMN2NS);
				
				receiveTask.setAttribute(new Attribute("id", node.getId()));
				receiveTask.setAttribute(new Attribute("name", node.getName()));
				
				Set<Element> incoming = this.getIncomingEdges(node);
				Set<Element> outgoing = this.getOutgoingEdges(node);
				receiveTask.addContent(incoming);
				receiveTask.addContent(outgoing);
				
				
				if (node.getMessage() != null) {					
					UUID msgFlowId = UUID.randomUUID();
					System.out.println(msgFlowId.toString());
					
					Role source = ((Receive) node).role;
					
					for (Role role : node.getRoles()) {
						if (processOwner == null) {
							if (source != role)
								processOwner = role;
						}
						roles.add(role);
					}
					
					Element msgFlow = new Element("messageFlow", BPMN2NS);
					msgFlow.setAttribute(new Attribute("id","sid-" + msgFlowId.toString()));
					msgFlow.setAttribute(new Attribute("messageRef", node.getMessage().id));
					msgFlow.setAttribute(new Attribute("sourceRef", ((Receive) node).role.id));
					msgFlow.setAttribute(new Attribute("targetRef", node.getId()));
					messageFlows.add(msgFlow);
					
					// XML:message
					Element msg = new Element("message", BPMN2NS);
					msg.setAttribute(new Attribute("id", node.getMessage().id));
					msg.setAttribute(new Attribute("name", node.getMessage().name));
					messages.add(msg);
				}
				
				processNodes.add(receiveTask);
				
			} else if (node instanceof PrivateActivity) {
				Element task = new Element("task", BPMN2NS);
				task.setAttribute(new Attribute("id", node.getId()));
				task.setAttribute(new Attribute("name", node.getName()));
				
				Set<Element> incoming = this.getIncomingEdges(node);
				Set<Element> outgoing = this.getOutgoingEdges(node);
				task.addContent(incoming);
				task.addContent(outgoing);
				
				processNodes.add(task);
				
			} else if (node instanceof Event) {
				// XML:event
				Element event;
				Set<Element> inEdges = this.getIncomingEdges(node);
				Set<Element> outEdges = this.getOutgoingEdges(node);
				
				if (!outEdges.isEmpty()) {
					event = new Element("startEvent", BPMN2NS);
				} else if (!inEdges.isEmpty()) {
					event = new Element("endEvent", BPMN2NS);
				} else {
					event = new Element("idkEvent");
				}
				
				event.setAttribute(new Attribute("id", node.getId()));
				event.setAttribute(new Attribute("name", node.getName()));
				
				if (!outEdges.isEmpty())
					event.addContent(outEdges);
				if (!inEdges.isEmpty())
					event.addContent(inEdges);
			
				processNodes.add(event);
				
			} else if (node instanceof XorGateway) {
				// XML:exclusiveGateway
				Element xorGateway = new Element("exclusiveGateway", BPMN2NS);
				Set<Element> inEdges = this.getIncomingEdges(node);
				Set<Element> outEdges = this.getOutgoingEdges(node);
				GatewayDirection direction;
				
				xorGateway.addContent(inEdges);
				xorGateway.addContent(outEdges);
				
				if (inEdges.size() > 1) {
					direction = GatewayDirection.Converging;
				} else {
					direction = GatewayDirection.Diverging;
				}
								
				xorGateway.setAttribute(new Attribute("id", node.getId()));
				xorGateway.setAttribute(new Attribute("name", node.getName()));
				xorGateway.setAttribute(new Attribute("gatewayDirection", direction.toString()));
				processNodes.add(xorGateway);
				
			} else if (node instanceof AndGateway) {
				// XML:parallelGateway
				Element andGateway = new Element("parallelGateway", BPMN2NS);
				Set<Element> inEdges = this.getIncomingEdges(node);
				Set<Element> outEdges = this.getOutgoingEdges(node);
				GatewayDirection direction;
				
				andGateway.addContent(inEdges);
				andGateway.addContent(outEdges);
				
				if (inEdges.size() > 1) {
					direction = GatewayDirection.Converging;
				} else {
					direction = GatewayDirection.Diverging;
				}
								
				andGateway.setAttribute(new Attribute("id", node.getId()));
				andGateway.setAttribute(new Attribute("name", node.getName()));
				andGateway.setAttribute(new Attribute("gatewayDirection", direction.toString()));
				processNodes.add(andGateway);
				
			} else {
				// what anderes
			}
		}
		
		for (Role role : roles) {
			// XML:participants
			Element participant = new Element("participant", BPMN2NS);
			
			UUID processId = UUID.randomUUID();
			
			participant.setAttribute(new Attribute("id", role.id));
			participant.setAttribute(new Attribute("name", role.name));
			participant.setAttribute(new Attribute("processRef", "sid-" + processId));
			
			participants.add(participant);
			
			// XML:process - attributes
			Element process = new Element("process", BPMN2NS);
			process.setAttribute(new Attribute("id", "sid-" + processId));
			process.setAttribute(new Attribute("name", role.name));
			process.setAttribute(new Attribute("isExecutable", "false"));
			process.setAttribute(new Attribute("processType", "None"));
			
			// XML:process - lane
			Element laneSet = new Element("laneSet", BPMN2NS);
			laneSet.setAttribute(new Attribute("id", "sid-" + UUID.randomUUID()));
			Element lane = new Element("lane", BPMN2NS);
			lane.setAttribute(new Attribute("id", "sid-" + UUID.randomUUID()));
			
			laneSet.addContent(lane);
			process.addContent(laneSet);
			
			processes.add(process);
			
		}
		// Process Owner & Process (not retrievable through privateModel)
		Element participant = new Element("participant", BPMN2NS);
		String partName = "Process Owner";
		
		UUID processId = UUID.randomUUID();
		UUID partId = UUID.randomUUID();
		
		participant.setAttribute(new Attribute("id", "sid-" + partId));
		participant.setAttribute(new Attribute("name", partName));
		participant.setAttribute(new Attribute("processRef", "sid-" + processId));
		
		participants.add(participant);
		
		Element process = new Element("process", BPMN2NS);
		process.setAttribute(new Attribute("id", "sid-" + processId));
		process.setAttribute(new Attribute("name", partName));
		process.setAttribute(new Attribute("isExecutable", "true"));
		process.setAttribute(new Attribute("processType", "Private"));
		
		// XML:process - lane
		Element laneSet = new Element("laneSet", BPMN2NS);
		laneSet.setAttribute(new Attribute("id", "sid-" + UUID.randomUUID()));
		Element lane = new Element("lane", BPMN2NS);
		lane.setAttribute(new Attribute("id", "sid-" + UUID.randomUUID()));
		
		lane.addContent(flowNodeRefs);
		laneSet.addContent(lane);
		
		process.addContent(laneSet);
		process.addContent(processNodes);
		process.addContent(seqFlows);
		
		processes.add(process);
		
		// put all together
		doc.getRootElement().addContent(messages);
		collaboration.addContent(participants);
		collaboration.addContent(messageFlows);
		doc.getRootElement().addContent(collaboration);
		doc.getRootElement().addContent(processes);
		
		XMLOutputter xmlOutput = new XMLOutputter();

		// display nice nice
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(doc, new FileWriter(outputfolder + "/" +  modelName));
				
		
	}
	
	private  Set<Element> getIncomingEdges(IPrivateNode node) {
		Set <Element> inEdges = new HashSet<Element>();
		for (Edge<IPrivateNode> edge : edges) {
			if (edge.getTarget().equals(node))
				inEdges.add(new Element("incoming", BPMN2NS).setText("sid-" + edge.getId()));				
		}
			
		return inEdges;
	}
	
	private  Set<Element> getOutgoingEdges(IPrivateNode node) {
		Set <Element> outEdges = new HashSet<Element>();
		for (Edge<IPrivateNode> edge : edges) {
			if (edge.getSource().equals(node))
				outEdges.add(new Element("outgoing", BPMN2NS).setText("sid-" + edge.getId()));				
		}
		return outEdges;
	}
	
}
