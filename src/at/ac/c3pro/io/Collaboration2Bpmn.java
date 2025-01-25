package at.ac.c3pro.io;

import at.ac.c3pro.chormodel.Collaboration;
import at.ac.c3pro.chormodel.IPublicModel;
import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.node.*;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Collaboration2Bpmn {

    static Namespace BPMN2NS = Namespace.getNamespace("http://www.omg.org/spec/BPMN/20100524/MODEL");

    private Collaboration collab = null;
    private final String name;
    private final List<Element> processes = new ArrayList<Element>();
    private final Set<Element> messages = new HashSet<Element>();
    private final Set<Element> participants = new HashSet<Element>();
    private final Set<Element> messageFlows = new HashSet<Element>();
    private final Map<String, String> operationRefs = new HashMap<String, String>();

    private final String outputFolder;

    public enum GatewayDirection {
        Diverging, Converging
    }

    public Collaboration2Bpmn(Collaboration collab, String name, String folder) {
        this.collab = collab;
        this.name = name;
        this.outputFolder = folder;
    }

    public void buildXML() throws IOException {
        // generate node operationRefIds and build map
        for (Map.Entry<IPublicNode, IPublicNode> entry : collab.Pu2Pu.entrySet()) {

            // Trim away the participant info ( (p1) or (r), etc), so that the building
            // process can use the hash function normally
            String key;
            if (entry.getKey() instanceof InteractionActivity) {
                key = ((InteractionActivity) entry.getKey()).getNameWithoutSenderReceiverInfo();
            } else
                key = entry.getKey().getName();

            operationRefs.putIfAbsent(key, "sid-" + UUID.randomUUID());
            // operationRefs.putIfAbsent(entry.getKey().getName(), "sid-" +
            // UUID.randomUUID());
        }

        Element definitions = new Element("definitions", BPMN2NS);
        // definitions.setAttribute(new Attribute("typeLanguage",
        // "http://www.w3.org/2001/XMLSchema"));
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
        definitions.setAttribute("schemaLocation",
                "http://www.omg.org/spec/BPMN/20100524/MODEL http://www.omg.org/spec/BPMN/2.0/20100501/BPMN20.xsd",
                xsi);

        // XML:collaboration
        Element collaboration = new Element("collaboration", BPMN2NS);
        collaboration.setAttribute(new Attribute("id", collab.id));
        System.out.println("Collab id:" + collab.id);

        for (Role role : collab.roles) {

            List<Element> flowNodeRefs = new ArrayList<Element>();
            List<Element> processNodes = new ArrayList<Element>();
            List<Element> seqFlows = new ArrayList<Element>();
            Set<IPublicNode> nodes = new HashSet<IPublicNode>();

            // XML:roles
            System.out.println(role.id);
            System.out.println(role.name);
            Element participant = new Element("participant", BPMN2NS);
            participant.setAttribute(new Attribute("id", role.id));
            participant.setAttribute(new Attribute("name", role.name));
            UUID processId = UUID.randomUUID();
            participant.setAttribute(new Attribute("processRef", "sid-" + processId));

            participants.add(participant);

            IPublicModel puModel = collab.R2PuM.get(role);

            // XML:process - attributes
            Element process = new Element("process", BPMN2NS);
            process.setAttribute(new Attribute("id", "sid-" + processId));
            process.setAttribute(new Attribute("isClosed", "true"));
            process.setAttribute(new Attribute("isExecutable", "false"));
            process.setAttribute(new Attribute("name", role.name));
            process.setAttribute(new Attribute("processType", "Public"));

            // XML:process - lane
            Element laneSet = new Element("laneSet", BPMN2NS);
            laneSet.setAttribute(new Attribute("id", "sid-" + UUID.randomUUID()));
            Element lane = new Element("lane", BPMN2NS);
            lane.setAttribute(new Attribute("id", "sid-" + UUID.randomUUID()));

            for (Edge<IPublicNode> edge : puModel.getdigraph().getEdges()) {
                // XML:process - seqFlow
                Element seqFlow = new Element("sequenceFlow", BPMN2NS);
                seqFlow.setAttribute(new Attribute("id", "sid-" + edge.getId()));
                seqFlow.setAttribute(new Attribute("name", edge.getName()));
                seqFlow.setAttribute(new Attribute("sourceRef", edge.getSource().getId()));
                seqFlow.setAttribute(new Attribute("targetRef", edge.getTarget().getId()));

                nodes.add(edge.getSource());
                nodes.add(edge.getTarget());

                seqFlows.add(seqFlow);
            }

            for (IPublicNode node : nodes) {
                // XML:process - lane -> flowNode
                Element flowNodeRef = new Element("flowNodeRef", BPMN2NS);
                flowNodeRef.setText(node.getId());
                flowNodeRefs.add(flowNodeRef);

                // XML:process - nodes - messages - msgFlow
                if (node instanceof Send) {
                    // XML:process - sendTask
                    Element sendTask = new Element("sendTask", BPMN2NS);
                    sendTask.setAttribute(new Attribute("id", node.getId()));
                    sendTask.setAttribute(new Attribute("name", node.getName()));
                    sendTask.setAttribute(new Attribute("operationRef",
                            operationRefs.get(((Send) node).getNameWithoutSenderReceiverInfo())));
                    Set<Element> incoming = this.getIncomingEdges(node, puModel);
                    Set<Element> outgoing = this.getOutgoingEdges(node, puModel);
                    sendTask.addContent(incoming);
                    sendTask.addContent(outgoing);
                    processNodes.add(sendTask);

                    // XML:message
                    Element msg = new Element("message", BPMN2NS);
                    msg.setAttribute(new Attribute("id", node.getMessage().id));
                    msg.setAttribute(new Attribute("name", node.getMessage().name));
                    messages.add(msg);

                    // XML:messageFlow
                    Element msgFlow = new Element("messageFlow", BPMN2NS);
                    msgFlow.setAttribute(new Attribute("id", "sid-" + UUID.randomUUID()));
                    msgFlow.setAttribute(new Attribute("messageRef", node.getMessage().id));
                    msgFlow.setAttribute(new Attribute("sourceRef", node.getId()));
                    msgFlow.setAttribute(new Attribute("targetRef", collab.Pu2Pu.get(node).getId()));
                    messageFlows.add(msgFlow);

                } else if (node instanceof Receive) {
                    // XML:process - receiveTask
                    Element receiveTask = new Element("receiveTask", BPMN2NS);
                    receiveTask.setAttribute(new Attribute("id", node.getId()));
                    receiveTask.setAttribute(new Attribute("name", node.getName()));
                    receiveTask.setAttribute(new Attribute("operationRef",
                            operationRefs.get(((Receive) node).getNameWithoutSenderReceiverInfo())));
                    receiveTask.addContent(this.getIncomingEdges(node, puModel));
                    receiveTask.addContent(this.getOutgoingEdges(node, puModel));
                    processNodes.add(receiveTask);
                } else if (node instanceof Event) {
                    // XML:event
                    Element event;
                    Set<Element> inEdges = this.getIncomingEdges(node, puModel);
                    Set<Element> outEdges = this.getOutgoingEdges(node, puModel);

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
                    Set<Element> inEdges = this.getIncomingEdges(node, puModel);
                    Set<Element> outEdges = this.getOutgoingEdges(node, puModel);
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
                    Set<Element> inEdges = this.getIncomingEdges(node, puModel);
                    Set<Element> outEdges = this.getOutgoingEdges(node, puModel);
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

                }

            }

            System.out.println("SEQF: " + seqFlows.size() + " flowNodeRefs: " + flowNodeRefs.size() + " processNodes: "
                    + processNodes.size());

            // put process together
            lane.addContent(flowNodeRefs);
            laneSet.addContent(lane);
            process.addContent(laneSet);
            process.addContent(processNodes);
            process.addContent(seqFlows);
            processes.add(process);
        }

        // put doc together
        doc.getRootElement().addContent(messages);
        collaboration.addContent(participants);
        collaboration.addContent(messageFlows);
        doc.getRootElement().addContent(collaboration);
        doc.getRootElement().addContent(processes);

        XMLOutputter xmlOutput = new XMLOutputter();

        // display nice nice
        xmlOutput.setFormat(Format.getPrettyFormat());
        xmlOutput.output(doc, new FileWriter(outputFolder + "/" + name + ".bpmn"));

    }

    private Set<Element> getIncomingEdges(IPublicNode node, IPublicModel puModel) {
        Set<Element> inEdges = new HashSet<Element>();
        for (Edge<IPublicNode> edge : puModel.getdigraph().getEdges()) {
            if (edge.getTarget().equals(node))
                inEdges.add(new Element("incoming", BPMN2NS).setText("sid-" + edge.getId()));
        }

        return inEdges;
    }

    private Set<Element> getOutgoingEdges(IPublicNode node, IPublicModel puModel) {
        Set<Element> outEdges = new HashSet<Element>();
        for (Edge<IPublicNode> edge : puModel.getdigraph().getEdges()) {
            if (edge.getSource().equals(node))
                outEdges.add(new Element("outgoing", BPMN2NS).setText("sid-" + edge.getId()));
        }
        return outEdges;
    }

}
