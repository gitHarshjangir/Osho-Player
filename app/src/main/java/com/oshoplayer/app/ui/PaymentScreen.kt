package com.oshoplayer.app.ui

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.oshoplayer.app.R
import com.oshoplayer.app.viewmodel.AudioMixerUiState
import com.oshoplayer.app.viewmodel.PaymentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PaymentScreen(
    state: AudioMixerUiState,
    onClose: () -> Unit,
    onSubmit: (utr: String, imageUri: Uri) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var utr by remember { mutableStateOf("") }
    var confirmUtr by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Activate Premium",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (state.isPaymentPending) {
                    // Pending State
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3C2B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Your premium request is underway, please wait! We will activate your subscription shortly.",
                                color = Color.White,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    // Form State
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.googlepay_qr),
                                    contentDescription = "UPI QR Code",
                                    modifier = Modifier
                                        .size(300.dp)
                                        .padding(8.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                "UPI ID: your.upi.id@bank",
                                color = Color(0xFF00FF7F),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.googlepay_qr)
                                        val values = ContentValues().apply {
                                            put(MediaStore.Images.Media.DISPLAY_NAME, "OshoPlayer_QR.jpg")
                                            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/OshoPlayer")
                                        }
                                        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                                        if (uri != null) {
                                            context.contentResolver.openOutputStream(uri)?.use { out ->
                                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                                            }
                                            launch(Dispatchers.Main) {
                                                showSaveDialog = true
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2D)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Save QR to Gallery", color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        "Please enter your UTR code carefully.",
                        color = Color(0xFFFFB74D),
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = utr,
                        onValueChange = { utr = it; localError = null },
                        label = { Text("UTR Number") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1E1E1E),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = confirmUtr,
                        onValueChange = { confirmUtr = it; localError = null },
                        label = { Text("Confirm UTR Number") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1E1E1E),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2D)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (selectedImageUri == null) "Upload Screenshot" else "Screenshot Selected ✓", color = Color.White)
                    }

                    val errorToShow = localError ?: state.activationError
                    if (errorToShow != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            errorToShow,
                            color = Color(0xFFFF5252),
                            fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val isThrottled = state.paymentThrottleTimerMs > 0
                    val minutes = state.paymentThrottleTimerMs / 60000
                    val seconds = (state.paymentThrottleTimerMs % 60000) / 1000
                    val timeString = String.format("%02d:%02d", minutes, seconds)

                    Button(
                        onClick = {
                            if (isThrottled) return@Button
                            if (utr.isBlank()) {
                                localError = "UTR number cannot be empty."
                                return@Button
                            }
                            if (utr != confirmUtr) {
                                localError = "UTR numbers do not match."
                                return@Button
                            }
                            if (selectedImageUri == null) {
                                localError = "Please upload a payment screenshot."
                                return@Button
                            }
                            onSubmit(utr, selectedImageUri!!)
                        },
                        enabled = !isThrottled && state.paymentSubmissionStatus != PaymentStatus.Submitting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF7F), disabledContainerColor = Color(0xFF00FF7F).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.paymentSubmissionStatus == PaymentStatus.Submitting) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                        } else if (isThrottled) {
                            Text("Wait $timeString to try again", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        } else {
                            Text("Submit Payment", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                containerColor = Color(0xFF1E1E1E),
                title = { Text("Next Steps", color = Color.White) },
                text = { Text("Return back and send us your UTR number so that we can activate your plan.", color = Color.White) },
                confirmButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("OK", color = Color(0xFF00FF7F))
                    }
                }
            )
        }
    }
}
