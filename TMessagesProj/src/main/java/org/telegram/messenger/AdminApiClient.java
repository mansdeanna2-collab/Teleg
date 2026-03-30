package org.telegram.messenger;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client for communicating with the Telegram Admin Backend Server.
 * Handles user registration, heartbeat, ban checking, report submission,
 * announcement fetching, and configuration retrieval.
 */
public class AdminApiClient {

    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 10000;

    private static volatile AdminApiClient instance;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AdminApiClient() {}

    public static AdminApiClient getInstance() {
        if (instance == null) {
            synchronized (AdminApiClient.class) {
                if (instance == null) {
                    instance = new AdminApiClient();
                }
            }
        }
        return instance;
    }

    /**
     * Register or update user info on admin server.
     */
    public void registerUser(long telegramId, String firstName, String lastName,
                             String username, String phoneNumber, String deviceInfo,
                             String appVersion, ApiCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("telegramId", telegramId);
                body.put("firstName", firstName);
                body.put("lastName", lastName);
                body.put("username", username);
                body.put("phoneNumber", phoneNumber);
                body.put("deviceInfo", deviceInfo);
                body.put("appVersion", appVersion);

                JSONObject result = post("/api/client/register", body);
                if (callback != null) {
                    mainHandler.post(() -> callback.onSuccess(result));
                }
            } catch (Exception e) {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.e("AdminApiClient registerUser error", e);
                }
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * Send heartbeat to admin server and check ban status.
     */
    public void sendHeartbeat(long telegramId, HeartbeatCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject result = post("/api/client/heartbeat?telegramId=" + telegramId, new JSONObject());
                if (result != null && result.has("data")) {
                    JSONObject data = result.getJSONObject("data");
                    String status = data.optString("status", "ACTIVE");
                    String banReason = data.optString("banReason", null);

                    if (callback != null) {
                        mainHandler.post(() -> callback.onResult(status, banReason));
                    }
                }
            } catch (Exception e) {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.e("AdminApiClient heartbeat error", e);
                }
            }
        });
    }

    /**
     * Check if user is banned.
     */
    public void checkUserStatus(long telegramId, HeartbeatCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject result = get("/api/client/user-status?telegramId=" + telegramId);
                if (result != null && result.has("data")) {
                    JSONObject data = result.getJSONObject("data");
                    String status = data.optString("status", "ACTIVE");
                    String banReason = data.optString("banReason", null);

                    if (callback != null) {
                        mainHandler.post(() -> callback.onResult(status, banReason));
                    }
                }
            } catch (Exception e) {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.e("AdminApiClient checkStatus error", e);
                }
                if (callback != null) {
                    mainHandler.post(() -> callback.onResult("ACTIVE", null));
                }
            }
        });
    }

    /**
     * Submit a moderation report.
     */
    public void submitReport(long reporterId, String reporterName,
                             long reportedUserId, String reportedUserName,
                             String reportType, String contentType,
                             String description, ApiCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("reporterId", reporterId);
                body.put("reporterName", reporterName);
                body.put("reportedUserId", reportedUserId);
                body.put("reportedUserName", reportedUserName);
                body.put("reportType", reportType);
                body.put("contentType", contentType);
                body.put("description", description);

                JSONObject result = post("/api/client/report", body);
                if (callback != null) {
                    mainHandler.post(() -> callback.onSuccess(result));
                }
            } catch (Exception e) {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.e("AdminApiClient submitReport error", e);
                }
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * Get active announcements from the admin server.
     */
    public void getAnnouncements(AnnouncementsCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject result = get("/api/client/announcements");
                if (result != null && result.has("data")) {
                    JSONArray announcements = result.getJSONArray("data");
                    if (callback != null) {
                        mainHandler.post(() -> callback.onResult(announcements));
                    }
                }
            } catch (Exception e) {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.e("AdminApiClient getAnnouncements error", e);
                }
            }
        });
    }

    /**
     * Get a configuration value from the admin server.
     */
    public void getConfig(String key, ConfigCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject result = get("/api/client/config?key=" + java.net.URLEncoder.encode(key, "UTF-8"));
                if (result != null && result.has("data")) {
                    JSONObject data = result.getJSONObject("data");
                    String value = data.optString("value", null);
                    if (callback != null) {
                        mainHandler.post(() -> callback.onResult(key, value));
                    }
                }
            } catch (Exception e) {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.e("AdminApiClient getConfig error", e);
                }
            }
        });
    }

    // ====== HTTP methods ======

    private JSONObject get(String path) throws Exception {
        String serverUrl = AdminConfig.getAdminServerUrl();
        if (TextUtils.isEmpty(serverUrl)) return null;

        URL url = new URL(serverUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("Content-Type", "application/json");

        return readResponse(conn);
    }

    private JSONObject post(String path, JSONObject body) throws Exception {
        String serverUrl = AdminConfig.getAdminServerUrl();
        if (TextUtils.isEmpty(serverUrl)) return null;

        URL url = new URL(serverUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        return readResponse(conn);
    }

    private JSONObject readResponse(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        BufferedReader reader;
        if (code >= 200 && code < 300) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            java.io.InputStream errorStream = conn.getErrorStream();
            if (errorStream == null) {
                conn.disconnect();
                throw new Exception("HTTP error " + code + " with no response body");
            }
            reader = new BufferedReader(new InputStreamReader(errorStream));
        }

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        conn.disconnect();

        return new JSONObject(sb.toString());
    }

    // ====== Callbacks ======

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    public interface HeartbeatCallback {
        void onResult(String status, String banReason);
    }

    public interface AnnouncementsCallback {
        void onResult(JSONArray announcements);
    }

    public interface ConfigCallback {
        void onResult(String key, String value);
    }
}
