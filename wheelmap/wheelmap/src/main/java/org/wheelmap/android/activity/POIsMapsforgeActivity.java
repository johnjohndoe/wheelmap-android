/*
Copyright (C) 2011 Michal Harakal and Michael Kroez

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS-IS" BASIS
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
        
*/

package org.wheelmap.android.activity;

import org.wheelmap.android.fragment.POIsMapsforgeFragment;
import org.wheelmap.android.fragment.POIsMapsforgeWorkerFragment;
import org.wheelmap.android.fragment.POIsMapsforgeWorkerFragment.OnPOIsMapsforgeWorkerFragmentListener;
import org.wheelmap.android.online.R;
import org.wheelmap.android.service.SyncServiceException;
import org.wheelmap.android.ui.InfoActivity;
import org.wheelmap.android.ui.SearchActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

public class POIsMapsforgeActivity extends MapsforgeMapActivity implements
		OnClickListener, OnPOIsMapsforgeWorkerFragmentListener {
	private final static String TAG = "mapsforge";
	private ProgressBar mProgressBar;
	private ImageButton mSearchButton;

	private POIsMapsforgeFragment mapFragment;
	private POIsMapsforgeWorkerFragment mapWorkerFragment;
	private boolean isSearchMode;
	private boolean isShowingDialog;
	private boolean isInForeground;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d( TAG, "Activity onCreate" );
		setContentView(R.layout.activity_mapsforge_fragments);
		mProgressBar = (ProgressBar) findViewById(R.id.progressbar_map);
		mSearchButton = (ImageButton) findViewById(R.id.btn_title_search);
		TextView listView = (TextView) findViewById(R.id.switch_list);
		listView.setOnClickListener(this);
	}

	@Override
	protected void onStart() {
		super.onStart();
		FragmentManager fm = getSupportFragmentManager();

		mapFragment = (POIsMapsforgeFragment) fm
				.findFragmentById(R.id.map_fragment);
		mapWorkerFragment = (POIsMapsforgeWorkerFragment) fm
				.findFragmentByTag(POIsMapsforgeWorkerFragment.TAG);
		Log.d( TAG, "Fragment: " + mapFragment + " WorkerFragment:" + mapWorkerFragment );
	}

	@Override
	public void onResume() {
		super.onResume();
		isInForeground = true;
		Log.d(TAG, "onResume isInForeground = " + isInForeground);
	}

	@Override
	public void onPause() {
		super.onPause();
		isInForeground = false;
		Log.d(TAG, "onPause isInForeground = " + isInForeground);
	}

	public void onClick(View view) {
		int id = view.getId();
		if (id == R.id.switch_list) {
			Intent intent = new Intent(POIsMapsforgeActivity.this,
					org.wheelmap.android.activity.POIsListActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			startActivity(intent);
			overridePendingTransition(0, 0);
		}
	}

	public void onSearchClick(View v) {
		isSearchMode = !isSearchMode;
		updateSearchStatus();

		if (isSearchMode) {
			updateSearchStatus();

			final Intent intent = new Intent(POIsMapsforgeActivity.this,
					SearchActivity.class);
			intent.putExtra(SearchActivity.EXTRA_SHOW_MAP_HINT, true);
			startActivityForResult(intent, SearchActivity.PERFORM_SEARCH);
		}
	}

//	@Override
//	public boolean onPrepareOptionsMenu(Menu menu) {
//		startActivity(new Intent(this, NewSettingsActivity.class));
//		return super.onPrepareOptionsMenu(menu);
//	}

//	public void onListClick(View v) {
//		Intent intent = new Intent(this, POIsListActivity.class);
//		intent.putExtra(POIsListActivity.EXTRA_IS_RECREATED, false);
//		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
//				| Intent.FLAG_ACTIVITY_NO_ANIMATION);
//		startActivity(intent);
//		overridePendingTransition(0, 0);
//
//	}

	public void onCenterClick(View v) {
		mapFragment.navigateToLocation();
	}

	public void onInfoClick(View v) {
		Intent intent = new Intent(this, InfoActivity.class);
		startActivity(intent);
	}

	@Override
	public boolean onSearchRequested() {
		Bundle extras = new Bundle();
		startSearch(null, false, extras, false);
		return true;
	}

	@Override
	public void onRefreshStatusChange(boolean refreshStatus) {
		if ( refreshStatus )
			mProgressBar.setVisibility( View.VISIBLE);
		else
			mProgressBar.setVisibility( View.GONE );
	}

	private void updateSearchStatus() {
		mSearchButton.setSelected(isSearchMode);
		if (mapWorkerFragment != null)
			mapWorkerFragment.setSearchMode(isSearchMode);
	}

	@Override
	public void onSearchModeChange(boolean isSearchMode) {
		this.isSearchMode = isSearchMode;
		mSearchButton.setSelected(isSearchMode);
	}

	/**
	 * This method is called when the sending activity has finished, with the
	 * result it supplied.
	 * 
	 * @param requestCode
	 *            The original request code as given to startActivity().
	 * @param resultCode
	 *            From sending activity as per setResult().
	 * @param data
	 *            From sending activity as per setResult().
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// You can use the requestCode to select between multiple child
		// activities you may have started. Here there is only one thing
		// we launch.
		if (requestCode == SearchActivity.PERFORM_SEARCH) {
			// This is a standard resultCode that is sent back if the
			// activity doesn't supply an explicit result. It will also
			// be returned if the activity failed to launch.
			if (resultCode == RESULT_OK) {
				if (data != null && data.getExtras() != null) {
					Bundle bundle = data.getExtras();
					mapFragment.startSearch( bundle );
				}
			}
		}
	}

	private void showErrorDialog(SyncServiceException e) {
		if (!isInForeground)
			return;
		if (isShowingDialog)
			return;

		isShowingDialog = true;

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if (e.getErrorCode() == SyncServiceException.ERROR_NETWORK_FAILURE)
			builder.setTitle(R.string.error_network_title);
		else
			builder.setTitle(R.string.error_occurred);
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setMessage(e.getRessourceString());
		builder.setNeutralButton(R.string.okay,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						isShowingDialog = false;
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	@Override
	public void onError(SyncServiceException e) {
		showErrorDialog( e );
	}
	
}