package wah.mikooo.Ui;


import wah.mikooo.Main;
import wah.mikooo.MediaPlayer.Player;
import wah.mikooo.MediaPlayer.Song;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;


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

        /**
         * Previous songs
         */
        // Create the two selectable lists
        int indexofSong = 0;
        List<SongButton> prevSongsAsButtons = new ArrayList<>();

        for (Song s: Player.sb.getPrevQueue()) {
            prevSongsAsButtons.add(new SongButton(indexofSong, s.path, interfaceLink.getPlayer()));
            indexofSong++;
        }


        // Create the two selectable lists
        JTable table1; // previous songs
        DefaultTableModel tableModel;

        tableModel = new DefaultTableModel();
        tableModel.addColumn("Name"); // Add column header

        for (SongButton wah : prevSongsAsButtons) {
            tableModel.addRow(new Object[]{wah.alias}); // Add row data
        }

        table1 = new JTable(tableModel);
        table1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table1.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int selectedRow = table1.getSelectedRow();
                    if (selectedRow >= 0) {
                        SongButton selectedWah = prevSongsAsButtons.get(selectedRow);
                        selectedWah.doAction();
                    }
                }
            }
        });

        JScrollPane scrollPane1 = new JScrollPane(table1);
        scrollPane1.setPreferredSize(new Dimension(600, 270));

// Add the scroll pane to the panel
        rightPanel.add(scrollPane1, BorderLayout.NORTH);


        /**
         * Next songs
         */
        indexofSong++; // skip current song

        List<SongButton> nextongsAsButtons = new ArrayList<>();

        for (Song s: Player.sb.getNextQueue()) {
            nextongsAsButtons.add(new SongButton(indexofSong, s.path, interfaceLink.getPlayer()));
            indexofSong++;
        }



        JList<String> list2; // next songs
        DefaultListModel<String> listModelNext;

        listModelNext = new DefaultListModel<>();
        for (SongButton wah : nextongsAsButtons) {
            listModelNext.addElement(wah.alias);
        }

        list2 = new JList<>(listModelNext);
        list2.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        list2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int index = list2.locationToIndex(evt.getPoint());
                if (index >= 0) {
                    SongButton selectedWah = nextongsAsButtons.get(index);
                    selectedWah.doAction();
                }
            }
        });




//        JScrollPane prevSongs = new JScrollPane(list1);
//        prevSongs.setPreferredSize(new Dimension(600, 270));
        JScrollPane nextSongs = new JScrollPane(list2);
        nextSongs.setPreferredSize(new Dimension(600, 270));


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
//        rightPanel.add(prevSongs, BorderLayout.NORTH);
        rightPanel.add(textArea, BorderLayout.CENTER);
        rightPanel.add(imagePanel, BorderLayout.EAST);
        rightPanel.add(nextSongs, BorderLayout.SOUTH);
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
