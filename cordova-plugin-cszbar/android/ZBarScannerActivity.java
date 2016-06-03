package org.cloudsky.cordovaPlugins;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dtr.zbar.build.ZBarDecoder;
import com.duoxieyun.lenglianzhushou.R;

import java.lang.reflect.Field;

public class ZBarScannerActivity extends Activity {

    // For retrieving R.* resources, from the actual app package
    // (we can't use actual.application.package.R.* in our code as we
    // don't know the applciation package name when writing this plugin).
    public static final String EXTRA_QRVALUE = "qrValue";
    public static final String EXTRA_PARAMS = "params";
    private String package_name;
    private Resources resources;
    private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;
    private CameraManager mCameraManager;
    private static final int CAMERA_PERMISSION_REQUEST = 1;

    private  Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    Toast.makeText(ZBarScannerActivity.this, "缺少相机权限", Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                default:
                    break;
            }
        }
    };
    private TextView scanResult;
    private FrameLayout scanPreview;
    private Button scanRestart;
    private RelativeLayout scanContainer;
    private RelativeLayout scanCropView;
    private ImageView scanLine;

    private  TextView tvBack;
    private Rect mCropRect = null;
    private boolean barcodeScanned = false;
    private boolean previewing = true;

    private  ImageView imageViewLight;

    public void onCreate(Bundle savedInstanceState) {
        int permissionCheck = ContextCompat.checkSelfPermission(this.getBaseContext(), Manifest.permission.CAMERA);
        Log.i("权限",permissionCheck+"");
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            setUpCamera();

        } else {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);

        }
        super.onCreate(savedInstanceState);

    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.i("权限", "requestCode:" + requestCode + "");
        switch (requestCode) {

            case CAMERA_PERMISSION_REQUEST: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setUpCamera();
                } else {

                    onBackPressed();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
    private void  setUpCamera()
    {
        setContentView(getResourceId("layout/activity_capture"));
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        findViewById();
//        addEvents();
        initViews();
    }
    private int getResourceId(String typeAndName) {
        if (package_name == null) package_name = getApplication().getPackageName();
        if (resources == null) resources = getApplication().getResources();
        return resources.getIdentifier(typeAndName, null, package_name);
    }
    private void findViewById() {
        scanPreview = (FrameLayout) findViewById(getResourceId("id/capture_preview"));
        scanResult = (TextView) findViewById(getResourceId("id/capture_scan_result"));
//        scanRestart = (Button) findViewById(getResourceId("id/capture_restart_scan"));
        scanContainer = (RelativeLayout) findViewById(getResourceId("id/capture_container"));
        scanCropView = (RelativeLayout) findViewById(getResourceId("id/capture_crop_view"));
        scanLine = (ImageView) findViewById(getResourceId("id/capture_scan_line"));
        imageViewLight= (ImageView) findViewById(getResourceId("id/capture_scan_light"));
        tvBack=(TextView) findViewById(getResourceId("id/back"));
        tvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ZBarScannerActivity.this.finish();
            }
        });
    }

