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
import jsettlers.graphics.ui.Label;
import jsettlers.graphics.ui.LabeledButton;
import jsettlers.graphics.ui.UIPanel;

/**
 * The shared game data setup panel. Shown when the original Settlers 3 game data is missing. Lets the
 * user select the installation folder or download the game data from GOG or Ubisoft Connect. The same
 * panel runs on desktop and Android.
 *
 * @author Copilot
 */
public class GameDataPanel extends UIPanel {
	/**
	 * Creates a new game data setup panel.
	 *
	 * @param actions
	 *            The actions the panel triggers on the host application.
	 */
	public GameDataPanel(GameDataActions actions) {
		MenuImageProvider images = MenuImageProvider.getInstance();
		if (images != null) {
			setBackgroundImage(images.getBackground());
		}

		addChild(new Label(Labels.getString("game-data-title"), EFontSize.HEADLINE), 0.1f, 0.85f, 0.9f, 0.98f);
		addChild(new Label(Labels.getString("game-data-explanation"), EFontSize.NORMAL, Label.EHorizontalAlignment.CENTER), 0.1f, 0.6f, 0.9f, 0.8f);

		UIPanel buttonPanel = new UIPanel();
		addChild(buttonPanel, 0.3f, 0.1f, 0.7f, 0.55f);

		addMenuButton(buttonPanel, "game-data-choose-directory", 0f, 0.75f, 1f, 0.95f, actions::chooseDirectory);
		addMenuButton(buttonPanel, "game-data-download-gog", 0f, 0.5f, 1f, 0.7f, actions::downloadFromGog);
		addMenuButton(buttonPanel, "game-data-download-ubisoft", 0f, 0.25f, 1f, 0.45f, actions::downloadFromUbisoft);
		addMenuButton(buttonPanel, "i-need-help-button", 0f, 0f, 1f, 0.2f, actions::openHelp);
	}

	private void addMenuButton(UIPanel panel, String labelKey, float left, float bottom, float right, float top, Runnable action) {
		LabeledButton button = new LabeledButton(Labels.getString(labelKey), new ExecutableAction() {
			@Override
			public void execute() {
				action.run();
			}
		}, EFontSize.HEADLINE);
		panel.addChild(button, left, bottom, right, top);
	}
}
