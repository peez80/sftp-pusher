package de.stiffi.admin.foldersync;

import de.stiffi.admin.foldersync.api.FileHandler;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.kohsuke.randname.RandomNameGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;


public class FolderSyncWorkerTest {

    private List<Path> testFilesRelativePaths = new ArrayList<>();

    private static SftpConnection sftpConnection;

    private static final String SFTP_HOST = "localhost";
    private static final String SFTP_USER = "foo";
    private static final String SFTP_PASS = "pass";
    private static final int SFTP_PORT = 22;
    private static final String SFTP_ROOT_PATH = "/upload";




    @BeforeClass
    public static void setupClass() throws IOException, InterruptedException {
        startSftpContainer();
        setupVerifySftpConnection();
    }



    @AfterClass
    public static void stopSftpContainer() throws IOException, InterruptedException {
        Runtime.getRuntime().exec("docker rm -f sftptest");
        Thread.sleep(1000);
    }


    @Test
    public void testConnect() throws InterruptedException, IOException {
        //Given

        //When
        FolderSyncWorker me = new FolderSyncWorker(getLocalRootPath(), SFTP_HOST, SFTP_USER, SFTP_PASS, SFTP_PORT, SFTP_ROOT_PATH);
    }

    @Test
    public void testSync() throws IOException {
        //Given
        FolderSyncWorker me = new FolderSyncWorker(getLocalRootPath(), SFTP_HOST, SFTP_USER, SFTP_PASS, SFTP_PORT, SFTP_ROOT_PATH);

        //When
        List<SyncFilePair> syncedFiles = me.go();

        //Then
        Assert.assertEquals(testFilesRelativePaths.size(), syncedFiles.size());
        for (Path relativePath : testFilesRelativePaths) {
            assertFileBackupped(relativePath);
        }
    }

    @Test
    public void testSyncAndOnlyNewFile() throws IOException {
        //Given
        FolderSyncWorker me = new FolderSyncWorker(getLocalRootPath(), SFTP_HOST, SFTP_USER, SFTP_PASS, SFTP_PORT, SFTP_ROOT_PATH);
        me.go();


        //When
        Path testFileAbsolutePath= createLocalTestFile("some/other/path/myNewFile.txt", 10);
        me = new FolderSyncWorker(getLocalRootPath(), SFTP_HOST, SFTP_USER, SFTP_PASS, SFTP_PORT, SFTP_ROOT_PATH);
        List<SyncFilePair> syncedFiles = me.go();


        //Then
        Assert.assertEquals(1, syncedFiles.size());
        assertFileBackupped(getLocalRootPath().relativize(testFileAbsolutePath));
    }

    @Test
    public void testSyncFileSizeChangedFile() throws IOException {
        //Given
        FolderSyncWorker me = new FolderSyncWorker(getLocalRootPath(), SFTP_HOST, SFTP_USER, SFTP_PASS, SFTP_PORT, SFTP_ROOT_PATH);
        me.go();


        //When
        Path testFileAbsolutePath= createLocalTestFile(testFilesRelativePaths.get(0).toString(), 20000);
        me = new FolderSyncWorker(getLocalRootPath(), SFTP_HOST, SFTP_USER, SFTP_PASS, SFTP_PORT, SFTP_ROOT_PATH);
        List<SyncFilePair> syncedFiles = me.go();


        //Then
        Assert.assertEquals(1, syncedFiles.size());
        assertFileBackupped(testFilesRelativePaths.get(0));
    }

    @Test
    public void testSyncModifiedTimeChangedFile() throws IOException {
        //Given
        FolderSyncWorker me = new FolderSyncWorker(getLocalRootPath(), SFTP_HOST, SFTP_USER, SFTP_PASS, SFTP_PORT, SFTP_ROOT_PATH);
        me.go();


        //When
        Files.setLastModifiedTime(getLocalRootPath().resolve(testFilesRelativePaths.get(2)), FileTime.from(LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC)));
        me = new FolderSyncWorker(getLocalRootPath(), SFTP_HOST, SFTP_USER, SFTP_PASS, SFTP_PORT, SFTP_ROOT_PATH);
        List<SyncFilePair> syncedFiles = me.go();


