package com.car.frpc_android.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.car.frpc_android.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.LinkedHashSet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.internal.Utils;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class LogcatActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.tv_logcat)
    TextView tvLogcat;
    @BindView(R.id.sv_logcat)
    ScrollView svLogcat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logcat);
        ButterKnife.bind(this);
        initToolbar();
        readLog(false);
    }

    private void readLog(boolean flush) {
        HashSet<String> lst = new LinkedHashSet<String>();
        lst.add("logcat");
        lst.add("-d");
        lst.add("-v");
        lst.add("time");
        lst.add("-s");
        lst.add("GoLog,com.car.frpc_android.FrpcService");
        Observable.create((ObservableOnSubscribe<String>) emitter -> {

            if (flush) {
                HashSet<String> lst2 = new LinkedHashSet<String>();
                lst2.add("logcat");
                lst2.add("-c");
                Process process = Runtime.getRuntime().exec(lst2.toArray(new String[0]));
                process.waitFor();
            }

            Process process = Runtime.getRuntime().exec(lst.toArray(new String[0]));

            InputStreamReader in = new InputStreamReader(process.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(in);

            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                emitter.onNext(line);
            }
            in.close();
            bufferedReader.close();
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(String s) {
                        tvLogcat.append(s);
                        tvLogcat.append("\r\n");
                        svLogcat.fullScroll(View.FOCUS_DOWN);
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {

                    }
                });


    }

    private void initToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_logcat, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.copy:
                setClipboard(tvLogcat.getText().toString());
                Toast.makeText(this, R.string.copySuccess, Toast.LENGTH_SHORT).show();
                break;
            case R.id.delete:
                readLog(true);
                tvLogcat.setText("");
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setClipboard(String content) {
        try {
            ClipboardManager cmb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("logcat", content);
            cmb.setPrimaryClip(clipData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
