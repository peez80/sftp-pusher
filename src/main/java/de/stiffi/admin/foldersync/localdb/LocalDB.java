package de.stiffi.admin.foldersync.localdb;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.Remote;
import java.util.HashMap;
import java.util.Map;

public class LocalDB {

    private Map<Path, RemoteFileInfoEntity> db = new HashMap<>();

    public boolean needsSync(Path localFilePath) throws IOException {
        RemoteFileInfoEntity dbEntry = db.get(localFilePath);
        if (dbEntry == null) {
            return true;
        }

        return Files.size(localFilePath) != dbEntry.getLastSyncFilesize()
                || Files.getLastModifiedTime(localFilePath).toMillis() != dbEntry.getLastSyncFilesize();
    }

    public void markSynched(Path path) {
        RemoteFileInfoEntity fileInfo = db.get(path);
        if (fileInfo == null) {
            fileInfo = new RemoteFileInfoEntity(path);
            db.put(path, fileInfo);
        }

        try {
            fileInfo.setLastSyncTimestamp(System.currentTimeMillis());
            fileInfo.setLastSyncFilesize(Files.size(path));
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public void remove(Path path) {
        db.remove(path);
    }

    public void load(Path dbFile) throws IOException {
        Gson gson = new GsonBuilder().create();
        String json = new String(Files.readAllBytes(dbFile));
        Map<Path, RemoteFileInfoEntity> map = gson.fromJson(json, HashMap.class);
        db = map;
    }

    public void save(Path dbFile) throws IOException {
        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(db);
        Files.write(dbFile, json.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
    }

    public int size() {
        return db.size();
    }
}
