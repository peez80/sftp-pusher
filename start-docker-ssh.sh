#!/bin/bash
docker rm -f sftpserver

docker run -itd --name sftpserver -p 22:22 -d atmoz/sftp foo:pass:::upload