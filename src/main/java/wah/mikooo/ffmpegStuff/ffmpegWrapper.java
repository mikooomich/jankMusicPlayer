package wah.mikooo.ffmpegStuff;

import java.io.*;

import static wah.mikooo.MediaPlayer.Player.ffmpegBinary;

public class ffmpegWrapper {

    /**
     * Transcode audio file to wav PCM s16le "file" using ffmpeg.
     * @param fileName directory of file
     * @return byte array
     */
    public static byte[] ffmpegOwO(String fileName) {
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
//                    reader.lines().forEach(line -> System.out.println(line));
                    reader.lines().forEach(line -> line.compareTo("e"));
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
    public static InputStream ffmpegOwOStream(String fileName) {
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
//                            reader.lines().forEach(line -> System.out.println(line));
                            reader.lines().forEach(line -> line.compareTo("e"));
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

}
