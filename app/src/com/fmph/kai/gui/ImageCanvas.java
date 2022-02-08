package com.fmph.kai.gui;

import com.fmph.kai.util.Vector2D;
import com.fmph.kai.util.Compute;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class ImageCanvas extends Canvas {
    private Line line;
    private Image image;
    private double imageWidth;
    private double aspectRatio;
    private Vector2D imagePosition;
    private Vector2D offset;
    private Compute compute;

    public ImageCanvas(double width, double height, Compute compute) {
        this.compute = compute;
        setWidth(width);
        setHeight(height);
        imageWidth = width;
        imagePosition = Vector2D.zero();
        line = null;
        image = null;
        offset = null;
    }

    public void reset() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, getWidth(), getHeight());
        if (image != null) {
            gc.drawImage(image, imagePosition.x, imagePosition.y, imageWidth, imageWidth * aspectRatio);
            compute.maxMouseX = (int) getWidth();
            compute.maxMouseY = (int) (image.getHeight() * getWidth() / image.getWidth());
        }
    }

    public void setImage(Image image) {
        this.image = image;
        if (image != null)
            aspectRatio = image.getHeight()/image.getWidth();
        imageWidth = getWidth();
        imagePosition = Vector2D.zero();
        reset();
    }

    public void rightPressed(Vector2D mousePosition) {
        this.offset = imagePosition.add(mousePosition.multiply(-1));
    }

    public void rightReleased(Vector2D mousePosition) {
        imagePosition = mousePosition.add(offset);
        reset();
        offset = null;
    }

    public void rightDragged(Vector2D mousePosition) {
        if (offset == null) return;
        imagePosition = mousePosition.add(offset);
        reset();
    }

    public Image getImage() {
        return image;
    }

    public boolean leftClick(Vector2D mousePosition) {
        if (image == null) return false;
        if (compute.lengthLen < 0) {
            System.out.println("prvy if klik"+compute.clickLenX1);
            if (compute.clickLenX1 < 0) {
                Line.pointX(mousePosition.x, mousePosition.y, getGraphicsContext2D());
                compute.clickLenX1 = (int) mousePosition.x;
                compute.clickLenY1 = (int) mousePosition.y;
                System.out.println("klik 1");
                return false;
            } else {
                Line.pointX(mousePosition.x, mousePosition.y, getGraphicsContext2D());
                compute.clickLenX2 = (int) mousePosition.x;
                compute.clickLenY2 = (int) mousePosition.y;
                System.out.println("klik 2");
                return true;
            }
        } else {
            if (line == null) {
                line = new Line(mousePosition.x, mousePosition.y, getGraphicsContext2D());
                System.out.println("prvy bod x:" + mousePosition.x + "y:" + mousePosition.y);
                compute.clickLineX1 = (int) mousePosition.x;
                compute.clickLineY1 = (int) mousePosition.y;
                return false;
            } else {
                reset();
                line.setX1(mousePosition.x);
                line.setY1(mousePosition.y);
                line.draw();
                System.out.println("druhy bod x:" + mousePosition.x + "y:" + mousePosition.y);
                compute.clickLineX2 = (int) mousePosition.x;
                compute.clickLineY2 = (int) mousePosition.y;
                return true;
            }
        }
    }

    public void resetLine() {
        line = null;
        reset();
    }

    public void zoom(double d, Vector2D mousePosition) {
        if (image == null || d < 0 && imageWidth - getWidth() < 0.1) return;
        imageWidth += d;
        imagePosition = imagePosition.add(mousePosition.multiply(d).divide(getWidth()).multiply(-1));
        reset();
    }
}
