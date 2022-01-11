package com.fmph.kai.util;


import javafx.scene.paint.Color;

public class MPoint {
    public int x = 0;
    public int y = 0;
    public Color color = null;
    public double r = 0;
    public double g = 0;
    public double b = 0;
    public double suc = 0;
    public int ldiff = 0;  //1 ak je viac ako predosly, -1 ak je menej ako predosly, 0 inac
    public int rdiff = 0;  //-1 ak je viac ako buduci, 1 ak je menej ako buduci, 0 inac   ... max je ldiff=1 rdiff=-1
    public int seq = 0;

    public MPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void print(){
        System.out.println("Mpoint " + seq + " xy " + x + " " + y + " diff " + ldiff + " " + rdiff + " rgb " + r + " " + g + " " + b + " suc " + suc);
    }
}
