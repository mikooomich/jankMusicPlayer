package wah.mikooo.MediaPlayer;


import wah.mikooo.Ui.MainWindow;

import javax.sound.sampled.*;
import java.io.*;

import static wah.mikooo.ffmpegStuff.ffmpegWrapper.ffmpegOwO;
import static wah.mikooo.ffmpegStuff.ffmpegWrapper.ffmpegOwOStream;


public class Player implements Runnable {


	public static boolean autoplay = Boolean.parseBoolean(MainWindow.config.retrieve("autoplay")); // play next song after end

	// user settings
	static boolean USE_LYRICS = false;
	boolean prevIsRestart = true; // previous command restarts song from beginning
	public static float defaultVolume = Float.parseFloat(MainWindow.config.retrieve("volume"));

	public static boolean useStreaming;
	public static boolean preferStreaming;

	public static String ffmpegBinary = MainWindow.config.retrieve("ffmpeg");
	public static String ffprobeBinary = MainWindow.config.retrieve("ffprobe");


	// internal player variables
	public static SongBoard sb; // queues and available songs
	public static boolean paused = false; // paused status
	static boolean prevRequest = false; // request for a next/prev command
	static boolean nextRequest = false;
	static boolean forceWait = false; // force dispatcher to wait

	public static Mouth mouth;
	static Thread mouthThread;


	/**
	 * Initialize with a SongBoard
	 *
	 * @param sb
	 */
	public Player(SongBoard sb) {
		this.sb = sb;
	}


