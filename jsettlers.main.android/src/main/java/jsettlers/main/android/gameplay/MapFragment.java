/*
 * Copyright (c) 2017
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package jsettlers.main.android.gameplay;

import java.util.HashSet;
import java.util.Set;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;

import android.arch.lifecycle.Observer;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;

import go.graphics.android.GOSurfaceView;
import go.graphics.area.Area;
import go.graphics.region.Region;
import go.graphics.region.RegionContent;
import go.graphics.text.EFontSize;
import jsettlers.common.menu.EGameError;
import jsettlers.common.menu.EProgressState;
import jsettlers.common.menu.IMapInterfaceConnector;
import jsettlers.common.menu.IStartedGame;
import jsettlers.common.menu.IStartingGame;
import jsettlers.common.menu.IStartingGameListener;
import jsettlers.common.selectable.ISelectionSet;
import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.map.MapContent;
import jsettlers.graphics.map.draw.ImageProvider;
import jsettlers.graphics.menu.MenuContent;
import jsettlers.graphics.menu.MenuImageProvider;
import jsettlers.graphics.ui.Label;
import jsettlers.graphics.ui.UIPanel;
import jsettlers.main.android.R;
import jsettlers.main.android.core.GameStarter;
import jsettlers.main.android.core.controls.ControlsResolver;
import jsettlers.main.android.core.controls.AndroidTextInputProvider;
import jsettlers.main.android.core.controls.GameMenu;
import jsettlers.main.android.core.controls.SelectionControls;
import jsettlers.main.android.core.controls.SelectionListener;
import jsettlers.main.android.core.navigation.BackPressedListener;
import jsettlers.main.android.core.ui.FragmentUtil;
import jsettlers.main.android.gameplay.gamemenu.GameMenuDialog;
import jsettlers.main.android.gameplay.statistics.EndgameStatisticsActivity_;

/**
 * Shows the game map. The in-game UI (main panel, minimap, chat) is rendered by the shared {@code OriginalControls}
 * on the GL surface, so no Android specific control fragments are needed anymore. While the game is starting, a GL
 * loading panel with the game background is shown instead of a native loading screen.
 */
@EFragment(R.layout.fragment_map)
@OptionsMenu(R.menu.game)
public class MapFragment extends Fragment implements SelectionListener, BackPressedListener, IStartingGameListener {
	private static final String TAG_GAME_MENU_DIALOG = "com.jsettlers.gamemenufragment";

	private GameStarter gameStarter;
	private SelectionControls selectionControls;
	private GameMenu gameMenu;
	private Region goRegion;
	private Label loadingLabel;

	@ViewById(R.id.toolbar)
	Toolbar toolbar;
	@ViewById(R.id.frame_layout)
	FrameLayout frameLayout;

	@AfterViews
	void setupToolbar() {
		FragmentUtil.setActionBar(this, toolbar);
		FragmentUtil.setDisplayShowTitleEnabled(this, false);
	}

	@AfterViews
	void setup() {
		gameStarter = (GameStarter) getActivity().getApplication();
		addMapView(new MenuContent(createLoadingPanel(), action -> {
		}));

		IStartingGame startingGame = gameStarter.getStartingGame();
		if (startingGame.isStartupFinished()) {
			setupGame();
		} else {
			startingGame.setListener(this);
		}
	}

	private UIPanel createLoadingPanel() {
		UIPanel panel = new UIPanel();
		MenuImageProvider images = MenuImageProvider.getInstance();
		if (images != null) {
			panel.setBackgroundImage(images.getBackground());
		}
		loadingLabel = new Label(Labels.getProgress(EProgressState.LOADING), EFontSize.HEADLINE);
		panel.addChild(loadingLabel, 0.1f, 0.4f, 0.9f, 0.6f);
		return panel;
	}

	private void addMapView(RegionContent content) {
		goRegion = new Region(Region.POSITION_CENTER);
		goRegion.setContent(content);

		Area goArea = new Area();
		goArea.set(goRegion);

		GOSurfaceView goView = new GOSurfaceView(getActivity(), goArea, () -> new HashSet<>());
		goView.setContextDestroyedListener(() -> ImageProvider.getInstance().invalidateAll());
		frameLayout.addView(goView);
	}

