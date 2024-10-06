package at.ac.c3pro.io;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import at.ac.c3pro.ImpactAnalysis.ImpactAnalysisUtil.Pair;
import at.ac.c3pro.chormodel.Collaboration;
import at.ac.c3pro.chormodel.IChoreographyModel;
import at.ac.c3pro.chormodel.IRole;
import at.ac.c3pro.chormodel.MultiDirectedGraph;
import at.ac.c3pro.chormodel.PublicModel;
import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.node.AndGateway;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.Event;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.IGateway;
import at.ac.c3pro.node.IPublicNode;
import at.ac.c3pro.node.Message;
import at.ac.c3pro.node.Receive;
import at.ac.c3pro.node.Send;
import at.ac.c3pro.node.XorGateway;

public class Bpmn2Collaboration {

	public Bpmn2Collaboration() {
		// TODO Auto-generated constructor stub
	}

	// log4j ---
	static Logger logger = Logger.getLogger(Bpmn2Collaboration.class);

	// OMG BPMN 2.0 URI --- JDOM Namespace
	static Namespace BPMN2NS = Namespace.getNamespace("http://www.omg.org/spec/BPMN/20100524/MODEL");

	protected Element collaboration;
	protected List<Element> messages;
	protected List<Element> messageFlow;
	protected List<Element> processes;
	private String modelName;
	public IChoreographyModel choreoModel = null;
	public Collaboration collab = null;
	private Map<IPublicNode, String> MapPuNodeID2ChoreoNode = new HashMap<IPublicNode, String>();

	public Bpmn2Collaboration(String model_path_tpl, String model_name) throws Exception {
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

		collab = new Collaboration(collaboration.getAttributeValue("id"));
		Set<Message> messagelist = new HashSet<Message>();
		Map<String, Triple> messageFlowMap = new HashMap<String, Triple>();
		Map<String, Role> processId2role = new HashMap<String, Role>();
		Set<IPublicNode> pumNodes = new HashSet<IPublicNode>(); // contains all created nodes of all public models:
																// needed for building the map pu2puNode
		Map<String, String> activity2process = new HashMap<String, String>();

		// we start by parsing and building the message instances in the choreography
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
			Role role = new Role(((Element) obj).getAttributeValue("name"), ((Element) obj).getAttributeValue("id"));
			collab.addRole(role);
			processId2role.put(((Element) obj).getAttributeValue("processRef"), role);
		}

		// mapping between the message flow used for correlation message source, target

		for (Object obj : messageFlow) {
			if (obj instanceof Element) {
				Element elem = (Element) obj;
				messageFlowMap.put(elem.getAttributeValue("id"), new Triple(elem.getAttributeValue("messageRef"),
						elem.getAttributeValue("sourceRef"), elem.getAttributeValue("targetRef")));
			}
		}

		// sendTask/receiveTask -> process mapping
		for (Object obj : processes) {
			Element processElem = (Element) obj;
			String processId = processElem.getAttributeValue("id");
			for (Object pnode : processElem.getChildren()) {
				Element nodeElem = (Element) pnode;
				if (nodeElem.getName().equals("sendTask") || nodeElem.getName().equals("receiveTask")) {
					String activityId = nodeElem.getAttributeValue("id");
					activity2process.put(activityId, processId);
				}
			}
		}

