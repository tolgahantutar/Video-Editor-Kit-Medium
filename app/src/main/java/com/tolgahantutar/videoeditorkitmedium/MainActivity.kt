package com.tolgahantutar.videoeditorkitmedium

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.huawei.hms.videoeditor.sdk.*
import com.huawei.hms.videoeditor.sdk.ai.HVEAIProcessCallback
import com.huawei.hms.videoeditor.sdk.asset.HVEVisibleAsset
import com.huawei.hms.videoeditor.sdk.bean.HVEVideoProperty
import com.huawei.hms.videoeditor.sdk.lane.HVEVideoLane
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var editor: HuaweiVideoEditor
    private lateinit var timeLine: HVETimeLine
    private lateinit var videoLane: HVEVideoLane
    private lateinit var visibleAsset: HVEVisibleAsset
    private lateinit var videoPreviewContainer: ConstraintLayout
    private lateinit var chooseImageButton: Button
    private lateinit var chooseVideoButton: Button
    private lateinit var convertVideoButton: Button
    private lateinit var playOrPauseButton: ImageButton
    private lateinit var exportButton: Button
    private lateinit var exportConfirmButton: Button
    private lateinit var currentTimeTexView: TextView
    private lateinit var totalTimeTexView: TextView
    private lateinit var fileNameEditText: EditText
    private lateinit var progressTextView: TextView
    private lateinit var frameRateRadioGroup: RadioGroup
    private lateinit var resolutionRadioGroup: RadioGroup
    private lateinit var seekBar: SeekBar
    private var frameRate = 30
    private lateinit var hveVideoProperty: HVEVideoProperty
    private lateinit var exportVideoCallback: HVEExportManager.HVEExportVideoCallback
    private lateinit var playCallback: HuaweiVideoEditor.PlayCallback
    private var playStatus = false
    private var lastPlayTime = 0L
    private var isImage = false
    val sdf = SimpleDateFormat("mm:ss")
    private lateinit var dialog: Dialog
    private lateinit var exportSettingsDialog: Dialog
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher:  ActivityResultLauncher<Array<String>>
    private var apiKey = "Your API KEY"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        initConfigurations()
        initEnvironment()
        initContracts()


        chooseImageButton.setOnClickListener {
            isImage = true
            checkPermissions()
        }
        chooseVideoButton.setOnClickListener {
            isImage = false
            checkPermissions()
        }
        convertVideoButton.setOnClickListener {
            if (videoLane.assets.isNullOrEmpty()) {
                Toast.makeText(this, "Please select a media before converting operation", Toast.LENGTH_SHORT).show()
            }else {
                dialog.show()
                convertVideo()
            }
        }

        playOrPauseButton.setOnClickListener {
            playVideo()
        }

        exportButton.setOnClickListener {
            exportSettingsDialog.show()
        }

        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (p2) {
                    val formatted = sdf.format(Date(p1.toLong()))
                    currentTimeTexView.text = formatted
                    editor.seekTimeLine(p1.toLong())
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                editor.pauseTimeLine()
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                if (!playStatus) {
                    lastPlayTime = p0!!.progress.toLong()
                }else {
                    editor.playTimeLine(p0!!.progress.toLong(), timeLine.endTime)
                }
            }

        })

        exportConfirmButton.setOnClickListener {
            dialog.show()
            when (frameRateRadioGroup.checkedRadioButtonId) {
                R.id.twenty_four_radioButton -> {
                    frameRate = 24
                }
                R.id.twenty_five_radioButton -> {
                    frameRate = 25
                }
                R.id.thirty_radioButton -> {
                    frameRate = 30
                }
                R.id.fifty_radioButton -> {
                    frameRate = 50
                }
                R.id.sixty_radioButton -> {
                    frameRate = 60
                }
            }
            when(resolutionRadioGroup.checkedRadioButtonId) {
                R.id.seven_two_zero_radioButton -> {
                    hveVideoProperty = HVEVideoProperty(1280, 720)
                }
                R.id.one_zero_eight_zero_radioButton -> {
                    hveVideoProperty = HVEVideoProperty(1920, 1080)
                }
                R.id.twoK_radioButton -> {
                    hveVideoProperty = HVEVideoProperty(2560, 1600)
                }
                R.id.fourK_radioButton -> {
                    hveVideoProperty = HVEVideoProperty(3849, 2160)
                }
            }

            HVEExportManager().exportVideo(
                this@MainActivity.editor,
                exportVideoCallback,
                hveVideoProperty,
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + File.separator
            + "VideoEditorKitMedium" + File.separator + fileNameEditText.text.toString() + ".mp4"
            )
        }


        exportVideoCallback = object: HVEExportManager.HVEExportVideoCallback {
            override fun onCompileProgress(p0: Long, p1: Long) {
                runOnUiThread {
                    if (p0 != 0L && p1 != 0L) {
                        progressTextView.text = "Exporting %" + (100/(p1/p0)).toString()
                    }
                }
            }
            override fun onCompileFinished(p0: String?, p1: Uri?) {
                runOnUiThread {
                    exportSettingsDialog.dismiss()
                    dialog.dismiss()
                    Toast.makeText(this@MainActivity, "Exported Successfully", Toast.LENGTH_SHORT).show()
                    progressTextView.text = "0"
                }
            }
            override fun onCompileFailed(p0: Int, p1: String?) {
                runOnUiThread {
                    exportSettingsDialog.dismiss()
                    dialog.dismiss()
                    Toast.makeText(this@MainActivity, "$p0 Export Failed Due to : ${p1.toString()}", Toast.LENGTH_SHORT).show()
                    progressTextView.text = "0"
                }
            }
        }

        playCallback = object: HuaweiVideoEditor.PlayCallback {
            override fun onPlayProgress(currentTime: Long) {
                val formatted = sdf.format(Date(currentTime))
                lastPlayTime = currentTime
                runOnUiThread {
                    currentTimeTexView.text = formatted
                    seekBar.progress = currentTime.toInt()
                }
            }
            override fun onPlayStopped() {
                Log.i("MYTAG", "Play Stopped")
            }
            override fun onPlayFinished() {
                playStatus = false
                playOrPauseButton.setImageResource(R.drawable.ic_baseline_play_arrow_24)
                lastPlayTime = 0L
            }
            override fun onPlayFailed() {
                Log.i("MYTAG", "Play Failed")
            }
        }
    }

    private fun playVideo() {
        playStatus = if (!playStatus) {
            editor.playTimeLine(lastPlayTime, timeLine.endTime)
            editor.setPlayCallback(playCallback)
            playOrPauseButton.setImageResource(R.drawable.ic_baseline_pause_24)
            true
        }else {
            editor.pauseTimeLine()
            playOrPauseButton.setImageResource(R.drawable.ic_baseline_play_arrow_24)
            false
        }
    }

    private fun convertVideo() {
        visibleAsset.addColorAIEffect(object : HVEAIProcessCallback {
            override fun onProgress(progress: Int) {
                runOnUiThread {
                    progressTextView.text = "Converting %$progress"
                }
            }
            override fun onSuccess() {
                print("success")
                runOnUiThread {
                    editor.setDisplay(videoPreviewContainer)
                    dialog.dismiss()
                }
            }
            override fun onError(errorCode: Int, errorMessage: String) {
                print(errorMessage)
                dialog.dismiss()
            }
        })
    }

    private fun openGallery() {
        val intent = Intent()
        if (isImage) {
            intent.type = "image/*"
        }else {
            intent.type = "video/*"
        }
        intent.action = Intent.ACTION_GET_CONTENT
        resultLauncher.launch(intent)
    }

    private fun initEnvironment() {
        try {
            editor.initEnvironment()
        } catch (error: LicenseException) {
            Log.e("MYTAG", "initEnvironment failed: " + error.errorMsg)
            finish()
            return
        }
    }

    private fun initViews() {
        videoPreviewContainer = findViewById(R.id.video_content_layout)
        chooseImageButton = findViewById(R.id.choose_image_button)
        chooseVideoButton = findViewById(R.id.choose_video_button)
        convertVideoButton = findViewById(R.id.convert_ai_color_button)
        currentTimeTexView = findViewById(R.id.current_time_textView)
        totalTimeTexView = findViewById(R.id.total_time_TextView)
        playOrPauseButton = findViewById(R.id.play_or_pause_button)
        exportButton = findViewById(R.id.export_converted_video)
        seekBar = findViewById(R.id.seekbar)

        dialog = Dialog(this@MainActivity)
        val inflate = LayoutInflater.from(this@MainActivity).inflate(R.layout.progress, null)
        dialog.setContentView(inflate)
        dialog.setCancelable(false)
        progressTextView = inflate.findViewById(R.id.progress_number_textView)

        exportSettingsDialog = Dialog(this)
        val inflateExportSettingsDialog = LayoutInflater.from(this).inflate(R.layout.export_layout, null)
        exportSettingsDialog.setContentView(inflateExportSettingsDialog)
        exportConfirmButton = inflateExportSettingsDialog.findViewById(R.id.confirm_export_button)
        fileNameEditText = inflateExportSettingsDialog.findViewById(R.id.file_name_editText)
        frameRateRadioGroup = inflateExportSettingsDialog.findViewById(R.id.frame_rate_radioGroup)
        resolutionRadioGroup = inflateExportSettingsDialog.findViewById(R.id.resolution_radioGroup)
    }

    private fun initConfigurations() {
        MediaApplication.getInstance().setApiKey(apiKey)
        MediaApplication.getInstance().setLicenseId(UUID.randomUUID().toString())

        editor = HuaweiVideoEditor.create(applicationContext)
        timeLine = editor.timeLine
        videoLane = timeLine.appendVideoLane()
    }
    private fun initContracts() {
        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->
            if (result.resultCode == Activity.RESULT_OK) {

                //1.
                val filePath = FileUtils(this).getPath(result.data!!.data)
                videoLane.removeAllAssets()
                //2.
                visibleAsset = if (isImage) {
                    videoLane.appendImageAsset(filePath)
                }else {
                    videoLane.appendVideoAsset(filePath)
                }
                //3.
                editor.setDisplay(videoPreviewContainer)
                //4.
                seekBar.max = timeLine.endTime.toInt()
                //5.
                val sdf = SimpleDateFormat("mm:ss")
                val formatted = sdf.format(Date(timeLine.endTime))
                totalTimeTexView.text = formatted
            }
        }

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){permissions ->
                if (permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true &&
                    permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true) {
                openGallery()
                }else {
                    Toast.makeText(this, "Please allow app to access external storage", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkPermissions(){
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        openGallery()
            }
            else -> {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        }
    }
}