package com.gmail.ndraiman.wifipasswords.extras;

import com.gmail.ndraiman.wifipasswords.pojo.WifiEntry;

import java.util.ArrayList;

public interface WifiListLoadedListener {

    public void onWifiListLoaded(ArrayList<WifiEntry> listWifi);
}
