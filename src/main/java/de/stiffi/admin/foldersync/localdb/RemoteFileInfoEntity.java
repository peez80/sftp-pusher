package de.stiffi.admin.foldersync.localdb;

import java.nio.file.Path;
import java.nio.file.Paths;

public class RemoteFileInfoEntity {
    private String localPath;
    private long lastSyncTimestamp;
    private long lastSyncFilesize;

    public RemoteFileInfoEntity(Path path) {
        this.localPath = path.toString();
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public long getLastSyncTimestamp() {
        return lastSyncTimestamp;
    }

    public void setLastSyncTimestamp(long lastSyncTimestamp) {
        this.lastSyncTimestamp = lastSyncTimestamp;
    }

    public long getLastSyncFilesize() {
        return lastSyncFilesize;
    }

    public void setLastSyncFilesize(long lastSyncFilesize) {
        this.lastSyncFilesize = lastSyncFilesize;
    }
}
