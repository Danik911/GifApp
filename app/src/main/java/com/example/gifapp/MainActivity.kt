package com.example.gifapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.example.gifapp.ui.composable.BackgroundAsset
import com.example.gifapp.ui.composable.SelectBackgroundAsset
import com.example.gifapp.ui.composable.theme.GifAppTheme

class MainActivity : ComponentActivity() {

    private val cropAssetLauncher: ActivityResultLauncher<CropImageContractOptions> =
        this.registerForActivityResult(CropImageContract()) { cropResult ->
            if (cropResult.isSuccessful) {
                cropResult.uriContent?.let { uri ->
                    when (val state = _state.value) {
                        is MainState.DisplaySelectBackgroundAsset,
                        is MainState.DisplayBackgroundAsset -> {
                            _state.value = MainState.DisplayBackgroundAsset(
                                backgroundAssetUri = uri
                            )
                        }
                        else -> throw Exception("Invalid state: $state")
                    }
                }
            } else {
                Toast.makeText(
                    this,
                    "Something went wrong cropping the image", Toast.LENGTH_SHORT
                ).show()
            }

        }

    private val backgroundAssetPickerLauncher: ActivityResultLauncher<String> =
        this.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->

            cropAssetLauncher.launch(
                options(
                    uri = uri
                ) {
                    setGuidelines(CropImageView.Guidelines.ON)
                }
            )
        }

    private val _state: MutableState<MainState> = mutableStateOf(MainState.Initial)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GifAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val state = _state.value
                    Column(modifier = Modifier.fillMaxSize()) {
                        when (state) {
                            MainState.Initial -> {
                                //TODO("Show loading UI)
                                _state.value = MainState.DisplaySelectBackgroundAsset(
                                    backgroundAssetPickerLauncher
                                )
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
