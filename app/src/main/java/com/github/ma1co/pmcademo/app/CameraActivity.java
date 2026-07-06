package com.github.ma1co.pmcademo.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.IOException;
import java.util.List;

import com.sony.scalar.hardware.CameraEx;
import com.sony.wifi.direct.DirectConfiguration;
import com.sony.wifi.direct.DirectManager;

public class CameraActivity extends BaseActivity implements SurfaceHolder.Callback {
    private SurfaceHolder surfaceHolder;
    private CameraEx camera;
    private HttpServer httpServer;

    private WifiManager wifiManager;
    private DirectManager wifiDirectManager;
    private BroadcastReceiver wifiDirectStateReceiver;

    private volatile boolean isBursting = false;

    @SuppressWarnings("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiDirectManager = (DirectManager) getSystemService(DirectManager.WIFI_DIRECT_SERVICE);

        wifiDirectStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                wifiDirectStateChanged(intent.getIntExtra(DirectManager.EXTRA_DIRECT_STATE, DirectManager.DIRECT_STATE_UNKNOWN));
            }
        };
        httpServer = new HttpServer(new HttpServer.BurstStatusProvider() {
            @Override
            public boolean isBursting() {
                return CameraActivity.this.isBursting;
            }
        },this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera = CameraEx.open(0, null);
        Camera normalCamera = camera.getNormalCamera();
        Camera.Parameters parameters=normalCamera.getParameters();
        CameraEx.ParametersModifier params = camera.createParametersModifier(parameters);
        params.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_BURST);
        params.setBurstDriveSpeed(CameraEx.ParametersModifier.BURST_DRIVE_SPEED_HIGH);
        params.setBurstDriveButtonReleaseBehave(CameraEx.ParametersModifier.BURST_DRIVE_BUTTON_RELEASE_BEHAVE_STOP);
        normalCamera.setParameters(parameters);
        surfaceHolder.addCallback(this);
        registerReceiver(wifiDirectStateReceiver, new IntentFilter(DirectManager.DIRECT_STATE_CHANGED_ACTION));
        wifiManager.setWifiEnabled(true);
        wifiDirectManager.setDirectEnabled(true);
        try {
            httpServer.start();
        } catch (IOException ignored) {
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        isBursting = false;
        camera.release();
        camera = null;
        surfaceHolder.removeCallback(this);
        unregisterReceiver(wifiDirectStateReceiver);
        wifiDirectManager.setDirectEnabled(false);
        wifiManager.setWifiEnabled(false);
        httpServer.stop();
    }
    protected void wifiDirectStateChanged(int state) {
        switch (state) {
            case DirectManager.DIRECT_STATE_ENABLING:
                break;
            case DirectManager.DIRECT_STATE_ENABLED:
                wifiDirectEnabled();
                break;
        }
    }
    protected void wifiDirectEnabled() {
        List<DirectConfiguration> configurations = wifiDirectManager.getConfigurations();
        if (configurations.isEmpty()) {
        } else {
            wifiDirectManager.startGo(configurations.get(configurations.size() - 1).getNetworkId());
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera.getNormalCamera().setPreviewDisplay(holder);
            camera.getNormalCamera().startPreview();
        } catch (IOException e) {
            // Ignore preview setup failures here; the camera lifecycle will handle retries.
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    protected  boolean onEnterKeyDown() {
        return true;
    }
    @Override
    protected boolean onFocusKeyDown() {
        camera.getNormalCamera().autoFocus(null);
        isBursting = true;
        return true;
    }

    @Override
    protected boolean onFocusKeyUp() {
        isBursting = false;
        camera.getNormalCamera().cancelAutoFocus();
        return true;
    }

    @Override
    protected boolean onShutterKeyDown() {
        camera.burstableTakePicture();
        return true;
    }

    @Override
    protected boolean onShutterKeyUp() {
        camera.cancelTakePicture();
        return true;
    }
    @Override
    protected void setColorDepth(boolean highQuality) {
        super.setColorDepth(false);
    }
}
