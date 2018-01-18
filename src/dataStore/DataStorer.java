package dataStore;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import solver.Solver;

/**
 *
 * @author yaw
 */
public class DataStorer {

    public String basePath;
    public String dataset;
    public String scenario;

    private Solver solver;

    // Geospatial data.
    private int width;
    private int height;
    private double lowerLeftX;
    private double lowerLeftY;
    private double cellSize;

    // Source and sink data.
    private Source[] sources;
    private Sink[] sinks;
    private LinearComponent[] linearComponents;
    private int[] sourceSinkCellLocations;  // Cell number for each source and sink node

    // Raw network information
    private int[][] shortestPaths;   // [pathNum] = [nodeInPath1, nodeInPath2,...]
    private double[] shortestPathCosts;  //[pathNum] = costForPathNum
    private double[][] adjacencyCosts;
    private double[][] modifiedAdjacencyCosts;
    private double[][] rightOfWayCosts;
    private double[][] constructionCosts;

    // Candidate network graph information
    private int[] graphVertices;    // Set of all vertices in graph (source/sink/junction)
    private HashMap<Edge, Double> graphEdgeCosts;   // Cost for each edge between vertices
    private HashMap<Edge, int[]> graphEdgeRoutes;   // Cell-to-cell route for each edge between vertices
    private HashMap<Edge, Double> graphEdgeRightOfWayCosts;   // Cost for each edge between vertices
    private HashMap<Edge, Double> graphEdgeConstructionCosts;   // Cost for each edge between vertices
    private HashSet<Edge> delaunayPairs;

    private HashMap<Edge, ArrayList<Edge>> sourceSinkRoutes;

    public DataStorer(String basePath, String dataset, String scenario) {
        this.basePath = basePath;
        this.dataset = dataset;
        this.scenario = scenario;
    }

    public int[][] getShortestPathEdges() {
        if (shortestPaths == null) {
            generateShortestPaths();
        }
        return shortestPaths;
    }

    // Get raw Delaunay edges.
    public HashSet<int[]> getDelaunayEdges() {
        if (delaunayPairs == null) {
            generateDelaunayPairs();
        }
        HashSet<int[]> edges = new HashSet<>();
        for (Edge e : delaunayPairs) {
            edges.add(new int[]{e.v1, e.v2});
        }
        return edges;
    }

    public HashSet<int[]> getGraphEdges() {
        if (graphEdgeRoutes == null) {
            generateCandidateGraph();
        }
        return new HashSet<>(graphEdgeRoutes.values());
    }

    public void generateShortestPaths() {
        loadNetworkCosts();

        Object[] pathDetails = solver.generateAllPairShortestPaths();
        shortestPaths = (int[][]) pathDetails[0];
        shortestPathCosts = (double[]) pathDetails[1];
        DataInOut.saveShortestPathsNetwork();
    }

    public void generateDelaunayPairs() {
        delaunayPairs = solver.generateDelaunayPairs();
        DataInOut.saveDelaunayPairs();
    }

    public void generateCandidateGraph() {
        loadNetworkCosts();
        generateDelaunayPairs();
        
        Object[] graphComponents = solver.generateDelaunayCandidateGraph();
        graphVertices = (int[]) graphComponents[0];
        graphEdgeCosts = (HashMap<Edge, Double>) graphComponents[1];
        graphEdgeRoutes = (HashMap<Edge, int[]>) graphComponents[2];
        DataInOut.saveCandidateGraph();

        // Make right of way and construction costs
        Object[] costComponents = solver.makeComponentCosts();
        graphEdgeRightOfWayCosts = (HashMap<Edge, Double>) costComponents[0];
        graphEdgeConstructionCosts = (HashMap<Edge, Double>) costComponents[1];
    }

    public void loadNetworkCosts() {
        if (adjacencyCosts == null) {
            DataInOut.loadCosts();

            // Make right of way and construction costs
            Object[] costComponents = solver.makeComponentCosts();
            graphEdgeRightOfWayCosts = (HashMap<Edge, Double>) costComponents[0];
            graphEdgeConstructionCosts = (HashMap<Edge, Double>) costComponents[1];
        }
    }

