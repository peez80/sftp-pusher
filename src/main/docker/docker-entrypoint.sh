#!/bin/bash
/opt/foldersync/bin/foldersync -localroot /backuproot -sftphost ${SFTP_HOST} -sftpuser ${SFTP_USER} -sftppass ${SFTP_PASS} -sftpport ${SFTP_PORT} -sftproot ${SFTP_ROOT} -uselocaldb ${USE_LOCAL_DB}
