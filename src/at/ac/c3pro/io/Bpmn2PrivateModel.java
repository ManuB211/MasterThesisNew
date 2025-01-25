package at.ac.c3pro.io;

import at.ac.c3pro.chormodel.*;
import at.ac.c3pro.node.*;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import java.util.*;

public class Bpmn2PrivateModel {

    static Logger logger = Logger.getLogger(Bpmn2Collaboration.class);

    // OMG BPMN 2.0 URI --- JDOM Namespace
    static Namespace BPMN2NS = Namespace.getNamespace("http://www.omg.org/spec/BPMN/20100524/MODEL");

    protected Element collaboration;
    protected List<Element> messages;
    protected List<Element> messageFlow;
    protected List<Element> processes;
    private final String modelName;
    public IPrivateModel privateModel = null;
    public Map<IPrivateNode, String> PrivateNode2PublicNodeIDMap = new HashMap<IPrivateNode, String>();

    public Bpmn2PrivateModel(String model_path_tpl, String model_name) throws Exception {
        // super();
        this.modelName = model_name;
        String fileName = String.format(model_path_tpl, model_name);
        System.out.print(fileName);
        if (logger.isInfoEnabled())
            logger.info("Reading BPMN 2.0 file: " + fileName);
        Document doc = new SAXBuilder().build(fileName);
        collaboration = doc.getRootElement().getChild("collaboration", BPMN2NS);
        messages = doc.getRootElement().getChildren("message", BPMN2NS);
        messageFlow = collaboration.getChildren("messageFlow", BPMN2NS);
        processes = doc.getRootElement().getChildren("process", BPMN2NS);
        initGraph();
    }

    protected void initGraph() {
        if (logger.isInfoEnabled())
            logger.info("Creating graph");

        Set<Message> messagelist = new HashSet<Message>();
        Map<String, Triple> messageFlowMap = new HashMap<String, Triple>();
        Set<IPrivateNode> privateNodes = new HashSet<IPrivateNode>(); // contains all created nodes of all public
        // models: needed for building the map pu2puNode
        Set<Role> roles = new HashSet<Role>();

        // we start by parsing and building the message instances
        for (Object obj : messages) {
            if (obj instanceof Element) {
                Element elem = (Element) obj;
                String id = elem.getAttributeValue("id");
                String name = elem.getAttributeValue("name");
                messagelist.add(new Message(name, id));
            }
        }

        // finding the roles
        for (Object obj : collaboration.getChildren("participant", BPMN2NS)) {
            roles.add(new Role(((Element) obj).getAttributeValue("name"), ((Element) obj).getAttributeValue("id")));
        }

        // mapping between the message flow used for correlation message source, target

        for (Object obj : messageFlow) {
            if (obj instanceof Element) {
                Element elem = (Element) obj;
                messageFlowMap.put(elem.getAttributeValue("id"), new Triple(elem.getAttributeValue("messageRef"),
                        elem.getAttributeValue("sourceRef"), elem.getAttributeValue("targetRef")));
            }
        }

        // we parse the participants and build the corresponding graphs
        for (Object obj : processes) {
            Element processElem = (Element) obj;
            String processId = processElem.getAttributeValue("id");
            String processName = processElem.getAttributeValue("name");

            if (processElem.getAttributeValue("processType").equals("Private")) {

                MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode> digraph = new MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode>();
                List<Element> edges = new LinkedList<Element>();

                for (Object pnode : processElem.getChildren()) { // constructing the graph of the current process
                    if (pnode instanceof Element) {
                        Element nodeElem = (Element) pnode;
                        String nodeId = nodeElem.getAttributeValue("id");
                        String nodeName = nodeElem.getAttributeValue("name");

                        if (nodeElem.getName().equals("startEvent") || nodeElem.getName().equals("endEvent")) {

                            privateNodes.add(new Event(nodeName, nodeId));

                        } else if (nodeElem.getName().equals("sendTask")) {

                            String IDoperationRef = nodeElem.getAttributeValue("operationRef");

                            Triple t = this.getTarget(nodeId, messageFlowMap);
                            Message message = null;
                            Role targetRole = null;

                            if (t != null) {
                                for (Message m : messagelist)
                                    if (m.getId().equals(t.messageRef))
                                        message = m;
                                for (Role role : roles) {
                                    if (role.id.equals(t.targetRef))
                                        targetRole = role;
                                }
                                IPrivateNode prn = new Send(targetRole, message, nodeName, nodeId);
                                privateNodes.add(prn);
                                PrivateNode2PublicNodeIDMap.put(prn, IDoperationRef);

                            } else {
                                if (logger.isInfoEnabled())
                                    logger.info("mismatching send task" + nodeElem);
                            }

                        } else if (nodeElem.getName().equals("receiveTask")) {

                            String IDoperationRef = nodeElem.getAttributeValue("operationRef");

                            Triple t = this.getSource(nodeId, messageFlowMap);
                            Message message = null;
                            Role sourceRole = null;
                            if (t != null) {
                                for (Message m : messagelist)
                                    if (m.getId().equals(t.messageRef))
                                        message = m;
                                for (Role role : roles)
                                    if (role.id.equals(t.sourceRef)) // bug: targetRed -> should be sourceRef
                                        sourceRole = role;

                                IPrivateNode prn = new Receive(sourceRole, message, nodeName, nodeId);
                                privateNodes.add(prn);
                                PrivateNode2PublicNodeIDMap.put(prn, IDoperationRef);
                            } else {
                                if (logger.isInfoEnabled())
                                    logger.info("mismatching receive task" + nodeElem);
                            }

                        } else if (nodeElem.getName().equals("task")) {

                            privateNodes.add(new PrivateActivity(nodeName, nodeId));

                        } else if (nodeElem.getName().equals("exclusiveGateway")) {
                            privateNodes.add(new XorGateway(nodeName, nodeId));
                        } else if (nodeElem.getName().equals("parallelGateway")) {
                            privateNodes.add(new AndGateway(nodeName, nodeId));
                        } else if (nodeElem.getName().equals("sequenceFlow"))
                            edges.add(nodeElem);
                        else if (logger.isInfoEnabled())
                            logger.warn("Unprocessed Element: " + nodeElem.getName() + ", id: "
                                    + nodeElem.getAttributeValue("id"));
                    }
                }

                for (Element flow : edges) {
                    IPrivateNode src = getNode(flow.getAttributeValue("sourceRef"), privateNodes);
                    IPrivateNode tgt = getNode(flow.getAttributeValue("targetRef"), privateNodes);

                    if (src != null && tgt != null)
                        digraph.addEdge(src, tgt);
                    else {
                        logger.fatal("Malformed graph! Dangling edge: " + flow.getAttributeValue("id"));
                        throw new RuntimeException("Malformed graph");
                    }
                }
                if (logger.isInfoEnabled())
                    logger.info("Graph created");

                // building the public model and filling the role2Public model Map of the
                // collaboration
                if (digraph != null) {
                    privateModel = new PrivateModel(digraph, processName);
                    if (logger.isInfoEnabled())
                        logger.info("public model added: " + privateModel.getName());
                }
            }

        }

    }

