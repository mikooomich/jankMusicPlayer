package wah.mikooo.Utilities;


import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


public class Configurator {
	private HashMap<String, String> defaults;

	private HashMap<String, String> settings;

	private static final String CONFIG_VERSION = "0.0.1";
	public static final String[] AVAIL_SETTINGS = {"max_scanner_threads",
			"max_scan_time_seconds",
			"clean_scan",
			"recursive_scan",
			"library_path",
			"ffmpeg",
			"ffprobe",
			"autoplay",
			"previous_button_acts_as_replay_seconds",
			"volume",
	};

	public static final String[] AVAIL_BOOL_SETTINGS = {
			"clean_scan",
			"recursive_scan",
			"autoplay"
	};


	public Configurator() throws FileNotFoundException {

		makeDefaults();
		settings = new HashMap<>();


		try {
			BufferedReader configRead = new BufferedReader(new FileReader(new File("./config.wah")));

			configRead.lines().forEach(
					line -> {
						// format is name=value
						if (!line.startsWith("#") && line.contains("=")) {
							String key = line.substring(0, line.indexOf("="));

							switch (key) {
								// booleans
								case "autoplay", "clean_scan", "recursive_scan" ->
									// read all invalid bool values as false
										settings.put(line.substring(0, line.indexOf("=")), line.substring(line.indexOf("=") + 1));

								// check numbers are numbers
								case "volume" -> {
									System.out.println("aaa");
									try {
										Integer.parseInt(line.substring(line.indexOf("=") + 1));
									} catch (NumberFormatException e) {
										settings.put(key, defaults.get(key));
										break;
									}

									settings.put(key, line.substring(line.indexOf("=") + 1));
									break;
								}


								default -> settings.put(key, line.substring(line.indexOf("=") + 1));
							}

						}
					});

		} catch (FileNotFoundException e) {
			saveConfig();
		}

		System.out.println("aaaaa");
	}


	public static void main(String[] args) throws FileNotFoundException {
		Configurator tester = new Configurator();
		tester.saveConfig(true);
	}

	/**
	 * Create default values hashmap
	 */
	private void makeDefaults() {
		defaults = new HashMap<>();

		defaults.put("max_scanner_threads", "4");
		defaults.put("max_scan_time_seconds", "5");
		defaults.put("clean_scan", "true");
		defaults.put("recursive_scan", "false");
		defaults.put("library_path", "./stronghold");

		defaults.put("ffmpeg", "ffmpeg");
		defaults.put("ffprobe", "ffprobe");

		defaults.put("autoplay", "true");
		defaults.put("previous_button_acts_as_replay_seconds", "10");
		defaults.put("volume", "0");
	}


	public void saveConfig() {
		saveConfig(false);
	}

	public void saveConfig(boolean writeDefaults) {
		final String infoText = "# Hi. equal sign (=) separates the key and value. Use hashtag (#) for comments.\n" +
				"# Note: comments will always be at the top when saving settings\nversion=0.0.1";
		List<String> comments = new LinkedList<>();


		// copy comments from old config file
		try {
			BufferedReader configRead = new BufferedReader(new FileReader(new File("./config.wah")));
			comments = new LinkedList<>();
			List<String> finalComments = comments;
			configRead.lines().forEach(
					line -> {
						// format is name=value
						if (!line.startsWith("#")) {
							finalComments.add(line);
						}
					}
			);
		} catch (FileNotFoundException e) {
			writeDefaults = true;
		}


		// write new config
		FileWriter out = null;

		try {
			out = new FileWriter("./config.wah");
			if (writeDefaults) {
				out.write(infoText);
			} else {
				FileWriter finalOut = out;
				comments.forEach(i -> {
					try {
						finalOut.append(i);
					} catch (IOException e) {
						System.out.println("FAILED to write setting: " + i);
					}
				});
			}


			out.append(CONFIG_VERSION + "\n");
			out.flush();


			// write the setting. Pull default if not found
			for (String s : AVAIL_SETTINGS) {
				if (!settings.containsKey(s)) {
					out.append(s + "=" + defaults.get(s) + "\n");
				} else {
					out.append(s + "=" + settings.get(s) + "\n");
				}
			}

			out.close();
		} catch (IOException e) {
			System.out.println("ERROR SAVING FILE");
			try {
				out.close();
			} catch (Exception x) {
			}
		}
	}

	/**
	 * Retrieve setting
	 *
	 * @param key
	 * @return
	 */
	public String retrieve(String key) {
		return settings.get(key);
	}

	/**
	 * Assign new value to setting
	 * <p>
	 * 0 - Sucess
	 * -1 - Key not found
	 *
	 * @return exit status
	 */
	public int set(String key, boolean value) {
		// disallow setting of some values here?
		if (settings.containsKey(key)) {

			if (key.contains("_BOOL")) {
				settings.put(key, String.valueOf(value));
				return 0;
			}
		}

		return -1;
	}


	/**
	 * Assign new value to setting
	 * <p>
	 * 0 - Success
	 * -1 - Key not found
	 *
	 * @return exit status
	 */
	public int set(String key, String value) {
		// disallow setting of some values here?
		if (settings.containsKey(key)) {
			settings.put(key, value);
			return 0;
		}

		return -1;
	}


	/**
	 * Assign new value to setting.
	 * Volume above 12db will be limited to 12db, volume below -50db will be treated as mute.
	 * <p>
	 * 0 - Success
	 * -1 - Key not found
	 *
	 * @return exit status
	 */
	public <E extends Number> int set(String key, E value) {
		// disallow setting of some values here?
		if (settings.containsKey(key)) {

			if (key.compareTo("volume") == 0) { // decimal values
				float newVal = 0;

				if (value.floatValue() > 12) {
					newVal = 12;
				}
				if (value.floatValue() < -50) {
					newVal = Float.MIN_VALUE;
				}

				settings.put(key, String.valueOf(newVal));
			} else { // int values
				settings.put(key, String.valueOf(value.intValue()));
			}

		}

		return -1;
	}
}
