package wah.mikooo.MediaPlayer;

import wah.mikooo.Ui.MainWindow;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static wah.mikooo.ffmpegStuff.ffmpegWrapper.*;
import static wah.mikooo.ffmpegStuff.ffmpegWrapper.ffmpegOwO;


public class Mouth implements Runnable {

	private String fileName;
	private long length; // song length in ms

	boolean goDieNow = false;
	boolean atEnd = false;
	boolean playing = false;
	boolean hotRestart = false;

	SourceDataLine line;
	AudioInputStream aas = null;

	int totalBytes = 0; // total bytes "played"

	Song currSong;


	private Thread mouthThread = null;

	//	private SongBoard sb;
	private Player plr;

	/**
	 * Create a Mouth
	 *
	 * @param path
	 * @param useStreaming
	 * @param preferStreaming
	 */
	public Mouth(Song path, Player playerLink, boolean useStreaming, boolean preferStreaming, boolean useHybridStreaming) {
		playerLink.useStreaming = useStreaming;
		if (!useStreaming) { // auto disable prefer streaming
			playerLink.preferStreaming = false;
		}
		else {
			playerLink.preferStreaming = preferStreaming;
			playerLink.useHybridStreaming = useHybridStreaming;
		}

		plr = playerLink;
		path.setPlayer(playerLink);

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
	public Mouth(Song path, Player playerLink) {
		this(path, playerLink, true, false, true);
	}


	/**
	 * Get the current position in the song in miliseconds since beginning. -1 will be returned if there is no data
	 *
	 * @return
	 */
	public long getCurrentPosMs() {
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
		plr.getCurrentlyPlaying().lyrics.killSession(); // kill lyric printer
	}


	/**
	 * Return whether or not the song has finished playing
	 *
	 * @return
	 */
	public boolean atEnd() {
		if (atEnd) {
			return true;
		}
		else {
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
	 * <p>
	 * If useStreaming is true, first lookup memory for song, then use streaming as a fallback if required.
	 * If useStreaming is false, we will always wait for FFMpeg transcode to finish before playing.
	 * If useHybridStreaming is true, we will directly stream from the byte array FFMpeg transcode into.
	 * If preferStreaming is true, we will always FFMpeg stream the song without saving transcode to memory.
	 * <p>
	 * With useStreaming enable and hybrid streaming disabled, the song is directly streamed from FFMpeg, and a copy of the song will be saved in memory (these are 2 separate threads).
	 */
	synchronized void audioSession() {

		InputStream audioStream = null;
		byte[] audioFile = null;
		totalBytes = 0;

		System.out.println("Streaming fallback is set to: " + plr.useStreaming);
		System.out.println("Prefer Streaming is set to: " + plr.preferStreaming);

		// look up in memory first
		if (!plr.preferStreaming) {
			System.out.println("Seeing if we have");
			audioFile = plr.getCurrentlyPlaying().getAudio(); // lookup, retrieve
		}

		// if streaming is enabled, stream and recode to memory in parallel
		// else finish recode before start playing
		if (plr.useStreaming && !plr.useHybridStreaming) {
			// fallback to streaming if not already in memory
			if (audioFile == null || plr.preferStreaming) {
				audioStream = ffmpegOwOStream(fileName);

				if (!plr.preferStreaming) {
					Thread thread = new Thread(new Runnable() {
						@Override
						public void run() {
							// load into memory. Hi this is a terrible hack and I should just feed the one input stream into 2 outputs... but here we are
							plr.getCurrentlyPlaying().setAudio(ffmpegOwO(fileName));
							/**
							 *   remember to create write lock in songboard
							 *   or will end up writing when not supposed to
							 */
						}
					});
					thread.start();
				}
			}
		}
		else if (plr.useStreaming && plr.useHybridStreaming) {
			System.out.println("Using hybrid streaming");
			plr.getCurrentlyPlaying().setAudio(ffmpegOwOHybrid(fileName));
			System.out.println("Starting playing now");
			audioFile = plr.getCurrentlyPlaying().getAudio(); // lookup, retrieve
		}
		else { // use streaming == false
			System.out.println("Waiting for transcode to finish before playing");
			plr.getCurrentlyPlaying().setAudio(ffmpegOwO(fileName));
			System.out.println("Transcode finished, Starting playing now");
			audioFile = plr.getCurrentlyPlaying().getAudio(); // lookup, retrieve
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

			}
			else { // we have the full song already loaded
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
			FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
			gainControl.setValue(plr.defaultVolume);

			// Write audio data to the line buffer
			byte[] buffer = new byte[4096];
			int bytesRead;
			playing = true;


			// variable for hopefully performance
			float frameRate = aas.getFormat().getFrameRate();
			int frameSize = aas.getFormat().getFrameSize();
			System.out.println(frameRate + "werfouihjweouifhjweofiujweoifwjefoiweojf" + frameSize);
			long lastSecond = 0;
			boolean lyricsEnabled = plr.USE_LYRICS && plr.getCurrentlyPlaying().lyrics != null;


			MainWindow.drawCenter();
			if (lyricsEnabled && plr.getCurrentlyPlaying().lyrics != null) {
				plr.getCurrentlyPlaying().lyrics.startSession(); // start lyric printer
			}
			MainWindow.redrawTitles(); // update ui

			// feed to audio system
			while ((bytesRead = aas.read(buffer)) != -1) {
				if (goDieNow) { // stop playing immediately
					line.close();
					break;
				}
				if (plr.paused || plr.forceWait) { // player requests pause
					playing = false;

					// tell lyric player to stop
					if (lyricsEnabled) {
						plr.getCurrentlyPlaying().lyrics.pause(getCurrentPosMs());
					}

					wait();
					System.out.println("i awoke");
					playing = true;
					plr.getCurrentlyPlaying().lyrics.resume(); // i truly hope that this doesn't become an issue where the the the if it decided to print lyrics if still paused. The alternative is spamming resume on *every* buffer copy
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
					// System.out.println(totalBytes / frameSize / frameRate);
					MainWindow.updateSongPos(currSong);
					lastSecond = (long) Math.floor((long) (totalBytes / frameSize / frameRate));
				}
			}


			atEnd = true;
			plr.paused = false;
			playing = false;
			if (plr.autoplay) {
				plr.nextRequest = true; // signal next
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
			plr.forceRecheck();
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
		}
		else {
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
			Thread.sleep(100); // this looks like a deadlock possibility? Allow goDieNow as a force exit?
		}
		totalBytes += bytePosition;
		System.out.println("skipped (bytes)" + aas.skip(bytePosition));
		if (plr.USE_LYRICS) {
			plr.getCurrentlyPlaying().lyrics.startSession();
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
		}
		else {
			fastFwd(ms, frameSize, frameRate);
			if (plr.USE_LYRICS) {
				plr.getCurrentlyPlaying().lyrics.startSession();
			}
		}
	}


}