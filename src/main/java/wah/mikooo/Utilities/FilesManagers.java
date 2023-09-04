package wah.mikooo.Utilities;

import wah.mikooo.MediaPlayer.Song;
import wah.mikooo.Ui.MainWindow;
import wah.mikooo.ffmpegStuff.MetadataExtractor;

import java.io.File;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FilesManagers {
    public static final int MAX_SCANNER_THREADS = Integer.parseInt(MainWindow.config.retrieve("max_scanner_threads"));
    public static final long MAX_SCAN_TIME = Integer.parseInt(MainWindow.config.retrieve("max_scan_time_seconds"));
    public static boolean CLEAN_SCAN = Boolean.parseBoolean(MainWindow.config.retrieve("clean_scan"));
    static final boolean recursive = Boolean.parseBoolean(MainWindow.config.retrieve("recursive_scan"));
    static final String libraryPath = MainWindow.config.retrieve("library_path");

    List<Song> availFiles; // data structure of all songs scanned


    public FilesManagers() {
        availFiles = new ArrayList<>();

        if (!new File(libraryPath).exists()) {
            new File(libraryPath).mkdir();
        }
    }

    /**
     * Scanner entry point. This will use the libraryPath.
     * The default is to rescan the directory instead of dynamically add/remove songs
     * @return
     */
    public List<Song> scanner() throws InterruptedException {
        return scanner(CLEAN_SCAN);
    }

    /**
     * Scanner entry point. This will use the libraryPath as the root directory.
     * @return avalFiles List of songs
     */
    public List<Song> scanner(boolean clean) throws InterruptedException {
        // create new list when requested,
        if (clean) {
            availFiles = new ArrayList<>();
        }

        if (!recursive) {
            scan(libraryPath);
        }
        else  {
            scanInclSubDir(libraryPath);
        }

        // thread pool for concurrent scanning
        System.out.println("Beginning metadata/album art extraction");
        ThreadPoolExecutor metadataExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_SCANNER_THREADS);
        for (Song s: availFiles) {
            metadataExecutor.submit(new MetadataExtractor(s));
        }

        metadataExecutor.shutdown();
        boolean execPoolStatus = metadataExecutor.awaitTermination(MAX_SCAN_TIME, TimeUnit.SECONDS);
        System.out.println("Active count: " + metadataExecutor.getActiveCount());
        System.out.println(("Queue count: " + metadataExecutor.getQueue().size()));

        if (!execPoolStatus) {
            System.out.println("WARNING: Scanner hit timeout before completing. ");
        }

        return availFiles;
    }

    /**
     * Scan for songs in current folder
     * @param path folder to scan
     * @return List of subdirectories
     */
    private List<String> scan(String path) {
        System.out.println("scan iteratopn");
        List<String> subfolders = new LinkedList<>();

        // sort between subfolders and files
        Arrays.stream(Objects.requireNonNull(new File(path).listFiles())).forEach(file -> {
            System.out.println(file.getAbsolutePath());
            if (file.isDirectory()) {
//                System.out.println("NOT adding " + file.getAbsolutePath());
                if (recursive) {
                    subfolders.add(file.getAbsolutePath());
                }
            }
            else if (!availFiles.stream().anyMatch(song -> song.path.equals(file.getAbsolutePath()))) {
                availFiles.add(new Song(file.getAbsolutePath()));
//                System.out.println("adding " + file.getAbsolutePath());
            }
        });

        return subfolders;
    }


    /**
     * Scan for songs, including subfolders. Recursive.
     * @param path folder to scan
     */
    private void scanInclSubDir(String path) {
        List<String> subfolders = scan(path);

        for (String folder: subfolders) {
            scanInclSubDir(folder);
        }
    }


}
