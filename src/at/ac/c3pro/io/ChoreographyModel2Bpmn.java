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


import at.ac.c3pro.chormodel.IChoreographyModel;
import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.node.AndGateway;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.Event;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.Interaction;
import at.ac.c3pro.node.XorGateway;


public class ChoreographyModel2Bpmn {
	
	static Namespace BPMN2NS = Namespace.getNamespace("http://www.omg.org/spec/BPMN/20100524/MODEL");
		
	private Set<Element> messages = new HashSet<Element>();
	private Set<Element> participants = new HashSet<Element>(); 
	private Set<Element> messageFlows = new HashSet<Element>();
	private List<Element> seqFlows = new ArrayList<Element>();
	private List<Element> chorTasks = new ArrayList<Element>();
	private List<Element> gateways = new ArrayList<Element>();
	private List<Element> events = new ArrayList<Element>();
	private List<Element> bpmnShapes = new ArrayList<Element>();
	private List<Element> bpmnEdges = new ArrayList<Element>(); 
	
	private Collection<Edge<IChoreographyNode>> chorEdges;
	private Set<IChoreographyNode> chorNodes = new HashSet<IChoreographyNode>();
	private String modelName;
	public IChoreographyModel choreoModel= null; 
	public Set<Role> roles = new HashSet<Role>();

	private String outputFolder;
	
	public enum GatewayDirection {
		Diverging, Converging;
	}
	
