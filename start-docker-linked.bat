#!/bin/bash
docker network create sftptest
docker rm -f sftpserver
docker run -itd --name sftpserver --network sftptest -p 22:22 -d atmoz/sftp foo:pass:::upload

#docker build -t peez/sftp-push .

docker run -it --network sftptest -v /c/Users/peez/Desktop:/backuproot:ro -e SFTP_HOST=sftpserver -e SFTP_USER=foo -e SFTP_PASS=pass -e SFTP_PORT=22 -e SFTP_ROOT=upload peez/sftp-push


