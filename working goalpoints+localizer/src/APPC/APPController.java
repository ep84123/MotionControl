package APPC;


import base.Input;
import base.IterativeController;
import base.Output;
import edu.wpi.first.wpilibj.DriverStation;



public class APPController extends IterativeController<Point2D, Double[]> {
    protected static final double DEFAULT_LOOKAHEAD = 0.3;
    protected static final double DEFAULT_EPSILON = 0.005;
    protected static final double DEFAULT_TOLERANCEDIST = 0.01;
    protected static final double DEFAULT_MINONTARGETTIME = 10;
    protected static final double DEFAULT_SLOWDOWN = 0.1;
    protected static final double DEFAULT_LB = 0.49;
    
    protected static final int LOOKBACK_DISTANCE = 5;

    /**
     * The most recent robot location calc
     */
    private Point2D m_robotLoc;
    /**
     * the path the controller is following
     */
    private Path m_path;
    /**
     * Look ahead value
     */
    private double m_lookAhead;
    /**
     * tolerance constant when searching for the goal point
     */
    private double m_epsilon;
    /**
     * The point we are trying to reach in robot coordinates
     */
    private Point2D m_goalPointR;
    /**
     * The length of the robot form middle of back wheels to middle of front wheels, in m
     */
    public static double Lb = -1;

    /**
     *used in update() to know if this search is the first
     */
    private boolean firstSearch;

    /**
     * starts slowing down when the distance to the end of path is shorter than this
     */
    private double m_slowDownDistance;



    public static void setLb(double newVal){
        if (Lb != -1){
             throw new RuntimeException("Lb already set (wtf why your robot changes size)");
        }
        if (newVal <= 0){
            throw new RuntimeException(String.format("Lb must be positive but it was set to %d", newVal));
        }
        if (newVal > 1 || newVal < 0.4){
            DriverStation.reportWarning(String.format("Lb was set to %d, isn't that a little to big/small?", newVal), false);
        }
        Lb = newVal;
    }

    /**
     * 
     * @param in The input object
     * @param out The motor manager object
     * @param path The path the robot will follow
     * @param lookAhead Look Ahead distance
     * @param epsilon Margin of search for goal point on path 
     * @param toleranceDist Absolute tolerance distance
     * @param minOnTargetTime Minimal time on target required for the controller
     * @param slowDownDistance Distance from path end point in which the robot will slow down
     * @param Lb Distance between middle of the front and rear wheels
     */
    

    
    public APPController(Input<Point2D> in, Output<Double[]> out,Path path){
    	this(in,out,DEFAULT_PERIOD,path,DEFAULT_LOOKAHEAD,DEFAULT_EPSILON,DEFAULT_TOLERANCEDIST,DEFAULT_MINONTARGETTIME,DEFAULT_SLOWDOWN,DEFAULT_LB);
    }
    
    
    public APPController(Input<Point2D> in, Output<Double[]> out,Path path, double lookAhead,double epsilon,double toleranceDist,double minOnTargetTime,double slowDownDistance, double Lb) {
        this(in,out,DEFAULT_PERIOD,path,lookAhead,epsilon,toleranceDist,minOnTargetTime,slowDownDistance, Lb);
    }
    
    /**
     * 
     * @param in The input object
     * @param out The motor manager object
     * @param period The time period of calling the controller calculation
     * @param path The path the robot will follow
     * @param lookAhead Look Ahead distance
     * @param epsilon Margin of search for goal point on path 
     * @param toleranceDist Absolute tolerance distance
     * @param minOnTargetTime Minimal time on target required for the controller
     * @param slowDownDistance Distance from path end point in which the robot will slow down
     * @param Lb Distance between middle of the front and rear wheels
     */
    public APPController(Input<Point2D> in, Output<Double[]> out,double period,Path path, double lookAhead,double epsilon,double toleranceDist,double minOnTargetTime,double slowDownDistance, double Lb) {
        super(in,out,period);
        m_robotLoc = in.recieve();
        m_path = path;
        //@TODO HOLY FUCKING SHIT WHY DID YOU MAKE IT CRASH - IT INITIALIZES LB EVERY AUTO RUN PLZ FIX NOW - Guyde <3
        if (APPController.Lb == -1)
        	setLb(Lb);
        
        m_lookAhead = lookAhead;
        m_epsilon = epsilon > 0 ? epsilon : DEFAULT_EPSILON;
        m_destination = path.getLast();
        m_tolerance = new AbsoluteTolerance(toleranceDist,minOnTargetTime);
        m_slowDownDistance = slowDownDistance;
        firstSearch = true;
        m_goalPointR = null;
        if(epsilon <= 0)
            DriverStation.reportWarning(String.format("epsilon should be a positive number and has been replaced with a default value"), false);
    }


    /**
     * Calculates the robot position using information
     * from encoders, gyro and accelerometer. then sets it
     * If we will have time we need to implement Kalman Filter
     */
    public void updateRobotLocation(){
        m_robotLoc = m_input.recieve();
    	//System.out.print("moving from-"+m_robotLoc);
    	//m_robotLoc = m_robotLoc.moveBy(0,0.02);
    	System.out.println(" cur robot loc: "+m_robotLoc);
    }

