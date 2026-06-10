package com.example.arfurnitureproject

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// Sceneview and ARCore Imports
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.PlacementMode

// Retail item model data structure definition
data class FurnitureItem(
    val name: String,
    val category: String,
    val dimensions: String,
    val modelUrl: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ModernARApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernARApp() {
    val context = LocalContext.current

    // Inventory storage tracking nodes natively for removal later
    val placedNodes = remember { mutableStateListOf<ArModelNode>() }
    var sceneViewReference by remember { mutableStateOf<ArSceneView?>(null) }

    var selectedModelUrl by remember { mutableStateOf<String?>(null) }
    var statusText by remember { mutableStateOf("Scan the floor. Select an item below to begin!") }

    // State parameters for UI overlay controls
    var showInstructions by remember { mutableStateOf(true) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Control flag for the screen capture pipeline
    var isCapturing by remember { mutableStateOf(false) }

    // Structured Retail Showroom Inventory Catalog Array
    val furnitureCatalog = remember {
        listOf(
            FurnitureItem(
                name = "Sheen Chair",
                category = "Living Room",
                dimensions = "75 x 65 x 80 cm",
                modelUrl = "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master/2.0/SheenChair/glTF-Binary/SheenChair.glb"
            ),
            FurnitureItem(
                name = "Glam Velvet Sofa",
                category = "Living Room",
                dimensions = "180 x 90 x 85 cm",
                modelUrl = "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master/2.0/GlamVelvetSofa/glTF-Binary/GlamVelvetSofa.glb"
            ),
            FurnitureItem(
                name = "Iridescence Lamp",
                category = "Accents & Lighting",
                dimensions = "35 x 35 x 60 cm",
                modelUrl = "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master/2.0/IridescenceLamp/glTF-Binary/IridescenceLamp.glb"
            ),
            FurnitureItem(
                name = "Antique Camera Setup",
                category = "Accents & Lighting",
                dimensions = "25 x 20 x 15 cm",
                modelUrl = "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master/2.0/AntiqueCamera/glTF-Binary/AntiqueCamera.glb"
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // The AR Camera Feed Container
        ARScene(
            modifier = Modifier.fillMaxSize(),
            nodes = emptyList(), // Bypassing unstable declarative Compose list parameters
            planeRenderer = true,
            onCreate = { arSceneView ->
                sceneViewReference = arSceneView

                // Native ARCore View Tap interaction handler
                arSceneView.onTapAr = { hitResult, motionEvent ->
                    if (selectedModelUrl != null) {
                        statusText = "Downloading 3D asset... Keep your phone steady!"

                        // 1. Generate a fixed physical anchor directly from the real-world floor intersection point
                        val physicalAnchor = hitResult.createAnchor()

                        // 2. Instantiate the node with transformation flags enabled
                        val newNode = ArModelNode(
                            engine = arSceneView.engine,
                            placementMode = PlacementMode.DISABLED
                        ).apply {
                            followHitPosition = false
                            anchor = physicalAnchor

                            // Enable touch gestures natively on this specific object
                            isPositionEditable = true // 1-finger drag along floor
                            isRotationEditable = true // 2-finger twist rotation
                            isScaleEditable = true    // Pinch-to-zoom scaling

                            // Load structural geometry from remote endpoint asynchronously
                            loadModelGlbAsync(
                                glbFileLocation = selectedModelUrl!!,
                                scaleToUnits = 0.5f
                            ) { _ ->
                                statusText = "Locked! Drag to move, twist to rotate, pinch to scale."
                            }
                        }

                        // Inject down directly into the engine's active hardware scene graph
                        arSceneView.addChild(newNode)
                        placedNodes.add(newNode)
                    } else {
                        statusText = "Please select an item from the catalog first!"
                    }
                }
            }
        )

        // --- PERSISTENT DOCK OVERLAY STRIP ---
        if (!isCapturing) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- STRIP ROW 1: UTILITY ACTIONS (Balanced 50/50 Split) ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // BUTTON 1: Open Retail Catalog Drawer
                        Button(
                            onClick = { showBottomSheet = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Catalog", maxLines = 1)
                        }

                        // BUTTON 2: SCREEN CAPTURE SHUTTER PIPELINE
                        Button(
                            onClick = {
                                val currentSceneView = sceneViewReference
                                if (currentSceneView != null) {
                                    statusText = "Saving layout... Please keep phone steady!"
                                    isCapturing = true

                                    currentSceneView.post {
                                        try {
                                            val snapshotBitmap = Bitmap.createBitmap(
                                                currentSceneView.width,
                                                currentSceneView.height,
                                                Bitmap.Config.ARGB_8888
                                            )

                                            PixelCopy.request(
                                                currentSceneView,
                                                snapshotBitmap,
                                                { resultStatus ->
                                                    if (resultStatus == PixelCopy.SUCCESS) {
                                                        val imagePathStr = MediaStore.Images.Media.insertImage(
                                                            context.contentResolver,
                                                            snapshotBitmap,
                                                            "AR_Furniture_Design_${System.currentTimeMillis()}",
                                                            "My Custom Virtual Interior Layout Design"
                                                        )

                                                        if (imagePathStr != null) {
                                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                                type = "image/jpeg"
                                                                putExtra(Intent.EXTRA_STREAM, Uri.parse(imagePathStr))
                                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            }
                                                            context.startActivity(Intent.createChooser(shareIntent, "Share Room Via"))
                                                            statusText = "Room design saved successfully to your gallery!"
                                                        } else {
                                                            statusText = "Storage compilation write failed."
                                                        }
                                                    } else {
                                                        statusText = "Composition capture failed. Attempt scan again."
                                                    }
                                                    isCapturing = false
                                                },
                                                Handler(Looper.getMainLooper())
                                            )
                                        } catch (e: Exception) {
                                            statusText = "Capture runtime error: ${e.localizedMessage}"
                                            isCapturing = false
                                        }
                                    }
                                } else {
                                    statusText = "Engine initialization incomplete."
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("Capture", maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // --- STRIP ROW 2: CLEAN ROOM ACTION ---
                    Button(
                        onClick = {
                            placedNodes.forEach { node ->
                                sceneViewReference?.removeChild(node)
                                node.destroy()
                            }
                            placedNodes.clear()
                            selectedModelUrl = null
                            statusText = "Scene cleared! Open catalog to select an item."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Clear Scene", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // --- EXPANDABLE MATERIAL 3 RETAIL BOTTOM SHEET CATALOG ---
        if (showBottomSheet && !isCapturing) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Select Furniture Asset",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(furnitureCatalog) { item ->
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedModelUrl = item.modelUrl
                                        statusText = "${item.name} selected! Tap your floor grid to drop it."
                                        showBottomSheet = false
                                    },
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                    ) {
                                        Text(
                                            text = item.name.first().toString(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = item.category,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = "Size: ${item.dimensions}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- FULL VIEW APP INSTRUCTION OVERLAY DIALOG ---
        if (showInstructions && !isCapturing) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxHeight(0.85f) // Constraints bounds to guarantee comfortable viewport scaling
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(modifier = Modifier.padding(20.dp)) {

                            // DISMISS 'X' BUTTON
                            IconButton(
                                onClick = { showInstructions = false },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Text(
                                    text = "✕",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // INSTRUCTION TEXT MATRIX
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                                    // FIXED CRITICAL BUG: Enables structural native canvas touch gestures layout updates scrolling smoothly
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "How to Use the App",
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Text(
                                    text = "1. Scan Your Room\nMove your phone slowly over a brightly lit, textured surface (like a rug, carpet, or wood grain) until a white dotted tracking grid fully maps out.\n\n" +
                                            "2. Browse the Catalog\nTap the 'Catalog' button to pull up our interactive retail drawer. Review classifications, dimensions, and pick an asset.\n\n" +
                                            "3. Drop Furniture\nTap anywhere directly on that white dotted floor grid to download and lock your asset to real-world coordinates.\n\n" +
                                            "4. Refine Placement (Gestures)\n• Drag (1 Finger): Slide items cleanly across your room.\n" +
                                            "• Twist (2 Fingers): Spin items to set layout orientation.\n" +
                                            "• Pinch: Scale spatial sizes up or down.\n\n" +
                                            "5. Capture & Share Design\nTap the 'Capture' button. The app will instantly clear all menus, snapshot your mixed-reality layout, and launch the Android Share Sheet to message your design to friends or interior decorators.\n\n" +
                                            "6. Reset Layout\nUse the red 'Clear Scene' row button to clean your environment canvas and start fresh.",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = { showInstructions = false },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Got It!")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}