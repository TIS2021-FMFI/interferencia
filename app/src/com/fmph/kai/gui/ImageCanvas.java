package com.fmph.kai.gui;

import com.fmph.kai.util.Compute;
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
        Compute.maxMouseX = (int) getWidth();
        Compute.maxMouseY = (int) (image.getHeight()*getWidth()/image.getWidth());
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
        if (line == null) {
            line = new Line(x, y, getGraphicsContext2D());
            System.out.println("prvy bod x:"+ x+ "y:"+ y);
            Compute.clickLineX1 = (int)x;
            Compute.clickLineY1 = (int)y;
            return false;
        } else {
            reset();
            line.setX1(x);
            line.setY1(y);
            line.draw();
            System.out.println("druhy bod x:"+ x+ "y:"+ y);
            Compute.clickLineX2 = (int)x;
            Compute.clickLineY2 = (int)y;
            return true;
        }
    }





    public void resetLine() {
        line = null;
        reset();
    }
}
