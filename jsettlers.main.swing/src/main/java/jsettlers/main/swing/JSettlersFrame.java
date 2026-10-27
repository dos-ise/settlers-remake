/*******************************************************************************
 * Copyright (c) 2015 - 2017
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
package jsettlers.main.swing;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import go.graphics.area.Area;
import go.graphics.region.Region;
import go.graphics.sound.SoundPlayer;
import go.graphics.swing.AreaContainer;
import go.graphics.swing.contextcreator.EBackendType;
import go.graphics.swing.sound.SwingSoundPlayer;

import jsettlers.common.CommitInfo;
import jsettlers.common.action.EActionType;
import jsettlers.common.action.SaveAction;
import jsettlers.common.menu.IJoinPhaseMultiplayerGameConnector;
import jsettlers.common.menu.IMapInterfaceConnector;
import jsettlers.common.menu.IMultiplayerConnector;
import jsettlers.common.menu.IMultiplayerListener;
import jsettlers.common.menu.IStartedGame;
import jsettlers.common.menu.IStartingGame;
import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.map.ETextDrawPosition;
import jsettlers.graphics.map.MapContent;
import jsettlers.graphics.menu.MainMenuActions;
import jsettlers.graphics.menu.MapListProvider;
import jsettlers.graphics.menu.MapMenuItem;
import jsettlers.graphics.menu.MapPreviewImage;
import jsettlers.graphics.menu.MenuContent;
import jsettlers.graphics.menu.MenuImageProvider;
import jsettlers.graphics.menu.SettingsMenuPanel;
import jsettlers.graphics.ui.TextInputDialog;
import jsettlers.logic.map.loading.EMapStartResources;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.logic.map.loading.list.MapList;
import jsettlers.logic.map.loading.newmap.MapFileHeader;
import jsettlers.logic.map.loading.savegame.SavegameLoader;
import jsettlers.logic.player.InitialGameState;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.main.swing.menu.SwingMenuImageDecoder;
import jsettlers.main.swing.menu.multiplayer.lan.LANConnectionPanel;
import jsettlers.main.swing.menu.startinggamemenu.StartingGamePanel;
import jsettlers.main.swing.settings.SettingsManager;

/**
 * @author codingberlin
 */
public class JSettlersFrame extends JFrame {
	private static final long serialVersionUID = 2607082717493797224L;

	private final StartingGamePanel startingGamePanel = new StartingGamePanel(this);
	private final SwingSoundPlayer soundPlayer = new SwingSoundPlayer(SettingsManager.getInstance());

	private Timer redrawTimer;
	private Timer multiplayerUpdateTimer;
	private boolean fullScreen = false;
	private boolean textInputDialogOpen = false;
	private AreaContainer areaContainer;

	JSettlersFrame() throws HeadlessException {
		setTitle("JSettlers - Version: " + CommitInfo.COMMIT_HASH_SHORT);
		setIconImage(JSettlersSwingUtil.APP_ICON);

		SettingsManager settingsManager = SettingsManager.getInstance();

		showMainMenu();
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(1200, 800));
		pack();
		setLocationRelativeTo(null);

		fullScreen = settingsManager.getFullScreenMode();
		updateFullScreenMode();

