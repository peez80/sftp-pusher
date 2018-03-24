package de.stiffi.admin.foldersync;

import com.jcraft.jsch.*;
import de.stiffi.admin.foldersync.api.FileHandler;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class SftpConnection {

    private ChannelSftp sftp;

    public SftpConnection(String host, String username, String password, int port) {
        try {
            init(host, username, password, port);
        } catch (JSchException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }


    private void init(String host, String username, String password, int port) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        System.out.println("Connect: host: " + host + ", user: " + username + ", port: " + port);
        session.connect();
        System.out.println("Creating SFTP Channel.");
        sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect();
        System.out.println("SFTP Channel created.");
    }

    public FileHandler getFileHandler(String path) {
        return new FileHandler() {
            @Override
            public String getPath() {
                return path;
            }

            @Override
            public long getSize() {
                return getAttrs(path).getSize();
            }

            @Override
            public long getLastModifiedTime() {
                //TODO: UTC millis?
                return getAttrs(path).getMTime()*1000l;
            }

            @Override
            public void setLastModifiedTime(long timestampMillisUTC) {
                int mtime = (int)(timestampMillisUTC / 1000l);
                try {
                    sftp.setMtime(path, mtime);
                } catch (SftpException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }

            @Override
            public boolean exists() {
                return getAttrs(path) != null;
            }

            @Override
            public boolean isDir() {
                return getAttrs(path).isDir();
            }

            @Override
            public void createParentDir() {
                String parentDir = PathUtils.getParentDir(path);
                try {
                    createDirs(parentDir);
                } catch (SftpException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }


            }

            private boolean exists(String path) {
                return getAttrs(path) != null;
            }

            private void createDirs(String path) throws SftpException {
                String parentDir = PathUtils.getParentDir(path);
                if (parentDir != null && !exists(parentDir)) {
                    createDirs(parentDir);
                }
                if (!exists(path)) {
                    sftp.mkdir(path);
                }
            }

            @Override
            public InputStream getInputStream() {
                try {
                    return new BufferedInputStream(sftp.get(path));
                } catch (SftpException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }

            @Override
            public OutputStream getOutputStream() {
                try {
                    return new BufferedOutputStream(sftp.put(path, ChannelSftp.OVERWRITE));
                } catch (SftpException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }
        };
    }

    private SftpATTRS getAttrs(String path) {
        try {
            SftpATTRS attrs = sftp.stat(path);
            return attrs;
        } catch (SftpException e) {
            if (e.getMessage().toLowerCase().contains("no such file")) {
                return null;
            }
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
