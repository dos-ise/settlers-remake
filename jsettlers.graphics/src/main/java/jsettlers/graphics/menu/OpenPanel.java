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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import go.graphics.text.EFontSize;
import jsettlers.common.Color;
import jsettlers.graphics.action.ExecutableAction;
import jsettlers.graphics.image.Image;
import jsettlers.graphics.image.SingleImage;
import jsettlers.graphics.image.reader.ImageMetadata;
import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.ui.AspectRatioImagePanel;
import jsettlers.graphics.ui.Label;
import jsettlers.graphics.ui.LabeledButton;
import jsettlers.graphics.ui.UIInput;
import jsettlers.graphics.ui.UIList;
import jsettlers.graphics.ui.UIPanel;

/**
 * The shared map selection menu. Shows a searchable list of maps and lets the player open one.
 *
 * @author Copilot
 */
public class OpenPanel extends UIPanel {
	private final MapListProvider provider;
	private final List<MapMenuItem> allMaps;
	private final UIList<MapMenuItem> mapList;
	private final UIInput searchInput;
	private final Consumer<MapMenuItem> onOpen;
	private final Runnable onBack;
	private final Consumer<MapMenuItem> onDelete;

	/**
	 * Creates a new open panel.
	 *
	 * @param title
	 *            The title of the panel.
	 * @param provider
	 *            Provides the maps to show.
	 * @param openButtonText
	 *            The text of the open button.
	 * @param onOpen
	 *            Called with the selected map when the player opens it.
	 * @param onBack
	 *            Called when the player goes back to the main menu.
	 * @param onDelete
	 *            Called with the selected savegame when the player deletes it, or <code>null</code> if deletion is not
	 *            supported (e.g. for the fresh map list).
	 */
	public OpenPanel(String title, MapListProvider provider, String openButtonText, Consumer<MapMenuItem> onOpen, Runnable onBack,
			Consumer<MapMenuItem> onDelete) {
		this.provider = provider;
		this.allMaps = new ArrayList<>(provider.getMaps());
		this.onOpen = onOpen;
		this.onBack = onBack;
		this.onDelete = onDelete;

		Label titleLabel = new Label(title, EFontSize.HEADLINE);
		addChild(titleLabel, 0.1f, 0.9f, 0.9f, 1f);

		UIPanel searchBar = new UIPanel();
		searchBar.setBackgroundImage(createSolidImage(Color.WHITE, "map-search-border"));
		UIPanel searchField = new UIPanel();
		searchField.setBackgroundImage(createSolidImage(new Color(0.02f, 0.02f, 0.03f, 0.95f), "map-search-background"));
		searchInput = new UIInput();
		searchInput.setPlaceholder(Labels.getString("general.search"));
		searchInput.setTextChangedListener(text -> updateList());
		searchField.addChild(searchInput, 0.065f, 0, 1, 1);
		Image searchIcon = createSearchIcon();
		searchField.addChild(new AspectRatioImagePanel(searchIcon), 0.015f, 0.15f, 0.06f, 0.85f);
		searchBar.addChild(searchField, 0.004f, 0.1f, 0.996f, 0.9f);
		addChild(searchBar, 0.1f, 0.8f, 0.9f, 0.88f);

		mapList = new UIList<>(allMaps, MapListItem::new, 0.12f, MapListItem.LIST_BACKGROUND,
				MapListItem.EVEN_ROW_BACKGROUND, MapListItem.ODD_ROW_BACKGROUND, MapListItem.SELECTED_ROW_BACKGROUND);
		addChild(mapList, 0.1f, 0.15f, 0.9f, 0.78f);

		LabeledButton openButton = new LabeledButton(openButtonText, new ExecutableAction() {
			@Override
			public void execute() {
				MapMenuItem selected = mapList.getActiveItem();
				if (selected != null) {
					onOpen.accept(selected);
				}
			}
		});
		addChild(openButton, 0.1f, 0.05f, onDelete != null ? 0.4f : 0.5f, 0.13f);

		if (onDelete != null) {
			LabeledButton deleteButton = new LabeledButton(Labels.getString("start-loadgame-delete"), new ExecutableAction() {
				@Override
				public void execute() {
					MapMenuItem selected = mapList.getActiveItem();
					if (selected != null) {
						onDelete.accept(selected);
						reload();
					}
				}
			});
			addChild(deleteButton, 0.4f, 0.05f, 0.6f, 0.13f);
		}

		LabeledButton backButton = new LabeledButton(Labels.getString("settings-back"), new ExecutableAction() {
			@Override
			public void execute() {
				onBack.run();
			}
		});
		addChild(backButton, onDelete != null ? 0.6f : 0.5f, 0.05f, 0.9f, 0.13f);
	}

	/**
	 * Reloads the list from the provider and re-applies the search filter.
	 */
	private void reload() {
		allMaps.clear();
		allMaps.addAll(provider.getMaps());
		updateList();
	}

	private void updateList() {
		String filter = searchInput.getText().toLowerCase(Locale.ENGLISH);
		List<MapMenuItem> filtered = allMaps.stream()
				.filter(map -> filter.isEmpty() || containsIgnoreCase(map.getName(), filter)
						|| containsIgnoreCase(map.getId(), filter) || containsIgnoreCase(map.getDescription(), filter))
				.collect(Collectors.toList());
		mapList.setItems(filtered);
	}

	@Override
	public boolean handleKey(String keyCode) {
		if ("ENTER".equals(keyCode) || "RETURN".equals(keyCode)) {
			MapMenuItem selected = mapList.getActiveItem();
			if (selected != null) {
				onOpen.accept(selected);
				return true;
			}
		}
		return super.handleKey(keyCode);
	}

	private static boolean containsIgnoreCase(String value, String filter) {
		return value != null && value.toLowerCase(Locale.ENGLISH).contains(filter);
	}

	private static Image createSolidImage(Color color, String name) {
		ImageMetadata metadata = new ImageMetadata();
		metadata.width = 1;
		metadata.height = 1;
		metadata.offsetX = 0;
		metadata.offsetY = 0;
		return new SingleImage(metadata, new int[] { color.getRGBA() }, name);
	}

	private static Image createSearchIcon() {
		int size = 20;
		int[] pixels = new int[size * size];
		int white = Color.WHITE.getRGBA();
		for (int y = 2; y <= 13; y++) {
			for (int x = 2; x <= 13; x++) {
				int distance = (x - 7) * (x - 7) + (y - 7) * (y - 7);
				if (distance >= 16 && distance <= 25) {
					pixels[x + y * size] = white;
				}
			}
		}
		for (int offset = 0; offset < 7; offset++) {
			int coordinate = 11 + offset;
			pixels[coordinate + coordinate * size] = white;
			if (coordinate + 1 < size) {
				pixels[coordinate + 1 + coordinate * size] = white;
				pixels[coordinate + (coordinate + 1) * size] = white;
			}
		}

		ImageMetadata metadata = new ImageMetadata();
		metadata.width = size;
		metadata.height = size;
		metadata.offsetX = 0;
		metadata.offsetY = 0;
		return new SingleImage(metadata, pixels, "map-search-icon");
	}
}
