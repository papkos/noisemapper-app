package no.uio.ifi.akosp.noisemapper.model;

import java.util.Locale;

/**
 * Created on 2016.03.30..
 *
 * @author Ákos Pap
 */
public class Orientation {
    public float azimuth;
    public float pitch;
    public float roll;

    public Orientation(float azimuth, float pitch, float roll) {
        this.azimuth = azimuth;
        this.pitch = pitch;
        this.roll = roll;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "X: %.0f° | Y: %.0f° | Z: %.0f°",
                Math.toDegrees(pitch), Math.toDegrees(roll), Math.toDegrees(azimuth));
    }
}
