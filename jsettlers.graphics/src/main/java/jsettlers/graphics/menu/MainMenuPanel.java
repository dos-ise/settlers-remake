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

import java.util.function.Consumer;

import go.graphics.text.EFontSize;
import jsettlers.graphics.action.ExecutableAction;
import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.ui.ImagePanel;
import jsettlers.graphics.ui.LabeledButton;
import jsettlers.graphics.ui.UIPanel;

/**
 * The shared main menu. Shows the main buttons on the left and the currently active sub menu (e.g. the map selection)
 * on the right.
 *
 * @author Copilot
 */
public class MainMenuPanel extends UIPanel {
	private final UIPanel contentPanel = new UIPanel();
	private final MainMenuActions actions;

	/**
	 * Creates a new main menu.
	 *
	 * @param actions
	 *            The actions the menu triggers on the host application.
	 * @param freshMaps
	 *            Provides the maps for a new single player game.
	 * @param savedMaps
	 *            Provides the savegames to load.
	 */
	public MainMenuPanel(MainMenuActions actions, MapListProvider freshMaps, MapListProvider savedMaps) {
		this.actions = actions;
		MenuImageProvider images = MenuImageProvider.getInstance();
		if (images != null) {
			setBackgroundImage(images.getBackground());
		}

		UIPanel buttonPanel = new UIPanel();
		addChild(buttonPanel, 0.03f, 0.1f, 0.28f, 0.9f);

		addMenuButton(buttonPanel, "main-panel-new-single-player-game-button", 0f, 0.8f, 1f, 0.95f,
				() -> showOpenPanel(Labels.getString("start-newgame"), freshMaps, Labels.getString("start-newgame-start"), actions::startNewSinglePlayerGame));
		addMenuButton(buttonPanel, "start-loadgame", 0f, 0.65f, 1f, 0.8f,
				() -> showOpenPanel(Labels.getString("start-loadgame"), savedMaps, Labels.getString("start-loadgame-start"), actions::startLoadGame));
		addMenuButton(buttonPanel, "start-newmultiplayer", 0f, 0.5f, 1f, 0.65f, actions::showNewMultiplayerGame);
		addMenuButton(buttonPanel, "start-joinmultiplayer", 0f, 0.35f, 1f, 0.5f, actions::showJoinMultiplayerGame);
		addMenuButton(buttonPanel, "settings-title", 0f, 0.2f, 1f, 0.35f, actions::showSettings);
		addMenuButton(buttonPanel, "main-panel-exit-button", 0f, 0.05f, 1f, 0.2f, actions::exit);

		addChild(contentPanel, 0.32f, 0.1f, 0.97f, 0.9f);

		if (images != null) {
			// decorative leaves, drawn on top like in the desktop menu
			addChild(new ImagePanel(images.getLeavesRight()), 0.8f, 0.79f, 1f, 1f);
			addChild(new ImagePanel(images.getLeavesLeft()), 0f, 0.843f, 0.115f, 1f);
		}
	}

	private void addMenuButton(UIPanel panel, String labelKey, float left, float bottom, float right, float top, Runnable action) {
		LabeledButton button = new LabeledButton(Labels.getString(labelKey), new ExecutableAction() {
			@Override
			public void execute() {
				action.run();
			}
		}, EFontSize.HEADLINE);
		panel.addChild(button, left, bottom, right, top);
	}

	private void showOpenPanel(String title, MapListProvider provider, String openButtonText, Consumer<MapMenuItem> onSelect) {
		setContent(new OpenPanel(title, provider, openButtonText, onSelect, this::showMainContent));
	}

	/**
	 * Sets the content of the right hand side of the menu.
	 *
	 * @param panel
	 *            The panel to show.
	 */
	public void setContent(UIPanel panel) {
		contentPanel.removeAll();
		contentPanel.addChild(panel, 0, 0, 1, 1);
	}

	/**
	 * Shows the main menu content (clears the right hand side).
	 */
	public void showMainContent() {
		contentPanel.removeAll();
	}
}
