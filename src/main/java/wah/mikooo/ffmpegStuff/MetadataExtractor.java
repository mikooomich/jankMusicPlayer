package wah.mikooo.ffmpegStuff;

import javafx.scene.image.Image;
import wah.mikooo.Exceptions.MetadataExtractorException;
import wah.mikooo.MediaPlayer.LrcReader;
import wah.mikooo.MediaPlayer.Song;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;


import static wah.mikooo.Ui.MainWindow.ffmpegBinary;
import static wah.mikooo.Ui.MainWindow.ffprobeBinary;

public class MetadataExtractor implements Callable<Integer> {

	private Song target;

	/**
	 * A Song wrapper for metadata extraction. This is designed to be run with an Execution.
	 * <p>
	 * This will use ffprobe to check if this is a valid song, then ffmpeg to extract the album art,
	 * and simultaneously stealing the log output for the metadata.
	 * Song will be directly written to.
	 *
	 * @param target song to extract metadata for
	 */
	public MetadataExtractor(Song target) {
		this.target = target;
	}


	@Override
	public Integer call() {
		ProcessBuilder artExtractorProcess;
		Process artExtractor = null;

		try {
			// confirm there is audio stream
			ProcessBuilder ffprobeBuilder;
			Process ffprobe = null;

			System.out.println("Checking if valid: " + target.path);
			ffprobeBuilder = new ProcessBuilder(ffprobeBinary, "-i", target.path, "-show_streams", "-select_streams", "a", "-loglevel", "error");
			ffprobe = ffprobeBuilder.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(ffprobe.getInputStream()));


			if (reader.readLine() == null) {
				System.out.println(target.path + " IS NOT A VALID AUDIO FILE, skipping");

				target.setValidity(-1);
				return -1;
			}
			System.out.println("Valid audio stream found!");


			// setup FFMpeg to extract album art
			artExtractorProcess = new ProcessBuilder(ffmpegBinary, "-i", target.path, "-an", "-vframes", "1", "-c:v", "png", "-f", "image2pipe", "-");
			artExtractor = artExtractorProcess.start();
			reader = new BufferedReader(new InputStreamReader(artExtractor.getErrorStream()));


			// extract album art/thumbnail
			Process finalFfmpegThingy = artExtractor;
			new Thread(new Runnable() {
				@Override
				public void run() {
					System.out.println("Extracting image...");
					Image albumArtFromFFMPEG = null;
					albumArtFromFFMPEG = new Image(finalFfmpegThingy.getInputStream());
					target.albumArt = albumArtFromFFMPEG;
					System.out.println("Finishing image extractor for " + target.path);
				}
			}).start();

			/**
			 * TODO: refactor to use from ffprobe
			 */
			System.out.println("Reading metadata...");
			// steal ffmpeg output for metadata
			try {
				reader.lines().forEach(line -> {
//                    System.out.println(line);

					try {
						if (line.contains("GENRE")) {
							String[] parsed = line.split(":");
							target.genre = parsed[1].trim();
						}
						else if (line.contains("TITLE")) {
							String[] parsed = line.split(":");
							target.title = parsed[1].trim();
						}
						else if (line.contains("ARTIST")) {
							String[] parsed = line.split(":");
							target.artist = parsed[1].trim();
						}
						else if (line.contains("DATE")) {
							String[] parsed = line.split(":");
							target.date = parsed[1].trim();
						}
						else if (line.contains("ALBUM")) {
							String[] parsed = line.split(":");
							target.album = parsed[1].trim();
						}
						else if (line.contains("Duration")) { // bitrate AND length of song
							/**
							 * TODO: pull duration from ffprobe, store as exact ms. Use method in ffmpegWrapper hybrid method
							 */
							String[] parsed = line.split(", ");

							String raw = parsed[0].trim().split(" ")[1].trim().replace(".", ":");
							String[] cleaned = raw.split(":");
							long lengthMS = 0;
							for (int i = 0; i < cleaned.length; i++) { // parse timecode into milliseconds
								switch (i) {
									case 0:
										lengthMS += Long.parseLong(cleaned[i]) * 60 * 60 * 1000;
										break; // hours
									case 1:
										lengthMS += Long.parseLong(cleaned[i]) * 60 * 1000;
										break; // minutes
									case 2:
										lengthMS += Long.parseLong(cleaned[i]) * 1000;
										break; // seconds
									case 3:
										lengthMS += Long.parseLong(cleaned[i]);
										break; // milliseconds
								}
							}

							target.length = lengthMS;
							target.bitrate = Integer.parseInt(parsed[2].split(" ")[1]);
						}
						else if (line.contains("Audio:") && line.contains("Stream")) { // codec, sample rate, channels
							if (target.codec.isEmpty()) { // only take metadata of first audio track if many are available
								String[] parsed = line.split(", ");

								target.codec = parsed[0].split("Audio:")[1].trim();
								target.sampleRate = Integer.parseInt(parsed[1].split(" ")[0]);
								target.channel = parsed[2];
							}
						}

					} catch (Exception e) {
						throw new MetadataExtractorException("Error parsing metadata from song \"" + target.path + "\". The line of ffmpeg output is: \n" + line + "\n" + e.getMessage());
					}
				}); // end lambda

			} catch (MetadataExtractorException e) {
				System.out.println(e.getMessage());
				return 20;
			}


			// read sync lyrics file if applicable
			try {
				target.setLyrics(new LrcReader(target.path));
			} catch (Exception e) {
				System.out.println("There was an error reading the lyric file for " + target.path + ", skipping");
				target.setLyrics(null);
				e.printStackTrace();
			}

		} catch (IOException e) {
			System.out.println("FFMPEG/PROCESS ERROR... path is" + target.path);
			e.printStackTrace();
			throw new RuntimeException(e);
		}


		target.setValidity(0);
		return 0;
	}
}
