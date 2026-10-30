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

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
import go.graphics.swing.contextcreator.BackendSelector;
import go.graphics.swing.contextcreator.EBackendType;
import go.graphics.swing.contextcreator.GLFWContextCreator;
import go.graphics.swing.sound.SwingSoundPlayer;
import go.graphics.text.EFontSize;

import jsettlers.common.CommitInfo;
import jsettlers.common.action.EActionType;
import jsettlers.common.action.SaveAction;
import jsettlers.common.menu.EGameError;
import jsettlers.common.menu.EProgressState;
import jsettlers.common.menu.IJoinableGame;
import jsettlers.common.menu.IJoinPhaseMultiplayerGameConnector;
import jsettlers.common.menu.IJoiningGame;
import jsettlers.common.menu.IJoiningGameListener;
import jsettlers.common.menu.IMapInterfaceConnector;
import jsettlers.common.menu.IMultiplayerConnector;
import jsettlers.common.menu.IMultiplayerListener;
import jsettlers.common.menu.IStartedGame;
import jsettlers.common.menu.IStartingGame;
import jsettlers.common.menu.IStartingGameListener;
import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.map.ETextDrawPosition;
import jsettlers.graphics.map.MapContent;
import jsettlers.graphics.map.draw.ImageProvider;
import jsettlers.graphics.menu.GameDataActions;
import jsettlers.graphics.menu.GameDataPanel;
import jsettlers.graphics.menu.MainMenuActions;
import jsettlers.graphics.menu.MapListProvider;
import jsettlers.graphics.menu.MapMenuItem;
import jsettlers.graphics.menu.MapPreviewImage;
import jsettlers.graphics.menu.MenuContent;
import jsettlers.graphics.menu.MenuImageProvider;
import jsettlers.graphics.menu.MultiplayerPickerPanel;
import jsettlers.graphics.menu.SettingsMenuPanel;
import jsettlers.graphics.ui.Label;
import jsettlers.graphics.ui.TextInputDialog;
import jsettlers.graphics.ui.UIPanel;
import jsettlers.logic.map.loading.EMapStartResources;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.logic.map.loading.list.MapList;
import jsettlers.logic.map.loading.newmap.MapFileHeader;
import jsettlers.logic.map.loading.savegame.SavegameLoader;
import jsettlers.logic.player.InitialGameState;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.main.MultiplayerConnector;
import jsettlers.main.swing.menu.SwingMenuImageDecoder;
import jsettlers.main.swing.settings.SettingsManager;
import jsettlers.network.infrastructure.log.ConsoleConsumerLogger;
import jsettlers.network.infrastructure.log.Logger;

/**
 * @author codingberlin
 */
public class JSettlersFrame extends JFrame {
	private static final long serialVersionUID = 2607082717493797224L;

	private final SwingSoundPlayer soundPlayer = new SwingSoundPlayer(SettingsManager.getInstance());

	private Timer redrawTimer;
	private Timer multiplayerUpdateTimer;
	private boolean fullScreen = false;
	private boolean textInputDialogOpen = false;
	private AreaContainer areaContainer;
	private IMultiplayerConnector currentMultiplayerConnector;

