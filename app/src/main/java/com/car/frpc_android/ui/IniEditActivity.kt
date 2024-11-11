package com.car.frpc_android.ui

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.car.frpc_android.R
import com.car.frpc_android.database.AppDatabase
import com.car.frpc_android.database.Config
import com.github.ahmadaghazadeh.editor.widget.CodeEditor
import com.jeremyliao.liveeventbus.LiveEventBus
import io.reactivex.CompletableObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.UUID

class IniEditActivity : AppCompatActivity() {

    private val editText: CodeEditor by lazy { findViewById(R.id.editText) }

    private val toolbar: Toolbar by lazy { findViewById(R.id.toolbar) }

    private var config: Config? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ini_edit)
        initToolbar()

        LiveEventBus.get(INTENT_EDIT_INI, Config::class.java).observeSticky(
            this
        ) { value: Config? ->
            config = value
            editText.setText(config!!.cfg, 1)
            toolbar.title =
                if (TextUtils.isEmpty(config!!.name)) getString(R.string.noName) else config!!.name
        }
    }


    private fun initToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_template -> startActivity(
                Intent(
                    this@IniEditActivity,
                    TemplateActivity::class.java
                )
            )

            R.id.action_save -> actionSave()
        }
        return super.onOptionsItemSelected(item)
    }


    private fun actionSave() {
        MaterialDialog.Builder(this)
            .title(if (TextUtils.isEmpty(config!!.name)) R.string.titleInputFileName else R.string.titleModifyFileName)
            .canceledOnTouchOutside(false)
            .autoDismiss(false)
            .negativeText(R.string.cancel)
            .positiveText(R.string.done)
            .onNegative { dialog: MaterialDialog, which: DialogAction -> dialog.dismiss() }
            .input(
                "",
                if (TextUtils.isEmpty(config!!.name)) "" else config!!.name,
                false
            ) { dialog: MaterialDialog, input: CharSequence ->
                config!!.setName(input.toString())
                    .setCfg(editText.text)
                val action =
                    if (TextUtils.isEmpty(config!!.uid)) AppDatabase.getInstance(this@IniEditActivity)
                        .configDao()
                        .insert(
                            config!!.setUid(
                                UUID.randomUUID().toString()
                            )
                        ) else AppDatabase.getInstance(
                        this@IniEditActivity
                    )
                        .configDao()
                        .update(config)
                action
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : CompletableObserver {
                        override fun onSubscribe(d: Disposable) {
                        }

                        override fun onComplete() {
                            Toast.makeText(
                                this@IniEditActivity.applicationContext,
                                R.string.tipSaveSuccess,
                                Toast.LENGTH_SHORT
                            ).show()
                            dialog.dismiss()
                            LiveEventBus.get<Any?>(HomeFragment.EVENT_UPDATE_CONFIG).post(config)
                            finish()
                        }

                        override fun onError(e: Throwable) {
                            Toast.makeText(this@IniEditActivity, e.message, Toast.LENGTH_SHORT)
                                .show()
                        }
                    })
            }.show()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_add_text, menu)
        return true
    }

    companion object {
        const val INTENT_EDIT_INI: String = "INTENT_EDIT_INI"
    }
}
