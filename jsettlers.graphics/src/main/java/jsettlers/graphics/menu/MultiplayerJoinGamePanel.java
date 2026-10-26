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
import jsettlers.common.menu.ENetworkMessage;
import jsettlers.common.menu.IChatMessageListener;
import jsettlers.common.menu.IJoiningGame;
import jsettlers.common.menu.IJoiningGameListener;
import jsettlers.common.menu.IJoinPhaseMultiplayerGameConnector;
import jsettlers.common.menu.IMultiplayerConnector;
import jsettlers.common.menu.IMultiplayerListener;
import jsettlers.common.menu.IMultiplayerPlayer;
import jsettlers.common.menu.IMultiplayerSlot;
import jsettlers.common.menu.IOpenMultiplayerGameInfo;
import jsettlers.common.menu.EPeaceTime;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.utils.collections.ChangingList;
import jsettlers.graphics.action.ExecutableAction;
import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.ui.AspectRatioImagePanel;
import jsettlers.graphics.ui.CycleButton;
import jsettlers.graphics.ui.Label;
import jsettlers.graphics.ui.LabeledButton;
import jsettlers.graphics.ui.UIInput;
import jsettlers.graphics.ui.UIPanel;

/**
 * The shared multiplayer game setup panel. Shows the selected map, the network controlled player slots, the game
 * settings and a chat. The same panel runs on desktop and Android; the host opens a new game through the
 * {@link IMultiplayerConnector}, a joining player gets the {@link IJoinPhaseMultiplayerGameConnector} directly.
 *
 * @author Copilot
 */
public class MultiplayerJoinGamePanel extends UIPanel {
	private static final float SLOTS_TOP = 0.72f;
	private static final float SLOTS_BOTTOM = 0.20f;
	private static final float MAX_ROW_HEIGHT = 0.09f;

	private final JoinGameMapInfo mapInfo;
	private final String playerUUID;
	private final boolean isHost;
	private final Runnable onCancel;
	private final IMultiplayerListener multiplayerListener;

	private IJoinPhaseMultiplayerGameConnector connector;
	private final UIPanel slotsPanel = new UIPanel();
	private final List<MultiplayerSlotRow> slotRows = new ArrayList<>();
	private final StringBuilder chatText = new StringBuilder();
	private final Label chatLabel = new Label("", EFontSize.NORMAL, Label.EHorizontalAlignment.LEFT);
	private final UIInput chatInput = new UIInput();
	private final LabeledButton startButton;
	private final LabeledButton readyButton;
	private final CycleButton startResourcesButton;
	private final CycleButton peaceTimeButton;

	/**
	 * Creates a panel that hosts a new multiplayer game.
	 *
	 * @param mapInfo
	 *            The map to play on.
	 * @param playerUUID
	 *            The uuid of the local player.
	 * @param connector
	 *            The connection to the multiplayer server.
	 * @param gameInfo
	 *            The information needed to open the new game on the server.
	 * @param onCancel
	 *            Called when the player presses cancel.
	 * @param multiplayerListener
	 *            Receives game start and abort events.
	 */
	public static MultiplayerJoinGamePanel createHost(JoinGameMapInfo mapInfo, String playerUUID, IMultiplayerConnector connector,
			IOpenMultiplayerGameInfo gameInfo, Runnable onCancel, IMultiplayerListener multiplayerListener) {
		MultiplayerJoinGamePanel panel = new MultiplayerJoinGamePanel(mapInfo, playerUUID, true, onCancel, multiplayerListener);
		panel.openNewGame(connector, gameInfo);
		return panel;
	}

	/**
	 * Creates a panel that joins an existing multiplayer game.
	 *
	 * @param mapInfo
	 *            The map of the game to join.
	 * @param playerUUID
	 *            The uuid of the local player.
	 * @param connector
	 *            The join phase connector of the game.
	 * @param onCancel
	 *            Called when the player presses cancel.
	 * @param multiplayerListener
	 *            Receives game start and abort events.
	 */
	public static MultiplayerJoinGamePanel createJoin(JoinGameMapInfo mapInfo, String playerUUID,
			IJoinPhaseMultiplayerGameConnector connector, Runnable onCancel, IMultiplayerListener multiplayerListener) {
		MultiplayerJoinGamePanel panel = new MultiplayerJoinGamePanel(mapInfo, playerUUID, false, onCancel, multiplayerListener);
		panel.setup(connector);
		return panel;
	}

