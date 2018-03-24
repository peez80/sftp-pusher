package de.stiffi.admin.foldersync;


import org.apache.commons.cli.*;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FolderSync {

    public static final String LOCAL_ROOT = "localroot";
    public static final String SFTPHOST = "sftphost";
    public static final String SFTPUSER = "sftpuser";
    public static final String SFTPPASS = "sftppass";
    public static final String SFTPPORT = "sftpport";
    public static final String SFTPROOT = "sftproot";

    private static boolean isCommandValid(CommandLine cmd) {
        return cmd.hasOption(LOCAL_ROOT)
                && cmd.hasOption(SFTPHOST)
                && cmd.hasOption(SFTPUSER)
                && cmd.hasOption(SFTPPASS)
                && cmd.hasOption(SFTPPORT)
                && cmd.hasOption(SFTPROOT);
    }

    public static void main(String[] args) throws ParseException {

        Options options = new Options();
        options.addOption(LOCAL_ROOT, true, "Local Directory where the files reside that should be backed up");
        options.addOption(SFTPHOST, true, "Hostname of the target SFTP server");
        options.addOption(SFTPUSER, true, "Username to log into the target SFTP Server");
        options.addOption(SFTPPASS, true, "Password to log into the target SFTP Server");
        options.addOption(SFTPPORT, true, "Port to use for the target SFTP server");
        options.addOption(SFTPROOT, true, "Root Directory to place the backed up files on remote SFTP Server");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse( options, args);

        if (!isCommandValid(cmd)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "foldersync", options,  true);
            System.exit(1);
        }



        Path localRoot = Paths.get(cmd.getOptionValue(LOCAL_ROOT));
        String host = cmd.getOptionValue(SFTPHOST);
        String user = cmd.getOptionValue(SFTPUSER);
        String pass = cmd.getOptionValue(SFTPPASS);
        int port = Integer.parseInt(cmd.getOptionValue(SFTPPORT));
        String remoteRoot = cmd.getOptionValue(SFTPROOT);


        FolderSyncWorker me = new FolderSyncWorker(localRoot, host, user, pass, port, remoteRoot);
        me.go();

        System.exit(0);
    }
}
