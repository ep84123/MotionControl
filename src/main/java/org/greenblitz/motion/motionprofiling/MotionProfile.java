package org.greenblitz.motion.motionprofiling;

import java.util.ArrayList;
import java.util.List;

public class MotionProfile {

    protected List<Segment> segments;

    public MotionProfile(List<Segment> segs) {
        segments = segs;
    }

    public MotionProfile() {
        this(new ArrayList<>());
    }

    @Override
    public String toString() {
        return "MotionProfile{" +
                "segments=" + segments +
                '}';
    }

    public List<Segment> getSegments() {
        return segments;
    }

    /**
     * @param t point in time (in seconds)
     * @return The segment matching that point of time
     */
    public Segment getSegment(double t) {
        int lower = 0;
        int upper = segments.size() - 1;
        int testing;
        while (true){
            if (lower == upper)
                if (segments.get(lower).isTimePartOfSegment(t))
                    return segments.get(lower);
                else
                    throw new IndexOutOfBoundsException("No segment with such time");

            testing = (lower + upper) / 2;
            if (segments.get(testing).isTimePartOfSegment(t))
                return segments.get(testing);
            if (segments.get(testing).tStart > t)
                upper = testing - 1;
            else
                lower = testing + 1;
        }
    }

    /**
     * @return The time in which the profile finishes
     */
    public double getTEnd() {
        return segments.get(segments.size() - 1).tEnd;
    }

    /**
     * @param t point in time (in seconds)
     * @return whether or not the profile is finished by that time
     */
    public boolean isOver(double t) {
        return t >= getTEnd();
    }

    /**
     * @param t point in time (in seconds)
     * @return the acceleration at that time
     */
    public double getAcceleration(double t) {
        return getSegment(t).getAcceleration(t);
    }

    /**
     * @param t point in time (in seconds)
     * @return the velocity at that time
     */
    public double getVelocity(double t) {
        return getSegment(t).getVelocity(t);
    }

    /**
     * @param t point in time (in seconds)
     * @return the location at that time
     */
    public double getLocation(double t) {
        return getSegment(t).getLocation(t);
    }

    /**
     * Removes all segments with time length less then a milisecond.
     */
    public void removeBugSegments() {
        List<Segment> goodSegments = new ArrayList<>();
        for (Segment s : segments) {
            if (Math.abs(s.tEnd - s.tStart) > 1E-3)
                goodSegments.add(s);
        }
        segments = goodSegments;
    }

    /**
     * @param tStart
     * @param tEnd
     * @param accel
     * @param startVelocity
     * @param startLocation
     * @return
     */
    public static Segment createSegment(double tStart, double tEnd, double accel, double startVelocity, double startLocation) {
        return new Segment(tStart, tEnd, accel, startVelocity, startLocation);
    }

    public static class Segment {

        protected double tStart, tEnd, accel, startVelocity, startLocation;
        private final IndexOutOfBoundsException timeException =
                new IndexOutOfBoundsException("Time not in this segment");

        public Segment(double tStart, double tEnd, double accel, double startVelocity, double startLocation) {
            this.tStart = tStart;
            this.tEnd = tEnd;
            this.accel = accel;
            this.startVelocity = startVelocity;
            this.startLocation = startLocation;
        }

        public boolean isTimePartOfSegment(double t) {
            return t >= tStart && t <= tEnd;
        }

        public double getAcceleration(double t) {
            if (!isTimePartOfSegment(t))
                throw timeException;
            return accel;
        }

        public double getVelocity(double t) {
            if (!isTimePartOfSegment(t))
                throw timeException;
            return startVelocity + (t - tStart) * accel;
        }

        public double getLocation(double t) {
            if (!isTimePartOfSegment(t))
                throw timeException;
            double timePassed = (t - tStart);
            return startLocation + timePassed * startVelocity + 0.5 * Math.pow(timePassed, 2) * accel;
        }

        public double getTStart() {
            return tStart;
        }

        public void setTStart(double tStart) {
            this.tStart = tStart;
        }

        public double getTEnd() {
            return tEnd;
        }

        public void setTEnd(double tEnd) {
            this.tEnd = tEnd;
        }

        public double getAccel() {
            return accel;
        }

        public void setAccel(double accel) {
            this.accel = accel;
        }

        public double getStartVelocity() {
            return startVelocity;
        }

        public void setStartVelocity(double startVelocity) {
            this.startVelocity = startVelocity;
        }

        public double getStartLocation() {
            return startLocation;
        }

        public void setStartLocation(double startLocation) {
            this.startLocation = startLocation;
        }

        @Override
        public String toString() {
            return "Segment{" +
                    "tStart=" + tStart +
                    ", tEnd=" + tEnd +
                    ", accel=" + accel +
                    ", startVelocity=" + startVelocity +
                    ", startLocation=" + startLocation +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Segment segment = (Segment) o;

            if (Double.compare(segment.tStart, tStart) != 0) return false;
            if (Double.compare(segment.tEnd, tEnd) != 0) return false;
            if (Double.compare(segment.accel, accel) != 0) return false;
            if (Double.compare(segment.startVelocity, startVelocity) != 0) return false;
            return Double.compare(segment.startLocation, startLocation) == 0;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            temp = Double.doubleToLongBits(tStart);
            result = (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(tEnd);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(accel);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(startVelocity);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(startLocation);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }

}
