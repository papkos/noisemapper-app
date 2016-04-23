package no.uio.ifi.akosp.noisemapper;

import no.uio.ifi.akosp.noisemapper.model.Orientation;
import no.uio.ifi.akosp.noisemapper.model.State;

/**
 * Created on 2016.04.23..
 *
 * @author Ákos Pap
 */
public class Utils {

    public static boolean isInPocket(State state) {
        return isInPocket(state.getOrientation(), state.getProximity(), state.getLight());
    }

    public static boolean isInPocket(Orientation orientation, float proximity, float light) {
        return isInPocket(orientation, proximity, light, C.ORIENTATION_THRESHOLD, C.LIGHT_THRESHOLD);
    }

    public static boolean isInPocket(Orientation orientation, float proximity, float light,
                                     float orientationThreshold, float lightThreshold) {
        return (
                proximity < 10  // near
                        && light < lightThreshold  // For some reason on the Nexus 6 even when covered, light is 2.0 lux
                        && (Math.abs(orientation.pitch - Math.toRadians(-90)) < orientationThreshold  // vertical, head down
                        || Math.abs(orientation.pitch - Math.toRadians(90)) < orientationThreshold  // vertical, head up
                )

        );
    }
}
