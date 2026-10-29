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

import java.util.Date;

import jsettlers.graphics.image.Image;

/**
 * A map entry shown in the shared map selection menu.
 * <p>
 * This is a platform independent view of a map. The host application (desktop or Android) resolves the opaque
 * {@link #getId() id} back to its own map representation when the player starts a game.
 *
 * @author Copilot
 */
public class MapMenuItem {
	private final String id;
	private final String name;
	private final String description;
	private final Image preview;
	private final int minPlayers;
	private final int maxPlayers;
	private final Date creationDate;
	private final boolean savegame;

	/**
	 * Creates a new map menu item without a preview image.
	 *
	 * @param id
	 *            An opaque id that the host application can resolve back to its map representation.
	 * @param name
	 *            The display name of the map.
	 * @param description
	 *            An optional description of the map.
	 */
	public MapMenuItem(String id, String name, String description) {
		this(id, name, description, null, 0, 0, null);
	}

	/**
	 * Creates a new map menu item.
	 *
	 * @param id
	 *            An opaque id that the host application can resolve back to its map representation.
	 * @param name
	 *            The display name of the map.
	 * @param description
	 *            An optional description of the map.
	 * @param preview
	 *            The preview image of the map, or <code>null</code> if there is none.
	 */
	public MapMenuItem(String id, String name, String description, Image preview) {
		this(id, name, description, preview, 0, 0, null);
	}

	/**
	 * Creates a new map menu item.
	 *
	 * @param id
	 *            An opaque id that the host application can resolve back to its map representation.
	 * @param name
	 *            The display name of the map.
	 * @param description
	 *            An optional description of the map.
	 * @param preview
	 *            The preview image of the map, or <code>null</code> if there is none.
	 * @param minPlayers
	 *            The minimum number of players supported by the map.
	 * @param maxPlayers
	 *            The maximum number of players supported by the map.
	 * @param creationDate
	 *            The creation date of the map, or <code>null</code> if unknown.
	 */
	public MapMenuItem(String id, String name, String description, Image preview, int minPlayers, int maxPlayers,
			Date creationDate) {
		this(id, name, description, preview, minPlayers, maxPlayers, creationDate, false);
	}

	/**
	 * Creates a new map menu item.
	 *
	 * @param id
	 *            An opaque id that the host application can resolve back to its map representation.
	 * @param name
	 *            The display name of the map.
	 * @param description
	 *            An optional description of the map.
	 * @param preview
	 *            The preview image of the map, or <code>null</code> if there is none.
	 * @param minPlayers
	 *            The minimum number of players supported by the map.
	 * @param maxPlayers
	 *            The maximum number of players supported by the map.
	 * @param creationDate
	 *            The creation date of the map, or <code>null</code> if unknown.
	 * @param savegame
	 *            Whether this entry is a savegame. For savegames the description is the title the player entered when
	 *            saving.
	 */
	public MapMenuItem(String id, String name, String description, Image preview, int minPlayers, int maxPlayers,
			Date creationDate, boolean savegame) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.preview = preview;
		this.minPlayers = minPlayers;
		this.maxPlayers = maxPlayers;
		this.creationDate = creationDate;
		this.savegame = savegame;
	}

	/**
	 * Gets the opaque id of this map.
	 *
	 * @return The id.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Gets the display name of this map.
	 *
	 * @return The name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the description of this map.
	 *
	 * @return The description, may be empty.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Gets the preview image of this map.
	 *
	 * @return The preview image, or <code>null</code> if there is none.
	 */
	public Image getPreview() {
		return preview;
	}

	/**
	 * Gets the minimum number of players supported by this map.
	 *
	 * @return The minimum number of players.
	 */
	public int getMinPlayers() {
		return minPlayers;
	}

	/**
	 * Gets the maximum number of players supported by this map.
	 *
	 * @return The maximum number of players.
	 */
	public int getMaxPlayers() {
		return maxPlayers;
	}

	/**
	 * Gets the creation date of this map.
	 *
	 * @return The creation date, or <code>null</code> if unknown.
	 */
	public Date getCreationDate() {
		return creationDate;
	}

	/**
	 * Gets whether this entry is a savegame.
	 *
	 * @return <code>true</code> if this entry is a savegame.
	 */
	public boolean isSavegame() {
		return savegame;
	}
}
