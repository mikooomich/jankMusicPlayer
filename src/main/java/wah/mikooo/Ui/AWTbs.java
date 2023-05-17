package wah.mikooo.Ui;


import wah.mikooo.Main;
import wah.mikooo.MediaPlayer.Player;
import wah.mikooo.MediaPlayer.Song;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import javax.swing.*;



public class AWTbs extends JFrame {
    static Main interfaceLink;

    public AWTbs(Main interfaces) {
        interfaceLink = interfaces;

        setName("AIYA");
//        JButton exitButton = new JButton("exit");
//        exitButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                System.out.println("Terminating requested by user");
//                System.exit(0);
//            }
//        });
//
//
//
////
//        exitButton.setSize(100,100);
//        exitButton.setLocation(100, 200);


        JButton playButton = new JButton("play");
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Play button");
                interfaceLink.getPlayer().play();
                draw();
            }
        });
        JButton pauseButton = new JButton("pause");
        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("pause button");
                interfaceLink.getPlayer().pause();
                draw();
            }
        });
        JButton nextButton = new JButton("next");
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("next button");
                interfaceLink.getPlayer().next();
                draw();
            }
        });
        JButton prevButton = new JButton("prev");
        prevButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("prev button");
                interfaceLink.getPlayer().prev();
                draw();
            }
        });


        JButton addALLButton = new JButton("queue all");
        addALLButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("addALL button");
                interfaceLink.getPlayer().playAll();
                draw();
                validate();
            }
        });


        JButton forceRefreshUi = new JButton("Force Refresh UI");
        forceRefreshUi.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                draw();
            }
        });



        JPanel navBar = new JPanel(new FlowLayout(FlowLayout.CENTER));

        navBar.add(prevButton);
        navBar.add(playButton);
        navBar.add(pauseButton);
        navBar.add(nextButton);
        navBar.add(addALLButton);
        navBar.add(forceRefreshUi);
        add(navBar, BorderLayout.SOUTH);



        // Show the GUI
        setVisible(true);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(new Dimension(1280,720));
    }




    // draw ui
    public void draw() {
        try {
            Thread.sleep(400);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Create the panel for the right half of the screen
        JPanel rightPanel = new JPanel(new BorderLayout());

        // Create the two selectable lists
        JList<String> list1 = new JList<>(Player.sb.printPrev());
        JList<String> list2 = new JList<>(Player.sb.printNext());


        JScrollPane l1scrl = new JScrollPane(list1);
        l1scrl.setPreferredSize(new Dimension(600, 270));
        JScrollPane l2scrl = new JScrollPane(list2);
        l2scrl.setPreferredSize(new Dimension(600, 270));


        // Create the text printout
        JLabel textArea = new JLabel();


        Song currentSOng = Player.sb.getCurrentlyPlaying();
        JPanel imagePanel = new JPanel();


        BufferedImage img = currentSOng.albumArt;
        if (img != null) {


        ImageIcon image = new ImageIcon(currentSOng.albumArt.getScaledInstance(100, 100, Image.SCALE_DEFAULT)); // Replace with your own image path


        JLabel imageLabel = new JLabel(image);

        imagePanel.add(imageLabel);
    }
//        JScrollPane scrollPane = new JScrollPane(textArea);


        String statusText = "Temporary ui, it hopefully works well enough for testing <br>";

        try {
            statusText += "Paused = " + Player.paused +
                    "-----  autoplay = " +Player.autoplay +
                    "----- volume = " +Player.defaultVolume;

            try {
                statusText += "<br> Streaming = " +Player.mouth.useStreaming +
                        "-----  Force Stream = " +Player.mouth.useStreaming;
            }
            catch (Exception e) {
            }


            statusText += ("<br> Now playing: "+currentSOng.path);
            statusText += "<br> " + currentSOng.title + currentSOng.album +currentSOng.length +currentSOng.codec +currentSOng.date +currentSOng.sampleRate +currentSOng.artist;
        }
        catch (Exception e) {
        }


        textArea.setText("<html>"+statusText+"</html>");

        // Add the two lists and the text printout to the panel
        rightPanel.add(l1scrl, BorderLayout.NORTH);
        rightPanel.add(textArea, BorderLayout.CENTER);
        rightPanel.add(imagePanel, BorderLayout.EAST);
        rightPanel.add(l2scrl, BorderLayout.SOUTH);
        add(rightPanel, BorderLayout.CENTER);

        // Create the panel for the left half of the screen
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(500, 70));
        // Create the large selectable list
        JList<String> bigList = new JList<>(Player.sb.printAvail());

        // Add the large list to the panel
        leftPanel.add(new JScrollPane(bigList), BorderLayout.CENTER);
        add(leftPanel, BorderLayout.WEST);
        validate();
    }

    public static void main(String[] args) {
        AWTbs wwwww = new AWTbs(null);
    }

}
