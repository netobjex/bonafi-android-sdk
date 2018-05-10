package com.netobjex.bonafisdk.interfaces;


import com.netobjex.bonafisdk.model.TagModel;

public interface NetObjexWSThread {
    void onFinish(boolean isFound, TagModel data);

    void onError(String errorMsg);
}
