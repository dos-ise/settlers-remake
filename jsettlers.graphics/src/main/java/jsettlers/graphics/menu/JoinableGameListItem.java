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
import jsettlers.common.Color;
import jsettlers.common.menu.IJoinableGame;
import jsettlers.common.menu.IMapDefinition;
import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.ui.Label;
import jsettlers.graphics.ui.UIPanel;
import jsettlers.graphics.ui.UIListItem;

/**
 * A list item that shows the host name, the map name and the player count of a joinable multiplayer game.
 *
 * @author Copilot
 */
public class JoinableGameListItem extends UIPanel implements UIListItem {
	private static final Color HIGHLIGHT_COLOR = new Color(1, 1, 0, 1);

	private final Label nameLabel;

	/**
	 * Creates a new list item for the given joinable game.
	 *
	 * @param game
	 *            The game to show.
	 */
	public JoinableGameListItem(IJoinableGame game) {
		nameLabel = new Label(game.getName(), EFontSize.NORMAL, Label.EHorizontalAlignment.LEFT);
		addChild(nameLabel, 0.02f, 0.55f, 0.98f, 0.95f);

		IMapDefinition map = game.getMap();
		String info = "";
		if (map != null) {
			info = map.getMapName();
			if (map.getMaxPlayers() > 0) {
				info += "  " + Labels.getString("map-player-count", map.getMinPlayers(), map.getMaxPlayers());
			}
		}
		addChild(new Label(info, EFontSize.SMALL, Label.EHorizontalAlignment.LEFT), 0.02f, 0.1f, 0.98f, 0.5f);
	}

	@Override
	public void setHighlighted(boolean highlight) {
		nameLabel.setTextColor(highlight ? HIGHLIGHT_COLOR : null);
	}
}
