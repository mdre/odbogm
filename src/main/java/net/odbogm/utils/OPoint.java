package net.odbogm.utils;

/**
 * An attribute of this type is mapped to the OrientDB `OPoint` geometry object.
 * 
 * @author jbertinetti
 */
public class OPoint {

    private final double x;

    private final double y;


    public OPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

}
