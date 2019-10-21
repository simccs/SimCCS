package solver;

import dataStore.DataStorer;
import dataStore.Edge;
import dataStore.Source;
import dataStore.Sink;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import javafx.scene.control.TextArea;
import static utilities.Utilities.*;

/**
 *
 * @author yaw
 */
public class Solver {

    private DataStorer data;
    private TextArea messenger;

    public Solver(DataStorer data) {
        this.data = data;
    }

    // Find shortest path between each source/sink pair through cost surface
    public Object[] generateAllPairShortestPaths() {
        ArrayList<int[]> allPathsList = new ArrayList<>();
        ArrayList<Double> allPathCostsList = new ArrayList<>();
        int[] sourcesAndSinks = data.getSourceSinkCells();

        // Check for sources/sinks out of cost surface
        HashSet<Source> unreachableSources = new HashSet<>();
        for (Source src : data.getSources()) {
            boolean reachable = false;
            int cellNum = src.getCellNum();
            for (int neighborCell : data.getNeighborCells(cellNum)) {
                if (data.getEdgeWeight(cellNum, neighborCell, "r") < Double.MAX_VALUE) {
                    reachable = true;
                }
            }
            if (!reachable) {
                unreachableSources.add(src);
            }
        }

        HashSet<Sink> unreachableSinks = new HashSet<>();
        for (Sink snk : data.getSinks()) {
            boolean reachable = false;
            int cellNum = snk.getCellNum();
            for (int neighborCell : data.getNeighborCells(cellNum)) {
                if (data.getEdgeWeight(cellNum, neighborCell, "r") < Double.MAX_VALUE) {
                    reachable = true;
                }
            }
            if (!reachable) {
                unreachableSinks.add(snk);
            }
        }

        if (!unreachableSources.isEmpty() || !unreachableSinks.isEmpty()) {
            String message = "";
            if (!unreachableSources.isEmpty()) {
                message += "Sources outside of cost surface: ";
                for (Source src : unreachableSources) {
                    message += src.getLabel() + ", ";
                }
                message = message.substring(0, message.length() - 2);
                message += "\n";
            }

            if (!unreachableSinks.isEmpty()) {
                message += "Sinks outside of cost surface: ";
                for (Sink snk : unreachableSinks) {
                    message += snk.getLabel() + ", ";
                }
                message = message.substring(0, message.length() - 2);
            }
            messenger.setText(message);
        } else {
            for (int nodeNum = 0; nodeNum < sourcesAndSinks.length - 1; nodeNum++) {
                int[] destinations = new int[sourcesAndSinks.length - nodeNum - 1];
                System.arraycopy(sourcesAndSinks, nodeNum + 1, destinations, 0, destinations.length);
                Object[] sourcePathsAndCosts = dijkstra(sourcesAndSinks[nodeNum], destinations, .9999999);
                allPathsList.addAll((ArrayList<int[]>) sourcePathsAndCosts[0]);
                allPathCostsList.addAll((ArrayList<Double>) sourcePathsAndCosts[1]);
            }
            int[][] allPaths = allPathsList.toArray(new int[0][0]);
            double[] allPathCosts = convertDoubleArray(allPathCostsList.toArray(new Double[0]));
            return new Object[]{allPaths, allPathCosts};
        }
        return null;
    }

    public HashSet<Edge> generateDelaunayPairs() {
        Delaunay d = new Delaunay(data);
        int[] locations = data.getSourceSinkCells();
        return d.run(locations);
    }

