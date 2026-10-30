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
package jsettlers.graphics.settings;

import java.util.Collections;
import java.util.List;

/**
 * The settings that are shared between the desktop and Android versions of the game.
 * <p>
 * The platform specific settings storage (e.g. {@code SettingsManager} on desktop, {@code AndroidPreferences} on
 * Android) implements this interface so the shared settings menu can read and write the same values on both platforms.
 *
 * @author Copilot
 */
public interface ISettings {
	/**
	 * @return The name of the player.
	 */
	String getPlayerName();

	/**
	 * Sets the name of the player.
	 *
	 * @param name
	 *            The new name.
	 */
	void setPlayerName(String name);

	/**
	 * @return The volume of the sound effects (0..1).
	 */
	float getVolume();

	/**
	 * Sets the volume of the sound effects.
	 *
	 * @param volume
	 *            The new volume (0..1).
	 */
	void setVolume(float volume);

	/**
	 * @return <code>true</code> if music is enabled.
	 */
	boolean isMusicEnabled();

	/**
	 * Enables or disables music.
	 *
	 * @param enabled
	 *            The new state.
	 */
	void setMusicEnabled(boolean enabled);

	/**
	 * @return The volume of the music (0..1).
	 */
	float getMusicVolume();

	/**
	 * Sets the volume of the music.
	 *
	 * @param volume
	 *            The new volume (0..1).
	 */
	void setMusicVolume(float volume);

	/**
	 * @return <code>true</code> if all music tracks are played.
	 */
	boolean isMusicPlayAll();

	/**
	 * Sets whether all music tracks are played.
	 *
	 * @param playAll
	 *            The new state.
	 */
	void setMusicPlayAll(boolean playAll);

	/**
	 * @return The maximum frames per second (0 = unlimited).
	 */
	int getFpsLimit();

	/**
	 * Sets the maximum frames per second.
	 *
	 * @param fps
	 *            The new limit (0 = unlimited).
	 */
	void setFpsLimit(int fps);

	/**
	 * @return The GUI scale factor.
	 */
	float getGuiScale();

	/**
	 * Sets the GUI scale factor.
	 *
	 * @param scale
	 *            The new scale factor.
	 */
	void setGuiScale(float scale);

	/**
	 * @return <code>true</code> if the AI focuses on towers.
	 */
	boolean getAiTowerFocus();

	/**
	 * Sets whether the AI focuses on towers.
	 *
	 * @param focus
	 *            The new state.
	 */
	void setAiTowerFocus(boolean focus);

	/**
	 * @return The language code of the UI, e.g. {@code "en"}, {@code "de"} or {@code "da_DK"}.
	 */
	String getLanguage();

	/**
	 * Sets the language code of the UI.
	 *
	 * @param language
	 *            The new language code, e.g. {@code "en"}, {@code "de"} or {@code "da_DK"}.
	 */
	void setLanguage(String language);

	/**
	 * @return The names of the selectable drawing backends. Empty if the platform has no backend choice (e.g. Android).
	 */
	default List<String> getBackendNames() {
		return Collections.emptyList();
	}

	/**
	 * @return The name of the selected drawing backend.
	 */
	default String getBackendName() {
		return "";
	}

	/**
	 * Sets the drawing backend. Takes effect after a restart.
	 *
	 * @param name
	 *            The name of the backend, one of {@link #getBackendNames()}.
	 */
	default void setBackendName(String name) {
	}
}
