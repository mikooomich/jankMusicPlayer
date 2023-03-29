package wah.mikooo;

public class Song {

    public String path;
//    private String name;
//    String length;
    byte[] audio;


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
