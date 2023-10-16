package wah.mikooo.Ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import wah.mikooo.MediaPlayer.Player;
import wah.mikooo.MediaPlayer.Song;
import wah.mikooo.MediaPlayer.SongBoard;
import wah.mikooo.Utilities.Configurator;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import static wah.mikooo.MediaPlayer.Player.defaultVolume;
import static wah.mikooo.MediaPlayer.Player.sb;
import static wah.mikooo.Utilities.Configurator.AVAIL_BOOL_SETTINGS;

public class MainWindow extends Application {
	// links to backend
	static Player player;
	static Thread playerThread;
	public static Configurator config;

	// main elements
	static BorderPane main;
	static VBox controlsBox;
	static VBox navbar;
	static HBox topSHit;
	static VBox bottomShit;
	static VBox settingsPane;


	// pieces idk what to call this
	static Slider seekBar;
	static Label artist;
	static Label title;
	static VBox songTitles;


	@Override
	public void start(Stage stage) throws InterruptedException {
		Thread brains = new Thread(new Runnable() {
			public void run() {

				try {
					config = new Configurator();
				} catch (FileNotFoundException e) {
					throw new RuntimeException(e);
				}

				try {
					// player backend thread
					player = new Player(new SongBoard());
					playerThread = new Thread(player);
					System.out.println("Starting threads");
					//                uiThread.start();
					playerThread.start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});


		brains.start();

		// Hi so uhhh you know what's the best solution to a race condition? Ignoring the problem lol
		Thread.sleep(2000);


		main = new BorderPane();
		HBox root = new HBox();
		Scene scene = new Scene(main, 800, 600);
		scene.setFill(Color.GRAY);
		root.setPadding(new Insets(10, 10, 10, 10));
		root.setSpacing(20);


		// Player buttons
		Button play = new Button("Play");
		Button pause = new Button("pause");
		Button next = new Button("next");
		Button prev = new Button("prev");
		Button load = new Button("queue all");

		EventHandler<ActionEvent> playTrigger = new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				System.out.println("Play button");
				player.play();
			}
		};
		EventHandler<ActionEvent> pauseTrigger = new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				System.out.println("pause button");
				player.pause();
			}
		};
		EventHandler<ActionEvent> prevTrigger = new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				System.out.println("prev button");
				player.prev();
			}
		};
		EventHandler<ActionEvent> nextTrigger = new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				System.out.println("next button");
				player.next();
			}
		};
		EventHandler<ActionEvent> addAllTrigger = new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				System.out.println("addALL button");
				player.playAll();
			}
		};


//        Button redrawButt = new Button("redraw");
//        EventHandler<ActionEvent> redraw = new EventHandler<ActionEvent>() {
//            public void handle(ActionEvent e) {
//                System.out.println("redarw button");
//               redrawTitles();
//            }
//        };


		play.setOnAction(playTrigger);
		pause.setOnAction(pauseTrigger);
		next.setOnAction(nextTrigger);
		prev.setOnAction(prevTrigger);
		load.setOnAction(addAllTrigger);
//        redrawButt.setOnAction(redraw);

		root.getChildren().add(play);
		root.getChildren().add(pause);
		root.getChildren().add(next);
		root.getChildren().add(prev);
		root.getChildren().add(load);
//        root.getChildren().add(redrawButt);


		// Put shit where it should go
		topSHit = new HBox();
		controlsBox = new VBox();
		navbar = new VBox();
		bottomShit = new VBox();
		settingsPane = new VBox();

		redrawTitles();
		drawCenter();
		drawPlayerCtrls();
		drawSettingsPane();
		bottomShit.getChildren().add(root);
		seekBar = new Slider();
		bottomShit.getChildren().add(seekBar);


		seekBar.setOnMouseReleased(event -> {
			System.out.println("SEEKING TO" + seekBar.getValue());
			player.seekTo((int) seekBar.getValue());
		});


		main.setRight(controlsBox);
		main.setLeft(navbar);
		main.setTop(topSHit);
		main.setBottom(bottomShit);
		topSHit.getChildren().add(settingsPane);


		main.setStyle("-fx-background-color: gray");
		stage.setScene(scene);
		stage.show();
		stage.setTitle("WAAAAAAAAH");
	}


	private void drawSettingsPane() {
		/**
		 * So I have no better way to this than having individual event handlers
		 * and then just do the simple "manually assign the handlers". ugly though.
		 */
		EventHandler<ActionEvent> autoPLayToggle = new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				System.out.println("toggling autoplay " + e);
				player.setAutoplay();
			}
		};


		// for now just barf out everything on there

		List<CheckBox> toggles = new ArrayList<>();

		// Load all booleans
		for (String s : AVAIL_BOOL_SETTINGS) {
			System.out.println(s);
			CheckBox checkBox = new CheckBox(s);
			String val = config.retrieve(s);
			checkBox.setSelected(Boolean.parseBoolean(config.retrieve(s)));
			toggles.add(checkBox);


			switch (s) {
				case "autoplay":
					checkBox.setOnAction(autoPLayToggle);
					break;
			}

			settingsPane.getChildren().add(checkBox);
		}

	}


	public static void main(String[] args) {
		System.out.println("Hewwo world!");
		launch();
		playerThread.interrupt();
		System.out.println("Goodbye world!");
	}

	/**
	 * Update the position of the seek bar
	 *
	 * @param s Song
	 */
	public static void updateSongPos(Song s) {
		seekBar.setMax(s.length);
		seekBar.setMin(0);
		seekBar.setValue(Player.Mouth.getCurrentPosMs());
	}

	/**
	 * Draw the player controls
	 */
	public static void drawPlayerCtrls() {
		Slider slider = new Slider();
		slider.setOrientation(Orientation.VERTICAL);
		slider.setMin(-40);
		slider.setMax(5);
		slider.setValue(defaultVolume);
		slider.setMajorTickUnit(10);
		slider.setShowTickLabels(true);


		Button more = new Button("more");
		Button info = new Button("Info");
		Button showLrc = new Button("Lyrics");

		Button shuffle = new Button("SHuffle");
		Button repeat = new Button("Repeat");


		EventHandler<ActionEvent> moreTrigger = new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				System.out.println("more button");
			}
		};
		EventHandler<ActionEvent> infoTrigger = new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				System.out.println("info button");
			}
		};
		EventHandler<ActionEvent> shuffleTrigger = new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				System.out.println("shuffle button");
			}
		};
		EventHandler<ActionEvent> repeatTrigger = new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				System.out.println("repeat button");
			}
		};



		EventHandler<ActionEvent> showLyric = new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				System.out.println("toggle show lyrics");
				player.toggleUseLyrics();
				drawCenter();

				if (sb.getCurrentlyPlaying().lyrics != null) {
					sb.getCurrentlyPlaying().lyrics.startSession(); // start lyric printer
				}

			}
		};

		// maybe use this for volume
