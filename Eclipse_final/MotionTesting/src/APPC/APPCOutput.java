package APPC;

import org.usfirst.frc.team4590.robot.RobotStats;

import base.Output;
import edu.wpi.first.wpilibj.RobotDrive;

public class APPCOutput implements Output<Double[]> {
    private RobotDrive m_robotDrive;
    
    private static double safteyFactor = 0.5;
    private static double fullPower = 0.8;
    
    public APPCOutput(RobotDrive robotDrive){
        m_robotDrive = robotDrive;
    }

    
    public void curveDrive(RobotDrive r,double power,double curve){
    	if(curve == 0){
    		r.tankDrive(power, power);
    		return;
    	}
    	
    	double d = RobotStats.RightLeftWheelDistance;
    	double R = 1 / Math.abs(curve);
        double ratio;
        if (R - d / 2 == 0)
        	ratio = 0;
        else
        	ratio = (R + d / 2) / (R - d / 2);    
    	if(curve > 0)
    		r.tankDrive(power, power*ratio);		// left faster
    	else
    		r.tankDrive(power*ratio, power);  // right faster
    }

    /**
     * 
     * @param output the output to use on the engines. output[0]- power, output[1]- curve
     */
    @Override
    public void use(Double[] output) {
    	  
    	  curveDrive(m_robotDrive,output[1]*fullPower*safteyFactor,output[2]);
    	  System.out.printf("APPCOutput active: power = %f, curve = %f", output[0], output[1]);
    }
    
    public void tankDrive(double left, double right) {
    	m_robotDrive.tankDrive(left, right);
    }
    
    public void arcadeDrive(double magnitude, double curve) {
    	m_robotDrive.arcadeDrive(magnitude, curve);
    }
}
