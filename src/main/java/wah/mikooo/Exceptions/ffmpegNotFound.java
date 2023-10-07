package wah.mikooo.Exceptions;

public class ffmpegNotFound extends ffmpegError {
	public ffmpegNotFound() {
		super("No valid FFMpeg executable was found.");
	}

	public ffmpegNotFound(String message) {
		super("No valid FFMpeg executable was found. " + message);
	}
}
