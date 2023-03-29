package wah.mikooo;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class FilesManagers {
    List<Song> availFiles;
    final boolean recursive = false;
    public FilesManagers() {
        availFiles = new ArrayList<>();
    }

    public List<Song> scan() {
        if (!recursive) {

            Arrays.stream(Objects.requireNonNull(new File("./stronghold").listFiles())).forEach(file -> {
                //test for audio files
//            if (file.getName().contains("wav")) {
                availFiles.add(new Song(file.getName()));
//            }
            });

        }

        return availFiles;
    }


}
