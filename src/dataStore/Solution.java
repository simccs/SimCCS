package dataStore;

import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author yaw
 */
public class Solution {
    // Opened sources.
    private HashMap<Source, Double> sourceCaptureAmounts;   // MTCO2/yr
    private HashMap<Source, Double> sourceCosts;
    
    // Opened sinks.
    private HashMap<Sink, Double> sinkStorageAmounts;
    private HashMap<Sink, Double> sinkCosts;
    
    // Opened edges.
    private HashMap<Edge, Double> edgeTransportAmounts;
    private HashMap<Edge, Double> edgeCosts;
    
    // Other.
    private double captureAmountPerYear;
    private int projectLength;
    private double crf;
    
    public Solution() {
        sourceCaptureAmounts = new HashMap<>();
        sourceCosts = new HashMap<>();
        sinkStorageAmounts = new HashMap<>();
        sinkCosts = new HashMap<>();
        edgeTransportAmounts = new HashMap<>();
        edgeCosts = new HashMap<>();
    }
    
    public void addSourceCaptureAmount(Source src, double captureAmount) {
        if (!sourceCaptureAmounts.containsKey(src)) {
            sourceCaptureAmounts.put(src, 0.0);
        }
        sourceCaptureAmounts.put(src, sourceCaptureAmounts.get(src) + captureAmount);
        captureAmountPerYear += captureAmount;
    }
    
    public void addSourceCostComponent(Source src, double cost) {
        if (!sourceCosts.containsKey(src)) {
            sourceCosts.put(src, 0.0);
        }
        sourceCosts.put(src, sourceCosts.get(src) + cost);
    }
    
    public void addSinkStorageAmount(Sink snk, double captureAmount) {
        if (!sinkStorageAmounts.containsKey(snk)) {
            sinkStorageAmounts.put(snk, 0.0);
        }
        sinkStorageAmounts.put(snk, sinkStorageAmounts.get(snk) + captureAmount);
    }
    
    public void addSinkCostComponent(Sink snk, double cost) {
        if (!sinkCosts.containsKey(snk)) {
            sinkCosts.put(snk, 0.0);
        }
        sinkCosts.put(snk, sinkCosts.get(snk) + cost);
    }
    
    public void addEdgeTransportAmount(Edge edg, double captureAmount) {
        if (!edgeTransportAmounts.containsKey(edg)) {
            edgeTransportAmounts.put(edg, 0.0);
        }
        edgeTransportAmounts.put(edg, edgeTransportAmounts.get(edg) + captureAmount);
    }
    
    public void addEdgeCostComponent(Edge edg, double cost) {
        if (!edgeCosts.containsKey(edg)) {
            edgeCosts.put(edg, 0.0);
        }
        edgeCosts.put(edg, edgeCosts.get(edg) + cost);
    }
    
    public void setSourceCaptureAmounts(HashMap<Source, Double> sourceCaptureAmounts) {
        this.sourceCaptureAmounts = sourceCaptureAmounts;
    }
    
    public void setSourceCosts(HashMap<Source, Double> sourceCosts) {
        this.sourceCosts = sourceCosts;
    }
    
    public void setSinkStorageAmounts (HashMap<Sink, Double> sinkStorageAmounts) {
        this.sinkStorageAmounts = sinkStorageAmounts;
    }
    
    public void setSinkCosts(HashMap<Sink, Double> sinkCosts) {
        this.sinkCosts = sinkCosts;
    }
    
    public void setEdgeTransportAmounts(HashMap<Edge, Double> edgeTransportAmounts) {
        this.edgeTransportAmounts = edgeTransportAmounts;
    }
    
    public void setEdgeCosts(HashMap<Edge, Double> edgeCosts) {
        this.edgeCosts = edgeCosts;
    }
    
    //public void setTargetCaptureAmountPerYear(double targetCaptureAmount) {
    //    this.targetCaptureAmountPerYear = targetCaptureAmount;
    //}
    
    public void setProjectLength(int projectLength) {
        this.projectLength = projectLength;
    }
    
    public void setCRF(double crf) {
        this.crf = crf;
    }
    
    public HashSet<Source> getOpenedSources() {
        return new HashSet<>(sourceCaptureAmounts.keySet());
    }
    
    public HashSet<Sink> getOpenedSinks() {
        return new HashSet<>(sinkStorageAmounts.keySet());
    }
    
    public HashSet<Edge> getOpenedEdges() {
        return new HashSet<>(edgeTransportAmounts.keySet());
    }
    
    public HashMap<Source, Double> getSourceCaptureAmounts() {
        return sourceCaptureAmounts;
    }
    
    public HashMap<Source, Double> getSourceCosts() {
        return sourceCosts;
    }
    
    public HashMap<Sink, Double> getSinkStorageAmounts () {
        return sinkStorageAmounts;
    }
    
    public HashMap<Sink, Double> getSinkCosts() {
        return sinkCosts;
    }
    
    public HashMap<Edge, Double> getEdgeTransportAmounts() {
        return edgeTransportAmounts;
    }
    
    public HashMap<Edge, Double> getEdgeCosts() {
        return edgeCosts;
    }
    
    public int getNumOpenedSources() {
        return sourceCaptureAmounts.keySet().size();
    }
    
    public int getNumOpenedSinks() {
        return sinkStorageAmounts.keySet().size();
    }
    
    public double getCaptureAmount() {
        return captureAmountPerYear * projectLength;
    }
    
    public double getAnnualCaptureAmount() {
        return captureAmountPerYear;
    }
    
    public int getNumEdgesOpened() {
        return edgeTransportAmounts.keySet().size();
    }
    
    public int getProjectLength() {
        return projectLength;
    }
    
    public double getCRF() {
        return crf;
    }
    
    public double getTotalCaptureCost() {
        double cost = 0;
        for (Source src : sourceCosts.keySet()) {
            cost += sourceCosts.get(src);
        }
        return cost;
    }
    
    public double getUnitCaptureCost() {
        return getTotalCaptureCost() / (captureAmountPerYear * projectLength);
    }
    
    public double getTotalStorageCost() {
        double cost = 0;
        for (Sink snk : sinkCosts.keySet()) {
            cost += sinkCosts.get(snk);
        }
        return cost;
    }
    
    public double getUnitStorageCost() {
        return getTotalStorageCost() / (captureAmountPerYear * projectLength);
    }
    
    public double getTotalTransportCost() {
        double cost = 0;
        for (Edge edg : edgeCosts.keySet()) {
            cost += edgeCosts.get(edg);
        }
        return cost;
    }
    
    public double getUnitTransportCost() {
        return getTotalTransportCost() / (captureAmountPerYear * projectLength);
    }

    public double getTotalCost() {
        return getTotalCaptureCost() + getTotalStorageCost() + getTotalTransportCost();
    }
    
    public double getUnitTotalCost() {
        return getUnitCaptureCost() + getUnitStorageCost() + getUnitTransportCost();
    }
    
    public double getPercentCaptured(Source source) {
        return sourceCaptureAmounts.get(source) / source.getProductionRate();
    }
    
    public double getPercentStored(Sink sink) {
        return (sinkStorageAmounts.get(sink) * projectLength) / sink.getCapacity();
    }
}