	/**
	 * Play a song
	 *
	 * @param s song object
	 */
	private void player(Song s) {
		System.out.println("playing " + s.path);

		try {
			// kill old mouth, create new
			if (mouth != null) {
				mouth.kill();
			}

			mouth = null;
			mouthThread = null;
			mouth = new Mouth(s);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	/**
	 * Notify everything
	 */
	public synchronized void forceRecheck() {
		notifyAll();
		try {
			if (!mouth.playing) { // will deadlock if called and mouth is not in wait
				mouth.ayoWakeUp();
			}
		} catch (Exception e) {
//            e.printStackTrace();
			System.out.println("mouth is null lol");
		}

	}


	/**
	 * =========================
	 * INTERFACE FUNCTIONALITIES
	 * =========================
	 */

	/**
	 * Set volume.
	 * Artificially limited at +5db
	 *
	 * @param volume
	 */
	public void changeVolume(float volume) {
		if (volume > 5) {
			volume = 5;
		}
		if (mouth == null || mouth.getLine() == null) {
			return;
		}
		// volume
		FloatControl gainControl = (FloatControl) mouth.getLine().getControl(FloatControl.Type.MASTER_GAIN);
		gainControl.setValue(volume);

		defaultVolume = volume;
	}


	/**
	 * Play function, set paused status to false
	 */
	public void play() {
		System.out.println("register play");
		paused = false;
//        forceWait = false;
		forceRecheck();
	}

	/**
	 * Pause function, set paused status to true
	 */
	public void pause() {
		System.out.println("register pause");
		paused = true;
//        forceWait = true;
		forceRecheck();
	}

	/**
	 * Enqueue all songs
	 */
	public void playAll() {
		sb.enqueue(sb.availSongs);
	}


	/**
	 * Next function
	 * Sends a signal to mouth to shut up, and send set parameters for mouth dispatcher.
	 * Mouth will dispatch will wake up the mouth.
	 */
	public void next() {
		// send a signal to stop playing, wake the mouth dispatcher
		paused = false;
		nextRequest = true;
		if (mouth != null) {
			mouth.kill();
			mouth.ayoWakeUp();
		}

		System.out.println("Next command triggering wake up request");
	}


	public void prev() {
		paused = false;
		prevRequest = true;
		if (mouth != null) {
			mouth.kill();
			mouth.ayoWakeUp();
		}
		System.out.println("PREV command triggering wake up request");
	}


	/**
	 * Jump to a position in the queue
	 *
	 * @param targetIndex
	 */
	public void jumpto(int targetIndex) {
		int indexesToJump = targetIndex - sb.getCurrentIndex();

		if (indexesToJump > sb.getCurrentQueueSize() || indexesToJump + sb.getCurrentQueueSize() < 1) {
			throw new IndexOutOfBoundsException("Cannot jump to index " + targetIndex + ", max " + sb.getCurrentQueueSize() + ", min " + (sb.getCurrentIndex() - 1));
		}

//        System.out.println("JUMPING THIS MANY INDEXES " + indexesToJump  + "' FROM " + sb.getCurrentIndex());
		if (indexesToJump == 0) {
			// do nothing
//            System.out.println("Jumping finished " + sb.getCurrentlyPlaying().path);
			return;
		} else if (indexesToJump > 0) { // skipping 1 song is equivalent to calling next
			while (indexesToJump > 0) {
				sb.getNext(); // this next function does not dispatch mouth
				indexesToJump--;
			}
			next(); // when there is one song to skip left, call the next command to start playing
		} else if (indexesToJump < 0) { // skipping 1 song is equivalent to calling next
			while (indexesToJump < -1) {
				sb.getPrev(); // this next function does not dispatch mouth
				indexesToJump--;
			}
			prev(); // when there is one song to skip left, call the next command to start playing
		}


//        System.out.println("Jumping finished " + sb.getCurrentlyPlaying().path);
	}


	/**
	 * Jump to a part in the song
	 *
	 * @param ms Milliseconds since the current position of the song
	 */
	public void seekTo(int ms) {
		try {
			mouth.seekInSong(ms);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("very bad error when seeking");
		}
	}


	/**
	 * Set autoplay setting. This sets it to !autoplay
	 */
	public void setAutoplay() {
		setAutoplay(!autoplay);
	}

	/**
	 * Set autoplay setting
	 *
	 * @param value
	 */
	public void setAutoplay(boolean value) {
		autoplay = value;
		System.out.println("Autoplay is set to " + autoplay);
	}


	/**
	 * Handle dispatching mouth thread.
	 * This was a great idea theoretical, however in practice it
	 * is just messy and very, VERY error-prone. Wah.
	 * REMINDER: implement something to kill this thread once done
	 *
	 * @throws InterruptedException
	 */
	public synchronized void mouthDispatcher() throws InterruptedException {
		System.out.println("a");
		while (true) {

			inner:
			while (true) {

//            while (forceWait) {
//                wait();
//                forceWait = false;
//            }


				// primary wait case;
				// aka: stop playing when paused or nothing to play
				while (paused || sb.getCurrentlyPlaying() == null) {
					System.out.println("ENTERING primary wait case");
					wait(); // sleep until woken
					System.out.println("LEAVING primary wait case");
					break inner;
				}

				System.out.println("skipped primary wait");


				try {
					// dispatch song when reached end of audio stream
					if (!prevRequest && !nextRequest) { // skip if need process next request

						if (mouth != null && mouth.atEnd()) {
							System.out.println("Detect end of song, Playing new track");
							player(sb.getCurrentlyPlaying());
						} else if (mouth == null) {
							player(sb.getCurrentlyPlaying());
							System.out.println("Playing new track (first run)");
						}


						if (autoplay && mouth.atEnd()) {
							System.out.println("ENTERING autoplay wait");
							wait();
							System.out.println("LEAVING autoplay wait");
						}

					}


					/**
					 * "Why is this case here twice?"
					 * The former dispatches mouth, the latter modifies the queue.
					 * The actions are separated instead of being in one massive code block
					 */
					if (mouth != null && mouth.atEnd()) { // modify queue when playback ends

						if (paused) { // go to primary wait case
							System.out.println("Paused, time for primary wait");
							break inner;
						} else if (prevRequest) {
							if (sb.peekPrev() != null && !prevIsRestart) {
								System.out.println(sb.getPrev().path + " (prev requested)");
							}
							prevRequest = false;
							nextRequest = false;
							mouth.kill();
							break inner;

						} else if (nextRequest) { // assume reached end of song, dispatch song next loop run (easier understood than explained idk)
							if (sb.peekNext() != null) {
								System.out.println(sb.getNext().path + " (next requested)");
							}
							nextRequest = false;
							prevRequest = false;
							break inner;
						}
					}

					// wait unit next dispatch call
					wait();
				} catch (InterruptedException e) {
					System.exit(0); // assume interrupt means go terminate for now
					throw new RuntimeException(e);
				}


			} // inner while
		} // loop while

	}

	@Override
	public void run() {
		System.out.println("Starting player thread");
		try {
			mouthDispatcher();
		} catch (InterruptedException e) {
			System.exit(0); // assume interrupt means go terminate for now
			throw new RuntimeException(e);
		}
		System.out.println("Ending player thread");
	}


	/**
	 * Need to make multi thread, worker threads, need to acquire lock
	 * to only have one thing touching a song
	 */
	public void loadAllSongsIntoMemoryThisIsAVeryBadIdea() {
		sb.availSongs.stream().forEach(song -> {
			if (song.getAudio() == null) {
				song.setAudio(ffmpegOwO(song.path));
			}
		});
		System.out.println("loadAllSongsIntoMemoryThisIsAVeryBadIdea() method finish");
	}


	public class Mouth implements Runnable {

		private String fileName;
		private long length; // song length in ms

		boolean goDieNow = false;
		boolean atEnd = false;
		boolean playing = false;
		boolean hotRestart = false;

		SourceDataLine line;
		static AudioInputStream aas = null;

		static int totalBytes = 0; // total bytes "played"

		Song currSong;

		/**
		 * Create a Mouth
		 *
		 * @param path
		 * @param useStreaming
		 * @param preferStreaming
		 */
		public Mouth(Song path, boolean useStreaming, boolean preferStreaming) {
			Player.useStreaming = useStreaming;
			if (!useStreaming) { // auto disable prefer streaming
				Player.preferStreaming = false;
			} else {
				Player.preferStreaming = preferStreaming;
			}

			currSong = path;
			fileName = path.path;
			length = path.length;
			mouthThread = new Thread(this);
			mouthThread.start();
		}

		/**
		 * Create a Mouth with default seStreaming = true, preferStreaming = false
		 *
		 * @param path
		 */
		public Mouth(Song path) {
			this(path, true, false);
		}


		/**
		 * Get the current position in the song in miliseconds since beginning. -1 will be returned if there is no data
		 *
		 * @return
		 */
		public static long getCurrentPosMs() {
			// -1 is unavalible / no data
			if (aas != null) {
				return (long) (totalBytes / aas.getFormat().getFrameSize() / aas.getFormat().getFrameRate() * 1000);
			}

			return -1;
		}

		/**
		 * Send signal to stop talking
		 */
		public void kill() {
			goDieNow = true;
			if (line != null) {
				line.close();
			}
			sb.getCurrentlyPlaying().lyrics.killSession(); // kill lyric printer
		}


		/**
		 * Return whether or not the song has finished playing
		 *
		 * @return
		 */
		public boolean atEnd() {
			if (atEnd) {
				return true;
			} else {
				return false;
			}
		}

		/**
		 * Wakeup (notify) Mouth
		 */
		public synchronized void ayoWakeUp() {
			this.notify();
		}

		/**
		 * Return line (for volume control)
		 *
		 * @return
		 */
		public Line getLine() {
			return line;
		}


		/**
		 * Play sound.
		 * If useStreaming is true, first lookup memory for song, then use streaming as a fallback if required.
		 * If useStreaming is false, the user must manually do ffmpeg
		 * If preferStreaming is true, we will always ffmpeg stream the song.
		 * <p>
		 * While streaming, the song is directly streamed from ffmpeg, and a copy of the song will be saved in memory.
		 */
		synchronized void audioSession() {

			InputStream audioStream = null;
			byte[] audioFile = null;

			System.out.println("Streaming fallback is set to: " + useStreaming);
			System.out.println("Prefer Streaming is set to: " + preferStreaming);

			// look up in memory first
			if (!preferStreaming) {
				System.out.println("Seeing if we have");
				audioFile = sb.getCurrentlyPlaying().getAudio(); // lookup, retrieve
			}

			// fallback to streaming
			if (audioFile == null || preferStreaming) {
				audioStream = ffmpegOwOStream(fileName);


				Thread thread = new Thread(new Runnable() {
					@Override
					public void run() {
						// load into memory. Hi this is a terrible hack and I should just feed the one input stream into 2 outputs... but here we are
						sb.getCurrentlyPlaying().setAudio(ffmpegOwO(fileName));
						/**
						 *   remember to create write lock in songboard
						 *   or will end up writing when not supposed to
						 */
					}
				});
				thread.start();
			}


			try {

				if (audioFile == null) { // streaming fallback
					System.out.println("Using direct streaming");
					int attempts = 1;

					// attempt a maximum of 3 times to get a stream
					while (attempts < 4) {
						try {
							aas = AudioSystem.getAudioInputStream(audioStream);
							break;
						} catch (Exception e) {
							System.out.println("Failed to get stream, retrying: " + attempts);
						}
						attempts++;
					}
					Thread.sleep(1000);

				} else { // we have the full song already loaded
					System.out.println("Using loaded stream");
					aas = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioFile));
				}

				// bail if could not get something valid to play
				if (aas == null) {
					throw new Exception("Could not get stream.");
				}

				System.out.println(aas.getFormat());
				System.out.println(aas.markSupported());
				System.out.println(aas.getFrameLength());

				// chat gpt my beloved
				// Open a source data line with the same format as the audio input stream
				DataLine.Info info = new DataLine.Info(SourceDataLine.class, aas.getFormat());
				line = (SourceDataLine) AudioSystem.getLine(info);
				line.open(aas.getFormat());
				line.start();


				/**
				 * Why are you using Line when Clip supports wav?
				 * For whatever reason ffmpeg piping messes up something and
				 * cause unsupported format error or Audio Data < 0.
				 * But playing a wav file you converted with ffmpeg via terminal commands is fine.
				 * Idk, AudioInputStream reads as a valid file.
				 */


				// volume control
				FloatControl gainControl = (FloatControl) mouth.getLine().getControl(FloatControl.Type.MASTER_GAIN);
				gainControl.setValue(defaultVolume);

				// Write audio data to the line buffer
				byte[] buffer = new byte[4096];
				int bytesRead;
				playing = true;


				// variable for hopefully performance
				float frameRate = aas.getFormat().getFrameRate();
				int frameSize = aas.getFormat().getFrameSize();
				System.out.println(frameRate + "werfouihjweouifhjweofiujweoifwjefoiweojf" + frameSize);
				long lastSecond = 0;
				boolean lyricsEnabled = USE_LYRICS && sb.getCurrentlyPlaying().lyrics != null;


				MainWindow.drawCenter();
				if (lyricsEnabled && sb.getCurrentlyPlaying().lyrics != null) {
					sb.getCurrentlyPlaying().lyrics.startSession(); // start lyric printer
				}
				MainWindow.redrawTitles(); // update ui

				// feed to audio system
				while ((bytesRead = aas.read(buffer)) != -1) {
					if (goDieNow) { // stop playing immediately
						line.close();
						break;
					}
					if (paused || forceWait) { // player requests pause
						playing = false;

						// tell lyric player to stop
						if (lyricsEnabled) {
							sb.getCurrentlyPlaying().lyrics.pause(getCurrentPosMs());
						}

						wait();
						System.out.println("i awoke");
						playing = true;
						sb.getCurrentlyPlaying().lyrics.resume(); // i truly hope that this doesn't become an issue where the the the if it decided to print lyrics if still paused. The alternative is spamming resume on *every* buffer copy
						continue; // avoid playing sound if still paused
					}
					totalBytes += bytesRead;
					line.write(buffer, 0, bytesRead);

					/**
					 * Update player seek bar ever new second
					 * I try to use variable instead of methods for speed lol
					 * Save lasts second, trigger update every second
					 */
					if (Math.floor((long) (totalBytes / frameSize / frameRate)) > lastSecond) {
//                        System.out.println(totalBytes / frameSize / frameRate);
						MainWindow.updateSongPos(currSong);
						lastSecond = (long) Math.floor((long) (totalBytes / frameSize / frameRate));
					}
				}


				atEnd = true;
				paused = false;
				playing = false;
				if (autoplay) {
					nextRequest = true; // signal next
				}
				// Stop and close the line
				System.out.println(1);
				line.drain();
				System.out.println(2);
				line.stop();
				System.out.println(3);
				line.close();
				aas = null;


				// restart from beginning. Instead of returning to dispatcher
				// Intended for seek use
				if (hotRestart) {
					System.out.println("HOT RESTARTING THE AUDIO");
					hotRestart = false;
					goDieNow = false;
					atEnd = false;
					playing = false;
					audioSession();
				}

				MainWindow.updateSongPos(currSong); // end of song, set ui to end
				System.out.println("Mouth is going to shut up forever now");
				forceRecheck();
			} catch (Exception e) {
				System.out.println("Critical Mouth failure");
				e.printStackTrace();
				throw new RuntimeException("Critical Mouth failure: " + e.getMessage());
			}
		}

		@Override
		public void run() {
			audioSession();
		}


		/**
		 * Absolute seek to place in track
		 *
		 * @param ms Time in milliseconds
		 */
		public void seekInSong(int ms) throws Exception {
			long currentTime = getCurrentPosMs();

			if (ms > currentTime) {
				// Seeking forward requires RELATIVE OFFSET
				System.out.println("JUMPING FWD to " + ms);
				fastFwd(ms - currentTime, aas.getFormat().getFrameSize(), aas.getFormat().getFrameRate());
			} else {
				// Seeking backward requires ABSOLUTE POSITION
				System.out.println("JUMPING BACK to " + ms);
				rewind(ms, aas.getFormat().getFrameSize(), aas.getFormat().getFrameRate());
			}
		}


		/**
		 * Seek FORWARD RELATIVE to current place in track
		 *
		 * @param ms        Time in milliseconds
		 * @param frameSize AKA bytes per frame
		 * @param frameRate AKA sample rate
		 * @throws Exception
		 */
		private void fastFwd(long ms, int frameSize, float frameRate) throws Exception {
			long bytePosition = (ms * frameSize * (int) frameRate) / 1000;

			while (aas == null && !goDieNow) {
				// blocking until valid audio session
				Thread.sleep(100);                 // this looks like a deadlock possibility? Allow goDieNow as a force exit?
			}
			totalBytes += bytePosition;
			System.out.println("skipped (bytes)" + aas.skip(bytePosition));
			if (USE_LYRICS) {
				sb.getCurrentlyPlaying().lyrics.startSession();
			}
		}


		/**
		 * Seek BACKWARD relative to ABSOLUTE POSITION in track
		 *
		 * @param ms        Time in milliseconds
		 * @param frameSize AKA bytes per frame
		 * @param frameRate AKA sample rate
		 * @throws Exception
		 */
		private void rewind(long ms, int frameSize, float frameRate) throws Exception {
			/**
			 * Basically due to the limits of DataLine (or my knowledge of it)
			 * The easiest way to implement a rewind is to restart the audio stream and seek forward
			 */


			// calculate current position in song
			long currentPosMs = getCurrentPosMs();

			// hot restart the player
			hotRestart = true;
			kill();

			System.out.println("frame size" + frameSize + " frame rate " + frameRate);
			System.out.println("length is " + length + "   cur pos " + currentPosMs + " result = " + (currentPosMs + ms) + "total bytes " + totalBytes);

			totalBytes = 0; // reset odometer
			if (ms <= 0 || ms >= length) {
				// skip to next song/restart current if OOB
				return;
			} else {
				fastFwd(ms, frameSize, frameRate);
				if (USE_LYRICS) {
					sb.getCurrentlyPlaying().lyrics.startSession();
				}
			}
		}


	} // mouth

}