package com.example.dmsapplication.ui.homeView

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.dmsapplication.R
import com.example.dmsapplication.ml.analyzer.DmsAnalyzer
import com.example.dmsapplication.ml.math.CalibrationManager
import com.example.dmsapplication.ml.math.HeadPoseEstimator
import com.example.dmsapplication.ui.homeView.helper.AlarmHelper
import com.example.dmsapplication.ui.homeView.helper.CalibrationDialog
import com.example.dmsapplication.ui.homeView.helper.LocationHelper
import com.example.dmsapplication.ui.homeView.helper.OverlayView
import com.example.dmsapplication.utils.CrashDetector
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class HomeFragment : Fragment(), CalibrationDialog.CalibrationListener {
    private val viewModel: HomeViewModel by activityViewModels { HomeViewModelFactory(requireActivity().application) }
    private var faceLandmarker: FaceLandmarker? = null
    private var dmsAnalyzer: DmsAnalyzer? = null
    private var cameraExecutor = Executors.newSingleThreadExecutor()
    private lateinit var alarmHelper: AlarmHelper
    private lateinit var locationHelper: LocationHelper
    private lateinit var calibrationManager: CalibrationManager
    private lateinit var crashDetector: CrashDetector
    private lateinit var overlayView: OverlayView
    private lateinit var cameraBorder: CardView
    private lateinit var viewFinder: PreviewView
    private lateinit var tvStatus: TextView
    private lateinit var tvSpeedNumber: TextView
    private lateinit var pbSpeed: android.widget.ProgressBar
    private lateinit var tvDrowsyCount: TextView
    private lateinit var tvHeadCount: TextView
    private lateinit var tvYawnCount: TextView
    private lateinit var seekEyeThreshold: SeekBar
    private lateinit var tvEyeThresholdValue: TextView
    private lateinit var switchGpsHome: SwitchCompat
    @Volatile private var latestHeadAngles: HeadPoseEstimator.HeadAngles? = null
    private var isVectorEnabled = false
    private val handler = Handler(Looper.getMainLooper())
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        if (cameraExecutor.isShutdown) {
            cameraExecutor = Executors.newSingleThreadExecutor()
        }
        calibrationManager = CalibrationManager(requireContext())
        initViews(view)
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            v.setPadding(0, 0, 0, 0)
            insets
        }
        alarmHelper    = AlarmHelper(requireContext())
        locationHelper = LocationHelper(requireContext()) { speed, status ->
            viewModel.updateLocation(speed, status)
        }
        crashDetector = CrashDetector(requireContext()) {
            Toast.makeText(requireContext(), "PHÁT HIỆN VA CHẠM! Đang gửi cảnh báo...", Toast.LENGTH_LONG).show()
            val lat = locationHelper.currentLocation?.latitude ?: 0.0
            val lon = locationHelper.currentLocation?.longitude ?: 0.0
            viewModel.triggerCrashAlert(lat, lon)
        }
        setupFaceLandmarker()
        requestPermissionsLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        observeViewModel()
        if (!calibrationManager.isCalibrated) {
            view.post { showCalibrationDialog() }
        } else {
            viewModel.setCalibrated(true)
        }
        return view
    }
    private fun initViews(view: View) {
        tvStatus      = view.findViewById(R.id.tvStatus)
        tvDrowsyCount = view.findViewById(R.id.tvDrowsyCount)
        tvHeadCount   = view.findViewById(R.id.tvHeadCount)
        tvYawnCount   = view.findViewById(R.id.tvYawnCount)
        viewFinder    = view.findViewById(R.id.viewFinder)
        overlayView   = view.findViewById(R.id.overlayView)
        cameraBorder  = view.findViewById(R.id.cameraBorder)
        tvSpeedNumber = view.findViewById(R.id.tvSpeedNumber)
        pbSpeed       = view.findViewById(R.id.pbSpeed)
        seekEyeThreshold = view.findViewById(R.id.seekEyeThreshold)
        tvEyeThresholdValue = view.findViewById(R.id.tvEyeThresholdValue)
        cameraBorder.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.success))
        viewFinder.scaleType = PreviewView.ScaleType.FILL_CENTER
        view.findViewById<SwitchCompat>(R.id.switchSunglasses)
            .setOnCheckedChangeListener { _, isChecked ->
                viewModel.setSunglassesMode(isChecked)
            }
        view.findViewById<SwitchCompat>(R.id.switchYawn)
            .setOnCheckedChangeListener { _, isChecked ->
                viewModel.setYawnMode(isChecked)
            }
        view.findViewById<SwitchCompat>(R.id.switchVectorHome)
            .setOnCheckedChangeListener { _, isChecked ->
                isVectorEnabled = isChecked
                if (!isChecked) overlayView.setResults(null)
            }
        switchGpsHome = view.findViewById<SwitchCompat>(R.id.switchGpsHome).apply {
            isChecked = viewModel.isGpsEnabled.value
            setOnCheckedChangeListener { _, isChecked ->
                viewModel.setGpsEnabled(isChecked)
                if (isChecked && !hasLocationPermission()) {
                    requestPermissionsLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        }
        view.findViewById<View>(R.id.btnRecalibrate).setOnClickListener {
            showCalibrationDialog()
        }
        seekEyeThreshold.max = ((DmsAnalyzer.MAX_EAR_THRESHOLD - DmsAnalyzer.MIN_EAR_THRESHOLD) * 100).roundToInt()
        seekEyeThreshold.progress = thresholdToProgress(viewModel.earThreshold.value)
        updateEyeThresholdText(viewModel.earThreshold.value)
        seekEyeThreshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    viewModel.setEarThreshold(progressToThreshold(progress))
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        view.findViewById<SwitchCompat>(R.id.switchCameraPreview)
            .setOnCheckedChangeListener { _, isChecked ->
                viewModel.setCameraPreviewEnabled(isChecked)
            }
        view.findViewById<SwitchCompat>(R.id.switchVectorHome)
            .setOnCheckedChangeListener { _, isChecked ->
                isVectorEnabled = isChecked
                if (isChecked && viewModel.isCameraPreviewEnabled.value) {
                    overlayView.visibility = View.VISIBLE
                } else {
                    overlayView.visibility = View.GONE
                    overlayView.setResults(null)
                }
            }
    }
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isDrowsy.collect { updateBorderAndAlarm() }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isHeadDistracted.collect { updateBorderAndAlarm() }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.drowsyCount.collect { count ->
                tvDrowsyCount.text = "$count"
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.headDistractedCount.collect { count ->
                tvHeadCount.text = "$count"
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.yawnCount.collect { count ->
                tvYawnCount.text = "$count"
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.suggestRest.collect { shouldRest ->
                if (shouldRest) {
                    Toast.makeText(requireContext(), "Hãy dừng xe nghỉ ngơi!", Toast.LENGTH_LONG).show()
                    alarmHelper.playRestAlert()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.speedKmh.collect { speedVal ->
                val displaySpeed = speedVal.toInt()
                tvSpeedNumber.text = displaySpeed.toString()
                val progressVal = displaySpeed.coerceIn(0, 120)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    pbSpeed.setProgress(progressVal, true)
                } else {
                    pbSpeed.progress = progressVal
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.locationStatus.collect { status -> tvStatus.text = status }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isGpsEnabled.collect { enabled ->
                if (::switchGpsHome.isInitialized && switchGpsHome.isChecked != enabled) {
                    switchGpsHome.isChecked = enabled
                }
                if (enabled) startLocationTrackingIfAllowed()
                else locationHelper.stopTracking()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isMonitoringEnabled.collect { monitoring ->
                if (!monitoring && !viewModel.isDrowsy.value && !viewModel.isHeadDistracted.value) {
                    cameraBorder.setCardBackgroundColor(ContextCompat.getColor(requireContext(), if (monitoring) R.color.success else R.color.text_muted))
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isSunglassesMode.collect { isSunglasses ->
                dmsAnalyzer?.isSunglassesMode = isSunglasses
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isYawnMode.collect { isYawn ->
                dmsAnalyzer?.isYawnMode = isYawn
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.earThreshold.collect { threshold ->
                dmsAnalyzer?.earThreshold = threshold
                if (::seekEyeThreshold.isInitialized) {
                    val progress = thresholdToProgress(threshold)
                    if (seekEyeThreshold.progress != progress) {
                        seekEyeThreshold.progress = progress
                    }
                    updateEyeThresholdText(threshold)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isCameraPreviewEnabled.collect { isEnabled ->
                if (isEnabled) {
                    viewFinder.visibility = View.VISIBLE
                    overlayView.visibility = if (isVectorEnabled) View.VISIBLE else View.GONE
                    updateBorderAndAlarm()
                } else {
                    viewFinder.visibility = View.INVISIBLE
                    overlayView.visibility = View.GONE
                    cameraBorder.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.camera_inactive))
                }
            }
        }
    }
    private fun updateBorderAndAlarm() {
        val isAlert = viewModel.isDrowsy.value || viewModel.isHeadDistracted.value
        cameraBorder.setCardBackgroundColor(ContextCompat.getColor(requireContext(), if (isAlert) R.color.danger else R.color.success))
        if (isAlert) alarmHelper.playAlert() else alarmHelper.stopAlert()
    }
    private fun thresholdToProgress(threshold: Float): Int {
        return ((threshold - DmsAnalyzer.MIN_EAR_THRESHOLD) * 100).roundToInt()
            .coerceIn(0, seekEyeThreshold.max)
    }
    private fun progressToThreshold(progress: Int): Float {
        return DmsAnalyzer.MIN_EAR_THRESHOLD + (progress / 100f)
    }
    private fun updateEyeThresholdText(threshold: Float) {
        tvEyeThresholdValue.text = String.format(Locale.US, "%.2f", threshold)
    }
    private fun showCalibrationDialog() {
        val dialog = CalibrationDialog()
        dialog.setCalibrationListener(this)
        dialog.show(childFragmentManager, "calibration")
    }
    override fun onCalibrationComplete() {
        val angles = latestHeadAngles
        if (angles != null) {
            calibrationManager.saveBaseline(angles)
            viewModel.setCalibrated(true)
            viewModel.resetStats()
            dmsAnalyzer?.resetAll()
            Toast.makeText(requireContext(), "Đã lưu hướng lái chuẩn!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Không phát hiện khuôn mặt. Hãy thử lại!", Toast.LENGTH_LONG).show()
        }
    }
    override fun onCalibrationCancelled() { }
    private fun setupFaceLandmarker() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("face_landmarker.task")
                .build()
            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.VIDEO)
                .setNumFaces(1)
                .setMinFaceDetectionConfidence(0.5f)
                .setMinFacePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build()
            faceLandmarker = FaceLandmarker.createFromOptions(requireContext(), options)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Lỗi AI: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.CAMERA] == true) startCamera()
            val locationGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                    hasLocationPermission()
            if (locationGranted) {
                if (!viewModel.isGpsEnabled.value) viewModel.setGpsEnabled(true)
                startLocationTrackingIfAllowed()
            } else if (!locationGranted && viewModel.isGpsEnabled.value) {
                viewModel.setGpsEnabled(false)
                Toast.makeText(requireContext(), "Chưa có quyền vị trí để giám sát bằng GPS", Toast.LENGTH_SHORT).show()
            }
        }
    private fun startLocationTrackingIfAllowed() {
        if (hasLocationPermission()) {
            locationHelper.startTracking()
        }
    }
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }
    private fun startCamera() {
        val fl = faceLandmarker ?: return
        val analyzer = DmsAnalyzer(
            faceLandmarker     = fl,
            calibrationManager = calibrationManager,
            onResults = { isDrowsy, isHeadDistracted, isYawning, result ->
                result?.faceLandmarks()?.firstOrNull()?.let { lm ->
                    latestHeadAngles = HeadPoseEstimator.estimate(lm)
                }
                activity?.runOnUiThread {
                    val wasAlert = viewModel.isDrowsy.value || viewModel.isHeadDistracted.value
                    viewModel.onDmsResult(isDrowsy, isHeadDistracted, isYawning)
                    val isAlert = viewModel.isDrowsy.value || viewModel.isHeadDistracted.value
                    if (!wasAlert && isAlert) {
                        captureAndSave()
                    }
                    if (isVectorEnabled) {
                        overlayView.setResults(result)
                    }
                }
            }
        )
        analyzer.isSunglassesMode = viewModel.isSunglassesMode.value
        analyzer.isYawnMode = viewModel.isYawnMode.value
        analyzer.earThreshold = viewModel.earThreshold.value
        dmsAnalyzer = analyzer
        crashDetector.startListening()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(viewFinder.display.rotation)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { it.setAnalyzer(cameraExecutor, analyzer) }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                imageAnalysis
            )
        }, ContextCompat.getMainExecutor(requireContext()))
    }
    private fun captureAndSave() {
        val bitmap = viewFinder.bitmap ?: return
        viewModel.saveViolationRecord(bitmap)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        cameraExecutor.shutdown()
        faceLandmarker?.close()
        alarmHelper.release()
        locationHelper.stopTracking()
        crashDetector.stopListening()
    }
}
