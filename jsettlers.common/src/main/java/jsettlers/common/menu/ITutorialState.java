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
package jsettlers.common.menu;

/**
 * The state of the tutorial that is currently running in a game, or <code>null</code> if the game is not a tutorial.
 * <p>
 * The tutorial logic (in the game logic) tracks the current task and whether it is completed. The UI reads this
 * interface to display the task text and the arrow that advances to the next task.
 *
 * @author Copilot
 */
public interface ITutorialState {
	/**
	 * @return The label key of the current tutorial task text. The UI resolves it with
	 *         {@code Labels.getString(key)} so the text can be localized.
	 */
	String getCurrentText();

	/**
	 * @return <code>true</code> if the current task has been completed and the arrow to the next task is available.
	 */
	boolean isCurrentTaskComplete();

	/**
	 * Advances to the next tutorial task. Called when the player clicks the arrow.
	 */
	void advanceToNextTask();

	/**
	 * @return <code>true</code> if the tutorial has been finished.
	 */
	boolean isFinished();

	/**
	 * Called by the UI when the player moved the map (used by the "move the map" task).
	 */
	void notifyMapMoved();
}
