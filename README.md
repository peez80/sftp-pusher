# FolderSync

I'm doing remote backup to a other's synology, but since rsync stopped working for some reason and cloudSync won't be able
to handle large files on a slow internet connection, I decided to write my own backup.

This backup "solutioin" is initially intended for my personal use only.

# What it does
It just takes a root directory and copies all new or changed files to a remote SFTP location. It is intended not to synchronize. This means that locally deleted files will remain in the target directory.
This saves me from e.g. my own dumbness, if I accidentially delete anything ;)


# Usage
Currently it only runs via docker. Just mount the root dir of your backup (with readonly) to the container and set the sftp data via environment parameters:

    docker run -it --rm \
        -v <your_local_backup_root>:/backuproot:ro \
        -e SFTP_HOST=somehost \
        -e SFTP_USER=someuser \
        -e SFTP_PASS=somwpassword \
        -e SFTP_PORT=22 \
        -e SFTP_ROOT=somedir \
        peez/sftp-pusher