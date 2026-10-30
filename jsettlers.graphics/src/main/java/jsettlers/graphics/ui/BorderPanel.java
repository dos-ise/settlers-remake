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

		// Geometry mirrors the desktop BorderDrawer. Vertical positions below are measured downwards from the top edge.
		float pad = inset * height;
		float t = thickness * height;
		float corner = cornerSize * height;
		float half = corner / 2;

		// top and bottom edge (the corners are attached by the side pieces)
		drawHorizontalSegment(gl, x1 + pad + half, x2 - pad - half, y2 - pad, t);
		drawHorizontalSegment(gl, x1 + pad + half, x2 - pad - half, y1 + pad, t);

		drawFrameSide(gl, x1 + pad, false, y2, pad, height, corner, t);
		drawFrameSide(gl, x2 - pad, true, y2, pad, height, corner, t);

		if (dividerX >= 0) {
			drawFrameSide(gl, x1 + dividerX * width, true, y2, pad, height, corner, t);
		}
	}

	/**
	 * Draws one vertical side of the frame including its two corner hooks, like BorderDrawer.drawVertical0.
	 *
	 * @param x
	 *            x position of the vertical line
	 * @param right
	 *            whether the hooks extend to the left (right side of a panel) or to the right (left side)
	 * @param top
	 *            GL y coordinate of the top edge of the panel
	 */
	private void drawFrameSide(GLDrawContext gl, float x, boolean right, float top, float pad, float height, float corner, float t) {
		float half = corner / 2;
		float far = right ? x - corner : x + corner;
		float near = x;
		float mid = right ? x - half : x + half;
		float lo = Math.min(near, far);
		float hi = Math.max(near, far);

		float yA = pad + half; // upper corner line, measured from the top
		float yB = height - pad - half; // lower corner line, measured from the top

		drawVerticalSegment(gl, x, yA, yB, top, t);
		// upper corner
		drawVerticalSegment(gl, mid, pad, pad + corner, top, t);
		drawHorizontalSegment(gl, lo, hi, top - yA, t);
		drawVerticalSegment(gl, far, pad - half, pad + half, top, t);
		// lower corner
		drawVerticalSegment(gl, mid, yB - half, yB + half, top, t);
		drawHorizontalSegment(gl, lo, hi, top - yB, t);
		drawVerticalSegment(gl, far, yB, yB + corner, top, t);
	}

	/**
	 * Draws a horizontal line segment, centered on the given GL y coordinate.
	 */
	private void drawHorizontalSegment(GLDrawContext gl, float xFrom, float xTo, float y, float t) {
		float length = Math.abs(xTo - xFrom);
		if (length > 0) {
			texture.drawImageAtRect(gl, Math.min(xFrom, xTo), y - t / 2, length, t, 1);
		}
	}

	/**
	 * Draws a vertical line segment centered on x, between two distances measured downwards from the top edge.
	 */
	private void drawVerticalSegment(GLDrawContext gl, float x, float fromTop, float toTop, float top, float t) {
		float length = Math.abs(toTop - fromTop);
		if (length > 0) {
			float bottom = top - Math.max(fromTop, toTop);
			texture.drawImageAtRect(gl, x - t / 2, bottom, t, length, 1);
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
