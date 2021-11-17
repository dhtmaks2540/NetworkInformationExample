package kr.co.lee.phonestateexample

import android.Manifest
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.TelephonyManager
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    lateinit var listView: ListView
    lateinit var listDatas: ArrayList<String>
    lateinit var listAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.lab1_listview)

        listDatas = ArrayList()
        listAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listDatas)
        listView.adapter = listAdapter

        // TelephonyManager 객체 생성
        val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        // TelePhoneManager의 listen 메서드의 첫 번째 매개변수로 phoneStateListener를 구현한 클래스를 등록
        // 뒤의 매개변수는 어느 상태변화를 감지할 것인지
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE or PhoneStateListener.LISTEN_SERVICE_STATE)
        
        // TelephonyManager를 이용하여 스마트폰 정보 획득
        listDatas.add("countryIso : ${telephonyManager.networkCountryIso}")
        listDatas.add("operatorName : ${telephonyManager.networkOperatorName}")
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            listDatas.add("PhoneNumber : ${telephonyManager.line1Number}")
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), 100)
        }

        checkNetwork()

        val wifiIntentFilter = IntentFilter()
        wifiIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        wifiIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        wifiIntentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION)
    }

    // 휴대전화의 여러 상태를 받기 위한 PhoneStateListener
    val phoneStateListener = object: PhoneStateListener() {
        // 서비스 상태 변경
        override fun onServiceStateChanged(serviceState: ServiceState?) {
            when(serviceState?.state) {
                ServiceState.STATE_EMERGENCY_ONLY -> listDatas.add("onServiceStateChanged STATE_EMERGENCY_ONLY")
                ServiceState.STATE_IN_SERVICE -> listDatas.add("onServiceStateChanged STATE_IN_SERVICE")
                ServiceState.STATE_OUT_OF_SERVICE -> listDatas.add("onServiceStateChanged STATE_OUT_OF_SERVICE")
                ServiceState.STATE_POWER_OFF -> listDatas.add("onServiceStateChanged STATE_POWER_OFF")
            }

            // 데이터 변경을 View에 반영하기
            listAdapter.notifyDataSetChanged()
        }

        // 통화 상태 변경
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            when(state) {
                TelephonyManager.CALL_STATE_IDLE -> listDatas.add("onCallStateChanged CALL_STATE_IDLE : $phoneNumber")
                TelephonyManager.CALL_STATE_RINGING -> listDatas.add("onCallStateChanged CALL_STATE_RINGING : $phoneNumber")
                TelephonyManager.CALL_STATE_OFFHOOK -> listDatas.add("onCallStateChanged CALL_STATE_OFFHOOK : $phoneNumber")
            }
            // 데이터 변경을 View에 반영하기
            listAdapter.notifyDataSetChanged()
        }
    }

    private fun checkNetwork() {
        // 네트워크 접속정보를 확인하기 위한 ConnectivityManager
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        // 네트워크 정보를 설정하는 NetworkRequest 객체
        val networkReq = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        val handler = object: Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when(msg.what) {
                    0 -> listAdapter.notifyDataSetChanged()
                }
            }
        }

        val thread = object: Thread() {
            override fun run() {
                val message = Message()
                message.what = 0
                handler.sendMessage(message)
            }
        }

        // API Level 21이상부터는 registerNetworkCallback을 사용하여 네트워크가 사용 가능하거나 불가능할 때마다 콜백 함수 호출
        // 매개변수로 NetworkRequest와 NetworkCallback을 구현하는 객체를 전달 
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            connectivityManager.registerNetworkCallback(networkReq, object : ConnectivityManager.NetworkCallback() {
                // 연결 가능
                override fun onAvailable(network: Network) {
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    if(capabilities != null) {
                        if(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                            listDatas.add("NetworkInfo : Online - WIFI")
                        } else if(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                            listDatas.add("NetworkInfo : Online - CELLULAR")
                        }
                    } else {
                        listDatas.add("NetworkInfo : offLine")
                    }
                    thread.start()
                }

                override fun onUnavailable() {
                    listDatas.add("NetworkInfo : offLine")
                    thread.start()
                }

                // 연결 불가능
                override fun onLost(network: Network) {
                    listDatas.add("NetworkInfo : offLine...")
                    thread.start()
                }
            })
        } else {
            // requestNetwork 함수에 매개변수로 networkRequest 객체와 NetworkCallback 객체 지정
            // 필요한 순간 한번 정보를 획득
            connectivityManager.requestNetwork(networkReq, object : ConnectivityManager.NetworkCallback() {
                // 연결 가능
                override fun onAvailable(network: Network) {
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    if(capabilities != null) {
                        if(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                            listDatas.add("NetworkInfo : Online - WIFI")
                        } else if(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                            listDatas.add("NetworkInfo : Online - CELLULAR")
                        }
                    } else {
                        listDatas.add("NetworkInfo : offLine")
                    }
                    listAdapter.notifyDataSetChanged()
                }

                override fun onUnavailable() {
                    listDatas.add("NetworkInfo : offLine...")
                    listAdapter.notifyDataSetChanged()
                }
            })

            println("21 미만")
            listAdapter.notifyDataSetChanged()
        }
    }
}