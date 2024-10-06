package at.ac.c3pro.io;


import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;


import at.ac.c3pro.chormodel.ChoreographyModel;
import at.ac.c3pro.chormodel.IChoreographyModel;
import at.ac.c3pro.chormodel.MultiDirectedGraph;
import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.node.AndGateway;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.Event;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.Interaction;
import at.ac.c3pro.node.Message;
import at.ac.c3pro.node.XorGateway;

public class Bpmn2ChoreographyModel {
		
	// log4j ---
		static Logger logger = Logger.getLogger(Bpmn2ChoreographyModel.class);
		
		// OMG BPMN 2.0 URI --- JDOM Namespace
		static Namespace BPMN2NS = Namespace.getNamespace("http://www.omg.org/spec/BPMN/20100524/MODEL");
		
		protected Element choreography;	
		protected List<Element> messages;
		protected List<Element> messageFlow;
		private String modelName;
		public IChoreographyModel choreoModel= null; 
		public Set<Role> roles = new HashSet<Role>();
		
	
	
	public Bpmn2ChoreographyModel(String model_path_tpl, String model_name) throws Exception {
		//super();
		this.modelName = model_name;

		//BasicConfigurator.configure();
		PropertyConfigurator.configure("./log4j.properties");
		 
		String fileName = String.format(model_path_tpl, model_name);
		System.out.print(fileName);
		if (logger.isInfoEnabled()) logger.info("Reading BPMN 2.0 file: " + fileName);
		Document doc = new SAXBuilder().build(fileName);	
		choreography = doc.getRootElement().getChild("choreography", BPMN2NS);	
		
		if(choreography != null){
			messages = doc.getRootElement().getChildren("message", BPMN2NS);
			if(messages != null){
				messageFlow = choreography.getChildren("messageFlow",BPMN2NS);
				initGraph();
			}else{
				logger.fatal("Graph not Created : no message flow specified in the choreography file");
				throw new RuntimeException("parsing failure");
			}
		} else{
			logger.fatal("File parsing failure: the selected file is not a choreography file");
			throw new RuntimeException("parsing failure");	
		}
	}
	
