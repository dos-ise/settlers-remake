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

import java.util.List;
import java.util.function.Consumer;

import jsettlers.common.action.Action;
import jsettlers.graphics.action.ExecutableAction;

/**
 * A labeled button that cycles through a list of options on every click and shows the currently selected option.
 * <p>
 * This is the shared replacement for a combo box in the original game style.
 *
 * @author Copilot
 */
public class CycleButton extends LabeledButton {
	private final List<String> options;
	private final Consumer<Integer> onSelect;
	private int index;

	/**
	 * Creates a new cycle button.
	 *
	 * @param options
	 *            The options to cycle through. Must not be empty.
	 * @param initialIndex
	 *            The initially selected option index.
	 * @param onSelect
	 *            Called with the new option index after every change.
	 */
	public CycleButton(List<String> options, int initialIndex, Consumer<Integer> onSelect) {
		super(options.get(initialIndex), null);
		this.options = options;
		this.index = initialIndex;
		this.onSelect = onSelect;
	}

	@Override
	protected String getText() {
		return options.get(index);
	}

	@Override
	public Action getAction() {
		return new ExecutableAction() {
			@Override
			public void execute() {
				index = (index + 1) % options.size();
				if (onSelect != null) {
					onSelect.accept(index);
				}
			}
		};
	}

	/**
	 * Sets the selected option index without firing the callback.
	 *
	 * @param index
	 *            The new index.
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * Gets the currently selected option index.
	 *
	 * @return The index.
	 */
	public int getIndex() {
		return index;
	}
}
