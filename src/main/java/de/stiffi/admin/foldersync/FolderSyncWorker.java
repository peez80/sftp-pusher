package de.stiffi.admin.foldersync;

import de.stiffi.DPHelpers.DPHelpers;
import de.stiffi.DPHelpers.Files.DirSpider;
import de.stiffi.DPHelpers.Files.SimpleDirSpider;
import de.stiffi.DPHelpers.StopWatch;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Push all new or changed files from local to remote
 */
public class FolderSyncWorker {

    /**
     * Where to start the recursive Sync
     */
    private Path localRootPath;
    private String remoteRootPath;

    private SftpConnection sftpConnection;
    private List<SyncFilePair> localFiles;
    private AtomicLong localFilesSizeComplete;


    public FolderSyncWorker(Path localRootPath, String sftpHost, String sftpUser, String sftpPassword, int sftpPort, String sftpRootPath) {
        this.localRootPath = localRootPath;
        this.remoteRootPath = sftpRootPath;
        sftpConnection = new SftpConnection(sftpHost, sftpUser, sftpPassword, sftpPort);
    }

    public List<SyncFilePair> go() {
        indexFiles();

        AtomicInteger processedFiles = new AtomicInteger(0);
        List<SyncFilePair> syncedFiles = new ArrayList<>();

        StopWatch bytesStopWatch = new StopWatch(localFilesSizeComplete.get());

        localFiles.forEach(filePair -> {
            if (shouldSync(filePair)) {
                try {
                    System.out.println("");
                    System.out.println("Uploading " + filePair.getLocal().getPath());
                    StopWatch uploadStopwatch = new StopWatch();

                    uploadLocalFileToRemote(filePair);

                    long elapsedMillis = uploadStopwatch.getElapsedTimeMillis();
                    double speed = (double)((double)filePair.getLocal().getSize() / (double)elapsedMillis);
                    syncedFiles.add(filePair);
                    bytesStopWatch.increment(filePair.getLocal().getSize());
                    System.out.println("finished. Speed: " + DPHelpers.formatBytes(speed) + "/s");
                    System.out.println("");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            processedFiles.incrementAndGet();

            System.out.print("Files processed: " + processedFiles.get() + "/" + localFiles.size()
                    + ", Files uploaded: " + syncedFiles.size()
                    + ", transferred: " + DPHelpers.formatBytes(bytesStopWatch.getProcessed()) + "/" + DPHelpers.formatBytes(localFilesSizeComplete.get())
                    + ", remaining Time: " + DPHelpers.formatDuration(bytesStopWatch.getEstimatedRemainingTimeMillis(), DPHelpers.DurationFormat.dhms)
                    + "              \r");
        });

        System.out.println("Files processed: " + processedFiles.get() + "/" + localFiles.size()
                + ", Files uploaded: " + syncedFiles.size()
                + ", transferred: " + DPHelpers.formatBytes(bytesStopWatch.getProcessed()) + "/" + DPHelpers.formatBytes(localFilesSizeComplete.get())
                + ", remaining Time: " + DPHelpers.formatDuration(bytesStopWatch.getEstimatedRemainingTimeMillis(), DPHelpers.DurationFormat.dhms)
               );

        return syncedFiles;
    }

    private Collection<SyncFilePair> indexFiles() {

        localFiles = new LinkedList<>();
        localFilesSizeComplete = new AtomicLong(0);

        AtomicInteger indexedFiles = new AtomicInteger(0);

        DirSpider dirSpider = new SimpleDirSpider(localRootPath.toFile().getAbsoluteFile()) {
            @Override
            public void handleFile(File file) {
                String relativePath = localRootPath.relativize(file.toPath().toAbsolutePath()).toString();
                SyncFilePair filePair = new SyncFilePair(localRootPath.toAbsolutePath().toString(), remoteRootPath, relativePath, FolderSyncWorker.this.sftpConnection);
                localFiles.add(filePair);
                localFilesSizeComplete.addAndGet(filePair.getLocal().getSize());
                int count = indexedFiles.incrementAndGet();
                if (count % 100 == 0) {
                    System.out.print("Indexed Files: " + count + ", Size: " + DPHelpers.formatBytes(localFilesSizeComplete.get()) + "     \r");
                }
            }
        };
        dirSpider.go();
        System.out.println("Indexed Files: " + indexedFiles.get() + ", Size: " + DPHelpers.formatBytes(localFilesSizeComplete.get()) + "     ");
        System.out.println("");
        return localFiles;
    }


    private boolean shouldSync(SyncFilePair filePair) {
        //Currently only one-way sync and we won't remove any deleted files
        if (!filePair.getRemote().exists()) {
            return true;
        } else if (filePair.getLocal().getSize() != filePair.getRemote().getSize()) {
            return true;
        } else if (filePair.getLocal().getLastModifiedTime() > filePair.getRemote().getLastModifiedTime()) {
            return true;
        }
        return false;
    }

    /**
     * Overwrites the file in destination with the file from source
     *
     * @param filePair
     * @throws IOException
     */
    private void uploadLocalFileToRemote(SyncFilePair filePair) throws IOException {
        //Since there is no observable Stream Copy mechanism, I'll build it on my own...
        filePair.getRemote().createParentDir();
        try (InputStream in = new BufferedInputStream(filePair.getLocal().getInputStream()); OutputStream out = new BufferedOutputStream(filePair.getRemote().getOutputStream())) {
            IOUtils.copyLarge(in, out);
        }
        //Add 1000ms to the resulting filetime because sftp will make a floor to the seconds, so the reulting file would always be "older" than the original file.
        filePair.getRemote().setLastModifiedTime(filePair.getLocal().getLastModifiedTime() + 1000l);
    }


}
