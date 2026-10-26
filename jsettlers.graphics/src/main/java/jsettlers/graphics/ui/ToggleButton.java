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

import jsettlers.common.action.Action;
import jsettlers.graphics.action.ExecutableAction;

/**
 * A labeled button that toggles between a checked and an unchecked state on every click.
 * <p>
 * The checked state is reflected by the active button image. This is the shared replacement for a checkbox.
 *
 * @author Copilot
 */
public class ToggleButton extends LabeledButton {
	private final Consumer<Boolean> onToggle;
	private boolean checked;

	/**
	 * Creates a new toggle button.
	 *
	 * @param text
	 *            The label of the button.
	 * @param checked
	 *            The initial checked state.
	 * @param onToggle
	 *            Called with the new state after every toggle.
	 */
	public ToggleButton(String text, boolean checked, Consumer<Boolean> onToggle) {
		super(text, null);
		this.checked = checked;
		this.onToggle = onToggle;
	}

	@Override
	public boolean isActive() {
		return checked;
	}

	@Override
	public Action getAction() {
		return new ExecutableAction() {
			@Override
			public void execute() {
				checked = !checked;
				onToggle.accept(checked);
			}
		};
	}

	/**
	 * Sets the checked state without firing the toggle callback.
	 *
	 * @param checked
	 *            The new state.
	 */
	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	/**
	 * Gets the current checked state.
	 *
	 * @return <code>true</code> if the button is checked.
	 */
	public boolean isChecked() {
		return checked;
	}
}