    public String getModelName() {
        return this.modelName;
    }

    private IPrivateNode getNode(String id, Set<IPrivateNode> nodes) {
        for (IPrivateNode node : nodes)
            if (node.getId().equals(id))
                return node;
        return null;
    }

    private Triple getTarget(String id, Map<String, Triple> messageFlowMap) {
        for (Triple t : messageFlowMap.values())
            if (t.sourceRef.equals(id))
                return t;

        return null;
    }

    private Triple getSource(String id, Map<String, Triple> messageFlowMap) {
        for (Triple t : messageFlowMap.values())
            if (t.targetRef.equals(id))
                return t;

        return null;
    }

    public Map<IPrivateNode, IPublicNode> getMapPr2puNode(IPublicModel pum) {

        Map<IPrivateNode, IPublicNode> pr2puNodeMap = new HashMap<IPrivateNode, IPublicNode>();

        for (IPrivateNode prn : this.PrivateNode2PublicNodeIDMap.keySet()) {
            for (IPublicNode pun : pum.getdigraph().getVertices()) {
                if (pun.getId().equals(PrivateNode2PublicNodeIDMap.get(prn)))
                    pr2puNodeMap.put(prn, pun);
            }
        }

        return pr2puNodeMap;
    }

    public Map<IPublicNode, IPrivateNode> getMapPu2prNode(IPublicModel pum) {

        Map<IPublicNode, IPrivateNode> pu2prNodeMap = new HashMap<IPublicNode, IPrivateNode>();

        for (IPrivateNode prn : this.PrivateNode2PublicNodeIDMap.keySet()) {
            for (IPublicNode pun : pum.getdigraph().getVertices()) {
                if (pun.getId().equals(PrivateNode2PublicNodeIDMap.get(prn)))
                    pu2prNodeMap.put(pun, prn);
            }
        }

        return pu2prNodeMap;
    }

    public class Triple {
        String messageRef;
        String sourceRef; // activity source: send or receive id
        String targetRef; // activity target: send or receive id

        public Triple(String message, String source, String target) {
            this.messageRef = message;
            this.sourceRef = source;
            this.targetRef = target;
        }

    }

}
