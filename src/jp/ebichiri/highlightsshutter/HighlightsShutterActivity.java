package jp.ebichiri.highlightsshutter;

import java.util.Timer;
import java.util.TimerTask;

import tw.com.prolific.driver.pl2303.PL2303Driver;
import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class HighlightsShutterActivity extends Activity {
    private static final String TAG = HighlightsShutterActivity.class.getSimpleName();

    DataTransfer mSerial;
    VideoRecoder mVideoRecoder;
    private static final String ACTION_USB_PERMISSION = "com.physicaloid.laserrangefinder.USB_PERMISSION";

    Button btStart;
    Button btDebug;
    TextView tvRec;
    TextView tvDebug;

    Timer mTimer;

    MediaPlayer mShutterSE;
    UploadAsyncTask mUpload;

    Context mContext;

    String[] mFileName;
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_highlights_shutter);

        mContext = this;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getActionBar().hide();

        btStart = (Button) findViewById(R.id.btStart);
        btDebug = (Button) findViewById(R.id.btDebug);
        tvRec = (TextView) findViewById(R.id.tvRec);
        tvDebug = (TextView) findViewById(R.id.tvDebug);

        mSerial = new DataTransfer(new PL2303Driver((UsbManager) getSystemService(Context.USB_SERVICE),
                this, ACTION_USB_PERMISSION), tvDebug); 

        mVideoRecoder = new VideoRecoder();

        mTimer      = new Timer();

        SurfaceView mySurfaceView = (SurfaceView) findViewById(R.id.svCamera);
        SurfaceHolder holder = mySurfaceView.getHolder();
        holder.addCallback(mVideoRecoder);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mShutterSE = MediaPlayer.create(this, R.raw.shutter);
        mShutterSE.setVolume(100, 100);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mSerial.open();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSerial.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        end();
    }

    boolean mRunning = false;
    boolean mRecording;
    boolean mLonger;
    int mCh;
    int mChUp;
    Handler mHandl = new Handler();

    public void onClickStart(View v) {
        if (!mRunning) {
            if (mShowDebug || mSerial.open()) {
                Log.d(TAG, "Serial Start");
                mSerial.ReadThreadStart();
                mCh = 0;
                mVideoRecoder.startRecoding(mCh);

                mRecording  = false;
                mLonger     = false;

                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
//                        if(!mLonger) {
                            mVideoRecoder.stopRecording();
                            mSerial.clearTrigger();

                            if(mRecording) {
                                mChUp = mCh;
                                setRecText("Uploading");
//                                mUpload.execute(mSerial.getParams());
                                mHandl.post(new Runnable() {
                                    public void run() {
                                        try {
                                            Thread.sleep(100);
                                        } catch (InterruptedException e) {
                                        }
                                        new UploadAsyncTask(tvRec).execute(mSerial.getParams(mChUp));
                                    }
                                });
                            }

                            if(mCh == 0) {mCh = 1;}
                            else {mCh = 0;}

                            mVideoRecoder.startRecoding(mCh);
                            mRecording = false;
/*                        } else {
                            mLonger = false;
                        }
*/                    }
                },1000*10, 1000*10); // 10秒録画を繰り返す


                // トリガー登録
                mSerial.addListener(new SensorTriggerListener() {
                    @Override
                    public void onSensorTrigger() {
                        mShutterSE.start();
                        mRecording = true;
                        mLonger = true;
                        setRecText("REC");
                    }
                });


                btStart.setText("Stop");
                mRunning = true;

            } else {
                Toast.makeText(this, "Cannot open", Toast.LENGTH_LONG).show();
            }
        } else {
            end();
        }
    }

    private void end() {
        Log.d(TAG,"Stop recording");
        mSerial.close();
        mSerial.clearListener();
        btStart.setText("Start");
        mRunning = false;
        mTimer.cancel();
        mVideoRecoder.stopRecording();
    }

    public void onClickTrigger(View v) {
        mShutterSE.start();
        mRecording = true;
        mLonger = true;
        setRecText("REC");
    }

    boolean mShowDebug = false;
    public void onClickDebug(View v) {
        if(mShowDebug) {
            btDebug.setText("DebugOff");
            tvDebug.setVisibility(TextView.INVISIBLE);
            mShowDebug = false;
        } else {
            btDebug.setText("DebugOn");
            tvDebug.setVisibility(TextView.VISIBLE);
            mShowDebug = true;
        }
    }

    Handler mHandler = new Handler();
    void setRecText(String str) {
        final String recstr = str;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                tvRec.setTextColor(0xFFFF0000);
                tvRec.setText(recstr);
            }
            });
    }

}
