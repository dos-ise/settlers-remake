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

import go.graphics.text.EFontSize;
import jsettlers.common.ai.EPlayerType;
import jsettlers.common.menu.EPeaceTime;
import jsettlers.common.player.ECivilisation;
import jsettlers.graphics.action.ExecutableAction;
import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.ui.AspectRatioImagePanel;
import jsettlers.graphics.ui.CycleButton;
import jsettlers.graphics.ui.Label;
import jsettlers.graphics.ui.LabeledButton;
import jsettlers.graphics.ui.UIPanel;

/**
 * The shared game setup panel (join game). Shows the selected map, the game settings (number of players, start
 * resources, peace time) and the player slots. The same panel runs on desktop and Android; the host provides the map
 * info and a {@link IJoinGameStarter} callback.
 *
 * @author Copilot
 */
public class JoinGamePanel extends UIPanel {
	private static final float SLOTS_TOP = 0.72f;
	private static final float SLOTS_BOTTOM = 0.14f;
	private static final float MAX_ROW_HEIGHT = 0.09f;

	private final JoinGameMapInfo mapInfo;
	private final String playerName;
	private final IJoinGameStarter starter;
	private final Runnable onCancel;

	private final CycleButton numberOfPlayersButton;
	private final CycleButton startResourcesButton;
	private final CycleButton peaceTimeButton;
	private final List<JoinGamePlayerSlot> playerSlots = new ArrayList<>();
	private final UIPanel slotsPanel = new UIPanel();

	/**
	 * Creates a new join game panel for a single player game.
	 *
	 * @param mapInfo
	 *            The map to set up.
	 * @param playerName
	 *            The name of the human player.
	 * @param starter
	 *            Called with the collected settings when the player presses start.
	 * @param onCancel
	 *            Called when the player presses cancel.
	 */
	public JoinGamePanel(JoinGameMapInfo mapInfo, String playerName, IJoinGameStarter starter, Runnable onCancel) {
		this.mapInfo = mapInfo;
		this.playerName = playerName;
		this.starter = starter;
		this.onCancel = onCancel;

		MenuImageProvider images = MenuImageProvider.getInstance();
		if (images != null) {
			setBackgroundImage(images.getBackground());
		}

		addChild(new Label(Labels.getString("join-game-panel-new-single-player-game-title"), EFontSize.HEADLINE), 0.1f, 0.92f, 0.9f, 1f);

		if (mapInfo.getPreview() != null) {
			addChild(new AspectRatioImagePanel(mapInfo.getPreview()), 0.03f, 0.60f, 0.18f, 0.88f);
		}
		addChild(new Label(mapInfo.getMapName(), EFontSize.NORMAL, Label.EHorizontalAlignment.LEFT), 0.03f, 0.55f, 0.18f, 0.60f);

		numberOfPlayersButton = new CycleButton(playerCountOptions(), mapInfo.getMaxPlayers() - 1, index -> updateNumberOfPlayerSlots(index + 1));
		startResourcesButton = new CycleButton(startResourcesOptions(), 2, null);
		peaceTimeButton = new CycleButton(peaceTimeOptions(), 0, null);

		addSetting(0, "join-game-panel-number-of-players", numberOfPlayersButton);
		addSetting(1, "join-game-panel-start-resources", startResourcesButton);
		addSetting(2, "join-game-panel-peace-time", peaceTimeButton);

		addChild(slotsPanel, 0f, 0f, 1f, 1f);
		buildPlayerSlots();
		updateNumberOfPlayerSlots(mapInfo.getMaxPlayers());

		LabeledButton cancelButton = new LabeledButton(Labels.getString("join-game-panel-cancel"), new ExecutableAction() {
			@Override
			public void execute() {
				onCancel.run();
			}
		});
		addChild(cancelButton, 0.1f, 0.02f, 0.45f, 0.10f);

		LabeledButton startButton = new LabeledButton(Labels.getString("join-game-panel-start"), new ExecutableAction() {
			@Override
			public void execute() {
				startGame();
			}
		});
		addChild(startButton, 0.5f, 0.02f, 0.9f, 0.10f);
	}

	private void addSetting(int column, String labelKey, CycleButton button) {
		float left = 0.24f + column * 0.2f;
		float right = left + 0.16f;
		addChild(new Label(Labels.getString(labelKey), EFontSize.NORMAL, Label.EHorizontalAlignment.LEFT), left, 0.84f, right, 0.90f);
		addChild(button, left, 0.80f, right, 0.84f);
	}

	private List<String> playerCountOptions() {
		List<String> options = new ArrayList<>();
		for (int i = 1; i <= mapInfo.getMaxPlayers(); i++) {
			options.add(String.valueOf(i));
		}
		return options;
	}

	private List<String> startResourcesOptions() {
		List<String> options = new ArrayList<>();
		for (String name : new String[] { "LOW_GOODS", "MEDIUM_GOODS", "HIGH_GOODS" }) {
			options.add(Labels.getString("map-start-resources-" + name));
		}
		return options;
	}

	private List<String> peaceTimeOptions() {
		List<String> options = new ArrayList<>();
		for (EPeaceTime peaceTime : EPeaceTime.VALUES) {
			options.add(Labels.getString("peace-time-" + peaceTime.name()));
		}
		return options;
	}

	private void buildPlayerSlots() {
		playerSlots.clear();
		for (byte i = 0; i < mapInfo.getMaxPlayers(); i++) {
			JoinGamePlayerSlot slot = new JoinGamePlayerSlot(i);
			JoinGamePlayerSlotData[] initialSlots = mapInfo.getInitialSlots();
			if (initialSlots != null && i < initialSlots.length && initialSlots[i] != null) {
				slot.setInitial(initialSlots[i]);
			}
			playerSlots.add(slot);
		}
	}

