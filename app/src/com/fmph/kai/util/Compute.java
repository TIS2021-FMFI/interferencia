package com.fmph.kai.util;



import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;


import java.awt.*;
import java.util.ArrayList;

public class Compute {

    public int maxMouseX = 0;
    public int maxMouseY = 0;

    public int clickLineX1 = -1;
    public int clickLineY1 = -1;
    public int clickLineX2 = -1;
    public int clickLineY2 = -1;

    public int clickLenX1 = -1;
    public int clickLenY1 = -1;
    public int clickLenX2 = -1;
    public int clickLenY2 = -1;

    public double lengthClickLen = -1;
    public double lengthLen = -1;
    public int lineWidth = 5;
    public int numMaxima = 2; // <- use this value for the calculations

    public double ymin,ymax;
    public double x1, y1, x2, y2;
    public double x1Len, y1Len, x2Len, y2Len;
    public double dHandler;

    public double chybicka = 0.005;

    public ArrayList<MPoint> pointlist = new ArrayList<MPoint>();

    public ArrayList<MPoint> maxlist = new ArrayList<MPoint>();

    public Compute()
    {

    }

    public int getMaxNumber(){
        return maxlist.size();
    }


    public MPoint getMax(int maxidx){
        return maxlist.get(maxidx);
    }

    public double getRealDistance(int maxidx1, int maxidx2){
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

    public boolean analyze() {

        //setup derivacii + vyhladenie malych zmien
        for (int i = 0; i < pointlist.size(); i++) {
            if (i == 0) {
                pointlist.get(i).ldiff = 0;
            } else {
                pointlist.get(i).ldiff = getlrdiff(pointlist.get(i - 1).suc, pointlist.get(i).suc);
            }
            if (i == pointlist.size() - 1) {
                pointlist.get(i).rdiff = 0;
            } else {
                pointlist.get(i).rdiff = getlrdiff(pointlist.get(i).suc, pointlist.get(i + 1).suc);
            }
        }

        MPoint lmax = null;
        if (pointlist.get(0).rdiff == 0) {
            //initial bod moze byt lmax
            lmax = pointlist.get(0);
        }
        for (MPoint p:pointlist) {
            if (p.ldiff > 0 && p.rdiff < 0){
                //ciste maximum
                maxlist.add(p);
            } else if (p.ldiff == 0 && p.rdiff < 0){
                //konstanta a klesa
                if (lmax != null){
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

    private int getlrdiff(double a, double b){
        //vrati 0 ak konstanta, 1 ak rastie a -1 ak klesa
        if (Math.abs(a - b) < chybicka) {
            return 0;
        }
        if (a < b) {
            return 1;
        }
        return -1;
    }

    private double color2intensity(Color c)
    {
        return c.getRed() + c.getGreen() + c.getBlue();
    }

    private void calculateIntensity(PixelReader pixelreader, MPoint p) {

        double psum = 0.0;
        double yp = p.y - (lineWidth - 1) / 2.0;
        int counted = 0;
        for (int ny = 0; ny < lineWidth; ny++) {
            double xp = p.x - (lineWidth - 1) / 2.0;
            for (int nx = 0; nx < lineWidth; nx++) {
                int x = (int)(xp + 0.5);
                int y = (int)(yp + 0.5);
                if ((x - p.x) * (x - p.x) + (y - p.y) * (y - p.y) <= (lineWidth * lineWidth / 4.0)) {
                    counted++;
                    Color c = pixelreader.getColor(x, y);
                    psum += color2intensity(c);
                }
                xp += 1.0;
            }
            yp += 1.0;
        }

        p.suc = psum / counted;
    }

    public double zoomFreeCoordinateX(double imageWidth, int x)
    {
        return (x * imageWidth)/(double)maxMouseX;
    }

    public double zoomFreeCoordinateY(double imageHeight, int y)
    {
        return (y * imageHeight)/(double)maxMouseY;
    }

    public boolean colorize(Image image) {
        PixelReader pixelreader = image.getPixelReader();
        int maxImageX = (int) image.getWidth();
        int maxImageY = (int) image.getHeight();

        x1 = zoomFreeCoordinateX(maxImageX, clickLineX1);
        y1 = zoomFreeCoordinateY(maxImageY, clickLineY1);
        x2 = zoomFreeCoordinateX(maxImageX, clickLineX2);
        y2 = zoomFreeCoordinateY(maxImageY, clickLineY2);

        x1Len = zoomFreeCoordinateX(maxImageX, clickLenX1);
        y1Len = zoomFreeCoordinateY(maxImageY, clickLenY1);
        x2Len = zoomFreeCoordinateX(maxImageX, clickLenX2);
        y2Len = zoomFreeCoordinateY(maxImageY, clickLenY2);

        interpolate(x1, y1, x2, y2);

        try {
            int seq = 0;
            for (MPoint p : pointlist) {
                calculateIntensity(pixelreader, p);
                p.seq = seq;
                seq = seq + 1;
                if (seq == 1)
                {
                    ymin = p.suc;
                    ymax = p.suc;
                }
                else if (p.suc < ymin) ymin = p.suc; else if (p.suc > ymax) ymax = p.suc;
            }
            return true;
        } catch (Exception e){
            System.out.println(e);
            return false;
        }
    }

    public void interpolate(double x1, double y1, double x2, double y2) {
        // delta of exact value and rounded value of the dependent variable
        int numberOfSteps = (int)(0.5 + Math.sqrt((double) (x1-x2)*(x1-x2) + (double) (y1-y2)*(y1-y2)));

        pointlist.clear();
        maxlist.clear();

        for (int step = 0; step <= numberOfSteps; step++)
        {
            double x = x1 + step * (x2 - x1) / numberOfSteps;
            double y = y1 + step * (y2 - y1) / numberOfSteps;
            pointlist.add(new MPoint(x, y));
        }
    }

    public void printPointList(){
        System.out.println("\nPRINT POINT LIST - START");
        for(MPoint p:pointlist){
            p.print();
        }
        System.out.println("PRINT POINT LIST - END\n");
    }

    public void printMaxList(){
        System.out.println("\nPRINT MAX LIST - START");
        for(MPoint p:maxlist){
            p.print();
        }
        System.out.println("PRINT MAX LIST - END\n");
    }

}