	protected void initGraph() {
		if (logger.isInfoEnabled()) logger.info("Creating graph");
		
		MultiDirectedGraph<Edge<IChoreographyNode>,IChoreographyNode> digraph = new MultiDirectedGraph<Edge<IChoreographyNode>,IChoreographyNode>();
		Set<Message> messagelist = new HashSet<Message>(); 
		Set<IChoreographyNode> chorNodes = new HashSet<IChoreographyNode>();
		Map<String, Triple> messageFlowMap = new HashMap<String, Triple>();
		List<Element> edges = new LinkedList<Element>();
		Map<String,Role> ID2RoleMap = new HashMap<String,Role>();
		
		//we start by parsing and building the message instances in the choreography
		for(Object obj : messages){
			if(obj instanceof Element){
				Element elem = (Element) obj;
				String id = elem.getAttributeValue("id");
				String name = elem.getAttributeValue("name");
				messagelist.add(new Message(name, id));
			}			
		}
	
		//mapping between the message flow  used for correlation message source, target
		
		System.out.println(messageFlow.size());
		
 		for(Object obj : messageFlow){
			if(obj instanceof Element){
				Element elem = (Element) obj;
				messageFlowMap.put(elem.getAttributeValue("id"), new Triple(elem.getAttributeValue("messageRef"),elem.getAttributeValue("sourceRef"),elem.getAttributeValue("targetRef")));
			}			
		}
		
		//finding the roles + normalization: participants having the same name are added as a same role with a same ID
		//Indeed, for two different interactions I1 and I2 that use a same role R, Signavio generates a new ID for R in 
		//each of these interactions nodes in the xml file
		boolean exists =false;
		for (Object role : choreography.getChildren("participant",BPMN2NS)){
			for(Role r : roles){
				if(r.name.equals(((Element)role).getAttributeValue("name"))){
					exists=true;
					ID2RoleMap.put(((Element)role).getAttributeValue("id"), r);
				}
			}
			if(exists == false)
				roles.add(new Role(( (Element) role).getAttributeValue("name"), ((Element)role).getAttributeValue("id")));
			exists=false;
		}
		for(Role r : roles)
			ID2RoleMap.put(r.id, r);
		
		//we parse the participants and build the Roles, then the interactions, gateways and events.
		for (Object obj : choreography.getChildren()){
			if (obj instanceof Element) {
				Element elem = (Element) obj;
				String id = elem.getAttributeValue("id");
				String name = elem.getAttributeValue("name");
				
				if (id.equals("sid-52E9A27A-5E32-45DD-BF70-0007CCCBC155"))
					System.out.println("here we are");
					
				if( elem.getName().equals("startEvent") || elem.getName().equals("endEvent")){
					            chorNodes.add(new Event(name,id));
					 
				} else if (elem.getName().equals("choreographyTask")){
							 
							 
					/* TODO Multiple Messages Approach							 
					List<Element> chorMsgRefs;
					Set<Message> messages = new HashSet<Message>();
					 
					chorMsgRefs = elem.getChildren("messageFlowRef", BPMN2NS);
					 
					int msgCount = 0;
					 
					for (Element msg : chorMsgRefs) {
						msgCount++;
						String msgRef = msg.getValue();
						Triple t = messageFlowMap.get(msgRef);
						Message message=null;
				 		Role sourceRole= null;
				 		Role targetRole = null;
						
						for(Message m : messagelist)
				 			if(m.getId().equals(t.messageRef))
				 				message = m;
				 		for(Role role : roles){
				 			if(role.id.equals(ID2RoleMap.get(t.sourceRef).id))
				 				sourceRole= role;
				 			if(role.id.equals(ID2RoleMap.get(t.targetRef).id))
				 				targetRole = role;
				 		}
						
					}
					 
					System.out.println("INTERACTION MSG COUT: " + msgCount);
							 
					*/		 
						     
			 		Triple t = messageFlowMap.get(elem.getChildText("messageFlowRef",BPMN2NS));
			 		Message message=null;
			 		Role sourceRole= null;
			 		Role targetRole = null;
			 		
			 		for(Message m : messagelist)
			 			if(m.getId().equals(t.messageRef))
			 				message = m;
			 		for(Role role : roles){
			 			if(role.id.equals(ID2RoleMap.get(t.sourceRef).id))
			 				sourceRole=role;
			 			if(role.id.equals(ID2RoleMap.get(t.targetRef).id))
			 				targetRole=role;
			 		}
			 		chorNodes.add(new Interaction(name, id, sourceRole,targetRole, message));
//System.out.println(sourceRole.id+"=="+sourceRole.name+"-->"+targetRole.name+"=="+targetRole.id);
				} else if (elem.getName().equals("exclusiveGateway")) {
						chorNodes.add(new XorGateway(name, id));
				} else if (elem.getName().equals("parallelGateway")) {
					chorNodes.add(new AndGateway(name, id));				
				} else if (elem.getName().equals("sequenceFlow"))
					edges.add(elem);
				else if (logger.isInfoEnabled()) logger.warn("Unprocessed Element: " + elem.getName() + ", id: " + elem.getAttributeValue("id"));
			}
		}

		for (Element flow : edges) {
			IChoreographyNode src = getNode(flow.getAttributeValue("sourceRef"), chorNodes);
			IChoreographyNode tgt = getNode(flow.getAttributeValue("targetRef"), chorNodes);
	
			if (src != null && tgt != null)
				digraph.addEdge(src, tgt);
			else {
				logger.fatal("Malformed graph! Dangling edge: " + flow.getAttributeValue("id"));
				throw new RuntimeException("Malformed graph");
			}
		}
		
		if (logger.isInfoEnabled()) logger.info("Graph created");
		if (digraph != null) choreoModel = new ChoreographyModel(digraph);
	}

	public String getModelName() {
		return this.modelName;
	}
	
	private IChoreographyNode getNode(String id, Set<IChoreographyNode> nodes){
		for(IChoreographyNode node : nodes)
			if(node.getId().equals(id))
				return node;
		return null;
	}
	
	
	public class Triple{
		String messageRef;
		String sourceRef;
		String targetRef;
		
		public Triple(String message, String source, String target){
			this.messageRef=message;
			this.sourceRef = source; //sender id: Role
			this.targetRef = target; //receiver id: Role
		}
		
 	}
	
}


