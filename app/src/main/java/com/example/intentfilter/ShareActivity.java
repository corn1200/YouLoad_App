package com.example.intentfilter;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.DownloadProgressCallback;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ShareActivity extends AppCompatActivity {
    ProgressDialog Downloadpd;
    CompositeDisposable disposable = new CompositeDisposable();
    VideoInfo streamInfo;

    public DownloadProgressCallback callback = new DownloadProgressCallback() {
        @Override
        public void onProgressUpdate(float progress, long etaInSeconds) {
            final float p = progress;
            final long es = etaInSeconds;
            runOnUiThread(() -> {
                Downloadpd.setProgress((int) p);
                if ((int) p >= 100) {
                    Downloadpd.setMessage("오디오 변환 중");
                } else {
                    Downloadpd.setMessage(p + "% (완료까지 " + es + " 초)");
                }
            });
        }
    };

    @Override
    protected void onDestroy() {
        disposable.dispose();
        super.onDestroy();
    }

    public boolean isStoragePermissionGranted() { // 권한 허용 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 버전 확인
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1); // 허용 요구
                return false;
            }
        } else {
            return true;
        }
    }

    @NonNull
    private File getDownloadLocation() {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File youtubeDLDir = new File(downloadsDir, "youtube-downloads");
        if (!youtubeDLDir.exists()) youtubeDLDir.mkdir(); // make directory
        return youtubeDLDir;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        ActionBar ab = getSupportActionBar();

        ab.setIcon(R.mipmap.ic_icon_foreground);
        ab.setDisplayUseLogoEnabled(true);
        ab.setDisplayShowHomeEnabled(true);

        new Thread(() -> {
            try {
                YoutubeDL.getInstance().updateYoutubeDL(getApplication());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        ConnectivityManager ConntectCheck = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = ConntectCheck.getActiveNetworkInfo();
        Button YouLoadDownLoad_btn = findViewById(R.id.YouLoadDownloadButton);
        Bundle extras = getIntent().getExtras();
        TextView YouLoad_txt = findViewById(R.id.YouLoadText);
        ImageView YouLoad_img = findViewById(R.id.YouLoadBackground);

        try {
            YoutubeDL.getInstance().init(getApplication());
            FFmpeg.getInstance().init(getApplication());
            streamInfo = YoutubeDL.getInstance().getInfo(extras.getString(Intent.EXTRA_TEXT));
            String Title = streamInfo.getTitle();
            String Thumbnail = streamInfo.getThumbnail();
            YouLoad_txt.setText(Title);
            Glide.with(this).load(Thumbnail).into(YouLoad_img);
            if (networkInfo != null && networkInfo.isConnected()) {
                Toast.makeText(this, "네트워크 연결중입니다.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            YouLoad_txt.setText("오류 발생");
            if (networkInfo == null && networkInfo.isConnected() == false){
                Toast.makeText(this,"네트워크 상태를 확인하십시오", Toast.LENGTH_SHORT).show();
            }
        }

        YouLoadDownLoad_btn.setOnClickListener(view -> {
            if (!isStoragePermissionGranted()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ShareActivity.this);
                builder.setMessage("파일 시스템 권한이 승인되지 않아 다운로드 할 수 없습니다.").setPositiveButton("확인", (dialogInterface, i) -> dialogInterface.dismiss());
                builder.create();
            } else {
                try {
                    final YoutubeDLRequest req = new YoutubeDLRequest(extras.getString(Intent.EXTRA_TEXT));
                    File path = getDownloadLocation();
                    String AbsolutePath = path.getAbsolutePath() + "/" + streamInfo.getTitle().replaceAll("/", "|");
                    req.setOption("-o", AbsolutePath + ".%(ext)s");
                    req.setOption("-x");
                    req.setOption("--audio-format", "mp3");
                    AbsolutePath += ".mp3";

                    final String apath = AbsolutePath;
                    Downloadpd = new ProgressDialog(ShareActivity.this); // ShareAcitivity에 알림 띄움
                    Downloadpd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    Downloadpd.setMessage("다운로드 시작");
                    Downloadpd.setCancelable(false);
                    Downloadpd.show();
                    Disposable disp = Observable.fromCallable(() -> YoutubeDL.getInstance().execute(req, callback)).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread()).subscribe(YoutubeDLResponse -> {
                        Downloadpd.setMessage("다운로드 완료");
                        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + apath)));
                        Downloadpd.dismiss();
                    });
                    disposable.add(disp);
                } catch (Exception e) {
                    e.printStackTrace();
                    Downloadpd.dismiss();
                }
            }
        });

    }
}