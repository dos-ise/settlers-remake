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

import go.graphics.GLDrawContext;
import java.util.Optional;
import jsettlers.common.position.FloatRectangle;
import jsettlers.common.action.Action;

public interface UIElement {
	void setPosition(FloatRectangle position);

	/**
	 * Draws the element at the given position.
	 * 
	 * @param gl
	 */
	void drawAt(GLDrawContext gl);

	Optional<Action> getAction(float relativex, float relativey);

	String getDescription(float relativex, float relativey);

	/**
	 * Called once to indicate that this element is not attached to the gui and could be visible.
	 */
	void onAttach();

	/**
	 * The opposite of {@link #onAttach()}.
	 */
	void onDetach();

	/**
	 * Handles a zoom event (usually created by the mouse wheel) at the given relative position.
	 * <p>
	 * Menus use this to scroll lists. The default implementation does nothing.
	 *
	 * @param relativex
	 *            The relative x position of the event (0..1).
	 * @param relativey
	 *            The relative y position of the event (0..1).
	 * @param zoomFactor
	 *            The zoom factor, 1 means no zoom, values &gt; 1 zoom in, values &lt; 1 zoom out.
	 */
	default void handleZoom(float relativex, float relativey, float zoomFactor) {
	}

	/**
	 * Handles a pan (drag) event at the given relative position.
	 * <p>
	 * Menus use this to scroll lists with touch. The pan distance is the cumulative distance from the start of the
	 * pan. The default implementation does nothing.
	 *
	 * @param relativex
	 *            The relative x position of the pan start (0..1).
	 * @param relativey
	 *            The relative y position of the pan start (0..1).
	 * @param panDistanceX
	 *            The cumulative x distance of the pan in pixels.
	 * @param panDistanceY
	 *            The cumulative y distance of the pan in pixels.
	 */
	default void handlePan(float relativex, float relativey, float panDistanceX, float panDistanceY) {
	}

	/**
	 * Called when a pan (drag) gesture starts at the given relative position.
	 *
	 * @param relativex
	 *            The relative x position of the pan start (0..1).
	 * @param relativey
	 *            The relative y position of the pan start (0..1).
	 */
	default void handlePanStart(float relativex, float relativey) {
	}

	/**
	 * Called when a pan (drag) gesture ends at the given relative position.
	 *
	 * @param relativex
	 *            The relative x position of the pan start (0..1).
	 * @param relativey
	 *            The relative y position of the pan start (0..1).
	 */
	default void handlePanEnd(float relativex, float relativey) {
	}
}
