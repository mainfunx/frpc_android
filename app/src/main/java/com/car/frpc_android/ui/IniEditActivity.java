package com.car.frpc_android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.car.frpc_android.R;
import com.car.frpc_android.database.AppDatabase;
import com.car.frpc_android.database.Config;
import com.github.ahmadaghazadeh.editor.widget.CodeEditor;
import com.jeremyliao.liveeventbus.LiveEventBus;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class IniEditActivity extends AppCompatActivity {

    public static final String INTENT_EDIT_INI = "INTENT_EDIT_INI";
    @BindView(R.id.editText)
    CodeEditor editText;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private Config config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ini_edit);
        ButterKnife.bind(this);
        initToolbar();

        LiveEventBus.get(INTENT_EDIT_INI, Config.class).observeSticky(this, value -> {
            config = value;
            editText.setText(config.getCfg(), 1);
            toolbar.setTitle(TextUtils.isEmpty(config.getName()) ? getString(R.string.noName) : config.getName());
        });


    }


    private void initToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
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
                .title(TextUtils.isEmpty(config.getName()) ? R.string.titleInputFileName : R.string.titleModifyFileName)
                .canceledOnTouchOutside(false)
                .autoDismiss(false)
                .negativeText(R.string.cancel)
                .positiveText(R.string.done)
                .onNegative((dialog, which) -> dialog.dismiss())
                .input("", TextUtils.isEmpty(config.getName()) ? "" : config.getName(), false, (dialog, input) ->
                {
                    config.setName(input.toString())
                            .setCfg(editText.getText());
                    Completable action =
                            TextUtils.isEmpty(config.getUid()) ?
                            AppDatabase.getInstance(IniEditActivity.this)
                                    .configDao()
                                    .insert(config.setUid(UUID.randomUUID().toString())) :
                            AppDatabase.getInstance(IniEditActivity.this)
                                    .configDao()
                                    .update(config);
                    action
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new CompletableObserver() {
                                @Override
                                public void onSubscribe(@NonNull Disposable d) {

                                }

                                @Override
                                public void onComplete() {
                                    Toast.makeText(IniEditActivity.this.getApplicationContext(), R.string.tipSaveSuccess, Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                    LiveEventBus.get(HomeFragment.EVENT_UPDATE_CONFIG).post(config);
                                    finish();
                                }

                                @Override
                                public void onError(@NonNull Throwable e) {
                                    Toast.makeText(IniEditActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

                                }
                            });
                }).show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_add_text, menu);
        return true;
    }
}
