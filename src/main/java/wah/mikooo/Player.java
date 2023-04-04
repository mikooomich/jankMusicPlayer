package wah.mikooo;

import javax.sound.sampled.*;
import java.io.*;

import static wah.mikooo.Player.Mouth.ffmpegOwO;


public class Player implements Runnable {

    static SongBoard sb;
    static boolean autoplay = true;
    static boolean paused = false;
    static boolean prevRequest = false;
    static boolean nextRequest = false;
    static boolean forceWait = false;

    static String ffmpegBinary;

    // the thing that plays audio
    static Mouth mouth;
    static Thread mouthThread;
    static float defaultVolume = -10;


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
            mouth = new Mouth(s);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * Set volume.
     * Artificially limited at +5db
     * @param volume
     */
    public void changeVolume(float volume) {
        if (volume > 5) {
            volume = 5;
        }
        if (mouth == null || mouth.getLine() == null) {
            return;
        }
        // volume
        FloatControl gainControl = (FloatControl) mouth.getLine().getControl(FloatControl.Type.MASTER_GAIN);
        gainControl.setValue(volume);

        defaultVolume = volume;
    }

    /**
     * Notify everything
     */
    public synchronized void forceRecheck() {
        notifyAll();
        try {
            if (!mouth.playing) { // will deadlock if called and mouth is not in wait
                mouth.ayoWakeUp();
            }
        }
        catch (Exception e ) {
//            e.printStackTrace();
            System.out.println("mouth is null lol");
        }

        Main.ui.draw();
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
     * Sends a signal to mouth to shut up, and send set parameters for mouth dispatcher.
     * Mouth will dispatch will wake up the mouth.
     */
    public void next() {
        // send a signal to stop playing, wake the mouth dispatcher
        paused = false;
        nextRequest = true;
        if (mouth != null) {
            mouth.kill();
            mouth.ayoWakeUp();
        }

        System.out.println("Next command triggering wake up request");
    }


    public void prev() {
        paused = false;
        prevRequest = true;
        if (mouth != null) {
            mouth.kill();
            mouth.ayoWakeUp();
        }
        System.out.println("PREV command triggering wake up request");
    }


    /**
     * Handle dispatching mouth thread.
     * This was a great idea theoretical, however in practice it
     * is just messy and very, VERY error-prone. Wah.
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
                if (!prevRequest && !nextRequest) { // skip if need process next request

                    if (mouth != null && mouth.atEnd()) {
                        System.out.println("Detect end of song, Playing new track");
                        player(sb.getCurrentlyPlaying());
                    } else if (mouth == null) {
                        player(sb.getCurrentlyPlaying());
                        System.out.println("Playing new track (first run)");
                    }


                    if (autoplay && mouth.atEnd()) {
                        System.out.println("ENTERING autoplay wait");
                        wait();
                        System.out.println("LEAVING autoplay wait");
                    }

                }


                /**
                 * "Why is this case here twice?"
                 * The former dispatches mouth, the latter modifies the queue.
                 * The actions are separated instead of being in one massive code block
                 */
                if (mouth != null && mouth.atEnd()) { // modify queue when playback ends

                        if (paused) { // go to primary wait case
                            System.out.println("Paused, time for primary wait");
                            break inner;
                        } else if (prevRequest) {
                            System.out.println("enter prev");
                            System.out.println(sb.getPrev().path + " (autoplaying)");
                            prevRequest = false;
                            break inner;

                        } else { // assume reached end of song, dispatch song next loop run (easier understood than explained idk)
                            System.out.println(sb.getNext().path + " (autoplaying)");
                            nextRequest = false;
                           break inner;
                        }
                    }

                // wait unit next dispatch call
                wait();
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
     * Need to make multi thread, worker threads, need to acquire lock
     * to only have one thing touching a song
     */
    public void loadAllSongsIntoMemoryThisIsAVeryBadIdea() {
        sb.availSongs.stream().forEach(song -> {
            if (song.getAudio() == null) {
                song.setAudio(ffmpegOwO(song.path));
            }
        });
        System.out.println("loadAllSongsIntoMemoryThisIsAVeryBadIdea() method finish");
    }


    class Mouth implements Runnable {

        private String fileName;
        boolean useStreaming;
        boolean preferStreaming;

        boolean goDieNow = false;
        boolean atEnd = false;
        boolean playing = false;

        SourceDataLine line;

        /**
         * Create a Mouth
         * @param path
         * @param useStreaming
         * @param preferStreaming
         */
        public Mouth(Song path, boolean useStreaming, boolean preferStreaming) {
           this.useStreaming = useStreaming;
           if (!useStreaming) { // auto disable prefer streaming
               this.preferStreaming = false;
           }
           else {
               this.preferStreaming = preferStreaming;
           }


            fileName = path.path;
            mouthThread = new Thread(this);
            mouthThread.start();
        }

        /**
         * Create a Mouth with default seStreaming = true, preferStreaming = false
         * @param path
         */
        public Mouth(Song path) {
            this(path, true, false);
        }


        /**
         * Send signal to stop talking
         */
        public void kill() {
            goDieNow = true;
            if (line != null) {
                line.close();
            }
        }


        /**
         * Return whether or not the song has finished playing
         * @return
         */
        public boolean atEnd() {
            if (atEnd) {
                return true;
            }
            else {return false;}
        }

        /**
         * Wakeup (notify) Mouth
         */
        public synchronized void ayoWakeUp() {
            this.notify();
        }

        /**
         * Return line (for volume control)
         * @return
         */
        public Line getLine() {
            return line;
        }


        /**
         * Transcode audio file to wav PCM s16le "file" using ffmpeg.
         * @param fileName directory of file
         * @return byte array
         */
        static byte[] ffmpegOwO(String fileName) {
            ProcessBuilder processBuilder;
            Process ff = null;

            InputStream streamOutOfProcess;

            try {
                processBuilder = new ProcessBuilder(ffmpegBinary, "-i" , fileName, "-f", "wav", "pipe:1");
                ff = processBuilder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(ff.getErrorStream()));
                streamOutOfProcess = ff.getInputStream();

                // handle ffmpeg output
                Thread printThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        reader.lines().forEach(line -> System.out.println(line));
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
         * Transcode audio file to wav PCM s16le stream using ffmpeg.
         * @param fileName directory of file
         * @return stream
         */
        static InputStream ffmpegOwOStream(String fileName) {
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
                                    reader.lines().forEach(line -> System.out.println(line));
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
                try {Thread.sleep(500);} catch (Exception e) {e.printStackTrace();}
                return dataOut[0];
        }


        /**
         * Play sound.
         * If useStreaming is true, first lookup memory for song, then use streaming as a fallback if required.
         * If useStreaming is false, the user must manually do ffmpeg
         * If preferStreaming is true, we will always ffmpeg stream the song.
         *
         * While streaming, the song is directly streamed from ffmpeg, and a copy of the song will be saved in memory.
         *
         */
        synchronized void audioSession() {

            InputStream audioStream = null;
            byte[] audioFile = null;

            System.out.println("Streaming fallback is set to: " + useStreaming);
            System.out.println("Prefer Streaming is set to: " + preferStreaming);

            // look up in memory first
            if (!preferStreaming) {
                System.out.println("Seeing if we have");
                audioFile = sb.getCurrentlyPlaying().getAudio(); // lookup, retrieve
            }

            // fallback to streaming
            if (audioFile == null || preferStreaming) {
                audioStream = ffmpegOwOStream(fileName);


                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // load into memory. Hi this is a terrible hack and I should just feed the one input stream into 2 outputs... but here we are
                        sb.getCurrentlyPlaying().setAudio(ffmpegOwO(fileName));
                        /**
                         *   remember to create write lock in songboard
                         *   or will end up writing when not supposed to
                         */
                    }
                });
                thread.start();
            }


            try {
                AudioInputStream aas = null;
                if (audioFile == null) { // streaming fallback
                    System.out.println("Using direct streaming");
                    int attempts = 1;

                    // attempt a maximum of 3 times to get a stream
                    while (attempts < 4) {
                        try {
                            aas = AudioSystem.getAudioInputStream(audioStream);
                            break;
                        }
                        catch (Exception e) {
                            System.out.println("Failed to get stream, retrying: " + attempts);
                        }
                        attempts ++;
                    }
                    Thread.sleep(1000);

                }
                else { // we have the full song already loaded
                    System.out.println("Using loaded stream");
                    aas = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioFile));
                }

