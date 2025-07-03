var exec = require('cordova/exec');

/**
 * Modern Intent Plugin
 * Compatible with Cordova Android 14.x
 */
var ModernIntent = {
    
    /**
     * Get the intent that launched the activity
     */
    getIntent: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, "ModernIntent", "getIntent", []);
    },
    
    /**
     * Start an activity using an intent
     */
    startActivity: function(params, successCallback, errorCallback) {
        exec(successCallback, errorCallback, "ModernIntent", "startActivity", [params]);
    },
    
    /**
     * Start an activity for result
     */
    startActivityForResult: function(params, successCallback, errorCallback) {
        exec(successCallback, errorCallback, "ModernIntent", "startActivityForResult", [params]);
    },
    
    /**
     * Send broadcast intent
     */
    sendBroadcast: function(action, extras, successCallback, errorCallback) {
        exec(successCallback, errorCallback, "ModernIntent", "sendBroadcast", [action, extras]);
    },
    
    /**
     * Register broadcast receiver
     */
    registerBroadcastReceiver: function(filters, successCallback, errorCallback) {
        exec(successCallback, errorCallback, "ModernIntent", "registerBroadcastReceiver", [filters]);
    },
    
    /**
     * Unregister broadcast receiver
     */
    unregisterBroadcastReceiver: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, "ModernIntent", "unregisterBroadcastReceiver", []);
    },
    
    /**
     * Set callback for new intents
     */
    onIntent: function(callback) {
        exec(callback, null, "ModernIntent", "onIntent", []);
    },
    
    /**
     * Check if package exists
     */
    packageExists: function(packageName, successCallback, errorCallback) {
        exec(successCallback, errorCallback, "ModernIntent", "packageExists", [packageName]);
    },
    
    /**
     * Get real path from content URL (for file access)
     */
    getRealPathFromContentUrl: function(contentUrl, successCallback, errorCallback) {
        exec(successCallback, errorCallback, "ModernIntent", "getRealPathFromContentUrl", [contentUrl]);
    },
    
    /**
     * Set new intent handler (compatible with old plugin)
     */
    setNewIntentHandler: function(callback) {
        if (callback) {
            this.onIntent(callback);
        } else {
            // Clear handler
            exec(null, null, "ModernIntent", "clearIntentHandler", []);
        }
    },
    
    /**
     * Get cordova intent (compatible with old plugin)
     */
    getCordovaIntent: function(successCallback, errorCallback) {
        this.getIntent(successCallback, errorCallback);
    },
    
    // Constants
    ACTION_SEND: "android.intent.action.SEND",
    ACTION_VIEW: "android.intent.action.VIEW",
    ACTION_PICK: "android.intent.action.PICK",
    ACTION_GET_CONTENT: "android.intent.action.GET_CONTENT",
    ACTION_CALL: "android.intent.action.CALL",
    ACTION_SENDTO: "android.intent.action.SENDTO",
    
    EXTRA_TEXT: "android.intent.extra.TEXT",
    EXTRA_SUBJECT: "android.intent.extra.SUBJECT",
    EXTRA_STREAM: "android.intent.extra.STREAM",
    EXTRA_EMAIL: "android.intent.extra.EMAIL"
};

module.exports = ModernIntent; 