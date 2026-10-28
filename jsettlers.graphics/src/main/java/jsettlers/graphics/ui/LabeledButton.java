/*******************************************************************************
 * Copyright (c) 2015
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

import java.util.Optional;

import go.graphics.AbstractColor;
import go.graphics.GLDrawContext;
import go.graphics.text.EFontSize;
import go.graphics.text.TextDrawer;
import jsettlers.common.images.EImageLinkType;
import jsettlers.common.images.OriginalImageLink;
import jsettlers.common.action.Action;

/**
 * This is a special button that has a label on it.
 * 
 * @author Michael Zangl
 */
public class LabeledButton extends Button {
	private String text;
	private final Action action;

	private static final OriginalImageLink BUTTON = new OriginalImageLink(EImageLinkType.GUI,
			3, 324, 0);
	private static final OriginalImageLink BUTTON_ACTIVE = new OriginalImageLink(
			EImageLinkType.GUI, 3, 327, 0);

	private final EFontSize size;
	private final AbstractColor textColor;
	private boolean enabled = true;

	public LabeledButton(String text, Action action, EFontSize size) {
		this(text, action, size, null);
	}

	/**
	 * Creates a new labeled button.
	 *
	 * @param text
	 *            The text to show on the button.
	 * @param action
	 *            The action to fire when the button is pressed.
	 * @param size
	 *            The font size of the text.
	 * @param textColor
	 *            The color of the text, or <code>null</code> to use the default color.
	 */
	public LabeledButton(String text, Action action, EFontSize size, AbstractColor textColor) {
		super(action, BUTTON, BUTTON_ACTIVE, null);
		this.size = size;
		this.text = text;
		this.action = action;
		this.textColor = textColor;
	}

	public LabeledButton(String text, Action action) {
		this(text, action, EFontSize.NORMAL);
	}

	@Override
	public void drawAt(GLDrawContext gl) {
		super.drawAt(gl);

		TextDrawer drawer = gl.getTextDrawer(size);
		float cx = getPosition().getCenterX();
		float cy = getPosition().getCenterY();
		if (textColor != null) {
			drawer.drawString(cx - drawer.getWidth(text) / 2, cy - drawer.getHeight(text) / 2, textColor, text);
		} else {
			drawer.renderCentered(cx, cy, getText());
		}
	}

	protected String getText() {
		return text;
	}

	/**
	 * Changes the text shown on the button.
	 *
	 * @param text
	 *            The new text.
	 */
	public void setText(String text) {
		this.text = text;
	}
	
	@Override
	public Optional<Action> getAction(float relativex, float relativey) {
		return enabled ? super.getAction(relativex, relativey) : Optional.empty();
	}

	/**
	 * Enables or disables the button. A disabled button won't send any actions.
	 * 
	 * @param enabled
	 *            <code>true</code> to enable the button to send actions (default).
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}
}
