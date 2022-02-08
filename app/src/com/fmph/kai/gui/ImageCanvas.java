package com.fmph.kai.gui;

import com.fmph.kai.util.Compute;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class ImageCanvas extends Canvas {
    private Line line = null;
    private Image image;
    private Compute compute;

    public ImageCanvas(double width, double height, Compute compute) {
        this.compute = compute;
        setWidth(width);
        setHeight(height);
    }

    private void reset() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, getWidth(), getHeight());
        gc.drawImage(image, 0, 0, getWidth(), image.getHeight()*getWidth()/image.getWidth());
        compute.maxMouseX = (int) getWidth();
        compute.maxMouseY = (int) (image.getHeight()*getWidth()/image.getWidth());
    }

    public void setImage(Image image) {
        this.image = image;
        reset();
    }

    public Image getImage() {
        return image;
    }

    public boolean click(double x, double y) {
        if (image == null) return false;
        if (compute.lengthLen < 0) {
            if (compute.clickLenX1 < 0) {
                Line.pointX(x, y, getGraphicsContext2D());
                compute.clickLenX1 = (int) x;
                compute.clickLenY1 = (int) y;
                return false;
            } else {
                Line.pointX(x, y, getGraphicsContext2D());
                compute.clickLenX2 = (int) x;
                compute.clickLenY2 = (int) y;
                return true;
            }
        } else {
            if (line == null) {
                line = new Line(x, y, getGraphicsContext2D());
                compute.clickLineX1 = (int) x;
                compute.clickLineY1 = (int) y;
                return false;
            } else {
                reset();
                line.setX1(x);
                line.setY1(y);
                line.draw();
                compute.clickLineX2 = (int) x;
                compute.clickLineY2 = (int) y;
                return true;
            }
        }
    }





    public void resetLine() {
        line = null;
        reset();
    }
}
