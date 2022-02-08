package com.fmph.kai.gui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Line {
    private double x0, y0;
    private double x1, y1;
    private final GraphicsContext gc;

    public void setX1(double x1) {
        this.x1 = x1;
    }

    public void setY1(double y1) {
        this.y1 = y1;
    }


    public Line(double x0, double y0, GraphicsContext gc) {
        this.x0 = x0;
        this.y0 = y0;
        this.gc = gc;
        gc.setFill(Color.DARKBLUE);
        gc.fillOval(x0-5, y0-5, 10, 10);
    }

    public static void pointX(double px0, double py0, GraphicsContext gc) {
        gc.setFill(Color.GREENYELLOW);
        gc.fillOval(px0-5, py0-5, 10, 10);
    }

    public void draw() {
        gc.setLineWidth(3);
        gc.setStroke(Color.RED);
        gc.strokeLine(x0, y0, x1, y1);
        gc.setFill(Color.DARKBLUE);
        gc.fillOval(x1-5, y1-5, 10, 10);
        gc.fillOval(x0-5, y0-5, 10, 10);
    }
}