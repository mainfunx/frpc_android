package com.car.frpc_android.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.car.frpc_android.CommonUtils
import com.car.frpc_android.R
import com.github.ahmadaghazadeh.editor.widget.CodeEditor
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class TemplateActivity : AppCompatActivity() {

    private val editText: CodeEditor by lazy { findViewById(R.id.editText) }

    private val toolbar: Toolbar by lazy { findViewById(R.id.toolbar) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ini_edit)

        initToolbar()

        initEdit()
    }

    private fun initEdit() {
        CommonUtils.getStringFromRaw(this@TemplateActivity, R.raw.frpc_full)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<String> {
                override fun onSubscribe(d: Disposable) {
                }

                override fun onNext(content: String) {
                    editText!!.setText(content, 1)
                }

                override fun onError(e: Throwable) {
                }

                override fun onComplete() {
                }
            })
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar!!.setNavigationOnClickListener { v: View? -> finish() }
        toolbar!!.setTitle(R.string.titleTemplate)
    }
}
