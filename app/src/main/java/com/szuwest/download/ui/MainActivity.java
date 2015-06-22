package com.szuwest.download.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.szuwest.download.DownloadManager;
import com.szuwest.download.domain.DownLoadFile;
import com.szuwest.download.network.DownloadListener;

import java.io.File;


public class MainActivity extends ActionBarActivity implements DownloadListener{

    private static final String URL = "http://m.down.sandai.net/TimeAlbum/Android/sms.apk";

    private ProgressBar progressBar;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DownloadManager.getInstance().init();
        DownloadManager.getInstance().addDownloadListener(this);

        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DownloadManager.getInstance().removeDownloadListener(this);
    }

    private void initView() {
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setMax(100);
        DownLoadFile file = DownloadManager.getInstance().getDownloadFile(URL);
        if (file != null) {
            progressBar.setProgress(file.computeProgress());
        }

        statusText = (TextView) findViewById(R.id.statusText);
        Button operateBtn = (Button) findViewById(R.id.operateBtn);
        operateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DownLoadFile file = DownloadManager.getInstance().getDownloadFile(URL);
                if (file != null) {
                    if (file.getState() == DownLoadFile.DOWNSTAT_FINISH) {
                        onDownloadSuccess(file);
                    } else if (file.getState() == DownLoadFile.DOWNSTAT_DOWNLOAD
                            || file.getState() == DownLoadFile.DOWNSTAT_WAIT) {
                        DownloadManager.getInstance().pauseTask(file);
                    } else {
                        DownloadManager.getInstance().resumeTask(file);
                    }
                } else {
                    DownloadManager.getInstance().downloadFile(MainActivity.this, URL);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDownloadStart(DownLoadFile task) {
        statusText.setText("waiting");
    }

    @Override
    public void onDownloadUpdate(DownLoadFile task, long completeSize) {
        statusText.setText("downloading");
        progressBar.setProgress(task.computeProgress());
    }

    @Override
    public void onDownloadStop(DownLoadFile task) {
        statusText.setText("stop");
    }

    @Override
    public void onDownloadSuccess(DownLoadFile task) {
        statusText.setText("finish");
        if (task.getAbsolutePath().endsWith(".apk")) {
            installApk(this, task.getAbsolutePath());
        }
    }

    @Override
    public void onDownloadFail(DownLoadFile task) {
        statusText.setText("fail code : " + task.getFailCode());
    }

    public static void installApk(Activity context, String apkPath) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(apkPath)), "application/vnd.android.package-archive");
        context.startActivity(intent);
    }
}
