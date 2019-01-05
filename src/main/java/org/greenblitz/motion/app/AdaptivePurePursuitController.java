package org.greenblitz.motion.app;

import org.greenblitz.motion.base.Point;
import org.greenblitz.motion.base.Position;

public class AdaptivePurePursuitController {
    private Path m_path;

    public final double m_lookAhead;
    private final double m_wheelBase;

    public AdaptivePurePursuitController(Path path, double lookAhead, double wheelBase) {
        m_path = path;
        m_lookAhead = lookAhead;
        m_wheelBase = wheelBase;
    }

    public static double[] driveValuesTo(Position robotLoc, Point target, double wheelDist) {
        Point diff = Point.sub(target, robotLoc).rotate(-robotLoc.getAngle());
        double curvature = 2 * diff.getX() / Point.normSquared(diff);
        if (curvature == 0)
            return new double[]{1, 1};
        double radius = 1/curvature;
        double rightRadius = radius + wheelDist/2;
        double leftRadius = radius - wheelDist/2;
        if(curvature > 0)
            return new double[]{leftRadius/rightRadius, 1};
        else
            return new double[]{1, rightRadius/leftRadius};
    }

    public double[] iteration(Position robotLoc){
        return driveValuesTo(robotLoc, m_path.intersection(robotLoc, m_lookAhead), m_wheelBase);
    }
}