	public ChoreographyModel2Bpmn(IChoreographyModel choreoModel, String modelName, String folder) {
		this.choreoModel= choreoModel;
		this.modelName = modelName;
		this.chorEdges = choreoModel.getdigraph().getEdges();
		this.outputFolder = folder;
	}
	
	
	public void buildXML() throws IOException {
				
		// counter xml elements
		int sequenceFlowCount = chorEdges.size();
		int messageFlowCount = 0;
		int messageCount = 0;
		int choreographyTaskCount = 0;
		int xorGatewayCount = 0;
		int andGatewayCount = 0;
		int eventCount = 0;
		int participantCount = 0;
	
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
		
		
		// XML:sequenceFlow
		for (Edge<IChoreographyNode> edge : chorEdges) {
				chorNodes.add(edge.getSource());
				chorNodes.add(edge.getTarget());
				
				Element seqFlow = new Element("sequenceFlow", BPMN2NS);
				seqFlow.setAttribute(new Attribute("id", "sid-" + edge.getId()));
				seqFlow.setAttribute(new Attribute("name", edge.getName()));
				seqFlow.setAttribute(new Attribute("sourceRef", "sid-" + edge.getSource().getId()));
				seqFlow.setAttribute(new Attribute("targetRef", "sid-" + edge.getTarget().getId()));
				seqFlows.add(seqFlow);
				
				// XML:BPMNEdge
				Element bpmnEdge = new Element("BPMNEdge", bpmndi);
				bpmnEdge.setAttribute(new Attribute("bpmnElement", "sid-" + edge.getId()));
				bpmnEdge.setAttribute(new Attribute("id", "sid-" + edge.getId() + "_gui"));
				
				bpmnEdges.add(bpmnEdge);				
				
		}
		
		for (IChoreographyNode node : chorNodes) {
			
			// XML:BPMNdi
			Element bpmnShape = new Element("BPMNShape", bpmndi);
			bpmnShape.setAttribute(new Attribute("bpmnElement", node.getId()));
			bpmnShape.setAttribute(new Attribute("id", node.getId() + "_gui"));
			
			Element bounds = new Element("Bounds",omgdc);
			bounds.setAttribute(new Attribute("height", "0"));
			bounds.setAttribute(new Attribute("width", "0"));
			bounds.setAttribute(new Attribute("x", "0"));
			bounds.setAttribute(new Attribute("y", "0"));
			
			bpmnShape.addContent(bounds);
			
			bpmnShapes.add(bpmnShape);
			
			if (node instanceof Interaction) {
				// XML:choreographyTask
				Element choreoTask = new Element("choreographyTask", BPMN2NS);
				// id / name 
				choreoTask.setAttribute(new Attribute("id", "sid-" + node.getId()));
				choreoTask.setAttribute(new Attribute("name", node.getName()));
				choreoTask.setAttribute(new Attribute("initiatingParticipantRef", "sid-" + ((Interaction) node).getSender().id));
				
				// incoming and outgoing through nodes
				choreoTask.addContent(this.getIncomingEdges(node));
				choreoTask.addContent(this.getOutgoingEdges(node));
				//partRef
				choreoTask.addContent(new Element("participantRef", BPMN2NS).setText("sid-" + ((Interaction) node).getSender().id));
				choreoTask.addContent(new Element("participantRef", BPMN2NS).setText("sid-" + ((Interaction) node).getReceiver().id));
				
				// XML:messageFLow				
				if (node.getMessage() != null) {
					
					UUID msgFlowId = UUID.randomUUID();
					System.out.println(msgFlowId.toString());
					choreoTask.addContent(new Element("messageFlowRef", BPMN2NS).setText("sid-" + msgFlowId.toString()));
					
					Element msgFlow = new Element("messageFlow", BPMN2NS);
					msgFlow.setAttribute(new Attribute("id","sid-" + msgFlowId.toString()));
					msgFlow.setAttribute(new Attribute("messageRef", "sid-" + node.getMessage().id));
					msgFlow.setAttribute(new Attribute("sourceRef", "sid-" + ((Interaction) node).getSender().id));
					msgFlow.setAttribute(new Attribute("targetRef", "sid-" + ((Interaction) node).getReceiver().id));
					messageFlows.add(msgFlow);
					
					// XML:message
					Element msg = new Element("message", BPMN2NS);
					msg.setAttribute(new Attribute("id", "sid-" + node.getMessage().id));
					msg.setAttribute(new Attribute("name", node.getMessage().name));
					messages.add(msg);
					messageCount++;
				}
				
				boolean msgVisible = true;
				String participantBandKind = "top_initiating";
				
				for (Role role : node.getRoles()) {
					// XML:BPMNShape (choreoTask)
					Element bpmnShapeIA = new Element("BPMNShape", bpmndi);
					bpmnShapeIA.setAttribute(new Attribute("bpmnElement", "sid-" + role.id));
					bpmnShapeIA.setAttribute(new Attribute("choreographyActivityShape", "sid-" + node.getId() + "_gui"));
					bpmnShapeIA.setAttribute(new Attribute("id", "sid-" + role.id + "_gui"));
  					bpmnShapeIA.setAttribute(new Attribute("isMessageVisible", Boolean.toString(msgVisible)));
					bpmnShapeIA.setAttribute(new Attribute("participantBandKind", participantBandKind));
					
					Element boundsIA = new Element("Bounds", omgdc);
					boundsIA.setAttribute(new Attribute("height", "0"));
					boundsIA.setAttribute(new Attribute("width", "0"));
					boundsIA.setAttribute(new Attribute("x", "0"));
					boundsIA.setAttribute(new Attribute("y", "0"));
					
					bpmnShapeIA.addContent(boundsIA);
					
					bpmnShapes.add(bpmnShapeIA);	
					participantBandKind = "middle_non_initiating";
					
					addRole(role);
				}
				
				chorTasks.add(choreoTask);
				choreographyTaskCount++;
				messageFlowCount++;
				
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
								
				andGateway.setAttribute(new Attribute("id", "sid-" + node.getId()));
				andGateway.setAttribute(new Attribute("name", node.getName()));
				andGateway.setAttribute(new Attribute("gatewayDirection", direction.toString()));
				gateways.add(andGateway);
				
				andGatewayCount++;
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
								
				xorGateway.setAttribute(new Attribute("id", "sid-" + node.getId()));
				xorGateway.setAttribute(new Attribute("name", node.getName()));
				xorGateway.setAttribute(new Attribute("gatewayDirection", direction.toString()));
				gateways.add(xorGateway);
				xorGatewayCount++;
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
				
				event.setAttribute(new Attribute("id", "sid-" + node.getId()));
				event.setAttribute(new Attribute("name", node.getName()));
				
				if (!outEdges.isEmpty())
					event.addContent(outEdges);
				if (!inEdges.isEmpty())
					event.addContent(inEdges);
				
				events.add(event);
				eventCount++;
			} else {
			}
		}
		// XML:participants
		for (Role role : roles) {
			Element participant = new Element("participant", BPMN2NS);
			participant.setAttribute(new Attribute("id", "sid-" + role.id));
			participant.setAttribute(new Attribute("name", role.name));
			participants.add(participant);
		}
		participantCount = roles.size();
		
		System.out.print("SEQ: " + sequenceFlowCount + " MSG: " + messageCount + 
				" MSGF: " + messageFlowCount + " PART: " + participantCount + 
				" XOR: " + xorGatewayCount + " AND: " + andGatewayCount +
				" CHOR: " + choreographyTaskCount + " EVENT: " + eventCount);
		
		
		// put doc together
		doc.getRootElement().addContent(messages);
		Element choreo = new Element("choreography", BPMN2NS);
		UUID choreoId = UUID.randomUUID();
		choreo.setAttribute(new Attribute("id", "sid-" + choreoId));
		choreo.setAttribute(new Attribute("isClosed", "false"));
		choreo.addContent(participants);
		choreo.addContent(messageFlows);
		choreo.addContent(chorTasks);
		choreo.addContent(events);
		choreo.addContent(gateways);
		choreo.addContent(seqFlows);

		doc.getRootElement().addContent(choreo);
		
		Element diagram = new Element("BPMNDigram", bpmndi);
		diagram.setAttribute(new Attribute("id", "sid-" + UUID.randomUUID()));
		
		Element bpmnPlane = new Element("BPMNPlane", bpmndi);
		bpmnPlane.setAttribute(new Attribute("bpmnElement", "sid-" + choreoId));
		bpmnPlane.setAttribute(new Attribute("id", "sid-" + UUID.randomUUID()));
		
		bpmnPlane.addContent(bpmnShapes);
		bpmnPlane.addContent(bpmnEdges);
		
		diagram.addContent(bpmnPlane);
//		doc.getRootElement().addContent(diagram);
		
		
		XMLOutputter xmlOutput = new XMLOutputter();

		// display nice nice
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(doc, new FileWriter(outputFolder + "/" + modelName + ".bpmn"));

			
	}
	
	private void addRole(Role role) {		
		for (Role r : roles) {
			if (r.name.equals(role.name))
				return;
		}
		roles.add(role);	
	}
	
	private Set<Role> getRoles() {
		return roles;
	}
	
	private  Set<Element> getIncomingEdges(IChoreographyNode node) {
		Set <Element> inEdges = new HashSet<Element>();
		for (Edge<IChoreographyNode> edge : chorEdges) {
			if (edge.getTarget().equals(node))
				inEdges.add(new Element("incoming", BPMN2NS).setText("sid-" + edge.getId()));				
		}
		return inEdges;
	}
	
	private  Set<Element> getOutgoingEdges(IChoreographyNode node) {
		Set <Element> outEdges = new HashSet<Element>();
		for (Edge<IChoreographyNode> edge : chorEdges) {
			if (edge.getSource().equals(node))
				outEdges.add(new Element("outgoing", BPMN2NS).setText("sid-" + edge.getId()));				
		}
		return outEdges;
	}
	
	
	

}
