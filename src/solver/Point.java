package solver;

/**
 *
 * @author yaw
 */
public class Point {
    private double x;
    private double y; 
   
    // create and initialize a random point in unit square
    public Point() {
        this.x = Math.random();
        this.y = Math.random();
    }

    // create and initialize a point with given (x, y)
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }
  
    // accessor methods
    public double x() { return x; }
    public double y() { return y; }

    // return Euclidean distance between this and p
    public double distanceTo(Point p) {
        double dx = this.x - p.x;
        double dy = this.y - p.y;
        return Math.sqrt(dx*dx + dy*dy);
    }

    // return signed area of triangle a->b->c
    public static double area(Point a, Point b, Point c) {
        return 0.5 * (a.x*b.y - a.y*b.x + a.y*c.x - a.x*c.y + b.x*c.y - c.x*b.y);
    }

    // is a->b->c a counterclockwise turn
    // +1 (yes), -1 (no), 0 (collinear)
    public static int ccw(Point a, Point b, Point c) {
        double area = area(a, b, c);
        if      (area > 0) return +1;
        else if (area < 0) return -1;
        else               return  0;
    }

    // is invoking point inside circle defined by a-b-c
    // if circle is degenerate, return true
    public boolean inside(Point a, Point b, Point c) {
         if      (ccw(a, b, c) > 0) return (in(a, b, c) > 0);
         else if (ccw(a, b, c) < 0) return (in(a, b, c) < 0);
         return true;
    }

    // return positive, negative, or zero depending on whether
    // invoking point is inside circle defined by a, b, and c
    // assumes a-b-c are counterclockwise
    private double in(Point a, Point b, Point c) {
         Point d = this;
         double adx = a.x - d.x;
         double ady = a.y - d.y;
         double bdx = b.x - d.x;
         double bdy = b.y - d.y;
         double cdx = c.x - d.x;  
         double cdy = c.y - d.y;

         double abdet = adx * bdy - bdx * ady;
         double bcdet = bdx * cdy - cdx * bdy;
         double cadet = cdx * ady - adx * cdy;
         double alift = adx * adx + ady * ady;
         double blift = bdx * bdx + bdy * bdy;
         double clift = cdx * cdx + cdy * cdy;

         return alift * bcdet + blift * cadet + clift * abdet;
    }

    // return string representation of this point
    public String toString() {
        return "(" + x + ", " + y + ")";
    } 
}
