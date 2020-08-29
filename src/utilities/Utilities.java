package utilities;

/**
 *
 * @author yaw
 */
public class Utilities {
    public static int[] convertIntegerArray(Integer[] a) {
        int[] returnA = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            returnA[i] = a[i];
        }
        return returnA;
    }

    public static double[] convertDoubleArray(Double[] a) {
        double[] returnA = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            returnA[i] = a[i];
        }
        return returnA;
    }
    
    public static double round(double val, int numAfterDecimal) {
        double div = Math.pow(10, numAfterDecimal);
        return Math.round(val * div) / div;
    }
    
    public static boolean isDouble(String value) {  
     try {  
         Double.parseDouble(value);  
         return true;  
      } catch (NumberFormatException e) {  
         return false;  
      }  
}
}