    public Object[] generateDelunayCanidateGraphSpeedUp() {

        //data.generateDelaunayPairs();
        HashSet<Edge> delaunayPairs = data.getDelaunayPairs();
        HashMap<Edge, Double> graphEdgeCosts = new HashMap<>();
        HashMap<Edge, int[]> graphEdgeRoutes = new HashMap<>();
        HashMap<Integer, HashSet<Integer>> vertexNeighbors = new HashMap<>();    // Neighbors of a given vertex
        HashSet<Integer> sourceSinksList = new HashSet<>(); // List of source and sink vertices
        HashSet<Integer> degree2Vertices = new HashSet<>(); // Non-source/sink vertices with degree 2

        // Populate initial costs, routes, and neighbors
        for (Edge pair : delaunayPairs) {
                   int pair2List[] = {pair.v2}; 
                   int[] path = ((ArrayList<int[]>)(dijkstra(pair.v1, pair2List,.9999999)[0])).get(0);

                    for (int i = 0; i < path.length - 1; i++) {
                        Edge e = new Edge(path[i], path[i + 1]);
                        graphEdgeCosts.put(e, data.getEdgeWeight(path[i], path[i + 1], "c"));
                        graphEdgeRoutes.put(e, new int[]{path[i], path[i + 1]});

                        // Add neighbor of i and i+1
                        if (!vertexNeighbors.containsKey(path[i])) {
                            vertexNeighbors.put(path[i], new HashSet<>());
                        }
                        vertexNeighbors.get(path[i]).add(path[i + 1]);
                        if (!vertexNeighbors.containsKey(path[i + 1])) {
                            vertexNeighbors.put(path[i + 1], new HashSet<>());
                        }
                        vertexNeighbors.get(path[i + 1]).add(path[i]);
                    }
        }

            // Populate vertex lists
            for (int cell : data.getSourceSinkCells()) {
                sourceSinksList.add(cell);
            }

            // Make set of removable degree 2 vertices
            for (int vertex : vertexNeighbors.keySet()) {
                if (!sourceSinksList.contains(vertex) && vertexNeighbors.get(vertex).size() == 2) {
                    degree2Vertices.add(vertex);
                }
            }

            // Reduce degree 2 vertices
            boolean degree2Removed = true;
            while (degree2Removed) {
                degree2Removed = false;
                for (Iterator<Integer> iter = degree2Vertices.iterator(); iter.hasNext();) {
                    int vertex = iter.next();
                    int[] neighbors = convertIntegerArray(vertexNeighbors.get(vertex).toArray(new Integer[0]));
                    // Only remove if it won't create multi-edges.
                    Edge newEdge = new Edge(neighbors[0], neighbors[1]);
                    if (!graphEdgeCosts.containsKey(newEdge)) {
                        degree2Removed = true;

                        // Get old edges
                        Edge oldEdge1 = new Edge(neighbors[0], vertex);
                        Edge oldEdge2 = new Edge(vertex, neighbors[1]);

                        // Add new edge to edgeCosts
                        double newCost = graphEdgeCosts.get(oldEdge1) + graphEdgeCosts.get(oldEdge2);
                        graphEdgeCosts.put(newEdge, newCost);

                        // Remove old edge from edgeCosts
                        graphEdgeCosts.remove(oldEdge1);
                        graphEdgeCosts.remove(oldEdge2);

                        // Add route for new edge to edgeRoutes
                        int[] oldRoute1 = graphEdgeRoutes.get(oldEdge1);
                        int[] oldRoute2 = graphEdgeRoutes.get(oldEdge2);
                        int[] newRoute = new int[oldRoute1.length + oldRoute2.length - 1];
                        if (oldRoute1[oldRoute1.length - 1] == vertex) {
                            for (int i = 0; i < oldRoute1.length; i++) {
                                newRoute[i] = oldRoute1[i];
                            }
                        } else {
                            for (int i = 0; i < oldRoute1.length; i++) {
                                newRoute[oldRoute1.length - 1 - i] = oldRoute1[i];
                            }
                        }
                        if (oldRoute2[0] == vertex) {
                            for (int i = 1; i < oldRoute2.length; i++) {
                                newRoute[i + oldRoute1.length - 1] = oldRoute2[i];
                            }
                        } else {
                            for (int i = 0; i < oldRoute2.length - 1; i++) {
                                newRoute[oldRoute1.length - 1 + oldRoute2.length - 1 - i] = oldRoute2[i];
                            }
                        }
                        graphEdgeRoutes.put(newEdge, newRoute);

                        // Remove route for old edge from edgeRoutes
                        graphEdgeRoutes.remove(oldEdge1);
                        graphEdgeRoutes.remove(oldEdge2);

                        // Add neighbors for endpoint of new edge to vertexNeighbors
                        vertexNeighbors.get(neighbors[0]).add(neighbors[1]);
                        vertexNeighbors.get(neighbors[0]).remove(vertex);
                        vertexNeighbors.get(neighbors[1]).add(neighbors[0]);
                        vertexNeighbors.get(neighbors[1]).remove(vertex);

                        // Remove old vertex from vertexNeighbors
                        vertexNeighbors.remove(vertex);

                        // Remove old vertex from degree2Vertices
                        iter.remove();
                    }
                }
            }
            int[] vertices = new int[vertexNeighbors.keySet().size()];
            int i = 0;
            for (int vertex : vertexNeighbors.keySet()) {
                vertices[i++] = vertex;
            }
            Arrays.sort(vertices);
            return new Object[]{vertices, graphEdgeCosts, graphEdgeRoutes};

    }
    
