package com.example.gifapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.example.gifapp.domain.model.MainState
import com.example.gifapp.ui.composable.BackgroundAsset
import com.example.gifapp.ui.composable.Gif
import com.example.gifapp.ui.composable.SelectBackgroundAsset
import com.example.gifapp.ui.composable.theme.GifAppTheme
import com.example.gifapp.use_cases.RealCacheProvided
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var imageLoader: ImageLoader

    fun checkFilePermissions(): Boolean {
        val writePermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val readPermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
        return writePermission == PackageManager.PERMISSION_GRANTED &&
                readPermission == PackageManager.PERMISSION_GRANTED
    }

    private val externalStoragePermissionRequest = this.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            Timber.d("$it")
            if (!it.value) {
                Toast.makeText(
                    this,
                    "To enable this permission you will have to do so in system settings",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    private fun launchPermissionRequest() {
        externalStoragePermissionRequest.launch(
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        )
    }

    private val cropAssetLauncher: ActivityResultLauncher<CropImageContractOptions> =
        this.registerForActivityResult(CropImageContract()) { cropResult ->
            if (cropResult.isSuccessful) {
                cropResult.uriContent?.let { uri ->
                    when (val state = viewModel.state) {
                        is MainState.DisplaySelectBackgroundAsset,
                        is MainState.DisplayBackgroundAsset -> {
                            viewModel.updateState(
                                mainState = MainState.DisplayBackgroundAsset(
                                    backgroundAssetUri = uri
                                )
                            )
                        }
                        else -> throw Exception("Invalid state: $state")
                    }
                }
            } else {
                viewModel.showToast(message = "Something went wrong cropping the image")

            }

        }

    private val backgroundAssetPickerLauncher: ActivityResultLauncher<String> =
        this.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                cropAssetLauncher.launch(
                    options(
                        uri = uri
                    ) {
                        setGuidelines(CropImageView.Guidelines.ON)
                    }
                )
            } ?: viewModel.showToast(message = "Something went wrong selecting the image")
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO("This initialization will be removed after Hilt implementation")
        viewModel.setCacheProvider(RealCacheProvided(app = application))
        imageLoader = ImageLoader.Builder(application)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()

        viewModel.toastEventRelay.onEach { toastEvent ->
            if (toastEvent != null) {
                Toast.makeText(this, toastEvent.message, Toast.LENGTH_LONG).show()
            }
        }

        setContent {
            GifAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {

                    val state = viewModel.state
                    val view = LocalView.current

                    Column(modifier = Modifier.fillMaxSize()) {
                        when (state) {
                            MainState.Initial -> {
                                //TODO("Show loading UI)
                                viewModel.updateState(MainState.DisplaySelectBackgroundAsset)
                            }
                            is MainState.DisplaySelectBackgroundAsset -> SelectBackgroundAsset(
                                launchImagePicker = {
                                    backgroundAssetPickerLauncher.launch("image/*")
                                }
                            )
                            is MainState.DisplayBackgroundAsset -> BackgroundAsset(
                                backgroundAssetUri = state.backgroundAssetUri,
                                launchImagePicker = {
                                    backgroundAssetPickerLauncher.launch("image/*")
                                },
                                updateCapturingViewBounds = { rect ->
                                    viewModel.updateState(
                                        state.copy(capturingViewBounds = rect)
                                    )
                                },
                                startBitmapCaptureJob = {
                                    viewModel.runBitmapCaptureJob(
                                        contentResolver = contentResolver,
                                        view = view,
                                        window = window
                                    )
                                },
                                stopBitmapCaptureJob = viewModel::stopBitmapCaptureJob,
                                bitmapLoadingState = state.bitmapCaptureLoadingState,
                                loadingState = state.loadingState
                            )
                            is MainState.DisplayGif -> Gif(
                                imageLoader = imageLoader,
                                gifUri = state.resizedGifUri ?: state.gifUri,
                                discardGif = viewModel::deleteGif,
                                onSaveGif = {
                                    viewModel.saveGif(
                                        launchPermissionRequest = ::launchPermissionRequest,
                                        checkFilePermission = ::checkFilePermissions,
                                        contentResolver = contentResolver,
                                        context = this@MainActivity
                                    )
                                },
                                loadingState = state.loadingState,
                                adjustedBytes = state.adjustedByteSize,
                                updatedAdjustedBytes = {
                                    // TODO: delegate to viewModel
                                },
                                sizePercentage = state.sizePercentage,
                                updateSizePercentage = {
                                    // TODO: delegate to viewModel
                                },
                                currentGifSize = state.originalGifSize,
                                isResizedGif = state.resizedGifUri != null,
                                resizeGif = {
                                    // TODO: delegate to viewModel
                                },
                                resetResizing = {
                                    // TODO: delegate to viewModel
                                },
                            )

                        }
                    }
                }
            }
        }
    }
}
