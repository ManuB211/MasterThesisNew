package at.ac.c3pro.util;

import at.ac.c3pro.node.*;
import org.jbpt.graph.abs.IDirectedGraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GraphHelper {

    /**
     * Performs a BFS starting with the end node and returns a list of all nodes or all interactions on the way (dependent on parameter)
     * Version for the IPublicNodes
     */
    public static List<IPublicNode> performBackwardsBFSPublic(IDirectedGraph<Edge<IPublicNode>, IPublicNode> graph, IPublicNode start, boolean onlyInteractions) {
        List<IPublicNode> queue = new ArrayList<>(Collections.singleton(start));
        List<IPublicNode> explored = new ArrayList<>(Collections.singletonList(start));

        //End node has to be in the result
        List<IPublicNode> bfsResult = new ArrayList<>(Collections.singletonList(start));
        while (!queue.isEmpty()) {

            IPublicNode currNode = queue.remove(0);

            if (onlyInteractions) {
                if (currNode instanceof Receive || currNode instanceof Send) {
                    bfsResult.add(currNode);
                }
            } else {
                bfsResult.add(currNode);
            }

            List<IPublicNode> parentsOfCurr = new ArrayList<>(graph.getDirectPredecessors(currNode));

            for (IPublicNode parent : parentsOfCurr) {
                if (!explored.contains(parent)) {
                    explored.add(parent);
                    queue.add(parent);
                }
            }
        }
        return bfsResult;
    }

    /**
     * Performs a BFS starting with the end node and returns a list of all nodes or all interactions on the way (dependent on parameter)
     * Version for the IPrivateNodes
     */
    public static List<IPrivateNode> performBackwardsBFSPrivate(IDirectedGraph<Edge<IPrivateNode>, IPrivateNode> graph, IPrivateNode start, boolean onlyInteractions) {
        List<IPrivateNode> queue = new ArrayList<>(Collections.singleton(start));
        List<IPrivateNode> explored = new ArrayList<>(Collections.singletonList(start));

        //End node has to be in the result
        List<IPrivateNode> bfsResult = new ArrayList<>(Collections.singletonList(start));
        while (!queue.isEmpty()) {

            IPrivateNode currNode = queue.remove(0);

            if (onlyInteractions) {
                if (currNode instanceof Receive || currNode instanceof Send) {
                    bfsResult.add(currNode);
                }
            } else {
                bfsResult.add(currNode);
            }

            List<IPrivateNode> parentsOfCurr = new ArrayList<>(graph.getDirectPredecessors(currNode));

            for (IPrivateNode parent : parentsOfCurr) {
                if (!explored.contains(parent)) {
                    explored.add(parent);
                    queue.add(parent);
                }
            }
        }
        return bfsResult;
    }


}
