package org.wheelmap.android.ui.mapsforge;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.MapViewMode;
import org.wheelmap.android.model.Map;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.provider.Settings.System;

public class ConfigureMapView {
	private final static MapViewMode defaultViewMode = MapViewMode.MAPNIK_TILE_DOWNLOAD;

	// private final static MapViewMode defaultViewMode =
	// MapViewMode.OSMARENDER_TILE_DOWNLOAD;

	public static void pickAppropriateMap(Context context, MapView mapView) {

		ProviderInfo info = context.getPackageManager().resolveContentProvider(
				Map.AUTHORITY, 0);

		if (info == null) {
			mapView.setMapViewMode(defaultViewMode);
			return;
		}

		ContentResolver resolver = context.getContentResolver();
		Cursor c;
		try {
			 c = resolver.query(Map.CONTENT_URI_SELECTED,
					Map.selectedPROJECTION, null, null, null);
		} catch (RuntimeException e) {
			mapView.setMapViewMode(defaultViewMode);
			return;
		}
		if ( c == null ) {
			mapView.setMapViewMode(defaultViewMode);
			return;
		}
		
		c.close();
	
		String mapFile;
		if (c.getCount() == 1) {
			c.moveToFirst();
			mapFile = Map.createPath(Map.getParentName(c), Map.getName(c));
			mapView.setMapFile(mapFile);
			if (mapView.hasValidMapFile())
				mapView.setMapViewMode(MapViewMode.CANVAS_RENDERER);
			else
				mapView.setMapViewMode(defaultViewMode);
		} else
			mapView.setMapViewMode(defaultViewMode);

	}
}
