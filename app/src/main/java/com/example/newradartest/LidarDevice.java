package com.example.newradartest;

import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.example.newradartest.CosSinUtil.*;


public class LidarDevice {

    private final static String TAG = "LidarDevice";

    /**
     * socket 长连接
     */
    private Socket socket;
    /**
     * 输出流
     */
    private BufferedWriter writer;
    /**
     * 输入流
     */
    private BufferedReader reader;

    /**
     * 是否连接
     */
    private boolean isConnected;
    /**
     * 是否开始数据传输
     */
    private boolean isStreamed;
    /**
     * 扫描频率
     */
    private int frequency;

    /**
     * 线程管理池
     */
    private ExecutorService mThreadPool;
    /**
     * 主线程handler
     */
    private Handler mHandler;

    /**
     * a frame has 8 blocks, a block has 96 ranges (30Hz for example), frame.length = 8 * 96
     */
    // private ArrayList<Integer> frame;
    private ArrayList<Integer> frameDistanceCos;
    private ArrayList<Integer> frameDistanceSin;
    private double[] cosUtil;
    private double[] sinUtil;

    /**
     * 当前扫描数据对应的block
     */
    private int block;


    public boolean isConnected() {
        return isConnected;
    }

    private void setConnected(boolean connected) {
        isConnected = connected;
    }

    private void setStreamed(boolean streamed) {
        isStreamed = streamed;
    }

    private void setFrequency(int fre) {
        frequency = fre;
    }

    public int getFrequency() {
        return frequency;
    }

    /**
     * 构造函数
     */
    public LidarDevice(Handler handler) {

        socket = null;
        writer = null;
        reader = null;

        setConnected(false);
        setStreamed(false);
        setFrequency(-1);

        int cpuNumbers = Runtime.getRuntime().availableProcessors();
        // 根据CPU数目初始化线程池
        mThreadPool = Executors.newFixedThreadPool(cpuNumbers * 5);

        mHandler = handler;

        // frame = new ArrayList<>();
        frameDistanceCos = new ArrayList<>();
        frameDistanceSin = new ArrayList<>();
        cosUtil = cos4;
        sinUtil = sin4;
        block = -1;

    }

