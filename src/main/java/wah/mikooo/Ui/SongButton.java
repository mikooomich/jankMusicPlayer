package wah.mikooo.Ui;

import wah.mikooo.MediaPlayer.Player;


public class SongButton {

	final String alias;
	int indexx;
	Player playerLink;

	/**
	 * Adapter for UI
	 *
	 * @param index      Index of song in both queues combined
	 * @param alias      Display name
	 * @param jumpToLink
	 */
	public SongButton(int index, String alias, Player jumpToLink) {
		this.indexx = index;
		this.alias = alias;
		playerLink = jumpToLink;
	}

	/**
	 * Calls the jump to method to jump to this song
	 */
	public void doAction() {
		System.out.println("TRY Jumping to: " + alias + " INDEX = " + indexx);
		playerLink.jumpto(indexx);
	}


}
