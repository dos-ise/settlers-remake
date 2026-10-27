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

/**
 * The actions the shared main menu can trigger on the host application.
 * <p>
 * The host application (desktop or Android) implements this interface to react to the menu buttons.
 *
 * @author Copilot
 */
public interface MainMenuActions {
	/**
	 * Starts a new single player game on the given map.
	 *
	 * @param map
	 *            The selected map.
	 */
	void startNewSinglePlayerGame(MapMenuItem map);

	/**
	 * Loads the given savegame.
	 *
	 * @param map
	 *            The selected savegame.
	 */
	void startLoadGame(MapMenuItem map);

	/**
	 * Deletes the given savegame from disk.
	 *
	 * @param map
	 *            The savegame to delete.
	 */
	void deleteSavegame(MapMenuItem map);

	/**
	 * Shows the settings.
	 */
	void showSettings();

	/**
	 * Shows the multiplayer setup to host a new network game.
	 */
	void showNewMultiplayerGame();

	/**
	 * Shows the multiplayer setup to join a network game.
	 */
	void showJoinMultiplayerGame();

	/**
	 * Exits the application.
	 */
	void exit();
}
