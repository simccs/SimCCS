package solver;

import dataStore.DataStorer;
import dataStore.Edge;
import java.util.Arrays;
import java.util.HashSet;

/**
 *
 * @author yaw
 */
public class Delaunay {
    private Point[] points;
    DataStorer data;
    HashSet<Neighbors> edges;

    public Delaunay(DataStorer data) {
        this.data = data;
    }

    public HashSet<Edge> run(int[] rawPoints) {
        if (rawPoints.length == 2) {
            HashSet<Edge> delaunayPairs = new HashSet<>();
            delaunayPairs.add(new Edge(rawPoints[0], rawPoints[1]));
            return delaunayPairs;
        }
        
        edges = new HashSet<>();
        
        points = new Point[rawPoints.length];
        for (int ptNum = 0; ptNum < rawPoints.length; ptNum++) {
            double[] coords = data.cellLocationToRawXY(rawPoints[ptNum]);
            points[ptNum] = new Point(coords[0], coords[1]);
        }
        
        for (Point p : points) {
            addPoint(p.x(), p.y());
        }
        
        HashSet<Edge> delaunayPairs = new HashSet<>();
        
        for (Neighbors n : edges) {
            delaunayPairs.add(new Edge(n.cellLocations[0], n.cellLocations[1]));
        }
        
        return delaunayPairs;
    }
    
    public void addPoint(double x, double y) {
        for (int i = 0; i < points.length; i++) {
            for (int j = i+1; j < points.length; j++) {
                for (int k = j+1; k < points.length; k++) {
                    boolean isTriangle = true;
                    for (int a = 0; a < points.length; a++) {
                        if (a == i || a == j || a == k) continue;
                        if (points[a].inside(points[i], points[j], points[k])) {
                           isTriangle = false;
                           break;
                        }
                    }
                    if (isTriangle) {
                        int point1Cell = data.xyToVectorized((int)points[i].x(), (int)points[i].y());
                        int point2Cell = data.xyToVectorized((int)points[j].x(), (int)points[j].y());
                        int point3Cell = data.xyToVectorized((int)points[k].x(), (int)points[k].y());
                        Neighbors edge1 = new Neighbors(point1Cell, point2Cell);
                        Neighbors edge2 = new Neighbors(point1Cell, point3Cell);
                        Neighbors edge3 = new Neighbors(point2Cell, point3Cell);
                        edges.add(edge1);
                        edges.add(edge2);
                        edges.add(edge3);
                    }
                }
            }
        }
    }   
    
    public class Neighbors{
        public int[] cellLocations;
        
        public Neighbors(int c1, int c2) {
            if (c1 < c2) {
            cellLocations = new int[] {c1, c2};
            } else {
                cellLocations = new int[] {c2, c1};
            }
        }

        @Override
        public boolean equals(Object o) {
            Neighbors other = (Neighbors) o;
            return Arrays.equals(other.cellLocations, cellLocations);
        }
        
        @Override
        public int hashCode() {
            return Arrays.hashCode(cellLocations);
        }
    }
}
