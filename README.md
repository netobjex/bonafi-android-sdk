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

```
import com.netobjex.bonafisdk.interfaces.NetObjexWSThread;
import com.netobjex.bonafisdk.model.TagModel;
import com.netobjex.bonafisdk.services.NetObjexServices;

String TAG = "";
String VALUE = "";

NetObjexServices netObjexServices = new NetObjexServices(BASE_URL, PRIVATE_KEY, CLIENT_ID);
netObjexServices.getData(TAG,VALUE, new NetObjexWSThread() {
    @Override
    public void onFinish(boolean isFound, TagModel data) {
        //isFound is to check if the given parameter matched with any item on the server
        //data contains every information provided from our server about the item with the TAG and its VALUE
    }

    @Override
    public void onError(String errorMsg) {
        
    }
});
```

## Prerequisites

Kindly remember to add internet permission to enable the sdk to access the internet. Pretty easy to use


