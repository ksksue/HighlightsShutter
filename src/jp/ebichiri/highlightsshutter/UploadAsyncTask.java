package jp.ebichiri.highlightsshutter;

import java.io.File;
import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

public class UploadAsyncTask extends AsyncTask<String, Integer, Integer> {
    static final String TAG = UploadAsyncTask.class.getSimpleName();
    ProgressDialog dialog;
    TextView tvRec;

    public UploadAsyncTask(TextView tv) {
        tvRec = tv;
    }

    @Override
    protected Integer doInBackground(String... params) {
        String fileName = params[0];
        try {
            File file = new File(fileName);

            Log.d(TAG,"Upload : "+fileName);

            HttpClient httpClient = new DefaultHttpClient();
            // HttpPost httpPost = new
            // HttpPost("http://219.94.251.92/index.php/upload");
            HttpPost httpPost = new HttpPost(
                    "http://219.94.251.92/index.php/upload");
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            MultipartEntity multipartEntity = new MultipartEntity(
                    HttpMultipartMode.BROWSER_COMPATIBLE);

            FileBody fileBody = new FileBody(file, "video/mp4");
            StringBody event = new StringBody(params[1]);
            StringBody accx = new StringBody(params[2]);
            StringBody accy = new StringBody(params[3]);
            StringBody accz = new StringBody(params[4]);
            StringBody pressure = new StringBody(params[5]);
            StringBody volume = new StringBody(params[6]);

            multipartEntity.addPart("movie", fileBody);
            multipartEntity.addPart("event", event);
            multipartEntity.addPart("volume", volume);
            multipartEntity.addPart("pressure", pressure);
            multipartEntity.addPart("accx", accx);
            multipartEntity.addPart("accy", accy);
            multipartEntity.addPart("accz", accz);

            httpPost.setEntity(multipartEntity);
            httpClient.execute(httpPost, responseHandler);
        } catch (ClientProtocolException e) {
            Log.e("UploadAsyncTask", e.toString());
        } catch (IOException e) {
            Log.e("UploadAsyncTask", e.toString());
        }

        appendDebugText("Complete:"+fileName);
        Log.d(TAG,"Upload Completed");
        return 0;
    }

    @Override
    protected void onPostExecute(Integer result) {
    }

    @Override
    protected void onPreExecute() {
    }

    Handler mHandler = new Handler();
    void appendDebugText(String str) {
        final String recstr = str;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                tvRec.setTextColor(0xFFFF0000);
                tvRec.append(recstr);
            }
            });
    }

}