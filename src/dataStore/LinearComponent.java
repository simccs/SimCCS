package dataStore;

/**
 *
 * @author yaw
 */
public class LinearComponent {
    private double conAlpha; //con = construction
    private double conBeta;
    private double rowAlpha; //row = right of way
    private double rowBeta;
    
    private DataStorer data;
    
    public LinearComponent(DataStorer data) {
        this.data = data;
    }
    
    public void setConAlpha(double conAlpha) {
        this.conAlpha = conAlpha;
    }
    
    public void setConBeta(double conBeta) {
        this.conBeta = conBeta;
    }
    
    public void setRowAlpha(double rowAlpha) {
        this.rowAlpha = rowAlpha;
    }
    
    public void setRowBeta(double rowBeta) {
        this.rowBeta = rowBeta;
    }
    
    public double getConAlpha() {
        return conAlpha;
    }
    
    public double getRowAlpha() {
        return rowAlpha;
    }
    
    public double getConBeta() {
        return conBeta;
    }
    
    public double getRowBeta() {
        return rowBeta;
    }
}
