#!/bin/bash

# retrieve the raw audio files from the ftp server where the android app uploaded them

# put yout ftp server name here
HOST="ftpservername"
USER="anonymous"
PASSWD="anonymous"

ftp -n -v $HOST << EOT
ascii
user $USER $PASSWD
prompt
cd upload
mget recwav*
bye
EOT


