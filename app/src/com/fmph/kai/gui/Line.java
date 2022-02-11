package com.fmph.kai.gui;

import com.fmph.kai.util.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Represents the line displayed on the canvas.
 */
public class Line {
    private Vector2D start;
    private Vector2D stop;
    private final GraphicsContext gc;

    /**
     * Sets the `stop` member variable.
     * @param stop is the new stop vector
     */
    public void setStop(Vector2D stop) { this.stop = stop; }

    /**
     * Returns the stop 2d vector.
     * @return the 2d vector
     */
    public Vector2D getStop() { return this.stop; }

    /**
     * Sets the start position of the line.
     * @param start is the 2d vector of the start position
     */
    public void setStart(Vector2D start) { this.start = start; }

    /**
     * Returns the starting position of the line.
     * @return the start position 2d vector
     */
    public Vector2D getStart() { return this.start; }

    /**
     * Initialized the line on the specified start point.
     * Draws the point using the GraphicsContext provided.
     * @param start is the starting vector
     * @param gc is the graphics context
     * @see GraphicsContext
     */
    public Line(Vector2D start, GraphicsContext gc) {
        this.start = start;
        this.gc = gc;
        gc.setFill(Color.DARKBLUE);
        gc.fillOval(start.x-5, start.y-5, 10, 10);
    }

    /**
     * Draws a point onto the canvas.
     * @param px0 is the x coordinate
     * @param py0 is the y coordinate
     * @param gc is the graphics context
     */
    public static void pointX(double px0, double py0, GraphicsContext gc) {
        gc.setFill(Color.GREENYELLOW);
        gc.fillOval(px0-5, py0-5, 10, 10);
    }

    /**
     * Draws the line onto the canvas using the `start` and `stop` member variables.
     */
    public void draw() {
        gc.setLineWidth(3);
        gc.setStroke(Color.RED);
        gc.strokeLine(start.x, start.y, stop.x, stop.y);
        gc.setFill(Color.DARKBLUE);
        gc.fillOval(stop.x-5, stop.y-5, 10, 10);
        gc.fillOval(start.x-5, start.y-5, 10, 10);
    }
}