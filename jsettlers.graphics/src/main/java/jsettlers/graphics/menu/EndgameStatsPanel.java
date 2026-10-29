/*******************************************************************************
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
 *******************************************************************************/
package jsettlers.graphics.menu;

import go.graphics.text.EFontSize;
import jsettlers.common.menu.IStartedGame;
import jsettlers.common.player.IEndgameStatistic;
import jsettlers.common.player.IInGamePlayer;
import jsettlers.graphics.action.ExecutableAction;
import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.ui.Label;
import jsettlers.graphics.ui.LabeledButton;
import jsettlers.graphics.ui.UIPanel;

/**
 * The shared endgame statistics panel. Shows the final statistics of all players (name, team, manna, gold, soldiers)
 * and lets the player leave. The same panel runs on desktop and Android.
 *
 * @author Copilot
 */
public class EndgameStatsPanel extends UIPanel {
	private static final float TABLE_TOP = 0.88f;
	private static final float TABLE_BOTTOM = 0.15f;
	private static final float[] COLUMNS = { 0.1f, 0.3f, 0.45f, 0.6f, 0.75f, 0.9f };

	private final Runnable onExit;

	/**
	 * Creates a new endgame statistics panel.
	 *
	 * @param onExit
	 *            Called when the player leaves the panel.
	 */
	public EndgameStatsPanel(Runnable onExit) {
		this.onExit = onExit;

		MenuImageProvider images = MenuImageProvider.getInstance();
		if (images != null) {
			setBackgroundImage(images.getBackground());
		}

		addChild(new Label(Labels.getString("stats-panel-title"), EFontSize.HEADLINE), 0.1f, 0.92f, 0.9f, 1f);

		addHeaderRow();
	}

	/**
	 * Sets the game whose statistics are shown. Must be called before the panel is displayed.
	 *
	 * @param game
	 *            The finished game.
	 */
	public void setGame(IStartedGame game) {
		IInGamePlayer[] players = game.getAllInGamePlayers();
		float rowHeight = Math.min((TABLE_TOP - TABLE_BOTTOM) / (players.length + 1), 0.08f);
		for (int i = 0; i < players.length; i++) {
			float top = TABLE_TOP - (i + 1) * rowHeight;
			addPlayerRow(players[i], top - rowHeight, top);
		}

		LabeledButton exitButton = new LabeledButton(Labels.getString("stats-panel-exit"), new ExecutableAction() {
			@Override
			public void execute() {
				onExit.run();
			}
		});
		addChild(exitButton, 0.1f, 0.02f, 0.9f, 0.10f);
	}

	private void addHeaderRow() {
		String[] headers = {
				Labels.getString("stats-panel-name"),
				Labels.getString("stats-panel-team"),
				Labels.getString("stats-panel-manna"),
				Labels.getString("stats-panel-gold"),
				Labels.getString("stats-panel-soldiers")
		};
		for (int i = 0; i < headers.length; i++) {
			addChild(new Label(headers[i], EFontSize.NORMAL, Label.EHorizontalAlignment.LEFT), COLUMNS[i], TABLE_TOP - 0.05f, COLUMNS[i + 1], TABLE_TOP);
		}
	}

	private void addPlayerRow(IInGamePlayer player, float bottom, float top) {
		IEndgameStatistic statistic = player.getEndgameStatistic();
		String[] values = {
				statistic.getName(),
				String.valueOf(statistic.getTeam()),
				String.valueOf(statistic.getAmountOfProducedMana()),
				String.valueOf(statistic.getAmountOfProducedGold()),
				String.valueOf(statistic.getAmountOfProducedSoldiers())
		};
		for (int i = 0; i < values.length; i++) {
			addChild(new Label(values[i], EFontSize.NORMAL, Label.EHorizontalAlignment.LEFT), COLUMNS[i], bottom, COLUMNS[i + 1], top);
		}
	}
}
