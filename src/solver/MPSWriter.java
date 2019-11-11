package solver;

import dataStore.DataStorer;
import dataStore.Edge;
import dataStore.LinearComponent;
import dataStore.Sink;
import dataStore.Source;
import dataStore.UnidirEdge;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author yaw
 */
public class MPSWriter {

    public static void writeCapPriceMPS(String fileName, DataStorer data, double crf, double numYears, double capacityTarget, String basePath, String dataset, String scenario, int modelVersion) {
        //model version: 1 - cap, 2 - price, 3 - time.

        // Collect data
        Source[] sources = data.getSources();
        Sink[] sinks = data.getSinks();
        LinearComponent[] linearComponents = data.getLinearComponents();
        int[] graphVertices = data.getGraphVertices();
        HashMap<Integer, HashSet<Integer>> neighbors = data.getGraphNeighbors();

        HashMap<Edge, Double> edgeConstructionCosts = data.getGraphEdgeConstructionCosts();
        HashMap<Edge, Double> edgeRightOfWayCosts = data.getGraphEdgeRightOfWayCosts();
        HashMap<Source, Integer> sourceCellToIndex = new HashMap<>();
        HashMap<Integer, Source> sourceIndexToCell = new HashMap<>();
        HashMap<Sink, Integer> sinkCellToIndex = new HashMap<>();
        HashMap<Integer, Sink> sinkIndexToCell = new HashMap<>();
        HashMap<Integer, Integer> vertexCellToIndex = new HashMap<>();
        HashMap<Integer, Integer> vertexIndexToCell = new HashMap<>();
        HashMap<UnidirEdge, Integer> edgeToIndex = new HashMap<>();
        HashMap<Integer, UnidirEdge> edgeIndexToEdge = new HashMap<>();
        HashSet<Integer> sourceCells = new HashSet<>();
        HashSet<Integer> sinkCells = new HashSet<>();

        HashMap<String, HashSet<ConstraintTerm>> intVariableToConstraints = new HashMap<>();
        HashMap<String, HashSet<ConstraintTerm>> contVariableToConstraints = new HashMap<>();
        HashMap<String, String> constraintToSign = new HashMap<>();
        HashMap<String, Double> constraintRHS = new HashMap<>();
        HashMap<String, VariableBound> variableBounds = new HashMap<>();

        // Set pipe capacity factor if right of way costs are provided.
        double pipeUtilization = 1.0;
        if (linearComponents[0].getRowSlope() != 0) {
            pipeUtilization = .93;
        }

        // Initialize cell/index maps
        for (int i = 0; i < sources.length; i++) {
            sourceCellToIndex.put(sources[i], i);
            sourceIndexToCell.put(i, sources[i]);
            sourceCells.add(sources[i].getCellNum());
        }
        for (int i = 0; i < sinks.length; i++) {
            sinkCellToIndex.put(sinks[i], i);
            sinkIndexToCell.put(i, sinks[i]);
            sinkCells.add(sinks[i].getCellNum());
        }
        for (int i = 0; i < graphVertices.length; i++) {
            vertexCellToIndex.put(graphVertices[i], i);
            vertexIndexToCell.put(i, graphVertices[i]);
        }
        int index = 0;
        for (Edge e : edgeConstructionCosts.keySet()) {
            UnidirEdge e1 = new UnidirEdge(e.v1, e.v2);
            edgeToIndex.put(e1, index);
            edgeIndexToEdge.put(index, e1);
            index++;

            UnidirEdge e2 = new UnidirEdge(e.v2, e.v1);
            edgeToIndex.put(e2, index);
            edgeIndexToEdge.put(index, e2);
            index++;
        }

        // Build model
        // Make variables
        // Source openings
        String[] s = new String[sources.length];
        for (int i = 0; i < sources.length; i++) {
            s[i] = "s[" + i + "]";
            variableBounds.put(s[i], new VariableBound("UP", 1));
        }

        // Capture amounts
        String[] a = new String[sources.length];
        for (int i = 0; i < sources.length; i++) {
            a[i] = "a[" + i + "]";
        }

        // Reservoir openings
        String[] r = new String[sinks.length];
        for (int i = 0; i < sinks.length; i++) {
            r[i] = "r[" + i + "]";
            variableBounds.put(r[i], new VariableBound("UP", 1));
        }

        // Injection amounts
        String[] b = new String[sinks.length];
        for (int i = 0; i < sinks.length; i++) {
            b[i] = "b[" + i + "]";
        }

        // Well openings
        String[] w = new String[sinks.length];
        for (int i = 0; i < sinks.length; i++) {
            w[i] = "w[" + i + "]";
            variableBounds.put(w[i], new VariableBound("LI", 0));
        }

        // Pipeline between i and j with trend c
        String[][] y = new String[edgeToIndex.size()][linearComponents.length];
        for (int e = 0; e < edgeToIndex.size(); e++) {
            for (int c = 0; c < linearComponents.length; c++) {
                y[e][c] = "y[" + e + "][" + c + "]";
                variableBounds.put(y[e][c], new VariableBound("UP", 1));
            }
        }

        // Pipeline capcaity
        String[][] p = new String[edgeToIndex.size()][linearComponents.length];
        for (int e = 0; e < edgeToIndex.size(); e++) {
            for (int c = 0; c < linearComponents.length; c++) {
                p[e][c] = "p[" + e + "][" + c + "]";
            }
            //}
        }

        // Make constraints
        // Pipeline capacity constraints
        int constraintCounter = 1;
        for (int e = 0; e < edgeToIndex.size(); e++) {
            for (int c = 0; c < linearComponents.length; c++) {
                String constraint = "A" + constraintCounter++;
                if (!contVariableToConstraints.containsKey(p[e][c])) {
                    contVariableToConstraints.put(p[e][c], new HashSet<ConstraintTerm>());
                }
                contVariableToConstraints.get(p[e][c]).add(new ConstraintTerm(constraint, 1));

                if (!intVariableToConstraints.containsKey(y[e][c])) {
                    intVariableToConstraints.put(y[e][c], new HashSet<ConstraintTerm>());
                }
                intVariableToConstraints.get(y[e][c]).add(new ConstraintTerm(constraint, -linearComponents[c].getMaxCapacity()));

                constraintToSign.put(constraint, "L");
                constraintRHS.put(constraint, 0.0);

                constraint = "A" + constraintCounter++;
                contVariableToConstraints.get(p[e][c]).add(new ConstraintTerm(constraint, 1));
                constraintToSign.put(constraint, "G");
            }
        }

        // No pipeline loops
        constraintCounter = 1;
        for (int e = 0; e < edgeToIndex.size(); e++) {
            //for (int src : graphVertices) {
            //for (int dest : neighbors.get(src)) {
            String constraint = "B" + constraintCounter++;
            for (int c = 0; c < linearComponents.length; c++) {
                if (!intVariableToConstraints.containsKey(y[e][c])) {
                    intVariableToConstraints.put(y[e][c], new HashSet<ConstraintTerm>());
                }
                intVariableToConstraints.get(y[e][c]).add(new ConstraintTerm(constraint, 1));

            }
            constraintToSign.put(constraint, "L");
            constraintRHS.put(constraint, 1.0);
            //}
        }

        // Conservation of flow
        constraintCounter = 1;
        for (int src : graphVertices) {
            String constraint = "C" + constraintCounter++;
            for (int dest : neighbors.get(src)) {
                UnidirEdge edge = new UnidirEdge(src, dest);
                for (int c = 0; c < linearComponents.length; c++) {
                    if (!contVariableToConstraints.containsKey(p[edgeToIndex.get(edge)][c])) {
                        contVariableToConstraints.put(p[edgeToIndex.get(edge)][c], new HashSet<ConstraintTerm>());
                    }
                    contVariableToConstraints.get(p[edgeToIndex.get(edge)][c]).add(new ConstraintTerm(constraint, 1));
                }
            }

            for (int dest : neighbors.get(src)) {
                UnidirEdge edge = new UnidirEdge(dest, src);
                for (int c = 0; c < linearComponents.length; c++) {
                    if (!contVariableToConstraints.containsKey(p[edgeToIndex.get(edge)][c])) {
                        contVariableToConstraints.put(p[edgeToIndex.get(edge)][c], new HashSet<ConstraintTerm>());
                    }
                    contVariableToConstraints.get(p[edgeToIndex.get(edge)][c]).add(new ConstraintTerm(constraint, -1));
                }
            }

            // Set right hand side
            if (sourceCells.contains(src)) {
                for (Source source : sources) {
                    if (source.getCellNum() == src) {
                        if (!contVariableToConstraints.containsKey(a[sourceCellToIndex.get(source)])) {
                            contVariableToConstraints.put(a[sourceCellToIndex.get(source)], new HashSet<ConstraintTerm>());
                        }
                        contVariableToConstraints.get(a[sourceCellToIndex.get(source)]).add(new ConstraintTerm(constraint, -1));
                    }
                }
            }
            if (sinkCells.contains(src)) {
                for (Sink sink : sinks) {
                    if (sink.getCellNum() == src) {
                        if (!contVariableToConstraints.containsKey(b[sinkCellToIndex.get(sink)])) {
                            contVariableToConstraints.put(b[sinkCellToIndex.get(sink)], new HashSet<ConstraintTerm>());
                        }
                        contVariableToConstraints.get(b[sinkCellToIndex.get(sink)]).add(new ConstraintTerm(constraint, 1));
                    }
                }
            }
            
            constraintToSign.put(constraint, "E");
        }

        // Capture capped by max production
        constraintCounter = 1;
        for (Source src : sources) {
            String constraint = "D" + constraintCounter++;

            if (!intVariableToConstraints.containsKey(s[sourceCellToIndex.get(src)])) {
                intVariableToConstraints.put(s[sourceCellToIndex.get(src)], new HashSet<ConstraintTerm>());
            }
            intVariableToConstraints.get(s[sourceCellToIndex.get(src)]).add(new ConstraintTerm(constraint, src.getProductionRate()));

            if (!contVariableToConstraints.containsKey(a[sourceCellToIndex.get(src)])) {
                contVariableToConstraints.put(a[sourceCellToIndex.get(src)], new HashSet<ConstraintTerm>());
            }
            contVariableToConstraints.get(a[sourceCellToIndex.get(src)]).add(new ConstraintTerm(constraint, -1));
            constraintToSign.put(constraint, "G");
            //constraintRHS.put(constraint, 0.0);
        }

        // Well injection capped by max injectivity
        constraintCounter = 1;
        for (Sink snk : sinks) {
            String constraint = "E" + constraintCounter++;

            if (!intVariableToConstraints.containsKey(w[sinkCellToIndex.get(snk)])) {
                intVariableToConstraints.put(w[sinkCellToIndex.get(snk)], new HashSet<ConstraintTerm>());
            }
            intVariableToConstraints.get(w[sinkCellToIndex.get(snk)]).add(new ConstraintTerm(constraint, snk.getWellCapacity()));

            if (!contVariableToConstraints.containsKey(b[sinkCellToIndex.get(snk)])) {
                contVariableToConstraints.put(b[sinkCellToIndex.get(snk)], new HashSet<ConstraintTerm>());
            }
            contVariableToConstraints.get(b[sinkCellToIndex.get(snk)]).add(new ConstraintTerm(constraint, -1));
            constraintToSign.put(constraint, "G");
            //constraintRHS.put(constraint, 0.0);
        }

        // Storage capped by max capacity
        constraintCounter = 1;
        for (Sink snk : sinks) {
            String constraint = "F" + constraintCounter++;
            if (!intVariableToConstraints.containsKey(r[sinkCellToIndex.get(snk)])) {
                intVariableToConstraints.put(r[sinkCellToIndex.get(snk)], new HashSet<ConstraintTerm>());
            }
            intVariableToConstraints.get(r[sinkCellToIndex.get(snk)]).add(new ConstraintTerm(constraint, snk.getCapacity() / numYears));

            if (!contVariableToConstraints.containsKey(b[sinkCellToIndex.get(snk)])) {
                contVariableToConstraints.put(b[sinkCellToIndex.get(snk)], new HashSet<ConstraintTerm>());
            }
            contVariableToConstraints.get(b[sinkCellToIndex.get(snk)]).add(new ConstraintTerm(constraint, -1));
            constraintToSign.put(constraint, "G");
            //constraintRHS.put(constraint, 0.0);
        }

        String constraint;

        // Set amount of CO2 to capture
        if (modelVersion == 1) {
            constraintCounter = 1;
            constraint = "G" + constraintCounter++;
            for (Source src : sources) {
                if (!contVariableToConstraints.containsKey(a[sourceCellToIndex.get(src)])) {
                    contVariableToConstraints.put(a[sourceCellToIndex.get(src)], new HashSet<ConstraintTerm>());
                }
                contVariableToConstraints.get(a[sourceCellToIndex.get(src)]).add(new ConstraintTerm(constraint, 1));
            }
            constraintToSign.put(constraint, "G");
            constraintRHS.put(constraint, capacityTarget);
        }

        // Hardcode constants.
        contVariableToConstraints.put("captureTarget", new HashSet<ConstraintTerm>());
        contVariableToConstraints.get("captureTarget").add(new ConstraintTerm("H1", 1));
        constraintToSign.put("H1", "E");
        constraintRHS.put("H1", capacityTarget);
        contVariableToConstraints.put("crf", new HashSet<ConstraintTerm>());
        contVariableToConstraints.get("crf").add(new ConstraintTerm("H2", 1));
        constraintToSign.put("H2", "E");
        constraintRHS.put("H2", crf);
        contVariableToConstraints.put("projectLength", new HashSet<ConstraintTerm>());
        contVariableToConstraints.get("projectLength").add(new ConstraintTerm("H3", 1));
        constraintToSign.put("H3", "E");
        constraintRHS.put("H3", numYears);
        contVariableToConstraints.put("modelVersion", new HashSet<ConstraintTerm>());
        contVariableToConstraints.get("modelVersion").add(new ConstraintTerm("H4", 1));
        constraintToSign.put("H4", "E");
        constraintRHS.put("H4", (double) modelVersion);

        // Make objective
        constraint = "OBJ";
        for (Source src : sources) {
            if (!intVariableToConstraints.containsKey(s[sourceCellToIndex.get(src)])) {
                intVariableToConstraints.put(s[sourceCellToIndex.get(src)], new HashSet<ConstraintTerm>());
            }
            intVariableToConstraints.get(s[sourceCellToIndex.get(src)]).add(new ConstraintTerm(constraint, src.getOpeningCost(crf)));

            if (!contVariableToConstraints.containsKey(a[sourceCellToIndex.get(src)])) {
                contVariableToConstraints.put(a[sourceCellToIndex.get(src)], new HashSet<ConstraintTerm>());
            }
            contVariableToConstraints.get(a[sourceCellToIndex.get(src)]).add(new ConstraintTerm(constraint, src.getCaptureCost()));
        }

        for (int e = 0; e < edgeToIndex.size(); e++) {
            for (int c = 0; c < linearComponents.length; c++) {
                UnidirEdge unidirEdge = edgeIndexToEdge.get(e);
                Edge bidirEdge = new Edge(unidirEdge.v1, unidirEdge.v2);

                if (!intVariableToConstraints.containsKey(y[e][c])) {
                    intVariableToConstraints.put(y[e][c], new HashSet<ConstraintTerm>());
                }
                double coefficient = (linearComponents[c].getConIntercept() * edgeConstructionCosts.get(bidirEdge) + linearComponents[c].getRowIntercept() * edgeRightOfWayCosts.get(bidirEdge)) * crf;
                intVariableToConstraints.get(y[e][c]).add(new ConstraintTerm(constraint, coefficient));

                if (!contVariableToConstraints.containsKey(p[e][c])) {
                    contVariableToConstraints.put(p[e][c], new HashSet<ConstraintTerm>());
                }
                coefficient = (linearComponents[c].getConSlope() * edgeConstructionCosts.get(bidirEdge) + linearComponents[c].getRowSlope() * edgeRightOfWayCosts.get(bidirEdge)) * crf / pipeUtilization;
                contVariableToConstraints.get(p[e][c]).add(new ConstraintTerm(constraint, coefficient));
            }
        }

        for (Sink snk : sinks) {
            if (!intVariableToConstraints.containsKey(r[sinkCellToIndex.get(snk)])) {
                intVariableToConstraints.put(r[sinkCellToIndex.get(snk)], new HashSet<ConstraintTerm>());
            }
            intVariableToConstraints.get(r[sinkCellToIndex.get(snk)]).add(new ConstraintTerm(constraint, snk.getOpeningCost(crf)));

            if (!intVariableToConstraints.containsKey(w[sinkCellToIndex.get(snk)])) {
                intVariableToConstraints.put(w[sinkCellToIndex.get(snk)], new HashSet<ConstraintTerm>());
            }
            intVariableToConstraints.get(w[sinkCellToIndex.get(snk)]).add(new ConstraintTerm(constraint, snk.getWellOpeningCost(crf)));

            if (!contVariableToConstraints.containsKey(b[sinkCellToIndex.get(snk)])) {
                contVariableToConstraints.put(b[sinkCellToIndex.get(snk)], new HashSet<ConstraintTerm>());
            }
            contVariableToConstraints.get(b[sinkCellToIndex.get(snk)]).add(new ConstraintTerm(constraint, snk.getInjectionCost()));
        }

        constraintToSign.put(constraint, "N");

        makeFile(fileName, basePath, dataset, scenario, intVariableToConstraints, contVariableToConstraints, constraintToSign, constraintRHS, variableBounds);
    }