        //Then
        Assert.assertEquals(1, syncedFiles.size());
        assertFileBackupped(testFilesRelativePaths.get(0));
    }

    @Test
    public void testNoAdditionalSyncNecessary() throws IOException {
        //Given
        FolderSyncWorker me = new FolderSyncWorker(getLocalRootPath(), SFTP_HOST, SFTP_USER, SFTP_PASS, SFTP_PORT, SFTP_ROOT_PATH);
        me.go();


        Files.setLastModifiedTime(getLocalRootPath().resolve(testFilesRelativePaths.get(2)), FileTime.from(LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC)));
        me = new FolderSyncWorker(getLocalRootPath(), SFTP_HOST, SFTP_USER, SFTP_PASS, SFTP_PORT, SFTP_ROOT_PATH);
        me.go();

        //When
        me = new FolderSyncWorker(getLocalRootPath(), SFTP_HOST, SFTP_USER, SFTP_PASS, SFTP_PORT, SFTP_ROOT_PATH);
        List<SyncFilePair> syncedFiles = me.go();

        //Then
        Assert.assertEquals(0, syncedFiles.size());
    }

    private Path getLocalRootPath() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        Path resourcesPath = new File(classLoader.getResource(".").getFile()).toPath();
        Path testFilesPath = resourcesPath.resolve("testfiles");
        Files.createDirectories(testFilesPath);
        return testFilesPath;
    }

    @Before
    public void setupTestDirectory() throws IOException {

        FileUtils.cleanDirectory(getLocalRootPath().toFile());

        testFilesRelativePaths = new ArrayList<>();

        testFilesRelativePaths.add(getLocalRootPath().relativize(createLocalTestFile("folder1/sub1/nextsub/file1.txt", 1000)));
        testFilesRelativePaths.add(getLocalRootPath().relativize(createLocalTestFile("folder1/sub2/file2.txt", 3000)));
        testFilesRelativePaths.add(getLocalRootPath().relativize(createLocalTestFile("folder1/sub2/file3.txt", 1500)));
        testFilesRelativePaths.add(getLocalRootPath().relativize(createLocalTestFile("folder2/file4.txt", 5000)));
    }

    /**
     * Create File with random content - append if file already exists, create dir, if dir not exists
     *
     * @param relativePath
     * @param size
     * @return Absolute Path of created File
     */
    private Path createLocalTestFile(String relativePath, long size) throws IOException {
        Path path = getLocalRootPath().resolve(relativePath);
        Path parentDir = path.getParent();

        Files.createDirectories(parentDir);
        RandomNameGenerator rng = new RandomNameGenerator();
        StringBuilder sb = new StringBuilder();
        while (sb.length() < size) {
            sb.append("_").append(rng.next());
        }

        Files.write(path, sb.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        Instant fileDate = LocalDateTime.now().minusMinutes(20l).toInstant(ZoneOffset.UTC);

        Files.setLastModifiedTime(path, FileTime.from(fileDate));
        return path;
    }

    private static void startSftpContainer() throws IOException, InterruptedException {
        stopSftpContainer();
        System.out.println("Starting SFTP Docker");
        Runtime.getRuntime().exec("docker run -itd --name sftptest -p 22:22 -d atmoz/sftp foo:pass:::upload");
        Thread.sleep(4000);
    }

    private static void setupVerifySftpConnection() {
        sftpConnection = new SftpConnection(SFTP_HOST, SFTP_USER, SFTP_PASS, SFTP_PORT);
    }

    private void assertFileBackupped(Path relativePath) throws IOException {
        Path localPath = getLocalRootPath().resolve(relativePath);
        String remotePath = (SFTP_ROOT_PATH + "/" + relativePath.toString()).replace("\\", "/");
        FileHandler remote= sftpConnection.getFileHandler(remotePath);

        Assert.assertEquals(Files.exists(localPath), remote.exists());
        Assert.assertEquals(Files.size(localPath), remote.getSize());
        Assert.assertTrue(Files.getLastModifiedTime(localPath).toMillis() < remote.getLastModifiedTime());
    }
}