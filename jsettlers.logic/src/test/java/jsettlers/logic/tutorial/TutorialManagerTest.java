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
package jsettlers.logic.tutorial;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jsettlers.common.ai.EPlayerType;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.menu.IStartedGame;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.integration.ai.AiTestUtils;
import jsettlers.logic.buildings.Building;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.grid.MainGrid;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.logic.player.InitialGameState;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.main.replay.ReplayUtils;
import jsettlers.network.client.OfflineNetworkConnector;
import jsettlers.testutils.TestUtils;
import jsettlers.testutils.map.MapUtils;

/**
 * Tests the {@link TutorialManager} state machine: the intro task, advancing to the next task, the map move task and
 * the building detection.
 */
public class TutorialManagerTest {
	private static final byte PLAYER = 0;

	private IStartedGame startedGame;
	private MainGrid grid;
	private TutorialManager tutorialManager;

	@Before
	public void startGame() throws Exception {
		TestUtils.setupTempResourceManager();
		MapLoader map = MapUtils.getMountainlake();
		PlayerSetting[] playerSettings = AiTestUtils.getDefaultPlayerSettings(map.getMaxPlayers());
		playerSettings[PLAYER] = new PlayerSetting(EPlayerType.HUMAN, ECivilisation.ROMAN, PLAYER);

		JSettlersGame.GameRunner game = AiTestUtils.createStartingGame(playerSettings, map);
		startedGame = ReplayUtils.waitForGameStartup(game);

		MatchConstants.clock().setPausing(true); // the test thread executes the game
		MatchConstants.clock().fastForwardTo(1000);

		grid = game.getMainGrid();
		tutorialManager = new TutorialManager(grid, PLAYER);
	}

	@After
	public void stopGame() {
		ReplayUtils.awaitShutdown(startedGame);
	}

	@Test
	public void testIntroTaskIsCompleteAndAdvances() {
		tutorialManager.timerEvent();
		assertTrue("intro task should be complete", tutorialManager.isCurrentTaskComplete());
		assertEquals("tutorial-task-0", tutorialManager.getCurrentText());

		tutorialManager.advanceToNextTask();
		assertEquals("tutorial-task-1", tutorialManager.getCurrentText());
		assertFalse("the new task is not complete yet", tutorialManager.isCurrentTaskComplete());
	}

	@Test
	public void testMapMoveCompletesTask() {
		tutorialManager.timerEvent();
		tutorialManager.advanceToNextTask(); // now on the "move the map" task
		assertFalse(tutorialManager.isCurrentTaskComplete());

		tutorialManager.notifyMapMoved();
		tutorialManager.timerEvent();
		assertTrue("map move should complete the task", tutorialManager.isCurrentTaskComplete());
	}

	@Test
	public void testBuildingDetection() throws Exception {
		// advance to the "build a lumberjack" task
		tutorialManager.timerEvent();
		tutorialManager.advanceToNextTask(); // task 1: move the map
		tutorialManager.notifyMapMoved();
		tutorialManager.timerEvent();
		tutorialManager.advanceToNextTask(); // task 2: lumberjack
		assertFalse(tutorialManager.isCurrentTaskComplete());

		ShortPoint2D position = findConstructionPosition(EBuildingType.LUMBERJACK);
		grid.getGuiInputGrid().constructBuildingAt(position, EBuildingType.LUMBERJACK, PLAYER);

		tutorialManager.timerEvent();
		assertFalse("a building under construction must not complete the task",
				tutorialManager.isCurrentTaskComplete());

		// finish the construction
		Building building = Building.getAllBuildings().stream()
				.filter(b -> b.getPlayer().playerId == PLAYER)
				.filter(b -> b.getBuildingVariant().isVariantOf(EBuildingType.LUMBERJACK))
				.filter(b -> !b.isDestroyed())
				.findFirst()
				.orElseThrow(() -> new AssertionError("lumberjack building not found"));
		finishConstruction(building);

		tutorialManager.timerEvent();
		assertTrue("a finished building should complete the task", tutorialManager.isCurrentTaskComplete());
	}

	/**
	 * Sets the private construction state of the building to CONSTRUCTED, as the real construction would do once all
	 * materials are delivered.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void finishConstruction(Building building) throws Exception {
		Field stateField = Building.class.getDeclaredField("state");
		stateField.setAccessible(true);
		Class stateClass = Class.forName("jsettlers.logic.buildings.Building$EBuildingState");
		stateField.set(building, Enum.valueOf(stateClass, "CONSTRUCTED"));
	}

	@Test
	public void testTutorialStateAvailableAfterGameStart() throws Exception {
		MapLoader map = MapUtils.getMountainlake();
		PlayerSetting[] playerSettings = AiTestUtils.getDefaultPlayerSettings(map.getMaxPlayers());
		playerSettings[PLAYER] = new PlayerSetting(EPlayerType.HUMAN, ECivilisation.ROMAN, PLAYER);

		JSettlersGame game = new JSettlersGame(map, new OfflineNetworkConnector(),
				new InitialGameState(PLAYER, playerSettings, 1L));
		game.setTutorial(true);
		JSettlersGame.GameRunner runner = (JSettlersGame.GameRunner) game.start();
		IStartedGame startedGame = ReplayUtils.waitForGameStartup(runner);
		try {
			assertNotNull("the tutorial state must be available as soon as the game is shown",
					startedGame.getTutorialState());
		} finally {
			ReplayUtils.awaitShutdown(startedGame);
		}
	}

	/**
	 * Searches the whole map for a position where the given building can be constructed by the player.
	 */
	private ShortPoint2D findConstructionPosition(EBuildingType type) {
		short width = grid.getGuiInputGrid().getWidth();
		short height = grid.getGuiInputGrid().getHeight();
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				Optional<ShortPoint2D> position = grid.getGuiInputGrid()
						.getConstructablePosition(new ShortPoint2D(x, y), type, PLAYER);
				if (position.isPresent()) {
					return position.get();
				}
			}
		}
		throw new AssertionError("no construction position for " + type + " on the test map");
	}
}
