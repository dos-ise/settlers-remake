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
import java.util.HashSet;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.widget.FrameLayout;

import go.graphics.android.GOSurfaceView;
import go.graphics.area.Area;
import go.graphics.region.Region;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import jsettlers.graphics.menu.GameDataActions;
import jsettlers.graphics.menu.GameDataPanel;
import jsettlers.graphics.menu.MenuContent;
import jsettlers.graphics.menu.MenuImageProvider;
import jsettlers.main.android.R;
import jsettlers.main.android.core.resources.scanner.AndroidResourcesLoader;
import jsettlers.main.android.mainmenu.gog.GogLoginActivity;
import jsettlers.main.android.mainmenu.ubisoft.UbisoftLoginActivity;

/**
 * Shows the shared, GL rendered game data setup panel. It is shown when the original Settlers 3 game
 * data is missing and lets the user select the installation folder or download the game data from
 * GOG or Ubisoft Connect. The same panel code runs on desktop and Android.
 */
@EFragment(R.layout.fragment_shared_main_menu)
public class GameDataSetupFragment extends Fragment implements GameDataActions {
	private static final int REQUEST_CODE_GOG_LOGIN = 1;
	private static final int REQUEST_CODE_UBISOFT_LOGIN = 2;
	private static final String HELP_URL = "https://github.com/dos-ise/settlers-remake/blob/master/README.md";

	private Region goRegion;

	@ViewById(R.id.frame_layout)
	FrameLayout frameLayout;

	public static Fragment create() {
		return new GameDataSetupFragment_();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@AfterViews
	void setupMenu() {
		MenuImageProvider.setInstance(new MenuImageProvider(new AndroidMenuImageDecoder()));

		goRegion = new Region(Region.POSITION_CENTER);
		Area goArea = new Area();
		goArea.set(goRegion);
		GOSurfaceView goView = new GOSurfaceView(getActivity(), goArea, () -> new HashSet<>());
		frameLayout.addView(goView);

		showGameDataPanel();
	}

	private void showGameDataPanel() {
		GameDataPanel panel = new GameDataPanel(this);
		MenuContent menuContent = new MenuContent(panel, action -> {
		});
		goRegion.setContent(menuContent);
	}

	/**
	 * GameDataActions implementation
	 */
	@Override
	public void chooseDirectory() {
		DirectoryPickerDialog.show(getActivity(), this::onDirectoryChosen);
	}

	private void onDirectoryChosen(File directory) {
		AndroidResourcesLoader loader = new AndroidResourcesLoader(getActivity());
		loader.setResourcesDirectory(directory.getAbsolutePath());
		loader.setupSingle()
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(() -> showSharedMainMenu());
	}

	@Override
	public void downloadFromGog() {
		startActivityForResult(new Intent(getActivity(), GogLoginActivity.class), REQUEST_CODE_GOG_LOGIN);
	}

	@Override
	public void downloadFromUbisoft() {
		startActivityForResult(new Intent(getActivity(), UbisoftLoginActivity.class), REQUEST_CODE_UBISOFT_LOGIN);
	}

	@Override
	public void openHelp() {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(HELP_URL)));
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			// The download activity stored the resources directory; set up the resources before
			// showing the main menu.
			AndroidResourcesLoader loader = new AndroidResourcesLoader(getActivity());
			loader.setupSingle()
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(() -> showSharedMainMenu());
		}
	}

	private void showSharedMainMenu() {
		getFragmentManager().beginTransaction()
				.replace(R.id.frame_layout, SharedMainMenuFragment.create())
				.commit();
	}
}
