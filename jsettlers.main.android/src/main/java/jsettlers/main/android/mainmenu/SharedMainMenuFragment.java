/*
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
 */

package jsettlers.main.android.mainmenu;

import java.util.HashSet;
import java.util.stream.Collectors;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import go.graphics.android.GOSurfaceView;
import go.graphics.area.Area;
import go.graphics.region.Region;
import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.menu.JoinGameMapInfo;
import jsettlers.graphics.menu.JoinGamePanel;
import jsettlers.graphics.menu.JoinGamePlayerSlotData;
import jsettlers.graphics.menu.JoinGameSettings;
import jsettlers.graphics.menu.MainMenuActions;
import jsettlers.graphics.menu.MapListProvider;
import jsettlers.graphics.menu.MapMenuItem;
import jsettlers.graphics.menu.MapPreviewImage;
import jsettlers.graphics.menu.MenuContent;
import jsettlers.graphics.menu.MenuImageProvider;
import jsettlers.graphics.menu.SettingsMenuPanel;
import jsettlers.graphics.ui.UIInput;
import jsettlers.graphics.ui.UIPanel;
import jsettlers.logic.map.loading.EMapStartResources;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.logic.map.loading.list.MapList;
import jsettlers.logic.map.loading.newmap.MapFileHeader;
import jsettlers.logic.player.InitialGameState;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.main.android.R;
import jsettlers.main.android.core.AndroidPreferences;
import jsettlers.main.android.core.GameStarter;
import jsettlers.main.android.mainmenu.navigation.MainMenuNavigator;

/**
 * Shows the shared, GL rendered main menu. The same menu code runs on desktop and Android.
 */
@EFragment(R.layout.fragment_shared_main_menu)
public class SharedMainMenuFragment extends Fragment implements MainMenuActions {
	private GameStarter gameStarter;
	private MapListProvider freshMaps;
	private MapListProvider savedMaps;
	private Region goRegion;

	@ViewById(R.id.frame_layout)
	FrameLayout frameLayout;

	public static Fragment create() {
		return new SharedMainMenuFragment_();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		gameStarter = (GameStarter) getActivity().getApplication();
	}

	@AfterViews
	void setupMenu() {
		MenuImageProvider.setInstance(new MenuImageProvider(new AndroidMenuImageDecoder()));

		MapList mapList = gameStarter.getMapList();
		freshMaps = () -> mapList.getFreshMaps().getItems().stream()
				.map(loader -> toMapMenuItem(loader, false)).collect(Collectors.toList());
		savedMaps = () -> mapList.getSavedMaps().getItems().stream()
				.map(loader -> toMapMenuItem(loader, true)).collect(Collectors.toList());

		goRegion = new Region(Region.POSITION_CENTER);
		Area goArea = new Area();
		goArea.set(goRegion);

		GOSurfaceView goView = new GOSurfaceView(getActivity(), goArea, () -> new HashSet<>());
		frameLayout.addView(goView);

		showMainMenu();
	}

	private void showMainMenu() {
		showMenu(new jsettlers.graphics.menu.MainMenuPanel(this, freshMaps, savedMaps));
	}

	private void showMenu(UIPanel panel) {
		MenuContent menuContent = new MenuContent(panel, action -> {
		});
		menuContent.setInputFocusListener(this::handleInputFocus);
		goRegion.setContent(menuContent);
	}

	/**
	 * Android has no physical keyboard, so a focused text input opens a text entry dialog. The
	 * entered text is written back into the input field.
	 */
	private void handleInputFocus(UIInput input) {
		View view = View.inflate(getActivity(), R.layout.dialog_edit_text, null);
		final EditText editText = view.findViewById(R.id.edit_text);
		editText.setText(input.getText());
		new AlertDialog.Builder(getActivity())
				.setTitle(Labels.getString("general.search"))
				.setView(view)
				.setPositiveButton(android.R.string.ok, (d, which) -> input.setTextAndNotify(editText.getText().toString()))
				.setNegativeButton(android.R.string.cancel, null)
				.create()
				.show();
	}

	private MapMenuItem toMapMenuItem(MapLoader loader, boolean savegame) {
		return new MapMenuItem(loader.getMapId(), loader.getMapName(), loader.getDescription(),
				MapPreviewImage.create(loader.getImage()), loader.getMinPlayers(), loader.getMaxPlayers(),
				loader.getCreationDate(), savegame);
	}

