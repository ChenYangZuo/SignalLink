package com.nuedc.link;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private Spinner DevicesList;
    private ArrayList<String> DevicesListData;
    private final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private LineChart chart;
    private BarChart chart2;
    private TextView TDHx;
    private TextView Normalized1;
    private TextView Normalized2;
    private TextView Normalized3;
    private TextView Normalized4;
    private TextView Normalized5;

    private String text_TDHx;
    private String text_Normalized1;
    private String text_Normalized2;
    private String text_Normalized3;
    private String text_Normalized4;
    private String text_Normalized5;

    private ArrayList<Entry> values;
    private ArrayList<BarEntry> values2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chart = findViewById(R.id.chart);
        chart2 =findViewById(R.id.chart2);
        TDHx = findViewById(R.id.THDx);
        Normalized1 = findViewById(R.id.Normalized1);
        Normalized2 = findViewById(R.id.Normalized2);
        Normalized3 = findViewById(R.id.Normalized3);
        Normalized4 = findViewById(R.id.Normalized4);
        Normalized5 = findViewById(R.id.Normalized5);

        Bluetooth_Init();
        // 避免加载进界面后触发一次监听回调
        DevicesList.setSelection(0,true);
        DevicesList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String IndexData = DevicesListData.get(position);
                String[] DeviceData = IndexData.split("\n");
                Log.i("MyTag", "Name:" + DeviceData[0]);
                Log.i("MyTag", "Address:" + DeviceData[1]);
                Bluetooth_connect(mBluetoothAdapter.getRemoteDevice(DeviceData[1]));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        Chart_Init(chart);
        Chart2_Init(chart2);
    }

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler(){
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                Log.i("MyTag","刷新UI");
                TDHx.setText(text_TDHx);
                Normalized1.setText(text_Normalized1);
                Normalized2.setText(text_Normalized2);
                Normalized3.setText(text_Normalized3);
                Normalized4.setText(text_Normalized4);
                Normalized5.setText(text_Normalized5);
            }
            else if(msg.what == 1){
                Log.i("MyTag","Size1:"+ values.size());
                Log.i("MyTag","Size2:"+ values2.size());
                Chart_SetData(chart,values);
                Chart2_SetData(chart2,values2);
            }
        }
    };

    private void Bluetooth_Init() {
        // 获取蓝牙适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "此设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }
        // 开启蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
            Toast.makeText(this, "蓝牙已自动打开", Toast.LENGTH_SHORT).show();
        }

        DevicesListData = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, DevicesListData);
        DevicesList = findViewById(R.id.BluetoothDevices);
        // 取已绑定蓝牙
        Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
        if (bondedDevices.size() > 0) {
            for (BluetoothDevice device : bondedDevices) {
                DevicesListData.add(device.getName() + "\n" + device.getAddress());
                DevicesList.setAdapter(adapter);
            }
        } else {
            Log.i("MyTag", "bondedDevices.size=0");
        }
    }

    private void Bluetooth_connect(BluetoothDevice device) {
        // 开启连接线程
        ConnectThread mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        Toast.makeText(this, "建立蓝牙连接", Toast.LENGTH_SHORT).show();
    }

    private void Chart_Init(LineChart chart){
        chart.setDrawGridBackground(false);
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(false);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setDrawGridLines(true);
        chart.getXAxis().setDrawAxisLine(false);
        chart.invalidate();
    }

    private void Chart2_Init(BarChart chart){
        chart.getDescription().setEnabled(false);
        chart.setMaxVisibleValueCount(60);
        chart.setPinchZoom(false);
        chart.setDrawBarShadow(false);
        chart.setDrawGridBackground(false);
        chart.animateY(1500);
        chart.getLegend().setEnabled(false);
    }

    private void Chart_SetData(LineChart chart, ArrayList<Entry> values) {
        LineDataSet dataSet = new LineDataSet(values, "DataSet 1");
        dataSet.setColor(Color.BLACK);
        dataSet.setLineWidth(0.5f);
        dataSet.setDrawValues(false);
        dataSet.setDrawCircles(false);
        dataSet.setMode(LineDataSet.Mode.LINEAR);
        dataSet.setDrawFilled(false);

        LineData data = new LineData(dataSet);
        chart.setData(data);
        Legend l = chart.getLegend();
        l.setEnabled(false);
        chart.invalidate();
    }

    private void Chart2_SetData(BarChart chart, ArrayList<BarEntry> values) {
        BarDataSet set1 = new BarDataSet(values, "Data Set");
        set1.setColors(ColorTemplate.VORDIPLOM_COLORS);
        set1.setDrawValues(false);
        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(set1);
        BarData data = new BarData(dataSets);
        chart.setData(data);
        chart.setFitBars(true);
        chart.invalidate();
    }


    private class ConnectThread extends Thread {
        private final BluetoothSocket mBtSocket;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket bs = null;

            // 根据UUID获取欲连接设备
            try {
                bs = device.createRfcommSocketToServiceRecord(myUUID);
            } catch (IOException e) {
                Log.e("MyTag", "create() failed", e);
            }
            mBtSocket = bs;
        }

        public void run() {
            setName("ConnectThread");
            // 尝试连接蓝牙端口
            try {
                mBtSocket.connect();
            } catch (IOException e) {
                try {
                    mBtSocket.close();
                } catch (IOException e2) {
                    Log.e("MyTag", "close() fail", e2);
                }
                return;
            }

            ConnectedThread mConnectedThread = new ConnectedThread(mBtSocket);
            mConnectedThread.start();

        }

        public void cancel() {
            try {
                mBtSocket.close();
            } catch (IOException e) {
                Log.e("MyTag", "close() fail", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mBtSocket;
        private final InputStream mInputStream;

        public ConnectedThread(BluetoothSocket socket) {
            mBtSocket = socket;
            InputStream is = null;

            // 获取输入输出流
            try {
                is = socket.getInputStream();
            } catch (IOException e) {
                Log.i("MyTag", "get Stream fail", e);
            }

            mInputStream = is;
        }

        public void run() {
            byte[] buffer = new byte[2048];
            int bytes;
            values = new ArrayList<>();
            values2 = new ArrayList<>();
            int num = 0;
            int num2 = 0;
            while (true) {
                try {
                    bytes = mInputStream.read(buffer);
                    if (bytes != -1) {
                        String data = new String(buffer, 0, bytes);
                        Log.i("MyTag", data);
                        String[] strArr = data.split(",");
                        String FLAG = strArr[0];
                        switch (FLAG) {
                            case "HDU": {
                                // 数值帧
                                Log.i("MyTag", "HDU");
                                values = new ArrayList<>();
                                values2 = new ArrayList<>();
                                num = 0;
                                num2 = 0;
                                text_TDHx = strArr[1];
                                text_Normalized1 = strArr[2];
                                text_Normalized2 = strArr[3];
                                text_Normalized3 = strArr[4];
                                text_Normalized4 = strArr[5];
                                text_Normalized5 = strArr[6];
                                Message message = new Message();
                                message.what = 0;
                                handler.sendMessage(message);
                                break;
                            }
                            case "ELE": {
                                // 数据帧
                                Log.i("MyTag", "ELE");
                                for (int i = 1; i < strArr.length - 1; i++) {
                                    float val = Float.parseFloat(strArr[i]);
                                    values.add(new Entry(num++, val));
                                }
                                break;
                            }
                            case "WIR": {
                                Log.i("MyTag", "WIR");
                                for (int i = 1; i < strArr.length - 1; i++) {
                                    float val = Float.parseFloat(strArr[i]);
                                    values2.add(new BarEntry(num2++, val));
                                }
                                break;
                            }
                            case "END": {
                                // 结束帧
                                Message message = new Message();
                                message.what = 1;
                                handler.sendMessage(message);
                                break;
                            }
                        }
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void cancel() {
            try {
                mBtSocket.close();
            } catch (IOException e) {
                Log.e("MyTag", "close() fail", e);
            }
        }
    }

}