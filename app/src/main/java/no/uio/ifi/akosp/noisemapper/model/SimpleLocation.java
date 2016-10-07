package no.uio.ifi.akosp.noisemapper.model;

import android.location.Location;

import java.io.Serializable;

/**
 * Created on 2016.04.26..
 *
 * @author Ákos Pap
 */
public class SimpleLocation implements Serializable {

    protected double mLatitude;
    protected double mLongitude;

    protected boolean mHasAltitude = false;
    protected double mAltitude = 0.0f;
    protected boolean mHasSpeed = false;
    protected float mSpeed = 0.0f;
    protected boolean mHasBearing = false;
    protected float mBearing = 0.0f;
    protected boolean mHasAccuracy = false;
    protected float mAccuracy = 0.0f;

    protected String mProvider;

    public static SimpleLocation fromLocation(Location loc) {
        SimpleLocation sl = new SimpleLocation();
        sl.mLatitude = loc.getLatitude();
        sl.mLongitude = loc.getLongitude();

        sl.mHasAltitude = loc.hasAltitude();
        sl.mAltitude = loc.getAltitude();

        sl.mHasSpeed = loc.hasSpeed();
        sl.mSpeed = loc.getSpeed();

        sl.mHasBearing = loc.hasBearing();
        sl.mBearing = loc.getBearing();

        sl.mHasAccuracy = loc.hasAccuracy();
        sl.mAccuracy = loc.getAccuracy();

        sl.mProvider = loc.getProvider();
        return sl;
    }

    public Location asLocation() {
        Location loc = new Location(mProvider);
        loc.setLatitude(mLatitude);
        loc.setLongitude(mLongitude);
        if (mHasAltitude) { loc.setAltitude(mAltitude); }
        if (mHasSpeed) { loc.setSpeed(mSpeed); }
        if (mHasBearing) { loc.setBearing(mBearing); }
        if (mHasAccuracy) { loc.setAltitude(mAccuracy); }
        return loc;
    }

    public SimpleLocation() {}
    
    public SimpleLocation(double mLatitude,
                          double mLongitude,
                          boolean mHasAltitude,
                          double mAltitude,
                          boolean mHasSpeed,
                          float mSpeed,
                          boolean mHasBearing,
                          float mBearing,
                          boolean mHasAccuracy,
                          float mAccuracy,
                          String mProvider) {
        this.mLatitude = mLatitude;
        this.mLongitude = mLongitude;

        if (mHasAltitude) {
            this.mHasAltitude = true;
            this.mAltitude = mAltitude;
        }

        if (mHasSpeed) {
            this.mHasSpeed = true;
            this.mSpeed = mSpeed;
        }

        if (mHasBearing) {
            this.mHasBearing = true;
            this.mBearing = mBearing;
        }

        if (mHasAccuracy) {
            this.mHasAccuracy = true;
            this.mAccuracy = mAccuracy;
        }

        this.mProvider = mProvider;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public boolean hasAltitude() {
        return mHasAltitude;
    }

    public double getAltitude() {
        return mAltitude;
    }

    public boolean hasSpeed() {
        return mHasSpeed;
    }

    public float getSpeed() {
        return mSpeed;
    }

    public boolean hasBearing() {
        return mHasBearing;
    }

    public float getBearing() {
        return mBearing;
    }

    public boolean hasAccuracy() {
        return mHasAccuracy;
    }

    public float getAccuracy() {
        return mAccuracy;
    }

    public String getProvider() {
        return mProvider;
    }
}
