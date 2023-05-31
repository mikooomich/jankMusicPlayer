package wah.mikooo.MediaPlayer;

import wah.mikooo.Utilities.FilesManagers;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Queue data structure
 */
public class SongBoard {

    List<Song> queue; // next queue
    List<Song> prevQueue; // previous songs
    List<Song> availSongs; // all songs

    /**
     * Create a SongBoard, initialize by scanning the directory/ies
     */
    public SongBoard() {
        queue = new LinkedList<>();
        prevQueue = new LinkedList<>();
        rescan();
        System.out.println("Scanner completed. Found " + availSongs.size());
    }


    /**
     * Get all available songs
     */
    public void rescan() {
        FilesManagers fm = new FilesManagers();
        try {
            availSongs = fm.scanner();
        }
        catch (Exception e) {
            System.out.println("some unfortunate scanner error: " + Arrays.toString(e.getStackTrace()));
        }



        // remove invalid songs
        try {
            System.out.println("SCANNING AND REMOVING INVALIDS");
            List<Song> toRemove = availSongs.stream().filter(song -> song.status == -1).toList();
            toRemove.forEach(song -> {availSongs.remove(song); System.out.println("removed " + song.path);});
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("SCANNER TASK FINISHED");

    }


    /**
     * Enqueue a song
     * @param s single song
     */
    public void enqueue(Song s) {
        queue.add(s);
    }


    /**
     * Enqueue a list of songs
     * @param s list of songs
     */
    public void enqueue(List<Song> s) {
        queue.addAll(s);
    }


    /**
     * Enqueue a song after the currently playing song (index 0 of next)
     * @param s single song
     */
    public void enqueueNext(Song s) {
        if (queue.size() <= 1) {
            enqueue(s);
            return;
        }
        queue.add(1,s);
    }


    /**
     * Skip to index.
     * The song index provided will become the current playing song
     * ex. indexTo[1] >>>>>> list[0]
     * @param indexTo
     */
    public void advance(int indexTo) {

        for (int i = 0; i < indexTo; i++) {
            try {
                // add skipped songs to previous
                prevQueue.add(0, queue.remove(0));
            }
            catch (IndexOutOfBoundsException err) {
                System.out.println("There is no more, cannot advance");
                throw new IndexOutOfBoundsException("There is no more, cannot advance (next queue is empty)");
            }
        } // for
    }


    /**
     * Reverse of advance()
     * @param indexTo
     */
    public void deAdvance(int indexTo) {

        for (int i = 0; i < indexTo; i++) {
            try {
                // yeet the last element of prev to first element of queue
                queue.add(0,prevQueue.remove(0));
            }
            catch (IndexOutOfBoundsException err) {
                System.out.println("There is no more, cannot de-advance");
                throw new IndexOutOfBoundsException("There is no more, cannot de-advance (prev queue is empty)");
            }
        } // for
    }


    /**
     * Skip to next song
     * @return song (not really used at the moment)
     */
    public Song getNext() {
        try {
            advance(1);
            return queue.get(0);
        }
        catch (IndexOutOfBoundsException err) {
            System.out.println("The queue is empty");
            return null;
        }
    }


    /**
     * Reverse of getNext()
     * @return song (not really used at the moment)
     */
    public Song getPrev() {
        try {
            deAdvance(1);
            return queue.get(0);
        }
        catch (IndexOutOfBoundsException err) {
            System.out.println("The previous songs queue is empty, there are no previous songs");
            return null;
        }
    }


    /**
     * Get the next song in the queue without advancing (also doubles as is queue empty)
     * @return
     */
    public Song peekNext() {
        try {
            return queue.get(1);
        }
        catch (Exception e) {
            return null;
        }
    }

    public Song peekPrev() {
        try {
            return prevQueue.get(0);
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * Get currently playing song list[0]
     * @return
     */
    public Song getCurrentlyPlaying() {
        if (queue.size() < 1) {
            System.out.println("The queue is empty");
            return null;
        }

        return queue.get(0);
    }


    /**
     * Get array of previous songs
     * @return
     */
    public String[] printPrev() {
        // append the list in reverse
        String[] display = new String[prevQueue.size()];
        int index = prevQueue.size() - 1;
        for (Song s: prevQueue) {
            display[index] = s.path;
            index--;
        }
        return display;
    }


    /**
     * Get array of next songs
     * @return
     */
    public String[] printNext() {
        String[] display = new String[queue.size()];
        int index = 0; // skip now playing song
        for (Song s: queue) {
            if (index == 0) {
                index++;
                continue;
            }
           display[index] = s.path;
           index++;
        }
        return display;
    }


    /**
     * Get array of available songs
     * @return
     */
    public String[] printAvail() {
        String[] display = new String[availSongs.size()];
        int index = 0; // skip now playing song
        for (Song s: availSongs) {
            display[index] = s.path;
            index++;
        }
        return display;
    }

    public List<Song> getNextQueue() {
        return queue;
    }

    public List<Song> getPrevQueue() {
        return prevQueue;
    }




    /**
     * =========================
     * Getters for indexes/sizes
     * =========================
     */

    public int getCurrentQueueSize() {

        return prevQueue.size() + queue.size();
    }

    public int getRemainingSize() {
        return queue.size() - 1;
    }

    /**
     * Retrives the index of the currently playing song
     * @return
     */
    public int getCurrentIndex() {
        return getCurrentQueueSize() - getRemainingSize() + 1; // current index == pased index + 1
    }


}