//        slider.valueProperty().addListener( new ChangeListener<Number>() {
//
//            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
//
//                System.out.println("slider val=== " + newValue);
//            }
//        });
		slider.setOnMouseReleased(event -> {
			System.out.println(slider.getValue());
			player.changeVolume((float) slider.getValue());
		});


		more.setOnAction(moreTrigger);
		info.setOnAction(infoTrigger);
		shuffle.setOnAction(shuffleTrigger);
		repeat.setOnAction(repeatTrigger);
		showLrc.setOnAction(showLyric);


		navbar.getChildren().add(info);
		navbar.getChildren().add(more);
		navbar.getChildren().add(showLrc);

		controlsBox.getChildren().add(shuffle);
		controlsBox.getChildren().add(repeat);
		controlsBox.getChildren().add(slider);

	}


	/**
	 * Draw album art
	 */
	public static void drawCenter() {
		drawCenter(null);
	}

	/**
	 * Draw the center album art and lyric combo
	 * Provide null to draw album art, else a string for lyrics.
	 * I am likely heavily violating good programming practices.
	 *
	 * @param lyrics
	 */
	public static void drawCenter(String lyrics) {
		/**
		 * TODO: avoid loading image on every lyric request
		 */
		ImageView imageView;
		VBox centerShit = new VBox();
		centerShit.setPrefSize(400, 400);

		if (sb == null || sb.getCurrentlyPlaying() == null) {
			// draw placeholder
			try {

				imageView = new ImageView(new Image(new FileInputStream("./placeholder.png")));
				imageView.setFitWidth(400);
				imageView.setPreserveRatio(true);
				centerShit.getChildren().add(imageView);
			} catch (FileNotFoundException e) {
				Rectangle r = new Rectangle(0, 0, 400, 400);
				r.setFill(Color.BLUE);
				centerShit.getChildren().add(r);
			}
		} else if (lyrics == null && !player.getUseLyrics()) {
			// draw art instead of lyrics when lyrics are present and enabled
			System.out.println("drawing image\n\n\n\n\nAAAAAAAAAAAAAAAAAAAAAAAAAA");
			Image art = sb.getCurrentlyPlaying().albumArt;

			if (art == null) {
				imageView = new ImageView(new Image("./placeholder.png"));
			} else {
				imageView = new ImageView(art);
				imageView.setOnMouseClicked(e -> {

					System.out.println("toggle show lyrics");
					player.toggleUseLyrics();
					drawCenter();

					if (sb.getCurrentlyPlaying().lyrics != null) {
						sb.getCurrentlyPlaying().lyrics.startSession(); // start lyric printer
					}
				});
			}

			imageView.setFitWidth(400);
			imageView.setPreserveRatio(true);
			centerShit.getChildren().add(imageView);
		} else {
			centerShit.getChildren().add(new Label(lyrics));
		}

		// why the fuck does this fix the not on FX thread error
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				main.setCenter(centerShit);
			}
		});
	}


	/**
	 * Draw artist and titles/ info bar
	 */
	public static void redrawTitles() {
		if (sb == null || sb.getCurrentlyPlaying() == null) {
			title = new Label("TITLE");
			artist = new Label("ARTIST");
		} else {
			title = new Label(sb.getCurrentlyPlaying().title);
			artist = new Label(sb.getCurrentlyPlaying().artist);
		}

		songTitles = new VBox();
		songTitles.getChildren().add(title);
		songTitles.getChildren().add(artist);


		Button settings = new Button("Settings");
		EventHandler<ActionEvent> settingsTrigger = new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				System.out.println("settings button");
			}
		};

		settings.setOnAction(settingsTrigger);


		// why the fuck does this fix the not on FX thread error
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				topSHit = new HBox();
				topSHit.getChildren().add(songTitles);
				topSHit.getChildren().add(settings);
			}
		});

	}

}