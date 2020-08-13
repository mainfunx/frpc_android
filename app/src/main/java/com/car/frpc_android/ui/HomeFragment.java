package com.car.frpc_android.ui;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.car.frpc_android.Constants;
import com.car.frpc_android.FrpcService;
import com.car.frpc_android.R;
import com.car.frpc_android.adapter.FileListAdapter;
import com.github.clans.fab.FloatingActionButton;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static android.content.Context.ACTIVITY_SERVICE;

public class HomeFragment extends Fragment {


    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.fab)
    FloatingActionButton fab;
    @BindView(R.id.refreshView)
    SwipeRefreshLayout refreshView;
    @BindView(R.id.tv_state)
    TextView tvState;
    @BindView(R.id.tv_logcat)
    TextView tvLogcat;
    @BindView(R.id.sv_logcat)
    NestedScrollView svLogcat;

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
        listAdapter.addChildClickViewIds(R.id.iv_delete, R.id.iv_edit, R.id.info_container);

        listAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            if (view.getId() == R.id.iv_edit) {
                editIni(position);
            } else if (view.getId() == R.id.iv_delete) {
                deleteFile(position);
            } else if (view.getId() == R.id.info_container) {
                if (isRunService(getContext())) {
                    Toast.makeText(getContext(), R.string.needStopService, Toast.LENGTH_SHORT).show();
                    return;
                }
                listAdapter.setSelectItem(listAdapter.getItem(position));
            }
        });
        recyclerView.setAdapter(listAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        refreshView.setOnRefreshListener(this::setData);

        syncServiceState();
    }

    private void syncServiceState() {
        if (!isRunService(getContext())) {
            setServiceState(R.color.colorPlay, R.drawable.ic_play_white, R.string.notOpened);
        } else {
            setServiceState(R.color.colorStop, R.drawable.ic_stop_white, R.string.hasOpened);
        }
    }

    private void setServiceState(int color, int res, int text) {
        fab.setColorNormal(getResources().getColor(color));
        fab.setImageResource(res);
        tvState.setText(text);
    }

    @Override
    public void onResume() {
        super.onResume();
        setData();
    }

    private void editIni(int position) {
        File item = listAdapter.getItem(position);
        checkPermissions(aBoolean -> {
            if (!aBoolean) {
                Constants.tendToSettings(getContext());
                return;
            }
            Intent intent = new Intent(getContext(), IniEditActivity.class);
            intent.putExtra(getString(R.string.intent_key_file), item.getPath());
            startActivity(intent);
        });

    }

    private void deleteFile(int position) {
        File item = listAdapter.getItem(position);
        Observable.just(item)
                .map(file -> item.delete())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        if (aBoolean) {
                            listAdapter.removeAt(position);
                        } else {
                            Toast.makeText(getContext(), item.getName() + getString(R.string.actionDeleteFailed), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void setData() {
        getFiles().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<File>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        refreshView.setRefreshing(true);

                    }

                    @Override
                    public void onNext(List<File> files) {
                        refreshView.setRefreshing(false);
                        listAdapter.setList(files);
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();

                    }

                    @Override
                    public void onComplete() {
                        refreshView.setRefreshing(false);

                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind.unbind();
    }

    public Observable<List<File>> getFiles() {
        return Observable.create((ObservableOnSubscribe<List<File>>) emitter -> {
            File path = Constants.getIniFileParent(getContext());
            File[] files = path.listFiles();
            emitter.onNext(files != null ? Arrays.asList(files) : new ArrayList<>());
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

    }


    private void checkPermissions(Consumer<Boolean> consumer) {
        Disposable subscribe = new RxPermissions(this)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE)
                .subscribe(consumer);


    }

    @OnClick(R.id.fab)
    public void onViewClicked() {
        if (isRunService(getContext())) {
            getContext().stopService(new Intent(getContext(), FrpcService.class));
            setServiceState(R.color.colorPlay, R.drawable.ic_play_white, R.string.notOpened);
        } else {

            if (listAdapter.getSelectItem() == null) {
                Toast.makeText(getContext(), R.string.notSelectIni, Toast.LENGTH_SHORT).show();
                return;
            }
            readLog();
            Intent service = new Intent(getContext(), FrpcService.class);
            service.putExtra(getResources().getString(R.string.intent_key_file), listAdapter.getSelectItem().getPath());
            getContext().startService(service);
            setServiceState(R.color.colorStop, R.drawable.ic_stop_white, R.string.hasOpened);
        }

    }

    public boolean isRunService(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            String simpleName = FrpcService.class.getName();
            if (simpleName.equals(service.process)) {
                return true;
            }
        }
        return false;
    }

    private Disposable readingLog = null;

    private void readLog() {
        tvLogcat.setText("");
        if (readingLog != null) return;
        HashSet<String> lst = new LinkedHashSet<String>();
        lst.add("logcat");
        lst.add("-T");
        lst.add("0");
        lst.add("-v");
        lst.add("time");
        lst.add("-s");
        lst.add("GoLog,com.car.frpc_android.FrpcService");
        readingLog = Observable.create((ObservableOnSubscribe<String>) emitter -> {

            Process process = Runtime.getRuntime().exec(lst.toArray(new String[0]));

            InputStreamReader in = new InputStreamReader(process.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(in);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                emitter.onNext(line);
            }
            in.close();
            bufferedReader.close();
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        tvLogcat.append(s);
                        tvLogcat.append("\r\n");
                        svLogcat.fullScroll(View.FOCUS_DOWN);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                        tvLogcat.append(throwable.toString());
                        tvLogcat.append("\r\n");
                    }
                });


    }

}
