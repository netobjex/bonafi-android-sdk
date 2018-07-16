# Bonafi SDK

Provides the complete information of an item using the given key and value to the respective item's scanned tag

## Getting Started

To implement the sdk in your project, you only need to copy the bonafisdk.aar file into your projects lib folder add the following line to your build.gradle file

```
api(name:'bonafisdk', ext:'aar')
```

The [bonafisdk.aar](https://git.xqbator.com/bonafi/android-sdk/blob/master/bonafisdk.aar) file is in the root folder of this project after cloning/downloading it.

### Usage

After you've been provided the three basic parameter BASE_URL, PRIVATE_KEY & CLIENT_ID, then there are only two MANDATORY value needed to get information of any given item, which are: KEY & VALUE. 

Example

In manifest

```
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.WAKE_LOCK"/>


```
Then

```
import com.netobjex.bonafisdk.model.TagModel;
import com.netobjex.bonafisdk.services.NetObjexServices;
import com.netobjex.bonafisdk.interfaces.NetObjexWSThread;

String TAG = "";
String VALUE = "";

//GET DATA
NetObjexServices netObjex = new NetObjexServices(BASE_URL, PRIVATE_KEY, CLIENT_ID);
netObjex.getData(this, TAG,VALUE, new NetObjexWSThread() {
    @Override
    public void onFinish(boolean isFound, TagModel data) {
        //isFound is to check if the given parameter matched with any item on the server
        //data contains every information provided from our server about the item with the TAG and its VALUE
    }

    @Override
    public void onError(String errorMsg) {
        
    }
});

//UPLOAD FILES
List<String> paths;
//Add all the absolute path to the list paths and do the next step to upload them e.g. /storage/emulated/0/DCIM/Camera/imagefile.jpg
//Context is the application context e.g. MainActivity.this
//Username and password will be provided. Consult the admin
netObjexServices.bulkUpload(context, username, password, paths, new NetObjexWSCallback() {
    @Override
    public void onResponse(String data) {
        //data is the response from server
        Log.d("TAG", data);
    }
});
```

## Prerequisites

Kindly remember to add internet permission to enable the sdk to access the internet


