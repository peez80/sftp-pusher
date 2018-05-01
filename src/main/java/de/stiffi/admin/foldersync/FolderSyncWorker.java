package de.stiffi.admin.foldersync;

import de.stiffi.DPHelpers.DPHelpers;
import de.stiffi.DPHelpers.Files.DirSpider;
import de.stiffi.DPHelpers.Files.SimpleDirSpider;
import de.stiffi.DPHelpers.StopWatch;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Push all new or changed files from local to remote
 */
public class FolderSyncWorker {

    private int COPY_BUFFER_SIZE = 1 * 1024 * 1024;
    /**
     * Where to start the recursive Sync
     */
    private Path localRootPath;
    private String remoteRootPath;

    private SftpConnection sftpConnection;
    private List<SyncFilePair> localFiles;
    private AtomicLong localFilesSizeComplete = new AtomicLong(0l);

    private List<SyncFilePair> filesToPush = new ArrayList<>();
    private AtomicLong filesToPushSizeComplete = new AtomicLong(0l);

    private List<SyncFilePair> pushedFiles = new ArrayList<>();


    public FolderSyncWorker(Path localRootPath, String sftpHost, String sftpUser, String sftpPassword, int sftpPort, String sftpRootPath) {
        this.localRootPath = localRootPath;
        this.remoteRootPath = sftpRootPath;
        sftpConnection = new SftpConnection(sftpHost, sftpUser, sftpPassword, sftpPort);
    }

    public void setCopyBufferSize(int size) {
        COPY_BUFFER_SIZE = size;
    }

    public List<SyncFilePair> go() {
        indexLocalFiles();
        findFilesToSync();
        pushFoundFiles();
        return pushedFiles;
    }

    private void pushFoundFiles() {
        System.out.println("--- Pushing Files..");
        StopWatch transferredBytesStopWatch = new StopWatch(filesToPushSizeComplete.get());
        StopWatch transferredFilesStopWatch = new StopWatch(filesToPush.size());
        List<Double> speeds = new ArrayList<>();


        for (SyncFilePair filePair : filesToPush) {
            System.out.println("");
            StopWatch uploadStopWatch = new StopWatch();
            try {
                uploadLocalFileToRemote(filePair);
                pushedFiles.add(filePair);
            } catch (IOException e) {
                e.printStackTrace();
            }

            double speed = (double) ((double) filePair.getLocal().getSize() / ((double) uploadStopWatch.getElapsedTimeMillis() / 1000.0));
            speeds.add(speed);

            transferredFilesStopWatch.increment();

            transferredBytesStopWatch.increment(filePair.getLocal().getSize());

            System.out.println("------");

            printOverallProgress(transferredBytesStopWatch, transferredFilesStopWatch, speeds);
        }
    }

    public List<SyncFilePair> getPushedFiles() {
        return pushedFiles;
    }

    private void findFilesToSync() {
        System.out.println("--- Searching files that should be pushed...");

        StopWatch filesProcessedStopwatch = new StopWatch(localFiles.size());
        AtomicLong bytesProcessed = new AtomicLong(0l);

        final String s = "Processed: %d (%s) / %d (%s), Need Sync: %d (%s), Duration: %s, Remaining: %s        \r";

        localFiles.forEach(filePair -> {
            if (shouldSync(filePair)) {
                markForSync(filePair);
            }

            filesProcessedStopwatch.increment();
            bytesProcessed.addAndGet(filePair.getLocal().getSize());

            System.out.print(String.format(s,
                    filesProcessedStopwatch.getProcessed(), DPHelpers.formatBytes(bytesProcessed.get()),
                    filesProcessedStopwatch.getTotalCount(), DPHelpers.formatBytes(localFilesSizeComplete.get()),
                    filesToPush.size(), DPHelpers.formatBytes(filesToPushSizeComplete.get()),
                    DPHelpers.formatDuration(filesProcessedStopwatch.getElapsedTimeMillis(), DPHelpers.DurationFormat.dhms),
                    DPHelpers.formatDuration(filesProcessedStopwatch.getEstimatedRemainingTimeMillis(), DPHelpers.DurationFormat.dhms)
            ));
        });

        System.out.println("");
    }

    private void markForSync(SyncFilePair filePair) {
        filesToPush.add(filePair);
        filesToPushSizeComplete.addAndGet(filePair.getLocal().getSize());
    }

    private void printOverallProgress(StopWatch transferredBytesStopwatch, StopWatch syncedFilesCountStopwatch, List<Double> speeds) {
        String s = "## ";
        s += " Files: " + syncedFilesCountStopwatch.getProcessed() + " / " + syncedFilesCountStopwatch.getTotalCount();
        s += ", ";
        s += "Size: " + DPHelpers.formatBytes(transferredBytesStopwatch.getProcessed()) + " / " + DPHelpers.formatBytes(transferredBytesStopwatch.getTotalCount());
        s += ", ";
        s += "Time: " + DPHelpers.formatDuration(transferredBytesStopwatch.getElapsedTimeMillis(), DPHelpers.DurationFormat.dhms) + " ";
        s += "Remaining: " + DPHelpers.formatDuration(transferredBytesStopwatch.getEstimatedRemainingTimeMillis(), DPHelpers.DurationFormat.dhms);
        s += ", ";
        s += "Avg.Speed: " + DPHelpers.formatBytes(getAvgSpeed(speeds)) + "/s  \r";

        System.out.print(s);
    }

    private double getAvgSpeed(List<Double> speeds) {
        double speedTotal = 0;
        for (Double d : speeds) {
            speedTotal += d;
        }
        return speedTotal / speeds.size();
    }

    private void indexLocalFiles() {
        System.out.println("--- Searching local files...");
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
        System.out.println("--------------------------");
        System.out.println("Uploading " + filePair.getLocal().getPath() + ", " + DPHelpers.formatBytes(filePair.getLocal().getSize()));

        filePair.getRemote().createParentDir();

        StopWatch stopWatch = new StopWatch(filePair.getLocal().getSize());
        try (InputStream in = new BufferedInputStream(filePair.getLocal().getInputStream()); OutputStream out = new BufferedOutputStream(filePair.getRemote().getOutputStream())) {
            byte[] buf = new byte[COPY_BUFFER_SIZE];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
                stopWatch.increment(len);
                printFileProgress(stopWatch);
            }
        }
        //Add 1000ms to the resulting filetime because sftp will make a floor to the seconds, so the reulting file would always be "older" than the original file.
        filePair.getRemote().setLastModifiedTime(filePair.getLocal().getLastModifiedTime() + 1000l);

        System.out.println("");
    }

    private void printFileProgress(StopWatch stopWatch) {

        double speed = (double) ((double) stopWatch.getProcessed() / (double) (stopWatch.getElapsedTimeMillis() / 1000.0));

        System.out.print("File Upload: "
                + DPHelpers.formatBytes(stopWatch.getProcessed()) + " / " + DPHelpers.formatBytes(stopWatch.getTotalCount())
                + " Time elapsed: " + DPHelpers.formatDuration(stopWatch.getElapsedTimeMillis(), DPHelpers.DurationFormat.dhms)
                + " Time remaining: " + DPHelpers.formatDuration(stopWatch.getEstimatedRemainingTimeMillis(), DPHelpers.DurationFormat.dhms)
                + " Speed: " + DPHelpers.formatBytes(speed) + "/s"
                + "      \r");
    }


}
