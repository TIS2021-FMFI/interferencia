package com.fmph.kai.util;



import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;


import java.awt.*;
import java.util.ArrayList;

public class Compute {
    public int click = 0;

    public static int maxMouseX = 0;
    public static int maxMouseY = 0;

    public static int clickLineX1 = 0;
    public static int clickLineY1 = 0;
    public static int clickLineX2 = 0;
    public static int clickLineY2 = 0;

    public static int clickLenX1 = 0;
    public static int clickLenY1 = 0;
    public static int clickLenX2 = 0;
    public static int clickLenY2 = 0;
    public static double lengthClickLen = 100;
    public static double lengthLen = 5;

    public static double chybicka = 0.05;

    public static ArrayList<MPoint> pointlist = new ArrayList<MPoint>();

    public static ArrayList<MPoint> maxlist = new ArrayList<MPoint>();


    public static int getMaxNumber(){
        return maxlist.size();
    }

    public static MPoint getMax(int maxidx){
        return maxlist.get(maxidx);
    }

    public static double getRealDistance(int maxidx1, int maxidx2){
        try{
            double res = 0.0;
            MPoint p1 = getMax(maxidx1);
            MPoint p2 = getMax(maxidx2);
            res = Math.sqrt((double) (p1.x-p2.x)*(p1.x-p2.x) + (double) (p1.y-p2.y)*(p1.y-p2.y));
            res = res*lengthLen/lengthClickLen;
            return res;

        } catch (Exception ex){
            ex.printStackTrace();
            return 0.0;
        }
    }

    public static boolean analyze() {
        lengthClickLen = Math.sqrt((double) (clickLenX1-clickLenX2)*(clickLenX1-clickLenX2) + (double) (clickLenY1-clickLenY2)*(clickLenY1-clickLenY2));

        //setup derivacii + vyhladenie malych zmien
        for (int i=0; i<pointlist.size(); i++) {
            if(i == 0){
                pointlist.get(i).ldiff = 0;
            } else {
                pointlist.get(i).ldiff = getlrdiff(pointlist.get(i-1).suc, pointlist.get(i).suc);
            }
            if(i == pointlist.size()-1){
                pointlist.get(i).rdiff = 0;
            } else {
                pointlist.get(i).rdiff = getlrdiff(pointlist.get(i).suc, pointlist.get(i+1).suc);
            }
        }

        MPoint lmax = null;
        if(pointlist.get(0).rdiff == 0){
            //initial bod moze byt lmax
            lmax = pointlist.get(0);
        }
        for(MPoint p:pointlist){
            if(p.ldiff > 0 && p.rdiff < 0){
                //ciste maximum
                maxlist.add(p);
            } else if(p.ldiff == 0 && p.rdiff < 0){
                //konstanta a klesa
                if(lmax != null){
                    //maximum medzi lmax a p
                    int idx = (lmax.seq + p.seq)/2;  //zaokruhloanie, zoberie sa nejaky v strede
                    maxlist.add(pointlist.get(idx));
                    lmax = null;   //lmax sa pouzilo - bude null
                } else {
                    //nic sa neudeje... nie je lmax takze to klesalo potom konst a dalej klesa
                }
            } else if(p.ldiff > 0 && p.rdiff == 0){
                //rastie alebo konstantne
                lmax = p;   //aktualizuje sa, no matter what
            }
        }

        return true;
    }

    private static int getlrdiff(double a, double b){
        //vrati 0 ak konstanta, 1 ak rastie a -1 ak klesa
        if(Math.abs(a-b) < chybicka){
            return 0;
        }
        if(a < b){
            return 1;
        }
        return -1;
    }

    public static boolean colorize(Image image) {
        PixelReader pixelreader = image.getPixelReader();
        int maxImageX = (int) image.getWidth();
        int maxImageY = (int) image.getHeight();

        int x1 = (clickLineX1*(int) image.getWidth())/maxMouseX;
        int y1 = (clickLineY1*(int) image.getHeight())/maxMouseY;
        int x2 = (clickLineX2*(int) image.getWidth())/maxMouseX;
        int y2 = (clickLineY2*(int) image.getHeight())/maxMouseY;
        bresenham(x1, y1, x2, y2);

        try {
            int seq = 0;
            for (MPoint p : Compute.pointlist) {
                p.color = pixelreader.getColor(p.x, p.y);
                p.r = p.color.getRed();
                p.g = p.color.getGreen();
                p.b = p.color.getBlue();
                p.suc = p.r+p.g+p.b;
                p.seq = seq;
                seq = seq + 1;
                System.out.println("farby "+p.x+" "+p.y+" "+p.r+" "+p.g+" "+p.b+" "+p.suc);
            }
            return true;
        } catch (Exception e){
            System.out.println(e);
            return false;
        }
    }

    public static boolean bresenham(int x1, int y1, int x2, int y2) {
        // delta of exact value and rounded value of the dependent variable
        pointlist.clear();
        maxlist.clear();
        int d = 0;

        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);

        int dx2 = 2 * dx; // slope scaling factors to
        int dy2 = 2 * dy; // avoid floating point

        int ix = x1 < x2 ? 1 : -1; // increment direction
        int iy = y1 < y2 ? 1 : -1;

        int x = x1;
        int y = y1;

        if (dx >= dy) {
            while (true) {
                pointlist.add(new MPoint(x, y));
                if (x == x2)
                    break;
                x += ix;
                d += dy2;
                if (d > dx) {
                    y += iy;
                    d -= dx2;
                }
            }
        } else {
            while (true) {
                pointlist.add(new MPoint(x, y));
                if (y == y2)
                    break;
                y += iy;
                d += dx2;
                if (d > dy) {
                    x += ix;
                    d -= dy2;
                }
            }
        }
        return true;
    }

    public static void printPointList(){
        System.out.println("\nPRINT POINT LIST - START");
        for(MPoint p:pointlist){
            p.print();
        }
        System.out.println("PRINT POINT LIST - END\n");
    }

    public static void printMaxList(){
        System.out.println("\nPRINT MAX LIST - START");
        for(MPoint p:maxlist){
            p.print();
        }
        System.out.println("PRINT MAX LIST - END\n");
    }

}