    public static void writeSimpleTimeMPS(String fileName, DataStorer data, double crf, String basePath, String dataset, String scenario) {
        // Collect data
        Source[] sources = data.getSources();
        Sink[] sinks = data.getSinks();
        LinearComponent[] linearComponents = data.getLinearComponents();
        int[] graphVertices = data.getGraphVertices();
        HashMap<Integer, HashSet<Integer>> neighbors = data.getGraphNeighbors();
        double[][] timeConfiguration = data.getTimeConfiguration();

        HashMap<Edge, Double> edgeConstructionCosts = data.getGraphEdgeConstructionCosts();
        HashMap<Edge, Double> edgeRightOfWayCosts = data.getGraphEdgeRightOfWayCosts();
        HashMap<Source, Integer> sourceCellToIndex = new HashMap<>();
        HashMap<Integer, Source> sourceIndexToCell = new HashMap<>();
        HashMap<Sink, Integer> sinkCellToIndex = new HashMap<>();
        HashMap<Integer, Sink> sinkIndexToCell = new HashMap<>();
        HashMap<Integer, Integer> vertexCellToIndex = new HashMap<>();
        HashMap<Integer, Integer> vertexIndexToCell = new HashMap<>();
        HashMap<UnidirEdge, Integer> edgeToIndex = new HashMap<>();
        HashMap<Integer, UnidirEdge> edgeIndexToEdge = new HashMap<>();
        HashSet<Integer> sourceCells = new HashSet<>();
        HashSet<Integer> sinkCells = new HashSet<>();

        HashMap<String, HashSet<ConstraintTerm>> intVariableToConstraints = new HashMap<>();
        HashMap<String, HashSet<ConstraintTerm>> contVariableToConstraints = new HashMap<>();
        HashMap<String, String> constraintToSign = new HashMap<>();
        HashMap<String, Double> constraintRHS = new HashMap<>();
        HashMap<String, VariableBound> variableBounds = new HashMap<>();

        // Set pipe capacity factor if right of way costs are provided.
        double pipeUtilization = 1.0;
        if (linearComponents[0].getRowSlope() != 0) {
            pipeUtilization = .93;
        }

        // Initialize cell/index maps
        for (int i = 0; i < sources.length; i++) {
            sourceCellToIndex.put(sources[i], i);
            sourceIndexToCell.put(i, sources[i]);
            sourceCells.add(sources[i].getCellNum());
        }
        for (int i = 0; i < sinks.length; i++) {
            sinkCellToIndex.put(sinks[i], i);
            sinkIndexToCell.put(i, sinks[i]);
            sinkCells.add(sinks[i].getCellNum());
        }
        for (int i = 0; i < graphVertices.length; i++) {
            vertexCellToIndex.put(graphVertices[i], i);
            vertexIndexToCell.put(i, graphVertices[i]);
        }
        int index = 0;
        for (Edge e : edgeConstructionCosts.keySet()) {
            UnidirEdge e1 = new UnidirEdge(e.v1, e.v2);
            edgeToIndex.put(e1, index);
            edgeIndexToEdge.put(index, e1);
            index++;

            UnidirEdge e2 = new UnidirEdge(e.v2, e.v1);
            edgeToIndex.put(e2, index);
            edgeIndexToEdge.put(index, e2);
            index++;
        }

        // Build model
        // Make variables
        // Capture amounts
        String[][] a = new String[sources.length][timeConfiguration.length];
        for (int i = 0; i < sources.length; i++) {
            for (int t = 0; t < timeConfiguration.length; t++) {
                a[i][t] = "a[" + i + "][" + t + "]";
            }
        }

        // Injection amounts
        String[][] b = new String[sinks.length][timeConfiguration.length];
        for (int i = 0; i < sinks.length; i++) {
            for (int t = 0; t < timeConfiguration.length; t++) {
                b[i][t] = "b[" + i + "][" + t + "]";
            }
        }

        // Pipeline between i and j with trend c
        String[][][] y = new String[edgeToIndex.size()][linearComponents.length][timeConfiguration.length];
        for (int e = 0; e < edgeToIndex.size(); e++) {
            for (int c = 0; c < linearComponents.length; c++) {
                for (int t = 0; t < timeConfiguration.length; t++) {
                    y[e][c][t] = "y[" + e + "][" + c + "][" + t + "]";
                    variableBounds.put(y[e][c][t], new VariableBound("UP", 1));
                }
            }
        }

        // Purchased pipeline capacity
        String[][][] p = new String[edgeToIndex.size()][linearComponents.length][timeConfiguration.length];
        for (int e = 0; e < edgeToIndex.size(); e++) {
            for (int c = 0; c < linearComponents.length; c++) {
                for (int t = 0; t < timeConfiguration.length; t++) {
                    p[e][c][t] = "p[" + e + "][" + c + "][" + t + "]";
                }
            }
        }

        // Pipeline hosting amount
        String[][][] x = new String[edgeToIndex.size()][linearComponents.length][timeConfiguration.length];
        for (int e = 0; e < edgeToIndex.size(); e++) {
            for (int c = 0; c < linearComponents.length; c++) {
                for (int t = 0; t < timeConfiguration.length; t++) {
                    x[e][c][t] = "x[" + e + "][" + c + "][" + t + "]";
                }
            }
        }

        // Make constraints
        // Pipeline capacity constraints
        int constraintCounter = 1;
        for (int e = 0; e < edgeToIndex.size(); e++) {
            for (int c = 0; c < linearComponents.length; c++) {
                for (int t = 0; t < timeConfiguration.length; t++) {
                    String constraint = "A" + constraintCounter++;
                    if (!contVariableToConstraints.containsKey(p[e][c][t])) {
                        contVariableToConstraints.put(p[e][c][t], new HashSet<ConstraintTerm>());
                    }
                    contVariableToConstraints.get(p[e][c][t]).add(new ConstraintTerm(constraint, 1));

                    if (!intVariableToConstraints.containsKey(y[e][c][t])) {
                        intVariableToConstraints.put(y[e][c][t], new HashSet<ConstraintTerm>());
                    }
                    intVariableToConstraints.get(y[e][c][t]).add(new ConstraintTerm(constraint, -linearComponents[c].getMaxCapacity()));

                    constraintToSign.put(constraint, "L");
                    constraintRHS.put(constraint, 0.0);

                    constraint = "A" + constraintCounter++;
                    contVariableToConstraints.get(p[e][c][t]).add(new ConstraintTerm(constraint, 1));
                    constraintToSign.put(constraint, "G");
                }
            }
        }

        // Ensure new and existing pipeline have enough capacity
        constraintCounter = 1;
        for (int e = 0; e < edgeToIndex.size(); e++) {
            for (int c = 0; c < linearComponents.length; c++) {
                for (int t = 0; t < timeConfiguration.length; t++) {
                    String constraint = "B" + constraintCounter++;
                    if (!contVariableToConstraints.containsKey(x[e][c][t])) {
                        contVariableToConstraints.put(x[e][c][t], new HashSet<ConstraintTerm>());
                    }
                    contVariableToConstraints.get(x[e][c][t]).add(new ConstraintTerm(constraint, 1));

                    for (int tau = 0; tau <= t; tau++) {
                        if (!contVariableToConstraints.containsKey(p[e][c][tau])) {
                            contVariableToConstraints.put(p[e][c][tau], new HashSet<ConstraintTerm>());
                        }
                        contVariableToConstraints.get(p[e][c][tau]).add(new ConstraintTerm(constraint, -1));
                    }

                    constraintToSign.put(constraint, "L");
                    constraintRHS.put(constraint, 0.0);
                }
            }
        }

        // Conservation of flow
        constraintCounter = 1;
        for (int src : graphVertices) {
            for (int t = 0; t < timeConfiguration.length; t++) {
                String constraint = "C" + constraintCounter++;
                for (int dest : neighbors.get(src)) {
                    UnidirEdge edge = new UnidirEdge(src, dest);
                    for (int c = 0; c < linearComponents.length; c++) {
                        if (!contVariableToConstraints.containsKey(x[edgeToIndex.get(edge)][c][t])) {
                            contVariableToConstraints.put(x[edgeToIndex.get(edge)][c][t], new HashSet<ConstraintTerm>());
                        }
                        contVariableToConstraints.get(x[edgeToIndex.get(edge)][c][t]).add(new ConstraintTerm(constraint, 1));
                    }
                }

                for (int dest : neighbors.get(src)) {
                    UnidirEdge edge = new UnidirEdge(dest, src);
                    for (int c = 0; c < linearComponents.length; c++) {
                        if (!contVariableToConstraints.containsKey(x[edgeToIndex.get(edge)][c][t])) {
                            contVariableToConstraints.put(x[edgeToIndex.get(edge)][c][t], new HashSet<ConstraintTerm>());
                        }
                        contVariableToConstraints.get(x[edgeToIndex.get(edge)][c][t]).add(new ConstraintTerm(constraint, -1));
                    }
                }

                // Set right hand side
                if (sourceCells.contains(src)) {
                    for (Source source : sources) {
                        if (source.getCellNum() == src) {
                            if (!contVariableToConstraints.containsKey(a[sourceCellToIndex.get(source)][t])) {
                                contVariableToConstraints.put(a[sourceCellToIndex.get(source)][t], new HashSet<ConstraintTerm>());
                            }
                            contVariableToConstraints.get(a[sourceCellToIndex.get(source)][t]).add(new ConstraintTerm(constraint, -1));
                        }
                    }
                }
                if (sinkCells.contains(src)) {
                    for (Sink sink : sinks) {
                        if (sink.getCellNum() == src) {
                            if (!contVariableToConstraints.containsKey(b[sinkCellToIndex.get(sink)][t])) {
                                contVariableToConstraints.put(b[sinkCellToIndex.get(sink)][t], new HashSet<ConstraintTerm>());
                            }
                            contVariableToConstraints.get(b[sinkCellToIndex.get(sink)][t]).add(new ConstraintTerm(constraint, 1));
                        }
                    }
                }
                
                constraintToSign.put(constraint, "E");
            }
        }

        // Capture capped by max production
        constraintCounter = 1;
        for (Source src : sources) {
            for (int t = 0; t < timeConfiguration.length; t++) {
                String constraint = "D" + constraintCounter++;

                if (!contVariableToConstraints.containsKey(a[sourceCellToIndex.get(src)][t])) {
                    contVariableToConstraints.put(a[sourceCellToIndex.get(src)][t], new HashSet<ConstraintTerm>());
                }
                contVariableToConstraints.get(a[sourceCellToIndex.get(src)][t]).add(new ConstraintTerm(constraint, 1));
                constraintToSign.put(constraint, "L");

                constraintRHS.put(constraint, src.getProductionRate(t));
            }
        }

        // Storage capped by max capacity
        constraintCounter = 1;
        if (sinks[0].getNumCapacities() == 1) {
            for (Sink snk : sinks) {
                String constraint = "E" + constraintCounter++;

                for (int t = 0; t < timeConfiguration.length; t++) {
                    if (!contVariableToConstraints.containsKey(b[sinkCellToIndex.get(snk)][t])) {
                        contVariableToConstraints.put(b[sinkCellToIndex.get(snk)][t], new HashSet<ConstraintTerm>());
                    }
                    contVariableToConstraints.get(b[sinkCellToIndex.get(snk)][t]).add(new ConstraintTerm(constraint, timeConfiguration[t][1]));
                }
                constraintToSign.put(constraint, "L");
                constraintRHS.put(constraint, snk.getCapacity());
            }
        } else {
            for (Sink snk : sinks) {
                for (int t = 0; t < timeConfiguration.length; t++) {
                    String constraint = "E" + constraintCounter++;
                    if (!contVariableToConstraints.containsKey(b[sinkCellToIndex.get(snk)][t])) {
                        contVariableToConstraints.put(b[sinkCellToIndex.get(snk)][t], new HashSet<ConstraintTerm>());
                    }
                    contVariableToConstraints.get(b[sinkCellToIndex.get(snk)][t]).add(new ConstraintTerm(constraint, timeConfiguration[t][1]));
                    constraintToSign.put(constraint, "L");
                    constraintRHS.put(constraint, snk.getCapacity(t));
                }
            }
        }

        // Set amount of CO2 to capture
        constraintCounter = 1;
        for (int t = 0; t < timeConfiguration.length; t++) {
            String constraint = "F" + constraintCounter++;

            for (Source src : sources) {
                if (!contVariableToConstraints.containsKey(a[sourceCellToIndex.get(src)][t])) {
                    contVariableToConstraints.put(a[sourceCellToIndex.get(src)][t], new HashSet<ConstraintTerm>());
                }
                contVariableToConstraints.get(a[sourceCellToIndex.get(src)][t]).add(new ConstraintTerm(constraint, 1));
            }
            constraintToSign.put(constraint, "G");
            constraintRHS.put(constraint, timeConfiguration[t][2]);
        }

        // Hardcode constants.
        int num = 0;
        contVariableToConstraints.put("crf", new HashSet<ConstraintTerm>());
        contVariableToConstraints.get("crf").add(new ConstraintTerm("H" + num, 1));
        constraintToSign.put("H" + num, "E");
        constraintRHS.put("H" + num, crf);
        num++;

        for (int t = 0; t < timeConfiguration.length; t++) {
            contVariableToConstraints.put("N" + t, new HashSet<ConstraintTerm>());
            contVariableToConstraints.get("N" + t).add(new ConstraintTerm("H" + num, 1));
            constraintToSign.put("H" + num, "E");
            constraintRHS.put("H" + num, timeConfiguration[t][1]);
            num++;
        }

        // Make objective
        String constraint = "OBJ";
        for (Source src : sources) {
            for (int t = 0; t < timeConfiguration.length; t++) {
                if (!contVariableToConstraints.containsKey(a[sourceCellToIndex.get(src)][t])) {
                    contVariableToConstraints.put(a[sourceCellToIndex.get(src)][t], new HashSet<ConstraintTerm>());
                }
                contVariableToConstraints.get(a[sourceCellToIndex.get(src)][t]).add(new ConstraintTerm(constraint, src.getCaptureCost() * timeConfiguration[t][1]));
            }
        }

        for (int e = 0; e < edgeToIndex.size(); e++) {
            for (int c = 0; c < linearComponents.length; c++) {
                for (int t = 0; t < timeConfiguration.length; t++) {
                    double remainingTime = 0;
                    for (int tau = t; tau < timeConfiguration.length; tau++) {
                        remainingTime += timeConfiguration[tau][1];
                    }

                    UnidirEdge unidirEdge = edgeIndexToEdge.get(e);
                    Edge bidirEdge = new Edge(unidirEdge.v1, unidirEdge.v2);

                    if (!intVariableToConstraints.containsKey(y[e][c][t])) {
                        intVariableToConstraints.put(y[e][c][t], new HashSet<ConstraintTerm>());
                    }
                    double coefficient = (linearComponents[c].getConIntercept() * edgeConstructionCosts.get(bidirEdge) + linearComponents[c].getRowIntercept() * edgeRightOfWayCosts.get(bidirEdge)) * crf;
                    intVariableToConstraints.get(y[e][c][t]).add(new ConstraintTerm(constraint, coefficient * remainingTime));

                    if (!contVariableToConstraints.containsKey(p[e][c][t])) {
                        contVariableToConstraints.put(p[e][c][t], new HashSet<ConstraintTerm>());
                    }
                    coefficient = (linearComponents[c].getConSlope() * edgeConstructionCosts.get(bidirEdge) + linearComponents[c].getRowSlope() * edgeRightOfWayCosts.get(bidirEdge)) * crf / pipeUtilization;
                    contVariableToConstraints.get(p[e][c][t]).add(new ConstraintTerm(constraint, coefficient * remainingTime));
                }
            }
        }

        for (Sink snk : sinks) {
            for (int t = 0; t < timeConfiguration.length; t++) {
                if (!contVariableToConstraints.containsKey(b[sinkCellToIndex.get(snk)][t])) {
                    contVariableToConstraints.put(b[sinkCellToIndex.get(snk)][t], new HashSet<ConstraintTerm>());
                }
                contVariableToConstraints.get(b[sinkCellToIndex.get(snk)][t]).add(new ConstraintTerm(constraint, snk.getInjectionCost() * timeConfiguration[t][1]));
            }
        }

        constraintToSign.put(constraint, "N");

        makeFile(fileName, basePath, dataset, scenario, intVariableToConstraints, contVariableToConstraints, constraintToSign, constraintRHS, variableBounds);
    }

