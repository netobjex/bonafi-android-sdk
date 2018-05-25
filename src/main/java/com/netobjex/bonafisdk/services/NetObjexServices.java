package com.netobjex.bonafisdk.services;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.netobjex.bonafisdk.interfaces.NetObjexWSThread;
import com.netobjex.bonafisdk.interfaces.NetObjexWSToken;
import com.netobjex.bonafisdk.model.TagModel;
import com.netobjex.bonafisdk.utils.MQTTHelper;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

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

    private synchronized void getToken(Context context, NetObjexWSToken callback) {
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

    public synchronized void getData(final Context context, final String name, final String value, final NetObjexWSThread callback) {
        if (callback == null || TextUtils.isEmpty(name) || TextUtils.isEmpty(value)) return;
        getToken(context, new NetObjexWSToken() {
            @Override
            public void onToken(String data) {
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
        callService(context, apiURL, null, false, callback, null, "GET", token);
    }

    private void callPostWS(Context context, String apiURL, String data, NetObjexWSToken callback) {
        callService(context, apiURL, data, true, null, callback, "POST", null);
    }

    private void callService(final Context context, final String action, final String requestedTag, final boolean isTokenRequest, final NetObjexWSThread callback, final NetObjexWSToken tokenCallback, final String type, final String token) {
        Thread callThread = new Thread() {
            @Override
            public void run() {
                final String WEBSERVICE_URL = "/api/PublicAPI";
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
                        httpCall = new HttpPost(baseUrl + WEBSERVICE_URL + action);
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
                                tokenCallback.onToken(aResponse);
                            }
                        } else {
                            if (tokenCallback != null) {
                                tokenCallback.onToken(null);
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
}