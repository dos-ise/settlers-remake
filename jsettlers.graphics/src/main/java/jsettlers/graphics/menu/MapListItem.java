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

import java.text.SimpleDateFormat;
import java.util.Date;

import go.graphics.text.EFontSize;
import jsettlers.common.Color;
import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.ui.AspectRatioImagePanel;
import jsettlers.graphics.ui.Label;
import jsettlers.graphics.ui.UIPanel;
import jsettlers.graphics.ui.UIListItem;

/**
 * A list item that shows the preview image, the name and a short info line of a map in the shared map selection menu.
 *
 * @author Copilot
 */
public class MapListItem extends UIPanel implements UIListItem {
	private static final Color HIGHLIGHT_COLOR = new Color(1, 1, 0, 1);

	private final Label nameLabel;

	/**
	 * Creates a new list item for the given map.
	 *
	 * @param map
	 *            The map to show.
	 */
	public MapListItem(MapMenuItem map) {
		nameLabel = new Label(map.getName(), EFontSize.NORMAL, Label.EHorizontalAlignment.LEFT);
		float textLeft = 0.02f;
		if (map.getPreview() != null) {
			addChild(new AspectRatioImagePanel(map.getPreview()), 0.02f, 0.1f, 0.2f, 0.9f);
			textLeft = 0.22f;
		}

		Label infoLabel = new Label(buildInfo(map), EFontSize.SMALL, Label.EHorizontalAlignment.LEFT);

		addChild(nameLabel, textLeft, 0.55f, 0.98f, 0.95f);
		addChild(infoLabel, textLeft, 0.1f, 0.98f, 0.5f);
	}

	private static String buildInfo(MapMenuItem map) {
		StringBuilder info = new StringBuilder();
		// For savegames the description is the title the player entered when saving.
		if (map.isSavegame() && map.getDescription() != null && !map.getDescription().isEmpty()) {
			info.append(map.getDescription());
		}
		if (map.getMaxPlayers() > 0) {
			if (info.length() > 0) {
				info.append("  ");
			}
			info.append(Labels.getString("map-player-count", map.getMinPlayers(), map.getMaxPlayers()));
		}
		Date creationDate = map.getCreationDate();
		if (creationDate != null) {
			if (info.length() > 0) {
				info.append("  ");
			}
			info.append(new SimpleDateFormat(Labels.getString("date.date-only")).format(creationDate));
		}
		if (map.getId() != null && !map.getId().isEmpty()) {
			if (info.length() > 0) {
				info.append("  ");
			}
			info.append(map.getId());
		}
		return info.toString();
	}

	@Override
	public void setHighlighted(boolean highlight) {
		nameLabel.setTextColor(highlight ? HIGHLIGHT_COLOR : null);
	}
}
