package wah.mikooo.Exceptions;

public class ffmpegError extends RuntimeException {
    public ffmpegError() {
        super("A critical FFMpeg error occurred.");
    }
    public ffmpegError(String message) {
        super("A critical FFMpeg error occurred:" + message);
    }
}
