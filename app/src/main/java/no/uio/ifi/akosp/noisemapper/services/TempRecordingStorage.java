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

    private void checkAndProcess(UUID uuid) {
        if (files.containsKey(uuid) && states.containsKey(uuid)) {
            handler.handleRecordingReady(uuid, files.get(uuid), states.get(uuid));
            files.remove(uuid);
            states.remove(uuid);
        }
    }


    public interface RecordingReadyHandler {
        void handleRecordingReady(UUID uuid, File file, State phoneState);
    }
}
