package com.car.frpc_android.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.car.frpc_android.CommonUtils
import com.car.frpc_android.FrpcService
import com.car.frpc_android.R
import com.car.frpc_android.adapter.FileListAdapter
import com.car.frpc_android.database.AppDatabase
import com.car.frpc_android.database.Config
import com.chad.library.adapter.base.BaseQuickAdapter
import com.jeremyliao.liveeventbus.LiveEventBus
import frpclib.Frpclib
import io.reactivex.CompletableObserver
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class HomeFragment : Fragment() {

    private var _recyclerView: RecyclerView? = null
    private val recyclerView
        get() = _recyclerView!!

    private var _refreshView: SwipeRefreshLayout? = null
    private val refreshView
        get() = _refreshView!!

    private var listAdapter: FileListAdapter? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        _recyclerView = root.findViewById(R.id.recyclerView)
        _refreshView = root.findViewById(R.id.refreshView)
        init()
        return root
    }

    private fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // 在低于API 33的设备上，通知权限默认是启用的
        }
    }

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            if (ok) {
                requireContext().startService(Intent(context, FrpcService::class.java))
            } else {
                Toast.makeText(requireContext(),
                    getString(R.string.please_allow_notification_permissions), Toast.LENGTH_SHORT).show()
            }
        }

    @SuppressLint("InlinedApi")
    private fun init() {
        listAdapter = FileListAdapter()
        listAdapter!!.addChildClickViewIds(R.id.iv_play, R.id.iv_delete, R.id.iv_edit)

        listAdapter!!.setOnItemChildClickListener { adapter: BaseQuickAdapter<*, *>?, view: View, position: Int ->
            val item = listAdapter!!.getItem(position)
            if (view.id == R.id.iv_play) {
                if (!CommonUtils.isServiceRunning(FrpcService::class.java.name, context)) {
                    if (isNotificationPermissionGranted(requireContext())){
                        requireContext().startService(Intent(context, FrpcService::class.java))
                    }else{
                        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                if (Frpclib.isRunning(item.uid)) {
                    Frpclib.close(item.uid)
                    item.setConnecting(false)
                    listAdapter!!.notifyItemChanged(position)
                    checkAndStopService()
                    return@setOnItemChildClickListener
                }
                CommonUtils.waitService(FrpcService::class.java.name, context)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : CompletableObserver {
                        var progress: MaterialDialog? = null

                        override fun onSubscribe(d: Disposable) {
                            progress = MaterialDialog.Builder(context!!)
                                .content(R.string.tipWaitService)
                                .canceledOnTouchOutside(false)
                                .progress(true, 100)
                                .show()
                        }

                        override fun onComplete() {
                            progress!!.dismiss()
                            LiveEventBus.get<Any>(FrpcService.INTENT_KEY_FILE)
                                .postAcrossProcess(item.uid)
                            item.setConnecting(true)
                            listAdapter!!.notifyItemChanged(position)
                        }

                        override fun onError(e: Throwable) {
                        }
                    })
                return@setOnItemChildClickListener
            }

            if (Frpclib.isRunning(item.uid)) {
                Toast.makeText(
                    context,
                    resources.getText(R.string.tipServiceRunning),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnItemChildClickListener
            }
            if (view.id == R.id.iv_edit) {
                editConfig(position)
                return@setOnItemChildClickListener
            }
            if (view.id == R.id.iv_delete) {
                MaterialDialog.Builder(requireContext())
                    .title(R.string.dialogConfirmTitle)
                    .content(R.string.configDeleteConfirm)
                    .canceledOnTouchOutside(false)
                    .negativeText(R.string.cancel)
                    .positiveText(R.string.done)
                    .onNegative { dialog: MaterialDialog, which: DialogAction? -> dialog.dismiss() }
                    .onPositive { dialog: MaterialDialog?, which: DialogAction? ->
                        deleteConfig(
                            position
                        )
                    }
                    .show()
            }
        }
        recyclerView.adapter = listAdapter
        recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        refreshView.setOnRefreshListener { data }
        LiveEventBus.get(EVENT_UPDATE_CONFIG, Config::class.java).observe(
            this
        ) { config: Config ->
            val position = listAdapter!!.data.indexOf(config)
            if (position < 0) {
                listAdapter!!.addData(config)
            } else {
                listAdapter!!.notifyItemChanged(position)
            }
        }
        LiveEventBus.get(EVENT_RUNNING_ERROR, String::class.java).observe(
            this
        ) { uid: String? ->
            val position = listAdapter!!.data.indexOf(
                Config().setUid(
                    uid!!
                )
            )
            val item = listAdapter!!.getItem(position)
            item.setConnecting(false)
            listAdapter!!.notifyItemChanged(position)
            checkAndStopService()
        }


        recyclerView.postDelayed({ this.data }, 1500)
    }

    private fun checkAndStopService() {
        if (TextUtils.isEmpty(Frpclib.getUids())) {
            requireContext().stopService(Intent(context, FrpcService::class.java))
        }
    }


    private fun editConfig(position: Int) {
        val item = listAdapter!!.getItem(position)
        LiveEventBus.get<Any>(IniEditActivity.INTENT_EDIT_INI).post(item)
        startActivity(Intent(context, IniEditActivity::class.java))
    }

    private fun deleteConfig(position: Int) {
        AppDatabase.getInstance(context)
            .configDao()
            .delete(listAdapter!!.getItem(position))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : CompletableObserver {
                override fun onSubscribe(d: Disposable) {
                }

                override fun onComplete() {
                    listAdapter!!.removeAt(position)
                }

                override fun onError(e: Throwable) {
                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private val data: Unit
        get() {
            AppDatabase.getInstance(context)
                .configDao()
                .all
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<List<Config?>> {
                    override fun onSubscribe(d: Disposable) {
                        refreshView.isRefreshing = true
                    }

                    override fun onSuccess(configs: List<Config?>) {
                        refreshView.isRefreshing = false
                        listAdapter!!.setList(configs)
                    }

                    override fun onError(e: Throwable) {
                        refreshView.isRefreshing = false
                    }
                })
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _refreshView = null
        _recyclerView = null
    }


    companion object {
        const val EVENT_UPDATE_CONFIG: String = "EVENT_UPDATE_CONFIG"
        const val EVENT_RUNNING_ERROR: String = "EVENT_RUNNING_ERROR"
    }
}
