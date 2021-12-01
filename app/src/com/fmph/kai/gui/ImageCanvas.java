package com.fmph.kai.gui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class ImageCanvas extends Canvas {
    private Line line = null;
    private Image image;

    public ImageCanvas(double width, double height) {
        setWidth(width);
        setHeight(height);
    }

    private void reset() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, getWidth(), getHeight());
        gc.drawImage(image, 0, 0, getWidth(), image.getHeight()*getWidth()/image.getWidth());
    }

    public void setImage(Image image) {
        this.image = image;
        reset();
    }

    public boolean click(double x, double y) {
        if (image == null) return false;
        if (line == null) {
            line = new Line(x, y, getGraphicsContext2D());
            return false;
        } else {
            reset();
            line.setX1(x);
            line.setY1(y);
            line.draw();
            return true;
        }
    }

    public void resetLine() {
        line = null;
        reset();
    }
}
