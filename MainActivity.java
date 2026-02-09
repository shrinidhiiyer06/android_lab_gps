package com.example.prac_gps;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    TextView t1;
    Button b1;
    LocationManager LM;
    LocationListener LL;
    PreviewView viewFinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        t1 = findViewById(R.id.t1);
        b1 = findViewById(R.id.b1);
        viewFinder = findViewById(R.id.viewFinder);

        LM = (LocationManager) getSystemService(LOCATION_SERVICE);

        LL = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                // Displays data on top of the camera feed
                t1.setText("Latitude: " + location.getLatitude() +
                        "\nLongitude: " + location.getLongitude());
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                Toast.makeText(getApplicationContext(), "Please enable GPS", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        };

        b1.setOnClickListener(view -> checkPermissionsAndStart());
    }

    private void checkPermissionsAndStart() {
        // Checking for both Camera and Location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA
            }, 101);
        } else {
            startCamera();
            startGPS();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
            startGPS();
        } else {
            Toast.makeText(this, "Permissions are required to run this app", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                // Select back camera and bind to the app's lifecycle
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void startGPS() {
        try {
            // Request from GPS (Accurate, works best outdoors)
            LM.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 5, LL);

            // Request from Network (Faster, works indoors)
            LM.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 5, LL);

            t1.setText("Searching for location...");
            Toast.makeText(this, "GPS & Network tracking active", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
