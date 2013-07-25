package jp.ebichiri.highlightsshutter;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import tw.com.prolific.driver.pl2303.PL2303Driver;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

public class DataTransfer {
    private static final String TAG = DataTransfer.class.getSimpleName();

    private int THRESHOLD_ACCEL_X  = 400;
    private int THRESHOLD_ACCEL_Y  = 400;
    private int THRESHOLD_ACCEL_Z  = 400;
    private int THRESHOLD_PRESSURE = 100;
    private int THRESHOLD_MIC      = 100;

    int mAccelX = 0;
    int mAccelY = 0;
    int mAccelZ = 0;
    int mPressure = 0;
    int mMic = 0;

    private PL2303Driver mSerial;
    private PL2303Driver.BaudRate BAUDRATE = PL2303Driver.BaudRate.B115200;

    private int mReadSize = 0;
    private static final int MAX_READBUF_SIZE = 256;
    private byte[] rbuf = new byte[MAX_READBUF_SIZE];

    private boolean DEBUG_SENSOR_DISP_ON = false;

    TextView tvDebug;
    String[] mFileName;
    public DataTransfer(PL2303Driver serial, TextView debugView) {
        mSerial = serial;
        tvDebug = debugView;
        mFileName = new String[2];

        String sdcardPath = Environment.getExternalStorageDirectory().toString();
        mFileName[0] = sdcardPath + "/highlight1.mp4";
        mFileName[1] = sdcardPath + "/highlight2.mp4";
    }

    public String[] getParams(int ch) {
        String[] params = new String[7];
        params[0] = mFileName[ch];
        params[1] = "ebipr";
        params[2] = Integer.toString(mAccelX);
        params[3] = Integer.toString(mAccelY);
        params[4] = Integer.toString(mAccelZ);
        params[5] = Integer.toString(mPressure);
        params[6] = Integer.toString(mMic);
        return params;
    }

    public boolean open() {
        if(!mSerial.isConnected()) {
            mSerial.enumerate();
        }

        if (!mSerial.InitByBaudRate(BAUDRATE)) {
            return false;
        } else {
            return true;
        }
    }

    public boolean close() {
        if(mSerial.isConnected()) {
            ReadThreadStop();
            mSerial.end();
        }
        return true;
    }

    public int write(String str) {
        byte[] b = str.getBytes();
        return mSerial.write(b, b.length);
    }


    private boolean mReadThreadRunning = false;
    private boolean mReadThreadStop = true;

    /**
     * Starts read thread
     * @return true : sccessful, false :fail
     */
    public boolean ReadThreadStart() {
        if(!mReadThreadRunning) {
            mReadThreadRunning = true;
            mReadThreadStop = false;
            new Thread(mLoop).start();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Stops read thread
     * @return true : sccessful, false :fail
     */
    public boolean ReadThreadStop() {
        int count;
        if(mReadThreadRunning) {
            mReadThreadStop = true;
            count=0;
            while(mReadThreadRunning){
                if(count > 100) return false;   // 100 = 1sec
                try {
                    count++;
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            }
        }
        return true;
    }

    private Runnable mLoop = new Runnable() {
        @Override
        public void run() {

            if (mSerial == null) return;

            if(!mSerial.isConnected()) return;

            String rbufstr = "";
            StringBuilder parserstr = new StringBuilder();

            while (true) { // Read thread loop

                mReadSize = mSerial.read(rbuf);

                if (mReadSize > 0) {
                    if (mReadSize > MAX_READBUF_SIZE)
                        mReadSize = MAX_READBUF_SIZE;

                    try {
                        rbufstr = new String(rbuf, "UTF-8");
                        appendDebugText(rbufstr);
                        if (DEBUG_SENSOR_DISP_ON) {
                            Log.d(TAG, rbufstr + "\n");
                        }
                        parserstr.append(rbufstr);
                        rbufstr = "";
                    } catch (UnsupportedEncodingException e) {
                    }

                    if (sensorDataParser(parserstr)) {
                        if(sensorTriggerCheck()) {
                            sensorTrigger(); // Listenerイベント
                        }
                    }
                }
                parserstr.setLength(0);

                if(mReadThreadStop) {
                    mReadThreadRunning = false;
                    return;
                }

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }

            }
        }
    };

    boolean sensorDataParser(StringBuilder str) {
        int start = -1;
        int end   = -1;
        start = str.indexOf("S");
        if(start == -1) return false;
        end   = str.indexOf(".");
        if(end == -1) return false;

        try {
            start++;
            mAccelX = Integer.parseInt(str.substring(start, start + 4));
            start += 5;
            mAccelY = Integer.parseInt(str.substring(start, start + 4));
            start += 5;
            mAccelZ = Integer.parseInt(str.substring(start, start + 4));
            start += 5;
            mPressure = Integer.parseInt(str.substring(start, start + 4));
            start += 5;
            mMic = Integer.parseInt(str.substring(start, start + 4));
        } catch(Exception e) {
            Log.e(TAG, e.toString());
            return false;
        }
        return true;
    }

    boolean mTrigger = false;

    public void clearTrigger() {
        mTrigger = false;
    }

    boolean sensorTriggerCheck() {
        // TODO : チェック方法検討、時間でチェック頻度減らす
        boolean trigger = false;
        if (!mTrigger) {
            if (mAccelX > THRESHOLD_ACCEL_X)
                trigger = true;
            if (mAccelY > THRESHOLD_ACCEL_Y)
                trigger = true;
            if (mAccelZ > THRESHOLD_ACCEL_Z)
                trigger = true;
            if (mPressure > THRESHOLD_PRESSURE)
                trigger = true;
            if (mMic > THRESHOLD_MIC)
                trigger = true;
        }
        mTrigger = trigger;
        return trigger;
    }

    // センサトリガListenerイベント
    List<SensorTriggerListener> listenerList
        = new ArrayList<SensorTriggerListener>();

    public void addListener(SensorTriggerListener listener) {
        listenerList.add(listener);
    }

    public void clearListener() {
        listenerList.clear();
    }

    private void sensorTrigger() {
        for (SensorTriggerListener listener: listenerList) {
            listener.onSensorTrigger();
        }
    }

    Handler mHandler = new Handler();
    void appendDebugText(String str) {
        final String debugstr = str;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                tvDebug.setTextColor(0xFF00FF00);
                tvDebug.append(debugstr);
            }
            });
    }
}
