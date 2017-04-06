package de.bioforscher.singa.mathematics.forces;

public abstract class AbstractForce {

    private double forceConstant;

    public AbstractForce(double forceConstant) {
        this.forceConstant = forceConstant;
    }

    public double getForceConstant() {
        return this.forceConstant;
    }

    public void setForceConstant(double forceConstant) {
        this.forceConstant = forceConstant;
    }

}
