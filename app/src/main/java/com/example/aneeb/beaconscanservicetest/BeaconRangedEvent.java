package com.example.aneeb.beaconscanservicetest;

/**
 * A simple event wrapper for when a new region is entered
 */
public class BeaconRangedEvent {
    public int beaconMinor;

    public BeaconRangedEvent(int beaconMinor) {
        this.beaconMinor = beaconMinor;
    }
}