	private MultiplayerJoinGamePanel(JoinGameMapInfo mapInfo, String playerUUID, boolean isHost, Runnable onCancel,
			IMultiplayerListener multiplayerListener) {
		this.mapInfo = mapInfo;
		this.playerUUID = playerUUID;
		this.isHost = isHost;
		this.onCancel = onCancel;
		this.multiplayerListener = multiplayerListener;

		MenuImageProvider images = MenuImageProvider.getInstance();
		if (images != null) {
			setBackgroundImage(images.getBackground());
		}

		String titleKey = isHost ? "join-game-panel-new-multi-player-game-title" : "join-game-panel-join-multi-player-game-title";
		addChild(new Label(Labels.getString(titleKey), EFontSize.HEADLINE), 0.1f, 0.92f, 0.9f, 1f);

		if (mapInfo.getPreview() != null) {
			addChild(new AspectRatioImagePanel(mapInfo.getPreview()), 0.03f, 0.60f, 0.18f, 0.88f);
		}
		addChild(new Label(mapInfo.getMapName(), EFontSize.NORMAL, Label.EHorizontalAlignment.LEFT), 0.03f, 0.55f, 0.18f, 0.60f);

		startResourcesButton = new CycleButton(startResourcesOptions(), 2, index -> {
			if (connector != null) {
				connector.setStartResources(index + 1);
			}
		});
		peaceTimeButton = new CycleButton(peaceTimeOptions(), 0, index -> {
			if (connector != null) {
				connector.setPeaceTime(EPeaceTime.VALUES[index]);
			}
		});
		startResourcesButton.setEnabled(isHost);
		peaceTimeButton.setEnabled(isHost);

		addSetting(0, "join-game-panel-start-resources", startResourcesButton);
		addSetting(1, "join-game-panel-peace-time", peaceTimeButton);

		addChild(slotsPanel, 0f, 0f, 1f, 1f);

		addChild(chatLabel, 0.03f, 0.10f, 0.5f, 0.20f);
		addChild(chatInput, 0.03f, 0.02f, 0.4f, 0.10f);
		LabeledButton sendButton = new LabeledButton(Labels.getString("join-game-panel-send-chat-message"), new ExecutableAction() {
			@Override
			public void execute() {
				sendChat();
			}
		});
		addChild(sendButton, 0.42f, 0.02f, 0.5f, 0.10f);

		LabeledButton cancelButton = new LabeledButton(Labels.getString("join-game-panel-cancel"), new ExecutableAction() {
			@Override
			public void execute() {
				if (connector != null) {
					connector.abort();
				}
				onCancel.run();
			}
		});
		addChild(cancelButton, 0.55f, 0.02f, 0.7f, 0.10f);

		startButton = new LabeledButton(Labels.getString("join-game-panel-start"), new ExecutableAction() {
			@Override
			public void execute() {
				if (connector != null) {
					connector.startGame();
				}
			}
		});
		readyButton = new LabeledButton(Labels.getString("start-joining-ready"), new ExecutableAction() {
			@Override
			public void execute() {
				if (connector != null) {
					connector.setReady(true);
				}
			}
		});

		if (isHost) {
			addChild(startButton, 0.72f, 0.02f, 0.9f, 0.10f);
		} else {
			addChild(readyButton, 0.72f, 0.02f, 0.9f, 0.10f);
		}
	}

