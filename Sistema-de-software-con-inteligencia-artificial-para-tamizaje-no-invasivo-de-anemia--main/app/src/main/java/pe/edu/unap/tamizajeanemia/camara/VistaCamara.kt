package pe.edu.unap.tamizajeanemia.camara

import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

/**
 * Vista previa de la cámara trasera con recuadro guía, linterna y botón de disparo; guarda la
 * foto en [crearArchivo].
 */
@Composable
fun VistaCamara(
    crearArchivo: () -> File,
    onFoto: (File) -> Unit,
    modifier: Modifier = Modifier,
    guia: Pair<Float, Float>? = null,
) {
    val context = LocalContext.current
    val ciclo = LocalLifecycleOwner.current
    val captura = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }
    var camara by remember { mutableStateOf<Camera?>(null) }
    var linterna by remember { mutableStateOf(false) }
    var capturando by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { ProcessCameraProvider.getInstance(context).get().unbindAll() }
    }

    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).also { vista ->
                    val futuro = ProcessCameraProvider.getInstance(ctx)
                    futuro.addListener({
                        val preview = Preview.Builder().build()
                        preview.setSurfaceProvider(vista.surfaceProvider)
                        futuro.get().run {
                            unbindAll()
                            camara = bindToLifecycle(ciclo, CameraSelector.DEFAULT_BACK_CAMERA, preview, captura)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
        )
        guia?.let { (ancho, alto) ->
            // Oscurece todo menos el recuadro guía.
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawRect(Color.Black.copy(alpha = 0.55f))
                        val tam = Size(size.width * ancho, size.height * alto)
                        drawRoundRect(
                            Color.Transparent,
                            topLeft = Offset((size.width - tam.width) / 2, (size.height - tam.height) / 2),
                            size = tam,
                            cornerRadius = CornerRadius(16.dp.toPx()),
                            blendMode = BlendMode.Clear,
                        )
                    },
            )
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(ancho)
                    .fillMaxHeight(alto)
                    .border(3.dp, Color.White, RoundedCornerShape(16.dp)),
            )
        }
        if (camara?.cameraInfo?.hasFlashUnit() == true) {
            FilledTonalButton(
                onClick = {
                    linterna = !linterna
                    camara?.cameraControl?.enableTorch(linterna)
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            ) { Text(if (linterna) "Linterna: sí" else "Linterna: no") }
        }
        // Botón de disparo estilo cámara.
        Surface(
            onClick = {
                if (capturando) return@Surface
                capturando = true
                val archivo = crearArchivo()
                captura.takePicture(
                    ImageCapture.OutputFileOptions.Builder(archivo).build(),
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(resultado: ImageCapture.OutputFileResults) {
                            capturando = false
                            onFoto(archivo)
                        }

                        override fun onError(error: ImageCaptureException) {
                            capturando = false
                            Toast.makeText(context, "No se pudo tomar la foto: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                )
            },
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(5.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp).size(76.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (capturando) {
                    CircularProgressIndicator(Modifier.size(36.dp))
                } else {
                    Box(Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                }
            }
        }
    }
}
