/*
 * Copyright (c) 2026
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

package jsettlers.main.android.gameplay.statistics;

import java.util.HashSet;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.FrameLayout;

import go.graphics.android.GOSurfaceView;
import go.graphics.area.Area;
import go.graphics.region.Region;
import jsettlers.common.menu.IStartedGame;
import jsettlers.graphics.menu.EndgameStatsPanel;
import jsettlers.graphics.menu.MenuContent;
import jsettlers.graphics.menu.MenuImageProvider;
import jsettlers.graphics.map.draw.ImageProvider;
import jsettlers.main.android.R;
import jsettlers.main.android.core.GameManager;
import jsettlers.main.android.mainmenu.AndroidMenuImageDecoder;
import jsettlers.main.android.mainmenu.MainActivity_;

/**
 * Shown after a game has been quit. The result of the game is rendered by the shared GL endgame statistics panel,
 * the same panel that is used on desktop. Continues to the main menu.
 */
@EActivity(R.layout.activity_endgame_statistics)
public class EndgameStatisticsActivity extends AppCompatActivity {
	@ViewById(R.id.frame_layout)
	FrameLayout frameLayout;

	private GameManager gameManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		gameManager = (GameManager) getApplication();
	}

	@AfterViews
	void setupViews() {
		IStartedGame game = gameManager.getEndgameGame();
		if (game == null) {
			// The game is already gone (e.g. the activity was recreated after the process was killed).
			showMainMenu();
			return;
		}

		// The menu images are cached with GL handles of the previous activity's context. A fresh provider
		// re-decodes them so they are uploaded into this activity's GL context. The game images (e.g. the
		// button frames) are invalidated the same way.
		MenuImageProvider.setInstance(new MenuImageProvider(new AndroidMenuImageDecoder()));
		ImageProvider.getInstance().invalidateAll();

		EndgameStatsPanel statsPanel = new EndgameStatsPanel(this::showMainMenu);
		statsPanel.setGame(game);
		MenuContent menuContent = new MenuContent(statsPanel, action -> {
		});

		Region goRegion = new Region(Region.POSITION_CENTER);
		goRegion.setContent(menuContent);
		Area goArea = new Area();
		goArea.set(goRegion);
		GOSurfaceView goView = new GOSurfaceView(this, goArea, () -> new HashSet<>());
		frameLayout.addView(goView);
	}

	@Override
	public void onBackPressed() {
		showMainMenu();
	}

	private void showMainMenu() {
		// Clear the whole task so the finished GameActivity below is removed as well.
		MainActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK).start();
		finish();
	}
}