	private void updateNumberOfPlayerSlots(int count) {
		slotsPanel.removeAll();
		float rowHeight = Math.min((SLOTS_TOP - SLOTS_BOTTOM) / count, MAX_ROW_HEIGHT);
		for (int i = 0; i < count; i++) {
			float top = SLOTS_TOP - i * rowHeight;
			playerSlots.get(i).addTo(slotsPanel, top - rowHeight, top);
		}
	}

	private void startGame() {
		int playerCount = numberOfPlayersButton.getIndex() + 1;
		JoinGamePlayerSlotData[] playerSlotsData = new JoinGamePlayerSlotData[mapInfo.getMaxPlayers()];
		for (int i = 0; i < mapInfo.getMaxPlayers(); i++) {
			if (i < playerCount) {
				JoinGamePlayerSlot slot = playerSlots.get(i);
				playerSlotsData[i] = new JoinGamePlayerSlotData(slot.getCivilisation(), slot.getPlayerType(), slot.getTeam());
			} else {
				playerSlotsData[i] = new JoinGamePlayerSlotData(null, null, null);
			}
		}
		int startResourcesValue = startResourcesButton.getIndex() + 1;
		EPeaceTime peaceTime = EPeaceTime.VALUES[peaceTimeButton.getIndex()];
		starter.startGame(new JoinGameSettings(playerSlotsData, startResourcesValue, peaceTime, (byte) 0));
	}

	/**
	 * A single player slot row: name, civilisation, player type and team.
	 */
	private class JoinGamePlayerSlot {
		private final Label nameLabel;
		private final CycleButton civilisationButton;
		private final CycleButton typeButton;
		private final CycleButton teamButton;
		private final EPlayerType[] typeOptions;
		private ECivilisation civilisation;
		private EPlayerType playerType;
		private byte team;

		JoinGamePlayerSlot(byte index) {
			nameLabel = new Label("", EFontSize.NORMAL, Label.EHorizontalAlignment.LEFT);

			List<String> civOptions = new ArrayList<>();
			civOptions.add(Labels.getString("civilisation-random"));
			for (ECivilisation civ : ECivilisation.VALUES) {
				civOptions.add(Labels.getString("civilisation-" + civ.name()));
			}
			civilisationButton = new CycleButton(civOptions, 0, newIndex -> {
				civilisation = newIndex == 0 ? null : ECivilisation.VALUES[newIndex - 1];
				updateAiPlayerName();
			});

			typeOptions = index == 0 ? EPlayerType.VALUES_HUMAN_FIRST : EPlayerType.VALUES_AI_ONLY;
			List<String> typeOptionsText = new ArrayList<>();
			for (EPlayerType type : typeOptions) {
				typeOptionsText.add(Labels.getString("player-type-" + type.name()));
			}
			typeButton = new CycleButton(typeOptionsText, 0, newIndex -> {
				playerType = typeOptions[newIndex];
				updateAiPlayerName();
			});

			List<String> teamOptions = new ArrayList<>();
			for (int i = 1; i <= mapInfo.getMaxPlayers(); i++) {
				teamOptions.add(String.valueOf(i));
			}
			teamButton = new CycleButton(teamOptions, 0, newIndex -> team = (byte) (newIndex + 1));

			playerType = typeOptions[0];
			team = 1;
			updateAiPlayerName();
		}

		void setInitial(JoinGamePlayerSlotData data) {
			if (data.getCivilisation() != null) {
				civilisation = data.getCivilisation();
				civilisationButton.setIndex(civilisationIndex(civilisation));
				civilisationButton.setEnabled(false);
			}
			if (data.getPlayerType() != null) {
				playerType = data.getPlayerType();
				typeButton.setIndex(typeIndex(playerType));
				typeButton.setEnabled(false);
			}
			if (data.getTeam() != null) {
				team = data.getTeam();
				teamButton.setIndex(team - 1);
				teamButton.setEnabled(false);
			}
			updateAiPlayerName();
		}

		private int civilisationIndex(ECivilisation civilisation) {
			for (int i = 0; i < ECivilisation.VALUES.length; i++) {
				if (ECivilisation.VALUES[i] == civilisation) {
					return i + 1;
				}
			}
			return 0;
		}

		private int typeIndex(EPlayerType type) {
			for (int i = 0; i < typeOptions.length; i++) {
				if (typeOptions[i] == type) {
					return i;
				}
			}
			return 0;
		}

		private void updateAiPlayerName() {
			if (playerType != EPlayerType.HUMAN) {
				if (civilisation != null) {
					nameLabel.setText(Labels.getString("player-name-" + civilisation.name() + "-" + playerType.name()));
				} else {
					nameLabel.setText(Labels.getString("player-name-random"));
				}
			} else {
				nameLabel.setText(playerName);
			}
		}

		void addTo(UIPanel panel, float bottom, float top) {
			panel.addChild(nameLabel, 0.22f, bottom, 0.42f, top);
			panel.addChild(civilisationButton, 0.44f, bottom, 0.60f, top);
			panel.addChild(typeButton, 0.62f, bottom, 0.82f, top);
			panel.addChild(teamButton, 0.84f, bottom, 0.97f, top);
		}

		ECivilisation getCivilisation() {
			return civilisation;
		}

		EPlayerType getPlayerType() {
			return playerType;
		}

		byte getTeam() {
			return team;
		}
	}
}
