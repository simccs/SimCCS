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
    private double productionRate;
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
    
    public void setProductionRate(double productionRate) {
        this.productionRate = productionRate;
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
        return productionRate;
    }
}
