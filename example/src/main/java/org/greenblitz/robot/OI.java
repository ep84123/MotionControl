package org.greenblitz.robot;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;

import edu.wpi.first.wpilibj.command.CommandGroup;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.greenblitz.motion.app.AdaptivePolynomialPursuitController;
import org.greenblitz.motion.app.AdaptivePurePursuitController;
import org.greenblitz.motion.base.Point;
import org.greenblitz.motion.base.Position;
import org.greenblitz.motion.pathing.BasicAngleInterpolator;
import org.greenblitz.motion.pathing.Path;
import org.greenblitz.motion.pathing.PolynomialInterpolator;
import org.greenblitz.robot.commands.*;
import org.greenblitz.robot.commands.shifter.SwitchShift;
import org.greenblitz.robot.commands.vision.DriveToVisionTarget;
import org.greenblitz.utils.SmartJoystick;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class OI {

    private static OI instance;

    private NetworkTable visionTable;

    private SmartJoystick mainJS;

    public static OI getInstance() {
        if (instance == null) init();
        return instance;
    }

    public static void init() {
        instance = new OI();
    }

    private OI() {
        mainJS = new SmartJoystick(org.greenblitz.robot.RobotMap.JoystickID.MAIN);
        mainJS.setAxisInverted(SmartJoystick.JoystickAxis.LEFT_Y, true);
        mainJS.setAxisInverted(SmartJoystick.JoystickAxis.RIGHT_Y, true);
        mainJS.B.whileHeld(new DriveToVisionTarget());
        mainJS.Y.whenPressed(new MotionAndVision());
        mainJS.A.whenPressed(new APPCTestingCommand(
                new AdaptivePurePursuitController(
                        new Path<>(
                                getPath("Test Path.pf1.csv")),
                        0.5, RobotStats.Ragnarok.WHEELBASE,
                        0.1, false, 0.3, 0.6, 1)
                , new Position(3.073, 1.5)));
        mainJS.X.whenPressed(new ArcadeDriveByJoystick(mainJS));
        mainJS.R1.whenPressed(new TankDriveByJoystick(mainJS));
        mainJS.L1.whenPressed(new SwitchShift());
        visionTable = NetworkTableInstance.getDefault().getTable("vision");
    }

    public SmartJoystick getMainJS() {
        return mainJS;
    }

    public NetworkTable getVisionTable() {
        return visionTable;
    }

    public double getHatchDistance() {
        return visionTable.getEntry("hatch::distance").getDouble(0);
    }

    public double getHatchAngle() {
        return visionTable.getEntry("hatch::ang").getDouble(0);
    }

    private Position[] getPath(String filename) {
        CSVParser read;
            try {
                read = CSVFormat.DEFAULT.parse(new FileReader(new File("/home/lvuser/deploy/output/" + filename)));
                ArrayList<Position> path = new ArrayList<>();
                List<CSVRecord> records = read.getRecords();
                for (int i = 1; i < records.size() ; i++) {
                    path.add(new Position(new Point(Double.parseDouble(records.get(i).get(2)), Double.parseDouble(records.get(i).get(1)))));
                }
                System.out.println(path);
                return path.toArray(new Position[path.size()]);
            } catch (Exception e) { e.printStackTrace(); }
        System.out.println("Failed to read file");
        return new Position[0];
    } 
}