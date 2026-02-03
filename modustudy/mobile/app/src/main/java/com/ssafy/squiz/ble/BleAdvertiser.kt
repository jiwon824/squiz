package com.ssafy.squiz.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.util.UUID

/**
 * BLE Beacon 광고자 (스터디장용)
 * 스터디장이 출석 세션을 시작하면 BLE Beacon을 광고합니다.
 */
class BleAdvertiser(private val context: Context) {

    companion object {
        private const val TAG = "BleAdvertiser"

        // Squiz 앱 전용 UUID (SQUIZ를 아스키 코드로 변환한 형태 기반)
        const val SQUIZ_SERVICE_UUID = "53515549-5a00-1000-8000-00805f9b34fb"

        // Manufacturer ID (테스트용 - 실제로는 Bluetooth SIG에 등록 필요)
        private const val MANUFACTURER_ID = 0x004C // Apple iBeacon 호환
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var advertiser: BluetoothLeAdvertiser? = null

    // 광고 상태
    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    // 현재 광고 중인 세션 정보
    private var currentStudyId: Long? = null
    private var currentSessionId: Long? = null

    init {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
        advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
    }

    /**
     * BLE 광고 시작 (스터디장)
     * @param studyId 스터디 ID (Major 값으로 사용)
     * @param sessionId 세션 ID (Minor 값으로 사용)
     */
    fun startAdvertising(studyId: Long, sessionId: Long): Boolean {
        // Bluetooth 어댑터 재확인 (Bluetooth가 나중에 켜졌을 수 있음)
        if (bluetoothAdapter == null) {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bluetoothAdapter = bluetoothManager?.adapter
        }

        // Bluetooth가 꺼져있는지 확인
        if (bluetoothAdapter?.isEnabled != true) {
            Log.e(TAG, "Bluetooth가 꺼져있습니다.")
            return false
        }

        // Advertiser 재획득 (Bluetooth가 켜진 후에 획득해야 함)
        advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        if (advertiser == null) {
            Log.e(TAG, "BLE Advertiser를 사용할 수 없습니다. 기기가 BLE 광고를 지원하지 않습니다.")
            return false
        }

        if (_isAdvertising.value) {
            Log.w(TAG, "이미 광고 중입니다. 먼저 중지하세요.")
            return false
        }

        try {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .setTimeout(0) // 무제한 광고
                .build()

            // iBeacon 형식의 Manufacturer Data 생성
            val manufacturerData = createIBeaconData(studyId.toInt(), sessionId.toInt())

            val advertiseData = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addManufacturerData(MANUFACTURER_ID, manufacturerData)
                .build()

            advertiser?.startAdvertising(settings, advertiseData, advertiseCallback)

            currentStudyId = studyId
            currentSessionId = sessionId

            Log.d(TAG, "BLE 광고 시작 요청: studyId=$studyId, sessionId=$sessionId")
            return true

        } catch (e: SecurityException) {
            Log.e(TAG, "BLE 광고 권한 없음", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "BLE 광고 시작 실패", e)
            return false
        }
    }

    /**
     * BLE 광고 중지
     */
    fun stopAdvertising() {
        try {
            advertiser?.stopAdvertising(advertiseCallback)
            _isAdvertising.value = false
            currentStudyId = null
            currentSessionId = null
            Log.d(TAG, "BLE 광고 중지")
        } catch (e: SecurityException) {
            Log.e(TAG, "BLE 광고 중지 권한 없음", e)
        } catch (e: Exception) {
            Log.e(TAG, "BLE 광고 중지 실패", e)
        }
    }

    /**
     * iBeacon 형식의 Manufacturer Data 생성
     * 형식: [UUID (16 bytes)][Major (2 bytes)][Minor (2 bytes)][Tx Power (1 byte)]
     */
    private fun createIBeaconData(major: Int, minor: Int): ByteArray {
        val uuid = UUID.fromString(SQUIZ_SERVICE_UUID)

        val buffer = ByteBuffer.allocate(23)

        // iBeacon prefix (고정값)
        buffer.put(0x02.toByte()) // iBeacon type
        buffer.put(0x15.toByte()) // iBeacon length (21 bytes)

        // UUID (16 bytes)
        buffer.putLong(uuid.mostSignificantBits)
        buffer.putLong(uuid.leastSignificantBits)

        // Major (2 bytes) - Big Endian
        buffer.putShort(major.toShort())

        // Minor (2 bytes) - Big Endian
        buffer.putShort(minor.toShort())

        // Tx Power (측정된 1m 거리에서의 RSSI)
        buffer.put((-59).toByte())

        return buffer.array()
    }

    /**
     * 광고 콜백
     */
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            _isAdvertising.value = true
            Log.d(TAG, "BLE 광고 시작 성공: studyId=$currentStudyId, sessionId=$currentSessionId")
        }

        override fun onStartFailure(errorCode: Int) {
            _isAdvertising.value = false
            val errorMessage = when (errorCode) {
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "광고 데이터가 너무 큼"
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "광고자가 너무 많음"
                ADVERTISE_FAILED_ALREADY_STARTED -> "이미 광고 중"
                ADVERTISE_FAILED_INTERNAL_ERROR -> "내부 오류"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "기능 미지원"
                else -> "알 수 없는 오류 ($errorCode)"
            }
            Log.e(TAG, "BLE 광고 시작 실패: $errorMessage")
        }
    }

    /**
     * BLE 광고 지원 여부 확인
     */
    fun isSupported(): Boolean {
        return bluetoothAdapter?.isEnabled == true && advertiser != null
    }

    /**
     * Bluetooth 활성화 여부 확인
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * 현재 광고 중인 스터디 ID
     */
    fun getCurrentStudyId(): Long? = currentStudyId

    /**
     * 현재 광고 중인 세션 ID
     */
    fun getCurrentSessionId(): Long? = currentSessionId
}