    /**
     * 连接设备
     *
     * @param IPAdr
     * @param PORT
     */
    public void connect(String IPAdr, int PORT) {
        try {
            mThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    if (startTcpConnection(IPAdr, PORT)) {
                        setConnected(true);
                        Log.i(TAG, "设备连接成功");
                        // 雷达连接时一直处理输入流
                        while (isConnected)
                            handleReader();
                    } else {
                        setConnected(false);
                        Log.e(TAG, "设备连接失败");
                    }
                }
            });

            TimeUnit.MILLISECONDS.sleep(500);
            getScanFrequency();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * 尝试建立tcp连接
     *
     * @param IPAdr
     * @param PORT
     */
    private boolean startTcpConnection(String IPAdr, int PORT) {
        try {
            if (socket == null) {
                /* 建立socket */
                socket = new Socket(IPAdr, PORT);
                Log.i(TAG, "TCP连接创建成功");
            }
            /* 输出流 */
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            /* 输入流 */
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }


    /**
     * 断开设备连接
     */
    public void disconnect() {
        try {

            stopStreaming();

            if (writer != null) {
                writer.close();
                writer = null;
            }
            if (reader != null) {
                reader.close();
                reader = null;
            }
            if (socket != null) {
                socket.shutdownInput();
                socket.shutdownOutput();
                socket.getInputStream().close();
                socket.getOutputStream().close();
                socket.close();
                socket = null;
            }
            if (mThreadPool != null) {
                mThreadPool.shutdown();
                mThreadPool = null;
            }

            setConnected(false);
            Log.i(TAG, "成功断开设备连接");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 启动数据传输
     */
    public void startStreaming() {
        if (isConnected && !isStreamed) {
            mThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    String s = "{\"jsonrpc\":\"2.0\",\"method\":\"scan/startStreaming\",\"id\":\"startStreaming\"}" + "\r\n";
                    try {
                        writer.write(s);
                        writer.flush();
                        Log.i(TAG, "已发送启动数据传输命令: " + s);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     * 停止数据传输
     */
    public void stopStreaming() {
        if (isConnected && isStreamed) {
            mThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    String s = "{\"jsonrpc\":\"2.0\",\"method\":\"scan/stopStreaming\",\"id\":\"stopStreaming\"}" + "\r\n";
                    try {
                        writer.write(s);
                        writer.flush();
                        Log.i(TAG, "已发送停止数据传输命令: " + s);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     * 获取扫描频率
     */
    private void getScanFrequency() {
        if (isConnected) {
            mThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    String s = "{\"jsonrpc\":\"2.0\",\"method\":\"settings/get\",\"params\":{\"entry\":\"scan.frequency\"},\"id\":\"getScanFrequency\"}" + "\r\n";
                    try {
                        writer.write(s);
                        writer.flush();
                        Log.i(TAG, "已发送获取扫描频率命令: " + s);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     * 处理输入流
     */
    private void handleReader() {
        try {
            String str;
            int index = 0;
            while ((str = reader.readLine()) != null) {

                decodeRegularInstruction(str);

                // 当已开始传输数据时, 解析雷达扫描数据并计算距离
                if (isStreamed) {
                    decodeScanData(str);

                    // 将8个block数据组合成一帧, 用于距离计算
                    if (index < 8) {
                        if (block != index) {
                            index = 0;
                            // frame.clear();
                            frameDistanceCos.clear();
                            frameDistanceSin.clear();
                            continue;
                        }
                        index++;
                        if (index == 8) {
                            index = 0;
                            updateUI(frameDistanceCos, frameDistanceSin);
                            // frame.clear();
                            frameDistanceCos.clear();
                            frameDistanceSin.clear();
                        }
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * 解析json-rpc格式数据
     * 解析常规命令
     */
    private void decodeRegularInstruction(String str) {
        try {
            JSONObject jsonObj = JSONObject.parseObject(str);
            String jsonrpc = jsonObj.getString("jsonrpc");
            String result = jsonObj.getString("result");
            String id = jsonObj.getString("id");

            // 解析常规命令
            if (result != null && id != null
                    && jsonrpc != null && jsonrpc.equals("2.0")) {
                switch (id) {
                    case "startStreaming":
                        if (Integer.parseInt(result) == 0) setStreamed(true);
                        Log.i(TAG, "startStreaming instruction response is " + result);
                        break;
                    case "stopStreaming":
                        if (Integer.parseInt(result) == 0) setStreamed(false);
                        Log.i(TAG, "stopStreaming instruction response is " + result);
                        break;
                    case "getScanFrequency":
                        setFrequency(Integer.parseInt(result));
                        utilCosSin();
                        Log.i(TAG, "getScanFrequency instruction response is " + result);
                        break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 解析json-rpc格式数据
     * 解析雷达扫描数据
     */
    private void decodeScanData(String str) {
        try {
            JSONObject jsonObj = JSONObject.parseObject(str);
            String jsonrpc = jsonObj.getString("jsonrpc");
            String method = jsonObj.getString("method");

            // 解析扫描数据
            if (method != null && method.equals("notification/laserScan")
                    && jsonrpc != null && jsonrpc.equals("2.0")) {
                JSONObject params = jsonObj.getJSONObject("params");
                block = params.getIntValue("block");
                // Log.i(TAG, "block is " + block);
                JSONArray layers = params.getJSONArray("layers");
                if (layers.size() == 1) {
                    JSONObject layerObj = layers.getJSONObject(0);
                    String ranges = layerObj.getString("ranges");
                    // 解析base64 ranges单位为2mm
                    byte[] rangesByte = Base64.decode(ranges, Base64.DEFAULT);
                    int len = rangesByte.length;
                    // Log.i(TAG, "rangesStr is " + Arrays.toString(rangesByte));
                    for (int i = 0; i < len; i = i + 2) {
                        // range单位转化成1mm
                        int range = ((rangesByte[i] & 0xff) + ((rangesByte[i + 1] & 0xff) << 8)) * 2;
                        // frame.add(range);

                        // 距离乘以对应cos三角函数转化
                        // rangesByte.length is 192 (96 * 2) when frequency is 30Hz
                        // -135 + block * 33.75 + 33.75 / (rangesByte.length / 2) * (i / 2 + 1)
                        // double angle = -135 + block * 33.75 + 33.75 / (len / 2.0) * (i / 2.0 + 1);
                        // double radian = Math.toRadians(angle);
                        // int distanceCos = (int) (range * Math.cos(radian));
                        // frameDistanceCos.add(distanceCos);
                        // int distanceSin = (int) (range * Math.sin(radian));
                        // frameDistanceCos.add(distanceSin);
                        int distanceCos = (int) (range * cosUtil[block * len / 2 + i / 2]);
                        frameDistanceCos.add(distanceCos);
                        int distanceSin = (int) (range * sinUtil[block * len / 2 + i / 2]);
                        frameDistanceCos.add(distanceSin);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * 一帧数据计算距离
     * 通过mHandler发送结果至主线程更新UI
     */
    private void updateUI(ArrayList<Integer> dataX, ArrayList<Integer> dataY) {

        try {
            Message msg = mHandler.obtainMessage();
            msg.what = 43;
            msg.obj = dataX.get(dataX.size() / 2);
            mHandler.sendMessage(msg);

            Message msg1 = mHandler.obtainMessage();
            msg1.what = 44;
            msg1.obj = LiDingLeft(dataX, dataY);
            mHandler.sendMessage(msg1);

            Message msg2 = mHandler.obtainMessage();
            msg2.what = 45;
            msg2.obj = LiDingRight(dataX, dataY);
            mHandler.sendMessage(msg2);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 30Hz for example, frameDistance 96 * 8
     * 单路立定跳远 左侧 4 5 block + part of 6 block
     * 单路立定跳远 右侧 2 3 block + part of 1 block
     * when transform to 6 blocks, 96 * 8 / 6 = 128
     * 数组下标范围 左侧 128 * 3 ---- 128 * 5
     * 数组下标范围 右侧 128 * 1 ---- 128 * 3
     */
    private double LiDingLeft(ArrayList<Integer> dataX, ArrayList<Integer> dataY) {
        try {
            double dis = 2000;

            for (int i = 128 * 3; i < 128 * 5; i++) {
                if (dataX.get(i) > 2000 || dataY.get(i) > 1000 || dataX.get(i) <= 0) {
                    dataX.set(i, 2000);
                } else {
                    if (dataX.get(i) < dis)
                        dis = dataX.get(i);
                }
            }

            return dis;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;

    }

    private double LiDingRight(ArrayList<Integer> dataX, ArrayList<Integer> dataY) {

        try {
            double dis = 2000;

            for (int i = 128; i < 128 * 3; i++) {
                if (dataX.get(i) > 2000 || dataY.get(i) < -1000 || dataX.get(i) <= 0) {
                    dataX.set(i, 2000);
                } else {
                    if (dataX.get(i) < dis)
                        dis = dataX.get(i);
                }
            }

            return dis;

        } catch (Exception e) {
            e.printStackTrace();

        }
        return -1;

    }

    /**
     * 根据扫描频率选择对应cos sin util
     */
    private void utilCosSin() {
        Log.i(TAG, "util of frequency is " + frequency + "Hz");
        switch (frequency) {
            case 10:
                Log.i(TAG, "frequency is 10Hz");
                cosUtil = cos1;
                sinUtil = sin1;
                break;
            case 15:
                Log.i(TAG, "frequency is 15Hz");
                cosUtil = cos2;
                sinUtil = sin2;
                break;
            case 20:
                Log.i(TAG, "frequency is 20Hz");
                cosUtil = cos3;
                sinUtil = sin3;
                break;
            case 25:
            case 30:
                Log.i(TAG, "frequency is 25Hz or 30Hz");
                cosUtil = cos4;
                sinUtil = sin4;
                break;
        }
    }

}
