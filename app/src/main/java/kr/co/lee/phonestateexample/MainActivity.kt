package kr.co.lee.phonestateexample

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.*
import android.net.wifi.WifiManager
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    lateinit var listView: ListView
    lateinit var listDatas: ArrayList<String>
    lateinit var listAdapter: ArrayAdapter<String>

    lateinit var telephonyManager: TelephonyManager

    var connectivityManager: ConnectivityManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.lab1_listview)

        // 값이 바뀌는 것을 확인하기 위한 ListView
        listDatas = ArrayList()
        listAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listDatas)
        listView.adapter = listAdapter

        // TelephonyManager 객체 생성
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        // listen 메서드의 첫 번째 매개변수로 phoneStateListener를 구현한 클래스를 등록
        // 뒤의 매개변수는 어느 상태변화를 감지할 것인지
        telephonyManager.listen(phoneStateListener,
            PhoneStateListener.LISTEN_CALL_STATE or PhoneStateListener.LISTEN_SERVICE_STATE)

        getTelephonyService()
        checkNetwork()

        // IntentFilter 생성
        val wifiIntentFilter = IntentFilter()
        // action설정
        wifiIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        // 동적으로 Receiver 등록
        registerReceiver(wifiReceiver, wifiIntentFilter)

        checkWifi()
    }

    // 휴대전화의 여러 상태를 받기 위한 PhoneStateListener
    val phoneStateListener = object: PhoneStateListener() {
        // 서비스 상태 변경
        override fun onServiceStateChanged(serviceState: ServiceState?) {
            when(serviceState?.state) {
                ServiceState.STATE_EMERGENCY_ONLY ->
                    listDatas.add("onServiceStateChanged STATE_EMERGENCY_ONLY")
                ServiceState.STATE_IN_SERVICE ->
                    listDatas.add("onServiceStateChanged STATE_IN_SERVICE")
                ServiceState.STATE_OUT_OF_SERVICE ->
                    listDatas.add("onServiceStateChanged STATE_OUT_OF_SERVICE")
                ServiceState.STATE_POWER_OFF ->
                    listDatas.add("onServiceStateChanged STATE_POWER_OFF")
            }

            listAdapter.notifyDataSetChanged()
        }

        // 통화 상태 변경
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            when(state) {
                TelephonyManager.CALL_STATE_IDLE ->
                    listDatas.add("onCallStateChanged CALL_STATE_IDLE : $phoneNumber")
                TelephonyManager.CALL_STATE_RINGING ->
                    listDatas.add("onCallStateChanged CALL_STATE_RINGING : $phoneNumber")
                TelephonyManager.CALL_STATE_OFFHOOK ->
                    listDatas.add("onCallStateChanged CALL_STATE_OFFHOOK : $phoneNumber")
            }
            listAdapter.notifyDataSetChanged()
        }
    }

    // TelephonyManager를 이용하여 스마트폰 정보 획득
    private fun getTelephonyService() {
        listDatas.add("countryIso : ${telephonyManager.networkCountryIso}")
        listDatas.add("operatorName : ${telephonyManager.networkOperatorName}")
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            == PackageManager.PERMISSION_GRANTED) {
            listDatas.add("PhoneNumber : ${telephonyManager.line1Number}")
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), 100)
        }
    }
    
    private fun checkNetwork() {
        // 네트워크 접속정보를 확인하기 위한 ConnectivityManager
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        // 네트워크 정보를 설정하는 NetworkRequest 객체
        val networkReq = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
        
        // registerNetworkCallback을 사용하여 네트워크가 사용 가능하거나 불가능할 때마다 콜백 메서드 호출
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 매개변수로 NetworkRequest, NetworkCallback 객체
            connectivityManager?.registerNetworkCallback(networkReq,
                object : ConnectivityManager.NetworkCallback() {
                // 네트워크가 연결상태 일 때 호출되는 콜백 메서드
                override fun onAvailable(network: Network) {
                    // 활성화된 네트워크의 기능
                    val capabilities = connectivityManager?.getNetworkCapabilities(network)
                    if(capabilities != null) {
                        // Wifi 네트워크인 경우
                        if(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                            Log.d(TAG, "NetworkInfo : Online - WIFI")
                        // 이동통신망 네트워크 인 경우
                        } else if(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                            Log.d(TAG, "NetworkInfo : Online - CELLULAR")
                        }
                    }
                }

                // 네트워크가 발견되지 않았을 시 호출되는 콜백메서드
                override fun onUnavailable() {
                    Log.d(TAG, "NetworkInfo : onUnavailable")
                }

                // 네우워크가 끊겼을 경우 호출되거나 이 request를 더이상 만족하지 못하는 경우 호출되는 콜백메서드
                override fun onLost(network: Network) {
                    Log.d(TAG, "NetworkInfo : onLost")
                }
            })
        }
    }

    // 와이파이의 상태를 한번 파악하기 위한 메서드
    private fun checkWifi() {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        
        // 현재 wifi가 enable이 아니라면
        if(!wifiManager.isWifiEnabled) {
            listDatas.add("WifiManager : wifi disabled")
        } else { // wifi가 enable이라면
            listDatas.add("WifiManager : wifi enabled")
        }
    }

    // 와이파이의 상태를 파악하는 브로드캐스트 리시버
    val wifiReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // 와이파이 상태 변경 시
            if(intent?.action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
                // 상태획득
                val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1)
                // 가능 상태라면
                if(state == WifiManager.WIFI_STATE_ENABLED) {
                    listDatas.add("WIFI_STATE_CHANGED_ACTION : enable")
                } else { // 불가능 상태라면
                    listDatas.add("WIFI_STATE_CHANGED_ACTION : disable")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 브로드캐스트 리시버 등록 해제
        unregisterReceiver(wifiReceiver)
    }

    companion object {
        const val TAG = "Network Information"
    }
}