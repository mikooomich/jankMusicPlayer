# jankMusicPlayer
 
Hello and welcome to my jank music player.


## Features
- It plays music... hopefully...
- Attempts to reduce disk accesses by loading songs into memory and not unloading them
- Supports all formats your FFMpeg build supports
- A usable GUI (eventually)

## Boring Technical Details
Audio is transcoded into WAV (byte array) via FFMpeg pipe, and album arts in PNG; audio is played via SourceDataLine. A copy of the audio data is 
stored in memory, after the song is first played (or if preloaded). This serves as a "cache" to avoid decoding 
the audio files again in the same session.
You can "precompile" ALL songs if you wish to take on Google Chrome in the realm of memory usage.

#### Why are you using SourceDataLine when Clip supports WAV?
For whatever reason FFMpeg piping messes up something and cause unsupported format error or Audio Data < 0. 
However, playing a WAV file converted cia FFMpeg terminal commands is fine. IDK.


## Requirements
- FFMpeg via system path


## Notes
- Music library path, recursive scanning, FFMpeg, etc., is currently hardcoded until a config system is implemented
- The GUI is very pretty and *totally* not extremely proof of concept