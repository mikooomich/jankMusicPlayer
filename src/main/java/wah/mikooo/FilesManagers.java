package wah.mikooo;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
//        }

    }

    public List<Song> scan() {
        if (!recursive) {

            Arrays.stream(Objects.requireNonNull(new File(libraryPath).listFiles())).forEach(file -> {
                //test for audio files
//            if (file.getName().contains("wav")) {
                availFiles.add(new Song(file.getAbsolutePath()));
//            }
            });

        }

        return availFiles;
    }


}
