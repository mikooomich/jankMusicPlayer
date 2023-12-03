package wah.mikooo.Exceptions;

public class MetadataExtractorException extends ffmpegError {
	public MetadataExtractorException() {
		super("An error occurred parsing metadata.");
	}

	public MetadataExtractorException(String message) {
		super("An error occurred parsing metadata: " + message);
	}
}
