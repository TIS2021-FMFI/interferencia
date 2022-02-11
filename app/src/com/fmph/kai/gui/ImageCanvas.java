package com.fmph.kai.gui;

import com.fmph.kai.util.Vector2D;
import com.fmph.kai.util.Compute;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * The canvas, which displays the image, the line and dots selected.
 */
public class ImageCanvas extends Canvas {
    private Line line;
    private Image image;
    private double imageWidth;
    private double aspectRatio;
    private Vector2D imagePosition;
    private Vector2D offset;
    private final Compute compute;

    /**
     * Initializes the canvas.
     * @param width the width of the canvas
     * @param height the height of the canvas
     * @param compute an instance of the Compute class used for the computational tasks.
     */
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

    /**
     * Resets the canvas, redraws the image and line if available.
     */
    public void reset() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, getWidth(), getHeight());
        if (image != null) {
            gc.drawImage(image, imagePosition.x, imagePosition.y, imageWidth, imageWidth * aspectRatio);
            compute.maxMouseX = (int) getWidth();
            compute.maxMouseY = (int) (image.getHeight() * getWidth() / image.getWidth());
            if (line != null)
                line.draw();
        }
    }

    /**
     * Changes the background image of the canvas.
     * @param image the new image
     */
    public void setImage(Image image) {
        this.image = image;
        if (image != null)
            aspectRatio = image.getHeight()/image.getWidth();
        imageWidth = getWidth();
        imagePosition = Vector2D.zero();
        reset();
    }

    /**
     * Saves the mouse position to the `offset` member variable.
     * @param mousePosition vector of x and y coordinates of the mouse related to the canvas
     */
    public void rightPressed(Vector2D mousePosition) {
        this.offset = imagePosition.add(mousePosition.multiply(-1));
    }

    /**
     * Moves the image and updates the canvas.
     * Sets the `offset` to null.
     * @param mousePosition
     */
    public void rightReleased(Vector2D mousePosition) {
        imagePosition = mousePosition.add(offset);
        reset();
        offset = null;
    }

    /**
     * Moves the image and updates the canvas.
     * @param mousePosition
     */
    public void rightDragged(Vector2D mousePosition) {
        if (offset == null) return;
        imagePosition = mousePosition.add(offset);
        reset();
    }

    /**
     * Returns the current background image of the canvas.
     * @return the background image
     */
    public Image getImage() {
        return image;
    }

    /**
     * Defines the behaviour on the left mouse click.
     * @param mousePosition the 2d vector representing the mouse position
     * @return true if this is not the first click on the canvas
     */
    public boolean leftClick(Vector2D mousePosition) {
        if (image == null) return false;
        if (compute.lengthLen < 0) {
            if (compute.clickLenX1 < 0) {
                Line.pointX(mousePosition.x, mousePosition.y, getGraphicsContext2D());
                compute.clickLenX1 = (int) mousePosition.x;
                compute.clickLenY1 = (int) mousePosition.y;

                return false;
            } else {
                Line.pointX(mousePosition.x, mousePosition.y, getGraphicsContext2D());
                compute.clickLenX2 = (int) mousePosition.x;
                compute.clickLenY2 = (int) mousePosition.y;

                return true;
            }
        } else {
            if (line == null) {

                line = new Line(mousePosition, getGraphicsContext2D());

                compute.clickLineX1 = (int) mousePosition.x;
                compute.clickLineY1 = (int) mousePosition.y;
                return false;
            } else {
                line.setStop(mousePosition);
                reset();
                
                compute.clickLineX2 = (int) mousePosition.x;
                compute.clickLineY2 = (int) mousePosition.y;
                return true;
            }
        }
    }

    /**
     * Sets the `line` member variable to null and updates the canvas, so there is no line displayed.
     */
    public void resetLine() {
        line = null;
        reset();
    }

    /**
     * Zooms the image according to the mouse position.
     * @param d is the Y delta of the mouse wheel
     * @param mousePosition is the position of the mouse
     */
    public void zoom(double d, Vector2D mousePosition) {
        if (image == null || d < 0 && imageWidth - getWidth() < 0.1) return;
        imageWidth += d;
        imagePosition = imagePosition.add(mousePosition.multiply(d).divide(getWidth()).multiply(-1));
        reset();
    }
}
