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

import go.graphics.text.EFontSize;
import jsettlers.graphics.action.ExecutableAction;
import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.settings.ISettings;
import jsettlers.graphics.ui.Label;
import jsettlers.graphics.ui.LabeledButton;
import jsettlers.graphics.ui.Slider;
import jsettlers.graphics.ui.ToggleButton;
import jsettlers.graphics.ui.UIElement;
import jsettlers.graphics.ui.UIInput;
import jsettlers.graphics.ui.UIPanel;

/**
 * The shared settings menu. Shows the common settings (player name, volumes, music, fps limit, gui scale) and lets the
 * player save them or go back. The values are read from and written to a platform specific {@link ISettings}, so the
 * same menu runs on desktop and Android.
 *
 * @author Copilot
 */
public class SettingsMenuPanel extends UIPanel {
	private static final int ROW_COUNT = 8;
	private static final float CONTENT_TOP = 0.88f;
	private static final float CONTENT_BOTTOM = 0.15f;
	private static final float ROW_HEIGHT = (CONTENT_TOP - CONTENT_BOTTOM) / ROW_COUNT;

	private final ISettings settings;
	private final Runnable onBack;
	private final UIInput playerNameInput = new UIInput();
	private final Slider volumeSlider = new Slider("%", 0, 100, null, null);
	private final ToggleButton musicEnabledToggle = new ToggleButton("", false, null);
	private final Slider musicVolumeSlider = new Slider("%", 0, 100, null, null);
	private final ToggleButton playAllMusicToggle = new ToggleButton("", false, null);
	private final Slider fpsLimitSlider = new Slider("fps", 0, 240, "timerless redraw", null);
	private final Slider guiScaleSlider = new Slider("%", 50, 400, "system default", null);
	private final ToggleButton aiTowerFocusToggle = new ToggleButton("", false, null);

	/**
	 * Creates a new settings menu.
	 *
	 * @param settings
	 *            The platform specific settings storage.
	 * @param onBack
	 *            Called when the player leaves the settings (back or save).
	 */
	public SettingsMenuPanel(ISettings settings, Runnable onBack) {
		this.settings = settings;
		this.onBack = onBack;

		MenuImageProvider images = MenuImageProvider.getInstance();
		if (images != null) {
			setBackgroundImage(images.getBackground());
		}

		addChild(new Label(Labels.getString("settings-title"), EFontSize.HEADLINE), 0.1f, 0.92f, 0.9f, 1f);

		addSettingRow(0, "settings-name", playerNameInput);
		addSettingRow(1, "settings-volume", volumeSlider);
		addSettingRow(2, "settings-music-enabled", musicEnabledToggle);
		addSettingRow(3, "settings-volume-music", musicVolumeSlider);
		addSettingRow(4, "settings-music-playall", playAllMusicToggle);
		addSettingRow(5, "settings-fps-limit", fpsLimitSlider);
		addSettingRow(6, "settings-gui-scale", guiScaleSlider);
		addSettingRow(7, "settings-ai-tower-focus", aiTowerFocusToggle);

		LabeledButton backButton = new LabeledButton(Labels.getString("settings-back"), new ExecutableAction() {
			@Override
			public void execute() {
				onBack.run();
			}
		});
		addChild(backButton, 0.1f, 0.02f, 0.45f, 0.1f);

		LabeledButton saveButton = new LabeledButton(Labels.getString("settings-ok"), new ExecutableAction() {
			@Override
			public void execute() {
				save();
				onBack.run();
			}
		});
		addChild(saveButton, 0.5f, 0.02f, 0.9f, 0.1f);
	}

	private void addSettingRow(int row, String labelKey, UIElement control) {
		float bottom = CONTENT_BOTTOM + (ROW_COUNT - 1 - row) * ROW_HEIGHT;
		float top = bottom + ROW_HEIGHT;
		addChild(new Label(Labels.getString(labelKey), EFontSize.NORMAL, Label.EHorizontalAlignment.LEFT), 0.1f, bottom, 0.45f, top);
		addChild(control, 0.5f, bottom, 0.9f, top);
	}

	/**
	 * Reads the current values from the settings storage into the controls. Must be called before the panel is shown.
	 */
	public void initializeValues() {
		playerNameInput.setText(settings.getPlayerName());
		volumeSlider.setValue(Math.round(settings.getVolume() * 100));
		musicEnabledToggle.setChecked(settings.isMusicEnabled());
		musicVolumeSlider.setValue(Math.round(settings.getMusicVolume() * 100));
		playAllMusicToggle.setChecked(settings.isMusicPlayAll());
		fpsLimitSlider.setValue(settings.getFpsLimit());
		guiScaleSlider.setValue(Math.round(settings.getGuiScale() * 100));
		aiTowerFocusToggle.setChecked(settings.getAiTowerFocus());
	}

	private void save() {
		settings.setPlayerName(playerNameInput.getText());
		settings.setVolume(volumeSlider.getValue() / 100f);
		settings.setMusicEnabled(musicEnabledToggle.isChecked());
		settings.setMusicVolume(musicVolumeSlider.getValue() / 100f);
		settings.setMusicPlayAll(playAllMusicToggle.isChecked());
		settings.setFpsLimit(fpsLimitSlider.getValue());
		settings.setGuiScale(guiScaleSlider.getValue() / 100f);
		settings.setAiTowerFocus(aiTowerFocusToggle.isChecked());
	}
}
