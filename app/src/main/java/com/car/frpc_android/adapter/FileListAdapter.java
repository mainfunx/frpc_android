package com.car.frpc_android.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;

import androidx.core.widget.ContentLoadingProgressBar;
import androidx.core.widget.ImageViewCompat;

import com.car.frpc_android.CommonUtils;
import com.car.frpc_android.R;
import com.car.frpc_android.database.Config;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import frpclib.Frpclib;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class FileListAdapter extends BaseQuickAdapter<Config, BaseViewHolder> {


    public FileListAdapter() {
        super(R.layout.item_recycler_main);
    }



    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, Config file) {
        baseViewHolder.setText(R.id.tv_name, file.getName());
        boolean running =(file.getConnecting() != null && file.getConnecting())|| Frpclib.isRunning(file.getUid());
        baseViewHolder.setImageResource(R.id.iv_play, running ? R.drawable.ic_stop_white : R.drawable.ic_play_white);
        ImageViewCompat.setImageTintList(baseViewHolder.getView(R.id.iv_play), ColorStateList.valueOf(getContext().getResources().getColor(running ? R.color.colorStop : R.color.black)));




    }


}
