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
package jsettlers.graphics.ui;

import java.util.function.Consumer;

import go.graphics.text.EFontSize;
import jsettlers.common.Color;
import jsettlers.graphics.action.ExecutableAction;
import jsettlers.graphics.image.SingleImage;
import jsettlers.graphics.image.reader.ImageMetadata;
import jsettlers.graphics.localization.Labels;

/**
 * A modal text input dialog rendered by the shared GL UI. It dims the whole screen and asks the user
 * for a single line of text. The entered text (or the cancellation) is reported through callbacks.
 * <p>
 * The dialog is shown as an overlay on top of the game by {@code MapContent}. Text entry works through
 * the focused {@link UIInput}: on the desktop the keyboard writes directly into it, on Android the host
 * opens its text entry dialog when the input is focused.
 *
 * @author Copilot
 */
public class TextInputDialog extends UIPanel {
	private final UIInput input;

	/**
	 * Creates a new text input dialog.
	 *
	 * @param title
	 *            The title shown above the input field.
	 * @param onEntered
	 *            Called with the entered text when the player confirms. The text may be empty.
	 * @param onCancel
	 *            Called when the player cancels the dialog.
	 */
	public TextInputDialog(String title, Consumer<String> onEntered, Runnable onCancel) {
		// dim the whole screen behind the dialog
		ImageMetadata metadata = new ImageMetadata();
		metadata.width = 1;
		metadata.height = 1;
		metadata.offsetX = 0;
		metadata.offsetY = 0;
		int[] rgba = new int[] { new Color(0, 0, 0, 0.6f).getRGBA() };
		setBackgroundImage(new SingleImage(metadata, rgba, "dialog-overlay"));

		UIPanel box = new UIPanel();
		addChild(box, 0.25f, 0.35f, 0.75f, 0.65f);

		box.addChild(new Label(title, EFontSize.HEADLINE), 0.05f, 0.7f, 0.95f, 0.9f);

		input = new UIInput();
		box.addChild(input, 0.05f, 0.5f, 0.95f, 0.62f);

		box.addChild(new LabeledButton(Labels.getString("settings-ok"), new ExecutableAction() {
			@Override
			public void execute() {
				onEntered.accept(input.getText());
			}
		}), 0.05f, 0.1f, 0.5f, 0.3f);

		box.addChild(new LabeledButton(Labels.getString("abort"), new ExecutableAction() {
			@Override
			public void execute() {
				onCancel.run();
			}
		}), 0.5f, 0.1f, 0.95f, 0.3f);
	}

	/**
	 * Gets the text input field of this dialog. The host focuses it so that keyboard input (or the
	 * platform text entry dialog) is routed into it.
	 *
	 * @return The input field.
	 */
	public UIInput getInput() {
		return input;
	}
}
