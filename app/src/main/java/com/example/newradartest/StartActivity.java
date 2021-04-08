package com.example.newradartest;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;


public class StartActivity extends Activity {

    private static final String TAG = "StartActivity";

    /**
     * 定义Handler对象
     */
    private Handler handler = new Handler() {
        @Override
        /* 当有消息发送出来的时候就执行Handler的这个方法 */
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
//                case 43:
//                    /* 调试输出 */
//                    Log.i(TAG, " middle-->" + msg.obj.toString());
//                    /* 更新UI */
//                    text_middle.setText(msg.obj.toString());
//                    break;
                case 44:
                    /* 调试输出 */
                    Log.i(TAG, " left-->" + msg.obj.toString());
                    /* 更新UI */
                    text_left.setText(msg.obj.toString());
                    break;
                case 45:
                    /* 调试输出 */
                    Log.i(TAG, " right-->" + msg.obj.toString());
                    /* 更新UI */
                    text_right.setText(msg.obj.toString());
                    break;
            }
        }
    };

    private EditText edit_ip;
    private EditText edit_port;

    private TextView radar_state;
    private EditText edit_area_x;
    private EditText edit_area_y;

    //    private TextView text_middle;
    private TextView text_left;
    private TextView text_right;

    private LidarDevice lidarDevice;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        edit_ip = (EditText) findViewById(R.id.edit_ip);
        edit_port = (EditText) findViewById(R.id.edit_port);

        radar_state = (TextView) findViewById(R.id.radar_state);
        edit_area_x = (EditText) findViewById(R.id.edit_area_x);
        edit_area_y = (EditText) findViewById(R.id.edit_area_y);

//        text_middle = (TextView) findViewById(R.id.text_middle);
        text_left = (TextView) findViewById(R.id.text_left);
        text_right = (TextView) findViewById(R.id.text_right);

        lidarDevice = new LidarDevice(handler);

        Button btn_connect = (Button) findViewById(R.id.btn_connect);
        Button btn_disconnect = (Button) findViewById(R.id.btn_disconnect);
        Button btn_getScanFrequency = (Button) findViewById(R.id.btn_getScanFrequency);
        Button btn_startStreaming = (Button) findViewById(R.id.btn_startStreaming);
        Button btn_stopStreaming = (Button) findViewById(R.id.btn_stopStreaming);

        /* 连接雷达按钮 */
        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    if (!lidarDevice.isConnected()) {
                        /* 连接按钮处理函数 */
                        String IPAdr = edit_ip.getText().toString();
                        int PORT = Integer.parseInt(edit_port.getText().toString());
                        lidarDevice.connect(IPAdr, PORT);
                        radar_state.setText("已连接");

                        Log.i(TAG, "连接雷达成功");
                        Toast.makeText(StartActivity.this, "连接雷达成功", Toast.LENGTH_SHORT).show();

                    } else {

                        Log.i(TAG, "雷达已连接");
                        Toast.makeText(StartActivity.this, "雷达已连接", Toast.LENGTH_SHORT).show();

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        });

        /* 断开雷达连接按钮 */
        btn_disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    if (lidarDevice.isConnected()) {

                        /* 断开雷达连接按钮处理函数 */
                        lidarDevice.disconnect();
                        radar_state.setText("已断开");

                        Log.i(TAG, "与雷达的连接已断开");
                        Toast.makeText(StartActivity.this, "与雷达的连接已断开", Toast.LENGTH_SHORT).show();

                    } else {

                        Log.i(TAG, "与雷达的连接已处于断开状态");
                        Toast.makeText(StartActivity.this, "与雷达的连接已处于断开状态", Toast.LENGTH_SHORT).show();

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        });

        /* 查看雷达扫描频率按钮 */
        btn_getScanFrequency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    if (lidarDevice.isConnected()) {

                        int fre = lidarDevice.getFrequency();

                        Log.i(TAG, "雷达扫描频率为" + fre + "Hz");
                        Toast.makeText(StartActivity.this, "雷达扫描频率为" + fre + "Hz", Toast.LENGTH_SHORT).show();

                    } else {

                        Log.i(TAG, "未与雷达连接");
                        Toast.makeText(StartActivity.this, "未与雷达连接", Toast.LENGTH_SHORT).show();

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        });


        /* 启动雷达数据传输按钮 || 开始测量按钮 */
        btn_startStreaming.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    if (lidarDevice.isConnected()) {

                        int areaX = Integer.parseInt(edit_area_x.getText().toString());
                        int areaY = Integer.parseInt(edit_area_y.getText().toString());

                        if (areaX > 0 && areaY > 0) {

                            lidarDevice.setRestrictX(areaX);
                            lidarDevice.setRestrictY(areaY);

                            lidarDevice.startStreaming();

                            Log.i(TAG, "启动雷达数据传输并开始测量");
                            Toast.makeText(StartActivity.this, "启动雷达数据传输并开始测量", Toast.LENGTH_SHORT).show();

                        } else {

                            Log.i(TAG, "请输入正确的横向与纵向限制距离");
                            Toast.makeText(StartActivity.this, "请输入正确的横向与纵向限制距离", Toast.LENGTH_SHORT).show();

                        }

                    } else {

                        Log.i(TAG, "未与雷达连接");
                        Toast.makeText(StartActivity.this, "未与雷达连接", Toast.LENGTH_SHORT).show();

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        });


        /* 停止雷达数据传输按钮 || 停止测量按钮 */
        btn_stopStreaming.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    if (lidarDevice.isConnected()) {

                        lidarDevice.stopStreaming();

                        Log.i(TAG, "停止雷达数据传输并停止测量");
                        Toast.makeText(StartActivity.this, "停止雷达数据传输并停止测量", Toast.LENGTH_SHORT).show();

                    } else {

                        Log.i(TAG, "未与雷达连接");
                        Toast.makeText(StartActivity.this, "未与雷达连接", Toast.LENGTH_SHORT).show();

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        });

    }


}
