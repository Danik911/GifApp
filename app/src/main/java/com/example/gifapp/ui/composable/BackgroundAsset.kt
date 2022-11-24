package com.example.gifapp.ui.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.gifapp.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RenderBackground(
    assetContainerHeightDp: Int
) {
    Box(modifier = Modifier.wrapContentSize())
    {
        val backgroundAsset = painterResource(id = R.drawable.background_image)
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .height(assetContainerHeightDp.dp),
            painter = backgroundAsset,
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