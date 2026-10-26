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

import jsettlers.graphics.image.Image;

/**
 * The platform independent map information the shared join game panel needs. The hosts (desktop and Android) build
 * this from their {@code MapLoader} so the panel itself does not depend on the logic module.
 *
 * @author Copilot
 */
public class JoinGameMapInfo {
	private final String mapName;
	private final Image preview;
	private final int maxPlayers;
	private final JoinGamePlayerSlotData[] initialSlots;

	/**
	 * Creates a new map info.
	 *
	 * @param mapName
	 *            The name of the map.
	 * @param preview
	 *            The map preview image or <code>null</code>.
	 * @param maxPlayers
	 *            The maximum number of players supported by the map.
	 * @param initialSlots
	 *            The initial player slot settings from the map file header. May be shorter than {@code maxPlayers}.
	 */
	public JoinGameMapInfo(String mapName, Image preview, int maxPlayers, JoinGamePlayerSlotData[] initialSlots) {
		this.mapName = mapName;
		this.preview = preview;
		this.maxPlayers = maxPlayers;
		this.initialSlots = initialSlots;
	}

	public String getMapName() {
		return mapName;
	}

	public Image getPreview() {
		return preview;
	}

	public int getMaxPlayers() {
		return maxPlayers;
	}

	public JoinGamePlayerSlotData[] getInitialSlots() {
		return initialSlots;
	}
}
