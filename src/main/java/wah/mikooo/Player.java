package wah.mikooo;


import org.apache.commons.io.output.ByteArrayOutputStream;

import javax.sound.sampled.*;
import java.io.*;

import static wah.mikooo.Player.Mouth.ffmpegOwO;


public class Player implements Runnable {

    static SongBoard sb;
    boolean autoplay = true;
    static boolean paused = false;
    static boolean forceWait = false;

    // the thing that plays audio
    static Mouth mouth;
    static Thread mouthThread;

    /**
     * Initialize with a SongBoard
     * @param sb
     */
    public Player(SongBoard sb) {
        this.sb = sb;
    }


    /**
     * Play a song
     * @param s song object
     */
    private void player(Song s) {
        System.out.println("playing " + s.path);

        try {
            // kill old mouth, create new
            if (mouth != null) {mouth.kill();}

            mouth = null;
            mouthThread = null;
            mouth = new Mouth(s, false);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * Notify everything
     */
    public synchronized void forceRecheck() {
        notifyAll();
        try {
            mouth.ayoWakeUp();
        }
        catch (Exception e ) {
//            e.printStackTrace();
            System.out.println("mouth is null lol");
        }
    }


    /**
     * Play function, set paused status to false
     */
    public void play() {
        System.out.println("register play");
        paused = false;
//        forceWait = false;
        forceRecheck();
    }

    /**
     * Pause function, set paused status to true
     */
    public void pause() {
        System.out.println("register pause");
        paused = true;
//        forceWait = true;
        forceRecheck();
    }

    /**
     * Enqueue all songs
     */
    public void playAll() {
        sb.enqueue(sb.availSongs);
    }


    /**
     * Next function
     */
    public void next() {
        // send a signal to stop playing, wake the mouth dispatcher
        paused = false;
        mouth.kill();
        System.out.println("Next command triggering wake up request");
        forceRecheck();
    }


    /**
     * Handle dispatching mouth thread
     * REMINDER: implement something to kill this thread once done
     * @throws InterruptedException
     */
    public synchronized void mouthDispatcher() throws InterruptedException {
        System.out.println("a");
        while (true) {

            inner:
            while (true) {

//            while (forceWait) {
//                wait();
//                forceWait = false;
//            }


            // primary wait case;
            // aka: stop playing when paused or nothing to play
            while (paused || sb.getCurrentlyPlaying() == null) {
                System.out.println("ENTERING primary wait case");
                wait(); // sleep until woken
                System.out.println("LEAVING primary wait case");
                break inner;
            }



            try {
                // dispatch song when reached end of audio stream
                if (mouth != null && mouth.atEnd()) {
                    sb.getNext();
                    System.out.println("Detect end of song, Playing new track");
                    player(sb.getCurrentlyPlaying());
                }
                else if (mouth == null) {
                    player(sb.getCurrentlyPlaying());
                    System.out.println("Playing new track (first run)");
                }


                if (autoplay) {
                    System.out.println("ENTERING autoplay wait");
                    wait();
                    System.out.println("LEAVING autoplay wait");

                    if (paused) { // go to primary wait case
                        System.out.println("Paused, time for primary wait");
                        break inner;
                    }
                    else { // assume reached end of song, dispatch song next loop run (easier understood than explained idk)
                        System.out.println(sb.getNext().path + " (autoplaying)");
                        next();
                    }
                }

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }


        } // inner while
        } // loop while

    }

    @Override
    public void run() {
        System.out.println("Starting player thread");
        try {
            mouthDispatcher();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Ending player thread");
    }



    /**
     * Need to make multi thread
     */
    public void loadAllSongsIntoMemoryThisIsAVeryBadIdea() {

        sb.availSongs.stream().forEach(song -> {
            if (song.getAudio() == null) {
                song.setAudio(ffmpegOwO(song.path));
            }
        });
        System.out.println("uhhhhhhh method finish");
    }


    class Mouth implements Runnable {

        private static final String out  = "-f wav pipe:1";
        private String fileName;
        boolean useStreaming;

        boolean goDieNow = false;
        boolean atEnd = false;


        public Mouth(Song path, boolean useStreaming) {
           this.useStreaming = useStreaming;


            fileName = path.path;
            mouthThread = new Thread(this);
            mouthThread.start();
        }
        SourceDataLine line;

        public void kill() {
            goDieNow = true;
            line.close();
        }

        public boolean atEnd() {
            if (atEnd) {
                return true;
            }
            else {return false;}

        }


        public synchronized void ayoWakeUp() {
            this.notify();
        }


        static byte[] ffmpegOwO(String fileName) {

            System.out.println("The ffmpeg command is:---------- " +  String.format("./ffmpegBS/ffmpeg.exe -i \"%s\" %s", fileName, out));

            Process ff = null;

            InputStream inputStream;

            try {
                ff = Runtime.getRuntime().exec(String.format("./ffmpegBS/ffmpeg.exe -i \"./stronghold/%s\" %s", fileName, out));
                BufferedReader reader = new BufferedReader(new InputStreamReader(ff.getErrorStream()));
                inputStream = ff.getInputStream();


                Thread uiThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        reader.lines().forEach(line -> System.out.println(line));
                    }
                });
                uiThread.start();

                org.apache.commons.io.output.ByteArrayOutputStream wah = new ByteArrayOutputStream();
                inputStream.transferTo(wah);

                return wah.toByteArray();


            } catch (IOException e) {
                System.out.println("FFMPEG FAILURE");
                throw new RuntimeException(e);
            }
        }

        synchronized void doStuff() {




            byte[] weh = null;
//
            System.out.println("streamin set to " + useStreaming);
            if (!useStreaming) {
                // lookup, retrieve
                System.out.println("seeing if we have");
                weh = sb.getCurrentlyPlaying().getAudio();

            }

            // fallback to streaming
            if (weh == null) {
                System.out.println("using streaming" + useStreaming);
                weh = ffmpegOwO(fileName);
                sb.getCurrentlyPlaying().setAudio(weh);
            }






//        System.out.println("-----------a---------");
//        AudioInputStream aas = AudioSystem.getAudioInputStream(new File("./idk2.wav"));
//            AudioInputStream aas = null;
            try {
                AudioInputStream aas = AudioSystem.getAudioInputStream(new ByteArrayInputStream(weh));

                System.out.println(aas.getFormat());
                System.out.println(aas.markSupported());
                System.out.println(aas.getFrameLength());

                // chat gpt my beloved
                // Open a source data line with the same format as the audio input stream
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, aas.getFormat());
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(aas.getFormat());
                line.start();

//            Objects.requireNonNull(clip);
                FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(-10.0f); // Reduce volume by 10 decibels.

                // Write audio data to the line buffer
                byte[] buffer = new byte[4096];
                int bytesRead;
                int aiya = 0;

                while ((bytesRead = aas.read(buffer)) != -1) {
                    if (goDieNow) {
                        line.close();
                        break;
                    }
                    if (paused || forceWait) {
                        wait();
                        System.out.println("i awoke");
                    }

                    line.write(buffer, 0, bytesRead);
                    aiya++;
                }


                atEnd = true;
                // Stop and close the line
                System.out.println(1);
                line.drain();
                System.out.println(2);
                line.stop();
                System.out.println(3);
                line.close();


                paused = false;

                System.out.println("Mouth is going to shut up forever now");



            } catch (UnsupportedAudioFileException e) {
                System.out.println("critical audio  failure");
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (IOException e) {
                System.out.println("critical audio  failure");
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (LineUnavailableException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            doStuff();
        }


    }
}