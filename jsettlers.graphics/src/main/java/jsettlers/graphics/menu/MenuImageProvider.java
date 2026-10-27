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
package jsettlers.graphics.menu;

import jsettlers.graphics.image.Image;

/**
 * Provides the shared menu images (background, decorative leaves) that are used by the platform independent menus. The images are decoded lazily by a
 * platform specific {@link MenuImageDecoder}.
 */
public class MenuImageProvider {
	private static MenuImageProvider instance;

	private final MenuImageDecoder decoder;

	private Image background;
	private Image leavesLeft;
	private Image leavesRight;
	private Image icon;
	private Image border;

	public MenuImageProvider(MenuImageDecoder decoder) {
		this.decoder = decoder;
	}

	/**
	 * Sets the provider used by the shared menus. Must be called once at startup on each platform.
	 */
	public static void setInstance(MenuImageProvider provider) {
		instance = provider;
	}

	/**
	 * @return The current provider, or {@code null} if none was set.
	 */
	public static MenuImageProvider getInstance() {
		return instance;
	}

	/**
	 * @return The main menu background image.
	 */
	public Image getBackground() {
		if (background == null) {
			background = decoder.decode("jsettlers/graphics/menu/ui_background.jpg");
		}
		return background;
	}

	/**
	 * @return The decorative leaves image for the left side.
	 */
	public Image getLeavesLeft() {
		if (leavesLeft == null) {
			leavesLeft = decoder.decode("jsettlers/graphics/menu/ui_leafs-left.png");
		}
		return leavesLeft;
	}

	/**
	 * @return The decorative leaves image for the right side.
	 */
	public Image getLeavesRight() {
		if (leavesRight == null) {
			leavesRight = decoder.decode("jsettlers/graphics/menu/ui_leafs-right.png");
		}
		return leavesRight;
	}

	/**
	 * @return The game icon shown in the main menu when no sub menu is open.
	 */
	public Image getIcon() {
		if (icon == null) {
			icon = decoder.decode("jsettlers/graphics/menu/ui_icon.png");
		}
		return icon;
	}

	/**
	 * @return The gold-yellow border texture used for the menu frame.
	 */
	public Image getBorder() {
		if (border == null) {
			border = decoder.decode("jsettlers/graphics/menu/texture-border.png");
		}
		return border;
	}
}
