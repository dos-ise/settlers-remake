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

import jsettlers.common.menu.IMapDefinition;
import jsettlers.common.menu.IOpenMultiplayerGameInfo;

/**
 * The information needed to open a new multiplayer game on a server. Shared between desktop and Android.
 *
 * @author Copilot
 */
public class OpenMultiplayerGameInfo implements IOpenMultiplayerGameInfo {
	private final String matchName;
	private final IMapDefinition map;

	public OpenMultiplayerGameInfo(String matchName, IMapDefinition map) {
		this.matchName = matchName;
		this.map = map;
	}

	@Override
	public String getMatchName() {
		return matchName;
	}

	@Override
	public IMapDefinition getMapDefinition() {
		return map;
	}

	@Override
	public int getMaxPlayers() {
		return map.getMaxPlayers();
	}
}
