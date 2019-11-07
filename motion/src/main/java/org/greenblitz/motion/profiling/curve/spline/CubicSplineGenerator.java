package org.greenblitz.motion.profiling.curve.spline;

import org.greenblitz.motion.base.Point;
import org.greenblitz.motion.base.State;

import java.awt.*;

/**
 * @author alexey
 */
public class CubicSplineGenerator {

    /**
     * @param start
     * @param end
     * @return
     */
    public static ThirdDegreePolynomialCurve generateSpline(State start, State end){
        return generateSpline(start, end, 1);
    }

    /**
     *
     * @param start
     * @param end
     * @param t
     * @return
     */
    public static ThirdDegreePolynomialCurve generateSpline(State start, State end, double t){
        double angS = start.getAngle();
        double angE = end.getAngle();
        ThirdDegreePolynomialCurve ret = new ThirdDegreePolynomialCurve(
                getParams(start.getX(), end.getX(), Math.sin(angS), Math.sin(angE), t),
                getParams(start.getY(), end.getY(), Math.cos(angS), Math.cos(angE), t), 0, 1, t
        );
//        if(Point.subtract(ret.getLocation(1), end).norm() > 0.01){
//            throw new RuntimeException("What");
//        }
        return ret;
    }

    /**
     * See
     * https://matrixcalc.org/en/slu.html#solve-using-Gaussian-elimination%28%7B%7B0,0,0,1,0%7D,%7B1,1,1,1,x%7D,%7B0,0,1,0,d_0%7D,%7B3,2,1,0,d_1%7D%7D%29
     * or this
     * https://matrixcalc.org/en/slu.html#solve-using-Gaussian-elimination%28%7B%7B0,0,0,1,x_0%7D,%7B1,1,1,1,x_1%7D,%7B0,0,1,0,d_0%7D,%7B3,2,1,0,d_1%7D%7D%29
     * or this
     * https://matrixcalc.org/en/slu.html#solve-using-Gaussian-elimination%28%7B%7B0,0,0,1,x_0%7D,%7Bt%5E3,t%5E2,t,1,x_1%7D,%7B0,0,1,0,d_0%7D,%7B3%2At%5E2,2%2At,1,0,d_1%7D%7D%29
     *
     * (the lower the equation, the more recent)
     *
     * @param dervStart
     * @param dervEnd
     * @return
     */
    protected static double[] getParams(double dStart, double dEnd, double dervStart, double dervEnd, double t){
        double dx = dEnd - dStart;
        double tt = t*t;
        return new double[] {
                dStart, dervStart, (-2*dervStart*t - dervEnd*t + 3*dx)/tt, (dervStart*t + dervEnd*t - 2*dx)/(t*tt)
        };
    }

}
