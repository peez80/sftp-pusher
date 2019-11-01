package de.stiffi.admin.foldersync.localdb;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

public class LocalDB {

    private Set<RemoteFileInfoEntity> db = new HashSet<>();

    private RemoteFileInfoEntity find(String localFilePath) {
        for (RemoteFileInfoEntity e : db) {
            if (e.getLocalPath().equalsIgnoreCase(localFilePath)) {
                return e;
            }
        }
        return null;
    }

    public boolean needsSync(String localFilePath) throws IOException {
        RemoteFileInfoEntity dbEntry = find(localFilePath);
        if (dbEntry == null) {
            return true;
        }

        Path localFile = Paths.get(localFilePath);
        return Files.size(localFile) != dbEntry.getLastSyncFilesize()
                || Files.getLastModifiedTime(localFile).toMillis() != dbEntry.getLastSyncTimestamp();
    }

    public void markSynched(String path) {
        RemoteFileInfoEntity fileInfo = find(path);
        Path file = Paths.get(path);
        if (fileInfo == null) {
            fileInfo = new RemoteFileInfoEntity(file);
            db.add(fileInfo);
        }

        try {
            fileInfo.setLastSyncTimestamp(Files.getLastModifiedTime(file).toMillis());
            fileInfo.setLastSyncFilesize(Files.size(file));
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public void remove(String path) {
        db.remove(path);
    }

    public void load(Path dbFile) throws IOException {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(dbFile))) {
            db = (Set<RemoteFileInfoEntity>) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public void save(Path dbFile) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(dbFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            out.writeObject(db);
        }
    }

    public int size() {
        return db.size();
    }
}