    public static void writeFlowHeuristicMPS(String fileName, DataStorer data, double crf, double numYears, double capacityTarget, String basePath, String dataset, String scenario, int modelVersion) {
        //model version: 1 - cap, 2 - price, 3 - time.

        // Collect data
        Source[] sources = data.getSources();
        Sink[] sinks = data.getSinks();
        LinearComponent[] linearComponents = data.getLinearComponents();
        int[] graphVertices = data.getGraphVertices();
        HashMap<Integer, HashSet<Integer>> neighbors = data.getGraphNeighbors();

        HashMap<Edge, Double> edgeConstructionCosts = data.getGraphEdgeConstructionCosts();
        HashMap<Edge, Double> edgeRightOfWayCosts = data.getGraphEdgeRightOfWayCosts();
        HashMap<Source, Integer> sourceCellToIndex = new HashMap<>();
        HashMap<Integer, Source> sourceIndexToCell = new HashMap<>();
        HashMap<Sink, Integer> sinkCellToIndex = new HashMap<>();
        HashMap<Integer, Sink> sinkIndexToCell = new HashMap<>();
        HashMap<Integer, Integer> vertexCellToIndex = new HashMap<>();
        HashMap<Integer, Integer> vertexIndexToCell = new HashMap<>();
        HashMap<UnidirEdge, Integer> edgeToIndex = new HashMap<>();
        HashMap<Integer, UnidirEdge> edgeIndexToEdge = new HashMap<>();
        HashSet<Integer> sourceCells = new HashSet<>();
        HashSet<Integer> sinkCells = new HashSet<>();

        HashMap<String, HashSet<ConstraintTerm>> contVariableToConstraints = new HashMap<>();
        HashMap<String, String> constraintToSign = new HashMap<>();
        HashMap<String, Double> constraintRHS = new HashMap<>();
        HashMap<String, VariableBound> variableBounds = new HashMap<>();

        // Set pipe capacity factor if right of way costs are provided.
        double pipeUtilization = 1.0;
        if (linearComponents[0].getRowSlope() != 0) {
            pipeUtilization = .93;
        }

        // Initialize cell/index maps
        for (int i = 0; i < sources.length; i++) {
            sourceCellToIndex.put(sources[i], i);
            sourceIndexToCell.put(i, sources[i]);
            sourceCells.add(sources[i].getCellNum());
        }
        for (int i = 0; i < sinks.length; i++) {
            sinkCellToIndex.put(sinks[i], i);
            sinkIndexToCell.put(i, sinks[i]);
            sinkCells.add(sinks[i].getCellNum());
        }
        for (int i = 0; i < graphVertices.length; i++) {
            vertexCellToIndex.put(graphVertices[i], i);
            vertexIndexToCell.put(i, graphVertices[i]);
        }
        int index = 0;
        for (Edge e : edgeConstructionCosts.keySet()) {
            UnidirEdge e1 = new UnidirEdge(e.v1, e.v2);
            edgeToIndex.put(e1, index);
            edgeIndexToEdge.put(index, e1);
            index++;

            UnidirEdge e2 = new UnidirEdge(e.v2, e.v1);
            edgeToIndex.put(e2, index);
            edgeIndexToEdge.put(index, e2);
            index++;
        }

        // Build model
        // Make variables
        // Capture amounts
        String[] a = new String[sources.length];
        for (int i = 0; i < sources.length; i++) {
            a[i] = "a[" + i + "]";
        }

        // Injection amounts
        String[] b = new String[sinks.length];
        for (int i = 0; i < sinks.length; i++) {
            b[i] = "b[" + i + "]";
        }

        // Pipeline capcaity
        String[][] p = new String[edgeToIndex.size()][linearComponents.length];
        for (int e = 0; e < edgeToIndex.size(); e++) {
            for (int c = 0; c < linearComponents.length; c++) {
                p[e][c] = "p[" + e + "][" + c + "]";
            }
            //}
        }

        // Make constraints
        // Pipeline capacity constraints
        int constraintCounter = 1;
        for (int e = 0; e < edgeToIndex.size(); e++) {
            for (int c = 0; c < linearComponents.length; c++) {
                String constraint = "A" + constraintCounter++;
                if (!contVariableToConstraints.containsKey(p[e][c])) {
                    contVariableToConstraints.put(p[e][c], new HashSet<ConstraintTerm>());
                }
                contVariableToConstraints.get(p[e][c]).add(new ConstraintTerm(constraint, 1));

                constraintToSign.put(constraint, "L");
                constraintRHS.put(constraint, linearComponents[c].getMaxCapacity());

                constraint = "A" + constraintCounter++;
                contVariableToConstraints.get(p[e][c]).add(new ConstraintTerm(constraint, 1));
                constraintToSign.put(constraint, "G");
            }
        }

        // Conservation of flow
        constraintCounter = 1;
        for (int src : graphVertices) {
            String constraint = "B" + constraintCounter++;
            for (int dest : neighbors.get(src)) {
                UnidirEdge edge = new UnidirEdge(src, dest);
                for (int c = 0; c < linearComponents.length; c++) {
                    if (!contVariableToConstraints.containsKey(p[edgeToIndex.get(edge)][c])) {
                        contVariableToConstraints.put(p[edgeToIndex.get(edge)][c], new HashSet<ConstraintTerm>());
                    }
                    contVariableToConstraints.get(p[edgeToIndex.get(edge)][c]).add(new ConstraintTerm(constraint, 1));
                }
            }

            for (int dest : neighbors.get(src)) {
                UnidirEdge edge = new UnidirEdge(dest, src);
                for (int c = 0; c < linearComponents.length; c++) {
                    if (!contVariableToConstraints.containsKey(p[edgeToIndex.get(edge)][c])) {
                        contVariableToConstraints.put(p[edgeToIndex.get(edge)][c], new HashSet<ConstraintTerm>());
                    }
                    contVariableToConstraints.get(p[edgeToIndex.get(edge)][c]).add(new ConstraintTerm(constraint, -1));
                }
            }

            // Set right hand side
            if (sourceCells.contains(src)) {
                for (Source source : sources) {
                    if (source.getCellNum() == src) {
                        if (!contVariableToConstraints.containsKey(a[sourceCellToIndex.get(source)])) {
                            contVariableToConstraints.put(a[sourceCellToIndex.get(source)], new HashSet<ConstraintTerm>());
                        }
                        contVariableToConstraints.get(a[sourceCellToIndex.get(source)]).add(new ConstraintTerm(constraint, -1));
                    }
                }
            }
            if (sinkCells.contains(src)) {
                for (Sink sink : sinks) {
                    if (sink.getCellNum() == src) {
                        if (!contVariableToConstraints.containsKey(b[sinkCellToIndex.get(sink)])) {
                            contVariableToConstraints.put(b[sinkCellToIndex.get(sink)], new HashSet<ConstraintTerm>());
                        }
                        contVariableToConstraints.get(b[sinkCellToIndex.get(sink)]).add(new ConstraintTerm(constraint, 1));
                    }
                }

            }
            constraintToSign.put(constraint, "E");
        }

        // Capture capped by max production
        constraintCounter = 1;
        for (Source src : sources) {
            String constraint = "C" + constraintCounter++;

            if (!contVariableToConstraints.containsKey(a[sourceCellToIndex.get(src)])) {
                contVariableToConstraints.put(a[sourceCellToIndex.get(src)], new HashSet<ConstraintTerm>());
            }
            contVariableToConstraints.get(a[sourceCellToIndex.get(src)]).add(new ConstraintTerm(constraint, 1));
            constraintToSign.put(constraint, "L");
            constraintRHS.put(constraint, src.getProductionRate());
        }

        // Well injection capped by max injectivity
        constraintCounter = 1;
        for (Sink snk : sinks) {
            String constraint = "D" + constraintCounter++;

            if (!contVariableToConstraints.containsKey(b[sinkCellToIndex.get(snk)])) {
                contVariableToConstraints.put(b[sinkCellToIndex.get(snk)], new HashSet<ConstraintTerm>());
            }
            contVariableToConstraints.get(b[sinkCellToIndex.get(snk)]).add(new ConstraintTerm(constraint, 1));
            constraintToSign.put(constraint, "L");
            constraintRHS.put(constraint, snk.getCapacity() / numYears);
        }

        String constraint;

        // Set amount of CO2 to capture
        if (modelVersion == 1) {
            constraintCounter = 1;
            constraint = "E" + constraintCounter++;
            for (Source src : sources) {
                if (!contVariableToConstraints.containsKey(a[sourceCellToIndex.get(src)])) {
                    contVariableToConstraints.put(a[sourceCellToIndex.get(src)], new HashSet<ConstraintTerm>());
                }
                contVariableToConstraints.get(a[sourceCellToIndex.get(src)]).add(new ConstraintTerm(constraint, 1));
            }
            constraintToSign.put(constraint, "G");
            constraintRHS.put(constraint, capacityTarget);
        }

        // Hardcode constants.
        contVariableToConstraints.put("captureTarget", new HashSet<ConstraintTerm>());
        contVariableToConstraints.get("captureTarget").add(new ConstraintTerm("H1", 1));
        constraintToSign.put("H1", "E");
        constraintRHS.put("H1", capacityTarget);
        contVariableToConstraints.put("crf", new HashSet<ConstraintTerm>());
        contVariableToConstraints.get("crf").add(new ConstraintTerm("H2", 1));
        constraintToSign.put("H2", "E");
        constraintRHS.put("H2", crf);
        contVariableToConstraints.put("projectLength", new HashSet<ConstraintTerm>());
        contVariableToConstraints.get("projectLength").add(new ConstraintTerm("H3", 1));
        constraintToSign.put("H3", "E");
        constraintRHS.put("H3", numYears);
        contVariableToConstraints.put("modelVersion", new HashSet<ConstraintTerm>());
        contVariableToConstraints.get("modelVersion").add(new ConstraintTerm("H4", 1));
        constraintToSign.put("H4", "E");
        constraintRHS.put("H4", (double) modelVersion);

        // Make objective
        constraint = "OBJ";
        for (Source src : sources) {
            if (!contVariableToConstraints.containsKey(a[sourceCellToIndex.get(src)])) {
                contVariableToConstraints.put(a[sourceCellToIndex.get(src)], new HashSet<ConstraintTerm>());
            }
            contVariableToConstraints.get(a[sourceCellToIndex.get(src)]).add(new ConstraintTerm(constraint, src.getCaptureCost()));
        }

        for (int e = 0; e < edgeToIndex.size(); e++) {
            for (int c = 0; c < linearComponents.length; c++) {
                UnidirEdge unidirEdge = edgeIndexToEdge.get(e);
                Edge bidirEdge = new Edge(unidirEdge.v1, unidirEdge.v2);

                if (!contVariableToConstraints.containsKey(p[e][c])) {
                    contVariableToConstraints.put(p[e][c], new HashSet<ConstraintTerm>());
                }

                double fixedCost = (linearComponents[c].getConIntercept() * edgeConstructionCosts.get(bidirEdge) + linearComponents[c].getRowIntercept() * edgeRightOfWayCosts.get(bidirEdge)) * crf;
                double variableCost = (linearComponents[c].getConSlope() * edgeConstructionCosts.get(bidirEdge) + linearComponents[c].getRowSlope() * edgeRightOfWayCosts.get(bidirEdge)) * crf / pipeUtilization;
                //double coefficient = variableCost + (fixedCost / linearComponents[c].getMaxCapacity());
                double coefficient = variableCost + (fixedCost / 1);
                contVariableToConstraints.get(p[e][c]).add(new ConstraintTerm(constraint, coefficient));
            }
        }

        for (Sink snk : sinks) {
            if (!contVariableToConstraints.containsKey(b[sinkCellToIndex.get(snk)])) {
                contVariableToConstraints.put(b[sinkCellToIndex.get(snk)], new HashSet<ConstraintTerm>());
            }
            contVariableToConstraints.get(b[sinkCellToIndex.get(snk)]).add(new ConstraintTerm(constraint, snk.getInjectionCost()));
        }

        constraintToSign.put(constraint, "N");

        makeFile(fileName, basePath, dataset, scenario, new HashMap<String, HashSet<ConstraintTerm>>(), contVariableToConstraints, constraintToSign, constraintRHS, variableBounds);
    }