		// we parse the participants and build the corresponding graphs
		for (Object obj : processes) {
			Element processElem = (Element) obj;
			String processId = processElem.getAttributeValue("id");
			String processName = processElem.getAttributeValue("name");

			MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> digraph = new MultiDirectedGraph<Edge<IPublicNode>, IPublicNode>();
			List<Element> edges = new LinkedList<Element>();

			for (Object pnode : processElem.getChildren()) { // constructing the graph of the current process
				if (pnode instanceof Element) {
					Element nodeElem = (Element) pnode;
					String nodeId = nodeElem.getAttributeValue("id");
					String nodeName = nodeElem.getAttributeValue("name");

					if (nodeElem.getName().equals("startEvent") || nodeElem.getName().equals("endEvent")) {
						pumNodes.add(new Event(nodeName, nodeId));

					} else if (nodeElem.getName().equals("sendTask")) {

						String IDoperationRef = nodeElem.getAttributeValue("operationRef");
						Triple t = this.getTarget(nodeId, messageFlowMap);
						Message message = null;
						Role targetRole = processId2role.get(activity2process.get(t.targetRef));

						if (t != null) {
							for (Message m : messagelist)
								if (m.getId().equals(t.messageRef))
									message = m;

							System.out.println("targetRole: " + targetRole);
							IPublicNode pun = new Send(targetRole, message, nodeName, nodeId);
							pumNodes.add(pun);
							this.MapPuNodeID2ChoreoNode.put(pun, IDoperationRef);

						} else {
							if (logger.isInfoEnabled())
								logger.info("mismatching send task" + nodeElem);
						}
					} else if (nodeElem.getName().equals("receiveTask")) {

						String IDoperationRef = nodeElem.getAttributeValue("operationRef");
						Triple t = this.getSource(nodeId, messageFlowMap);
						Message message = null;
						Role sourceRole = processId2role.get(activity2process.get(t.sourceRef));
						System.out.println("sourceRole: " + sourceRole);
						if (t != null) {
							for (Message m : messagelist)
								if (m.getId().equals(t.messageRef))
									message = m;
							IPublicNode pun = new Receive(sourceRole, message, nodeName, nodeId);
							pumNodes.add(pun);
							this.MapPuNodeID2ChoreoNode.put(pun, IDoperationRef);

						} else {
							if (logger.isInfoEnabled())
								logger.info("mismatching receive task" + nodeElem);
						}
					} else if (nodeElem.getName().equals("exclusiveGateway")) {
						XorGateway gtw = new XorGateway(nodeName, nodeId);
						pumNodes.add(gtw);
						if (collab.Name2PuGtws.containsKey(nodeName))
							collab.Name2PuGtws.get(nodeName)
									.add(new Pair<IRole, IGateway>(processId2role.get(processId), gtw));
						else {
							Set<Pair<IRole, IGateway>> set = new HashSet<Pair<IRole, IGateway>>();
							set.add(new Pair<IRole, IGateway>(processId2role.get(processId), gtw));
							collab.Name2PuGtws.put(nodeName, set);
						}
					} else if (nodeElem.getName().equals("parallelGateway")) {
						AndGateway gtw = new AndGateway(nodeName, nodeId);
						pumNodes.add(gtw);
						if (collab.Name2PuGtws.containsKey(nodeName))
							collab.Name2PuGtws.get(nodeName)
									.add(new Pair<IRole, IGateway>(processId2role.get(processId), gtw));
						else {
							Set<Pair<IRole, IGateway>> set = new HashSet<Pair<IRole, IGateway>>();
							set.add(new Pair<IRole, IGateway>(processId2role.get(processId), gtw));
							collab.Name2PuGtws.put(nodeName, set);
						}
					} else if (nodeElem.getName().equals("sequenceFlow"))
						edges.add(nodeElem);
					else if (logger.isInfoEnabled())
						logger.warn("Unprocessed Element: " + nodeElem.getName() + ", id: "
								+ nodeElem.getAttributeValue("id"));
				}
			}

			for (Element flow : edges) {
				IPublicNode src = getNode(flow.getAttributeValue("sourceRef"), pumNodes);
				IPublicNode tgt = getNode(flow.getAttributeValue("targetRef"), pumNodes);

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
				PublicModel p = new PublicModel(digraph, processName);

				Role role = processId2role.get(processId);
				collab.addPublicModel(role, p);
				collab.R2PuM.put(role, p);
				collab.PuM2R.put(p, role);
				if (role == null && logger.isInfoEnabled()) {
					logger.info("public model created but no corresponding role identified");
				}

				if (logger.isInfoEnabled())
					logger.info("public model added: " + p.getName());
			}

		}

