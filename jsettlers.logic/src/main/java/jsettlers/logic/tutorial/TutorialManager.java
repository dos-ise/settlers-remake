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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.menu.ITutorialState;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.EWinState;
import jsettlers.logic.buildings.Building;
import jsettlers.logic.map.grid.MainGrid;
import jsettlers.logic.player.Player;
import jsettlers.network.synchronic.timer.INetworkTimerable;

/**
 * Tracks the progress of the tutorial mission. It is ticked by the game clock and checks the game state (buildings,
 * movables, win state) to decide when the current task is completed. The UI reads the state through
 * {@link ITutorialState} and shows the task text and the arrow to the next task.
 * <p>
 * The manager is a pure observer: it never changes the simulation, so it does not affect determinism.
 *
 * @author Copilot
 */
public class TutorialManager implements INetworkTimerable, ITutorialState {
	private final MainGrid mainGrid;
	private final byte playerId;
	private final List<TutorialTask> tasks = new ArrayList<>();
	private final Map<EBuildingType, Integer> initialBuildingCounts = new EnumMap<>(EBuildingType.class);

	private volatile int currentTask = 0;
	private volatile boolean currentTaskComplete = false;
	private volatile boolean mapMoved = false;

	public TutorialManager(MainGrid mainGrid, byte playerId) {
		this.mainGrid = mainGrid;
		this.playerId = playerId;
		recordInitialBuildingCounts();
		createTasks();
	}

	private void recordInitialBuildingCounts() {
		for (EBuildingType type : EBuildingType.VALUES) {
			initialBuildingCounts.put(type, countBuildings(type));
		}
	}

	private void createTasks() {
		tasks.add(new TutorialTask("tutorial-task-0", () -> true));
		tasks.add(new TutorialTask("tutorial-task-1", () -> mapMoved));
		tasks.add(new TutorialTask("tutorial-task-2", () -> hasBuilt(EBuildingType.LUMBERJACK, 1)));
		tasks.add(new TutorialTask("tutorial-task-3", () -> hasBuilt(EBuildingType.SAWMILL, 1)));
		tasks.add(new TutorialTask("tutorial-task-4", () -> hasBuilt(EBuildingType.STONECUTTER, 1)));
		tasks.add(new TutorialTask("tutorial-task-5", () -> hasBuilt(EBuildingType.TOWER, 3)));
		tasks.add(new TutorialTask("tutorial-task-6",
				() -> hasBuilt(EBuildingType.SMALL_LIVINGHOUSE, 1) && hasBuilt(EBuildingType.FORESTER, 1)
						&& hasBuilt(EBuildingType.LUMBERJACK, 2)));
		tasks.add(new TutorialTask("tutorial-task-7", () -> getMovableCount(EMovableType.GEOLOGIST) >= 5));
		tasks.add(new TutorialTask("tutorial-task-8",
				() -> hasBuilt(EBuildingType.IRONMELT, 1) && hasBuilt(EBuildingType.TOOLSMITH, 1)
						&& hasBuilt(EBuildingType.WEAPONSMITH, 1)));
		tasks.add(new TutorialTask("tutorial-task-9",
				() -> hasBuilt(EBuildingType.FARM, 1) && hasBuilt(EBuildingType.PIG_FARM, 1)
						&& hasBuilt(EBuildingType.SLAUGHTERHOUSE, 1) && hasBuilt(EBuildingType.WATERWORKS, 1)));
		tasks.add(new TutorialTask("tutorial-task-10",
				() -> hasBuilt(EBuildingType.FARM, 2) && hasBuilt(EBuildingType.MILL, 1) && hasBuilt(EBuildingType.BAKER, 1)));
		tasks.add(new TutorialTask("tutorial-task-11",
				() -> hasBuilt(EBuildingType.BARRACK, 1) && hasBuilt(EBuildingType.MEDIUM_LIVINGHOUSE, 1)));
		tasks.add(new TutorialTask("tutorial-task-12", () -> getMovableCount(EMovableType.THIEF) >= 1));
		tasks.add(new TutorialTask("tutorial-task-13", () -> {
			Player player = getPlayer();
			return player != null && player.getWinState() == EWinState.WON;
		}));
		tasks.add(new TutorialTask("tutorial-task-14", () -> true));
	}

	@Override
	public void timerEvent() {
		if (currentTask < tasks.size()) {
			currentTaskComplete = tasks.get(currentTask).isComplete();
		}
	}

	@Override
	public String getCurrentText() {
		if (currentTask >= tasks.size()) {
			return "";
		}
		return tasks.get(currentTask).getTextKey();
	}

	@Override
	public boolean isCurrentTaskComplete() {
		return currentTaskComplete;
	}

	@Override
	public void advanceToNextTask() {
		if (currentTaskComplete && currentTask < tasks.size() - 1) {
			currentTask++;
			currentTaskComplete = false;
		}
	}

	@Override
	public boolean isFinished() {
		return currentTask >= tasks.size() - 1;
	}

	@Override
	public void notifyMapMoved() {
		mapMoved = true;
	}

	private boolean hasBuilt(EBuildingType type, int count) {
		return countBuildings(type) - initialBuildingCounts.getOrDefault(type, 0) >= count;
	}

	private int countBuildings(EBuildingType type) {
		return (int) Building.getAllBuildings().stream()
				.filter(building -> building.getPlayer().playerId == playerId)
				.filter(building -> building.getBuildingVariant().isVariantOf(type))
				.filter(building -> !building.isDestroyed())
				.count();
	}

	private int getMovableCount(EMovableType type) {
		Player player = getPlayer();
		return player == null ? 0 : player.getSettlerInformation().getMovableCount(type);
	}

	private Player getPlayer() {
		return mainGrid.getPartitionsGrid().getPlayer(playerId);
	}

	/**
	 * A single tutorial task: the label key of its text and the condition that marks it as completed.
	 */
	private static final class TutorialTask {
		private final String textKey;
		private final BooleanSupplier completionCondition;

		TutorialTask(String textKey, BooleanSupplier completionCondition) {
			this.textKey = textKey;
			this.completionCondition = completionCondition;
		}

		String getTextKey() {
			return textKey;
		}

		boolean isComplete() {
			return completionCondition.getAsBoolean();
		}
	}
}