	JSettlersFrame() throws HeadlessException {
		setTitle("JSettlers - Version: " + CommitInfo.COMMIT_HASH_SHORT);
		setIconImage(JSettlersSwingUtil.APP_ICON);
		GLFWContextCreator.setWindowIcon(JSettlersSwingUtil.APP_ICON);

		SettingsManager settingsManager = SettingsManager.getInstance();

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
		return BackendSelector.resolveBackend(SettingsManager.getInstance().getBackend()) == EBackendType.GLFW;
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
		closeMultiplayerConnector();
		MenuImageProvider.setInstance(new MenuImageProvider(new SwingMenuImageDecoder()));
		MapList mapList = MapList.getDefaultList();
		MapListProvider freshMaps = () -> mapList.getFreshMaps().getItems().stream()
				.map(loader -> toMapMenuItem(loader, false)).collect(Collectors.toList());
		MapListProvider savedMaps = () -> mapList.getSavedMaps().getItems().stream()
				.map(loader -> toMapMenuItem(loader, true)).collect(Collectors.toList());

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
			public void deleteSavegame(MapMenuItem map) {
				MapLoader loader = resolveMap(map);
				if (loader != null) {
					loader.getListedMap().delete();
					MapList.getDefaultList().reload();
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

	/**
	 * Shows the shared, GL rendered game data setup panel. It is shown when the original Settlers 3
	 * game data is missing and lets the user select the installation folder or download the game data
	 * from GOG or Ubisoft Connect. The same panel code runs on desktop and Android.
	 */
	public void showGameDataSetup(GameDataActions actions) {
		MenuImageProvider.setInstance(new MenuImageProvider(new SwingMenuImageDecoder()));
		GameDataPanel panel = new GameDataPanel(actions);
		showMenuContent(new MenuContent(panel, action -> {
		}));
	}

	/**
	 * Shows the shared game data setup panel and blocks until the user has selected or downloaded a
	 * Settlers 3 folder.
	 *
	 * @return The selected folder, or null if the user cancelled.
	 */
	public File askForGameDataFolder() {
		CompletableFuture<File> folderFuture = new CompletableFuture<>();
		SwingGameDataActions actions = new SwingGameDataActions(this, folderFuture);
		showGameDataSetup(actions);
		try {
			return folderFuture.get();
		} catch (InterruptedException | ExecutionException ex) {
			return null;
		}
	}

	private MapMenuItem toMapMenuItem(MapLoader loader, boolean savegame) {
		return new MapMenuItem(loader.getMapId(), loader.getMapName(), loader.getDescription(),
				MapPreviewImage.create(loader.getImage()), loader.getMinPlayers(), loader.getMaxPlayers(),
				loader.getCreationDate(), savegame);
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

	/**
	 * Shows a GL loading panel on the same surface as the game while it is starting, like on Android.
	 * The native Swing loading panel is not needed anymore.
	 */
	public void showStartingGamePanel(IStartingGame startingGame) {
		UIPanel loadingPanel = new UIPanel();
		MenuImageProvider images = MenuImageProvider.getInstance();
		if (images != null) {
			loadingPanel.setBackgroundImage(images.getBackground());
		}
		Label loadingLabel = new Label(Labels.getProgress(EProgressState.LOADING), EFontSize.HEADLINE);
		loadingPanel.addChild(loadingLabel, 0.1f, 0.4f, 0.9f, 0.6f);
		showMenuContent(new MenuContent(loadingPanel, action -> {
		}));

		startingGame.setListener(new IStartingGameListener() {
			@Override
			public void startProgressChanged(EProgressState state, float progress) {
				SwingUtilities.invokeLater(() -> loadingLabel.setText(Labels.getProgress(state)));
			}

			@Override
			public IMapInterfaceConnector preLoadFinished(IStartedGame game) {
				return showStartedGame(game);
			}

			@Override
			public void startFailed(EGameError errorType, Exception exception) {
				String errorMessage;
				if (errorType == EGameError.MAPLOADING_ERROR) {
					errorMessage = Labels.getString("errordlg-map-loading-failed");
				} else {
					errorMessage = Labels.getString("errordlg-start-failed") + " " + exception.getMessage();
				}
				JOptionPane.showMessageDialog(JSettlersFrame.this, errorMessage, Labels.getString("errordlg-header"), JOptionPane.ERROR_MESSAGE);
				showSharedMainMenu();
			}

			@Override
			public void startFinished() {
			}

			@Override
			public void startingLoadingGame() {
				ImageProvider.getInstance().startPreloading();
			}

			@Override
			public void waitForPreloading() {
				ImageProvider.getInstance().waitForPreloadingFinish();
			}
		});
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
			// game window is visible during the match; it is shown again when a menu is shown.
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
	 * Shows the shared, GL rendered multiplayer picker. The player can host a new game (by picking a fresh map) or
	 * join an existing game on the local server. The same panel code runs on desktop and Android.
	 */
	private void showMultiplayerConnection() {
		closeMultiplayerConnector();
		Logger logger = new ConsoleConsumerLogger("jsettlers-swing-multiplayer", line -> {
		});		currentMultiplayerConnector = new MultiplayerConnector("localhost", SettingsManager.getInstance().getUUID(),
				SettingsManager.getInstance().getUserName(), logger);
		MapList mapList = MapList.getDefaultList();
		MapListProvider freshMaps = () -> mapList.getFreshMaps().getItems().stream()
				.map(loader -> toMapMenuItem(loader, false)).collect(Collectors.toList());
		MultiplayerPickerPanel picker = new MultiplayerPickerPanel(currentMultiplayerConnector, freshMaps,
				map -> {
					MapLoader loader = resolveMap(map);
					if (loader != null) {
						showNewMultiPlayerGameMenu(loader, currentMultiplayerConnector);
					}
				},
				game -> joinMultiplayerGame(currentMultiplayerConnector, game),
				this::showSharedMainMenu);
		showMenuContent(new MenuContent(picker, action -> {
		}));
	}

	private void joinMultiplayerGame(IMultiplayerConnector connector, IJoinableGame game) {
		IJoiningGame joiningGame = connector.joinMultiplayerGame(game);
		joiningGame.setListener(new IJoiningGameListener() {
			@Override
			public void joinProgressChanged(EProgressState state, float progress) {
			}

			@Override
			public void gameJoined(IJoinPhaseMultiplayerGameConnector joinConnector) {
				MapLoader mapLoader = MapList.getDefaultList().getMapById(game.getMap().getMapId());
				if (mapLoader != null) {
					showJoinMultiplayerMenu(joinConnector, mapLoader, connector.getPlayerUUID());
				}
			}
		});
	}

	private void closeMultiplayerConnector() {
		if (currentMultiplayerConnector != null) {
			((jsettlers.network.client.IClientConnection) currentMultiplayerConnector)
					.action(jsettlers.network.client.IClientConnection.EClientAction.CLOSE, null);
			currentMultiplayerConnector = null;
		}
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