		// building the correlation Map between public nodes of different public models
		for (Triple t : messageFlowMap.values()) {
			collab.Pu2Pu.put(getNode(t.sourceRef, pumNodes), getNode(t.targetRef, pumNodes));
			collab.Pu2Pu.put(getNode(t.targetRef, pumNodes), getNode(t.sourceRef, pumNodes));
		}
	}

	// this method returns the mapping between the public nodes and the matching
	// choreography nodes in the choreoModel
	public Map<IPublicNode, IChoreographyNode> getMapPu2ChoreoNode(IChoreographyModel cm) {

		Map<IPublicNode, IChoreographyNode> pu2choreoNodeMap = new HashMap<IPublicNode, IChoreographyNode>();

		for (IPublicNode pun : this.MapPuNodeID2ChoreoNode.keySet()) {
			for (IChoreographyNode cn : cm.getdigraph().getVertices()) {
				if (cn.getId().equals(this.MapPuNodeID2ChoreoNode.get(pun)))
					pu2choreoNodeMap.put(pun, cn);
			}
		}
		return pu2choreoNodeMap;
	}

	// this method returns the mapping between the choreography nodes and the pair
	// nodes in the matching public modes
	public Map<IChoreographyNode, Pair<IPublicNode, IPublicNode>> getChoreoNode2PuNPairMap(IChoreographyModel cm) {

		Map<IChoreographyNode, Pair<IPublicNode, IPublicNode>> ChoreoNode2PuNPairMap = new HashMap<IChoreographyNode, Pair<IPublicNode, IPublicNode>>();

		for (IChoreographyNode cn : cm.getdigraph().getVertices()) {
			IPublicNode first = null;
			IPublicNode second = null;
			int aux = 0;
			for (IPublicNode pun : this.MapPuNodeID2ChoreoNode.keySet()) {
				if (cn.getId().equals(this.MapPuNodeID2ChoreoNode.get(pun))) {
					if (aux == 0) {
						first = pun;
						aux++;
					}
					if (aux == 1) {
						second = pun;
					}
				}
			}
			if (aux == 1 && first != null && second != null)
				ChoreoNode2PuNPairMap.put(cn, new Pair<IPublicNode, IPublicNode>(first, second));
			else
				logger.fatal("Matching problem chorNode2PuNodePair: " + cn.getId()
						+ " do not have corresonding public nodes");
		}

		return ChoreoNode2PuNPairMap;
	}

	// this method returns the mapping between the choreography gateways and the set
	// of gateways in the matching public modes. it also fills the map between
	// public gateways and chor gateway
	public Map<IGateway, Set<Pair<IRole, IGateway>>> getchorGtw2PuGtws(IChoreographyModel cm) {

		Map<IGateway, Set<Pair<IRole, IGateway>>> chorGtw2PuGtws = new HashMap<IGateway, Set<Pair<IRole, IGateway>>>();
		for (IChoreographyNode cn : cm.getdigraph().getVertices()) {
			if (cn instanceof IGateway) {
				if (collab.Name2PuGtws.containsKey(cn.getName())) {
					chorGtw2PuGtws.put((IGateway) cn, collab.Name2PuGtws.get(cn.getName()));
				} else
					logger.fatal("Matching problem: " + cn.getId() + " do not have corresonding public nodes");

			}
		}
		for (IGateway gtw : chorGtw2PuGtws.keySet()) {
			for (Pair<IRole, IGateway> pair : chorGtw2PuGtws.get(gtw))
				collab.PuGtw2chorGtw.put(pair.second, gtw);
		}

		return chorGtw2PuGtws;
	}

	/*
	 * public Map<Pair<IPublicNode,IPublicNode>,IChoreographyNode>
	 * getMapPr2puNode(IChoreographyModel cm){
	 * 
	 * Map<IPublicNode,IChoreographyNode> pu2choreoNodeMap = new
	 * HashMap<IPublicNode,IChoreographyNode>();
	 * 
	 * for(IPublicNode pun: this.MapPuNodeID2ChoreoNode.keySet()){
	 * for(IChoreographyNode cn: cm.getdigraph().getVertices()){
	 * if(cn.getId().equals(this.MapPuNodeID2ChoreoNode.get(pun)))
	 * pu2choreoNodeMap.put(pun,cn); } }
	 * 
	 * return pu2choreoNodeMap; }
	 */

	public String getModelName() {
		return this.modelName;
	}

	private IPublicNode getNode(String id, Set<IPublicNode> nodes) {
		for (IPublicNode node : nodes)
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
