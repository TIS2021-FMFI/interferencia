package com.fmph.kai.gui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;

public class ImageCanvas extends Canvas {
    private Line line;
    private Image image;
    private double imageWidth;
    private double aspectRatio;
    private double left;
    private double top;

    public ImageCanvas(double width, double height) {
        setWidth(width);
        setHeight(height);
        imageWidth = width;
        left = 0;
        top = 0;
        line = null;
        image = null;
    }

    public void reset() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, getWidth(), getHeight());
        if (image != null)
            gc.drawImage(image, left, top, imageWidth, imageWidth*aspectRatio);
    }

    public void setImage(Image image) {
        this.image = image;
        if (image != null)
            aspectRatio = image.getHeight()/image.getWidth();
        imageWidth = getWidth();
        left = 0;
        top = 0;
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

    public void zoom(double d, double x, double y) {
        if (d < 0 && imageWidth - getWidth() < 0.1) return;
        imageWidth += d;
        left -= d*x/getWidth();
        top -= d*y/(imageWidth*aspectRatio);
        reset();
    }
}
