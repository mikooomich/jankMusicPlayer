package wah.mikooo.MediaPlayer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LrcReader {

    /**
     * Sync lyric (lrc) file representation
     */
    public class LyricEntry {
        String lyric;
        long timestampMS;

        /**
         * Lyric/timestamp combo representation
         * For timestamping: Assume the lyric file adheres to the laws of time measuring and overflow.
         * Miliseconds uses 2 digits ex: 00:00.10 is 100ms
         * @param timestamp time stamp in format "min:sec.ms"
         * @param content
         */
        private LyricEntry(String timestamp, String content) {
            lyric = content;

            String raw = timestamp.trim().replace(".", ":").replace("[", "").replace("]", "");
            String[] cleaned = raw.split(":");

            // convert string timestamp to long
            long lengthMS = 0;
            for (int i = 0; i < cleaned.length; i++) {
                switch (i) {
                    case 0: lengthMS += Long.parseLong(cleaned[i]) * 60 * 1000; break; // minutes
                    case 1: lengthMS += Long.parseLong(cleaned[i]) * 1000; break; // seconds
                    case 2: lengthMS += Long.parseLong(cleaned[i]) * 10; break; // milliseconds, assume 2 ms digits (100s and 10s)
                }
            }
            timestampMS = lengthMS;
        }
    }




    private List<LyricEntry> data; // main data structure


    /**
     * Read the corresponding lrc file given the song. Lrc must be in the same directory as the song.
     * @param path file path as string
     * @throws IOException
     */
    public LrcReader(String path) throws IOException {
        data = new ArrayList<>();
        System.out.println("Starting read of: " + path);

        BufferedReader configRead = new BufferedReader(new FileReader(getCorrespondingLRCpath(path)));

        try {
            String lineData = configRead.readLine();

            outer:
            while (true) {
                String lyricBuffer = "";
                String timestamp = "";

                //for each line, parse
                while (true) {

                    // EOF
                    if (lineData == null) {
                        data.add(new LyricEntry(timestamp, lyricBuffer)); // flash "buffer"
                        break outer;
                    }

                    // found timestamp, begin read
                    if (lineData.startsWith("[") && timestamp.isBlank()) {
                        timestamp = lineData.substring(1, lineData.indexOf("]"));
                        lyricBuffer = lineData.substring(lineData.indexOf("]") + 1) + "\n";
                    } else if (lineData.startsWith("[") && !timestamp.isBlank()) { // commit to list
                        // add code here to strip the extra \n\n\n\n spam between lyrics in the future
                        data.add(new LyricEntry(timestamp, lyricBuffer));
                        break;
                    } else if (lineData.isBlank() && lyricBuffer.isBlank()) {
                        // ignore blanks in between timestamp and lyric blocks
                        continue;
                    } else {
                        lyricBuffer += lineData + "\n";
                    }


                    lineData = configRead.readLine();

                } // inner while
            } // outer while
        }

        catch (IOException e) {
//            System.out.println("Reader error: ");
//            e.printStackTrace();
            throw new IOException(e);
        }

        configRead.close();
    }


    /**
     * Retrieve the lrc path corresponding to the song.
     * The sync lyric file would be the name of the song, with the file extention as .lrc
     * @param songPath file path of song
     * @return
     */
    private String getCorrespondingLRCpath(String songPath) {
        for (int i = songPath.length()-1; i >= 0; i--) {
            if (songPath.charAt(i) == '.') {
                System.out.println("FOUND file extension");
                return songPath.substring(0, i) + ".lrc";
            }
        }

        return songPath + ".lrc";
    }

    /**
     * debug list printer
     */
    private void printlist() {
        data.forEach(lyricEntry -> {
            System.out.println("timestamp (ms)-> " + lyricEntry.timestampMS + "\nLyric--> " + lyricEntry.lyric);
        });
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Hewwo");
        LrcReader waaa = new LrcReader("./test.ml2");
        waaa.printlist();
    }

}