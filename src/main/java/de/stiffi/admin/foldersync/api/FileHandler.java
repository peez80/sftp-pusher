package de.stiffi.admin.foldersync.api;

import java.io.InputStream;
import java.io.OutputStream;

public interface FileHandler {
    String getPath();
    long getSize();

    /**
     * Return Unix Timestamp in Milliseconds - UTC
     * @return
     */
    long getLastModifiedTime();
    void setLastModifiedTime(long timestampMillisUTC);
    boolean exists();
    boolean isDir();
    void createParentDir();
    InputStream getInputStream();
    OutputStream getOutputStream();
}
