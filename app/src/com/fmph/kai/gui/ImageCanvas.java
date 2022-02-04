package com.fmph.kai.gui;

import com.fmph.kai.util.Vector2D;
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
    private Vector2D imagePosition;
    private Vector2D offset;

    public ImageCanvas(double width, double height) {
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
        if (image != null)
            gc.drawImage(image, imagePosition.x, imagePosition.y, imageWidth, imageWidth*aspectRatio);
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

    public boolean leftClick(double x, double y) {
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

    public void zoom(double d, Vector2D mousePosition) {
        if (image == null || d < 0 && imageWidth - getWidth() < 0.1) return;
        imageWidth += d;
        imagePosition = imagePosition.add(mousePosition.multiply(d).divide(getWidth()).multiply(-1));
        reset();
    }
}
