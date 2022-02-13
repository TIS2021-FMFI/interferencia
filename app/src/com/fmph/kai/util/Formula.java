package com.fmph.kai.util;
import java.lang.*;

public class Formula {

    /** the required precision of the iterative search for resulting value, algorithm parameter */
    private double precision = 0.000001;

    /** minimum possible radius of sample curvature, algorithm parameter */
    public static final double LOWER_BOUND_FOR_R = 200;
    /** maximum possible radius of sample curvature, algorithm parameter */
    public static final double UPPER_BOUND_FOR_R = 25000;
    /** distance between sample to screen, algorithm parameter */
    public double d = 100;
    /** wave length of the laser, algorithm parameter */
    public double lambda = 0.000650;

    public Formula()
    {
       //TODO: read config of algorithm parameters?
    }

    public double getR_f(double r, double x1, double x2) {
        double res;
        double delta1 = (r - Math.sqrt(r * r - (1 - d / (d + r / 2)*(1 - d / (d + r / 2)*x1*x1)))) *
                (1 + ( (x1 * x1 + (d + r / 2)*(d + r / 2))) / (d + r / 2)) +
                ( d * Math.sqrt(x1 * x1 + (d + r / 2) * (d + r / 2))) / (d + r / 2);

        double delta2 = (r - Math.sqrt(r * r - (1 - d / (d + r / 2)*(1 - d / (d + r / 2)*x2*x2)))) *
                (1 + ( (x2 * x2 + (d + r / 2)*(d + r / 2))) / (d + r / 2)) +
                ( d * Math.sqrt(x2 * x2 + (d + r / 2) * (d + r / 2))) / (d + r / 2);

        res = delta2-delta1-lambda;
        return res;
    }

    /** calculates sample curvature radius for specified interference maxima
     * @param x1 position of the first maximum in the 1D coordinates on the selected line
     * @param x2 position of the second maximum in the 1D coordinates on the selected line
     * @return estimation of the sample curvature radius
     */
    public double finalR(double x1, double x2) {

            double left = LOWER_BOUND_FOR_R;
            double right = UPPER_BOUND_FOR_R;

            double lF = getR_f(left, x1, x2);
            double rF = getR_f(right, x1, x2);

            long iterations = 0;

            while (right - left > precision)
            {
                double middle = (left + right) / 2;
                double mF = getR_f(middle, x1, x2);
                if (lF * mF <= 0)
                {
                    right = middle;
                    rF = mF;
                }
                else
                {
                    left = middle;
                    lF = mF;
                }
                iterations++;
            }

           // System.out.println("D = "+ d + "R = " + (right + left) / 2);
        return (right + left) / 2;
    }
}
