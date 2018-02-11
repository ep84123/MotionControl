package APPC;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import base.EnvironmentPort;
import base.Input;
import base.IterativeController;
import base.ScaledEncoder;
import base.point.orientation.Orientation2D;
import base.point.IPoint2D;
import base.point.orientation.IOrientation2D;
import base.point.orientation.IOrientation2D.DirectionEffect;

public class Localizer implements Input<IPoint2D> {
	public static final double PERIOD = IterativeController.DEFAULT_PERIOD / 4;
	public static final Object LOCK = new Object();

	private IOrientation2D m_location;

	private ScaledEncoder[] m_leftWrappedEncoders;
	private ScaledEncoder[] m_rightWrappedEncoders;
	private double m_wheelDistance;

	private EnvironmentPort ePort = EnvironmentPort.DEFAULT;

	/**
	 * 
	 * @param left
	 *            left encoders
	 * @param right
	 *            right encoders
	 * @param location
	 *            the initial location
	 * @param wheelDistance
	 *            distance between the wheels (right left)
	 */
	public Localizer(ScaledEncoder[] left, ScaledEncoder[] right, Orientation2D location, double wheelDistance) {
		m_location = location;
		m_leftWrappedEncoders = left;
		m_rightWrappedEncoders = right;
		m_wheelDistance = wheelDistance;
		Timer m_timer = new Timer();
		m_timer.schedule(new LocalizeTimerTask(), 0, (long) (1000 * PERIOD));
	}

	/**
	 * 
	 * @param left
	 *            left encoders
	 * @param right
	 *            right encoders
	 * @param location
	 *            the initial location
	 * @param wheelDistance
	 *            distance between the wheels (right left)
	 */
	public Localizer(ScaledEncoder left, ScaledEncoder right, Orientation2D location, double wheelDistance) {
		this(new ScaledEncoder[] { left }, new ScaledEncoder[] { right }, location, wheelDistance);
	}

	/**
	 * 
	 * @param left
	 *            left encoder
	 * @param right
	 *            right encoder
	 * @param wheelDist
	 *            distance between the wheels (right left)
	 * @return new localizer
	 */
	public static Localizer of(ScaledEncoder left, ScaledEncoder right, double wheelDist) {
		return new Localizer(left, right, Orientation2D.mutable(0, 0, 0), wheelDist);
	}

	/**
	 * 
	 * @return distance traveled by left encoders
	 */
	public double getLeftDistance() {
		return Arrays.stream(m_leftWrappedEncoders).map(ScaledEncoder::getDistance).reduce((a, b) -> a + b).orElse(.0)
				/ m_leftWrappedEncoders.length;
	}

	/**
	 * 
	 * @return distance traveled by right encoders
	 */
	public double getRightDistance() {
		return Arrays.stream(m_rightWrappedEncoders).map(ScaledEncoder::getDistance).reduce((a, b) -> a + b).orElse(.0)
				/ m_rightWrappedEncoders.length;
	}

	private class LocalizeTimerTask extends TimerTask {

		private double leftDist;
		private double rightDist;

		@Override
		/**
		 * Update the robot position
		 */
		public void run() {
			if (ePort.isEnabled()) {
				ePort.putNumber("Encoder Left", getLeftDistance());
				ePort.putNumber("Right encoder", getRightDistance());
				ePort.putNumber("X-pos R", m_location.getX());
				ePort.putNumber("Y-pos R", m_location.getY());

				double rightDistDiff = -rightDist;
				double leftDistDiff = -leftDist;
				leftDist = getLeftDistance();
				rightDist = getRightDistance();
				rightDistDiff += rightDist;
				leftDistDiff += leftDist;

				if (leftDistDiff == rightDistDiff) {
					synchronized (LOCK) {
						m_location = (Orientation2D) m_location.moveBy(0, leftDistDiff, 0, DirectionEffect.RESERVED);
						return;
					}
				}

				boolean leftIsLong = leftDistDiff > rightDistDiff;
				double shortDist = leftIsLong ? rightDistDiff : leftDistDiff;

				double angle = (rightDistDiff - leftDistDiff) / m_wheelDistance;

				double radiusFromCenter = -(shortDist / angle + Math.signum(angle) * m_wheelDistance / 2);
				double adjustedRadiusFromCenter = radiusFromCenter;
				IOrientation2D rotationOrigin = 
						(Orientation2D) m_location.moveBy(adjustedRadiusFromCenter, 0, 0, DirectionEffect.RESERVED);
				synchronized (LOCK) {
					m_location = m_location.rotateAround(rotationOrigin, angle, DirectionEffect.CHANGED);
				}
				System.out.println("WARNING - robot location: " + m_location);
			} else {
				reset();
			}
		}
	}

	@Override
	public IPoint2D recieve() {
		synchronized (LOCK) {
			return m_location;
		}
	}

	/**
	 * reset the encoders and the localizer saved location.
	 */
	public void reset() {
		for (ScaledEncoder enc : m_leftWrappedEncoders)
			enc.reset();

		for (ScaledEncoder enc : m_rightWrappedEncoders)
			enc.reset();

		m_location = IOrientation2D.GLOBAL_ORIGIN;
	}

	public void setEnvironmentPort(EnvironmentPort ePort) {
		this.ePort = ePort;
	}
}
