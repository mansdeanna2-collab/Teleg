package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Configuration for the Admin Backend Server connection.
 * Manages the admin server URL and related settings.
 */
public class AdminConfig {

    private static final String PREF_NAME = "admin_config";
    private static final String KEY_SERVER_URL = "admin_server_url";
    private static final String KEY_ENABLED = "admin_enabled";
    private static final String KEY_HEARTBEAT_INTERVAL = "heartbeat_interval_ms";

    /**
     * Default admin server URL. Change this to your production server address.
     * Format: http(s)://host:port (no trailing slash)
     */
    private static final String DEFAULT_SERVER_URL = "http://10.0.2.2:8080";

    private static final boolean DEFAULT_ENABLED = true;
    private static final long DEFAULT_HEARTBEAT_INTERVAL = 300000; // 5 minutes

    /**
     * Get the admin server base URL.
     */
    public static String getAdminServerUrl() {
        try {
            SharedPreferences prefs = ApplicationLoader.applicationContext
                    .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            return prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL);
        } catch (Exception e) {
            return DEFAULT_SERVER_URL;
        }
    }

    /**
     * Set the admin server URL.
     */
    public static void setAdminServerUrl(String url) {
        try {
            SharedPreferences prefs = ApplicationLoader.applicationContext
                    .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_SERVER_URL, url).apply();
        } catch (Exception e) {
            if (BuildVars.LOGS_ENABLED) {
                FileLog.e("AdminConfig setUrl error", e);
            }
        }
    }

    /**
     * Check if admin server communication is enabled.
     */
    public static boolean isEnabled() {
        try {
            SharedPreferences prefs = ApplicationLoader.applicationContext
                    .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            return prefs.getBoolean(KEY_ENABLED, DEFAULT_ENABLED);
        } catch (Exception e) {
            return DEFAULT_ENABLED;
        }
    }

    /**
     * Enable or disable admin server communication.
     */
    public static void setEnabled(boolean enabled) {
        try {
            SharedPreferences prefs = ApplicationLoader.applicationContext
                    .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
        } catch (Exception e) {
            if (BuildVars.LOGS_ENABLED) {
                FileLog.e("AdminConfig setEnabled error", e);
            }
        }
    }

    /**
     * Get the heartbeat interval in milliseconds.
     */
    public static long getHeartbeatInterval() {
        try {
            SharedPreferences prefs = ApplicationLoader.applicationContext
                    .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            return prefs.getLong(KEY_HEARTBEAT_INTERVAL, DEFAULT_HEARTBEAT_INTERVAL);
        } catch (Exception e) {
            return DEFAULT_HEARTBEAT_INTERVAL;
        }
    }
}
