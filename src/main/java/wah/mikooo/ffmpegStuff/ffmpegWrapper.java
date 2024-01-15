package wah.mikooo.ffmpegStuff;

import java.io.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.StringTemplate.STR;
import static wah.mikooo.Ui.MainWindow.ffmpegBinary;
import static wah.mikooo.Ui.MainWindow.ffprobeBinary;


public class ffmpegWrapper {
	public static final int BYTE_BUFFER_SIZE = 1000000;

	/**
	 * Transcode audio file to WAV PCM s16le "file" using FFmpeg.
	 * Requires ffmpegBinary to be set
	 * <p>
	 * WARNING: This is a blocking call until transcoding finishes.
	 *
	 * @param fileName directory to file including file name
	 * @return Audio data (byte array)
	 */
	public static byte[] ffmpegOwO(String fileName) {
		ProcessBuilder processBuilder;
		Process ff = null;

		InputStream streamOutOfProcess;

		try {
			processBuilder = new ProcessBuilder(ffmpegBinary, "-i", fileName, "-f", "wav", "pipe:1");
			ff = processBuilder.start();

			BufferedReader reader = new BufferedReader(new InputStreamReader(ff.getErrorStream()));
			streamOutOfProcess = ff.getInputStream();

			// handle ffmpeg output
			Thread printThread = new Thread(new Runnable() {
				@Override
				public void run() {
//                    reader.lines().forEach(line -> System.out.println(line));
					reader.lines().forEach(line -> line.compareTo("e"));
				}
			});
			printThread.start();


			ByteArrayOutputStream wah = new ByteArrayOutputStream();
			streamOutOfProcess.transferTo(wah);

			return wah.toByteArray();


		} catch (IOException e) {
			System.out.println("FFMPEG/PROCESS ERROR");
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}


	/**
	 * Transcode audio file to WAV PCM s16le Input Stream using FFmpeg.
	 * Requires ffmpegBinary to be set
	 * <p>
	 *
	 * @param fileName directory of file
	 * @return Audio data (Input Stream)
	 */
	public static InputStream ffmpegOwOStream(String fileName) {
		final InputStream[] dataOut = {null};

		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				ProcessBuilder processBuilder;
				Process ff = null;

				InputStream streamOutOfProcess;

				try {
					processBuilder = new ProcessBuilder(ffmpegBinary, "-i", fileName, "-f", "wav", "pipe:1");
					ff = processBuilder.start();

					BufferedReader reader = new BufferedReader(new InputStreamReader(ff.getErrorStream()));
					streamOutOfProcess = ff.getInputStream();
					dataOut[0] = streamOutOfProcess;

					// handle ffmpeg output
					Thread printThread = new Thread(new Runnable() {
						@Override
						public void run() {
//                            reader.lines().forEach(line -> System.out.println(line));
							reader.lines().forEach(line -> line.compareTo("e"));
						}
					});
					printThread.start();

				} catch (IOException e) {
					System.out.println("FFMPEG/PROCESS ERROR");
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			} // run
		});
		thread.start();

		// hope err... I mean ensure we do not return before ffmpeg stream initiated
		try {
			Thread.sleep(500);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dataOut[0];
	}


	/**
	 * Transcode audio file to WAV PCM s16le "file" asynchronously using FFmpeg.
	 * Requires ffmpegBinary to be set
	 * <p>
	 * WARNING: This is a non-blocking call, data is written to the byte array asynchronously.
	 * Here be the asynchronous demons.
	 *
	 * @param fileName directory of file
	 * @return Audio data (byte array)
	 */
	public static byte[] ffmpegOwOHybrid(String fileName) {
		ProcessBuilder ffmpeg;
		Process ffm = null;

		ProcessBuilder ffprobe;
		Process ffp = null;

		InputStream streamOutOfProcess;

		try {
			ffmpeg = new ProcessBuilder(ffmpegBinary, "-i", fileName, "-f", "wav", "pipe:1");
			ffm = ffmpeg.start();

			streamOutOfProcess = ffm.getInputStream();

			ffprobe = new ProcessBuilder(ffprobeBinary, "-i", fileName, "-show_streams", "-select_streams", "a", "-loglevel", "error");
			ffp = ffprobe.start();
			BufferedReader ffprobeOut = new BufferedReader(new InputStreamReader(ffp.getInputStream()));

			// get track duration from ffprobe
			AtomicReference<Float> trackDuration = new AtomicReference<>((float) -1.0);
			ffprobeOut.lines().forEach((line) -> {
//				System.out.println(line);
				if (line.contains("duration=")) {
//					System.out.println("FOUND DURATION " + line);
					trackDuration.set(Float.parseFloat(line.substring(9)));
					System.out.println(trackDuration.get());
				}
			});

			// TODO: Use ffmpeg option from config
			final int frameRATE = 48000;
			final int frameSize = 4;

			int trackLengthBytes = (int) Math.ceil(trackDuration.get() * frameRATE * frameSize);
			byte[] songData = new byte[trackLengthBytes];

			/**
			 * Write transcode output to byte array... via async means
			 */
			Thread printThread = new Thread(new Runnable() {
				@Override
				public void run() {
					System.out.println("Total bytes to read:" + trackLengthBytes);

					try {
						int totalBytesRead = 0;
						int bytesRead = 0;

						// read to array lol
						while (totalBytesRead < trackLengthBytes && (bytesRead = streamOutOfProcess.readNBytes(songData, totalBytesRead, Math.min(trackLengthBytes - totalBytesRead, BYTE_BUFFER_SIZE)) // if BYTE_BUFFER_SIZE is greater than remaining, use remaining size
						) > 0) {
							totalBytesRead += bytesRead;
//							System.out.println("BYTE" + bytesRead);
//							System.out.println("TOTAL BYTES" + totalBytesRead);
						}

						System.out.println(STR."Writerite complete. Track duration: \{trackDuration.get()} Bytes: \{totalBytesRead}");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			printThread.start();

			return songData;

		} catch (IOException e) {
			System.out.println("FFMPEG/PROCESS ERROR");
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

}
