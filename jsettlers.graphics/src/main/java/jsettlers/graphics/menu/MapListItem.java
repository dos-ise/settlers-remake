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

import go.graphics.GLDrawContext;
import go.graphics.text.EFontSize;
import go.graphics.text.TextDrawer;
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
	private static final int MAX_DESCRIPTION_CHARACTERS = 240;

	static final Color LIST_BACKGROUND = new Color(0.03f, 0.03f, 0.05f, 0.92f);
	static final Color EVEN_ROW_BACKGROUND = new Color(0.08f, 0.07f, 0.1f, 0.78f);
	static final Color ODD_ROW_BACKGROUND = new Color(0.28f, 0.16f, 0.14f, 0.78f);
	static final Color SELECTED_ROW_BACKGROUND = new Color(0.48f, 0.36f, 0.12f, 0.9f);

	private static final Color HIGHLIGHT_COLOR = new Color(1, 0.9f, 0.45f, 1);
	private static final Color PLAYER_COUNT_COLOR = new Color(0.35f, 0.55f, 1, 1);

	private final Label nameLabel;
	private final Label playerCountLabel;

	/**
	 * Creates a new list item for the given map.
	 *
	 * @param map
	 *            The map to show.
	 */
	public MapListItem(MapMenuItem map) {
		nameLabel = new Label(map.getName(), EFontSize.NORMAL, Label.EHorizontalAlignment.LEFT);
		float textLeft = 0.025f;
		if (map.getPreview() != null) {
			addChild(new AspectRatioImagePanel(map.getPreview()), 0.015f, 0.08f, 0.15f, 0.92f);
			textLeft = 0.17f;
		}

		Label infoLabel = new Label(buildInfo(map), EFontSize.SMALL, Label.EHorizontalAlignment.LEFT);
		String description = map.getDescription();
		Label descriptionLabel = new TruncatedDescriptionLabel(description == null ? "" : description);

		addChild(nameLabel, textLeft, 0.69f, 0.76f, 0.97f);
		addChild(infoLabel, textLeft, 0.37f, 0.97f, 0.67f);
		addChild(descriptionLabel, textLeft, 0.06f, 0.97f, 0.35f);

		if (map.getMaxPlayers() > 0) {
			playerCountLabel = new Label(Labels.getString("map-player-count", map.getMinPlayers(), map.getMaxPlayers()),
					EFontSize.SMALL, Label.EHorizontalAlignment.RIGHT);
			playerCountLabel.setTextColor(PLAYER_COUNT_COLOR);
			addChild(playerCountLabel, 0.77f, 0.69f, 0.97f, 0.97f);
		} else {
			playerCountLabel = null;
		}
	}

	private static String buildInfo(MapMenuItem map) {
		StringBuilder info = new StringBuilder();
		// For savegames the description is the title the player entered when saving.
		if (map.isSavegame() && map.getDescription() != null && !map.getDescription().isEmpty()) {
			info.append(map.getDescription());
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
		if (highlight && playerCountLabel != null) {
			playerCountLabel.setTextColor(HIGHLIGHT_COLOR);
		} else if (playerCountLabel != null) {
			playerCountLabel.setTextColor(PLAYER_COUNT_COLOR);
		}
	}

	private static class TruncatedDescriptionLabel extends Label {
		private final String fullText;
		private String displayedText;

		TruncatedDescriptionLabel(String text) {
			super(text, EFontSize.SMALL, Label.EHorizontalAlignment.LEFT);
			fullText = text;
		}

		@Override
		public void drawAt(GLDrawContext gl) {
			TextDrawer drawer = gl.getTextDrawer(EFontSize.SMALL);
			int characterCount = Math.min(fullText.codePointCount(0, fullText.length()), MAX_DESCRIPTION_CHARACTERS);
			String text = fullText;
			if (characterCount < fullText.codePointCount(0, fullText.length())
					|| drawer.getWidth(text) > getPosition().getWidth() * 0.96f) {
				int low = 0;
				int high = characterCount;
				String ellipsis = "...";
				float maxWidth = getPosition().getWidth() * 0.96f;
				while (low < high) {
					int middle = (low + high + 1) / 2;
					String candidate = fullText.substring(0, fullText.offsetByCodePoints(0, middle)) + ellipsis;
					if (drawer.getWidth(candidate) <= maxWidth) {
						low = middle;
					} else {
						high = middle - 1;
					}
				}
				text = fullText.substring(0, fullText.offsetByCodePoints(0, low)) + ellipsis;
			}
			if (!text.equals(displayedText)) {
				setText(text);
				displayedText = text;
			}
			super.drawAt(gl);
		}
	}
}
