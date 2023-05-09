package wah.mikooo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import static wah.mikooo.Player.ffmpegBinary;
import static wah.mikooo.Player.ffprobeBinary;

public class Song {

    public String path;
    byte[] audio;

    // metadata
    private String title;
    private String artist;
    private String album;
    private String date;
    private String codec = "";
    private int sampleRate;
    private String channel;
    private String genre;
    String length;
    int bitrate; // kb/s
    BufferedImage albumArt;

    /**
     * Construct a Song given a file path (assuming it has an audio stream). This will also parse metadata and album art.
     * @param path
     */
    public Song(String path) {
        this.path = path;

        // steam metadata from FFMpeg
        Thread metadata = new Thread(new Runnable() {
            @Override
            public void run() {
                ProcessBuilder processBuilder;
                Process ffmpegThingy = null;

                try {
                    // confirm there is audio stream
                    System.out.println("Checking if valid: " + path);
                    processBuilder = new ProcessBuilder(ffprobeBinary, "-i", path, "-show_streams", "-select_streams", "a", "-loglevel", "error");
                    ffmpegThingy = processBuilder.start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpegThingy.getInputStream()));


                    if (reader.readLine() == null) {
                        System.out.println(path + " IS NOT A VALID AUDIO FILE, skipping");
                        setPath("INVALID");
                        return;
                    }
                    System.out.println("Valid Song found!");



                    // setup ffmpeg to extract album art
                    processBuilder = new ProcessBuilder(ffmpegBinary, "-i", path, "-an", "-c:v", "png", "-f", "image2pipe", "-");
                    ffmpegThingy = processBuilder.start();

                    reader = new BufferedReader(new InputStreamReader(ffmpegThingy.getErrorStream()));
                    BufferedImage albumArtFromFFMPEG = ImageIO.read(ffmpegThingy.getInputStream());
                    albumArt = albumArtFromFFMPEG;

                    // steal ffmpeg output for metadata
                    reader.lines().forEach(line -> {
                                System.out.println(line);

                        try {
                            if (line.contains("GENRE")) {
                                String[] parsed = line.split(":");
                                genre = parsed[1].trim();
                            } else if (line.contains("TITLE")) {
                                String[] parsed = line.split(":");
                                title = parsed[1].trim();
                            } else if (line.contains("ARTIST")) {
                                String[] parsed = line.split(":");
                                artist = parsed[1].trim();
                            } else if (line.contains("DATE")) {
                                String[] parsed = line.split(":");
                                date = parsed[1].trim();
                            } else if (line.contains("ALBUM")) {
                                String[] parsed = line.split(":");
                                album = parsed[1].trim();
                            } else if (line.contains("Duration")) {
                                String[] parsed = line.split(", ");

                                length = parsed[0].trim().split(" ")[1].trim();
                                bitrate = Integer.parseInt(parsed[2].split(" ")[1]);
                            } else if (line.contains("Audio:") && line.contains("Stream")) {
                                if (codec.isEmpty()) { // only take first audio track if many are availibke
                                    String[] parsed = line.split(", ");

                                    codec = parsed[0].split("Audio:")[1].trim();
                                    sampleRate = Integer.parseInt(parsed[1].split(" ")[0]);
                                    channel = parsed[2];
                                }
                            }
                        }
                        catch (Exception e) {
                            System.out.println("Error parsing metadata from song \"" + path + "\". " + e.getMessage());
                            System.out.println(line);
                        }
                    }); // end lambda


                } catch (IOException e) {
                    System.out.println("FFMPEG/PROCESS ERROR");
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }

            } // run
        });
        metadata.start();
    }

    /**
     * Retrieve audio data
     * @return
     */
    public byte[] getAudio() {
        return audio;
    }

    /**
     * Set audio data
     * @param audioBytes
     */
    public void setAudio(byte[] audioBytes) {
        audio = audioBytes;
    }

    /**
     * Set file path of song
     * @param newPath
     */
    private void setPath (String newPath) {
        path = newPath;
    }
}
