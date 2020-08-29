package dataStore;

/**
 *
 * @author yaw
 */
public class Sink {
    private int cellNum;
    private double openingCost;
    private double omCost;
    private double wellOpeningCost;
    private double wellOMCost;
    private double injectionCost;
    private double wellCapacity;
    private double capacity;
    private String label;
    
    private DataStorer data;
    
    private double remainingCapacity;    //Heuristic
    private int numWells;   //Heuristic
    
    public Sink(DataStorer data) {
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
    
    public void setWellOpeningCost(double wellOpeningCost) {
        this.wellOpeningCost = wellOpeningCost;
    }
    
    public void setWellOMCost(double wellOMCost) {
        this.wellOMCost = wellOMCost;
    }
    
    public void setInjectionCost(double injectionCost) {
        this.injectionCost = injectionCost;
    }
    
    public void setWellCapacity(double wellCapacity) {
        this.wellCapacity = wellCapacity;
    }
    
    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }
    
    // Heuristic
    public void setRemainingCapacity(double remaingCapacity) {
        this.remainingCapacity = remaingCapacity;
    }
    
    // Heuristic
    public void setNumWells(int numWells) {
        this.numWells = numWells;
    }
    
    //Heuristic
    public int getNumWells() {
        return numWells;
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
    
    public double getWellOpeningCost(double crf) {
        return crf * wellOpeningCost + wellOMCost;
    }
    
    public double getInjectionCost() {
        return injectionCost;
    }
    
    public double getWellCapacity() {
        return wellCapacity;
    }
    
    public double getCapacity() {
        return capacity;
    }
    
    public boolean isSimplified() {
        if (openingCost == 0 && omCost == 0 && wellOpeningCost == 0 && wellOMCost == 0) {
            return true;
        }
        return false;
    }
}
