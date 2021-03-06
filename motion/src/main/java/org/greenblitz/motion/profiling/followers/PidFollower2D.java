package org.greenblitz.motion.profiling.followers;

import org.greenblitz.debug.RemoteCSVTarget;
import org.greenblitz.motion.base.Vector2D;
import org.greenblitz.motion.pid.CollapsingPIDController;
import org.greenblitz.motion.pid.PIDController;
import org.greenblitz.motion.pid.PIDObject;
import org.greenblitz.motion.profiling.MotionProfile2D;
import org.greenblitz.motion.profiling.kinematics.IConverter;
import org.greenblitz.motion.profiling.kinematics.ReverseLocalizerConverter;

/**
 * To use this, call init before each run.
 *
 * @author alexey
 * @see PidFollower2D#init()
 */
public class PidFollower2D {

    protected long startTime;
    protected double kVl, kAl;
    protected double kVr, kAr;
    protected MotionProfile2D profile;
    protected double PIDLimit;
    protected CollapsingPIDController leftController, rightController;
    protected PIDController angularVelocityController;
    protected double wheelDist;
    protected IConverter converter;

    protected RemoteCSVTarget wheelTarget;
    protected RemoteCSVTarget globalTarget;
    protected RemoteCSVTarget leftOutputTarget;
    protected RemoteCSVTarget rightOutputTarget;
    protected boolean sendData = false;

    /**
     * Use with EXTREME CAUTION. this is used for dynamic motion profiling and is
     * generally not that safe.
     *
     * @param profile
     */
    public void setProfile(MotionProfile2D profile) {
        this.profile = profile;
    }

    /**
     * coef  = coefficient
     * ff    = feed forward
     * vel   = velocity
     * acc   = acceleration
     * ang   = angle\angular
     *
     * @param kVl          coef for linear vel ff
     * @param kAl          coef for linear acc ff
     * @param kVr          coef for angular vel ff
     * @param kAr          coef for angular acc ff
     * @param vals         values for the PID controller on the wheel velocities
     * @param collapseVals The threshold of error at which the I value of the wheel vel PID resets
     * @param pidLimit     The maximum output of the PID controllers (for each one)
     * @param angVals      The coefs for the PID controller on the angular velocity
     * @param angCollapse  The threshold of error at which the I value of the angular vel PID resets
     * @param wheelDist    The distance between the right side of the chassis and the left (meters)
     * @param profile      The motion profile to follow
     */
    public PidFollower2D(double kVl, double kAl, double kVr, double kAr,
                         PIDObject vals, double collapseVals, double pidLimit, PIDObject angVals, double angCollapse, double wheelDist,
                         MotionProfile2D profile) {
        this.kVl = kVl;
        this.kAl = kAl;
        this.kVr = kVr;
        this.kAr = kAr;
        this.profile = profile;
        this.wheelDist = wheelDist;
        leftController = new CollapsingPIDController(vals, collapseVals);
        rightController = new CollapsingPIDController(vals, collapseVals);
        angularVelocityController = new CollapsingPIDController(angVals, angCollapse);
        PIDLimit = pidLimit;
        if (Double.isNaN(kVl + kAl + kVr + kAr + wheelDist + collapseVals + angCollapse)) {
            throw new RuntimeException("Something is NaN");
        }
    }

    public void setConverter(IConverter c) {
        converter = c;
    }

    /**
     * Resets all relevant data, call before every run.
     */
    public void init() {

        if (converter == null) {
            converter = new ReverseLocalizerConverter(wheelDist);
        }

        startTime = System.currentTimeMillis();
        leftController.configure(0, 0, -PIDLimit, PIDLimit, Double.NaN);
        rightController.configure(0, 0, -PIDLimit, PIDLimit, Double.NaN);
        angularVelocityController.configure(0, 0, -PIDLimit, PIDLimit, 0);

        if (sendData) {
            wheelTarget = RemoteCSVTarget.initTarget("WheelData", "time", "DesiredLeft", "ActualLeft",
                    "DesiredRight", "ActualRight");
            globalTarget = RemoteCSVTarget.initTarget("ProfileData", "time", "DesiredLinVel",
                    "ActualLinVel", "DesiredAngVel", "ActualAngVel");
            leftOutputTarget = RemoteCSVTarget.initTarget("LeftPower",
                    "time", "kv", "ka", "pid", "angular pid");
            rightOutputTarget = RemoteCSVTarget.initTarget("RightPower",
                    "time", "kv", "ka", "pid", "angular pid");
        }
    }