    public Set<Integer> getJunctions() {
        if (graphVertices == null) {
            generateCandidateGraph();
        }

        HashSet<Integer> junctions = new HashSet<>();
        for (int vertex : graphVertices) {
            junctions.add(vertex);
        }
        for (Source source : sources) {
            junctions.remove(source.getCellNum());
        }
        for (Sink sink : sinks) {
            junctions.remove(sink.getCellNum());
        }

        return junctions;
    }

    public int[] getGraphVertices() {
        if (graphVertices == null) {
            generateCandidateGraph();
        }
        return graphVertices;
    }

    public HashMap<Edge, Double> getGraphEdgeCosts() {
        if (graphEdgeCosts == null) {
            generateCandidateGraph();
        }
        return graphEdgeCosts;
    }

    public HashMap<Edge, Double> getGraphEdgeRightOfWawCosts() {
        if (graphEdgeRightOfWayCosts == null) {
            generateCandidateGraph();
        }
        return graphEdgeRightOfWayCosts;
    }

    public HashMap<Edge, Double> getGraphEdgeConstructionCosts() {
        if (graphEdgeConstructionCosts == null) {
            generateCandidateGraph();
        }
        return graphEdgeConstructionCosts;
    }

    public HashMap<Edge, int[]> getGraphEdgeRoutes() {
        if (graphEdgeRoutes == null) {
            generateCandidateGraph();
        }
        return graphEdgeRoutes;
    }

    public HashSet<Edge> getDelaunayPairs() {
        if (delaunayPairs == null) {
            generateDelaunayPairs();
        }
        return delaunayPairs;
    }

    // Get edge cost in base cost surface.
    public double getEdgeCost(int cell1, int cell2) {
        if (cell1 == cell2) {
            return 0;
        } else if (getNeighborNum(cell1, cell2) >= 0 && getNeighborNum(cell1, cell2) < 8) {
            return adjacencyCosts[cell1][getNeighborNum(cell1, cell2)];
        }
        return Double.MAX_VALUE;
    }

    public double getEdgeRightOfWayCost(int cell1, int cell2) {
        if (cell1 == cell2) {
            return 0;
        } else if (getNeighborNum(cell1, cell2) >= 0 && getNeighborNum(cell1, cell2) < 8) {
            return rightOfWayCosts[cell1][getNeighborNum(cell1, cell2)];
        }
        return Double.MAX_VALUE;
    }

    public double getEdgeConstructionCost(int cell1, int cell2) {
        if (cell1 == cell2) {
            return 0;
        } else if (getNeighborNum(cell1, cell2) >= 0 && getNeighborNum(cell1, cell2) < 8) {
            return constructionCosts[cell1][getNeighborNum(cell1, cell2)];
        }
        return Double.MAX_VALUE;
    }

    public double[] cellLocationToRawXY(int cell) {
        // NOTE: Cell counting starts at 1, not 0.
        int y = cell / width + 1;
        int x = cell - (y - 1) * width;
        return new double[]{x, y};
    }

    public double[] latLonToXY(double lat, double lon) {
        double y = height - (((lat - lowerLeftY) / cellSize) + 1) + 1;
        double x = (lon - lowerLeftX) / cellSize;
        return new double[]{x, y};
    }

    // Lat/lon to cell number.
    public int latLonToCell(double lat, double lon) {
        // NOTE: Cell counting starts at 1, not 0.
        int y = height - ((int) ((lat - lowerLeftY) / cellSize) + 1) + 1;
        int x = (int) ((lon - lowerLeftX) / cellSize) + 1;
        return xyToVectorized(x, y);
    }

    // Row/column to cell number.
    public int xyToVectorized(int x, int y) {
        // NOTE: Cell counting starts at 1, not 0.
        return (y - 1) * width + x;
    }

    // Cell to lat/lon
    public double[] cellToLatLon(int cell) {
        double[] xy = cellLocationToRawXY(cell);
        xy[0] -= .5;
        xy[1] -= .5;
        double lat = (height - xy[1]) * cellSize + lowerLeftY;
        double lon = xy[0] * cellSize + lowerLeftX;
        return new double[]{lat, lon};
    }

