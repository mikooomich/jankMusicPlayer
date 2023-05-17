package wah.mikooo;

import wah.mikooo.MediaPlayer.Player;
import wah.mikooo.MediaPlayer.SongBoard;
import wah.mikooo.Ui.AWTbs;

import javax.sound.sampled.*;
import java.io.*;


public class Main  {
    static Player player;
    static Main yes;
    static Thread playerThread;
    static Thread uiThread;
    public static AWTbs ui;


    public Main() throws UnsupportedAudioFileException, IOException, LineUnavailableException, InterruptedException {
        ui = new AWTbs(this);

        BufferedReader command = new BufferedReader(new InputStreamReader(System.in));
        out:
        while (true) {
            String cmd = null;
            try {
                cmd = command.readLine().split(" ")[0];
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("cmd");

            switch (cmd) {
                case "exit":
                    System.out.println("Terminating requested by user");
                    System.exit(0);
                    break out;
                case "p":
                    player.pause();
                    break;
                case "pl":
                    player.play();
                    break;
                case "n":
                    player.next();
                    break;
                case "pv":
                    player.prev();
                    break;
                case "t":
                    player.playAll();
                    break;
                case "preload":
                    player.loadAllSongsIntoMemoryThisIsAVeryBadIdea();
                    System.out.println("I think it finished");
                    break;
                case "m":
                    player.sb.getCurrentlyPlaying();
                    break;
            }


            command = new BufferedReader(new InputStreamReader(System.in));
        }
    }


    public Player getPlayer() {
        return player;
    }











    public static void main(String[] args) {
        System.out.println("Hewwo world!");

        try {

            // ui thread
            uiThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {

                        yes = new Main();

                    } catch(InterruptedException e) {
                        e.printStackTrace();
                    } catch (UnsupportedAudioFileException e) {
                        throw new RuntimeException(e);
                    } catch (LineUnavailableException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            // player backend thread
            player = new Player(new SongBoard());
            playerThread = new Thread(player);
            System.out.println("Starting threads");
            uiThread.start();
            playerThread.start();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Goodbye world!");

    }

}