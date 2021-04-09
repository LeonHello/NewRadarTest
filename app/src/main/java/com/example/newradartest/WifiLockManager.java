package com.example.newradartest;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;


public class WifiLockManager {

    private final static String TAG = "WifiLockManager";

    // 定义WifiManager对象
    private WifiManager mWifiManager;
    // 定义一个WifiLock
    private WifiManager.WifiLock mWifiLock;

    public WifiLockManager(Context context) {
        // 取得WifiManager对象
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // 第一种方式
        // createWifiLock("WifiLockManager");
        // 第二种方式
        // 注意，在createWifiLock的时候，一定要注意传WifiManager.WIFI_MODE_FULL_HIGH_PERF，否则的话在有些手机上貌似不起作用。不过估计对手机性能有所损耗
        createWifiLock("WifiLocKManager", WifiManager.WIFI_MODE_FULL_HIGH_PERF);
    }


    /**
     *      * 创建一个WifiLock
     *      *
     *      * @param localName 名称
     *      * @param lockType
     *      *                  WIFI_MODE_FULL == 1 <br/>
     *      *                  扫描，自动的尝试去连接一个曾经配置过的点<br />
     *      *                  WIFI_MODE_SCAN_ONLY == 2 <br/>
     *      *                  只剩下扫描<br />
     *      *                  WIFI_MODE_FULL_HIGH_PERF = 3 <br/>
     *      *                  在第一种模式的基础上，保持最佳性能<br />
     *      
     */
    public void createWifiLock(String localName, int lockType) {
        mWifiLock = mWifiManager.createWifiLock(lockType, localName);
    }


    /**
     *      * 创建一个WifiLock
     *      *
     *      * @param localName 名称
     *      
     */
    public void createWifiLock(String localName) {
        mWifiLock = mWifiManager.createWifiLock(localName);
    }


    /**
     *      * 锁定WifiLock
     *      
     */
    public void acquireWifiLock() {
        mWifiLock.acquire();
        Log.i(TAG, "锁定WifiLock");
    }


    /**
     *      * 解锁WifiLock
     *      
     */
    public void releaseWifiLock() {
        // 判断时候锁定
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
            Log.i(TAG, "解锁WifiLock");
        }
    }


}