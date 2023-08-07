package wah.mikooo.MediaPlayer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LrcReader {


    private static final long LYRIC_MS_DELAY = 500;
    boolean paused = false;
    boolean alive = false;


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
    private Thread lrcThread; // lyric "printer"
    private long currTstmp = 0; // current position in song

    /**
     * Read the corresponding lrc file given the song. Lrc must be in the same directory as the song.
     * @param path file path as string
     * @throws IOException
     */
    public LrcReader(String path) throws IOException {
        data = new ArrayList<>();
        System.out.println("Trying to read lyric file for: " + path);

        BufferedReader configRead;
        try {
            configRead = new BufferedReader(new FileReader(getCorrespondingLRCpath(path)));
        }
        catch (FileNotFoundException e) {
            System.out.println("WARNING: No lrc found for: " + path);
            data = null;
            return;
        }

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
     *
     *  Lyric "printer" functions
     *
     */


    /**
     * Print the lyric
     *
     * @param timeStamp ms (millisecond) to look up
     * @return
     * @throws InterruptedException
     */
    private long doPrint(long timeStamp) throws InterruptedException {
        LyricEntry lyric = lookupLyric(timeStamp);
        System.out.println(lyric.lyric + "(" + lyric.timestampMS + ")");

        // advance song odometer
        int index = data.indexOf(lyric);
        if (index < data.size() - 1) {
            // time to wait = next timestamp - current
           return (data.get(index + 1).timestampMS) - (lyric.timestampMS); // return next lyric
        }
        return Long.MAX_VALUE; // last lyric, show infinitely
    }


    /**
     * Retrieve the current lyric
     * @param timeStamp ms (millisecond) to look up
     * @return
     */
    private LyricEntry lookupLyric(long timeStamp) {

        if (timeStamp < 0) {
            throw new IndexOutOfBoundsException("Timestamp cannot be negative");
        }

        System.out.println("\nLOOKING UP: " + timeStamp);
        LyricEntry currentLyric = null;

        // find the last lyric <= the timeStamp
        for (LyricEntry entry : data) {
            if (entry.timestampMS > (timeStamp + LYRIC_MS_DELAY)) { // may require ms delay for synchronization with audio
                if (currentLyric == null) {
                    currentLyric = entry;
                }
                break;
            }
            currentLyric = entry;
        }

        return currentLyric;
    }


    /**
     * This method spawns a new thread that automatically shows the next lyric when it is time
     * Will keep printing until it is stopped by thread interrupt
     */
    public void lrcPrinter() {

        lrcThread = new Thread(new Runnable() {
            @Override
            public synchronized void run() {

                try {
                    while (alive) {
                        while (!paused) {
                            // This double loop serves no purpose because "resumes" are now treated as "start"
                            // I guess it won't hurt to keep it here...

                            long waitTime = doPrint(currTstmp);
                            currTstmp += waitTime;
                            wait(waitTime);
                        }

                        paused = true;
                    } // outer while
                } catch (InterruptedException e) {
                    System.out.println("DEBUG: LRC printer is exiting");
                    return;
                }

                System.out.println("DEBUG: LRC printer is exiting");
            } // run
        }); // thread
        lrcThread.start();
    }



    /**
     * Start a new lrcPrinter session
     * This method also kills the old session with killSession()
     */
    public void startSession() {
        if (data == null) {
            System.out.println("Song has no lyrics, exiting lyric printer");
            return;
        }
        // reset odometer
        if (currTstmp >= Long.MAX_VALUE) {
            currTstmp = 0;
        }


        // kill old session, reset flags
        killSession();
        paused = false;
        alive = true;

        lrcPrinter();
    }

    /**
     * Pause lyric printer
     *
     * @param timeStamp timestamp (in ms) to save to odometer
     */
    public void pause(long timeStamp) {
        killSession();
        currTstmp = timeStamp; // save current place
    }

    /**
     * Resume printer.
     * This is the same as startSession, but doesn't do anything if session is playing
     */
    public void resume() {
        if (!paused) {
            return;
        }

        startSession();
    }

    /**
     * Kill printer session by sending interrupt
     */
    public void killSession() {
        if (lrcThread == null || !lrcThread.isAlive()) {
            return;
        }

        lrcThread.interrupt();
    }



    /**
     * for debugging data structure
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