//    private void addEvents() {
//        scanRestart.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                if (barcodeScanned) {
//                    barcodeScanned = false;
//                    scanResult.setText("扫描中...");
//                    mCamera.setPreviewCallback(previewCb);
//                    mCamera.startPreview();
//                    previewing = true;
//                    mCamera.autoFocus(autoFocusCB);
//                }
//            }
//        });
//    }

    private void initViews() {
        autoFocusHandler = new Handler();
        mCameraManager = new CameraManager(this,handler);
        try {
            mCameraManager.openDriver();


        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "缺少相机权限", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mCamera = mCameraManager.getCamera();
        mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
        scanPreview.addView(mPreview);

        TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT,
                1f);
        animation.setDuration(2000);
        animation.setRepeatCount(-1);
        animation.setRepeatMode(Animation.RESTART);
        scanLine.startAnimation(animation);
        //scanResult.setText("扫描中...");
        mCamera.setPreviewCallback(previewCb);
        mCamera.startPreview();
        previewing = true;
        mCamera.autoFocus(autoFocusCB);
    }

    public void onPause() {
        super.onPause();
        releaseCamera();
    }

    @Override
    protected void onRestart() {
        super.onRestart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("home","onResume");
        if(mCamera==null)
        {
            Log.i("home","mCamera null");
            mCamera = mCameraManager.getCamera();
            Log.i("home","1");
            previewing = true;
            Log.i("home","2");

            mCamera.setPreviewCallback(previewCb);
            mCamera.startPreview();

        }
    }


    private void releaseCamera() {
        Log.i("home","release");
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            finish();
        }
    }

    public void turnback()
    {
        this.finish();
    }
    public void toggleFlash(View view) {

        mCamera.startPreview();
        Camera.Parameters camParams = mCamera.getParameters();
        //If the flash is set to off
        try {
            if (camParams.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF) && !(camParams.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) && !(camParams.getFlashMode().equals(Camera.Parameters.FLASH_MODE_ON)))

            {
                camParams.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                imageViewLight.setImageResource(R.drawable.on);
                Log.i("light","打开");

            }
            else //if(camParams.getFlashMode() == Parameters.FLASH_MODE_ON || camParams.getFlashMode()== Parameters.FLASH_MODE_TORCH)

            {
                camParams.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                imageViewLight.setImageResource(R.drawable.off);
                Log.i("light","关闭");
            }
        } catch (RuntimeException e) {

        }

        try {
            // camera.setParameters(camParams);
           // mCamera.setPreviewDisplay(holder);
            mCamera.setPreviewCallback(previewCb);
            mCamera.startPreview();

                mCamera.autoFocus(autoFocusCB); // We are not using any of the

            //tryStopPreview();
            //tryStartPreview();
            //camParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            mCamera.setParameters(camParams);
        } catch (RuntimeException e) {
            Log.d("csZBar", (new StringBuilder("Unsupported camera parameter reported for flash mode: ")).append("flash").toString());
        }
    }
    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (previewing)
                mCamera.autoFocus(autoFocusCB);
        }
    };

    Camera.PreviewCallback previewCb = new Camera.PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Size size = camera.getParameters().getPreviewSize();

            // 这里需要将获取的data翻转一下，因为相机默认拿的的横屏的数据
            byte[] rotatedData = new byte[data.length];
            for (int y = 0; y < size.height; y++) {
                for (int x = 0; x < size.width; x++)
                    rotatedData[x * size.height + size.height - y - 1] = data[x + y * size.width];
            }

            // 宽高也要调整
            int tmp = size.width;
            size.width = size.height;
            size.height = tmp;

            initCrop();
            ZBarDecoder zBarDecoder = new ZBarDecoder();
            String result = zBarDecoder.decodeCrop(rotatedData, size.width, size.height, mCropRect.left, mCropRect.top, mCropRect.width(), mCropRect.height());

            if (!TextUtils.isEmpty(result)) {
                previewing = false;
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();

                scanResult.setText("barcode result " + result);
                barcodeScanned = true;
                Log.i("tiaoma",result);
                //Return 1st found QR code value to the calling Activity.
                Intent result2 = new Intent();
                result2.putExtra(EXTRA_QRVALUE, result);
                setResult(Activity.RESULT_OK, result2);
                finish();
            }
        }
    };

    // Mimic continuous auto-focusing
    Camera.AutoFocusCallback autoFocusCB = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };

    /**
     * 初始化截取的矩形区域
     */
    private void initCrop() {
        int cameraWidth = mCameraManager.getCameraResolution().y;
        int cameraHeight = mCameraManager.getCameraResolution().x;

        /** 获取布局中扫描框的位置信息 */
        int[] location = new int[2];
        scanCropView.getLocationInWindow(location);

        int cropLeft = location[0];
        int cropTop = location[1] - getStatusBarHeight();

        int cropWidth = scanCropView.getWidth();
        int cropHeight = scanCropView.getHeight();

        /** 获取布局容器的宽高 */
        int containerWidth = scanContainer.getWidth();
        int containerHeight = scanContainer.getHeight();

        /** 计算最终截取的矩形的左上角顶点x坐标 */
        int x = cropLeft * cameraWidth / containerWidth;
        /** 计算最终截取的矩形的左上角顶点y坐标 */
        int y = cropTop * cameraHeight / containerHeight;

        /** 计算最终截取的矩形的宽度 */
        int width = cropWidth * cameraWidth / containerWidth;
        /** 计算最终截取的矩形的高度 */
        int height = cropHeight * cameraHeight / containerHeight;

        /** 生成最终的截取的矩形 */
        mCropRect = new Rect(x, y, width + x, height + y);
    }

    private int getStatusBarHeight() {
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = Integer.parseInt(field.get(obj).toString());
            return getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
