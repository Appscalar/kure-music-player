package com.kure.musicplayer.activities;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.kure.musicplayer.MusicController;
import com.kure.musicplayer.R;
import com.kure.musicplayer.kMP;
import com.kure.musicplayer.adapters.AdapterSong;


/**
 * It is the "Now Playing Queue".
 *
 * Tasks:
 *
 * - List all currently playing songs.
 * - Has a MediaController, little widgets with
 *   buttons to play, pause, skip, etc.
 * - Lets the user append songs to it at any time.
 * - Allows the user to select any song inside it to
 *   start playing right away.
 *
 * Interface:
 *
 * If you want to play a set of musics, set the
 * ArrayList<Song> on `kmP.nowPlayingList` with all
 * the songs you want.
 *
 * Then, send an Extra called "song" that contains
 * the global ID of the Song you want to start
 * playing.
 *
 * Another thing you can do is to send an extra
 * of key "sort" with any value accepted by the
 * function `MusicService.sortBy()`.
 * Then, the list will get sorted that way before
 * it starts playing.
 *
 * - If we don't find that ID on the list, we start
 *   playing from the beginning.
 * - The Extra is optional: if you don't provide it
 *   it does nothing.
 */
public class ActivityNowPlaying extends ActivityMaster
	implements MediaPlayerControl,
	           OnItemClickListener,
	           OnItemLongClickListener {

	/**
	 * List that will display all the songs.
	 */
	private ListView songListView;

	private boolean paused = false;
	private boolean playbackPaused = false;

	private MusicController musicController;

	/**
	 * Thing that maps songs to items on the ListView.
	 *
	 * We're keeping track of it so we can refresh the ListView
	 * if the user wishes to change it's order.
	 *
	 * Check out the leftmost menu and it's options.
	 */
	private AdapterSong songAdapter;

	/**
	 * Gets called when the Activity is getting initialized.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_now_playing);

		songListView = (ListView)findViewById(R.id.activity_now_playing_song_list);

		// We'll play this pre-defined list.
		// By default we play the first track, although an
		// extra can change this. Look below.
		kMP.musicService.setList(kMP.nowPlayingList);
		kMP.musicService.setSong(0);

		// Connects the song list to an adapter
		// (thing that creates several Layouts from the song list)
		songAdapter = new AdapterSong(this, kMP.nowPlayingList);
		songListView.setAdapter(songAdapter);

		// Looking for optional extras
		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();

		if (bundle != null) {

			// There's the other optional extra - sorting rule
			if (bundle.containsKey("sort"))
				kMP.musicService.sortBy((String)bundle.get("sort"));

			// If we received an extra with the song position
			// inside the now playing list, start playing it
			if (bundle.containsKey("song")) {
				int songToPlayIndex = (int)bundle.get("song");

				// Prepare the music service to play the song.
				// `setSong` does limit-checking
				kMP.musicService.setSong(songToPlayIndex);
			}

			kMP.musicService.playSong();
		}

		// Scroll the list view to the current song.
		songListView.setSelection(kMP.musicService.currentSongPosition);

		// We'll get warned when the user clicks on an item
		// and when he long selects an item.
		songListView.setOnItemClickListener(this);
		songListView.setOnItemLongClickListener(this);

		setMusicController();

		if (playbackPaused) {
			setMusicController();
			playbackPaused = false;
		}

		// While we're playing music, add an item to the
		// Main Menu that returns here.
		ActivityMenuMain.addNowPlayingItem(this);

		// Customizing ActionBar
		ActionBar actionBar = getActionBar();
		if (actionBar != null) {
			// Making sure the leftmost button (Home Button) is there
			actionBar.setHomeButtonEnabled(true);

			// Change it to a sweet custom icon
			actionBar.setIcon(R.drawable.ic_menu_more);
		}
	}

	/**
	 * Activates the ActionBar's leftmost drop-down menu.
	 *
	 * @note We're creating the menu EVERY TIME you call
	 *       this function! Hope it doesn't become too
	 *       slow on some phones.
	 *
	 * @note All of it's items are defined on
	 * `res/menu/activity_now_playing_action_bar_submenu.xml`.
	 */
	public void showSubmenu() {

		// The menu can't possibly work if there's no ActionBar
		ActionBar actionBar = getActionBar();
		if (actionBar == null)
			return;

		// And now we create the drop-down menu, attaching
		// it to the leftmost button (Home Button).
		//
		// To do so I need to get a reference to the leftmost
		// button's View...
		//
		// (Source: http://stackoverflow.com/a/21125631)
		Window window = getWindow();
		View view = window.getDecorView();
		int resID = getResources().getIdentifier("action_bar_container", "id", "android");

		// ...and create the PopupMenu, populating with the options...
		PopupMenu popup = new PopupMenu(this, view.findViewById(resID));
		MenuInflater menuInflater = popup.getMenuInflater();

		menuInflater.inflate(R.menu.activity_now_playing_action_bar_submenu,
				             popup.getMenu());

		// ... then we tell what happens when the user selects any of it's items.
		PopupMenu.OnMenuItemClickListener listener = new PopupMenu.OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {

				switch (item.getItemId()) {

				// Sorting options - after changing the now playing list
				// order, we must refresh the ListView and scroll to the
				// currently playing song.

				// Will sort current songs by title
				case R.id.action_bar_submenu_title:
					kMP.musicService.sortBy("title");
					songAdapter.notifyDataSetChanged();
					songListView.setSelection(kMP.musicService.currentSongPosition);
					return false;

				// Will sort current songs by artist
				case R.id.action_bar_submenu_artist:
					kMP.musicService.sortBy("artist");
					songAdapter.notifyDataSetChanged();
					songListView.setSelection(kMP.musicService.currentSongPosition);
					return false;

				// Will sort current songs by album
				case R.id.action_bar_submenu_album:
					kMP.musicService.sortBy("album");
					songAdapter.notifyDataSetChanged();
					songListView.setSelection(kMP.musicService.currentSongPosition);
					return false;

				// Will sort current songs by track number
				case R.id.action_bar_submenu_track:
					kMP.musicService.sortBy("track");
					songAdapter.notifyDataSetChanged();
					songListView.setSelection(kMP.musicService.currentSongPosition);
					return false;

				// Will sort current songs randomly
				case R.id.action_bar_submenu_random:
					kMP.musicService.sortBy("random");
					songAdapter.notifyDataSetChanged();
					songListView.setSelection(kMP.musicService.currentSongPosition);
					return false;

				// Will ask the user for a new Playlist name, creating
				// it with the current songs.
				//
				// If there's already a playlist with that name, we'll
				// append a silly string to the new Playlist name.
				case R.id.action_bar_submenu_new_playlist:

					// The input box where user will type new name
					final EditText input = new EditText(ActivityNowPlaying.this);

					// Creating the dialog box that asks the user,
					// with the question and options.
					new AlertDialog.Builder(ActivityNowPlaying.this)
						.setTitle(ActivityNowPlaying.this.getString(R.string.menu_now_playing_dialog_create_playlist_title))
						.setMessage(ActivityNowPlaying.this.getString(R.string.menu_now_playing_dialog_create_playlist_subtitle))
						.setView(input)

						// Creates the OK button, attaching the action to create
						// the Playlist
						.setPositiveButton(ActivityNowPlaying.this.getString(R.string.menu_now_playing_dialog_create_playlist_button_ok),
						                   new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog, int whichButton) {

								String playlistName = input.getText().toString();

								// TODO: Must somehow update the Playlist Activity if it's
								//       on the background!
								//       The ListView only updates when Playlist Menu gets
								//       created from scratch.
								kMP.songs.newPlaylist(ActivityNowPlaying.this,
								                      "external",
								                      playlistName,
								                      kMP.nowPlayingList);

								// Congratulating the user with the new Playlist name
								Toast.makeText(ActivityNowPlaying.this,
								               ActivityNowPlaying.this.getString(R.string.menu_now_playing_dialog_create_playlist_success) + playlistName,
								               Toast.LENGTH_SHORT).show();

							}

						// Creates the CANCEL button, that doesn't do nothing
						// (since a Playlist is only created when pressing OK).
						}).setNegativeButton(ActivityNowPlaying.this.getString(R.string.menu_now_playing_dialog_create_playlist_button_cancel),
						                     new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog, int whichButton) {
								// Do nothing, yay!
							}

						// Lol, this is where we actually call the Dialog.
						// Note for newcomers: The code continues to execute.
						//                     This is an asynchronous task.
						}).show();

					return false;
				}
				return false;
			}
		};

		// Phew! Activating the callbacks when someone
		// clicks the menu and showing it.
		popup.setOnMenuItemClickListener(listener);
		popup.show();
	}

	/**
	 * Icon that will show on the top menu showing if
	 * `shuffle` is on/off and allowing the user to change it.
	 */
	private MenuItem shuffleItem;

	/**
	 * Icon that will show on the top menu showing if
	 * `repeat` is on/off and allowing the user to change it.
	 */
	private MenuItem repeatItem;

	/**
	 * Let's create the ActionBar (menu on the top).
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_now_playing_action_bar, menu);

		shuffleItem = menu.findItem(R.id.action_bar_shuffle);
		repeatItem  = menu.findItem(R.id.action_bar_repeat);

		refreshActionBarItems();
		refreshActionBarSubtitle();

		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * Refreshes the icons on the Action Bar based on the
	 * status of `shuffle` and `repeat`.
	 *
	 * Source:
	 * http://stackoverflow.com/a/11006878
	 */
	private void refreshActionBarItems() {

		shuffleItem.setIcon((kMP.musicService.isShuffle())?
				             R.drawable.ic_menu_shuffle_on:
		                     R.drawable.ic_menu_shuffle_off);

		repeatItem.setIcon((kMP.musicService.isRepeat())?
		                    R.drawable.ic_menu_repeat_on:
		                    R.drawable.ic_menu_repeat_off);
	}

	/**
	 * Sets the Action Bar subtitle to the currently playing
	 * song title.
	 */
	public void refreshActionBarSubtitle() {
		ActionBar actionBar = getActionBar();

		if (actionBar != null)
			if (kMP.musicService.currentSong != null)
				actionBar.setSubtitle(kMP.musicService.currentSong.getTitle());
	}

	/**
	 * This method gets called whenever the user clicks an
	 * item on the ActionBar.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		// The leftmost icon, usually with the app logo
		case android.R.id.home:
			showSubmenu();
			return true;

		case R.id.action_bar_shuffle:
			kMP.musicService.toggleShuffleMode();
			refreshActionBarItems();
			return true;

		case R.id.action_bar_repeat:
			kMP.musicService.toggleRepeatMode();
			refreshActionBarItems();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStart() {

		super.onStart();

		// WHY CANT I SET THE MUSIC CONTROLLER HERE AND LET IT BE
		// FOREVER?
	//	if (!this.isFinishing()) {
	//	    musicController.show(5000);
	//	}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if(event.getAction() == KeyEvent.ACTION_DOWN)
			if (keyCode == KeyEvent.KEYCODE_MENU)
				musicController.show();

		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Another Activity is taking focus.
	 * (either from user going to another Activity or home)
	 */
	@Override
	protected void onPause() {
		super.onPause();

		paused = true;
		playbackPaused = true;
	}

	/**
	 * Activity has become visible.
	 *
	 * @see onPause()
	 */
	@Override
	protected void onResume() {
		super.onResume();

		refreshActionBarSubtitle();

		if (paused) {
			// Ensure that the controller
			// is shown when the user returns to the app
			setMusicController();
			paused = false;
		}
	}

	/**
	 * Activity is no longer visible.
	 */
	@Override
	protected void onStop() {
		musicController.hide();
		super.onStop();
	}

	/**
	 * (Re)Starts the musicController.
	 */
	private void setMusicController() {
		musicController = new MusicController(ActivityNowPlaying.this);

		// What will happen when the user presses the
		// next/previous buttons?
		musicController.setPrevNextListeners(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Calling method defined on ActivityNowPlaying
				playNext();
			}
		}, new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// Calling method defined on ActivityNowPlaying
				playPrevious();
			}
		});

		// Binding to our media player
		musicController.setMediaPlayer(this);
		musicController.setAnchorView(findViewById(R.id.activity_now_playing_song_list));
		musicController.setEnabled(true);
	}

	@Override
	public void start() {
		kMP.musicService.unpause();
	}

	/**
	 * Callback to when the user pressed the `pause` button.
	 */
	@Override
	public void pause() {
		kMP.musicService.pausePlayer();
	}

	@Override
	public int getDuration() {
		if (kMP.musicService != null && kMP.musicService.musicBound && kMP.musicService.isPlaying())
			return kMP.musicService.getDuration();
		else
			return 0;
	}

	@Override
	public int getCurrentPosition() {
		if (kMP.musicService != null && kMP.musicService.musicBound && kMP.musicService.isPlaying())
			return kMP.musicService.getPosition();
		else
			return 0;
	}

	@Override
	public void seekTo(int position) {
		kMP.musicService.seekTo(position);
	}

	@Override
	public boolean isPlaying() {
		if (kMP.musicService != null && kMP.musicService.musicBound)
			return kMP.musicService.isPlaying();

		return false;
	}

	@Override
	public int getBufferPercentage() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean canPause() {
		return true;
	}

	@Override
	public boolean canSeekBackward() {
		return true;
	}

	@Override
	public boolean canSeekForward() {
		return true;
	}

	@Override
	public int getAudioSessionId() {
		// TODO Auto-generated method stub
		return 0;
	}

	// Back to the normal methods

	/**
	 * Jumps to the next song and starts playing it right now.
	 */
	private void playNext() {
		kMP.musicService.next();
		kMP.musicService.playSong();

		refreshActionBarSubtitle();

		// To prevent the MusicPlayer from behaving
		// unexpectedly when we pause the song playback.
		if (playbackPaused) {
			setMusicController();
			playbackPaused = false;
		}

		musicController.show();
	}

	/**
	 * Jumps to the previous song and starts playing it right now.
	 */
	private void playPrevious() {
		kMP.musicService.previous();
		kMP.musicService.playSong();

		refreshActionBarSubtitle();

		// To prevent the MusicPlayer from behaving
		// unexpectedly when we pause the song playback.
		if (playbackPaused) {
			setMusicController();
			playbackPaused = false;
		}

		musicController.show();
	}

	/**
	 * When the user selects a music inside the "Now Playing List",
	 * we'll start playing it right away.
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		// Prepare the music service to play the song.
		kMP.musicService.setSong(position);

		// Scroll the list view to the current song.
		songListView.setSelection(position);

		kMP.musicService.playSong();
		refreshActionBarSubtitle();

		if (playbackPaused) {
			setMusicController();
			playbackPaused = false;
		}
	}

	/**
	 * When the user long clicks a music inside the "Now Playing List".
	 */
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

		Toast.makeText(this, kMP.musicService.getSong(position).getGenre(), Toast.LENGTH_LONG).show();


		// Just a catch - if we return `false`, when an user
		// long clicks an item, the list will react as if
		// we've long clicked AND clicked.
		//
		// So by returning `false`, it will call both
		// `onItemLongClick` and `onItemClick`!
		return true;
	}
}

