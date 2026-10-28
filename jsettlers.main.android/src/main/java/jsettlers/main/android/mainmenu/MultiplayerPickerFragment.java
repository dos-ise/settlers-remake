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
import android.widget.FrameLayout;

import go.graphics.android.GOSurfaceView;
import go.graphics.area.Area;
import go.graphics.region.Region;
import jsettlers.common.menu.EProgressState;
import jsettlers.common.menu.IJoinableGame;
import jsettlers.common.menu.IJoinPhaseMultiplayerGameConnector;
import jsettlers.common.menu.IJoiningGame;
import jsettlers.common.menu.IJoiningGameListener;
import jsettlers.common.menu.IMultiplayerConnector;
import jsettlers.common.menu.IMultiplayerListener;
import jsettlers.common.menu.IStartingGame;
import jsettlers.graphics.menu.JoinGameMapInfo;
import jsettlers.graphics.menu.JoinGamePlayerSlotData;
import jsettlers.graphics.menu.MapListProvider;
import jsettlers.graphics.menu.MapMenuItem;
import jsettlers.graphics.menu.MapPreviewImage;
import jsettlers.graphics.menu.MenuContent;
import jsettlers.graphics.menu.MenuImageProvider;
import jsettlers.graphics.menu.MultiplayerJoinGamePanel;
import jsettlers.graphics.menu.MultiplayerPickerPanel;
import jsettlers.graphics.menu.OpenMultiplayerGameInfo;
import jsettlers.graphics.ui.UIPanel;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.logic.map.loading.list.MapList;
import jsettlers.logic.map.loading.newmap.MapFileHeader;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.android.R;
import jsettlers.main.android.core.AndroidPreferences;
import jsettlers.main.android.core.GameStarter;
import jsettlers.main.android.mainmenu.navigation.MainMenuNavigator;

/**
 * Shows the shared, GL rendered multiplayer picker. The player can host a new game (by picking a fresh map) or join an
 * existing game on the connected server. The same panel code runs on desktop and Android.
 */
@EFragment(R.layout.fragment_shared_main_menu)
public class MultiplayerPickerFragment extends Fragment {
	private GameStarter gameStarter;
	private IMultiplayerConnector connector;
	private Region goRegion;

	@ViewById(R.id.frame_layout)
	FrameLayout frameLayout;

	public static Fragment create() {
		return new MultiplayerPickerFragment_();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		gameStarter = (GameStarter) getActivity().getApplication();
	}

	@AfterViews
	void setupMenu() {
		MenuImageProvider.setInstance(new MenuImageProvider(new AndroidMenuImageDecoder()));

		goRegion = new Region(Region.POSITION_CENTER);
		Area goArea = new Area();
		goArea.set(goRegion);

		GOSurfaceView goView = new GOSurfaceView(getActivity(), goArea, () -> new HashSet<>());
		frameLayout.addView(goView);

		showPicker();
	}

	private void showPicker() {
		connector = gameStarter.getMultiPlayerConnector();
		MapList mapList = gameStarter.getMapList();
		MapListProvider freshMaps = () -> mapList.getFreshMaps().getItems().stream()
				.map(loader -> toMapMenuItem(loader)).collect(Collectors.toList());
		showMenu(new MultiplayerPickerPanel(connector, freshMaps, this::hostGame, this::joinGame, this::goBack));
	}

	private void hostGame(MapMenuItem map) {
		MapLoader loader = resolveMap(map);
		if (loader == null) {
			return;
		}
		AndroidPreferences preferences = new AndroidPreferences(getActivity());
		JoinGameMapInfo mapInfo = new JoinGameMapInfo(loader.getMapName(), MapPreviewImage.create(loader.getImage()),
				loader.getMaxPlayers(), toJoinGameSlots(loader.getFileHeader().getPlayerSettings()));
		OpenMultiplayerGameInfo gameInfo = new OpenMultiplayerGameInfo(preferences.getPlayerName(), loader);
		MultiplayerJoinGamePanel panel = MultiplayerJoinGamePanel.createHost(mapInfo, connector.getPlayerUUID(),
				connector, gameInfo, this::showPicker, new IMultiplayerListener() {
					@Override
					public void gameIsStarting(IStartingGame game) {
						gameStarter.setStartingGame(game);
						((MainMenuNavigator) getActivity()).showGame();
					}

					@Override
					public void gameAborted() {
						showPicker();
					}
				});
		showMenu(panel);
	}

	private void joinGame(IJoinableGame game) {
		IJoiningGame joiningGame = connector.joinMultiplayerGame(game);
		joiningGame.setListener(new IJoiningGameListener() {
			@Override
			public void joinProgressChanged(EProgressState state, float progress) {
			}

			@Override
			public void gameJoined(IJoinPhaseMultiplayerGameConnector joinConnector) {
				MapLoader loader = gameStarter.getMapList().getMapById(game.getMap().getMapId());
				if (loader == null) {
					return;
				}
				JoinGameMapInfo mapInfo = new JoinGameMapInfo(loader.getMapName(),
						MapPreviewImage.create(loader.getImage()), loader.getMaxPlayers(),
						toJoinGameSlots(loader.getFileHeader().getPlayerSettings()));
				MultiplayerJoinGamePanel panel = MultiplayerJoinGamePanel.createJoin(mapInfo,
						connector.getPlayerUUID(), joinConnector, MultiplayerPickerFragment.this::showPicker,
						new IMultiplayerListener() {
							@Override
							public void gameIsStarting(IStartingGame game) {
								gameStarter.setStartingGame(game);
								((MainMenuNavigator) getActivity()).showGame();
							}

							@Override
							public void gameAborted() {
								showPicker();
							}
						});
				showMenu(panel);
			}
		});
	}

	private void goBack() {
		((MainMenuNavigator) getActivity()).popToMenuRoot();
	}

	private void showMenu(UIPanel panel) {
		MenuContent menuContent = new MenuContent(panel, action -> {
		});
		goRegion.setContent(menuContent);
	}

	private MapMenuItem toMapMenuItem(MapLoader loader) {
		return new MapMenuItem(loader.getMapId(), loader.getMapName(), loader.getDescription(),
				MapPreviewImage.create(loader.getImage()), loader.getMinPlayers(), loader.getMaxPlayers(),
				loader.getCreationDate(), false);
	}

	private MapLoader resolveMap(MapMenuItem map) {
		return gameStarter.getMapList().getMapById(map.getId());
	}

	private static JoinGamePlayerSlotData[] toJoinGameSlots(PlayerSetting[] playerSettings) {
		JoinGamePlayerSlotData[] slots = new JoinGamePlayerSlotData[playerSettings.length];
		for (int i = 0; i < playerSettings.length; i++) {
			PlayerSetting setting = playerSettings[i];
			slots[i] = new JoinGamePlayerSlotData(setting.getCivilisation(), setting.getPlayerType(), setting.getTeamId());
		}
		return slots;
	}
}
