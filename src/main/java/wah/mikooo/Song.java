package wah.mikooo;

public class Song {

    public String path;
    byte[] audio;

    // metadata
    private String title;
    private String artist;
    private String album;
    private String date;
    private String encoder;
    private String genre;
    int length;


    public Song(String path) {
        this.path = path;

        // blah blah parse meta data
    }


    public byte[] getAudio() {
        return audio;
    }

    public void setAudio(byte[] audioBytes) {
        audio = audioBytes;
    }
}
