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
import jsettlers.common.menu.IJoinableGame;
import jsettlers.common.menu.IMultiplayerConnector;
import jsettlers.common.utils.collections.ChangingList;
import jsettlers.common.utils.collections.IChangingListListener;
import jsettlers.graphics.action.ExecutableAction;
import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.ui.Label;
import jsettlers.graphics.ui.LabeledButton;
import jsettlers.graphics.ui.UIList;
import jsettlers.graphics.ui.UIPanel;

/**
 * The shared multiplayer picker. Lets the player either host a new game (by picking a fresh map) or join an existing
 * game on the connected server. The same panel runs on desktop and Android.
 *
 * @author Copilot
 */
public class MultiplayerPickerPanel extends UIPanel {
	private final UIPanel listPanel = new UIPanel();
	private final UIList<MapMenuItem> mapList;
	private final UIList<IJoinableGame> gameList;
	private final LabeledButton continueButton;
	private boolean hostMode = true;

	/**
	 * Creates a new multiplayer picker panel.
	 *
	 * @param connector
	 *            The connection to the multiplayer server, used to list the joinable games.
	 * @param freshMaps
	 *            Provides the maps that can be hosted.
	 * @param onHost
	 *            Called with the selected map when the player hosts a new game.
	 * @param onJoin
	 *            Called with the selected game when the player joins one.
	 * @param onBack
	 *            Called when the player goes back to the main menu.
	 */
	public MultiplayerPickerPanel(IMultiplayerConnector connector, MapListProvider freshMaps, Consumer<MapMenuItem> onHost,
			Consumer<IJoinableGame> onJoin, Runnable onBack) {
		MenuImageProvider images = MenuImageProvider.getInstance();
		if (images != null) {
			setBackgroundImage(images.getBackground());
		}

		addChild(new Label(Labels.getString("multiplayer-picker-title"), EFontSize.HEADLINE), 0.1f, 0.9f, 0.9f, 1f);

		UIPanel buttonPanel = new UIPanel();
		addChild(buttonPanel, 0.03f, 0.1f, 0.28f, 0.85f);
		addModeButton(buttonPanel, "start-newmultiplayer", 0f, 0.7f, 1f, 0.85f, () -> setHostMode(true));
		addModeButton(buttonPanel, "start-joinmultiplayer", 0f, 0.55f, 1f, 0.7f, () -> setHostMode(false));

		addChild(listPanel, 0.32f, 0.15f, 0.97f, 0.85f);

		mapList = new UIList<>(freshMaps.getMaps(), MapListItem::new, 0.15f);
		gameList = new UIList<>(java.util.Collections.emptyList(), JoinableGameListItem::new, 0.15f);
		connector.getJoinableMultiplayerGames().setListener(new IChangingListListener<IJoinableGame>() {
			@Override
			public void listChanged(ChangingList<? extends IJoinableGame> list) {
				gameList.setItems(list.getItems());
			}
		});

		continueButton = new LabeledButton(Labels.getString("multiplayer-picker-continue"), new ExecutableAction() {
			@Override
			public void execute() {
				if (hostMode) {
					MapMenuItem selected = mapList.getActiveItem();
					if (selected != null) {
						onHost.accept(selected);
					}
				} else {
					IJoinableGame selected = gameList.getActiveItem();
					if (selected != null) {
						onJoin.accept(selected);
					}
				}
			}
		});
		addChild(continueButton, 0.32f, 0.02f, 0.5f, 0.12f);

		LabeledButton backButton = new LabeledButton(Labels.getString("settings-back"), new ExecutableAction() {
			@Override
			public void execute() {
				onBack.run();
			}
		});
		addChild(backButton, 0.55f, 0.02f, 0.7f, 0.12f);

		setHostMode(true);
	}

	private void addModeButton(UIPanel panel, String labelKey, float left, float bottom, float right, float top,
			Runnable action) {
		LabeledButton button = new LabeledButton(Labels.getString(labelKey), new ExecutableAction() {
			@Override
			public void execute() {
				action.run();
			}
		}, EFontSize.HEADLINE);
		panel.addChild(button, left, bottom, right, top);
	}

	private void setHostMode(boolean hostMode) {
		this.hostMode = hostMode;
		listPanel.removeAll();
		if (hostMode) {
			listPanel.addChild(mapList, 0, 0, 1, 1);
			continueButton.setText(Labels.getString("start-newmultiplayer-start"));
		} else {
			listPanel.addChild(gameList, 0, 0, 1, 1);
			continueButton.setText(Labels.getString("start-joinmultiplayer-start"));
		}
	}
}