    /**
     * For this function, the time is the time since the last call to init().
     *
     * @param left       The left wheel velocity
     * @param right      The right wheel velocity
     * @param angularVel The angular velocity
     * @return A vector of power to each motor in the format (left, right)
     * @see PidFollower2D#init()
     */
    public Vector2D run(double left, double right, double angularVel) {
        return run(left, right, angularVel, System.currentTimeMillis());
    }

    /**
     * @param left       The left wheel velocity
     * @param right      The right wheel velocity
     * @param angularVel The angular velocity
     * @param currTime   The curent time since the start of the profile <b>in seconds</b>
     * @return A vector of power to each motor in the format (left, right)
     */
    public Vector2D run(double left, double right, double angularVel, double currTime) {
        return run(left, right, angularVel, (long) (currTime * 1000.0));
    }

    /**
     * @param leftCurr   The left wheel velocity
     * @param rightCurr  The right wheel velocity
     * @param angularVel The angular velocity
     * @param curTime    The curent time since the start of the profile <b>in miliseconds</b>
     * @return A vector of power to each motor in the format (left, right)
     */
    public Vector2D run(double leftCurr, double rightCurr, double angularVel, long curTime) {
        return forceRun(leftCurr, rightCurr, angularVel, (curTime - startTime) / 1000.0);
    }

    public Vector2D forceRun(double leftCurr, double rightCurr, double angularVel, double timeNow) {
        if (profile.isOver(timeNow)) return new Vector2D(0, 0);

        Vector2D velocity = profile.getVelocity(timeNow);
        Vector2D acceleration = profile.getAcceleration(timeNow);

        angularVelocityController.setGoal(velocity.getY());
        double angularPIDOut = angularVelocityController.calculatePID(angularVel);

        if (Double.isNaN(angularPIDOut)) {
            throw new RuntimeException("Ang PID output is NaN");
        }

        Vector2D velocities = converter.convert(velocity);
        Vector2D accels = converter.convert(acceleration);

        double leftMotorV = velocities.getX();
        double leftMotorA = accels.getX();
        double rightMotorV = velocities.getY();
        double rightMotorA = accels.getY();

        if (Double.isNaN(leftMotorV + leftMotorA + rightMotorA + rightMotorV)) {
            throw new RuntimeException("One of the motor ff vals are NaN");
        }

        if (sendData) {
            wheelTarget.report(timeNow, leftMotorV, leftCurr, rightMotorV, rightCurr);
            globalTarget.report(timeNow, velocity.getX(), (leftCurr + rightCurr) / 2.0, velocity.getY(),
                    (leftCurr - rightCurr) / wheelDist);
        }

        leftController.setGoal(leftMotorV);
        rightController.setGoal(rightMotorV);

        double leftPID = leftController.calculatePID(leftCurr);
        double rightPID = rightController.calculatePID(rightCurr);

        if (Double.isNaN(leftPID + rightPID)) {
            throw new RuntimeException("LeftPID or RightPID are NaN");
        }

        if (sendData) {

            leftOutputTarget.report(timeNow, leftMotorV * kVl, leftMotorA * kAl,
                    leftPID, angularPIDOut);
            rightOutputTarget.report(timeNow, rightMotorV * kVr, rightMotorA * kAr,
                    rightPID, -angularPIDOut);

        }

        return new Vector2D(leftMotorV * kVl + leftMotorA * kAl + leftPID + angularPIDOut,
                rightMotorV * kVr + rightMotorA * kAr + rightPID - angularPIDOut);
    }

    /**
     * @return true if the profile finished running, false otherwise
     */
    public boolean isFinished() {
        return profile.isOver((System.currentTimeMillis() - startTime) / 1000.0);
    }

    /**
     * If this is true, data will be sent to CSVLogger about the profile following performance. If this is false
     * no data will be sent. By default, this is false.
     * <p>
     * NOTE: Don't call this function after calling init()!
     *
     * @param val whether to send data or not
     */
    public void setSendData(boolean val) {
        sendData = val;
    }

}