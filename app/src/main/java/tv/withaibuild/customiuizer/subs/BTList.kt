@file:Suppress("DEPRECATION")
package tv.withaibuild.customiuizer.subs

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.SubFragment
import tv.withaibuild.customiuizer.mods.GlobalActions
import tv.withaibuild.customiuizer.utils.AppHelper
import tv.withaibuild.customiuizer.utils.Helpers
import java.util.ArrayList

class BTList : SubFragment() {

    var fetchInterval = 15 * 1000
    var key: String? = null
    var listView1: ListView? = null
    var listView2: ListView? = null
    var handler: Handler? = null
    private var btAdapter1: BTAdapter? = null
    private var btAdapter2: BTAdapter? = null
    var mAppContext: Context? = null
    var btList: List<Pair<String, String>> = ArrayList()
    var addresses: java.util.LinkedHashSet<String> = java.util.LinkedHashSet()

    private val devicesReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            val deviceList: ArrayList<BluetoothDevice>? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableArrayListExtra("device_list", BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent?.getParcelableArrayListExtra("device_list")
            }
            val newList = ArrayList<Pair<String, String>>()
            if (deviceList != null) {
                for (device in deviceList) {
                    newList.add(Pair(device.address, device.name))
                }
            }
            btList = newList
            btAdapter1?.notifyDataSetChanged()
            btAdapter2?.notifyDataSetChanged()
            updateProgressBar()
        }
    }

    private val getCachedDevices: Runnable = object : Runnable {
        @SuppressLint("MissingPermission")
        override fun run() {
            if (BluetoothAdapter.getDefaultAdapter()?.isEnabled != true) return
            fetchCachedDevices()
            handler?.postDelayed(this, fetchInterval.toLong())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        this.padded = false
        super.onCreate(savedInstanceState)
    }

    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val args = requireArguments()
        key = args.getString("key")
        addresses = java.util.LinkedHashSet(
            AppHelper.getStringSetOfAppPrefs(key ?: "", emptySet()) ?: emptySet()
        )

        mAppContext = requireContext().applicationContext
        val ctx = mAppContext ?: return
        btAdapter1 = BTAdapter(ctx, true)
        btAdapter2 = BTAdapter(ctx, false)
        handler = Handler(Looper.getMainLooper())

        val view = this.view
        if (view != null) {
            listView1 = view.findViewById(android.R.id.text1)
            listView2 = view.findViewById(android.R.id.text2)

            val fetchStub: ViewStub = view.findViewById(R.id.fetch_devices)
            fetchStub.setLayoutResource(R.layout.pref_item)
            fetchStub.inflate()

            val fetch: View = view.findViewById(R.id.fetch_devices)
            (fetch.findViewById<TextView>(android.R.id.title)).setText(R.string.bt_fetch_devices_title)
            (fetch.findViewById<TextView>(android.R.id.summary)).setText(R.string.bt_fetch_devices_summ)
            fetch.setOnClickListener {
                val newList = ArrayList<Pair<String, String>>()
                btList = newList
                btAdapter1?.notifyDataSetChanged()
                btAdapter2?.notifyDataSetChanged()
                updateProgressBar()
                fetchCachedDevices()
            }
        }
        listView1?.adapter = btAdapter1
        listView1?.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val sr = btAdapter1?.getItem(position) ?: return@OnItemClickListener
            addresses = java.util.LinkedHashSet(
                AppHelper.getStringSetOfAppPrefs(key ?: "", emptySet()) ?: emptySet()
            )
            AppHelper.removeStringPair(addresses, sr.first)
            AppHelper.appPrefs?.edit()?.putStringSet(key ?: "", if (addresses.size == 0) null else addresses)?.apply()
            btAdapter1?.notifyDataSetChanged()
            btAdapter2?.notifyDataSetChanged()
        }
        listView2?.adapter = btAdapter2
        listView2?.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val sr = btAdapter2?.getItem(position) ?: return@OnItemClickListener
            addresses = java.util.LinkedHashSet(
                AppHelper.getStringSetOfAppPrefs(key ?: "", emptySet()) ?: emptySet()
            )
            AppHelper.addStringPair(addresses, sr.first, sr.second)
            AppHelper.appPrefs?.edit()?.putStringSet(key ?: "", addresses)?.apply()
            btAdapter1?.notifyDataSetChanged()
            btAdapter2?.notifyDataSetChanged()
        }

        if (BluetoothAdapter.getDefaultAdapter()?.isEnabled != true) {
            Toast.makeText(activity, R.string.request_bt, Toast.LENGTH_SHORT).show()
        }
        updateProgressBar()
    }

    fun fetchCachedDevices() {
        val intent = Intent(GlobalActions.ACTION_PREFIX + "FetchCachedDevices")
        intent.setPackage("com.android.systemui")
        mAppContext?.sendBroadcast(intent)
    }

    fun updateProgressBar() {
        val isEnabled = BluetoothAdapter.getDefaultAdapter()?.isEnabled == true
        val show = isEnabled && btList.size == 0
        view?.findViewById<View>(R.id.progress_bar)?.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun registerReceivers() {
        unregisterReceivers()
        mAppContext?.registerReceiver(
            devicesReceiver,
            IntentFilter(GlobalActions.EVENT_PREFIX + "CACHEDDEVICESUPDATE"),
            Context.RECEIVER_EXPORTED
        )
        handler?.postDelayed(getCachedDevices, 1000)
    }

    fun unregisterReceivers() {
        handler?.removeCallbacks(getCachedDevices)
        mAppContext?.let {
            try { it.unregisterReceiver(devicesReceiver) } catch (_: Throwable) {}
        }
    }

    override fun onDestroy() {
        unregisterReceivers()
        super.onDestroy()
    }

    override fun onPause() {
        unregisterReceivers()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        registerReceivers()
    }

    inner class BTAdapter(
        private val ctx: Context,
        private val isSelected: Boolean
    ) : BaseAdapter() {
        private val mInflater: LayoutInflater = LayoutInflater.from(ctx)

        override fun getCount(): Int {
            return if (isSelected) addresses.size else btList.size
        }

        override fun getItem(position: Int): Pair<String, String>? {
            return if (isSelected) {
                if (addresses.size == 0) return null
                val addr = addresses.toTypedArray()[position]
                val sep = addr.indexOf('|')
                if (sep == -1) Pair(addr, "") else Pair(addr.substring(0, sep), addr.substring(sep + 1))
            } else {
                btList[position]
            }
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun isEnabled(position: Int): Boolean {
            return isSelected || !Helpers.containsStringPair(addresses, getItem(position)?.first)
        }

        @SuppressLint("MissingPermission")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = if (convertView != null) {
                convertView
            } else {
                mInflater.inflate(R.layout.pref_item, parent, false)
            }

            val itemTitle: TextView = row.findViewById(android.R.id.title)
            val itemSumm: TextView = row.findViewById(android.R.id.summary)
            val sr = getItem(position) ?: return row
            itemTitle.text = sr.second ?: ""
            itemSumm.text = sr.first ?: ""

            if (isEnabled(position)) {
                row.isEnabled = true

                var isBonded = false
                val bonded = BluetoothAdapter.getDefaultAdapter()?.bondedDevices
                if (bonded != null) {
                    for (device in bonded) {
                        if (device.address == sr.first) isBonded = true
                    }
                }

                @Suppress("DEPRECATION")
                val colorRes = if (isBonded) R.color.highlight_normal_light else R.color.preference_primary_text_color
                @Suppress("DEPRECATION")
                itemTitle.setTextColor(resources.getColor(colorRes, activity?.theme))
                itemTitle.alpha = 1.0f
                itemSumm.alpha = 1.0f
            } else {
                row.isEnabled = false
                @Suppress("DEPRECATION")
                itemTitle.setTextColor(resources.getColor(R.color.preference_secondary_text_color, activity?.theme))
                itemTitle.alpha = 0.5f
                itemSumm.alpha = 0.5f
            }
            return row
        }
    }
}