	private void addSetting(int column, String labelKey, CycleButton button) {
		float left = 0.24f + column * 0.2f;
		float right = left + 0.16f;
		addChild(new Label(Labels.getString(labelKey), EFontSize.NORMAL, Label.EHorizontalAlignment.LEFT), left, 0.84f, right, 0.90f);
		addChild(button, left, 0.80f, right, 0.84f);
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

	private void openNewGame(IMultiplayerConnector connector, IOpenMultiplayerGameInfo gameInfo) {
		IJoiningGame joiningGame = connector.openNewMultiplayerGame(gameInfo);
		joiningGame.setListener(new IJoiningGameListener() {
			@Override
			public void joinProgressChanged(jsettlers.common.menu.EProgressState state, float progress) {
			}

			@Override
			public void gameJoined(IJoinPhaseMultiplayerGameConnector connector) {
				setup(connector);
			}
		});
	}

	private void setup(IJoinPhaseMultiplayerGameConnector connector) {
		this.connector = connector;
		connector.getSlots().setListener(changingSlots -> updateSlots(changingSlots));
		connector.setChatListener(new IChatMessageListener() {
			@Override
			public void chatMessageReceived(String authorId, String message) {
				appendChat(authorId + ": " + message);
			}

			@Override
			public void systemMessageReceived(IMultiplayerPlayer author, ENetworkMessage message) {
				appendChat(Labels.getString("network-message-" + message.name()));
			}
		});
		connector.setMultiplayerListener(multiplayerListener);
		updateSlots(connector.getSlots());
	}

	private void appendChat(String line) {
		chatText.append(line).append('\n');
		chatLabel.setText(chatText.toString());
	}

	private void sendChat() {
		String message = chatInput.getText();
		if (!message.isEmpty() && connector != null) {
			connector.sendChatMessage(message);
			chatInput.setText("");
		}
	}

	private void updateSlots(ChangingList<? extends IMultiplayerSlot> changingSlots) {
		List<? extends IMultiplayerSlot> slots = changingSlots.getItems();
		slotsPanel.removeAll();
		slotRows.clear();
		if (slots.isEmpty()) {
			return;
		}
		float rowHeight = Math.min((SLOTS_TOP - SLOTS_BOTTOM) / slots.size(), MAX_ROW_HEIGHT);
		for (int i = 0; i < slots.size(); i++) {
			IMultiplayerSlot slot = slots.get(i);
			IMultiplayerPlayer player = slot.getPlayer();
			boolean isMine = player != null && playerUUID.equals(player.getId());
			MultiplayerSlotRow row = new MultiplayerSlotRow((byte) i, isMine);
			row.update(slot);
			float top = SLOTS_TOP - i * rowHeight;
			row.addTo(slotsPanel, top - rowHeight, top);
			slotRows.add(row);
		}
	}

	/**
	 * A single multiplayer slot row: name, civilisation, type, team and ready state. Only the local
	 * player's own slot has editable controls.
	 */
	private class MultiplayerSlotRow {
		private final Label nameLabel;
		private final CycleButton civButton;
		private final CycleButton typeButton;
		private final CycleButton teamButton;
		private final Label readyLabel;
		private final byte slotIndex;
		private final boolean isMine;

		MultiplayerSlotRow(byte slotIndex, boolean isMine) {
			this.slotIndex = slotIndex;
			this.isMine = isMine;
			nameLabel = new Label("", EFontSize.NORMAL, Label.EHorizontalAlignment.LEFT);

			List<String> civOptions = new ArrayList<>();
			civOptions.add(Labels.getString("civilisation-random"));
			for (ECivilisation civ : ECivilisation.VALUES) {
				civOptions.add(Labels.getString("civilisation-" + civ.name()));
			}
			civButton = new CycleButton(civOptions, 0, newIndex -> {
				if (connector != null) {
					connector.setCivilisation(slotIndex, newIndex == 0 ? null : ECivilisation.VALUES[newIndex - 1]);
				}
			});

			List<String> typeOptions = new ArrayList<>();
			for (EPlayerType type : EPlayerType.VALUES) {
				typeOptions.add(Labels.getString("player-type-" + type.name()));
			}
			typeButton = new CycleButton(typeOptions, 0, newIndex -> {
				if (connector != null) {
					connector.setType(slotIndex, EPlayerType.VALUES[newIndex]);
				}
			});

			List<String> teamOptions = new ArrayList<>();
			for (int i = 1; i <= mapInfo.getMaxPlayers(); i++) {
				teamOptions.add(String.valueOf(i));
			}
			teamButton = new CycleButton(teamOptions, 0, newIndex -> {
				if (connector != null) {
					connector.setTeam(slotIndex, (byte) (newIndex + 1));
				}
			});

			readyLabel = new Label("", EFontSize.NORMAL, Label.EHorizontalAlignment.LEFT);

			civButton.setEnabled(isMine);
			typeButton.setEnabled(isMine);
			teamButton.setEnabled(isMine);
		}

		void update(IMultiplayerSlot slot) {
			IMultiplayerPlayer player = slot.getPlayer();
			nameLabel.setText(player == null ? "" : player.getName());
			if (slot.getCivilisation() != null) {
				civButton.setIndex(civIndex(slot.getCivilisation()));
			}
			typeButton.setIndex(typeIndex(slot.getType()));
			teamButton.setIndex(Math.max(0, slot.getTeam() - 1));
			if (player == null) {
				readyLabel.setText("");
			} else if (player.isReady()) {
				readyLabel.setText(Labels.getString("start-joining-ready"));
			} else {
				readyLabel.setText("");
			}
		}

		private int civIndex(ECivilisation civilisation) {
			for (int i = 0; i < ECivilisation.VALUES.length; i++) {
				if (ECivilisation.VALUES[i] == civilisation) {
					return i + 1;
				}
			}
			return 0;
		}

		private int typeIndex(EPlayerType type) {
			for (int i = 0; i < EPlayerType.VALUES.length; i++) {
				if (EPlayerType.VALUES[i] == type) {
					return i;
				}
			}
			return 0;
		}

		void addTo(UIPanel panel, float bottom, float top) {
			panel.addChild(nameLabel, 0.22f, bottom, 0.42f, top);
			panel.addChild(civButton, 0.44f, bottom, 0.60f, top);
			panel.addChild(typeButton, 0.62f, bottom, 0.82f, top);
			panel.addChild(teamButton, 0.84f, bottom, 0.97f, top);
			panel.addChild(readyLabel, 0.98f, bottom, 1f, top);
		}
	}
}
