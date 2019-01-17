package org.greenblitz.utils;

import com.ctre.phoenix.motorcontrol.can.TalonSRX;

/**
 * This class has many functions that make using an encoder much simpler.
 * The class uses the encoders prebuilt into the TalonSRX for all calculations.
 *
 * @see edu.wpi.first.wpilibj.Encoder
 * @see TalonSRX
 */

public class SmartEncoder {
    private final TalonSRX m_talon;
    private double m_ticksPerMeter;
    private int m_inverted = 1;

    public TalonSRX getTalon() {
        return m_talon;
    }

    /**
     * This constructor receives a TalonSRX and the ticks per meter of the Talon
     * It also checks to see if the ticks per meter are valid as well.
     * @param talon A TalonSRX object which is used of it's encoder. (m_talon)
     * @param ticksPerMeter A final double of the ticks per meter the talon feels per meter of movement.
     */
    public SmartEncoder(TalonSRX talon, double ticksPerMeter) {
        if (ticksPerMeter == +0.0 || !Double.isFinite(ticksPerMeter))
            throw new IllegalArgumentException("invalid ticks per meter value '" + ticksPerMeter + "'");

        m_talon = talon;
        m_ticksPerMeter = ticksPerMeter;
    }

    /**
     * This function returns the amount of ticks that have been felt by encoder since it was last reset.
     *
     * @return The amount of ticks felt by the talon.
     */
    public int getTicks() {
        return m_inverted * m_talon.getSensorCollection().getQuadraturePosition();
    }

    /**
     * This function returns the amount of meters that the encoder has felt, by dividing tick by the ticks per meter value.
     *
     * @return The ticks felt by the talon divided by the ticks it feels per meter.
     */
    public double getDistance() {
        return getTicks() / m_ticksPerMeter;
    }

    /**
     * This function returns the velocity felt by the encoder divided by the ticks per meter.
     *
     * @return Meter per second velocity of the encoder
     */
    public double getSpeed() {
        return ((double) getTickRate()) / m_ticksPerMeter;
    }


    /**
     * This function resets the encoder
     *
     * @return Error code if the encoder could not be reset, otherwise resets the encoder.
     */
    public void reset() {
        m_talon.getSensorCollection().setQuadraturePosition(0, 30);
    }

    public int getTickRate() {
        return m_inverted * m_talon.getSensorCollection().getQuadratureVelocity();
    }

    public double getVelocity() {
        return getSpeed();
    }

    public double getTicksPerMeter() {
        return m_ticksPerMeter;
    }

    public void invert() {
        m_inverted = -1;
    }

    public void clear() {
        m_inverted = 1;
    }
}