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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import go.graphics.text.EFontSize;
import jsettlers.common.Color;
import jsettlers.common.images.EImageLinkType;
import jsettlers.common.images.OriginalImageLink;
import jsettlers.graphics.action.ExecutableAction;
import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.settings.ISettings;
import jsettlers.graphics.ui.CycleButton;
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
	private static final int BASE_ROW_COUNT = 9;
	private static final float CONTENT_TOP = 0.84f;
	private static final float CONTENT_BOTTOM = 0.14f;
	private static final OriginalImageLink FRAME = new OriginalImageLink(EImageLinkType.GUI, 3, 324, 0);
	private static final Color TITLE_COLOR = new Color(1, 0.6f, 0, 1);

	/**
	 * The language codes of the available translations, matching the {@code labels_*.properties} files.
	 */
	private static final List<String> LANGUAGE_CODES = Arrays.asList("en", "de", "es", "pl", "ru", "da_DK");

	/**
	 * The native names of the available languages, shown in the language selector.
	 */
	private static final List<String> LANGUAGE_NAMES = Arrays.asList("English", "Deutsch", "Español", "Polski", "Русский", "Dansk");

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
	private final CycleButton languageButton = new CycleButton(LANGUAGE_NAMES, 0, null);
	private final List<String> backendNames;
	private final CycleButton backendButton;
	private final int rowCount;
	private final float rowHeight;

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

		backendNames = settings.getBackendNames();
		rowCount = BASE_ROW_COUNT + (backendNames.isEmpty() ? 0 : 1);
		rowHeight = (CONTENT_TOP - CONTENT_BOTTOM) / rowCount;
		backendButton = backendNames.isEmpty() ? null : new CycleButton(backendNames, 0, null);

		Label title = new Label(Labels.getString("settings-title"), EFontSize.HEADLINE);
		title.setTextColor(TITLE_COLOR);
		title.setBackground(FRAME);
		addChild(title, 0.05f, 0.9f, 0.95f, 0.98f);

		addSettingRow(0, Labels.getString("settings-name"), playerNameInput);
		addSettingRow(1, Labels.getString("settings-volume"), volumeSlider);
		addSettingRow(2, Labels.getString("settings-music-enabled"), musicEnabledToggle);
		addSettingRow(3, Labels.getString("settings-volume-music"), musicVolumeSlider);
		addSettingRow(4, Labels.getString("settings-music-playall"), playAllMusicToggle);
		addSettingRow(5, Labels.getString("settings-fps-limit"), fpsLimitSlider);
		addSettingRow(6, Labels.getString("settings-gui-scale"), guiScaleSlider);
		addSettingRow(7, Labels.getString("settings-ai-tower-focus"), aiTowerFocusToggle);
		addSettingRow(8, Labels.getString("settings-language"), languageButton);
		if (backendButton != null) {
			addSettingRow(9, Labels.getString("settings-backend") + " (" + Labels.getString("settings-backend-restart") + ")", backendButton);
		}

		LabeledButton backButton = new LabeledButton(Labels.getString("settings-back"), new ExecutableAction() {
			@Override
			public void execute() {
				onBack.run();
			}
		});
		addChild(backButton, 0.05f, 0.02f, 0.47f, 0.1f);

		LabeledButton saveButton = new LabeledButton(Labels.getString("settings-ok"), new ExecutableAction() {
			@Override
			public void execute() {
				save();
				onBack.run();
			}
		});
		addChild(saveButton, 0.53f, 0.02f, 0.95f, 0.1f);
	}

	private void addSettingRow(int row, String labelText, UIElement control) {
		float bottom = CONTENT_BOTTOM + (rowCount - 1 - row) * rowHeight;
		float top = bottom + rowHeight;
		Label label = new Label(labelText, EFontSize.NORMAL, Label.EHorizontalAlignment.LEFT);
		label.setBackground(FRAME);
		addChild(label, 0.05f, bottom, 0.5f, top);
		addChild(control, 0.53f, bottom, 0.95f, top);
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
		int languageIndex = LANGUAGE_CODES.indexOf(settings.getLanguage());
		languageButton.setIndex(languageIndex >= 0 ? languageIndex : 0);
		if (backendButton != null) {
			int backendIndex = backendNames.indexOf(settings.getBackendName());
			backendButton.setIndex(backendIndex >= 0 ? backendIndex : 0);
		}
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
		if (backendButton != null) {
			settings.setBackendName(backendNames.get(backendButton.getIndex()));
		}
		String language = LANGUAGE_CODES.get(languageButton.getIndex());
		settings.setLanguage(language);
		Labels.setLocale(parseLocale(language));
	}

	private static Locale parseLocale(String language) {
		String[] parts = language.split("_");
		return parts.length == 2 ? new Locale(parts[0], parts[1]) : new Locale(parts[0]);
	}
}
