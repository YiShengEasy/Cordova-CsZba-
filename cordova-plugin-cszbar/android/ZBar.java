package org.cloudsky.cordovaPlugins;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ZBar extends CordovaPlugin {

    // Configuration ---------------------------------------------------

    private static int SCAN_CODE = 1;


    // State -----------------------------------------------------------

    private boolean isInProgress = false;
    private CallbackContext scanCallbackContext;


    // Plugin API ------------------------------------------------------

    @Override
    public boolean execute (String action, JSONArray args, CallbackContext callbackContext)
    throws JSONException
    {
        if(action.equals("scan")) {
            if(isInProgress) {
                callbackContext.error("存在多个扫描进程!");
            } else {
                isInProgress = true;
                scanCallbackContext = callbackContext;
                JSONObject params = args.optJSONObject(0);

                Context appCtx = cordova.getActivity().getApplicationContext();
                Intent scanIntent = new Intent(appCtx, ZBarScannerActivity.class);
                scanIntent.putExtra(ZBarScannerActivity.EXTRA_PARAMS, params.toString());
                cordova.startActivityForResult(this, scanIntent, SCAN_CODE);
            }
            return true;
        } else {
            return false;
        }
    }


    // External results handler ----------------------------------------

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent result)
    {
        if(requestCode == SCAN_CODE) {
            switch(resultCode) {
                case Activity.RESULT_OK:
                    String barcodeValue = result.getStringExtra(ZBarScannerActivity.EXTRA_QRVALUE);
                    scanCallbackContext.success(barcodeValue);
                    break;
                case Activity.RESULT_CANCELED:
                    break;

                default:
                    scanCallbackContext.error("发生未知错误,请重试");
            }
            isInProgress = false;
            scanCallbackContext = null;
        }
    }
}
