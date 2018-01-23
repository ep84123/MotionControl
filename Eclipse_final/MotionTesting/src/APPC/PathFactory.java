package APPC;

import java.util.ArrayList;

public class PathFactory {

	private Path m_path = new Path();

	public PathFactory() {
		m_path.add(new Point2D(0, 0, 0));
	}

	public PathFactory(Point2D origin) {
		m_path.add(origin);
	}

	public PathFactory(Path path) {
		m_path = path;
		if (path.getTotalLength() == 0) {
			System.err.println("No origin supplied to path, setting default");
			m_path.add(new Point2D(0, 0, 0));
		}
	}

	/**
	 * Continue the path in a straight line form the last path point to the given point 
	 * @param connectTo the given point
	 * @param metersPerPoint the distance in meters between each point
	 * @return the factory
	 */
	public PathFactory connectLine(Point2D connectTo, double metersPerPoint) {
		Point2D origin = m_path.getLast();
		Point2D distance = origin.distanceVector(connectTo);
		if (distance.length() == 0) return this;
		double totalPoints = distance.length() / metersPerPoint;
		
		double xJump = distance.getX() / totalPoints,
			   yJump = distance.getY() / totalPoints;
		
		while (m_path.getLast().distance(connectTo) > 0) {
			if (m_path.getLast().distance(connectTo) <= metersPerPoint){
				// Code almost done, finish it off
				m_path.add(connectTo);
				break;
			}
			m_path.add(m_path.getLast().add(xJump, yJump));
		}
		return this;
	}
	
	
	//TODO test
	@Deprecated
	public PathFactory genStraightLine(double len, double rotation, double metersPerPoint) {
		Point2D origin = m_path.getLast();
		for (double i = 0; i <= len; i += metersPerPoint) {
			m_path.add(new Point2D(0, i, 0).rotate(rotation).add(origin));
			// System.out.println(m_path.getLast());
		}
		return this;
	}

	public Path construct() {
		return m_path;
	}

	public PathFactory sideways(double length) {
		return sideways(length, 0.01);
	}

	public PathFactory sideways(double length, double metersPerPoint) {
		for (double i = metersPerPoint; i < length; i += metersPerPoint)
			m_path.add(new Point2D(i, 0, 0));
		m_path.add(new Point2D(length, 0, 0));
		return this;
	}

	public PathFactory forwards(double length) {
		return forwards(length, 0.01);
	}

	public PathFactory forwards(double length, double metersPerPoint) {
		for (double i = metersPerPoint; i < length; i += metersPerPoint)
			m_path.add(new Point2D(0, i, 0));
		m_path.add(new Point2D(0, length, 0));
		return this;
	}

}
