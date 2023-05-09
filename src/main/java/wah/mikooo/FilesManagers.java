package wah.mikooo;

import java.io.File;
import java.util.*;

public class FilesManagers {
    List<Song> availFiles;
    static final boolean recursive = false;
    static final String libraryPath = "./stronghold";


    public FilesManagers() {
        availFiles = new ArrayList<>();
//        if ( found custom ffpmeg) {
//            Player.ffmpegBinary = from config;
//        }
//        else {
            System.out.println("Setting player to use system path ffmpeg");
            Player.ffmpegBinary = "ffmpeg";
            Player.ffprobeBinary = "ffprobe";
//        }

    }

    /**
     * Scanner entry point. This will use the libraryPath.
     * The default is to rescan the directory instead of dynamically add/remove songs
     * @return
     */
    public List<Song> scanner() {
        return scanner(true);
    }

    /**
     * Scanner entry point. This will use the libraryPath.
     * @return
     */
    public List<Song> scanner(boolean rescan) {
        if (rescan) {
            availFiles = new ArrayList<>();
        }


        if (!recursive) {
            scan(libraryPath);
        }
        else  {
            scanInclSubDir(libraryPath);
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
            else if (true && !availFiles.stream().anyMatch(song -> song.path == file.getAbsolutePath())) { // replace true with test for audio files here
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
