package VelocityManager;

import java.util.Date;

/**
 * Controls the voltage of an actuator using desired velocity.
 * 
 * @author Alexey
 *
 */
public class VoltageController {

	/**
	 * The constant that determines how quickly we converge to the desired
	 * velocity. Should be 1 at all times in theory, but could be changed.
	 */
	protected double m_Ka;

	/**
	 * The velocity constant of this actuator. When voltage passes is 0 and the
	 * robot is moving, you can calculate the constant as: <a href=
	 * "https://www.codecogs.com/eqnedit.php?latex=k_u&space;=&space;-&space;\frac{a}{U}"
	 * target="_blank"><img src=
	 * "https://latex.codecogs.com/gif.latex?k_u&space;=&space;-&space;\frac{a}{U}"
	 * title="k_u = - \frac{a}{U}" /></a> where a is the actuator acceleration
	 * and U the actuator velocity.
	 */
	protected double m_Ku;

	/**
	 * The voltage constant of this actuator. can be calculated as: <a href=
	 * "https://www.codecogs.com/eqnedit.php?latex=k_v&space;=&space;\frac{a&space;&plus;&space;k_u&space;\cdot&space;U}{V}"
	 * target="_blank"><img src=
	 * "https://latex.codecogs.com/gif.latex?k_v&space;=&space;\frac{a&space;&plus;&space;k_u&space;\cdot&space;U}{V}"
	 * title="k_v = \frac{a + k_u \cdot U}{V}" /></a> where a is the actuator
	 * acceleration, U the actuator velocity, Ku is m_Ku and V is the voltage
	 * passed.
	 */
	protected double m_Kv;

	/**
	 * In the calculation of the average time passed, how much importance is put
	 * on previous values compared to new ones. <br>
	 * 0 - no importance to past values <br>
	 * 1 - same importace as current calue <br>
	 * 20 - low importance to current values
	 */
	protected int m_pastTimeImportace;

	/**
	 * The last time this object was called
	 */
	protected Date m_lastCalled;

	/**
	 * The time it takes between each call of the controller
	 */
	protected double m_avarageCallTime = -1;

	private static final double DEFAULT_KA = 1;
	private static final int DEFAULT_PAST_IMP = 3;

	/**
	 * 
	 * @param Ku
	 *            {@link VoltageController#m_Ku}
	 * @param Kv
	 *            {@link VoltageController#m_Kv}
	 * @param Ka
	 *            {@link VoltageController#m_Ka}
	 * @param pastTimeImportance
	 *            {@link VoltageController#m_pastTimeImportace}
	 *            
	 * @throws RuntimeException when Ku or Kv are equal to 0
	 */
	public VoltageController(double Ku, double Kv, double Ka, int pastTimeImportance) {
		m_Ka = Ka;
		m_Ku = Ku;
		m_Kv = Kv;
		m_pastTimeImportace = pastTimeImportance;

		if (m_Ku == 0)
			throw new RuntimeException(
					"m_Kv was set to 0, are you implying the velocity has no affect over the acceleration?");
		if (m_Kv == 0)
			throw new RuntimeException(
					"m_Kv was set to 0, are you implying the voltage has no affect over the velocity?");
	}

	/**
	 * @see {@link VoltageController#VoltageController(double, double, double, int)}
	 * @param Ku
	 * @param Kv
	 * @param pastTimeImportance
	 */
	public VoltageController(double Ku, double Kv, int pastTimeImportance) {
		this(Ku, Kv, DEFAULT_KA, pastTimeImportance);
	}

	/**
	 * @see {@link VoltageController#VoltageController(double, double, double, int)}
	 * @param Ku
	 * @param Kv
	 * @param Ka
	 */
	public VoltageController(double Ku, double Kv, double Ka) {
		this(Ku, Kv, Ka, DEFAULT_PAST_IMP);
	}

	/**
	 * @see {@link VoltageController#VoltageController(double, double, double, int)}
	 * @param Ku
	 * @param Kv
	 */
	public VoltageController(double Ku, double Kv) {
		this(Ku, Kv, DEFAULT_KA, DEFAULT_PAST_IMP);
	}
	
	/**
	 * @see {@link VoltageController#m_Ka}
	 * @param val
	 */
	public void setKa(double val) {
		m_Ka = val;
	}

	/**
	 * @see {@link VoltageController#m_pastTimeImportace}
	 * @param val
	 */
	public void setPastTimeImportance(int val) {
		m_pastTimeImportace = val;
	}

	/**
	 * Reset the avarage call time and the time last called, must be called before start of program.
	 */
	public void resetTimeInterval() {
		m_avarageCallTime = -1;
		m_lastCalled = new Date();
	}

	/**
	 * 
	 * @param desiredVelocity
	 * @param currentVelocity as measured from sensors
	 * @return the voltage that should be supplied to the actuator
	 * @throws NullPointerException {@link VoltageController#resetTimeInterval()} wasn't called prior to this.
	 */
	public double getVoltage(double desiredVelocity, double currentVelocity) throws NullPointerException {
		return (getStartAcceleration(desiredVelocity, currentVelocity) + m_Ku * currentVelocity) / m_Kv;
	}

	/**
	 * 
	 * @param desiredVelocity
	 * @param currentVelocity as measured from sensors
	 * @return The desired average acceleration
	 */
	protected double getDesiredAcceleration(double desiredVelocity, double currentVelocity) {
		return (desiredVelocity - currentVelocity) * m_Ka;
	}

	/**
	 * 
	 * @param desiredVelocity
	 * @param currentVelocity as measured from sensors
	 * @return The desired start acceleration
	 * @throws NullPointerException {@link VoltageController#resetTimeInterval()} wasn't called prior to this.
	 */
	protected double getStartAcceleration(double desiredVelocity, double currentVelocity) throws NullPointerException {
		Date currDate = new Date();
		long milisecsPasses = currDate.getTime() - m_lastCalled.getTime();
		if (m_avarageCallTime == -1) {
			m_avarageCallTime = (milisecsPasses / 1000.0);
		} else {
			m_avarageCallTime = m_pastTimeImportace * m_avarageCallTime + (milisecsPasses / 1000.0);
			m_avarageCallTime /= m_pastTimeImportace + 1;
		}
		m_lastCalled = currDate;

		double da = getDesiredAcceleration(desiredVelocity, currentVelocity);
		if (da == 0)
			return 0;
		if (milisecsPasses == 0)
			return da;
		return 3 / (da * (3 - m_Ku * m_avarageCallTime));
	}
}
