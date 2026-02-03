package com.ssafy.squiz.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.util.UUID

/**
 * BLE Beacon 스캐너 (멤버용)
 * 스터디 멤버가 스터디장의 BLE Beacon을 탐색하여 출석합니다.
 */
class BleScanner(private val context: Context) {

    companion object {
        private const val TAG = "BleScanner"

        // Squiz 앱 전용 UUID (BleAdvertiser와 동일)
        const val SQUIZ_SERVICE_UUID = "53515549-5a00-1000-8000-00805f9b34fb"

        // Manufacturer ID (Apple iBeacon 호환)
        private const val MANUFACTURER_ID = 0x004C

        // 스캔 타임아웃 (기본 30초)
        const val DEFAULT_SCAN_TIMEOUT_MS = 30_000L
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var scanner: BluetoothLeScanner? = null

    // 스캔 상태
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // 발견된 Beacon 정보
    data class SquizBeacon(
        val uuid: String,
        val major: Int,      // studyId
        val minor: Int,      // sessionId
        val rssi: Int,       // 신호 강도
        val distance: Double // 추정 거리 (미터)
    )

    // 발견된 Beacon
    private val _foundBeacon = MutableStateFlow<SquizBeacon?>(null)
    val foundBeacon: StateFlow<SquizBeacon?> = _foundBeacon.asStateFlow()

    // 스캔 결과 콜백
    private var onBeaconFound: ((SquizBeacon) -> Unit)? = null

    // 찾고 있는 스터디/세션
    private var targetStudyId: Long? = null
    private var targetSessionId: Long? = null

    init {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
        scanner = bluetoothAdapter?.bluetoothLeScanner
    }

    /**
     * BLE 스캔 시작 (특정 스터디/세션 찾기)
     * @param studyId 찾으려는 스터디 ID (null이면 모든 Squiz Beacon)
     * @param sessionId 찾으려는 세션 ID (null이면 해당 스터디의 모든 세션)
     * @param onFound Beacon 발견 시 콜백
     */
    fun startScanning(
        studyId: Long? = null,
        sessionId: Long? = null,
        onFound: ((SquizBeacon) -> Unit)? = null
    ): Boolean {
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

        // Scanner 재획득 (Bluetooth가 켜진 후에 획득해야 함)
        scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            Log.e(TAG, "BLE Scanner를 사용할 수 없습니다.")
            return false
        }

        if (_isScanning.value) {
            Log.w(TAG, "이미 스캔 중입니다.")
            return false
        }

        try {
            targetStudyId = studyId
            targetSessionId = sessionId
            onBeaconFound = onFound

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build()

            // iBeacon Manufacturer Data 필터
            val filterBuilder = ScanFilter.Builder()

            // Manufacturer Data로 필터 (iBeacon prefix만 매칭)
            // 실제로는 전체 데이터를 받아서 파싱
            val filters = listOf(filterBuilder.build())

            scanner?.startScan(filters, settings, scanCallback)
            _isScanning.value = true
            _foundBeacon.value = null

            Log.d(TAG, "BLE 스캔 시작: targetStudyId=$studyId, targetSessionId=$sessionId")
            return true

        } catch (e: SecurityException) {
            Log.e(TAG, "BLE 스캔 권한 없음", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "BLE 스캔 시작 실패", e)
            return false
        }
    }

    /**
     * BLE 스캔 중지
     */
    fun stopScanning() {
        try {
            scanner?.stopScan(scanCallback)
            _isScanning.value = false
            targetStudyId = null
            targetSessionId = null
            onBeaconFound = null
            Log.d(TAG, "BLE 스캔 중지")
        } catch (e: SecurityException) {
            Log.e(TAG, "BLE 스캔 중지 권한 없음", e)
        } catch (e: Exception) {
            Log.e(TAG, "BLE 스캔 중지 실패", e)
        }
    }

    /**
     * 스캔 콜백
     */
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { processResult(it) }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { processResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            val errorMessage = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "이미 스캔 중"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "앱 등록 실패"
                SCAN_FAILED_INTERNAL_ERROR -> "내부 오류"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "기능 미지원"
                else -> "알 수 없는 오류 ($errorCode)"
            }
            Log.e(TAG, "BLE 스캔 실패: $errorMessage")
        }
    }

    /**
     * 스캔 결과 처리
     */
    private fun processResult(result: ScanResult) {
        val scanRecord = result.scanRecord ?: return

        // Manufacturer Data에서 iBeacon 파싱
        val manufacturerData = scanRecord.getManufacturerSpecificData(MANUFACTURER_ID)
        if (manufacturerData == null || manufacturerData.size < 23) {
            return // iBeacon 형식이 아님
        }

        val beacon = parseIBeaconData(manufacturerData, result.rssi)
        if (beacon == null || beacon.uuid != SQUIZ_SERVICE_UUID) {
            return // Squiz Beacon이 아님
        }

        // 타겟 스터디/세션 확인
        val matchesStudy = targetStudyId == null || beacon.major.toLong() == targetStudyId
        val matchesSession = targetSessionId == null || beacon.minor.toLong() == targetSessionId

        if (matchesStudy && matchesSession) {
            Log.d(TAG, "Squiz Beacon 발견: studyId=${beacon.major}, sessionId=${beacon.minor}, rssi=${beacon.rssi}, distance=${String.format("%.2f", beacon.distance)}m")

            _foundBeacon.value = beacon
            onBeaconFound?.invoke(beacon)
        }
    }

    /**
     * iBeacon 데이터 파싱
     */
    private fun parseIBeaconData(data: ByteArray, rssi: Int): SquizBeacon? {
        if (data.size < 23) return null

        // iBeacon prefix 확인
        if (data[0] != 0x02.toByte() || data[1] != 0x15.toByte()) {
            return null
        }

        val buffer = ByteBuffer.wrap(data)
        buffer.position(2) // prefix 스킵

        // UUID (16 bytes)
        val msb = buffer.getLong()
        val lsb = buffer.getLong()
        val uuid = UUID(msb, lsb).toString()

        // Major (2 bytes)
        val major = buffer.getShort().toInt() and 0xFFFF

        // Minor (2 bytes)
        val minor = buffer.getShort().toInt() and 0xFFFF

        // Tx Power (1 byte)
        val txPower = buffer.get().toInt()

        // 거리 계산 (Log-distance path loss model)
        val distance = calculateDistance(rssi, txPower)

        return SquizBeacon(
            uuid = uuid,
            major = major,
            minor = minor,
            rssi = rssi,
            distance = distance
        )
    }

    /**
     * RSSI 기반 거리 추정 (미터)
     */
    private fun calculateDistance(rssi: Int, txPower: Int): Double {
        if (rssi == 0) return -1.0

        val ratio = rssi.toDouble() / txPower
        return if (ratio < 1.0) {
            Math.pow(ratio, 10.0)
        } else {
            // Log-distance path loss model
            val accuracy = 0.89976 * Math.pow(ratio, 7.7095) + 0.111
            accuracy
        }
    }

    /**
     * BLE 스캔 지원 여부 확인
     */
    fun isSupported(): Boolean {
        return bluetoothAdapter?.isEnabled == true && scanner != null
    }

    /**
     * Bluetooth 활성화 여부 확인
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * 발견된 Beacon 초기화
     */
    fun clearFoundBeacon() {
        _foundBeacon.value = null
    }
}
