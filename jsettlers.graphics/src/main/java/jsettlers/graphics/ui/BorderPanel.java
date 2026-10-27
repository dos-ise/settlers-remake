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
 * Draws a decorative border frame from a texture, like the gold-yellow frame of the old desktop menu. The frame is drawn
 * in front of the background but behind the leaves. It consists of four edge lines, corner decorations and an optional
 * vertical divider line.
 *
 * @author Copilot
 */
public class BorderPanel implements UIElement {
	private final Image texture;
	private FloatRectangle position;

	/**
	 * Distance of the frame from the panel edges, relative to the panel height.
	 */
	private final float inset;

	/**
	 * Thickness of the frame lines, relative to the panel height.
	 */
	private final float thickness;

	/**
	 * Size of the corner decorations, relative to the panel height.
	 */
	private final float cornerSize;

	/**
	 * Relative x position of an additional vertical divider line, or {@code -1} if none should be drawn.
	 */
	private final float dividerX;

	/**
	 * Creates a new border panel with the default proportions of the old desktop menu.
	 *
	 * @param texture
	 *            The border texture to draw.
	 * @param dividerX
	 *            Relative x position of a vertical divider line, or {@code -1} for none.
	 */
	public BorderPanel(Image texture, float dividerX) {
		this(texture, 0.026f, 0.004f, 0.034f, dividerX);
	}

	/**
	 * Creates a new border panel.
	 *
	 * @param texture
	 *            The border texture to draw.
	 * @param inset
	 *            Distance of the frame from the panel edges, relative to the panel height.
	 * @param thickness
	 *            Thickness of the frame lines, relative to the panel height.
	 * @param cornerSize
	 *            Size of the corner decorations, relative to the panel height.
	 * @param dividerX
	 *            Relative x position of a vertical divider line, or {@code -1} for none.
	 */
	public BorderPanel(Image texture, float inset, float thickness, float cornerSize, float dividerX) {
		this.texture = texture;
		this.inset = inset;
		this.thickness = thickness;
		this.cornerSize = cornerSize;
		this.dividerX = dividerX;
	}

	@Override
	public void setPosition(FloatRectangle position) {
		this.position = position;
	}

	@Override
	public void drawAt(GLDrawContext gl) {
		FloatRectangle pos = getPosition();
		float x1 = pos.getMinX();
		float y1 = pos.getMinY();
		float x2 = pos.getMaxX();
		float y2 = pos.getMaxY();
		float width = pos.getWidth();
		float height = pos.getHeight();

		float in = inset * height;
		float t = thickness * height;
		float clen = cornerSize * height;
		float cl = clen / 2;

		// edge lines
		texture.drawImageAtRect(gl, x1 + in, y2 - in - t, width - 2 * in, t, 1); // top
		texture.drawImageAtRect(gl, x1 + in, y1 + in, width - 2 * in, t, 1); // bottom
		texture.drawImageAtRect(gl, x1 + in, y1 + in + t, t, height - 2 * in - 2 * t, 1); // left
		texture.drawImageAtRect(gl, x2 - in - t, y1 + in + t, t, height - 2 * in - 2 * t, 1); // right

		// corner decorations: L-shaped hooks like the original BorderDrawer
		// top-left
		drawLine(gl, x1 + in + cl, y2 - in - clen, x1 + in + cl, y2 - in, t);
		drawLine(gl, x1 + in, y2 - in - cl, x1 + in + clen, y2 - in - cl, t);
		drawLine(gl, x1 + in + clen, y2 - in - cl, x1 + in + clen, y2 - in + cl, t);
		// top-right
		drawLine(gl, x2 - in - cl, y2 - in - clen, x2 - in - cl, y2 - in, t);
		drawLine(gl, x2 - in - clen, y2 - in - cl, x2 - in, y2 - in - cl, t);
		drawLine(gl, x2 - in - clen, y2 - in - cl, x2 - in - clen, y2 - in + cl, t);
		// bottom-left
		drawLine(gl, x1 + in + cl, y1 + in, x1 + in + cl, y1 + in + clen, t);
		drawLine(gl, x1 + in, y1 + in + cl, x1 + in + clen, y1 + in + cl, t);
		drawLine(gl, x1 + in + clen, y1 + in - cl, x1 + in + clen, y1 + in + cl, t);
		// bottom-right
		drawLine(gl, x2 - in - cl, y1 + in, x2 - in - cl, y1 + in + clen, t);
		drawLine(gl, x2 - in - clen, y1 + in + cl, x2 - in, y1 + in + cl, t);
		drawLine(gl, x2 - in - clen, y1 + in - cl, x2 - in - clen, y1 + in + cl, t);

		// vertical divider
		if (dividerX >= 0) {
			float dx = x1 + dividerX * width;
			texture.drawImageAtRect(gl, dx - t / 2, y1 + in + t, t, height - 2 * in - 2 * t, 1);
		}
	}

	/**
	 * Draws a thin horizontal or vertical line segment with the border texture.
	 */
	private void drawLine(GLDrawContext gl, float x1, float y1, float x2, float y2, float t) {
		if (Math.abs(x2 - x1) < Math.abs(y2 - y1)) {
			// vertical
			texture.drawImageAtRect(gl, x1 - t / 2, Math.min(y1, y2), t, Math.abs(y2 - y1), 1);
		} else {
			// horizontal
			texture.drawImageAtRect(gl, Math.min(x1, x2), y1 - t / 2, Math.abs(x2 - x1), t, 1);
		}
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
