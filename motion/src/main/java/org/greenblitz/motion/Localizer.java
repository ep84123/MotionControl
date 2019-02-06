package org.greenblitz.motion;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import org.greenblitz.debug.RemoteCSVTarget;
import org.greenblitz.motion.base.Point;
import org.greenblitz.motion.base.Position;
import org.greenblitz.motion.base.Vector2D;

/**
 * runs in a seperate thread calculating the org.greenblitz.example.robot position
 *
 * @author Udi    ~ MudiAtalon
 * @author Alexey ~ savioor
 */
public class Localizer {

    private Position m_location = new Position(0, 0, 0); // Positive x direction is left

    private double m_wheelDistance;

    private double angle0;
    private double prevDistanceLeft;
    private double prevDistanceRight;
    private double zeroDistanceLeft, zeroDistanceRight;
    private long wakeTime;
    private boolean awake;

    private final Object LOCK = new Object();
    private final Object SLEEP_LOCK = new Object();

    private final RemoteCSVTarget m_logger;
    private final long timeOffset;

    private Localizer() {
        RemoteCSVTarget.initTarget("location", "x", "y");
        m_logger = RemoteCSVTarget.getTarget("location");
        timeOffset = System.currentTimeMillis();
        angle0 = 0;
        awake = false;
        wakeTime = System.currentTimeMillis();
    }

    private static final Localizer instance = new Localizer();

    public static Localizer getInstance(){
        return instance;
    }

    /**
     * <p>Get the org.greenblitz.example.robot location. This is the system: </p>
     * ^<br>
     * |<br>
     * |<br>
     * <--- R
     * <br> <br> Where 'R' is the org.greenblitz.example.robot, up is the y coord and left is the x coord
     *
     * @return
     */
    public Position getLocation() {
        synchronized (LOCK) {
            return m_location.clone();
        }
    }

    /**
     * sets initial values of Localizer, functions as constructor.
     *
     * @param leftTicks
     * @param rightTicks
     */
    public void configure(double wheelDistance, int leftTicks, int rightTicks) {
        m_wheelDistance = wheelDistance;
        reset(leftTicks, rightTicks);
    }

    /**
     * Reset prevDistanceLeft and prevDistanceRight.
     * You want to call this when reseting encoders for example
     */
    public void reset(double currentLeftDistance, double currentRightDistance, Position newPos) {
        prevDistanceLeft = currentLeftDistance;
        prevDistanceRight = currentRightDistance;
        zeroDistanceLeft = currentLeftDistance;
        zeroDistanceRight = currentRightDistance;
        angle0 = newPos.getAngle();
        m_location = newPos.clone();
    }

    /**
     * Location is the same, encoder values are reset
     * @param currLeft
     * @param currRight
     */
    public void resetEncoderes(double currLeft, double currRight){
        reset(currLeft, currRight, getLocation());
    }

    /**
     * Reset prevDistanceLeft and prevDistanceRight.
     * You want to call this when reseting encoders for example
     */
    public void reset(double currentLeftDistance, double currentRightDistance) {
        reset(currentLeftDistance, currentRightDistance, new Position(0, 0, 0));
    }

    /**
     * calculateMovement the location
     *
     * @param rightDist distance right wheel traveled
     * @param leftDist  distance left wheel traveled
     * @param robotAng  the angle of the robot
     * @return x difference, y difference, angle difference
     */
    public static Point calculateMovement(double rightDist, double leftDist, double wheelDistance, double robotAng) {
        if (rightDist == leftDist) {
            return new Point(0, rightDist).rotate(robotAng);
        }

        double distance = (rightDist + leftDist) / 2;
        double angle = (rightDist - leftDist) / wheelDistance;
        double circleRadius = distance / angle;

        double dy = circleRadius * Math.sin(angle);
        double dx = circleRadius * (1 - Math.cos(angle));
        return new Point(dx, dy).rotate(robotAng);
    }

    double sleepSpeedL;
    double sleepSpeedR;
    double sleepTime;
    public void setSleep(long milis, double leftSpeed, double rightSpeed){
        synchronized (SLEEP_LOCK) {
            awake = false;
            wakeTime = System.currentTimeMillis() + milis;
        }
        sleepTime = milis / 1000.0;
        sleepSpeedL = leftSpeed;
        sleepSpeedR = rightSpeed;
    }

    public void wakeUp(){
        synchronized (SLEEP_LOCK) {
            wakeTime = System.currentTimeMillis();
        }
    }

    public void update(double currentLeftDistance, double currentRightDistance) {
        synchronized (SLEEP_LOCK) {
            double dt = (System.currentTimeMillis() - wakeTime) / 1000.0;
            if (dt < 0) return;
            else if (!awake) {
                dt += sleepTime;
                sleepTime = 0;
                awake = true;
                resetEncoderes(currentLeftDistance, currentRightDistance);

                Point keepUp = calculateMovement(
                        sleepSpeedR*dt,
                        sleepSpeedL*dt,
                        m_wheelDistance,
                        m_location.getAngle()
                );
                synchronized (LOCK) {
                    m_location.translate(keepUp);
                    m_location.setAngle(angle0 + (sleepSpeedR - sleepSpeedL) * dt / m_wheelDistance);
                }
                angle0 = m_location.getAngle();

                return;

            }
        }
        Point dXdY = calculateMovement(
                currentRightDistance - prevDistanceRight, currentLeftDistance - prevDistanceLeft,
                m_wheelDistance, m_location.getAngle());

        synchronized (LOCK) {
            m_location.translate(dXdY);
            m_location.setAngle(angle0 + ((currentRightDistance - zeroDistanceRight
                    - currentLeftDistance + zeroDistanceLeft) / m_wheelDistance));
        }

        m_logger.report(m_location.getX(), m_location.getY());

        prevDistanceLeft = currentLeftDistance;
        prevDistanceRight = currentRightDistance;
    }

    @Deprecated
    public void forceSetLocation(Position location, double currentLeftDistance, double currentRightDistance) {
        reset(currentLeftDistance, currentRightDistance, location);
    }
}