                // bail if could not get something valid to play
                if (aas == null) {
                    throw new Exception("Could not get stream.");
                }

                System.out.println(aas.getFormat());
                System.out.println(aas.markSupported());
                System.out.println(aas.getFrameLength());

                // chat gpt my beloved
                // Open a source data line with the same format as the audio input stream
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, aas.getFormat());
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(aas.getFormat());
                line.start();


                /**
                 * Why are you using Line when Clip supports wav?
                 * For whatever reason ffmpeg piping messes up something and
                 * cause unsupported format error or Audio Data < 0.
                 * But playing a wav file you converted with ffmpeg via terminal commands is fine.
                 * Idk, AudioInputStream reads as a valid file.
                 */


                // volume control
                FloatControl gainControl = (FloatControl) mouth.getLine().getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(defaultVolume);

                // Write audio data to the line buffer
                byte[] buffer = new byte[4096];
                int bytesRead;
                playing = true;

                // feed to audio system
                while ((bytesRead = aas.read(buffer)) != -1) {
                    if (goDieNow) { // stop playing immediately
                        line.close();
                        break;
                    }
                    if (paused || forceWait) { // player requests pause
                        playing = false;
                        wait();
                        System.out.println("i awoke");
                        playing = true;
                        continue; // avoid playing sound if still paused
                    }

                    line.write(buffer, 0, bytesRead);
                }

                atEnd = true;
                paused = false;
                playing = false;
                if (autoplay) {
                    nextRequest = true; // signal next
                }
                // Stop and close the line
                System.out.println(1);
                line.drain();
                System.out.println(2);
                line.stop();
                System.out.println(3);
                line.close();


                System.out.println("Mouth is going to shut up forever now");
                forceRecheck();
            }

            catch (Exception e) {
                System.out.println("Critical Mouth failure");
                e.printStackTrace();
                throw new RuntimeException("Critical Mouth failure: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            audioSession();
        }


    }
}