    // Delaunay-based candidate graph.
    public Object[] generateDelaunayCandidateGraph() {
        //data.generateDelaunayPairs();
        HashSet<Edge> delaunayPairs = data.getDelaunayPairs();
        HashMap<Edge, Double> graphEdgeCosts = new HashMap<>();
        HashMap<Edge, int[]> graphEdgeRoutes = new HashMap<>();
        HashMap<Integer, HashSet<Integer>> vertexNeighbors = new HashMap<>();    // Neighbors of a given vertex
        HashSet<Integer> sourceSinksList = new HashSet<>(); // List of source and sink vertices
        HashSet<Integer> degree2Vertices = new HashSet<>(); // Non-source/sink vertices with degree 2

        // Populate initial costs, routes, and neighbors
        int[][] apShortestPaths = data.getShortestPathEdges();
        if (apShortestPaths != null) {
            for (Edge pair : delaunayPairs) {
                for (int[] path : apShortestPaths) {
                    if ((path[0] == pair.v1 && path[path.length - 1] == pair.v2) || (path[0] == pair.v2 && path[path.length - 1] == pair.v1)) {
                        for (int i = 0; i < path.length - 1; i++) {
                            Edge e = new Edge(path[i], path[i + 1]);
                            graphEdgeCosts.put(e, data.getEdgeWeight(path[i], path[i + 1], "c"));
                            graphEdgeRoutes.put(e, new int[]{path[i], path[i + 1]});

                            // Add neighbor of i and i+1
                            if (!vertexNeighbors.containsKey(path[i])) {
                                vertexNeighbors.put(path[i], new HashSet<>());
                            }
                            vertexNeighbors.get(path[i]).add(path[i + 1]);
                            if (!vertexNeighbors.containsKey(path[i + 1])) {
                                vertexNeighbors.put(path[i + 1], new HashSet<>());
                            }
                            vertexNeighbors.get(path[i + 1]).add(path[i]);
                        }
                    }
                }
            }

            // Populate vertex lists
            for (int cell : data.getSourceSinkCells()) {
                sourceSinksList.add(cell);
            }

            // Make set of removable degree 2 vertices
            for (int vertex : vertexNeighbors.keySet()) {
                if (!sourceSinksList.contains(vertex) && vertexNeighbors.get(vertex).size() == 2) {
                    degree2Vertices.add(vertex);
                }
            }

            // Reduce degree 2 vertices
            boolean degree2Removed = true;
            while (degree2Removed) {
                degree2Removed = false;
                for (Iterator<Integer> iter = degree2Vertices.iterator(); iter.hasNext();) {
                    int vertex = iter.next();
                    int[] neighbors = convertIntegerArray(vertexNeighbors.get(vertex).toArray(new Integer[0]));
                    // Only remove if it won't create multi-edges.
                    Edge newEdge = new Edge(neighbors[0], neighbors[1]);
                    if (!graphEdgeCosts.containsKey(newEdge)) {
                        degree2Removed = true;

                        // Get old edges
                        Edge oldEdge1 = new Edge(neighbors[0], vertex);
                        Edge oldEdge2 = new Edge(vertex, neighbors[1]);

                        // Add new edge to edgeCosts
                        double newCost = graphEdgeCosts.get(oldEdge1) + graphEdgeCosts.get(oldEdge2);
                        graphEdgeCosts.put(newEdge, newCost);

                        // Remove old edge from edgeCosts
                        graphEdgeCosts.remove(oldEdge1);
                        graphEdgeCosts.remove(oldEdge2);

                        // Add route for new edge to edgeRoutes
                        int[] oldRoute1 = graphEdgeRoutes.get(oldEdge1);
                        int[] oldRoute2 = graphEdgeRoutes.get(oldEdge2);
                        int[] newRoute = new int[oldRoute1.length + oldRoute2.length - 1];
                        if (oldRoute1[oldRoute1.length - 1] == vertex) {
                            for (int i = 0; i < oldRoute1.length; i++) {
                                newRoute[i] = oldRoute1[i];
                            }
                        } else {
                            for (int i = 0; i < oldRoute1.length; i++) {
                                newRoute[oldRoute1.length - 1 - i] = oldRoute1[i];
                            }
                        }
                        if (oldRoute2[0] == vertex) {
                            for (int i = 1; i < oldRoute2.length; i++) {
                                newRoute[i + oldRoute1.length - 1] = oldRoute2[i];
                            }
                        } else {
                            for (int i = 0; i < oldRoute2.length - 1; i++) {
                                newRoute[oldRoute1.length - 1 + oldRoute2.length - 1 - i] = oldRoute2[i];
                            }
                        }
                        graphEdgeRoutes.put(newEdge, newRoute);

                        // Remove route for old edge from edgeRoutes
                        graphEdgeRoutes.remove(oldEdge1);
                        graphEdgeRoutes.remove(oldEdge2);

                        // Add neighbors for endpoint of new edge to vertexNeighbors
                        vertexNeighbors.get(neighbors[0]).add(neighbors[1]);
                        vertexNeighbors.get(neighbors[0]).remove(vertex);
                        vertexNeighbors.get(neighbors[1]).add(neighbors[0]);
                        vertexNeighbors.get(neighbors[1]).remove(vertex);

                        // Remove old vertex from vertexNeighbors
                        vertexNeighbors.remove(vertex);

                        // Remove old vertex from degree2Vertices
                        iter.remove();
                    }
                }
            }
            int[] vertices = new int[vertexNeighbors.keySet().size()];
            int i = 0;
            for (int vertex : vertexNeighbors.keySet()) {
                vertices[i++] = vertex;
            }
            Arrays.sort(vertices);
            return new Object[]{vertices, graphEdgeCosts, graphEdgeRoutes};
        } else {
            return null;
        }
    }

