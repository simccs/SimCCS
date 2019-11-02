/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dataStore;

/**
 *
 * @author yaw
 */
public class UnidirEdge {
    public int v1;
    public int v2;

    public UnidirEdge(int v1, int v2) {
        this.v1 = v1;
        this.v2 = v2;
    }
    
    @Override
    public int hashCode() {
        if (v1 > v2) {
            return v1 + v2 - 1;
        }
        return v1 + v2 + 1;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UnidirEdge other = (UnidirEdge) obj;
        return (v1 == other.v1 && v2 == other.v2);
    }
    
    public String toString() {
        return v1 + " -> " + v2;
    }
}
