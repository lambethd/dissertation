package com.dl561.simulation.course.location;

import org.springframework.stereotype.Component;

@Component
public class Location {
    private double x;
    private double y;

    public Location() {
    }

    public Location(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
}