    public Object[] makeComponentCosts() {
        HashMap<Edge, int[]> graphEdgeRoutes = data.getGraphEdgeRoutes();
        HashMap<Edge, Double> constructionCosts = new HashMap<>();
        HashMap<Edge, Double> rightOfWayCosts = new HashMap<>();

        for (Edge edge : graphEdgeRoutes.keySet()) {
            int[] route = graphEdgeRoutes.get(edge);
            double constructionCost = 0;
            double rightOfWayCost = 0;
            for (int i = 0; i < route.length - 1; i++) {
                rightOfWayCost += data.getEdgeRightOfWayCost(route[i], route[i + 1]);
                constructionCost += data.getEdgeConstructionCost(route[i], route[i + 1]);
            }
            constructionCosts.put(edge, constructionCost);
            rightOfWayCosts.put(edge, rightOfWayCost);
        }
        return new Object[]{rightOfWayCosts, constructionCosts};
    }

    // Modfidied dijkstra for operating on cost surface and allowing to change edge weights
    public Object[] dijkstra(int src, int[] destinations, double edgeCostModification) {
        HashSet<Integer> connectedDests = new HashSet<>();
        for (int cell : destinations) {
            connectedDests.add(cell);
        }
        int numNodes = data.getHeight() * data.getWidth() + 1;
        PriorityQueue<Data> pQueue = new PriorityQueue<>(numNodes);
        double[] costs = new double[numNodes];
        int[] previous = new int[numNodes];
        Data[] map = new Data[numNodes];

        for (int cellNum = 0; cellNum < numNodes; cellNum++) {
            costs[cellNum] = Double.MAX_VALUE;
            previous[cellNum] = -1;
            map[cellNum] = new Data(cellNum, costs[cellNum]);
        }

        costs[src] = 0;
        map[src].distance = 0;
        pQueue.add(map[src]);

        while (!pQueue.isEmpty()) {
            Data u = pQueue.poll();
            if (!u.connected) {
                u.connected = true;
                connectedDests.remove(u.cellNum);
                if (!connectedDests.isEmpty()) {
                    for (int neighborCell : data.getNeighborCells(u.cellNum)) {
                        if (neighborCell != 0) {
                            double altDistance = costs[u.cellNum] + data.getModifiedEdgeRoutingCost(u.cellNum, neighborCell);
                            if (altDistance < costs[neighborCell] && !map[neighborCell].connected) {
                                costs[neighborCell] = altDistance;
                                previous[neighborCell] = u.cellNum;

                                map[neighborCell].distance = altDistance;
                                pQueue.add(map[neighborCell]);
                            }
                        }
                    }
                } else {
                    pQueue.clear();
                }
            }
        }

        // Build paths for nodes of interest.
        ArrayList<int[]> paths = new ArrayList<>();
        ArrayList<Double> pathCosts = new ArrayList<>();
        for (int dest : destinations) {
            ArrayList<Integer> pathList = new ArrayList<>();
            int node = dest;
            while (node != src) {
                pathList.add(0, node);
                node = previous[node];
            }
            pathList.add(0, node);

            // Modify edge costs and recalculate real cost
            double cost = 0;
            for (int i = 0; i < pathList.size() - 1; i++) {
                cost += data.getEdgeWeight(pathList.get(i), pathList.get(i + 1), "c");
                data.updateModifiedEdgeRoutingCost(pathList.get(i), pathList.get(i + 1), edgeCostModification);
                data.updateModifiedEdgeRoutingCost(pathList.get(i + 1), pathList.get(i), edgeCostModification);
            }
            pathCosts.add(cost);
            paths.add(convertIntegerArray(pathList.toArray(new Integer[0])));
        }
        return new Object[]{paths, pathCosts};
    }

    public void setMessenger(TextArea messenger) {
        this.messenger = messenger;
    }

    public TextArea getMessenger() {
        return messenger;
    }

    private class Data implements Comparable<Data> {

        public int cellNum;
        public double distance;
        public boolean connected = false;

        public Data(int cellNum, double distance) {
            this.cellNum = cellNum;
            this.distance = distance;
        }

        @Override
        public int compareTo(Data other) {
            return Double.valueOf(distance).compareTo(other.distance);
        }

        @Override
        public int hashCode() {
            return cellNum;
        }

        public boolean equals(Data other) {
            return distance == other.distance;
        }
    }
}
