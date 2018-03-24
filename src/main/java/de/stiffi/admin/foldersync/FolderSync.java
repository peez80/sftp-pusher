package de.stiffi.admin.foldersync;

import de.stiffi.DPHelpers.CommandLineParser.Argument;
import de.stiffi.DPHelpers.CommandLineParser.CommandLineParser;
import de.stiffi.DPHelpers.CommandLineParser.Option;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FolderSync {
    public static void main(String[] args) {
        CommandLineParser cmd= new CommandLineParser();
        cmd.add(new Argument("localRoot", "Local Directory where the files reside that should be backed up"));
        cmd.add(new Option("sftphost", "SFTP Hostname", "Hostname of the target SFTP server", true));
        cmd.add(new Option("sftpuser", "SFTP Username", "Username to log into the target SFTP Server", true));
        cmd.add(new Option("sftppass", "SFTP Password", "Password to log into the target SFTP Server", true));
        cmd.add(new Option("sftpport", "SFTP Port", "Port to use for the target SFTP server", true));
        cmd.add(new Option("sftproot", "SFTP Root Dir", "Root Directory to place the backed up files on remote SFTP Server", true));

        if (!cmd.validate(args)) {
            System.out.println("Usage: java -jar FolderSync.jar " + cmd.getUsage());
            System.exit(1);
        }


        Path localRoot = Paths.get(cmd.getArgument("localRoot").getValue());
        String host = cmd.getOption("sftphost").getValue();
        String user = cmd.getOption("sftpuser").getValue();
        String pass = cmd.getOption("sftppass").getValue();
        int port = Integer.parseInt(cmd.getOption("sftpport").getValue());
        String remoteRoot = cmd.getOption("sftproot").getValue();


        FolderSyncWorker me = new FolderSyncWorker(localRoot, host, user, pass, port, remoteRoot);
        me.go();

        System.exit(0);
    }
}
