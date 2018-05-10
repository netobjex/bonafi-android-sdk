# Bonafi SDK

Provides the complete information of an item using the given key and value to the respective item's scanned tag

## Getting Started

To implement the sdk in your project, you only need to copy the bonafisdk.aar file into your projects lib folder add the following line to your build.gradle file

```
api(name:'bonafisdk', ext:'aar')
```

The [bonafisdk.aar](https://git.xqbator.com/bonafi/android-sdk/blob/master/bonafisdk.iml) file is in the root folder of this project after cloning/downloading it.

### Usage

There are two MANDATORY value needed to get information of any given item, which are: Key and Value. 

Example

```
import com.netobjex.bonafisdk.interfaces.NetObjexWSThread;
import com.netobjex.bonafisdk.model.TagModel;
import com.netobjex.bonafisdk.services.NetObjexServices;

    String TAG = "TAG";
    String VALUE = "XE2P";

    NetObjexServices.getData(TAG,VALUE, new NetObjexWSThread() {
        @Override
        public void onFinish(boolean isFound, TagModel data) {
            //data contains every information provided from our server about the item with the TAG and its VALUE
            //isFound is to check if the given parameter matched with any item on the server
        }

        @Override
        public void onError(String errorMsg) {

        }
    });
```

## Prerequisites

Kindly remember to add internet permission to enable the sdk to access the internet. Pretty easy to use


