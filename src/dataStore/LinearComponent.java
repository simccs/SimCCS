package dataStore;

/**
 *
 * @author yaw
 */
public class LinearComponent {
    private double conSlope; //con = construction
    private double conIntercept;
    private double rowSlope; //row = right of way
    private double rowIntercept;
    private double maxCapacity;
    
    private DataStorer data;
    
    public LinearComponent(DataStorer data) {
        this.data = data;
    }
    
    public void setConSlope(double conSlope) {
        this.conSlope = conSlope;
    }
    
    public void setConIntercept(double conIntercept) {
        this.conIntercept = conIntercept;
    }
    
    public void setRowSlope(double rowSlope) {
        this.rowSlope = rowSlope;
    }
    
    public void setRowIntercept(double rowIntercept) {
        this.rowIntercept = rowIntercept;
    }
    
    public void setMaxCapacity(double maxCapacity) {
        this.maxCapacity = maxCapacity;
    }
    
    public double getConSlope() {
        return conSlope;
    }
    
    public double getRowSlope() {
        return rowSlope;
    }
    
    public double getConIntercept() {
        return conIntercept;
    }
    
    public double getRowIntercept() {
        return rowIntercept;
    }
    
    public double getMaxCapacity() {
        return maxCapacity;
    }
}
