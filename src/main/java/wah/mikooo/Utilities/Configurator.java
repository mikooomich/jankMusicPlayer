package wah.mikooo.Utilities;


import wah.mikooo.MediaPlayer.Player;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;


public class Configurator {

    private HashMap<String, String> settings = new HashMap<>();


    public Configurator() throws FileNotFoundException {
        BufferedReader configRead = new BufferedReader(new FileReader(new File("./config.wah")));

        configRead.lines().forEach(
               line -> {
                   // format is name=value
//                   String[] args = line.split("=");
                   if (!line.startsWith("#") && line.contains("=")) {
                       settings.put(line.substring(0, line.indexOf("=")), line.substring(line.indexOf("=")+1));
                   }
               });


     System.out.println("aaaaa");
    }

    /**
     * Retrieve setting
     * @param key
     * @return
     */
    public String retrieve(String key) {
        return settings.get(key);
    }

//    public static int set() {
//        // disallow setting of some values here?
//    }

}