	private void setupGame() {
		ControlsResolver controlsResolver = new ControlsResolver(getActivity());
		selectionControls = controlsResolver.getSelectionControls();
		gameMenu = controlsResolver.getGameMenu();
		MapContent mapContent = controlsResolver.getMapContent();
		// The text input provider needs an Activity context so its dialogs use the AppCompat theme
		// (the application context set up in ControlsAdapter has no theme and crashes on show()).
		mapContent.setTextInputProvider(new AndroidTextInputProvider(getActivity()));
		goRegion.setContent(mapContent);

		gameMenu.isPausedState().observe(this, pauseObserver);
		gameMenu.getGameState().observeForever(gameStateObserver);
	}

	/**
	 * IStartingGameListener implementation. The game is shown directly on the GL surface, so the native loading
	 * fragment is not needed anymore.
	 */
	@Override
	public void startProgressChanged(EProgressState state, float progress) {
		getActivity().runOnUiThread(() -> {
			if (loadingLabel != null) {
				loadingLabel.setText(Labels.getProgress(state));
			}
		});
	}

	@Override
	public IMapInterfaceConnector preLoadFinished(IStartedGame game) {
		return gameStarter.gameStarted(game);
	}

	@Override
	public void startFailed(EGameError errorType, Exception exception) {
		gameStarter.getStartingGame().setListener(null);
		Toast.makeText(getActivity(), errorType.toString(), Toast.LENGTH_LONG).show();
		getActivity().finish();
	}

	@Override
	public void startFinished() {
		gameStarter.getStartingGame().setListener(null);
		getActivity().runOnUiThread(this::setupGame);
	}

	@Override
	public void startingLoadingGame() {
		ImageProvider.getInstance().startPreloading();
	}

	@Override
	public void waitForPreloading() {
		ImageProvider.getInstance().waitForPreloadingFinish();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (gameMenu != null && gameMenu.isPausedState().getValue() == Boolean.FALSE) {
			gameMenu.unMute();
		}
		if (gameMenu != null) {
			gameMenu.onGameVisible();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (gameMenu != null) {
			gameMenu.mute();
			gameMenu.onGameHidden();
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		if (selectionControls != null) {
			selectionControls.addSelectionListener(this);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if (selectionControls != null) {
			selectionControls.removeSelectionListener(this);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (gameMenu != null) {
			gameMenu.getGameState().removeObserver(gameStateObserver);
		}
	}

	/**
	 * BackPressedListener implementation
	 */
	@Override
	public boolean onBackPressed() {
		if (selectionControls.getCurrentSelection() != null) {
			selectionControls.deselect();
			return true;
		}

		return true;
	}

	/**
	 * SelectionListener implementation. The selection display itself is handled by the shared OriginalControls.
	 */
	@Override
	public void selectionChanged(ISelectionSet selection) {
	}

	@OptionsItem(R.id.menu_item_show_game_menu)
	void showGameMenu() {
		if (getChildFragmentManager().findFragmentByTag(TAG_GAME_MENU_DIALOG) == null) {
			GameMenuDialog.create().show(getChildFragmentManager(), TAG_GAME_MENU_DIALOG);
		}
	}

	private Observer<Boolean> pauseObserver = paused -> {
		if (paused) {
			showGameMenu();
			frameLayout.setForeground(new ColorDrawable(ContextCompat.getColor(getActivity(), R.color.paused_mask)));
		} else {
			gameMenu.unMute();
			frameLayout.setForeground(null);
		}
	};

	private Observer<GameMenu.GameState> gameStateObserver = gameState -> {
		if (gameState == GameMenu.GameState.QUITTED) {
			// The game is shut down. Navigate to the endgame statistics, which continues to the main
			// menu. This is done here (and not only in the GameMenuDialog) so that it also works when
			// the game is quit from the notification or the resume screen. The GameActivity is the
			// root of the task, so it must not be finished here; the EndgameStatisticsActivity clears
			// the whole task when it continues to the main menu.
			if (isAdded()) {
				EndgameStatisticsActivity_.intent(getActivity()).start();
			}
		}
	};
}