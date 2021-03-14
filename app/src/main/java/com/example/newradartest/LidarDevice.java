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
    private ArrayList<Integer> frame;
    private ArrayList<Double> frameDistance;
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

    /**
     * 构造函数
     */
    public LidarDevice(Handler handler) {

        socket = null;
        writer = null;
        reader = null;

        setConnected(false);
        setStreamed(false);
        setFrequency(30);

        int cpuNumbers = Runtime.getRuntime().availableProcessors();
        // 根据CPU数目初始化线程池
        mThreadPool = Executors.newFixedThreadPool(cpuNumbers * 5);

        mHandler = handler;

        frame = new ArrayList<>();
        frameDistance = new ArrayList<>();
        block = -1;

    }

    /**
     * 连接设备
     *
     * @param IPAdr
     * @param PORT
     */
    public void connect(String IPAdr, int PORT) {
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                if (startTcpConnection(IPAdr, PORT)) {
                    setConnected(true);
                    Log.i(TAG, "设备连接成功");
                } else {
                    setConnected(false);
                    Log.e(TAG, "设备连接失败");
                }
            }
        });

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
            }
            /* 输出流 */
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            /* 输入流 */
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            Log.i(TAG, "tcp连接创建成功");

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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 启动数据传输
     */
    public void startStreaming() {
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

    /**
     * 停止数据传输
     */
    public void stopStreaming() {
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

    /**
     * 获取扫描频率
     */
    public void getScanFrequency() {
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                String s = "{\"jsonrpc\":\"2.0\",\"method\":\"settings/get\",\"params\":{\"entry\":\"scan.frequency\"},\"id\":\"getScanFrequency\"}";
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

    /**
     * 处理输入流线程
     */
    public void handleReaderThread() {
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String str;
                    int index = 0;
                    while ((str = reader.readLine()) != null) {
                        // 当雷达已连接时, 解析常规命令
                        if (isConnected) {
                            decodeRegularInstruction(str);
                        }

                        // 当雷达已连接并且已开始传输数据时, 解析雷达扫描数据并计算距离
                        if (isConnected && isStreamed) {
                            decodeScanData(str);

                            // 将8个block数据组合成一帧, 用于距离计算
                            if (index < 8) {
                                if (block != index) {
                                    index = 0;
                                    frame.clear();
                                    frameDistance.clear();
                                    continue;
                                }
                                index++;
                                if (index == 8) {
                                    index = 0;
                                    updateUI();
                                    frame.clear();
                                    frameDistance.clear();
                                }
                            }
                        }

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


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
                        frame.add(range);
                        // 距离乘以对应cos三角函数转化
                        // rangesByte.length is 192 (96 * 2) when frequency is 30Hz
                        // -135 + block * 33.75 + 33.75 / (rangesByte.length / 2) * (i / 2 + 1)
                        double angle = -135 + block * 33.75 + 33.75 / (len / 2.0) * (i / 2.0 + 1);
                        double distance = range * Math.cos(Math.toRadians(angle));
                        frameDistance.add(distance);
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
    private void updateUI() {
        Message msg = mHandler.obtainMessage();
        msg.what = 44;
        msg.obj = frameDistance.get(frameDistance.size() / 2 - 5);
        mHandler.sendMessage(msg);
    }


    /**
     * 通过三角函数计算一帧内所有数据点的距离
     */
    private void calculate(ArrayList<Integer> data) {
        switch (frequency) {
            case 10:
                Log.i(TAG, "frequency is 10");
                break;
            case 15:
                Log.i(TAG, "frequency is 15");
                break;
            case 20:
                Log.i(TAG, "frequency is 20");
                break;

            case 25:
            case 30:
                Log.i(TAG, "frequency is 25 or 30");
                break;
        }
    }

    /**
     * 极坐标系到直角坐标系的转化工具
     */
    public void util() {
        switch (frequency) {
            case 10:
                Log.i(TAG, "frequency is 10Hz");
                // 288 * 8 cos sin 270
                break;
            case 15:
                Log.i(TAG, "frequency is 15Hz");
                // 192 * 8 cos sin 270
                break;
            case 20:
                Log.i(TAG, "frequency is 20Hz");
                // 144 * 8 cos sin 270
                break;
            case 25:
            case 30:
                Log.i(TAG, "frequency is 25Hz or 30Hz");
                // 96 * 8 cos sin 270

                break;
        }
    }

}
