package com.example.lademanager;

import com.google.gson.annotations.SerializedName;

public class SonnenStatus {

    @SerializedName("Production_W")
    private Integer production;

    @SerializedName("RSOC")
    private Integer rsoc; // Relative State of Charge in %

    public Integer getProduction() {
        return production;
    }

    public Integer getRsoc() {
        return rsoc;
    }
}