	private MapLoader resolveMap(MapMenuItem map) {
		return gameStarter.getMapList().getMapById(map.getId());
	}

	/**
	 * MainMenuActions implementation
	 */
	@Override
	public void startNewSinglePlayerGame(MapMenuItem map) {
		MapLoader loader = resolveMap(map);
		if (loader == null) {
			return;
		}
		AndroidPreferences preferences = new AndroidPreferences(getActivity());
		JoinGameMapInfo mapInfo = new JoinGameMapInfo(loader.getMapName(), MapPreviewImage.create(loader.getImage()),
				loader.getMaxPlayers(), toJoinGameSlots(loader.getFileHeader().getPlayerSettings()));
		JoinGamePanel joinGamePanel = new JoinGamePanel(mapInfo, preferences.getPlayerName(), settings -> {
			PlayerSetting[] playerSettings = toPlayerSettings(settings, loader.getMaxPlayers());
			InitialGameState initialGameState = new InitialGameState(settings.getHumanPlayerSlot(), playerSettings,
					System.currentTimeMillis(), EMapStartResources.fromMapValue(settings.getStartResourcesValue()),
					settings.getPeaceTime());
			JSettlersGame game = new JSettlersGame(loader, initialGameState);
			gameStarter.setStartingGame(game.start());
			((MainMenuNavigator) getActivity()).showGame();
		}, this::showMainMenu);
		showMenu(joinGamePanel);
	}

	private static JoinGamePlayerSlotData[] toJoinGameSlots(PlayerSetting[] playerSettings) {
		JoinGamePlayerSlotData[] slots = new JoinGamePlayerSlotData[playerSettings.length];
		for (int i = 0; i < playerSettings.length; i++) {
			PlayerSetting setting = playerSettings[i];
			slots[i] = new JoinGamePlayerSlotData(setting.getCivilisation(), setting.getPlayerType(), setting.getTeamId());
		}
		return slots;
	}

	private static PlayerSetting[] toPlayerSettings(JoinGameSettings settings, int maxPlayers) {
		PlayerSetting[] playerSettings = new PlayerSetting[maxPlayers];
		for (int i = 0; i < maxPlayers; i++) {
			JoinGamePlayerSlotData slot = settings.getPlayerSlots()[i];
			if (slot.getPlayerType() != null) {
				playerSettings[i] = new PlayerSetting(slot.getPlayerType(), slot.getCivilisation(), slot.getTeam());
			} else {
				playerSettings[i] = new PlayerSetting();
			}
		}
		return playerSettings;
	}

	@Override
	public void startLoadGame(MapMenuItem map) {
		MapLoader loader = resolveMap(map);
		if (loader != null) {
			MapFileHeader mapFileHeader = loader.getFileHeader();
			PlayerSetting[] playerSettings = mapFileHeader.getPlayerSettings();
			byte playerId = mapFileHeader.getPlayerId();
			InitialGameState initialGameState = new InitialGameState(playerId, playerSettings, 4711);
			JSettlersGame game = new JSettlersGame(loader, initialGameState);
			gameStarter.setStartingGame(game.start());
			((MainMenuNavigator) getActivity()).showGame();
		}
	}

	@Override
	public void deleteSavegame(MapMenuItem map) {
		MapLoader loader = resolveMap(map);
		if (loader != null) {
			loader.getListedMap().delete();
			gameStarter.getMapList().reload();
		}
	}

	@Override
	public void showSettings() {
		AndroidPreferences preferences = new AndroidPreferences(getActivity());
		SettingsMenuPanel settingsPanel = new SettingsMenuPanel(preferences, this::showMainMenu);
		settingsPanel.initializeValues();
		showMenu(settingsPanel);
	}

	@Override
	public void showNewMultiplayerGame() {
		((MainMenuNavigator) getActivity()).showNewMultiPlayerPicker();
	}

	@Override
	public void showJoinMultiplayerGame() {
		((MainMenuNavigator) getActivity()).showJoinMultiPlayerPicker();
	}

	@Override
	public void exit() {
		getActivity().finish();
	}
}
