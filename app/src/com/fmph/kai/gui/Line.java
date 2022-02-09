package com.fmph.kai.gui;

import com.fmph.kai.util.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Line {
    private Vector2D start;
    private Vector2D stop;
    private final GraphicsContext gc;

    public void setStop(Vector2D stop) { this.stop = stop; }

    public Line(Vector2D start, GraphicsContext gc) {
        this.start = start;
        this.gc = gc;
        gc.setFill(Color.DARKBLUE);
        gc.fillOval(start.x-5, start.y-5, 10, 10);
    }

    public static void pointX(double px0, double py0, GraphicsContext gc) {
        gc.setFill(Color.GREENYELLOW);
        gc.fillOval(px0-5, py0-5, 10, 10);
    }

    public void draw() {
        gc.setLineWidth(3);
        gc.setStroke(Color.RED);
        gc.strokeLine(start.x, start.y, stop.x, stop.y);
        gc.setFill(Color.DARKBLUE);
        gc.fillOval(stop.x-5, stop.y-5, 10, 10);
        gc.fillOval(start.x-5, start.y-5, 10, 10);
    }
}