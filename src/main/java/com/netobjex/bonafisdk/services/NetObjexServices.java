package com.netobjex.bonafisdk.services;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.netobjex.bonafisdk.BuildConfig;
import com.netobjex.bonafisdk.interfaces.NetObjexWSThread;
import com.netobjex.bonafisdk.interfaces.NetObjexWSToken;
import com.netobjex.bonafisdk.model.TagModel;

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
    private static final String WEBSERVICE_URL = "/api/PublicAPI";
    private static final String TOKEN_ACTION = "/token";
    private static final String DATA_ACTION = "/getDigitalAssetsByAttributeValue";

    private static synchronized void getToken(Context context, NetObjexWSToken callback) {
        JSONObject dataObject = new JSONObject();
        try {
            dataObject.put("privateKey", BuildConfig.PRIVATE_KEY);
            dataObject.put("clientId", BuildConfig.CLIENT_ID);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        callPostWS(context, TOKEN_ACTION, dataObject.toString(), callback);
    }

    public static synchronized void getData(final Context context, final String name, final String value, final NetObjexWSThread callback) {
        if (callback == null || TextUtils.isEmpty(name) || TextUtils.isEmpty(value)) return;
        getToken(context, new NetObjexWSToken() {
            @Override
            public void onToken(String data) {
                Log.d("TAG_D", data);
                if (data == null || !data.contains("token")) {
                    callback.onFinish(false, null);
                    callback.onError("Internal error!");
                    return;
                }
                try {
                    JSONObject res = new JSONObject(data);
                    String token = res.getString("token");
                    String action = getDataAction(name, value);
                    callPostWS(context, action, token, callback);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static String getDataAction(String name, String value) {
        return DATA_ACTION + "?name=" + name + "&value=" + value;
    }

    private static void callPostWS(Context context, final String apiURL, String token, final NetObjexWSThread callback) {
        callService(context, apiURL, null, false, callback, null, "GET", token);
    }

    private static void callPostWS(Context context, final String apiURL, final String data, final NetObjexWSToken callback) {
        callService(context, apiURL, data, true, null, callback, "POST", null);
    }

    private static void callService(final Context context, final String action, final String requestedTag, final boolean isTokenRequest, final NetObjexWSThread callback, final NetObjexWSToken tokenCallback, final String type, final String token) {
        Thread callThread = new Thread() {
            @Override
            public void run() {
                final String DOMAIN_NAME = BuildConfig.BASE_URL;
                final String PRIVATE_KEY = BuildConfig.PRIVATE_KEY;
                Log.d("TAG_D",DOMAIN_NAME+" - "+PRIVATE_KEY);
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
                        httpCall = new HttpPost(DOMAIN_NAME + WEBSERVICE_URL + action);
                        if (requestedTag != null) {
                            StringEntity se;
                            se = new StringEntity(requestedTag, "UTF-8");
                            se.setContentType("application/json");
                            ((HttpPost) httpCall).setEntity(se);
                        }
                    } else if (type.equalsIgnoreCase("GET")) {
                        httpCall = new HttpGet(DOMAIN_NAME + WEBSERVICE_URL + action);
                    }
                    httpCall.setHeader("Accept", "application/json");
                    httpCall.setHeader("Content-Type", "application/json");
                    if (!isTokenRequest && token != null) {
                        httpCall.setHeader("X-Oauth-Token", token);
                        httpCall.setHeader("X-API-AUTH-KEY", PRIVATE_KEY);
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
                    Log.d("TAG_D.", aResponse);
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
                                if (jsonArray.length() == 0) return;
                                JSONObject jsonObject = jsonArray.getJSONObject(0);
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
                            } catch (JSONException e) {
                                e.printStackTrace();
                                if (callback != null) {
                                    callback.onFinish(false, null);
                                    callback.onError("Internal Error!\nError: " + e.getMessage());
                                }
                                return;
                            }
                            if (callback != null) {
                                callback.onFinish(true, model);
                            }
                        } else {
                            if (callback != null) {
                                callback.onFinish(false, null);
                            }
                        }
                    }
                }
            };
        };
        callThread.start();
    }
}