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
import jsettlers.graphics.action.ExecutableAction;
import jsettlers.graphics.localization.Labels;
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

		searchInput = new UIInput();
		searchInput.setTextChangedListener(text -> updateList());
		addChild(searchInput, 0.1f, 0.8f, 0.9f, 0.88f);

		mapList = new UIList<>(allMaps, MapListItem::new, 0.15f);
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
				.filter(map -> filter.isEmpty() || map.getName().toLowerCase(Locale.ENGLISH).contains(filter))
				.collect(Collectors.toList());
		mapList.setItems(filtered);
	}
}
