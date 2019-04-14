# small-speech

Quickly record and replay later audio notes.
This app does not support very long audio files, but it's very convenient to quickly record and save a few seconds audio note.
It should work on old devices as well (not tested though).

When replaying a previous audio note, the path to the raw audio file (16kHz, mono, 16 bits LE) is copied into the clipboard.

It's a work in progress for now.

## Details

Every audio record is saved in a file with a timestamped name.
All these WAV files can be FTP'ed into a FTP server by pressing "export WAV".
The FTP server name must be initially entered by pressing "settings".
It must allow anonymous uploads into the directory /upload/.

All the audio files can be deleted on the Android device by pressing "clear".

## Features

- lightweight
- no tracker, no pub, no dependence to google services, open source - totally safe for your privacy !
- compatible old devices (from Android gingerbread)

