package org.greenblitz.motion.pathing;

import org.greenblitz.motion.base.Position;

import java.util.List;

/**
 * @author Alexey
 */
public class BasicAngleInterpolator {


    public static Path<Position> interpolateAngles(Path<Position> original) {
        List<Position> m_path = original.getPath();
        for (int i = 1; i < m_path.size() - 1; i++) {
            m_path.get(i).setAngle(Math.atan2(
                    m_path.get(i + 1).getY() - m_path.get(i - 1).getY(),
                    m_path.get(i + 1).getX() - m_path.get(i - 1).getX()
            ));
        }
        return new Path<>(m_path);
    }

}
