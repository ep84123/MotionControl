package org.greenblitz.example.robot.subsystems;

import com.ctre.phoenix.motorcontrol.NeutralMode;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import org.greenblitz.example.robot.LocalizerRunner;
import org.greenblitz.example.robot.OI;
import org.greenblitz.example.robot.RobotMap;
import org.greenblitz.example.robot.RobotStats;
import org.greenblitz.example.robot.commands.ArcadeDriveByJoystick;
import org.greenblitz.example.utils.CANRobotDrive;
import org.greenblitz.example.utils.SmartEncoder;
import org.greenblitz.motion.base.Position;
import org.greenblitz.motion.pathfinder.PathFollower;

public class Chassis extends Subsystem {
    private NetworkTableEntry updateEntry = NetworkTableInstance.getDefault().getTable("motion").getEntry("isUpdated");
    private NetworkTableEntry xEntry = NetworkTableInstance.getDefault().getTable("motion").getSubTable("localizer").getEntry("x");
    private NetworkTableEntry yEntry = NetworkTableInstance.getDefault().getTable("motion").getSubTable("localizer").getEntry("y");
    private NetworkTableEntry headingEntry = NetworkTableInstance.getDefault().getTable("motion").getSubTable("localizer").getEntry("heading");

    private static final double POWER_LIMIT = 1.0;

    private static Chassis instance;

    private static final double TICKS_PER_METER_LEFT = RobotStats.Picasso.EncoderMetreScale.LEFT_POWER;
    private static final double TICKS_PER_METER_RIGHT = RobotStats.Picasso.EncoderMetreScale.RIGHT_POWER;

    private LocalizerRunner m_localizer;

    private SmartEncoder m_leftEncoder, m_rightEncoder;

    public SmartEncoder getLeftEncoder() {
        return m_leftEncoder;
    }

    public SmartEncoder getRightEncoder() {
        return m_rightEncoder;
    }

    private CANRobotDrive m_robotDrive;

    private boolean isCoast = true;

    public static Chassis getInstance() {
        if (instance == null) init();
        return instance;
    }

    public static void init() {
        instance = new Chassis();
    }

    private Chassis() {
        m_robotDrive = new CANRobotDrive(RobotMap.ChassisPort.FRONT_LEFT, RobotMap.ChassisPort.REAR_LEFT,
                                         RobotMap.ChassisPort.FRONT_RIGHT, RobotMap.ChassisPort.REAR_RIGHT);

        m_robotDrive.invert(CANRobotDrive.TalonID.FRONT_RIGHT);
        m_robotDrive.invert(CANRobotDrive.TalonID.REAR_RIGHT);

        m_leftEncoder = new SmartEncoder(m_robotDrive.getTalon(CANRobotDrive.TalonID.REAR_LEFT), TICKS_PER_METER_LEFT);
        m_rightEncoder = new SmartEncoder(m_robotDrive.getTalon(CANRobotDrive.TalonID.REAR_RIGHT), TICKS_PER_METER_RIGHT);
        m_rightEncoder.invert();
        m_leftEncoder.reset();
        m_rightEncoder.reset();

        m_localizer = new LocalizerRunner(getWheelbaseWidth(), getLeftEncoder(), getRightEncoder());
        m_localizer.start();
    }

    public void initDefaultCommand() {
        setDefaultCommand(new ArcadeDriveByJoystick(OI.getInstance().getMainJS()));
    }

    public void update() {
        SmartDashboard.putString("Chassis current command", getCurrentCommandName());
        SmartDashboard.putNumber("Chassis Distance", getDistance());
        SmartDashboard.putNumber("Chassis left ticks", getLeftTicks());
        SmartDashboard.putNumber("Chassis right ticks", getRightTicks());
        Position pos = m_localizer.getLocation();
        SmartDashboard.putNumber("robot x", pos.getX());
        SmartDashboard.putNumber("robot y", pos.getY());
        SmartDashboard.putNumber("robot angle", Math.toDegrees(pos.getAngle()));
        xEntry.setNumber(pos.getX());
        yEntry.setNumber(pos.getY());
        headingEntry.setNumber(pos.getAngle());
        updateEntry.setBoolean(true);
    }

    public void arcadeDrive(double moveValue, double rotateValue) {
        if (Math.abs(moveValue) > POWER_LIMIT)
            moveValue = Math.signum(moveValue) * POWER_LIMIT;
        m_robotDrive.arcadeDrive(-moveValue, rotateValue);
    }

    public void tankDrive(double leftValue, double rightValue) {
        m_robotDrive.tankDrive(-leftValue, -rightValue);
    }

    public void stop() {
        tankDrive(0, 0);
    }

    public void setBrake(){
        if(!isCoast)
            return;
        isCoast = false;
        m_robotDrive.getTalon(CANRobotDrive.TalonID.FRONT_LEFT).setNeutralMode(NeutralMode.Brake);
        m_robotDrive.getTalon(CANRobotDrive.TalonID.FRONT_RIGHT).setNeutralMode(NeutralMode.Brake);
        m_robotDrive.getTalon(CANRobotDrive.TalonID.REAR_RIGHT).setNeutralMode(NeutralMode.Brake);
        m_robotDrive.getTalon(CANRobotDrive.TalonID.REAR_LEFT).setNeutralMode(NeutralMode.Brake);
    }

    public void setCoast(){
        if(isCoast)
            return;
        isCoast = true;
        m_robotDrive.getTalon(CANRobotDrive.TalonID.FRONT_LEFT).setNeutralMode(NeutralMode.Coast);
        m_robotDrive.getTalon(CANRobotDrive.TalonID.FRONT_RIGHT).setNeutralMode(NeutralMode.Coast);
        m_robotDrive.getTalon(CANRobotDrive.TalonID.REAR_RIGHT).setNeutralMode(NeutralMode.Coast);
        m_robotDrive.getTalon(CANRobotDrive.TalonID.REAR_LEFT).setNeutralMode(NeutralMode.Coast);
    }

    public double getDistance() {
        return m_leftEncoder.getDistance() / 2 + m_rightEncoder.getDistance() / 2;
    }

    public double getSpeed() {
        return (m_leftEncoder.getSpeed() + m_rightEncoder.getSpeed()) / 2;
    }

    public double getLeftDistance() {
        return m_leftEncoder.getDistance();
    }

    public double getRightDistance() {
        return m_rightEncoder.getDistance();
    }

    public int getLeftTicks() {
        return m_leftEncoder.getTicks();
    }

    public int getRightTicks() {
        return m_rightEncoder.getTicks();
    }

    public double getLeftSpeed() {
        return m_leftEncoder.getSpeed();
    }

    public double getRightSpeed() {
        return m_rightEncoder.getSpeed();
    }

    public void resetSensors() {
        resetEncoders();
        m_localizer.reset();
    }

    public void resetLeftEncoder() {
        m_leftEncoder.reset();
    }

    public void resetRightEncoder() {
        m_rightEncoder.reset();
    }

    public void resetEncoders() {
        resetLeftEncoder();
        resetRightEncoder();
    }

    public void forceEncodersReset() {
        do {
            resetLeftEncoder();
            resetRightEncoder();
        } while(m_leftEncoder.getTicks() != 0 || m_rightEncoder.getTicks() != 0);
    }

    public double getWheelRadius() {
        return RobotStats.Picasso.Chassis.WHEEL_RADIUS;
    }

    public double getWheelbaseWidth() {
        return RobotStats.Picasso.Chassis.VERTICAL_DISTANCE;
    }

    public Position getLocation() {
        return m_localizer.getLocation();
    }
}