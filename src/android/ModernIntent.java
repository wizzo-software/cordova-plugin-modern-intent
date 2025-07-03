package com.wizzo.plugins;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Modern Intent Plugin for Cordova
 * Compatible with Android 14.x and modern Cordova versions
 */
public class ModernIntent extends CordovaPlugin {
    
    private static final String TAG = "ModernIntent";
    private static final int REQUEST_CODE_ACTIVITY = 1001;
    
    private CallbackContext onIntentCallback;
    private CallbackContext broadcastCallback;
    private BroadcastReceiver broadcastReceiver;
    private boolean isReceiverRegistered = false;
    
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) 
            throws JSONException {
        
        Log.d(TAG, "Action: " + action);
        
        switch (action) {
            case "getIntent":
                return getIntent(callbackContext);
            case "startActivity":
                return startActivity(args.getJSONObject(0), callbackContext);
            case "startActivityForResult":
                return startActivityForResult(args.getJSONObject(0), callbackContext);
            case "sendBroadcast":
                return sendBroadcast(args.getString(0), args.getJSONObject(1), callbackContext);
            case "registerBroadcastReceiver":
                return registerBroadcastReceiver(args.getJSONObject(0), callbackContext);
            case "unregisterBroadcastReceiver":
                return unregisterBroadcastReceiver(callbackContext);
            case "onIntent":
                return onIntent(callbackContext);
            case "packageExists":
                return packageExists(args.getString(0), callbackContext);
            case "getRealPathFromContentUrl":
                return getRealPathFromContentUrl(args.getString(0), callbackContext);
            case "clearIntentHandler":
                return clearIntentHandler(callbackContext);
            default:
                return false;
        }
    }
    
    /**
     * Get the intent that launched the activity
     */
    private boolean getIntent(CallbackContext callbackContext) {
        try {
            Intent intent = cordova.getActivity().getIntent();
            JSONObject intentData = intentToJSON(intent);
            callbackContext.success(intentData);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error getting intent: " + e.getMessage());
            callbackContext.error("Error getting intent: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Start an activity using an intent
     */
    private boolean startActivity(JSONObject params, CallbackContext callbackContext) {
        try {
            Intent intent = createIntent(params);
            cordova.getActivity().startActivity(intent);
            callbackContext.success();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error starting activity: " + e.getMessage());
            callbackContext.error("Error starting activity: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Start an activity for result
     */
    private boolean startActivityForResult(JSONObject params, CallbackContext callbackContext) {
        try {
            Intent intent = createIntent(params);
            cordova.startActivityForResult(this, intent, REQUEST_CODE_ACTIVITY);
            // Store callback for result
            this.onIntentCallback = callbackContext;
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error starting activity for result: " + e.getMessage());
            callbackContext.error("Error starting activity for result: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Send broadcast intent
     */
    private boolean sendBroadcast(String action, JSONObject extras, CallbackContext callbackContext) {
        try {
            Intent intent = new Intent(action);
            if (extras != null) {
                addExtrasToIntent(intent, extras);
            }
            cordova.getActivity().sendBroadcast(intent);
            callbackContext.success();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error sending broadcast: " + e.getMessage());
            callbackContext.error("Error sending broadcast: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Register broadcast receiver
     */
    private boolean registerBroadcastReceiver(JSONObject filters, CallbackContext callbackContext) {
        try {
            unregisterBroadcastReceiver(null); // Unregister any existing receiver
            
            JSONArray filterActions = filters.getJSONArray("filterActions");
            IntentFilter intentFilter = new IntentFilter();
            
            for (int i = 0; i < filterActions.length(); i++) {
                intentFilter.addAction(filterActions.getString(i));
            }
            
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        JSONObject intentData = intentToJSON(intent);
                        PluginResult result = new PluginResult(PluginResult.Status.OK, intentData);
                        result.setKeepCallback(true);
                        callbackContext.sendPluginResult(result);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error processing broadcast: " + e.getMessage());
                    }
                }
            };
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                cordova.getActivity().registerReceiver(broadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED);
            } else {
                cordova.getActivity().registerReceiver(broadcastReceiver, intentFilter);
            }
            
            isReceiverRegistered = true;
            this.broadcastCallback = callbackContext;
            
            // Keep callback alive
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error registering broadcast receiver: " + e.getMessage());
            callbackContext.error("Error registering broadcast receiver: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Unregister broadcast receiver
     */
    private boolean unregisterBroadcastReceiver(CallbackContext callbackContext) {
        try {
            if (isReceiverRegistered && broadcastReceiver != null) {
                cordova.getActivity().unregisterReceiver(broadcastReceiver);
                isReceiverRegistered = false;
                broadcastReceiver = null;
                broadcastCallback = null;
            }
            
            if (callbackContext != null) {
                callbackContext.success();
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering broadcast receiver: " + e.getMessage());
            if (callbackContext != null) {
                callbackContext.error("Error unregistering broadcast receiver: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Set callback for new intents
     */
    private boolean onIntent(CallbackContext callbackContext) {
        this.onIntentCallback = callbackContext;
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
        return true;
    }
    
    /**
     * Check if package exists
     */
    private boolean packageExists(String packageName, CallbackContext callbackContext) {
        try {
            PackageManager pm = cordova.getActivity().getPackageManager();
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            callbackContext.success("true");
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            callbackContext.success("false");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error checking package exists: " + e.getMessage());
            callbackContext.error("Error checking package exists: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get real path from content URL
     */
    private boolean getRealPathFromContentUrl(String contentUrl, CallbackContext callbackContext) {
        try {
            Uri uri = Uri.parse(contentUrl);
            String realPath = getRealPathFromUri(uri);
            if (realPath != null) {
                callbackContext.success(realPath);
            } else {
                callbackContext.error("Could not resolve real path from URI: " + contentUrl);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error getting real path: " + e.getMessage());
            callbackContext.error("Error getting real path: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Clear intent handler
     */
    private boolean clearIntentHandler(CallbackContext callbackContext) {
        this.onIntentCallback = null;
        if (callbackContext != null) {
            callbackContext.success();
        }
        return true;
    }
    
    /**
     * Get real path from URI - handles different URI schemes
     */
    private String getRealPathFromUri(Uri uri) {
        if (uri == null) return null;
        
        String scheme = uri.getScheme();
        if (scheme == null) return null;
        
        // Handle different URI schemes
        if ("file".equals(scheme)) {
            return uri.getPath();
        } else if ("content".equals(scheme)) {
            return getRealPathFromContentUri(uri);
        }
        
        return null;
    }
    
    /**
     * Get real path from content URI
     */
    private String getRealPathFromContentUri(Uri contentUri) {
        Context context = cordova.getActivity();
        String[] projection = { MediaStore.Images.Media.DATA };
        
        try {
            Cursor cursor = context.getContentResolver().query(contentUri, projection, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                        return cursor.getString(columnIndex);
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not get real path from content URI, trying alternative method");
        }
        
        // Alternative method for newer Android versions
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, contentUri)) {
                return getPathFromDocumentUri(context, contentUri);
            }
        } catch (Exception e) {
            Log.w(TAG, "Document URI method failed");
        }
        
        // Fallback - return the URI string itself
        return contentUri.toString();
    }
    
    /**
     * Get path from document URI (for Android KitKat and above)
     */
    private String getPathFromDocumentUri(Context context, Uri uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return null;
        }
        
        String documentId = DocumentsContract.getDocumentId(uri);
        
        // MediaProvider
        if (isMediaDocument(uri)) {
            String[] split = documentId.split(":");
            String type = split[0];
            
            Uri contentUri = null;
            if ("image".equals(type)) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else if ("video".equals(type)) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            } else if ("audio".equals(type)) {
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            }
            
            String selection = "_id=?";
            String[] selectionArgs = new String[]{split[1]};
            
            return getDataColumn(context, contentUri, selection, selectionArgs);
        }
        
        return null;
    }
    
    /**
     * Check if URI is a MediaProvider document
     */
    private boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
    
    /**
     * Get data column from content resolver
     */
    private String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        final String column = "_data";
        final String[] projection = { column };
        
        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting data column: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Create intent from JSON parameters
     */
    private Intent createIntent(JSONObject params) throws JSONException {
        Intent intent = new Intent();
        
        if (params.has("action")) {
            intent.setAction(params.getString("action"));
        }
        
        if (params.has("url")) {
            intent.setData(Uri.parse(params.getString("url")));
        }
        
        if (params.has("type")) {
            intent.setType(params.getString("type"));
        }
        
        if (params.has("extras")) {
            addExtrasToIntent(intent, params.getJSONObject("extras"));
        }
        
        if (params.has("flags")) {
            intent.setFlags(params.getInt("flags"));
        }
        
        return intent;
    }
    
    /**
     * Add extras to intent from JSON
     */
    private void addExtrasToIntent(Intent intent, JSONObject extras) throws JSONException {
        JSONArray names = extras.names();
        if (names != null) {
            for (int i = 0; i < names.length(); i++) {
                String key = names.getString(i);
                Object value = extras.get(key);
                
                if (value instanceof String) {
                    intent.putExtra(key, (String) value);
                } else if (value instanceof Integer) {
                    intent.putExtra(key, (Integer) value);
                } else if (value instanceof Boolean) {
                    intent.putExtra(key, (Boolean) value);
                } else if (value instanceof Double) {
                    intent.putExtra(key, (Double) value);
                } else if (value instanceof Float) {
                    intent.putExtra(key, (Float) value);
                } else if (value instanceof Long) {
                    intent.putExtra(key, (Long) value);
                }
            }
        }
    }
    
    /**
     * Convert intent to JSON
     */
    private JSONObject intentToJSON(Intent intent) throws JSONException {
        JSONObject json = new JSONObject();
        
        if (intent.getAction() != null) {
            json.put("action", intent.getAction());
        }
        
        if (intent.getData() != null) {
            json.put("data", intent.getData().toString());
        }
        
        if (intent.getType() != null) {
            json.put("type", intent.getType());
        }
        
        json.put("flags", intent.getFlags());
        
        // Handle clip data (for shared content)
        ClipData clipData = intent.getClipData();
        if (clipData != null && clipData.getItemCount() > 0) {
            JSONArray clipItems = new JSONArray();
            for (int i = 0; i < clipData.getItemCount(); i++) {
                ClipData.Item item = clipData.getItemAt(i);
                JSONObject clipItem = new JSONObject();
                
                if (item.getUri() != null) {
                    clipItem.put("uri", item.getUri().toString());
                }
                if (item.getText() != null) {
                    clipItem.put("text", item.getText().toString());
                }
                if (item.getHtmlText() != null) {
                    clipItem.put("htmlText", item.getHtmlText());
                }
                
                clipItems.put(clipItem);
            }
            json.put("clipItems", clipItems);
        }
        
        Bundle extras = intent.getExtras();
        if (extras != null) {
            JSONObject extrasJSON = new JSONObject();
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                if (value != null) {
                    // Handle different data types properly
                    if (value instanceof String) {
                        extrasJSON.put(key, (String) value);
                    } else if (value instanceof Integer) {
                        extrasJSON.put(key, (Integer) value);
                    } else if (value instanceof Boolean) {
                        extrasJSON.put(key, (Boolean) value);
                    } else if (value instanceof Double) {
                        extrasJSON.put(key, (Double) value);
                    } else if (value instanceof Long) {
                        extrasJSON.put(key, (Long) value);
                    } else if (value instanceof Float) {
                        extrasJSON.put(key, (Float) value);
                    } else {
                        extrasJSON.put(key, value.toString());
                    }
                }
            }
            json.put("extras", extrasJSON);
        }
        
        return json;
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE_ACTIVITY && onIntentCallback != null) {
            try {
                JSONObject result = new JSONObject();
                result.put("resultCode", resultCode);
                if (intent != null) {
                    result.put("intent", intentToJSON(intent));
                }
                onIntentCallback.success(result);
            } catch (JSONException e) {
                onIntentCallback.error("Error processing activity result: " + e.getMessage());
            }
            onIntentCallback = null;
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }
    
    @Override
    public void onNewIntent(Intent intent) {
        if (onIntentCallback != null) {
            try {
                JSONObject intentData = intentToJSON(intent);
                PluginResult result = new PluginResult(PluginResult.Status.OK, intentData);
                result.setKeepCallback(true);
                onIntentCallback.sendPluginResult(result);
            } catch (JSONException e) {
                Log.e(TAG, "Error processing new intent: " + e.getMessage());
            }
        }
        super.onNewIntent(intent);
    }
    
    @Override
    public void onDestroy() {
        unregisterBroadcastReceiver(null);
        super.onDestroy();
    }
} 