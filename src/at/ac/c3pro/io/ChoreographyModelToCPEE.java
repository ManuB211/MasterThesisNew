package at.ac.c3pro.io;

import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.node.*;
import at.ac.c3pro.util.CPEETemplates;
import at.ac.c3pro.util.GlobalTimestamp;
import at.ac.c3pro.util.OutputHandler;
import org.jbpt.graph.abs.IDirectedGraph;
import org.jbpt.utils.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class ChoreographyModelToCPEE {

    Map<Role, IDirectedGraph<Edge<IPublicNode>, IPublicNode>> role2PublicModel;

    StringBuilder sb;
    Random rand;

    String outputPath;

    public ChoreographyModelToCPEE(Map<Role, IDirectedGraph<Edge<IPublicNode>, IPublicNode>> input) throws IOException {
        this.role2PublicModel = input;
        this.sb = new StringBuilder();
        this.rand = new Random();
        OutputHandler.createOutputFolder("CPEE");
        outputPath = GlobalTimestamp.timestamp + "/CPEE/";
    }

    public void run() {

        for (Map.Entry<Role, IDirectedGraph<Edge<IPublicNode>, IPublicNode>> entry : this.role2PublicModel.entrySet()) {

            System.out.println("=============================================");
            System.out.println("Creating CPEE Model for " + entry.getKey().getName());
            System.out.println("=============================================");
            String cpeeContent = createCPEEFile(entry.getValue());

            String filename = "cpee_" + entry.getKey().name + ".xml";
            IOUtils.toFile(outputPath + filename, cpeeContent);
            sb = new StringBuilder();

            System.out.println("=============================================");
            System.out.println("Finished Creating CPEE Model");
            System.out.println("=============================================");
        }

    }

    /**
     * Do an aapted DFS to traverse the graph in the order needed to insert CPEE elements "on the fly"
     */
    private String createCPEEFile(IDirectedGraph<Edge<IPublicNode>, IPublicNode> graph) {
        IPublicNode beginNode = graph.getVertices().stream().filter(v -> v.getName().equals("start")).collect(Collectors.toList()).get(0);
        Map<IPublicNode, Boolean> visited = graph.getVertices().stream().collect(Collectors.toMap(v -> v, v -> false));

        visited.put(beginNode, Boolean.TRUE);

        List<IPublicNode> queue = new ArrayList<>();
        queue.add(beginNode);

        while (!queue.isEmpty()) {

            IPublicNode curr = queue.remove(queue.size() - 1);
            visited.put(curr, Boolean.TRUE);

            if (curr instanceof Receive || curr instanceof Send) {

                //Add corresponding template to the string builder
                addInteraction(curr);

                System.out.println("Interaction " + curr.getName());
                queue.addAll(graph.getDirectSuccessors(curr));
            } else if (curr instanceof AndGateway) {

                if (curr.getName().endsWith("_m")) {
                    System.out.println("And merge " + curr.getName());

                    //Only continue if all branches of AND have already been traversed/added
                    List<IPublicNode> pred = new ArrayList<>(graph.getDirectPredecessors(curr));

                    sb.append(CPEETemplates.PARALLEL_BRANCH_END);


                    if (pred.stream().allMatch(visited::get)) {
                        sb.append(CPEETemplates.PARALLEL_END);
                        queue.addAll(graph.getDirectSuccessors(curr));
                    } else {
                        sb.append(CPEETemplates.PARALLEL_BRANCH_START);
                    }

                } else {
                    System.out.println("And fork " + curr.getName());
                    sb.append(CPEETemplates.PARALLEL_START);
                    sb.append(CPEETemplates.PARALLEL_BRANCH_START);
                    queue.addAll(graph.getDirectSuccessors(curr));
                }

            } else if (curr instanceof XorGateway) {

                if (curr.getName().endsWith("_m")) {
                    System.out.println("XOR merge " + curr.getName());

                    //Only continue if all branches of AND have already been traversed/added
                    List<IPublicNode> pred = new ArrayList<>(graph.getDirectPredecessors(curr));

                    sb.append(CPEETemplates.EXCLUSIVE_BRANCH_END);

                    if (pred.stream().allMatch(visited::get)) {
                        sb.append(CPEETemplates.EXCLUSIVE_END);
                        queue.addAll(graph.getDirectSuccessors(curr));
                    } else {
                        sb.append(CPEETemplates.EXCLUSIVE_BRANCH_START);
                    }
                } else {
                    System.out.println("XOR fork " + curr.getName());
                    sb.append(CPEETemplates.EXCLUSIVE_START);
                    sb.append(CPEETemplates.EXCLUSIVE_BRANCH_START);

                    //always put the XOR_m if present last to ensure the optional step being present in the generated model
                    List<IPublicNode> succWorkingSet = new ArrayList<>(graph.getDirectSuccessors(curr));
                    IPublicNode merge = null;
                    List<IPublicNode> succ = new ArrayList<>();
                    for (IPublicNode s : succWorkingSet) {
                        if (s.getName().equals(curr.getName() + "_m")) {
                            merge = s;
                        } else {
                            succ.add(s);
                        }
                    }

                    if (merge != null) {
                        succ.add(merge);
                    }

                    queue.addAll(succ);
                }

            } else {
                //Event
                if (curr.getName().equals("start")) {
                    //Add all successors to queue
                    queue.addAll(graph.getDirectSuccessors(curr));
                } else {
                    //end

                    String content = sb.toString();

                    String rst = CPEETemplates.SKELETON.replaceFirst("\\?", GlobalTimestamp.timestamp);
                    return rst.replace("?", content);
                }

            }
        }

        return null;
    }

    /**
     * Argument Identification will always be "IAx"
     */
    private void addInteraction(IPublicNode node) {
        String[] nameSplit = node.getName().split(" ");
        String interactionType = nameSplit[0];

        String identifier = GlobalTimestamp.timestamp + "__" + nameSplit[1].split("\\(")[0];

        //Create random amount of seconds between 1 and 8 for the timeout
        Integer randomTimeout = rand.nextInt(8) + 1;

        if (interactionType.equals("S:")) {
            sb.append(CPEETemplates.getTimeout(identifier + "_t", randomTimeout));
            sb.append(CPEETemplates.getSync(identifier, identifier));
        } else if (interactionType.equals("R:")) {
            sb.append(CPEETemplates.getTimeoutWithLabel(identifier));
        } else if (interactionType.equals("M:") || interactionType.equals("H:")) {
            sb.append(CPEETemplates.getTimeout(identifier + "_t", randomTimeout));

            //Message receive
            if (nameSplit[2].equals("(r)")) {
                sb.append(CPEETemplates.getMessageReceive(identifier, identifier));
            } else {
                //Message Send
                sb.append(CPEETemplates.getMessageSend(identifier, identifier));
            }
        } else {
            System.out.println("Something went terribly wrong here");
            //Exception
        }


    }

}
