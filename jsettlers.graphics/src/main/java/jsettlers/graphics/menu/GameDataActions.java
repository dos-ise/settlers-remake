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

/**
 * The actions the game data setup panel triggers on the host application. The same panel runs on
 * desktop and Android; each platform provides its own implementation.
 *
 * @author Copilot
 */
public interface GameDataActions {
	/**
	 * Lets the user select the folder of an original Settlers 3 installation.
	 */
	void chooseDirectory();

	/**
	 * Downloads the game data from GOG.
	 */
	void downloadFromGog();

	/**
	 * Downloads the game data from Ubisoft Connect.
	 */
	void downloadFromUbisoft();

	/**
	 * Opens the help page.
	 */
	void openHelp();
}
