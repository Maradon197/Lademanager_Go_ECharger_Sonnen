package com.example.lademanager;

import com.google.gson.annotations.SerializedName;

public class GoEChargerStatus {

    @SerializedName("car")
    private Integer carStatus;

    @SerializedName("wh")
    private Double chargedEnergy;

    @SerializedName("pakku")
    private Integer batteryPower;

    public Integer getCarStatus() {
        return carStatus;
    }

    public Double getChargedEnergy() {
        return chargedEnergy;
    }

    public Integer getBatteryPower() {
        return batteryPower;
    }
}