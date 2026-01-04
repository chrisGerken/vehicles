package com.gerkenip.vehicles.model;

import java.util.Objects;

/**
 * Named color definition with RGB values.
 */
public class ColorDefinition {
    private String name;
    private int r;  // 0-255
    private int g;  // 0-255
    private int b;  // 0-255

    public ColorDefinition() {
    }

    public ColorDefinition(String name, int r, int g, int b) {
        this.name = name;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    /**
     * Returns hex color string in #RRGGBB format.
     */
    public String toHex() {
        return String.format("#%02X%02X%02X", r, g, b);
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getR() {
        return r;
    }

    public void setR(int r) {
        this.r = r;
    }

    public int getG() {
        return g;
    }

    public void setG(int g) {
        this.g = g;
    }

    public int getB() {
        return b;
    }

    public void setB(int b) {
        this.b = b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColorDefinition that = (ColorDefinition) o;
        return r == that.r && g == that.g && b == that.b && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, r, g, b);
    }

    @Override
    public String toString() {
        return name + " " + toHex();
    }
}
