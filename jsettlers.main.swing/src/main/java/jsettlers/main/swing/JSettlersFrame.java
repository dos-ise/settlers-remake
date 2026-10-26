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
import go.graphics.swing.sound.SwingSoundPlayer;

import jsettlers.common.CommitInfo;
import jsettlers.common.action.EActionType;
import jsettlers.common.action.SaveAction;
import jsettlers.common.menu.IJoinPhaseMultiplayerGameConnector;
import jsettlers.common.menu.IMapInterfaceConnector;
import jsettlers.common.menu.IMultiplayerConnector;
import jsettlers.common.menu.IStartedGame;
import jsettlers.common.menu.IStartingGame;
import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.map.ETextDrawPosition;
import jsettlers.graphics.map.MapContent;
import jsettlers.graphics.menu.MainMenuActions;
import jsettlers.graphics.menu.MapListProvider;
import jsettlers.graphics.menu.MapMenuItem;
import jsettlers.graphics.menu.MenuContent;
import jsettlers.graphics.menu.MenuImageProvider;
import jsettlers.graphics.menu.SettingsMenuPanel;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.logic.map.loading.list.MapList;
import jsettlers.logic.map.loading.newmap.MapFileHeader;
import jsettlers.logic.map.loading.savegame.SavegameLoader;
import jsettlers.logic.player.InitialGameState;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.main.swing.menu.SwingMenuImageDecoder;
import jsettlers.main.swing.menu.joinpanel.JoinGamePanel;
import jsettlers.main.swing.menu.startinggamemenu.StartingGamePanel;
import jsettlers.main.swing.menu.statspanel.EndgameStatsPanel;
import jsettlers.main.swing.settings.SettingsManager;

/**
 * @author codingberlin
 */
public class JSettlersFrame extends JFrame {
	private static final long serialVersionUID = 2607082717493797224L;

	private final EndgameStatsPanel endgameStatsPanel = new EndgameStatsPanel(this);
	private final StartingGamePanel startingGamePanel = new StartingGamePanel(this);
	private final JoinGamePanel joinGamePanel = new JoinGamePanel(this);
	private final SwingSoundPlayer soundPlayer = new SwingSoundPlayer(SettingsManager.getInstance());

	private Timer redrawTimer;
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
					toogleFullScreenMode();
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
		setVisible(true);

		GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice graphicsDevice = graphicsEnvironment.getDefaultScreenDevice();
		graphicsDevice.setFullScreenWindow(fullScreen ? this : null);
		if(areaContainer != null) areaContainer.notifyResize();
	}

	private void abortRedrawTimerIfPresent() {
		if (redrawTimer != null) {
			redrawTimer.stop();
			redrawTimer = null;
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
			public void exit() {
				exit();
			}
		}, freshMaps, savedMaps);

		MenuContent menuContent = new MenuContent(mainMenuPanel, action -> {
		});
		showMenuContent(menuContent);
	}

	private MapMenuItem toMapMenuItem(MapLoader loader) {
		return new MapMenuItem(loader.getMapId(), loader.getMapName(), loader.getDescription());
	}

	private MapLoader resolveMap(MapMenuItem map) {
		return MapList.getDefaultList().getMapById(map.getId());
	}

	private void showMenuContent(MenuContent menuContent) {
		abortRedrawTimerIfPresent();
		Region region = new Region(Region.POSITION_CENTER);
		region.setContent(menuContent);
		Area area = new Area();
		area.set(region);
		SwingUtilities.invokeLater(() -> {
			setContentPane(areaContainer = new AreaContainer(area, SettingsManager.getInstance().getBackend(),
					SettingsManager.getInstance().isGraphicsDebug(), SettingsManager.getInstance().getGuiScale()));
			revalidate();
			repaint();
			setVisible(true);
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

	public void showStartingGamePanel(IStartingGame startingGame) {
		startingGamePanel.setStartingGame(startingGame);
		setNewContentPane(startingGamePanel);
	}

	private void setNewContentPane(Container newContent) {
		abortRedrawTimerIfPresent();
		// The game runs in its own (GLFW) window, so this menu window is hidden during a match
		// (see setContent). Whenever we switch back to a menu/stats panel, show it again.
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
		Area area = new Area();
		area.set(region);

		int fpsLimit = SettingsManager.getInstance().getFpsLimit();
		if(fpsLimit != 0) {
			redrawTimer = new Timer((int)(1000.0f/fpsLimit), e -> region.requestRedraw());
			redrawTimer.setInitialDelay(0);
			redrawTimer.start();
		}

		SwingUtilities.invokeLater(() -> {
			setContentPane(areaContainer = new AreaContainer(area, SettingsManager.getInstance().getBackend(), SettingsManager.getInstance().isGraphicsDebug(), SettingsManager.getInstance().getGuiScale()));
			areaContainer.updateFPSLimit(fpsLimit);
			revalidate();
			repaint();
			// The game is rendered in its own (GLFW) window. Hide this menu window so only the
			// game window is visible during the match; it is shown again in setNewContentPane.
			setVisible(false);
		});
	}

	public void showNewSinglePlayerGameMenu(MapLoader mapLoader) {
		joinGamePanel.setSinglePlayerMap(mapLoader);
		setNewContentPane(joinGamePanel);
	}

	public void showNewMultiPlayerGameMenu(MapLoader mapLoader, IMultiplayerConnector connector) {
		joinGamePanel.setNewMultiPlayerMap(mapLoader, connector);
		setNewContentPane(joinGamePanel);
	}

	public void showJoinMultiplayerMenu(IJoinPhaseMultiplayerGameConnector joinPhaseMultiplayerGameConnector, MapLoader mapLoader, String playerUUID) {
		joinGamePanel.setJoinMultiPlayerMap(joinPhaseMultiplayerGameConnector, mapLoader, playerUUID);
		setNewContentPane(joinGamePanel);
	}

	public void showEndgameStatistics(IStartedGame game) {
		if(areaContainer != null) {
			areaContainer.disposeAll();
			areaContainer = null;
		}

		endgameStatsPanel.setGame(game);
		setNewContentPane(endgameStatsPanel);
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
		// Desktop save now depends on this Swing prompt listener: a bare SAVE request (SaveAction with null description) is intercepted here to ask the
		// user for a description and re-fired as a committed SaveAction; only the committed one is written by the game logic.
		connector.addListener(action -> {
			if (action.getActionType() != EActionType.SAVE) {
				return;
			}
			boolean needsPrompt = !(action instanceof SaveAction) || ((SaveAction) action).getDescription() == null;
			if (!needsPrompt) {
				return; // already the committed save; let the logic handle it (avoid a prompt loop)
			}
			SwingUtilities.invokeLater(() -> {
				String description = JOptionPane.showInputDialog(this, "Description for this savegame (optional):", "");
				if (description == null) {
					return; // user cancelled -> abort the save
				}
				connector.fireAction(new SaveAction(description));
			});
		});
		return connector;
	}
}