    private static void makeFile(String fileName, String basePath, String dataset, String scenario, HashMap<String, HashSet<ConstraintTerm>> intVariableToConstraints, HashMap<String, HashSet<ConstraintTerm>> contVariableToConstraints, HashMap<String, String> constraintToSign, HashMap<String, Double> constraintRHS, HashMap<String, VariableBound> variableBounds) {
        StringBuilder problemFormulation = new StringBuilder("NAME\tSimCCS\n");

        // Identify constraints.
        problemFormulation.append("ROWS\n");
        for (String constraint : constraintToSign.keySet()) {
            problemFormulation.append("\t" + constraintToSign.get(constraint) + "\t" + constraint + "\n");
        }

        // Identify columns.
        problemFormulation.append("COLUMNS\n");
        problemFormulation.append("\tMARK0000\t'MARKER'\t'INTORG'\n");
        for (String intVar : intVariableToConstraints.keySet()) {
            for (ConstraintTerm term : intVariableToConstraints.get(intVar)) {
                problemFormulation.append("\t" + intVar + "\t" + term.constraint + "\t" + term.coefficient + "\n");
            }
        }
        problemFormulation.append("\tMARK0001\t'MARKER'\t'INTEND'\n");
        for (String contVar : contVariableToConstraints.keySet()) {
            for (ConstraintTerm term : contVariableToConstraints.get(contVar)) {
                problemFormulation.append("\t" + contVar + "\t" + term.constraint + "\t" + term.coefficient + "\n");
            }
        }

        // Identify RHSs.
        problemFormulation.append("RHS\n");
        for (String constraint : constraintRHS.keySet()) {
            problemFormulation.append("\trhs\t" + constraint + "\t" + constraintRHS.get(constraint) + "\n");
        }

        // Identify bounds.
        problemFormulation.append("BOUNDS\n");
        for (String variable : variableBounds.keySet()) {
            problemFormulation.append("\t" + variableBounds.get(variable).type + " bnd\t" + variable + "\t" + variableBounds.get(variable).bound + "\n");
        }

        // End file.
        problemFormulation.append("ENDATA");

        // Clear mip directory.
        String mipDirectory = basePath + "/" + dataset + "/Scenarios/" + scenario + "/MIP/";
        File mipFolder = new File(mipDirectory);
        File[] mips = mipFolder.listFiles();
        if (mips != null) {
            for (File mip : mips) {
                mip.delete();
            }
        }

        // Save to file.
        String mipPath = mipDirectory + fileName;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(mipPath))) {
            bw.write(problemFormulation.toString());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static class ConstraintTerm {

        String constraint;
        double coefficient;

        public ConstraintTerm(String constraint, double coefficient) {
            this.constraint = constraint;
            this.coefficient = Math.round(coefficient * 100000.0) / 100000.0;
        }
    }

    private static class VariableBound {

        String type;
        double bound;

        public VariableBound(String type, double bound) {
            this.type = type;
            this.bound = bound;
        }
    }
}
