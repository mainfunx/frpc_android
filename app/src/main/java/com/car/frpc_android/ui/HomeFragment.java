package com.car.frpc_android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.afollestad.materialdialogs.MaterialDialog;
import com.car.frpc_android.CommonUtils;
import com.car.frpc_android.FrpcService;
import com.car.frpc_android.R;
import com.car.frpc_android.adapter.FileListAdapter;
import com.car.frpc_android.database.AppDatabase;
import com.car.frpc_android.database.Config;
import com.jeremyliao.liveeventbus.LiveEventBus;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import frpclib.Frpclib;
import io.reactivex.CompletableObserver;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class HomeFragment extends Fragment {
    public static final String EVENT_UPDATE_CONFIG = "EVENT_UPDATE_CONFIG";
    public static final String EVENT_RUNNING_ERROR = "EVENT_RUNNING_ERROR";

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.refreshView)
    SwipeRefreshLayout refreshView;

    private Unbinder bind;
    private FileListAdapter listAdapter;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        bind = ButterKnife.bind(this, root);
        init();
        return root;
    }

    private void init() {
        listAdapter = new FileListAdapter();
        listAdapter.addChildClickViewIds(R.id.iv_play, R.id.iv_delete, R.id.iv_edit);

        listAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            Config item = listAdapter.getItem(position);
            if (view.getId() == R.id.iv_play) {
                if (!CommonUtils.isServiceRunning(FrpcService.class.getName(), getContext())) {
                    getContext().startService(new Intent(getContext(), FrpcService.class));
                }
                if (Frpclib.isRunning(item.getUid())) {
                    Frpclib.close(item.getUid());
                    item.setConnecting(false);
                    listAdapter.notifyItemChanged(position);
                    checkAndStopService();
                    return;
                }
                CommonUtils.waitService(FrpcService.class.getName(), getContext())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new CompletableObserver() {
                            MaterialDialog progress;

                            @Override
                            public void onSubscribe(Disposable d) {
                                progress = new MaterialDialog.Builder(getContext())
                                        .content(R.string.tipWaitService)
                                        .canceledOnTouchOutside(false)
                                        .progress(true, 100)
                                        .show();

                            }

                            @Override
                            public void onComplete() {
                                progress.dismiss();
                                LiveEventBus.get(FrpcService.INTENT_KEY_FILE).postAcrossProcess(item.getUid());
                                item.setConnecting(true);
                                listAdapter.notifyItemChanged(position);


                            }

                            @Override
                            public void onError(Throwable e) {

                            }
                        });
                return;
            }

            if (Frpclib.isRunning(item.getUid())) {
                Toast.makeText(getContext(), getResources().getText(R.string.tipServiceRunning), Toast.LENGTH_SHORT).show();
                return;
            }
            if (view.getId() == R.id.iv_edit) {
                editConfig(position);
                return;
            }
            if (view.getId() == R.id.iv_delete) {
                new MaterialDialog.Builder(getContext())
                        .title(R.string.dialogConfirmTitle)
                        .content(R.string.configDeleteConfirm)
                        .canceledOnTouchOutside(false)
                        .negativeText(R.string.cancel)
                        .positiveText(R.string.done)
                        .onNegative((dialog, which) -> dialog.dismiss())
                        .onPositive((dialog, which) -> deleteConfig(position))
                        .show();
            }
        });
        recyclerView.setAdapter(listAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        refreshView.setOnRefreshListener(() -> getData());
        LiveEventBus.get(EVENT_UPDATE_CONFIG, Config.class).observe(this, config -> {
            int position = listAdapter.getData().indexOf(config);
            if (position < 0) {
                listAdapter.addData(config);
            } else {
                listAdapter.notifyItemChanged(position);
            }
        });
        LiveEventBus.get(EVENT_RUNNING_ERROR, String.class).observe(this, uid -> {

            int position = listAdapter.getData().indexOf(new Config().setUid(uid));
            Config item = listAdapter.getItem(position);
            item.setConnecting(false);
            listAdapter.notifyItemChanged(position);
            checkAndStopService();
        });


        recyclerView.postDelayed(this::getData, 1500);

    }

    private void checkAndStopService() {
        if (TextUtils.isEmpty(Frpclib.getUids())) {
            getContext().stopService(new Intent(getContext(), FrpcService.class));
        }
    }


    private void editConfig(int position) {
        Config item = listAdapter.getItem(position);
        LiveEventBus.get(IniEditActivity.INTENT_EDIT_INI).post(item);
        startActivity(new Intent(getContext(), IniEditActivity.class));

    }

    private void deleteConfig(int position) {
        AppDatabase.getInstance(getContext())
                .configDao()
                .delete(listAdapter.getItem(position))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        listAdapter.removeAt(position);

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });

    }

    private void getData() {
        AppDatabase.getInstance(getContext())
                .configDao()
                .getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<Config>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        refreshView.setRefreshing(true);

                    }

                    @Override
                    public void onSuccess(@NonNull List<Config> configs) {
                        refreshView.setRefreshing(false);
                        listAdapter.setList(configs);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        refreshView.setRefreshing(false);

                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind.unbind();
    }


}
