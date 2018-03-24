package de.stiffi.admin.foldersync;

import de.stiffi.admin.foldersync.api.FileHandler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;

/**
 * Encapsulates all Actions related to source+targetFile, so caller doesn't have to deal with relative paths etc.
 */
public class SyncFilePair {

    private FileHandler local;
    private FileHandler remote;
    private SftpConnection remoteSftpConnection;

    public SyncFilePair(String localRoot, String remoteRoot, String relativeFilePath, SftpConnection remoteSftpConnection) {
        this.remoteSftpConnection = remoteSftpConnection;
        local = getLocalFileInfo(localRoot, relativeFilePath);
        remote = getRemoteFileInfo(remoteRoot, relativeFilePath.replace("\\", "/"));
    }

    public FileHandler getLocal() {
        return local;
    }

    public FileHandler getRemote() {
        return remote;
    }


    private FileHandler getLocalFileInfo(String localRoot, String relativeFilePath) {
        Path localPath = Paths.get(localRoot, relativeFilePath).toAbsolutePath();

        return new FileHandler() {
            @Override
            public String getPath() {
                return localPath.toString();
            }

            @Override
            public long getSize() {
                try {
                    return Files.size(localPath);
                } catch (IOException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }

            @Override
            public long getLastModifiedTime() {
                try {

                    return Files.getLastModifiedTime(localPath).toMillis();
                } catch (IOException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }

            @Override
            public void setLastModifiedTime(long timestampMillisUTC) {
                try {
                    Files.setLastModifiedTime(localPath, FileTime.fromMillis(timestampMillisUTC));
                } catch (IOException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }

            @Override
            public boolean exists() {
                return Files.exists(localPath);
            }

            @Override
            public boolean isDir() {
                return Files.isDirectory(localPath);
            }

            @Override
            public void createParentDir() {
                try {
                    Files.createDirectories(localPath.getParent());
                } catch (IOException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }

            @Override
            public InputStream getInputStream() {
                try {
                    return new BufferedInputStream(Files.newInputStream(localPath, StandardOpenOption.READ));
                } catch (IOException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }

            @Override
            public OutputStream getOutputStream() {
                throw new UnsupportedOperationException("OutputStream on local files currently not supported");
            }
        };
    }

    private FileHandler getRemoteFileInfo(String remoteRoot, String relativeFilePath) {
        return remoteSftpConnection.getFileHandler(remoteRoot + "/" + relativeFilePath);
    }


    @Override
    public String toString() {
        String s = "";
        s += "Local: " + local.getPath() + ", Remote: " + remote.getPath();
        return s;
    }
}
