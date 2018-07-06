package com.netobjex.bonafisdk.services;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.netobjex.bonafisdk.interfaces.NetObjexWSCallback;
import com.netobjex.bonafisdk.interfaces.NetObjexWSThread;
import com.netobjex.bonafisdk.model.TagModel;
import com.netobjex.bonafisdk.utils.MQTTHelper;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class NetObjexServices {
    private String baseUrl;
    private String privateKey;
    private String clientId;
    public String mqttServerUri;
    public String mqttUsername;
    public String mqttPassword;

    public NetObjexServices(String mqttServerUrl, String mqttUsername, String mqttPassword, String baseUrl, String privateKey, String clientId) {
        this.baseUrl = baseUrl;
        this.privateKey = privateKey;
        this.clientId = clientId;
        this.mqttServerUri = mqttServerUrl;
        this.mqttPassword = mqttPassword;
        this.mqttUsername = mqttUsername;
    }

    private synchronized void getToken(Context context, NetObjexWSCallback callback) {
        final String TOKEN_ACTION = "/token";
        JSONObject dataObject = new JSONObject();
        try {
            dataObject.put("privateKey", privateKey);
            dataObject.put("clientId", clientId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        callPostWS(context, TOKEN_ACTION, dataObject.toString(), callback);
    }

    private synchronized void login(Context context, NetObjexWSCallback callback) {
        JSONObject dataObject = new JSONObject();
        try {
            dataObject.put("username", "mac@netobjex.com");
            dataObject.put("password", "123456");
            dataObject.put("remember", true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        callPostWS(context, null, dataObject.toString(), callback, true);
    }

    public synchronized void getData(final Context context, final String name, final String value, final NetObjexWSThread callback) {
        if (mqttServerUri != null && !mqttServerUri.contains(".netobjex.com")) {
            sendErrorCallback(callback, "Unrecognized MQTT Base Url");
            return;
        }
        if (callback == null || TextUtils.isEmpty(name) || TextUtils.isEmpty(value)) return;
        getToken(context, new NetObjexWSCallback() {
            @Override
            public void onResponse(String data) {
                if (data == null || !data.contains("token")) {
                    sendErrorCallback(callback, "Internal error!");
                    return;
                }
                try {
                    JSONObject res = new JSONObject(data);
                    String token = res.getString("token");
                    String action = getDataAction(name, value);
                    callPostWS(context, action, token, callback);
                    new MQTTHelper(context, mqttServerUri, mqttUsername, mqttPassword);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static String getDataAction(String name, String value) {
        final String DATA_ACTION = "/getDigitalAssetsByAttributeValue";
        return DATA_ACTION + "?name=" + name + "&value=" + value;
    }

    private void sendErrorCallback(NetObjexWSThread callback, String errorMsg) {
        callback.onFinish(false, null);
        callback.onError(errorMsg);
    }

    private void callPostWS(Context context, String apiURL, String token, NetObjexWSThread callback) {
        callService(context, apiURL, null, false, callback, null, "GET", token, false);
    }

    private void callPostWS(Context context, String apiURL, String data, NetObjexWSCallback callback) {
        callService(context, apiURL, data, true, null, callback, "POST", null, false);
    }

    private void callPostWS(Context context, String apiURL, String data, NetObjexWSCallback callback, boolean isLogin) {
        callService(context, apiURL, data, true, null, callback, "POST", null, isLogin);
    }

    private void callService(final Context context, final String action, final String requestedTag, final boolean isTokenRequest, final NetObjexWSThread callback, final NetObjexWSCallback tokenCallback, final String type, final String token, final boolean isLogin) {
        Thread callThread = new Thread() {
            @Override
            public void run() {
                final String WEBSERVICE_URL = "/api/PublicAPI";
                final String TOKEN_ACTION = "/api/users/authenticate";
                final DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpParams params = httpClient.getParams();
                HttpConnectionParams.setConnectionTimeout(params, 15000);
                HttpConnectionParams.setSoTimeout(params, 20000);
                HttpProtocolParams.setUseExpectContinue(httpClient.getParams(), true);
                int statusCode = 0;
                StringBuilder strBuffer = new StringBuilder();
                InputStream inputStream = null;
                String response = "";
                try {
                    HttpRequestBase httpCall = null;
                    if (type.equalsIgnoreCase("POST")) {
                        if (!isLogin)
                        httpCall = new HttpPost(baseUrl + WEBSERVICE_URL + action);
                        else httpCall = new HttpPost(baseUrl+TOKEN_ACTION);
                        if (requestedTag != null) {
                            StringEntity se;
                            se = new StringEntity(requestedTag, "UTF-8");
                            se.setContentType("application/json");
                            ((HttpPost) httpCall).setEntity(se);
                        }
                    } else if (type.equalsIgnoreCase("GET")) {
                        httpCall = new HttpGet(baseUrl + WEBSERVICE_URL + action);
                    }
                    httpCall.setHeader("Accept", "application/json");
                    httpCall.setHeader("Content-Type", "application/json");
                    if (!isTokenRequest && token != null) {
                        httpCall.setHeader("X-Oauth-Token", token);
                        httpCall.setHeader("X-API-AUTH-KEY", privateKey);
                        httpCall.setHeader("X-DEVICE-ID", getDeviceId(context));
                    }
                    HttpResponse httpResponse = httpClient.execute(httpCall);
                    HttpEntity entity = httpResponse.getEntity();
                    inputStream = entity.getContent();
                    int len = (int) httpResponse.getEntity().getContentLength();
                    byte[] returnData = new byte[1024];
                    len = 0;
                    while (-1 != (len = inputStream.read(returnData))) {
                        strBuffer.append(new String(returnData, 0, len, "UTF-8"));
                    }
                    if (inputStream != null)
                        inputStream.close();
                    response = strBuffer.toString();
                    statusCode = httpResponse.getStatusLine().getStatusCode();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    response = "";
                    statusCode = 0;
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                    response = "";
                    statusCode = 0;
                } catch (IOException e) {
                    e.printStackTrace();
                    response = "";
                    statusCode = 0;
                } catch (Exception e) {
                    e.printStackTrace();
                    response = "";
                    statusCode = 0;
                } finally {
                    Message msgObj = handler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putInt("statusCode", statusCode);
                    b.putString("response", response);
                    msgObj.setData(b);
                    handler.sendMessage(msgObj);
                }
            }

            @SuppressLint("HandlerLeak")
            private final Handler handler = new Handler() {
                public void handleMessage(Message msg) {
                    String aResponse = msg.getData().getString("response");
                    Log.d("TAG_D", aResponse);
                    if (isTokenRequest) {
                        if ((null != aResponse)) {
                            if (tokenCallback != null) {
                                tokenCallback.onResponse(aResponse);
                            }
                        } else {
                            if (tokenCallback != null) {
                                tokenCallback.onResponse(null);
                            }
                        }
                    } else {
                        TagModel model = new TagModel();
                        if (aResponse != null) {
                            try {
                                JSONArray jsonArray = new JSONArray(aResponse);
                                boolean isFound = false;
                                if (jsonArray.length() > 0) {
                                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                                    model.setTag(jsonObject.getString("TAG"));
                                    model.setIdentification(jsonObject.getString("identification"));
                                    model.setCost(jsonObject.getDouble("cost"));
                                    model.setManufacturer(jsonObject.getString("manufacture"));
                                    model.setSerialNo(jsonObject.getString("serial Number"));
                                    model.setDateOfManufacture(jsonObject.getString("Date of Manufacture"));
                                    model.setAuthorizedStore(jsonObject.getString("Authorized STORE"));
                                    model.setPhone(jsonObject.getString("phone"));
                                    model.setDateOfFirstArrivalAtStore(jsonObject.getString("Date of First Arrival at Store"));
                                    model.setDateOfFirstSold(jsonObject.getString("Date of First Sold"));
                                    model.setOriginalOwnerRegistration(jsonObject.getString("Original Owner Registration"));
                                    model.setEmail(jsonObject.getString("email"));
                                    model.setGift(jsonObject.getString("gift"));
                                    isFound = true;
                                }
                                if (callback != null) {
                                    callback.onFinish(isFound, model);
                                    if (!isFound)
                                        callback.onError("Item not found");
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                if (callback != null) {
                                    sendErrorCallback(callback, "Internal error!");
                                }
                            }
                        } else {
                            if (callback != null) {
                                sendErrorCallback(callback, "Item not found");
                            }
                        }
                    }
                }
            };
        };
        callThread.start();
    }

    private String getDeviceId(Context context) {
        String id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.d("TAG_D", id);
        return id;
    }

    public void bulkUpload(final Context context, final List<String> filesPath, final NetObjexWSCallback callback) {
        login(context, new NetObjexWSCallback() {
            @Override
            public void onResponse(String data) {
                if (data == null || !data.contains("token")) {
                    callback.onResponse("Internal Error!");
                    return;
                }
                try {
                    JSONObject res = new JSONObject(data);
                    final String token = res.getString("token");
                    Log.d("TAG_D", token);
                    Thread thread = new Thread(){
                        @Override
                        public void run() {
                            String responseStr = null;
                            List<File> files = new ArrayList<>();
                            for (String path : filesPath) {
                                Log.d("TAG_D", path);
                                File each = new File(path);
                                files.add(each);
                            }
                            final String WEBSERVICE_URL = baseUrl+"/api/files/upload?type=COUPON_IMAGE";
                            try {
                                HttpClient client = new DefaultHttpClient();
                                HttpParams params = client.getParams();
                                HttpConnectionParams.setConnectionTimeout(params, 1500000);
                                HttpConnectionParams.setSoTimeout(params, 2000000);
                                HttpPost post = new HttpPost(WEBSERVICE_URL);
                                MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                                for (File file : files) {
                                    byte bytes[] = new byte[(int) file.length()];
                                    ContentBody contentPart = new ByteArrayBody(bytes, "image/jpeg", file.getName());
                                    reqEntity.addPart(file.getName(), contentPart);
                                }
                                reqEntity.addPart("userId", new StringBody(clientId));
                                post.setEntity(reqEntity);
                                post.setHeader("X-Auth-Key", token);
                                post.setHeader("X-API-AUTH-KEY", privateKey);
                                HttpResponse response = client.execute(post);
                                HttpEntity resEntity = response.getEntity();
                                responseStr = EntityUtils.toString(resEntity);
                                if (resEntity != null) {
                                    Log.i("TAG_D", responseStr);
                                }
                            } catch (Exception ex) {
                                Log.e("TAG_D", "error: " + ex.getMessage(), ex);
                            } finally {
                                Message msgObj = handler.obtainMessage();
                                Bundle b = new Bundle();
                                b.putString("response", responseStr);
                                msgObj.setData(b);
                                handler.sendMessage(msgObj);
                            }
                        }
                        @SuppressLint("HandlerLeak")
                        private final Handler handler = new Handler() {
                            public void handleMessage(Message msg) {
                                String aResponse = msg.getData().getString("response");
                                if (aResponse != null) {
                                    Log.d("TAG_D", aResponse);
                                    callback.onResponse(aResponse);
                                } else callback.onResponse("Internal Error!");
                            }
                        };
                    };
                    thread.start();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
