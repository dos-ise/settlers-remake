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

import java.util.Optional;

import go.graphics.GLDrawContext;
import go.graphics.UIPoint;
import go.graphics.event.GOEvent;
import go.graphics.event.GOEventHandler;
import go.graphics.event.GOKeyEvent;
import go.graphics.event.GOModalEventHandler;
import go.graphics.event.command.GOCommandEvent;
import go.graphics.event.mouse.GOHoverEvent;
import go.graphics.region.RegionContent;
import go.graphics.text.EFontSize;
import go.graphics.text.TextDrawer;
import jsettlers.common.action.Action;
import jsettlers.common.action.IAction;
import jsettlers.common.position.FloatRectangle;
import jsettlers.graphics.action.ActionFireable;
import jsettlers.graphics.action.ActionHandler;
import jsettlers.graphics.action.ExecutableAction;
import jsettlers.graphics.font.FontDrawerFactory;
import jsettlers.graphics.map.ReplaceableTextDrawer;
import jsettlers.graphics.ui.UIInput;
import jsettlers.graphics.ui.UIPanel;
import jsettlers.graphics.utils.FocusAction;

/**
 * A {@link RegionContent} that renders a {@link UIPanel} menu tree and translates input events into actions.
 * <p>
 * This is the shared, platform independent host for all menus. It is rendered by the same GL backends as the game
 * itself (Swing {@code AreaContainer} on desktop, {@code GOSurfaceView} on Android), so the exact same menu code runs
 * on both platforms.
 *
 * @author Copilot
 */
public class MenuContent implements RegionContent, ActionFireable {
	private final UIPanel rootPanel;
	private final ActionFireable actionFireable;
	private final ReplaceableTextDrawer textDrawer = new ReplaceableTextDrawer();

	private UIInput focusedInput;
	private int width = 1;
	private int height = 1;
	private UIPoint mousePosition = new UIPoint(0, 0);
	private String tooltipString = "";

	/**
	 * Creates a new menu content.
	 *
	 * @param rootPanel
	 *            The root panel of the menu. It is positioned to fill the whole region.
	 * @param actionFireable
	 *            The target that receives all actions fired by the menu.
	 */
	public MenuContent(UIPanel rootPanel, ActionFireable actionFireable) {
		this.rootPanel = rootPanel;
		this.actionFireable = actionFireable;
	}

	@Override
	public void drawContent(GLDrawContext gl, int newWidth, int newHeight) {
		if (newWidth != width || newHeight != height) {
			width = newWidth;
			height = newHeight;
			rootPanel.setPosition(new FloatRectangle(0, 0, newWidth, newHeight));
		}

		if (textDrawer.getTextDrawer(gl, EFontSize.NORMAL).getWidth("a") == 0) {
			textDrawer.setTextDrawerFactory(new FontDrawerFactory());
		}

		gl.clearDepthBuffer();
		gl.setGlobalAttributes(0, 0, 0, 1, 1, 1);
		rootPanel.drawAt(gl);
		drawTooltip(gl);
	}

	@Override
	public void handleEvent(GOEvent event) {
		if (event instanceof GOCommandEvent) {
			GOCommandEvent commandEvent = (GOCommandEvent) event;
			Optional<Action> action = getActionForCommand(commandEvent);
			event.setHandler(new ActionHandler(action, this));
		} else if (event instanceof GOKeyEvent) {
			GOKeyEvent keyEvent = (GOKeyEvent) event;
			if (focusedInput != null) {
				keyEvent.setHandler(focusedInput);
			}
		} else if (event instanceof GOHoverEvent) {
			GOHoverEvent hoverEvent = (GOHoverEvent) event;
			hoverEvent.setHandler(hoverHandler);
		}
	}

	private Optional<Action> getActionForCommand(GOCommandEvent commandEvent) {
		UIPoint position = commandEvent.getCommandPosition();
		float relativeX = (float) (position.getX() / width);
		float relativeY = (float) (position.getY() / height);
		return rootPanel.getAction(relativeX, relativeY);
	}

	@Override
	public void fireAction(IAction action) {
		if (action instanceof FocusAction) {
			focusedInput = ((FocusAction) action).getInput();
		} else if (action instanceof ExecutableAction) {
			((ExecutableAction) action).execute();
		} else {
			actionFireable.fireAction(action);
		}
	}

	private final GOEventHandler hoverHandler = new GOModalEventHandler() {
		@Override
		public void phaseChanged(GOEvent event) {
		}

		@Override
		public void finished(GOEvent event) {
			changeMousePosition(((GOHoverEvent) event).getHoverPosition());
		}

		@Override
		public void aborted(GOEvent event) {
		}

		@Override
		public void eventDataChanged(GOEvent event) {
			changeMousePosition(((GOHoverEvent) event).getHoverPosition());
		}
	};

	private void changeMousePosition(UIPoint position) {
		mousePosition = position;
		float relativeX = (float) (position.getX() / width);
		float relativeY = (float) (position.getY() / height);
		String description = rootPanel.getDescription(relativeX, relativeY);
		tooltipString = description == null ? "" : description;
	}

	private void drawTooltip(GLDrawContext gl) {
		if (!tooltipString.isEmpty()) {
			TextDrawer drawer = textDrawer.getTextDrawer(gl, EFontSize.NORMAL);
			drawer.drawString((int) mousePosition.getX(), (int) mousePosition.getY(), tooltipString);
		}
	}
}