    public int[] getNeighborCells(int cellNum) {
        // NOTE: Neighbor numbering starts in upper left as 0 and goes in clockwise direction.
        int[] neighbors = {cellNum - width - 1, cellNum - width, cellNum - width + 1, cellNum + 1, cellNum + width + 1, cellNum + width, cellNum + width - 1, cellNum - 1};
        for (int i = 0; i < neighbors.length; i++) {
            if (neighbors[i] < 1 || neighbors[i] > height * width) {
                neighbors[i] = 0;
            }
        }
        return neighbors;
    }

    public int getNeighborNum(int centerCell, int neighborCell) {
        // NOTE: Neighbor numbering starts in upper left as 0 and goes in clockwise direction.
        if (neighborCell < centerCell - 1) {
            return neighborCell - (centerCell - width - 1);
        } else if (neighborCell > centerCell + 1) {
            return 4 + (centerCell + width + 1) - neighborCell;
        } else if (neighborCell == centerCell - 1) {
            return 7;
        } else if (neighborCell == centerCell + 1) {
            return 3;
        }
        return -1;
    }

    // Get array of all source and sink node cell locations
    public int[] getSourceSinkCells() {
        if (sourceSinkCellLocations == null) {
            sourceSinkCellLocations = new int[sources.length + sinks.length];
            for (int i = 0; i < sources.length; i++) {
                sourceSinkCellLocations[i] = sources[i].getCellNum();
            }
            for (int i = 0; i < sinks.length; i++) {
                sourceSinkCellLocations[sources.length + i] = sinks[i].getCellNum();
            }
        }
        return sourceSinkCellLocations;
    }

    public double getModifiedEdgeCost(int cell1, int cell2) {
        if (cell1 == cell2) {
            return 0;
        } else if (getNeighborNum(cell1, cell2) >= 0 && getNeighborNum(cell1, cell2) < 8) {
            return modifiedAdjacencyCosts[cell1][getNeighborNum(cell1, cell2)];
        }
        return Double.MAX_VALUE;
    }

    public void updateModifiedEdgeCost(int cell1, int cell2, double edgeCostModification) {
        if (cell1 == cell2) {

        } else if (getNeighborNum(cell1, cell2) >= 0 && getNeighborNum(cell1, cell2) < 8) {
            modifiedAdjacencyCosts[cell1][getNeighborNum(cell1, cell2)] = edgeCostModification * adjacencyCosts[cell1][getNeighborNum(cell1, cell2)];
        }
    }

    public int sourceNum(int vertex) {
        for (int i = 0; i < sources.length; i++) {
            if (vertex == sources[i].getCellNum()) {
                return i;
            }
        }
        return -1;
    }

    public int sinkNum(int vertex) {
        for (int i = 0; i < sinks.length; i++) {
            if (vertex == sinks[i].getCellNum()) {
                return i;
            }
        }
        return -1;
    }

    public HashMap<Integer, HashSet<Integer>> getGraphNeighbors() {
        HashMap<Integer, HashSet<Integer>> graphNeighbors = new HashMap<>();
        for (Edge e : graphEdgeCosts.keySet()) {
            int v1 = e.v1;
            int v2 = e.v2;
            if (!graphNeighbors.containsKey(v1)) {
                graphNeighbors.put(v1, new HashSet<>());
            }
            if (!graphNeighbors.containsKey(v2)) {
                graphNeighbors.put(v2, new HashSet<>());
            }
            graphNeighbors.get(v1).add(v2);
            graphNeighbors.get(v2).add(v1);
        }
        return graphNeighbors;
    }

