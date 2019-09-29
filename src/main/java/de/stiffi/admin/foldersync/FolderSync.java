package de.stiffi.admin.foldersync;


import com.google.common.base.Strings;
import org.apache.commons.cli.*;
import org.apache.commons.codec.binary.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FolderSync {

    public static final String LOCAL_ROOT = "localroot";
    public static final String SFTPHOST = "sftphost";
    public static final String SFTPUSER = "sftpuser";
    public static final String SFTPPASS = "sftppass";
    public static final String SFTPPRIVATEKEYFILE = "privatekeyfile";
    public static final String SFTPPORT = "sftpport";
    public static final String SFTPROOT = "sftproot";

    private static boolean isCommandValid(CommandLine cmd) {
        boolean good = cmd.hasOption(LOCAL_ROOT)
                && cmd.hasOption(SFTPHOST)
                && cmd.hasOption(SFTPPORT)
                && cmd.hasOption(SFTPROOT);

        if (good && cmd.hasOption(SFTPUSER) && !cmd.hasOption(SFTPPRIVATEKEYFILE) && !cmd.hasOption(SFTPPASS)) {
            good = false;
        }

        return good;
    }

    public static void main(String[] args) throws ParseException {

        Options options = new Options();
        options.addOption(LOCAL_ROOT, true, "Local Directory where the files reside that should be backed up");
        options.addOption(SFTPHOST, true, "Hostname of the target SFTP server");
        options.addOption(SFTPUSER, true, "Username to log into the target SFTP Server");
        options.addOption(SFTPPASS, true, "Password to log into the target SFTP Server");
        options.addOption(SFTPPORT, true, "Port to use for the target SFTP server");
        options.addOption(SFTPROOT, true, "Root Directory to place the backed up files on remote SFTP Server");
        options.addOption(SFTPPRIVATEKEYFILE, true, "Path to openssl private key file, PEM encoded");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (!isCommandValid(cmd)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("foldersync", options, true);
            System.exit(1);
        }


        Path localRoot = Paths.get(cmd.getOptionValue(LOCAL_ROOT));
        String host = cmd.getOptionValue(SFTPHOST);
        String user = cmd.getOptionValue(SFTPUSER);
        String passwordOrPrivateKeyFile = cmd.getOptionValue(SFTPPASS);
        int port = Integer.parseInt(cmd.getOptionValue(SFTPPORT));
        String remoteRoot = cmd.getOptionValue(SFTPROOT);
        String privateKeyFile = cmd.getOptionValue(SFTPPRIVATEKEYFILE);

        if (!Strings.isNullOrEmpty(privateKeyFile)) {
            passwordOrPrivateKeyFile = privateKeyFile;
        }


        FolderSyncWorker me = new FolderSyncWorker(localRoot, host, user, passwordOrPrivateKeyFile, port, remoteRoot);
        me.go();
        System.out.println("");
        System.out.println("SFTP Push done.");
        System.out.println("");

        System.exit(0);
    }
}
