package jp.ebichiri.highlightsshutter;

import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;

public class VideoRecoder implements SurfaceHolder.Callback{
    private MediaRecorder myRecorder;
    private boolean isRecording;
    SurfaceHolder v_holder;

    String[] mMovieFilePath;

    public VideoRecoder() {
        myRecorder = new MediaRecorder();
        isRecording = false;
        String sdcardPath = Environment.getExternalStorageDirectory().toString();
        mMovieFilePath = new String[2];
        mMovieFilePath[0] = sdcardPath + "/highlight1.mp4";
        mMovieFilePath[1] = sdcardPath + "/highlight2.mp4";
    }

    // MediaRecorderの初期設定
    public void initializeVideoSettings(int ch) {
//        myRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        myRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT); // 録画の入力ソースを指定
        myRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); // ファイルフォーマットを指定
//        myRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP); // ビデオエンコーダを指定
//        myRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT); // オーディオエンコーダを指定
//        myRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT); // ビデオエンコーダを指定
        myRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264); // ビデオエンコーダを指定

        myRecorder.setOutputFile(mMovieFilePath[ch]); // 動画の出力先となるファイルパスを指定
        myRecorder.setVideoFrameRate(30); // 動画のフレームレートを指定
//        myRecorder.setVideoSize(320, 240); // 動画のサイズを指定
        myRecorder.setVideoSize(640, 480); // 動画のサイズを指定
        myRecorder.setPreviewDisplay(v_holder.getSurface()); // 録画中のプレビューに利用するサーフェイスを指定する
        try {
            myRecorder.prepare(); // 録画準備
        } catch (Exception e) {
            Log.e("recMovie", e.getMessage());
        }
    }

    public void startRecoding(int ch) {
        // 録画中でなければ録画を開始
        if (!isRecording) {
            initializeVideoSettings(ch); // MediaRecorderの設定
            myRecorder.start(); // 録画開始
            isRecording = true; // 録画中のフラグを立てる
        }
    }

    public void stopRecording() {
        if(isRecording) {
            myRecorder.stop(); // 録画停止
            myRecorder.reset(); // オブジェクトをリセット
            isRecording = false; // 録画中のフラグを外す
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        v_holder = holder; // SurfaceHolderを保存
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //
    }
}
