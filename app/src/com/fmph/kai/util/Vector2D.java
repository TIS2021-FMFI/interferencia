package com.fmph.kai.util;

/**
 * Represent the 2D vector of two floating point numbers.
 */
public class Vector2D {
    /**
     * The `x` and `y` coordinates of the vector.
     */
    public double x, y;

    /**
     * Initializes the vector with the specified position.
     * @param x is the coordinate on the horizontal axis
     * @param y is the coordinate on the vertical axis
     */
    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Performs vector addition on this vector and returns a new one.
     * @param other is the vector to be added
     * @return new addition vector
     */
    public Vector2D add(Vector2D other) {
        this.x += other.x;
        this.y += other.y;
        return new Vector2D(x, y);
    }

    /**
     * Performs multiplication by a scalar value.
     * @param a is the scalar multiplicative
     * @return new multiplied vector
     */
    public Vector2D multiply(double a) {
        this.x *= a;
        this.y *= a;
        return new Vector2D(x, y);
    }

    /**
     * Divides the vector by a scalar value.
     * @param a is the scalar divider
     * @return new division vector
     */
    public Vector2D divide(double a) {
        this.x /= a;
        this.y /= a;
        return new Vector2D(x, y);
    }

    /**
     * Creates a zero vector representing the zero point of the coordinate system.
     * @return the new zero vector
     */
    public static Vector2D zero() {
        return new Vector2D(0, 0);
    }
}