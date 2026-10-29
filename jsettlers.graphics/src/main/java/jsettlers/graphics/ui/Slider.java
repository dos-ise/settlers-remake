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
import jsettlers.graphics.action.ExecutableAction;
import jsettlers.graphics.map.controls.original.panel.content.BarFill;

/**
 * A slider that lets the user select an integer value from a range. It is drawn as a bar in the original game style
 * with the current value shown next to it. This is the shared replacement for the Swing settings sliders.
 *
 * @author Copilot
 */
public class Slider extends UIPanel {
	private final int min;
	private final int max;
	private final String unit;
	private final String minString;
	private final Consumer<Integer> onChange;
	private final BarFill barFill = new BarFill();
	private final Label valueLabel;
	private int value;

	/**
	 * Creates a new slider.
	 *
	 * @param unit
	 *            The unit appended to the displayed value, e.g. "%" or "fps".
	 * @param min
	 *            The minimum value.
	 * @param max
	 *            The maximum value.
	 * @param minString
	 *            The text shown instead of the value when the slider is at its minimum, or <code>null</code>.
	 * @param onChange
	 *            Called with the new value after every change, or <code>null</code>.
	 */
	public Slider(String unit, int min, int max, String minString, Consumer<Integer> onChange) {
		this.min = min;
		this.max = max;
		this.unit = unit;
		this.minString = minString;
		this.onChange = onChange;
		this.value = min;

		addChild(barFill, 0, 0, 0.72f, 1);
		valueLabel = new Label("", EFontSize.NORMAL, Label.EHorizontalAlignment.LEFT);
		addChild(valueLabel, 0.75f, 0, 1, 1);

		barFill.setAction(new ExecutableAction() {
			@Override
			public void execute() {
				int newValue = Math.round(min + barFill.getBarFillPercentage() * (max - min));
				setValue(newValue);
				if (onChange != null) {
					onChange.accept(newValue);
				}
			}
		});
	}

	/**
	 * Sets the value of the slider without firing the change callback.
	 *
	 * @param value
	 *            The new value, clamped to the slider range.
	 */
	public void setValue(int value) {
		this.value = Math.max(min, Math.min(max, value));
		float percentage = max == min ? 0 : (float) (this.value - min) / (max - min);
		barFill.setBarFill(percentage, percentage);
		valueLabel.setText(formatValue());
	}

	/**
	 * Gets the current value of the slider.
	 *
	 * @return The value.
	 */
	public int getValue() {
		return value;
	}

	private String formatValue() {
		if (value == min && minString != null) {
			return minString;
		}
		return value + unit;
	}
}
