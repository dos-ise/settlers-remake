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

import java.util.Optional;

import go.graphics.GLDrawContext;
import jsettlers.common.action.Action;
import jsettlers.common.position.FloatRectangle;
import jsettlers.graphics.image.Image;

/**
 * A UI element that draws a single {@link Image} centered in its position, preserving the aspect ratio of the image.
 * It is not interactive.
 *
 * @author Copilot
 */
public class AspectRatioImagePanel implements UIElement {
	private final Image image;
	private FloatRectangle position;

	public AspectRatioImagePanel(Image image) {
		this.image = image;
	}

	@Override
	public void setPosition(FloatRectangle position) {
		this.position = position;
	}

	@Override
	public void drawAt(GLDrawContext gl) {
		FloatRectangle pos = getPosition();
		float imageWidth = image.getWidth();
		float imageHeight = image.getHeight();
		float scale = Math.min(pos.getWidth() / imageWidth, pos.getHeight() / imageHeight);
		float width = imageWidth * scale;
		float height = imageHeight * scale;
		float x = pos.getCenterX() - width / 2;
		float y = pos.getCenterY() - height / 2;
		image.drawImageAtRect(gl, x, y, width, height, 1);
	}

	@Override
	public Optional<Action> getAction(float relativex, float relativey) {
		return Optional.empty();
	}

	@Override
	public String getDescription(float relativex, float relativey) {
		return null;
	}

	@Override
	public void onAttach() {
	}

	@Override
	public void onDetach() {
	}

	private FloatRectangle getPosition() {
		return position;
	}
}