		KeyboardFocusManager keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		keyboardFocusManager.addKeyEventDispatcher(e -> {
			if (e.getID() == KeyEvent.KEY_PRESSED) {
				if (e.isAltDown() && e.getKeyCode() == KeyEvent.VK_ENTER) {
					if (isGlfwBackend()) {
						if (areaContainer != null) {
							areaContainer.toggleFullscreen();
						}
					} else {
						toogleFullScreenMode();
					}
					return true; // consume this key event.
				}
			}
			return false;
		});
	}

	private void toogleFullScreenMode() {
		fullScreen = !fullScreen;
		SettingsManager.getInstance().setFullScreenMode(fullScreen);
		updateFullScreenMode();
	}

	private void updateFullScreenMode() {
		if(areaContainer != null) areaContainer.removeSurface();
		dispose();

		setResizable(!fullScreen);
		setUndecorated(fullScreen);

		pack();
		if (!isGlfwBackend()) {
			setVisible(true);
		}

		GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice graphicsDevice = graphicsEnvironment.getDefaultScreenDevice();
		graphicsDevice.setFullScreenWindow(fullScreen && !isGlfwBackend() ? this : null);
		if(areaContainer != null) areaContainer.notifyResize();
	}

	/**
	 * The GLFW backend renders into its own native window, so the Swing frame is only a hidden
	 * container. Showing it would produce a second, empty window next to the game window.
	 */
	private boolean isGlfwBackend() {
		return SettingsManager.getInstance().getBackend() == EBackendType.GLFW;
	}

	private void abortRedrawTimerIfPresent() {
		if (redrawTimer != null) {
			redrawTimer.stop();
			redrawTimer = null;
		}
		if (multiplayerUpdateTimer != null) {
			multiplayerUpdateTimer.stop();
			multiplayerUpdateTimer = null;
		}
	}

	public void showMainMenu() {
		showSharedMainMenu();
	}

	/**
	 * Shows the shared, GL rendered main menu. The same menu code runs on desktop and Android.
	 */
	public void showSharedMainMenu() {
		MenuImageProvider.setInstance(new MenuImageProvider(new SwingMenuImageDecoder()));
		MapList mapList = MapList.getDefaultList();
		MapListProvider freshMaps = () -> mapList.getFreshMaps().getItems().stream()
				.map(this::toMapMenuItem).collect(Collectors.toList());
		MapListProvider savedMaps = () -> mapList.getSavedMaps().getItems().stream()
				.map(this::toMapMenuItem).collect(Collectors.toList());

		jsettlers.graphics.menu.MainMenuPanel mainMenuPanel = new jsettlers.graphics.menu.MainMenuPanel(new MainMenuActions() {
			@Override
			public void startNewSinglePlayerGame(MapMenuItem map) {
				MapLoader loader = resolveMap(map);
				if (loader != null) {
					showNewSinglePlayerGameMenu(loader);
				}
			}

			@Override
			public void startLoadGame(MapMenuItem map) {
				MapLoader loader = resolveMap(map);
				if (loader != null) {
					loadSavegame(loader);
				}
			}

			@Override
			public void showSettings() {
				showSettingsPanel();
			}

			@Override
			public void showNewMultiplayerGame() {
				showMultiplayerConnection();
			}

			@Override
			public void showJoinMultiplayerGame() {
				showMultiplayerConnection();
			}

			@Override
			public void exit() {
				exit();
			}
		}, freshMaps, savedMaps);

		MenuContent menuContent = new MenuContent(mainMenuPanel, action -> {
		});
		showMenuContent(menuContent);
	}

	private MapMenuItem toMapMenuItem(MapLoader loader) {
		return new MapMenuItem(loader.getMapId(), loader.getMapName(), loader.getDescription(),
				MapPreviewImage.create(loader.getImage()), loader.getMinPlayers(), loader.getMaxPlayers(),
				loader.getCreationDate());
	}

	private MapLoader resolveMap(MapMenuItem map) {
		return MapList.getDefaultList().getMapById(map.getId());
	}

	private void showMenuContent(MenuContent menuContent) {
		abortRedrawTimerIfPresent();
		Region region = new Region(Region.POSITION_CENTER);
		region.setContent(menuContent);
		SwingUtilities.invokeLater(() -> {
			if (areaContainer != null) {
				// Reuse the existing window so menu and game share one native window.
				areaContainer.setAreaContent(region);
			} else {
				Area area = new Area();
				area.set(region);
				setContentPane(areaContainer = new AreaContainer(area, SettingsManager.getInstance().getBackend(),
						SettingsManager.getInstance().isGraphicsDebug(), SettingsManager.getInstance().getGuiScale()));
			}
			revalidate();
			repaint();
			if (isGlfwBackend()) {
				// The GLFW window is the visible one; hide the Swing frame so it does not show
				// stale Swing content (e.g. the LAN connection panel) next to the GL window.
				setVisible(false);
			} else {
				setVisible(true);
			}
		});
	}

	/**
	 * Stops the render thread and destroys the native window of the current {@link AreaContainer}, if any.
	 * Only needed when the content pane is replaced by a plain Swing panel.
	 */
	private void disposeAreaContainer() {
		if (areaContainer != null) {
			areaContainer.disposeAll();
			areaContainer = null;
		}
	}

	private void showSettingsPanel() {
		SettingsMenuPanel settingsPanel = new SettingsMenuPanel(SettingsManager.getInstance(), this::showSharedMainMenu);
		settingsPanel.initializeValues();
		showMenuContent(new MenuContent(settingsPanel, action -> {
		}));
	}

	private void loadSavegame(MapLoader map) {
		SavegameLoader savegameLoader = (SavegameLoader) map;

		if (savegameLoader != null) {
			MapFileHeader mapFileHeader = savegameLoader.getFileHeader();
			PlayerSetting[] playerSettings = mapFileHeader.getPlayerSettings();
			byte playerId = mapFileHeader.getPlayerId();
			JSettlersGame game = new JSettlersGame(savegameLoader, new InitialGameState(playerId, playerSettings, -1));
			IStartingGame startingGame = game.start();
			showStartingGamePanel(startingGame);
		}
	}

	public void showStartingGamePanel(IStartingGame startingGame) {
		startingGamePanel.setStartingGame(startingGame);
		if (!isGlfwBackend()) {
			setNewContentPane(startingGamePanel);
		}
		// With the GLFW backend the menu window is reused for the game, so the Swing loading
		// panel is skipped and the menu stays visible until the game content replaces it.
	}

	private void setNewContentPane(Container newContent) {
		abortRedrawTimerIfPresent();
		disposeAreaContainer();
		// The GLFW window is destroyed when the area container is disposed, so the Swing frame
		// becomes the only window again and must be shown (e.g. for the LAN connection panel).
		if (isDisplayable() && !isVisible()) {
			setVisible(true);
		}
		setContentPane(newContent);
		revalidate();
		repaint();
	}

	public void exit() {
		soundPlayer.close();
		abortRedrawTimerIfPresent();
		System.exit(0);
	}

	public SoundPlayer getSoundPlayer() {
		return soundPlayer;
	}

	public void setContent(MapContent content) {
		Region region = new Region(500, 500);
		region.setContent(content);

		int fpsLimit = SettingsManager.getInstance().getFpsLimit();
		if(fpsLimit != 0) {
			redrawTimer = new Timer((int)(1000.0f/fpsLimit), e -> region.requestRedraw());
			redrawTimer.setInitialDelay(0);
			redrawTimer.start();
		}

		SwingUtilities.invokeLater(() -> {
			if (areaContainer != null) {
				// Reuse the existing window so menu and game share one native window.
				areaContainer.setAreaContent(region);
				areaContainer.updateFPSLimit(fpsLimit);
			} else {
				Area area = new Area();
				area.set(region);
				setContentPane(areaContainer = new AreaContainer(area, SettingsManager.getInstance().getBackend(), SettingsManager.getInstance().isGraphicsDebug(), SettingsManager.getInstance().getGuiScale()));
				areaContainer.updateFPSLimit(fpsLimit);
			}
			revalidate();
			repaint();
			// The game is rendered in its own (GLFW) window. Hide this menu window so only the
			// game window is visible during the match; it is shown again in setNewContentPane.
			if (isGlfwBackend()) {
				setVisible(false);
			}
		});
	}

	public void showNewSinglePlayerGameMenu(MapLoader mapLoader) {
		jsettlers.graphics.menu.JoinGameMapInfo mapInfo = new jsettlers.graphics.menu.JoinGameMapInfo(mapLoader.getMapName(),
				MapPreviewImage.create(mapLoader.getImage()), mapLoader.getMaxPlayers(),
				toJoinGameSlots(mapLoader.getFileHeader().getPlayerSettings()));
		jsettlers.graphics.menu.JoinGamePanel joinGamePanel = new jsettlers.graphics.menu.JoinGamePanel(mapInfo,
				SettingsManager.getInstance().getPlayer().getName(), settings -> {
			PlayerSetting[] playerSettings = toPlayerSettings(settings, mapLoader.getMaxPlayers());
			InitialGameState initialGameState = new InitialGameState(settings.getHumanPlayerSlot(), playerSettings,
					System.currentTimeMillis(), EMapStartResources.fromMapValue(settings.getStartResourcesValue()),
					settings.getPeaceTime());
			JSettlersGame game = new JSettlersGame(mapLoader, initialGameState);
			showStartingGamePanel(game.start());
		}, this::showSharedMainMenu);
		showMenuContent(new MenuContent(joinGamePanel, action -> {
		}));
	}

	private static jsettlers.graphics.menu.JoinGamePlayerSlotData[] toJoinGameSlots(PlayerSetting[] playerSettings) {
		jsettlers.graphics.menu.JoinGamePlayerSlotData[] slots = new jsettlers.graphics.menu.JoinGamePlayerSlotData[playerSettings.length];
		for (int i = 0; i < playerSettings.length; i++) {
			PlayerSetting setting = playerSettings[i];
			slots[i] = new jsettlers.graphics.menu.JoinGamePlayerSlotData(setting.getCivilisation(), setting.getPlayerType(),
					setting.getTeamId());
		}
		return slots;
	}

	private static PlayerSetting[] toPlayerSettings(jsettlers.graphics.menu.JoinGameSettings settings, int maxPlayers) {
		PlayerSetting[] playerSettings = new PlayerSetting[maxPlayers];
		for (int i = 0; i < maxPlayers; i++) {
			jsettlers.graphics.menu.JoinGamePlayerSlotData slot = settings.getPlayerSlots()[i];
			if (slot.getPlayerType() != null) {
				playerSettings[i] = new PlayerSetting(slot.getPlayerType(), slot.getCivilisation(), slot.getTeam());
			} else {
				playerSettings[i] = new PlayerSetting();
			}
		}
		return playerSettings;
	}

	/**
	 * Shows the Swing LAN multiplayer connection panel. This is the temporary entry point for the
	 * multiplayer until the shared multiplayer panels are fully wired up.
	 */
	private void showMultiplayerConnection() {
		abortRedrawTimerIfPresent();
		LANConnectionPanel lanConnectionPanel = new LANConnectionPanel(this);
		setNewContentPane(lanConnectionPanel);
		multiplayerUpdateTimer = new Timer(500, e -> {
			lanConnectionPanel.update();
			lanConnectionPanel.repaint();
		});
		multiplayerUpdateTimer.setInitialDelay(0);
		multiplayerUpdateTimer.start();
	}

	public void showNewMultiPlayerGameMenu(MapLoader mapLoader, IMultiplayerConnector connector) {
		jsettlers.graphics.menu.JoinGameMapInfo mapInfo = new jsettlers.graphics.menu.JoinGameMapInfo(mapLoader.getMapName(),
				MapPreviewImage.create(mapLoader.getImage()), mapLoader.getMaxPlayers(),
				toJoinGameSlots(mapLoader.getFileHeader().getPlayerSettings()));
		jsettlers.common.menu.IOpenMultiplayerGameInfo gameInfo = new jsettlers.graphics.menu.OpenMultiplayerGameInfo(
				SettingsManager.getInstance().getUserName(), mapLoader);
		jsettlers.graphics.menu.MultiplayerJoinGamePanel panel = jsettlers.graphics.menu.MultiplayerJoinGamePanel.createHost(
				mapInfo, connector.getPlayerUUID(), connector, gameInfo, this::showSharedMainMenu, new IMultiplayerListener() {
					@Override
					public void gameIsStarting(IStartingGame game) {
						showStartingGamePanel(game);
					}

					@Override
					public void gameAborted() {
						showSharedMainMenu();
					}
				});
		showMenuContent(new MenuContent(panel, action -> {
		}));
	}

	public void showJoinMultiplayerMenu(IJoinPhaseMultiplayerGameConnector joinPhaseMultiplayerGameConnector, MapLoader mapLoader, String playerUUID) {
		jsettlers.graphics.menu.JoinGameMapInfo mapInfo = new jsettlers.graphics.menu.JoinGameMapInfo(mapLoader.getMapName(),
				MapPreviewImage.create(mapLoader.getImage()), mapLoader.getMaxPlayers(),
				toJoinGameSlots(mapLoader.getFileHeader().getPlayerSettings()));
		jsettlers.graphics.menu.MultiplayerJoinGamePanel panel = jsettlers.graphics.menu.MultiplayerJoinGamePanel.createJoin(
				mapInfo, playerUUID, joinPhaseMultiplayerGameConnector, this::showSharedMainMenu, new IMultiplayerListener() {
					@Override
					public void gameIsStarting(IStartingGame game) {
						showStartingGamePanel(game);
					}

					@Override
					public void gameAborted() {
						showSharedMainMenu();
					}
				});
		showMenuContent(new MenuContent(panel, action -> {
		}));
	}

	public void showEndgameStatistics(IStartedGame game) {
		jsettlers.graphics.menu.EndgameStatsPanel statsPanel = new jsettlers.graphics.menu.EndgameStatsPanel(this::showSharedMainMenu);
		statsPanel.setGame(game);
		showMenuContent(new MenuContent(statsPanel, action -> {
		}));
	}

	private void showTextInputDialog(String title, Consumer<String> onEntered) {
		SwingUtilities.invokeLater(() -> {
			if (textInputDialogOpen) {
				return;
			}
			textInputDialogOpen = true;
			try {
				String text = JOptionPane.showInputDialog(this, title, Labels.getString("chat_title"), JOptionPane.PLAIN_MESSAGE);
				if (text != null && !text.trim().isEmpty()) {
					onEntered.accept(text);
				}
			} finally {
				textInputDialogOpen = false;
			}
		});
	}

	public IMapInterfaceConnector showStartedGame(IStartedGame startedGame) {
		MapContent content = new MapContent(startedGame, soundPlayer, ETextDrawPosition.DESKTOP);
		content.setTextInputProvider(this::showTextInputDialog);
		SwingUtilities.invokeLater(() -> setContent(content));
		startedGame.setGameExitListener(exitGame -> SwingUtilities.invokeLater(() -> showEndgameStatistics(exitGame)));
		IMapInterfaceConnector connector = content.getInterfaceConnector();
		// A bare SAVE request (SaveAction with a null description) is intercepted here to ask for a
		// description and re-fired as a committed SaveAction; only the committed one is written by the
		// game logic. The shared GL dialog is used on all platforms.
		connector.addListener(action -> {
			if (action.getActionType() != EActionType.SAVE) {
				return;
			}
			boolean needsPrompt = !(action instanceof SaveAction) || ((SaveAction) action).getDescription() == null;
			if (!needsPrompt) {
				return; // already the committed save; let the logic handle it (avoid a prompt loop)
			}
			SwingUtilities.invokeLater(() -> {
				content.showDialog(new TextInputDialog(Labels.getString("save-game-description"), description -> {
					content.hideDialog();
					// An empty description falls back to the map name in MainGrid.generateSaveHeader.
					connector.fireAction(new SaveAction(description));
				}, content::hideDialog));
			});
		});
		return connector;
	}
}
