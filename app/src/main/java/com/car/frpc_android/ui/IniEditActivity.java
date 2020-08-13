package com.car.frpc_android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.car.frpc_android.Constants;
import com.car.frpc_android.R;
import com.github.ahmadaghazadeh.editor.widget.CodeEditor;

import java.io.File;
import java.io.FileWriter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class IniEditActivity extends AppCompatActivity {


    @BindView(R.id.editText)
    CodeEditor editText;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ini_edit);
        ButterKnife.bind(this);

        String filePath = getIntent().getStringExtra(getString(R.string.intent_key_file));
        if (!TextUtils.isEmpty(filePath)) file = new File(filePath);
        initToolbar();

        initEdit();


    }

    private void initEdit() {
        Observable.create((ObservableOnSubscribe<String>) emitter -> {
            emitter.onNext(file != null ? Constants.getStringFromFile(file) :
                    Constants.getStringFromRaw(IniEditActivity.this, R.raw.frpc));
            emitter.onComplete();
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(String s) {
                        editText.setText(s, 1);
                    }

                    @Override
                    public void onError(Throwable e) {

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
        toolbar.setTitle(file == null ? getString(R.string.noName) : file.getName());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_template:
                startActivity(new Intent(IniEditActivity.this, TemplateActivity.class));
                break;
            case R.id.action_save:
                actionSave();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    private void actionSave() {
        new MaterialDialog.Builder(this)
                .title(file == null ? R.string.titleInputFileName : R.string.titleModifyFileName)
                .canceledOnTouchOutside(false)
                .autoDismiss(false)
                .negativeText(R.string.cancel)
                .onNegative((dialog, which) -> dialog.dismiss())
                .input("", file == null ? "" : file.getName(), false, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        String fileName = !input.toString().endsWith(Constants.INI_FILE_SUF) ? input + Constants.INI_FILE_SUF : input.toString();
                        saveFile(fileName, new Observer<File>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onNext(File file) {
                                Toast.makeText(IniEditActivity.this.getApplicationContext(), R.string.tipSaveSuccess, Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                finish();
                            }

                            @Override
                            public void onError(Throwable e) {
                                e.printStackTrace();
                                Toast.makeText(IniEditActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onComplete() {

                            }
                        });

                    }
                }).show();
    }


    private void saveFile(String fileName, Observer<File> observer) {
        Observable.create((ObservableOnSubscribe<File>) emitter -> {
            File file = new File(Constants.getIniFileParent(this), fileName);
            FileWriter writer = new FileWriter(file);
            writer.write(editText.getText());
            writer.close();
            emitter.onNext(file);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_add_text, menu);
        return true;
    }
}
