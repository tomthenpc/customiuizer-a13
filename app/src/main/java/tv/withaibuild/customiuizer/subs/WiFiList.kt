@file:Suppress("DEPRECATION")
package tv.withaibuild.customiuizer.subs

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.NetworkInfo
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
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
import tv.withaibuild.customiuizer.utils.AppHelper
import tv.withaibuild.customiuizer.utils.Helpers
import java.util.ArrayList

class WiFiList : SubFragment() {

    var scanInterval = 15 * 1000
    var key: String? = null
    var listView1: ListView? = null
    var listView2: ListView? = null
    var handler: Handler? = null
    private var wifiAdapter1: WiFiAdapter? = null
    private var wifiAdapter2: WiFiAdapter? = null
    var wifiManager: WifiManager? = null
    var mAppContext: Context? = null
    var wifiList: List<ScanResult> = ArrayList()
    var bssids: java.util.LinkedHashSet<String> = java.util.LinkedHashSet()

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION -> {
                    wifiList = try {
                        wifiManager?.scanResults ?: ArrayList()
                    } catch (ignore: SecurityException) {
                        ArrayList()
                    }
                    wifiAdapter1?.notifyDataSetChanged()
                    wifiAdapter2?.notifyDataSetChanged()
                    updateProgressBar()
                }
                WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                    val netInfo: NetworkInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO, NetworkInfo::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)
                    }
                    if (netInfo == null) return
                    if (netInfo.detailedState == NetworkInfo.DetailedState.CONNECTED) isWiFiReady()
                    if (netInfo.detailedState == NetworkInfo.DetailedState.CONNECTED ||
                        netInfo.detailedState == NetworkInfo.DetailedState.DISCONNECTED
                    ) {
                        handler?.removeCallbacks(getScanResults)
                        handler?.postDelayed(getScanResults, 1000)
                    }
                }
            }
        }
    }

    private val getScanResults: Runnable = object : Runnable {
        override fun run() {
            try {
                wifiManager?.startScan()
            } catch (ignore: Throwable) {
                if (ignore is OutOfMemoryError || ignore is ThreadDeath || ignore is VirtualMachineError) throw ignore
            }
            handler?.postDelayed(this, scanInterval.toLong())
            this@WiFiList.updateProgressBar()
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
        bssids = java.util.LinkedHashSet(
            AppHelper.getStringSetOfAppPrefs(key ?: "", emptySet()) ?: emptySet()
        )

        mAppContext = requireContext().applicationContext
        wifiManager = mAppContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val ctx = mAppContext ?: return
        wifiAdapter1 = WiFiAdapter(ctx, true)
        wifiAdapter2 = WiFiAdapter(ctx, false)
        handler = Handler(Looper.getMainLooper())

        val view = this.view
        if (view != null) {
            listView1 = view.findViewById(android.R.id.text1)
            listView2 = view.findViewById(android.R.id.text2)

            val locationStub: ViewStub = view.findViewById(R.id.location_settings)
            locationStub.setLayoutResource(R.layout.pref_item)
            locationStub.inflate()

            val location: View = view.findViewById(R.id.location_settings)
            (location.findViewById<TextView>(android.R.id.title)).setText(R.string.wifi_location_title)
            (location.findViewById<TextView>(android.R.id.summary)).setText(R.string.wifi_location_summ)
            location.setOnClickListener {
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        }
        listView1?.adapter = wifiAdapter1
        listView1?.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            val sr = wifiAdapter1?.getItem(position) ?: return@OnItemClickListener
            bssids = java.util.LinkedHashSet(
                AppHelper.getStringSetOfAppPrefs(key ?: "", emptySet()) ?: emptySet()
            )
            AppHelper.removeStringPair(bssids, sr.first)
            AppHelper.appPrefs?.edit()?.putStringSet(key ?: "", if (bssids.size == 0) null else bssids)?.apply()
            wifiAdapter1?.notifyDataSetChanged()
            wifiAdapter2?.notifyDataSetChanged()
        }
        listView2?.adapter = wifiAdapter2
        listView2?.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val sr = wifiAdapter2?.getItem(position) ?: return@OnItemClickListener
            bssids = java.util.LinkedHashSet(
                AppHelper.getStringSetOfAppPrefs(key ?: "", emptySet()) ?: emptySet()
            )
            AppHelper.addStringPair(bssids, sr.first, sr.second)
            AppHelper.appPrefs?.edit()?.putStringSet(key ?: "", bssids)?.apply()
            wifiAdapter1?.notifyDataSetChanged()
            wifiAdapter2?.notifyDataSetChanged()
        }

        isWiFiReady()
        updateProgressBar()
    }

    fun updateProgressBar() {
        val view = this.view
        val isWifiEnabled = wifiManager?.isWifiEnabled == true
        val isLocationEnabled = isLocationServicesEnabled()
        val isLoading = isWifiEnabled && isLocationEnabled && wifiList.size == 0
        view?.findViewById<View>(R.id.progress_bar)?.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    fun isLocationServicesEnabled(): Boolean {
        val locationManager = activity?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (t: Throwable) {
            if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
            false
        }
    }

    fun isWiFiReady() {
        val act = activity ?: return
        if (wifiManager?.isWifiEnabled == false) {
            Toast.makeText(act, R.string.request_wifi, Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !isLocationServicesEnabled()) {
            Toast.makeText(act, R.string.request_location, Toast.LENGTH_LONG).show()
        }
    }

    fun registerReceivers() {
        unregisterReceivers()
        isWiFiReady()
        val intentFilter = IntentFilter().apply {
            addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        }
        mAppContext?.registerReceiver(wifiReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        handler?.postDelayed(getScanResults, 1000)
    }

    fun unregisterReceivers() {
        handler?.removeCallbacks(getScanResults)
        mAppContext?.let {
            try { it.unregisterReceiver(wifiReceiver) } catch (fatal: Throwable) {
                if (fatal is OutOfMemoryError || fatal is ThreadDeath || fatal is VirtualMachineError) throw fatal
}
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

    inner class WiFiAdapter(
        private val ctx: Context,
        private val isSelected: Boolean
    ) : BaseAdapter() {
        private val mInflater: LayoutInflater = LayoutInflater.from(ctx)

        override fun getCount(): Int {
            return if (isSelected) bssids.size else wifiList.size
        }

        @SuppressLint("MissingPermission")
        override fun getItem(position: Int): Pair<String, String>? {
            return if (isSelected) {
                if (bssids.size == 0) return null
                val addresses = bssids.toTypedArray()
                if (position >= addresses.size) return null
                val address = addresses[position]
                val sep = address.indexOf('|')
                if (sep == -1) Pair(address, "") else Pair(address.substring(0, sep), address.substring(sep + 1))
            } else {
                val scanResult = wifiList[position]
                Pair(scanResult.BSSID, scanResult.SSID ?: "")
            }
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun isEnabled(position: Int): Boolean {
            return isSelected || !Helpers.containsStringPair(bssids, getItem(position)?.first)
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
                val bssid = wifiManager?.connectionInfo?.bssid
                val colorRes = if (sr.first == bssid) R.color.highlight_normal_light else R.color.preference_primary_text_color
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