    // Get cell location of neighbor neighborNum of centerCell.
    public int getNeighbor(int centerCell, int neighborNum) {
        // NOTE: Neighbor numbering starts in upper left as 0 and goes in clockwise direction.
        if (neighborNum == 0 && centerCell > width && centerCell % width != 1) {
            return centerCell - width - 1;
        }
        if (neighborNum == 1 && centerCell > width) {
            return centerCell - width;
        }
        if (neighborNum == 2 && centerCell > width && centerCell % width != 0) {
            return centerCell - width + 1;
        }
        if (neighborNum == 3 && centerCell % width != 0) {
            return centerCell + 1;
        }
        if (neighborNum == 4 && centerCell % width != 0 && centerCell / width + 1 < height) {
            return centerCell + width + 1;
        }
        if (neighborNum == 5 && centerCell / width + 1 < height) {
            return centerCell + width;
        }
        if (neighborNum == 6 && centerCell / width + 1 < height && centerCell % width != 1) {
            return centerCell + width - 1;
        }
        if (neighborNum == 7 && centerCell % width != 1) {
            return centerCell - 1;
        }
        return -1;
    }

    // Data element get methods
    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Source[] getSources() {
        return sources;
    }

    public Sink[] getSinks() {
        return sinks;
    }

    public LinearComponent[] getLinearComponents() {
        return linearComponents;
    }

    public int[][] getShortestPaths() {
        return shortestPaths;
    }

    public double[] getShortestPathCosts() {
        return shortestPathCosts;
    }

    public String getCostSurfacePath() {
        File temp = new File(basePath + "/" + dataset + "/BaseData/CostSurface/cost.bmp");
        return basePath + "/" + dataset + "/BaseData/CostSurface/cost.bmp";
    }

    public double[][] getAdjacencyCosts() {
        return adjacencyCosts;
    }

    public double[][] getModifiedAdjacencyCosts() {
        return modifiedAdjacencyCosts;
    }

    // Data element set methods
    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setLowerLeftX(double lowerLeftX) {
        this.lowerLeftX = lowerLeftX;
    }

    public void setLowerLeftY(double lowerLeftY) {
        this.lowerLeftY = lowerLeftY;
    }

    public void setCellSize(double cellSize) {
        this.cellSize = cellSize;
    }

    public void setAdjacencyCosts(double[][] adjacencyCosts) {
        this.adjacencyCosts = adjacencyCosts;

        modifiedAdjacencyCosts = new double[adjacencyCosts.length][];
        for (int i = 0; i < adjacencyCosts.length; i++) {
            double[] temp = adjacencyCosts[i];
            int tempLength = temp.length;
            modifiedAdjacencyCosts[i] = new double[tempLength];
            System.arraycopy(temp, 0, modifiedAdjacencyCosts[i], 0, tempLength);
        }
    }

    public void setRightOfWayCosts(double[][] rightOfWayCosts) {
        this.rightOfWayCosts = rightOfWayCosts;
    }

    public void setConstructionCosts(double[][] constructionCosts) {
        this.constructionCosts = constructionCosts;
    }

    public void setSources(Source[] sources) {
        this.sources = sources;
    }

    public void setSinks(Sink[] sinks) {
        this.sinks = sinks;
    }

    public void setLinearComponents(LinearComponent[] linearComponents) {
        this.linearComponents = linearComponents;
    }

    public void setShortestPaths(int[][] shortestPaths) {
        this.shortestPaths = shortestPaths;
    }

    public void setShortestPathCosts(double[] shortestPathCosts) {
        this.shortestPathCosts = shortestPathCosts;
    }

    public void setGraphVertices(int[] vertices) {
        graphVertices = vertices;
    }

    public void setGraphEdgeCosts(HashMap<Edge, Double> edgeCosts) {
        graphEdgeCosts = edgeCosts;
    }

    public void setGraphEdgeRoutes(HashMap<Edge, int[]> edgeRoutes) {
        graphEdgeRoutes = edgeRoutes;
    }

    public void setGraphSourceSinkPaths(HashMap<Edge, ArrayList<Edge>> routes) {
        sourceSinkRoutes = routes;
    }

    public void setDelaunayPairs(HashSet<Edge> pairs) {
        delaunayPairs = pairs;
    }

    public void setSolver(Solver s) {
        solver = s;

        // Load data from files.
        DataInOut.loadData(basePath, dataset, scenario, this);
    }
}