    /**
     * Calculate the goal point we are trying to reach with path & lookahead
     */
    private void updateGoalPoint(){
        Point2D checkPoint;
        double min_distance = 0; //not relevant, set in order to not recieve an error
        boolean foundPoint = false;
        double distance;
        int goalPointIndex = 0; //not relevant, set in order to not recieve an error
        int i = 0;

        while(m_path.hasNext())
        {
        	//System.out.println(m_path.getCurrentIndex());
            checkPoint = m_path.get();
            distance = Math.abs(m_robotLoc.distance(checkPoint) - m_lookAhead);
            //System.out.println("dist between r:"+m_robotLoc+" c:"+checkPoint+"    is:"+distance);
            
            if(foundPoint) //once a point is found searches nearby points for a better point
            {
                if (distance<m_epsilon){
                	//System.out.println("another "+distance +" min is: "+min_distance +"THE POINT IS: "+checkPoint);
                    if(distance < min_distance){
                        min_distance = distance;
                        m_goalPointR = checkPoint;
                        goalPointIndex = m_path.getCurrentIndex();
                    }
                } else if (firstSearch) //preforms a global search on the first search
                    foundPoint = false;
                else {
                    m_path.setCurrentIndex(Math.max(goalPointIndex - LOOKBACK_DISTANCE,0));
                	//m_path.setCurrentIndex(Math.min(goalPointIndex - LOOKBACK_DISTANCE);
                    //System.out.println("robot: "+m_robotLoc);
                    //System.out.println("found a goalpoint!:: "+m_goalPointR);
                    return; //returns once the local search finishes
                }
            } else {
                if (distance<m_epsilon){
                	//System.out.println("found first dist "+distance);
                    foundPoint = true;
                    min_distance = distance;
                    m_goalPointR = checkPoint;
                    goalPointIndex = m_path.getCurrentIndex();
                }
            }
        }
        if(firstSearch){
            if(m_goalPointR == null){
                m_goalPointR = m_path.closestPointTo(m_robotLoc);
                //System.out.println("no goal");
            }
            firstSearch = false;
            m_path.setCurrentIndex(Math.max(goalPointIndex - LOOKBACK_DISTANCE,0));
            //System.out.println("in first search->");
        } else
            m_goalPointR = m_path.getLast();
        //System.out.println("None Found "+m_goalPointR);
    }

    /**
     * recieve the motor power ratio (leftMotor : rightMotor) we need to use
     * <p>https://pdfs.semanticscholar.org/82aa/c3a57f1941d11f13e6eb53e136bdea23894b.pdf</p>
     * @return the angular speed
     */
    public double getMotorRatio(){
        if (Lb == -1){
            throw new RuntimeException("Lb wasn't set prior to run");
        }
        // equation from:
        // https://pdfs.semanticscholar.org/82aa/c3a57f1941d11f13e6eb53e136bdea23894b.pdf
        // page 87
        //Drive
        double R = Math.pow(m_lookAhead, 2) / (2 * Math.abs(m_goalPointR.getX()));
        if (m_goalPointR.getX() > 0){
        	if (R - 0.5*Lb == 0){
        		//TODO
        	}
        	return (R - 0.5*Lb) / (R + 0.5*Lb);
        }
        if (R - 0.5*Lb == 0){
        	//TODO
        }
        return (R + 0.5*Lb) / (R - 0.5*Lb) ;
    }

    public double getCurve(){
    	Point2D goalVector = m_goalPointR.changePrespectiveTo(m_robotLoc);
    	return (2 * Math.abs(goalVector.getX())) / Math.pow(goalVector.length(), 2);
    }

    
    //TODO: fix all the constructors to call this and not super
    /*
    public APPController(Input<Point2D> in, Output<Double[]> out, Point2D destination) {
        super(in, out, destination);
    }

    public APPController(Output<Double[]> out, Point2D destination) {
        super(out, destination);
    }

    public APPController(Input<Point2D> in, Output<Double[]> out) {
        super(in, out);
    }

    public APPController(Output<Double[]> out) {
        super(out);
    }

    public APPController(Input<Point2D> in, Output<Double[]> out, double absoluteTolerance) {
        super(in, out);
        //setTolerance(new AbsoluteTolerance(absoluteTolerance));
    }
	*/

	@Override
    public void calculate() {
        if(m_tolerance.onTarget()){
        	System.out.println("STOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOP");
            m_output.stop();
        }
        else{
            updateRobotLocation();
            updateGoalPoint();
            m_output.use(new Double[]{getPowerPrecent(),getCurve()});
        }
    }

    @Override
    public void initParameters() {
    	/*
        try {
            m_parameters.put("Look-ahead distance", new Parameter<>("m_lookAhead", this));
        }
        catch(NoSuchFieldException e) {}
        */
    }


    public class AbsoluteTolerance extends TimedTolerance{

        double m_toleranceDist;

        public AbsoluteTolerance(double toleranceDist,double minTime){
            super(minTime);
            m_toleranceDist = toleranceDist;
        }
        /*
        public AbsoluteTolerance(double toleranceDist) {
            super(2 * DEFAULT_PERIOD);
            m_toleranceDist = toleranceDist;
        }
        */// discuss if this is needed
        @Override
        protected boolean onInstantTimeTarget(){
            return m_robotLoc.distance(m_destination)<m_toleranceDist;
        }

    }

    protected double getPowerPrecent(){
        return Math.min(1.0,m_robotLoc.distance(m_path.getLast())/m_slowDownDistance);
    }
}