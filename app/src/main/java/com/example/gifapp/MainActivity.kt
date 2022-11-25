package com.example.gifapp

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
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.example.gifapp.ui.composable.BackgroundAsset
import com.example.gifapp.ui.composable.SelectBackgroundAsset
import com.example.gifapp.ui.composable.theme.GifAppTheme
import kotlinx.coroutines.flow.onEach

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels<MainViewModel>()

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
                                }
                            )

                        }
                    }
                }
            }
        }
    }
}
