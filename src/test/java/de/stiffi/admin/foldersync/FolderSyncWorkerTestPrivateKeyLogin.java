package de.stiffi.admin.foldersync;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FolderSyncWorkerTestPrivateKeyLogin {

    @BeforeClass
    public static void startSftpServer() throws IOException, InterruptedException {
        stopSftpServer();

        String command = "docker run -d --name sftpserver -p 1022:22 -v "
                + getPublicKeyPath() + ":/home/foo/.ssh/keys/id_rsa.pub:ro " +
                " atmoz/sftp foo:pass:::upload";
        System.out.println(command);
        Runtime.getRuntime().exec(command);
        System.out.println("Sleep 120sek");
        //Thread.sleep(120000);
    }

    @AfterClass
    public static void stopSftpServer() throws IOException {
        Runtime.getRuntime().exec("docker rm -f sftpserver");
    }

    @Ignore
    @Test
    public void testLoginWithPrivateKey() throws IOException {
        FolderSyncWorker me = new FolderSyncWorker(getLocalTestFilesPath(),
                FolderSyncWorkerTest.SFTP_HOST,
                FolderSyncWorkerTest.SFTP_USER,
                getPrivateKeyPath(),
                22,
                FolderSyncWorkerTest.SFTP_ROOT_PATH);

    }

    @Test
    public void testLocalRootPath() throws IOException {
        System.out.println(getLocalRootPath());
    }


    private static Path getLocalRootPath() throws IOException {
        ClassLoader classLoader = FolderSyncWorkerTestPrivateKeyLogin.class.getClassLoader();
        Path resourcesPath = new File(classLoader.getResource(".").getFile()).toPath();
        return resourcesPath;
    }

    private static String getPublicKeyPath() throws IOException {
        return getLocalRootPath().resolve("id_rsa.pub").toString();
    }

    private static String getPrivateKeyPath() throws IOException {
        return getLocalRootPath().resolve("id_rsa").toString();
    }

    private Path getLocalTestFilesPath() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        Path resourcesPath = new File(classLoader.getResource(".").getFile()).toPath();
        Path testFilesPath = resourcesPath.resolve("testfiles");
        Files.createDirectories(testFilesPath);
        return testFilesPath;
    }


}
