package at.ac.c3pro.util;

import at.ac.c3pro.ImpactAnalysis.ImpactAnalysisUtil.Pair;
import at.ac.c3pro.chormodel.*;
import at.ac.c3pro.node.*;
import at.ac.c3pro.node.Interaction.InteractionType;

import java.io.IOException;
import java.util.*;

public class ChoreographyGenerator {

    public static Choreography generateChoreographyFromModel(IChoreographyModel chorModel) {

        /*
         * -- public Collaboration collaboration= null;//choreography model + all public
         * models and roles map public List<IPrivateModel> privateModels= null; public
         * String id; public Map<IPrivateModel, IPublicModel> P2P = new
         * HashMap<IPrivateModel, IPublicModel>(); -- public
         * Map<IPublicNode,IChoreographyNode> Pu2Ch = new
         * HashMap<IPublicNode,IChoreographyNode>();
         *
         * public Map<IPrivateNode,IPublicNode> Pr2Pu = new
         * HashMap<IPrivateNode,IPublicNode>(); public Map<IPublicNode,IPrivateNode>
         * Pu2Pr = new HashMap<IPublicNode,IPrivateNode>(); -- public
         * Map<IChoreographyNode, Pair<IPublicNode,IPublicNode>> Ch2PuPair = new
         * HashMap<IChoreographyNode, Pair<IPublicNode,IPublicNode>>(); -- public
         * Map<IGateway, Set<Pair<IRole, IGateway>>> ChGtw2PuGtws = new
         * HashMap<IGateway, Set<Pair<IRole, IGateway>>>();
         */

        /*
         * public String id; -- public Set<Role> roles = new HashSet<Role>(); -- public
         * Set<IPublicModel> puModels = new HashSet<IPublicModel>(); -- public
         * Map<Role,IPublicModel> R2PuM = new HashMap<Role,IPublicModel>(); -- public
         * Map<IPublicModel, Role> PuM2R = new HashMap<IPublicModel, Role>(); -- public
         * Map<IPublicNode,IPublicNode> Pu2Pu = new HashMap<IPublicNode,IPublicNode>();
         * //Name2PuGtws is a map between A gateway Name and all its instances in the
         * public models. Name-->Set((role,Gtw)). -- public Map<String,
         * Set<Pair<IRole,IGateway>>> Name2PuGtws = new HashMap<String,
         * Set<Pair<IRole,IGateway>>>(); -- public Map<IGateway, IGateway> PuGtw2chorGtw
         * = new HashMap<IGateway, IGateway>();
         */

        Choreography choreography = new Choreography();
        choreography.choreo = chorModel;
        choreography.collaboration = new Collaboration("collab");
        choreography.collaboration.roles = FragmentUtil.extractRoles(chorModel.getRoot());

        try {
            OutputHandler.createOutputFolder("Reductions");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // filling publicmodels, R2PuM, PuM2R
        for (Role role : choreography.collaboration.roles) {
            IPublicModel pum = ChoreographyGenerator.transformChorModel2PubModel(chorModel.projectionRole(role, true),
                    role);
            choreography.collaboration.puModels.add(pum);
            choreography.collaboration.R2PuM.put(role, pum);
            choreography.collaboration.PuM2R.put(pum, role);
        }
        // filling Pu2Pu map
        for (IPublicModel pumS : choreography.collaboration.puModels) {
            for (IPublicModel pumT : choreography.collaboration.puModels) {
                if (!pumS.equals(pumT)) {
                    for (IPublicNode nodeS : pumS.getdigraph().getVertices())
                        for (IPublicNode nodeT : pumT.getdigraph().getVertices()) {
                            if (nodeS instanceof InteractionActivity && nodeT instanceof InteractionActivity) {
                                if (((InteractionActivity) nodeS).getNameWithoutSenderReceiverInfo().equalsIgnoreCase(
                                        ((InteractionActivity) nodeT).getNameWithoutSenderReceiverInfo()))
                                    choreography.collaboration.Pu2Pu.put(nodeS, nodeT);
                            } // else if(nodeS instanceof Event && nodeT instanceof Event){

                            // }
                        }
                }

            }
        }
        // filling Name2PuGtws map
        for (IPublicModel pumS : choreography.collaboration.puModels)
            for (IPublicNode nodeS : pumS.getdigraph().getVertices()) {
                if (nodeS instanceof IGateway) {
                    Pair<IRole, IGateway> pairS = new Pair<IRole, IGateway>(choreography.collaboration.PuM2R.get(pumS),
                            (Gateway) nodeS);
                    if (choreography.collaboration.Name2PuGtws.containsKey(nodeS.getName())) {
                        choreography.collaboration.Name2PuGtws.get(nodeS.getName()).add(pairS);
                    } else {
                        Set<Pair<IRole, IGateway>> set = new HashSet<Pair<IRole, IGateway>>();
                        set.add(pairS);
                        choreography.collaboration.Name2PuGtws.put(nodeS.getName(), set);
                    }
                }
            }

        // filling PuGtw2chorGtw map, Ch2PuPair map, ChGtw2PuGtws mapChGtw2PuGtws

        for (IChoreographyNode chorNode : chorModel.getdigraph().getVertices()) {

            if (chorNode instanceof IGateway) {
                for (IPublicModel pum : choreography.collaboration.puModels) {
                    for (IPublicNode pun : pum.getdigraph().getVertices()) {
                        if (pun instanceof IGateway && chorNode.getName().equalsIgnoreCase(pun.getName())) {
                            choreography.Pu2Ch.put(pun, chorNode);
                            choreography.collaboration.PuGtw2chorGtw.put((Gateway) pun, (Gateway) chorNode);
                            if (choreography.ChGtw2PuGtws.containsKey(chorNode))
                                choreography.ChGtw2PuGtws.get(chorNode).add(new Pair<IRole, IGateway>(
                                        choreography.collaboration.PuM2R.get(pum), (Gateway) pun));
                            else {
                                Set<Pair<IRole, IGateway>> set = new HashSet<Pair<IRole, IGateway>>();
                                set.add(new Pair<IRole, IGateway>(choreography.collaboration.PuM2R.get(pum),
                                        (Gateway) pun));
                                choreography.ChGtw2PuGtws.put((Gateway) chorNode, set);

                            }
                        }
                    }
                }
            } else if (chorNode instanceof Interaction) {
                IPublicNode first = null;
                IPublicNode second = null;
                int aux = 0;
                for (IPublicModel pum : choreography.collaboration.puModels) {
                    for (IPublicNode pun : pum.getdigraph().getVertices()) {
                        if (pun.getName().equalsIgnoreCase(chorNode.getName())) {
                            choreography.Pu2Ch.put(pun, chorNode);
                            if (aux == 0) {
                                first = pun;
                                aux++;
                            }
                            if (aux == 1) {
                                second = pun;
                            }
                            if (aux == 1 && first != null && second != null)
                                choreography.Ch2PuPair.put(chorNode, new Pair<IPublicNode, IPublicNode>(first, second));
                        }
                    }

                }
            }

        }

        // generate private models, prM2puM map, R2PrM map
        for (Map.Entry<Role, IPublicModel> entry : choreography.collaboration.R2PuM.entrySet()) {
            System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
            IPrivateModel prModel = ChoreographyGenerator.transformPubModel2PrModel(entry.getValue(), entry.getKey());
            choreography.P2P.put(prModel, entry.getValue());
            choreography.privateModels.add(prModel);
            choreography.R2PrM.put(entry.getKey(), prModel);
        }

//		// Pr2Pu (nodes) map, Pu2Pr map (nodes)
//		for(IPublicModel puModel : choreography.collaboration.puModels){
//			for (IPublicNode puNode : puModel.getdigraph().getVertices()) {
//				if
//			}
//			
//		}

        return choreography;
    }

    public static IPublicModel transformChorModel2PubModel(
            IRpstModel<Edge<IChoreographyNode>, IChoreographyNode> choreoM, Role currentRole) {
        MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> graph = new MultiDirectedGraph<Edge<IPublicNode>, IPublicNode>();
        Map<IChoreographyNode, IPublicNode> C2Pnode = new HashMap<IChoreographyNode, IPublicNode>();

        System.out.println("<><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><>");
        System.out.println("Printing edges off choreography model for role " + currentRole);
        System.out.println("<><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><>");

        for (Edge<IChoreographyNode> e : choreoM.getdigraph().getEdges()) {
            System.out.println(e);
        }

        System.out.println("<><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><>");
        System.out.println("All edges of choreography model printed for role " + currentRole);
        System.out.println("<><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><>");

        for (IChoreographyNode node : choreoM.getdigraph().getVertices()) {
            System.out.println("Node:" + node);
            if (node instanceof Event) {
                Event eventNode = (Event) node;
                eventNode.setRole(currentRole);
                C2Pnode.put(node, eventNode);
            } else if (node instanceof Gateway) {
                C2Pnode.put(node, (Gateway) node);
            } else if (node instanceof Interaction) {
                putNodeInC2PNode(C2Pnode, ((Interaction) node), currentRole);
            } else {
                System.out.println("Unknown type of node : " + node);
            }
        }

        // Ensure a clean handling of the Handover-of-Work interaction type
        HOWHandler howHandler = new HOWHandler(choreoM.getdigraph(), currentRole);
        howHandler.run();

        Collection<Edge<IChoreographyNode>> edges = howHandler.getEdges();

        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println("Start of Debug output for puModel generation of " + currentRole.name);
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        for (Edge<IChoreographyNode> e : edges) {
            System.out.println("Edge: " + e);
            System.out.println("Source" + C2Pnode.get(e.getSource()));
            System.out.println("Source" + C2Pnode.get(e.getTarget()));
            graph.addEdge(C2Pnode.get(e.getSource()), C2Pnode.get(e.getTarget()));
        }
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println("End of Debug output for puModel generation of " + currentRole.name);
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

        return new PublicModel(graph, currentRole.name + "PuM");
    }

    /**
     * Puts the node in the C2Pnode data structure. Each interaction type is
     * annotated with its name, as well as sender/receiver info (if applicable)
     */
    private static void putNodeInC2PNode(Map<IChoreographyNode, IPublicNode> C2Pnode, Interaction node,
                                         Role currentRole) {

        //TODO: Filter out instances, where one participant contains more than one HOW as receiver

        // Throw exception if interaction node has none of the defined types
        if (!InteractionType.MESSAGE_EXCHANGE.equals(node.getInteractionType())
                && !InteractionType.HANDOVER_OF_WORK.equals(node.getInteractionType())
                && !InteractionType.SHARED_RESOURCE.equals(node.getInteractionType())
                && !InteractionType.SYNCHRONOUS_ACTIVITY.equals(node.getInteractionType())) {
            throw new IllegalStateException(
                    "Interaction node can not be matched to any valid interaction type. Please ensure that each Interaction node gets assigned a valid type");
        }

        if (node.getParticipant1().name.equals(currentRole.name))
            C2Pnode.put(node, new Send(node.getParticipant2(), node.getMessage(),
                    getAnnotationOfInteraction(node.getInteractionType(), node.getName(), true)));
        else if (node.getParticipant2().name.equals(currentRole.name))
            C2Pnode.put(node, new Receive(node.getParticipant1(), node.getMessage(),
                    getAnnotationOfInteraction(node.getInteractionType(), node.getName(), false)));
        else
            System.out.println("currentrole " + currentRole + "not identified in interaction " + node);

    }

    /**
     * Computes the String an interaction node is annotated with in the resulting
     * graph
     */
    private static String getAnnotationOfInteraction(InteractionType interactionType, String nodeName,
                                                     boolean isParticipant1) {

        switch (interactionType) {
            case MESSAGE_EXCHANGE:
                return "M: " + nodeName + (isParticipant1 ? " (s)" : " (r)");
            case HANDOVER_OF_WORK:
                return "H: " + nodeName + (isParticipant1 ? " (s)" : " (r)");
            case SHARED_RESOURCE:
                return "R: " + nodeName + (isParticipant1 ? " (p1)" : " (p2)");
            case SYNCHRONOUS_ACTIVITY:
                return "S: " + nodeName + (isParticipant1 ? " (p1)" : " (p2)");
        }

        throw new IllegalArgumentException("The interaction type has to be defined");
    }

    public static IPrivateModel transformPubModel2PrModel(IPublicModel puModel, Role currentRole) {
        MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode> graph = new MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode>();
        Map<IPublicNode, IPrivateNode> puNode2prNode = new HashMap<IPublicNode, IPrivateNode>();

        for (IPublicNode node : puModel.getdigraph().getVertices()) {
            if (node instanceof Event) {
                puNode2prNode.put(node, (Event) node);
            } else if (node instanceof Gateway) {
                puNode2prNode.put(node, (Gateway) node);
            } else if (node instanceof Send) {
                puNode2prNode.put(node, (Send) node);
            } else if (node instanceof Receive) {
                puNode2prNode.put(node, (Receive) node);
            } else {
                System.out.println("basically not possible");
            }
        }

        for (Edge<IPublicNode> e : puModel.getdigraph().getEdges()) {
            graph.addEdge(puNode2prNode.get(e.getSource()), puNode2prNode.get(e.getTarget()));
        }

        return new PrivateModel(graph, currentRole.name + "_PrM");
    }

}
