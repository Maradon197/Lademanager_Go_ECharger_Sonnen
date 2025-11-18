package com.example.lademanager;

import android.animation.ObjectAnimator;
import android.content.res.ColorStateList;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private String goEApiUrl;
    private String sonnenApiUrl;

    private static final String TAG = "MainActivity";

    private SwitchCompat onOffSwitch;
    private TextView batteryLevel, carLevel, chargedEnergy, pvInput, switchText;
    private ImageButton refreshButton;

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Load IP addresses from strings.xml
        String goEChargerIp = getString(R.string.go_e_charger_ip);
        String sonnenIp = getString(R.string.sonnen_ip);

        goEApiUrl = "http://" + goEChargerIp + "/api/status";
        sonnenApiUrl = "http://" + sonnenIp + "/api/v1/status";

        onOffSwitch = findViewById(R.id.onOffSwitch);
        onOffSwitch.setShowText(false);

        batteryLevel = findViewById(R.id.batteryLevel);
        carLevel = findViewById(R.id.carLevel);
        chargedEnergy = findViewById(R.id.chargingTime); // Keeping original ID
        pvInput = findViewById(R.id.pvInput);
        switchText = findViewById(R.id.switchText);
        refreshButton = findViewById(R.id.refreshButton);

        updateGoEInfo(null);
        updateSonnenInfo(null);

        refreshButton.setOnClickListener(v -> {
            fetchChargerStatus();
            fetchSonnenStatus();
        });

        onOffSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            TransitionDrawable background = (TransitionDrawable) findViewById(R.id.main).getBackground();
            if (isChecked) {
                background.startTransition(500);
                onOffSwitch.setThumbTintList(ContextCompat.getColorStateList(this, R.color.switch_thumb_summer));
                onOffSwitch.setTrackTintList(ContextCompat.getColorStateList(this, R.color.switch_track_summer));
                animateTextColor(android.R.color.black, 500);
            } else {
                background.reverseTransition(100);
                onOffSwitch.setThumbTintList(ContextCompat.getColorStateList(this, R.color.switch_thumb_dark));
                onOffSwitch.setTrackTintList(ContextCompat.getColorStateList(this, R.color.switch_track_dark));
                animateTextColor(android.R.color.white, 100);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fetchChargerStatus();
        fetchSonnenStatus();
    }

    private void fetchChargerStatus() {
        Request request = new Request.Builder()
                .url(goEApiUrl)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch charger status", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String responseBody = response.body().string();
                    Log.d(TAG, "Charger status: " + responseBody);

                    final GoEChargerStatus status = gson.fromJson(responseBody, GoEChargerStatus.class);

                    runOnUiThread(() -> {
                        updateGoEInfo(status);
                        Toast.makeText(MainActivity.this, "go-e data updated", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    Log.e(TAG, "Failed to fetch charger status: " + response.code() + " " + response.message());
                }
            }
        });
    }

    private void fetchSonnenStatus() {
        Request request = new Request.Builder()
                .url(sonnenApiUrl)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch Sonnen status", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String responseBody = response.body().string();
                    Log.d(TAG, "Sonnen status: " + responseBody);

                    final SonnenStatus status = gson.fromJson(responseBody, SonnenStatus.class);

                    runOnUiThread(() -> {
                        updateSonnenInfo(status);
                        Toast.makeText(MainActivity.this, "Sonnen data updated", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    Log.e(TAG, "Failed to fetch Sonnen status: " + response.code() + " " + response.message());
                }
            }
        });
    }

    private void updateGoEInfo(GoEChargerStatus status) {
        String initialValue = getString(R.string.initial_value);

        if (status != null) {
            // Car Status
            String carStatusText = getCarStatusText(status.getCarStatus());
            carLevel.setText(getString(R.string.car_level_label, carStatusText));

            // Charged Energy
            String energyText = "--";
            if(status.getChargedEnergy() != null) {
                energyText = String.format(Locale.getDefault(), "%.2f kWh", status.getChargedEnergy() / 1000.0);
            }
            chargedEnergy.setText(getString(R.string.charged_energy_label, energyText));

        } else {
            carLevel.setText(getString(R.string.car_level_label, initialValue));
            chargedEnergy.setText(getString(R.string.charged_energy_label, initialValue));
        }
    }

    private void updateSonnenInfo(SonnenStatus status) {
        String initialValue = getString(R.string.initial_value);
        if (status != null) {
            // PV Power
            String pvText = "--";
            if(status.getProduction() != null) {
                pvText = String.format(Locale.getDefault(), "%d W", status.getProduction());
            }
            pvInput.setText(getString(R.string.pv_input_label, pvText));

            // Battery Power
            String batteryText = "--";
            if (status.getRsoc() != null) {
                batteryText = String.format(Locale.getDefault(), "%d %%", status.getRsoc());
            }
            batteryLevel.setText(getString(R.string.battery_level_label, batteryText));

        } else {
            pvInput.setText(getString(R.string.pv_input_label, initialValue));
            batteryLevel.setText(getString(R.string.battery_level_label, initialValue));
        }
    }

    private String getCarStatusText(Integer carStatus) {
        if (carStatus == null) return "Unbekannt";
        switch (carStatus) {
            case 1: return "Wartet auf Fahrzeug";
            case 2: return "Ladevorgang läuft";
            case 3: return "Warten auf Ladefreigabe";
            case 4: return "Ladung beendet";
            default: return "Unbekannter Status";
        }
    }

    private void animateTextColor(int colorResId, int duration) {
        int color = ContextCompat.getColor(this, colorResId);
        ObjectAnimator.ofArgb(batteryLevel, "textColor", color).setDuration(duration).start();
        ObjectAnimator.ofArgb(carLevel, "textColor", color).setDuration(duration).start();
        ObjectAnimator.ofArgb(chargedEnergy, "textColor", color).setDuration(duration).start();
        ObjectAnimator.ofArgb(pvInput, "textColor", color).setDuration(duration).start();
        ObjectAnimator.ofArgb(switchText, "textColor", color).setDuration(duration).start();
    }
}