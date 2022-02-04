package com.fmph.kai.util;

public class Vector2D {
    public double x, y;
    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }
    public Vector2D add(Vector2D other) {
        this.x += other.x;
        this.y += other.y;
        return new Vector2D(x, y);
    }
    public Vector2D multiply(double a) {
        this.x *= a;
        this.y *= a;
        return new Vector2D(x, y);
    }
    public Vector2D divide(double a) {
        this.x /= a;
        this.y /= a;
        return new Vector2D(x, y);
    }
    public static Vector2D zero() {
        return new Vector2D(0, 0);
    }
}