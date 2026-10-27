/*
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
 */

package jsettlers.main.android.mainmenu;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import jsettlers.common.resources.SettlersFolderChecker;
import jsettlers.main.android.R;

/**
 * Lets the user browse the file system and pick the folder of an original Settlers 3 installation.
 * The dialog is shown on top of the shared GL game data panel.
 */
public class DirectoryPickerDialog {
	/**
	 * Listener for the selected directory.
	 */
	public interface DirectorySelectedListener {
		void onDirectorySelected(File directory);
	}

	private DirectoryPickerDialog() {
	}

	/**
	 * Shows the directory picker dialog.
	 *
	 * @param context
	 *            The context to show the dialog in.
	 * @param listener
	 *            Called when a valid Settlers 3 folder was selected.
	 */
	public static void show(Context context, DirectorySelectedListener listener) {
		DirectoryAdapter adapter = new DirectoryAdapter(context, listener);
		ListView listView = new ListView(context);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener((parent, view, position, id) -> adapter.positionSelected(position));

		new AlertDialog.Builder(context)
				.setTitle(R.string.resource_directory_title)
				.setView(listView)
				.setNegativeButton(android.R.string.cancel, null)
				.create()
				.show();
	}

	private static class DirectoryAdapter extends ArrayAdapter<String> {
		private final File baseDirectory;
		private final DirectorySelectedListener listener;
		private File currentDirectory;

		DirectoryAdapter(Context context, DirectorySelectedListener listener) {
			super(context, android.R.layout.simple_list_item_1);
			this.listener = listener;
			this.baseDirectory = Environment.getExternalStorageDirectory();
			this.currentDirectory = this.baseDirectory;
			updateList();
		}

		void positionSelected(int position) {
			String item = getItem(position);
			if (item == null) {
				return;
			}
			try {
				File newDirectory = new File(currentDirectory, item).getCanonicalFile();
				if (!newDirectory.exists()) {
					return;
				}
				currentDirectory = newDirectory;
				if (SettlersFolderChecker.checkSettlersFolder(currentDirectory).isValidSettlersFolder()) {
					listener.onDirectorySelected(currentDirectory);
				}
				updateList();
			} catch (IOException e) {
				// ignore this exception
			}
		}

		private void updateList() {
			clear();
			if (!baseDirectory.equals(currentDirectory)) {
				add("..");
			}
			File[] files = currentDirectory.listFiles(File::isDirectory);
			if (files != null && files.length > 0) {
				for (File file : files) {
					add(file.getName());
				}
			} else {
				add(getContext().getResources().getString(R.string.empty_directory));
			}
		}
	}
}
