# FolderSync

I'm doing remote backup to a other's synology, but since rsync stopped working for some reason and cloudSync won't be able
to handle large files on a slow internet connection, I decided to write my own backup.

This backup "solutioin" is initially intended for my personal use only.

# What it does
It just takes a root directory and copies all new or changed files to a remote SFTP location. It is intended not to synchronize. This means that locally deleted files will remain in the target directory.
This saves me from e.g. my own dumbness, if I accidentially delete anything ;)


# Usage
Just run "java -jar foldersync.jar" and get a command line explanation. Or better run the docker image (that makes sure that you can mount only what you need):

docker run -it --rm peez/sftp-pusher