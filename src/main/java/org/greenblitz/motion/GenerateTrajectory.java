package org.greenblitz.motion;

import jaci.pathfinder.Pathfinder;
import jaci.pathfinder.Trajectory;
import jaci.pathfinder.Waypoint;
import org.greenblitz.robot.RobotStats;

public class GenerateTrajectory {

    private static void unsafeGenerateTrajectory(Waypoint[] waypoints, Trajectory.FitMethod fit, int samples, double dt, Trajectory[] out){
        Trajectory.Config config = new Trajectory.Config(fit, samples, dt, RobotStats.Picasso.Chassis.MAX_VELOCITY,
                RobotStats.Picasso.Chassis.MAX_ACCELERATION,
                RobotStats.Picasso.Chassis.MAX_JERK);
        out[0] = Pathfinder.generate(waypoints, config);
    }

    public static Trajectory generateTrajectory(Waypoint[] waypoints, Trajectory.FitMethod fit, int samples, double dt)
    throws Exception{
        Trajectory[] ret = new Trajectory[1];
        ret[0] = null;

        Runnable toRun = () -> unsafeGenerateTrajectory(waypoints, fit, samples, dt, ret);

        Thread thread = new Thread(toRun);
        thread.start();
        long currentTime = System.currentTimeMillis();

        while (ret[0] == null){
            if (System.currentTimeMillis() - currentTime > 3*1000){
                break;
            }
        }

        if (ret[0] == null)
            throw new Exception("generator in infinite loop");

        return ret[0];
    }

    public static Trajectory generateTrajectory(Waypoint[] waypoints, int samples, double dt) throws Exception{
        return generateTrajectory(waypoints, Trajectory.FitMethod.HERMITE_CUBIC, samples, dt);
    }

    public static Trajectory generateTrajectory(Waypoint[] waypoints, double dt) throws Exception{
        return generateTrajectory(waypoints, Trajectory.Config.SAMPLES_HIGH, dt);
    }

    public static Trajectory generateTrajectory(Waypoint[] waypoints) throws Exception{
        return generateTrajectory(waypoints, 0.05);
    }

}
