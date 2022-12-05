package com.example.gifapp.ui.composable

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.rememberAsyncImagePainter
import com.example.gifapp.R
import com.example.gifapp.domain.model.DataState.Loading.LoadingState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BackgroundAsset(
    backgroundAssetUri: Uri,
    launchImagePicker: () -> Unit,
    updateCapturingViewBounds: (Rect) -> Unit,
    startBitmapCaptureJob: () -> Unit,
    bitmapLoadingState: LoadingState,
    stopBitmapCaptureJob: () -> Unit,
    loadingState: LoadingState
) {
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (topBar, assetContainer, bottomContainer) = createRefs()

        //Top bar
        // topBarHeight = (default app bar height) + (button padding)
        val topBarHeight = remember {
            56 + 16
        }

        RecordActionBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(topBarHeight.dp)
                .constrainAs(topBar) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .zIndex(2f)
                .background(Color.White),
            bitmapLoadingState = bitmapLoadingState,
            startBitmapCaptureJob = startBitmapCaptureJob,
            stopBitmapCaptureJob = stopBitmapCaptureJob

        )

        //Gif capture area
        val configuration = LocalConfiguration.current
        val assetContainerHeight = remember {
            (configuration.screenHeightDp * 0.6).toInt()
        }
        RenderBackground(
            modifier = Modifier
                .constrainAs(assetContainer) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(topBar.bottom)
                },
            assetContainerHeightDp = assetContainerHeight,
            backgroundAssetUri = backgroundAssetUri,
            updateCapturingViewBounds = updateCapturingViewBounds
        )
        StandardLoadingUI(loadingState = loadingState)

        // Bottom container
        val bottomContainerHeight = remember {
            configuration.screenHeightDp - assetContainerHeight - topBarHeight
        }
        BackgroundAssetFooter(
            modifier = Modifier
                .fillMaxWidth()
                .height(bottomContainerHeight.dp)
                .constrainAs(bottomContainer) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(assetContainer.bottom)
                    bottom.linkTo(parent.bottom)
                }
                .zIndex(2f)
                .background(Color.White),
            isRecording = bitmapLoadingState is LoadingState.Active,
            launchImagePicker = launchImagePicker
        )
    }

}

@Composable
fun RenderBackground(
    modifier: Modifier,
    backgroundAssetUri: Uri,
    assetContainerHeightDp: Int,
    updateCapturingViewBounds: (Rect) -> Unit
) {
    Box(modifier = modifier.wrapContentSize())
    {

        val painter = rememberAsyncImagePainter(model = backgroundAssetUri)

        Image(
            modifier = Modifier
                .fillMaxWidth()
                .height(assetContainerHeightDp.dp)
                .onGloballyPositioned {
                    updateCapturingViewBounds(it.boundsInRoot())
                },
            painter = painter,
            contentDescription = "Gif background",
            contentScale = ContentScale.Crop
        )
        RenderAsset(assetContainerHeightDp = assetContainerHeightDp)
    }
}

@Composable
fun RenderAsset(
    assetContainerHeightDp: Int
) {

    var offset by remember {
        mutableStateOf(Offset.Zero)
    }
    var zoom by remember {
        mutableStateOf(1f)
    }
    var angle by remember {
        mutableStateOf(0f)
    }

    val asset = painterResource(id = R.drawable.sunglasses_default)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(assetContainerHeightDp.dp)
    ) {
        Image(
            modifier = Modifier
                .graphicsLayer {
                    val rotateOffset = offset.rotateBy(angle)
                    translationX = -rotateOffset.x
                    translationY = -rotateOffset.y
                    scaleX = zoom
                    scaleY = zoom
                    rotationZ = angle
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .pointerInput(Unit) {
                    detectTransformGestures(
                        onGesture = { centroid, pan, gestureZoom, gestureRotation ->
                            val oldScale = zoom
                            val newScale = zoom * gestureZoom
                            angle += gestureRotation
                            zoom = newScale

                            offset = (offset - centroid * oldScale).rotateBy(-gestureRotation) +
                                    (centroid * newScale - pan * oldScale)
                        }
                    )
                }
                .size(200.dp, 200.dp),
            painter = asset,
            contentDescription = "Overlay image"
        )

    }
}

fun Offset.rotateBy(angle: Float): Offset {
    val angleInRadians = angle * PI / 180
    return Offset(
        (x * cos(angleInRadians) - y * sin(angleInRadians)).toFloat(),
        (x * sin(angleInRadians) + y * cos(angleInRadians)).toFloat()
    )
}