package dataStore;

/**
 *
 * @author yaw
 */
public class Source {
    private int cellNum;
    private double openingCost;
    private double omCost;
    private double captureCost;
    private double[] productionRates;
    private String label;
    
    private DataStorer data;
    
    private double remainingCapacity;    //Heuristic
    
    public Source(DataStorer data) {
        this.data = data;
    }
    
    public void setCellNum(int cellNum) {
        this.cellNum = cellNum;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    public void setOpeningCost(double openingCost) {
        this.openingCost = openingCost;
    }
    
    public void setOMCost(double omCost) {
        this.omCost = omCost;
    }
    
    public void setCaptureCost(double captureCost) {
        this.captureCost = captureCost;
    }
    
    public void setProductionRates(double[] productionRates) {
        this.productionRates = productionRates;
    }
    
    // Heuristic
    public void setRemainingCapacity(double remaingCapacity) {
        this.remainingCapacity = remaingCapacity;
    }

    // Heuristic
    public double getRemainingCapacity() {
        return remainingCapacity;
    }
    
    public int getCellNum() {
        return cellNum;
    }
    
    public String getLabel() {
        return label;
    }
    
    public double getOpeningCost(double crf) {
        return crf * openingCost + omCost;
    }
    
    public double getCaptureCost() {
        return captureCost;
    }
    
    public double getProductionRate() {
        return productionRates[0];
    }
    
    public double getProductionRate(int timeslot) {
        if (timeslot >= productionRates.length) {
            return productionRates[0];
        }
        return productionRates[timeslot];
    }
    
    public double getMaxProductionRate() {
        double max = Double.NEGATIVE_INFINITY;
        
        for (double cur: productionRates) {
            max = Math.max(max, cur);
        }
        return max;
    }
}
