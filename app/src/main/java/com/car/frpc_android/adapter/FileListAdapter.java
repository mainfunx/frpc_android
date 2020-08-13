package com.car.frpc_android.adapter;

import android.util.Log;
import android.widget.RadioButton;

import com.car.frpc_android.R;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class FileListAdapter extends BaseQuickAdapter<File, BaseViewHolder> {


    private File selectItem = null;

    public FileListAdapter() {
        super(R.layout.item_recycler_main);
    }

    public FileListAdapter setSelectItem(File selectItem) {
        this.selectItem = selectItem;
        notifyDataSetChanged();
        return this;
    }

    public File getSelectItem() {
        return selectItem;
    }

    @Override
    public void removeAt(int position) {
        File item = getItem(position);
        if (selectItem != null && item.getPath().equals(selectItem.getPath())) {
            selectItem = null;
        }
        super.removeAt(position);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, File file) {
        baseViewHolder.setText(R.id.tv_name, file.getName());
        RadioButton btnRadio = baseViewHolder.findView(R.id.btn_radio);
        btnRadio.setChecked(selectItem != null && selectItem.getPath().equals(file.getPath()));

    }


}
