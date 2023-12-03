package wah.mikooo.MediaPlayer;


import wah.mikooo.Ui.MainWindow;

import javax.sound.sampled.*;

import static wah.mikooo.ffmpegStuff.ffmpegWrapper.*;


public class Player implements Runnable {


	public static boolean autoplay = Boolean.parseBoolean(MainWindow.config.retrieve("autoplay")); // play next song after end

	// user settings
	boolean USE_LYRICS = false;
	boolean prevIsRestart = true; // previous command restarts song from beginning
	public float defaultVolume = Float.parseFloat(MainWindow.config.retrieve("volume"));

	public boolean useStreaming;
	public boolean preferStreaming;
	public boolean useHybridStreaming;




	// internal player variables
	protected SongBoard sb; // queues and available songs
	public boolean paused = false; // paused status
	boolean prevRequest = false; // request for a next/prev command
	boolean nextRequest = false;
	boolean forceWait = false; // force dispatcher to wait

	protected Mouth mouth;


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
			mouth = new Mouth(s, this);
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
	 * Toggle USE_LYRICS flag
	 */
	public void toggleUseLyrics() {
		setUseLyrics(!USE_LYRICS);
	}

	/**
	 * Directly set the value of USE_LYRICS
	 * @param value
	 */
	private void setUseLyrics(boolean value) {
		USE_LYRICS = value;
	}

	/**
	 * Get the value of USE_LYRICS
	 * @return
	 */
	public boolean getUseLyrics() {
		return USE_LYRICS;
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



	// fucking i changes shit to protected and the ui has sseus

	public void startLyricPrinter() {
		Song s = getCurrentlyPlaying();

		if (s.lyrics != null) {
			s.lyrics.startSession(); // start lyric printer
		}
	}

	public Song getCurrentlyPlaying() {
		if (sb == null)
			return null;
		return sb.getCurrentlyPlaying();
	}

	public long getCurrentPosMs() {
		return mouth.getCurrentPosMs();
	}

}