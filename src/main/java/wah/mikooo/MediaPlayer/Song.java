package wah.mikooo.MediaPlayer;

import javafx.scene.image.Image;

public class Song {

	public String path;
	byte[] audio;
	public int status = Integer.MIN_VALUE;
	/**
	 * Current status codes:
	 * <p>
	 * Integer.MINIMUM_VALUE - no code assigned
	 * 0 - No error
	 * -1 - Invalid audio file (No audio streams)
	 * 20 - Metadata reader error
	 */

	// metadata
	public String title;
	public String artist;
	public String album;
	public String date;
	public String codec = "";
	public int sampleRate;
	public String channel;
	public String genre;
	public long length;
	public int bitrate; // kb/s
	public Image albumArt;

	// lyrics
	public LrcReader lyrics;

	/**
	 * Construct a Song given a file path (assuming it has an audio stream). This will also parse metadata and album art.
	 *
	 * @param path
	 */
	public Song(String path) {
		this.path = path;
	}

	/**
	 * Retrieve audio data
	 *
	 * @return
	 */
	public byte[] getAudio() {
		return audio;
	}

	/**
	 * Set audio data
	 *
	 * @param audioBytes
	 */
	public void setAudio(byte[] audioBytes) {
		audio = audioBytes;
	}

	/**
	 * Set file path of song
	 *
	 * @param newPath
	 */
	public void setPath(String newPath) {
		path = newPath;
	}

	/**
	 * Set the status code
	 * <p>
	 * Integer.MINIMUM_VALUE - no code assigned
	 * 0 - No error
	 * -1 - Invalid audio file (No audio streams)
	 * 20 - Metadata reader error
	 *
	 * @param statusCode
	 */
	public void setValidity(int statusCode) {
		status = statusCode;
	}

//    /**
//     * Return sync lyrics object
//     * @return
//     */
//    public LrcReader getLyrics(int timestamp) {
//
//        // blah balh blah
//        return lyrics;
//    }


	/**
	 * Set sync lyrics object
	 *
	 * @return
	 */
	public void setLyrics(LrcReader newLyrics) {
		// maybe in the future only allow one write
		lyrics = newLyrics;
	}
}
