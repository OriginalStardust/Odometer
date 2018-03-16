package com.sunchang.odometer;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private OdometerService odometer;

    private boolean bound = false;

    private double savedDistance = 0.0;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            OdometerService.OdometerBinder odometerBinder = (OdometerService.OdometerBinder) iBinder;
            odometer = odometerBinder.getOdometer();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.watchMileage();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bound) {
            this.unbindService(connection);
            bound = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.bindOdometerService();
                    bound = true;
                } else {
                    Toast.makeText(this, "You denied the permission", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    public void onStartClick(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        } else {
            this.bindOdometerService();
            bound = true;
        }
    }

    public void onStopClick(View view) {
        if (bound) {
            this.unbindService(connection);
            odometer = null;
            bound = false;
        }
    }

    public void onResetClick(View view) {
        if (bound) {
            this.unbindService(connection);
            odometer = null;
            savedDistance = 0.0;
            bound = false;
        }
    }

    private void watchMileage() {
        final TextView distanceView = (TextView) this.findViewById(R.id.distance);
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            private double lastDistance = 0.0;
            @Override
            public void run() {
                double distance = 0.0;
                if (odometer != null) {
                    distance += (odometer.getMiles() + lastDistance);
                    savedDistance = distance;
                } else {
                    lastDistance = savedDistance;
                }
                String distanceStr;
                if (bound) {
                    distanceStr = String.format("%1$,.2f meters", distance);
                    distanceView.setText(distanceStr);
                } else {
                    distanceStr = String.format("%1$,.2f meters", savedDistance);
                    distanceView.setText(distanceStr);
                }
                handler.postDelayed(this, 1000);
            }
        });
    }

    private void bindOdometerService() {
        try {
            Intent intent = new Intent(this, OdometerService.class);
            this.bindService(intent, connection, BIND_AUTO_CREATE);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}
