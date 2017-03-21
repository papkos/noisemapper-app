package no.uio.ifi.akosp.noisemapper.services;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import no.uio.ifi.akosp.noisemapper.model.State;

/**
 * Created on 2016.10.07..
 *
 * @author Ákos Pap
 */
public class TempRecordingStorage {
    private Map<UUID, File> files = new HashMap<>();
    private Map<UUID, State> states = new HashMap<>();
    private Map<UUID, Integer> stepCounts = new HashMap<>();
    private final RecordingReadyHandler handler;

    public TempRecordingStorage(RecordingReadyHandler handler) {
        this.handler = handler;
    }

    public void addFile(UUID uuid, File file) {
        files.put(uuid, file);
        checkAndProcess(uuid);
    }

    public void addPhoneState(UUID uuid, State phoneState) {
        states.put(uuid, phoneState);
        checkAndProcess(uuid);
    }

    public void registerStartStepCount(UUID uuid, int startStepCount) {
        stepCounts.put(uuid, startStepCount);
    }

    public void registerStopStepCount(UUID uuid, int stopStepCount) {
        if (stepCounts.containsKey(uuid)) {
            int startStepCount = stepCounts.get(uuid);
            stepCounts.put(uuid, stopStepCount - startStepCount);
        }
    }

    private void checkAndProcess(UUID uuid) {
        if (files.containsKey(uuid) && states.containsKey(uuid)) {
            final State state = states.get(uuid);
            if (stepCounts.containsKey(uuid)) {
                int stepCount = stepCounts.get(uuid);
                state.setStepCount(stepCount);
                stepCounts.remove(uuid);
            }
            handler.handleRecordingReady(uuid, files.get(uuid), state);
            files.remove(uuid);
            states.remove(uuid);
        }
    }


    public interface RecordingReadyHandler {
        void handleRecordingReady(UUID uuid, File file, State phoneState);
    }
}
