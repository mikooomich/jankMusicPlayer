# jankMusicPlayer
 
Hello and welcome to my jank music player.


## Features
- It plays music... hopefully...
- Attempts to reduce disk accesses by loading songs into memory and not unloading them
- Supports all formats your FFMpeg build supports
- A usable GUI (eventually)

## Boring Technical Details
Audio is transcoded into WAV (byte array) via FFMpeg pipe (`ffmpeg -i <input file> -f wav pipe:1`), and album arts in PNG (`ffmpeg -i <input file> -an -vframes 1 -c:v png -f image2pipe -`). While extracting album art, 
metadata is read using the FFMpeg "error" output. Audio is played via SourceDataLine. 

A copy of the audio data is 
stored in memory, after the song is first played (or if preloaded). This serves as a "cache" to avoid decoding 
the audio files again in the same session.
You can "precompile" ALL songs if you wish to take on Google Chrome in the realm of memory usage.

#### Why are you using SourceDataLine when Clip supports WAV?
For whatever reason FFMpeg piping messes up something and cause unsupported format error or Audio Data < 0. 
However, playing a WAV file converted cia FFMpeg terminal commands is fine. IDK.


## Requirements
- FFMpeg via system path


## Notes
- Music library path, recursive scanning, FFMpeg, etc., is currently hardcoded until a config system is implemented.
- The GUI is very pretty and *totally* not extremely proof of concept.
- Tested with the version [latest FFMpeg dev build](https://www.gyan.dev/ffmpeg/builds/). As of the time of writing, 2023-05-15-git-2953ebe7b6. 
- Using older versions may or may not cause issues. Ex. 5.1.2 may cause such as metadata image extraction deadlock... or others.


### Error codes (Song validity status)
- Integer.MINIMUM_VALUE - no code assigned
- 0 - No error
- -1 - Invalid audio file
- 20 - Metadata reader error

