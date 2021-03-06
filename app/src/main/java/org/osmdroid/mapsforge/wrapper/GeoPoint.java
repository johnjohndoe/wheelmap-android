package org.osmdroid.mapsforge.wrapper;

import org.osmdroid.api.IGeoPoint;

/**
 * A wrapper for the Google {@link org.mapsforge.android.maps.GeoPoint} class. This implements
 * {@link IGeoPoint}, which is also implemented by the osmdroid {@link org.osmdroid.util.GeoPoint}.
 *
 * @author Neil Boyd
 */
public class GeoPoint implements IGeoPoint {

    private final org.mapsforge.android.maps.GeoPoint mGeoPoint;

    public GeoPoint(final org.mapsforge.android.maps.GeoPoint pGeoPoint) {
        mGeoPoint = pGeoPoint;
    }

    @Override
    public int getLatitudeE6() {
        return mGeoPoint.getLatitudeE6();
    }

    @Override
    public int getLongitudeE6() {
        return mGeoPoint.getLongitudeE6();
    }

    @Override
    public double getLatitude() {
        return mGeoPoint.getLatitude();
    }

    @Override
    public double getLongitude() {
        return mGeoPoint.getLongitude();
    }
}
