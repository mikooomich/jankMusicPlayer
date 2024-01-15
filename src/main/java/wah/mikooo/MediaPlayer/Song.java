package wah.mikooo.MediaPlayer;

import javafx.scene.image.Image;

public class Song {
	private Player plr;

	public String path;
	byte[] audio;
	public int status = Integer.MIN_VALUE;
	/**
	 * Current status codes:
	 * <p>
	 * Integer.MINIMUM_VALUE - no code assigned
	 * <p>
	 * 0 - No error
	 * <p>
	 * -1 - Invalid audio file (No audio streams)
	 * <p>
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
	 * Construct a very basic Song given a file path.
	 * <p>
	 * This only serves as a data type, and does not include any functionality to automatically
	 * load audio data or metadata.
	 *
	 * @param path file path
	 */
	public Song(String path) {
		this.path = path;
	}

	/**
	 * Construct a very basic Song given a file path. This will associate the song with the given Player.
	 * <p>
	 * This only serves as a data type, and does not include any functionality to automatically
	 * load audio data or metadata.
	 *
	 * @param path file path
	 */
	public Song(String path, Player playerLink) {
		this.path = path;
		plr = playerLink;
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
	 * @param audioBytes audio data
	 */
	public void setAudio(byte[] audioBytes) {
		audio = audioBytes;
	}

	/**
	 * Set file path of song
	 *
	 * @param newPath file path
	 */
	public void setPath(String newPath) {
		path = newPath;
	}

	/**
	 * Set the status code
	 * <p>
	 * Integer.MINIMUM_VALUE - no code assigned
	 * <p>
	 * 0 - No error
	 * <p>
	 * -1 - Invalid audio file (No audio streams)
	 * <p>
	 * 20 - Metadata reader error
	 *
	 * @param statusCode
	 */
	public void setValidity(int statusCode) {
		status = statusCode;
	}

	/**
	 * Set a Player to associate the song to
	 * @param playerLink
	 */
	public void setPlayer(Player playerLink) {
		plr = playerLink;
		lyrics.plr = plr;
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
	 */
	public void setLyrics(LrcReader newLyrics) {
		// maybe in the future only allow one write
		lyrics = newLyrics;
	}
}
