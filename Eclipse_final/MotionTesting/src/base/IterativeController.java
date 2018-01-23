package base;

import java.util.Timer;
import java.util.TimerTask;

import edu.wpi.first.wpilibj.DriverStation;

/**
 *
 * Represents a controller which has the basic structure of a loop which calls
 * it's input and output
 */

public abstract class IterativeController<IN, OUT> extends Controller<IN, OUT> {
    public static final double DEFAULT_PERIOD = .05;

    protected final double m_period;

    protected Timer m_controllerLoop; // the loop which will calculate the
				      // controller

    private IterativeController(Input<IN> in, Output<OUT> out, IN destination, double period) {
	super(in, out, destination);
	m_period = period;

	m_controllerLoop = new Timer();
	m_controllerLoop.schedule(new IterativeCalculationTask(), 0L, (long) (1000 * period));
    }

    public IterativeController(Input<IN> in, Output<OUT> out, IN destination) {
	this(in, out, destination, DEFAULT_PERIOD);
    }

    @SuppressWarnings("unchecked")
    public IterativeController(Output<OUT> out, IN destination) {
	this(NO_INPUT, out, destination);
    }

    public IterativeController(Input<IN> in, Output<OUT> out, double period) {
	this(in, out, null, period);
    }

    public IterativeController(Input<IN> in, Output<OUT> out) {
	this(in, out, DEFAULT_PERIOD);
    }

    @SuppressWarnings("unchecked")
    public IterativeController(Output<OUT> out) {
	this(NO_INPUT, out);
    }

    protected class IterativeCalculationTask extends TimerTask {
	public IterativeCalculationTask() {
	}

	@Override
	public void run() {
	    if (DriverStation.getInstance().isEnabled()) {
		if (m_controllerState == State.ENABLED) {
		    if (m_destination == null) {
			System.err.println("WARNING - destination is null");
			return;
		    }

		    if (m_tolerance == NO_TOLERANCE) {
			System.err.println("WARNING - tolerance not set");
			return;
		    }
		    boolean dafuckIsTrue = m_tolerance.onTarget();
		    System.out.println("Dafuck? Why is this shit " + dafuckIsTrue);
		    if (!dafuckIsTrue) {
			calculate();
		    } else {
			m_controllerState = State.END;
			m_output.stop();
			try {
			    throw new RuntimeException("FUCK YOU");
			} catch (Exception e){
			    e.printStackTrace();
			}
			System.out.println("WARNING: controller has finished running");
		    }
		} else {
		    if (m_controllerState == State.END)
			stop();
		}
		// free(); //test only first iteration
	    } else {
		free();
		System.out.println("APPCOutput object #" + this.hashCode() + " is now freed");
	    }
	}
    }

    public void free() {
	m_controllerLoop.cancel();
	super.free();
	synchronized (LOCK) {
	    m_controllerLoop = null;
	}
    }
}
