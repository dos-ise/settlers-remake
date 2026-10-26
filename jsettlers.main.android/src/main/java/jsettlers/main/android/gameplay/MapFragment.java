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

import go.graphics.android.GOSurfaceView;
import go.graphics.area.Area;
import go.graphics.region.Region;
import jsettlers.common.selectable.ISelectionSet;
import jsettlers.graphics.map.MapContent;
import jsettlers.graphics.map.draw.ImageProvider;
import jsettlers.main.android.R;
import jsettlers.main.android.core.controls.ControlsResolver;
import jsettlers.main.android.core.controls.GameMenu;
import jsettlers.main.android.core.controls.SelectionControls;
import jsettlers.main.android.core.controls.SelectionListener;
import jsettlers.main.android.core.navigation.BackPressedListener;
import jsettlers.main.android.core.ui.FragmentUtil;
import jsettlers.main.android.gameplay.gamemenu.GameMenuDialog;

/**
 * Shows the game map. The in-game UI (main panel, minimap, chat) is rendered by the shared {@code OriginalControls}
 * on the GL surface, so no Android specific control fragments are needed anymore.
 */
@EFragment(R.layout.fragment_map)
@OptionsMenu(R.menu.game)
public class MapFragment extends Fragment implements SelectionListener, BackPressedListener {
	private static final String TAG_GAME_MENU_DIALOG = "com.jsettlers.gamemenufragment";

	private SelectionControls selectionControls;
	private GameMenu gameMenu;

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
	void setupControls() {
		ControlsResolver controlsResolver = new ControlsResolver(getActivity());
		selectionControls = controlsResolver.getSelectionControls();
		gameMenu = controlsResolver.getGameMenu();
		addMapViews(controlsResolver.getMapContent());
	}

	@AfterViews
	void registerObservers() {
		gameMenu.isPausedState().observe(this, pauseObserver);
		gameMenu.getGameState().observeForever(gameStateObserver);
	}

	private void addMapViews(MapContent mapContent) {
		Region goRegion = new Region(Region.POSITION_CENTER);
		goRegion.setContent(mapContent);

		Area goArea = new Area();
		goArea.set(goRegion);

		GOSurfaceView goView = new GOSurfaceView(getActivity(), goArea, () -> new HashSet<>());
		goView.setContextDestroyedListener(() -> ImageProvider.getInstance().invalidateAll());
		frameLayout.addView(goView);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (gameMenu.isPausedState().getValue() == Boolean.FALSE) {
			gameMenu.unMute();
		}
		gameMenu.onGameVisible();
	}

	@Override
	public void onPause() {
		super.onPause();
		gameMenu.mute();
		gameMenu.onGameHidden();
	}

	@Override
	public void onStart() {
		super.onStart();
		selectionControls.addSelectionListener(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		selectionControls.removeSelectionListener(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		gameMenu.getGameState().removeObserver(gameStateObserver);
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
			getActivity().finish();
		}
	};
}