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
    /**
     * 定义Handler对象
     */
    private Handler handler = new Handler() {
        @Override
        /* 当有消息发送出来的时候就执行Handler的这个方法 */
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 44:
                    /* 调试输出 */
                    Log.i("mHandler", "-->" + msg.obj.toString());
                    /* 更新UI */
                    text_test.setText(msg.obj.toString());
                    break;
            }
        }
    };

    private EditText edit_ip;
    private EditText edit_port;
    private TextView text_test;

    private LidarDevice lidarDevice = new LidarDevice(handler);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        edit_ip = (EditText) findViewById(R.id.edit_ip);
        edit_port = (EditText) findViewById(R.id.edit_port);
        text_test = (TextView) findViewById(R.id.text_test);

        Button btn_connect = (Button) findViewById(R.id.btn_connect);
        Button btn_disconnect = (Button) findViewById(R.id.btn_disconnect);
        Button btn_startStreaming = (Button) findViewById(R.id.btn_startStreaming);
        Button btn_stopStreaming = (Button) findViewById(R.id.btn_stopStreaming);

        /* 连接雷达按钮 */
        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!lidarDevice.isConnected()) {
                    /* 连接按钮处理函数 */
                    String IPAdr = edit_ip.getText().toString();
                    int PORT = Integer.parseInt(edit_port.getText().toString());
                    lidarDevice.connect(IPAdr, PORT);

                    Toast.makeText(StartActivity.this, "连接雷达成功", Toast.LENGTH_SHORT).show();

                } else {

                    Toast.makeText(StartActivity.this, "雷达已连接", Toast.LENGTH_SHORT).show();

                }

            }
        });

        /* 断开雷达连接按钮 */
        btn_disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (lidarDevice.isConnected()) {

                    /* 断开雷达连接按钮处理函数 */
                    lidarDevice.stopStreaming();
                    lidarDevice.disconnect();

                    Toast.makeText(StartActivity.this, "与雷达的连接已断开", Toast.LENGTH_SHORT).show();

                } else {

                    Toast.makeText(StartActivity.this, "与雷达的连接已处于断开状态", Toast.LENGTH_SHORT).show();

                }

            }
        });


        /* 启动雷达数据传输按钮 */
        btn_startStreaming.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (lidarDevice.isConnected()) {

                    lidarDevice.startStreaming();
                    Toast.makeText(StartActivity.this, "成功启动雷达数据传输", Toast.LENGTH_SHORT).show();

                    lidarDevice.handleReaderThread();
                    // lidarDevice.getScanFrequency();


                } else {

                    Toast.makeText(StartActivity.this, "未与雷达连接", Toast.LENGTH_SHORT).show();

                }

            }
        });


        /* 停止雷达数据传输按钮 */
        btn_stopStreaming.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (lidarDevice.isConnected()) {

                    lidarDevice.stopStreaming();

                    Toast.makeText(StartActivity.this, "雷达数据传输已停止", Toast.LENGTH_SHORT).show();
                } else {

                    Toast.makeText(StartActivity.this, "未与雷达连接", Toast.LENGTH_SHORT).show();

                }

            }
        });

    }



}
