package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.ImageAnalysisResult
import com.example.data.model.PromptDetailLevel
import com.example.data.model.PromptMode
import com.example.ui.components.AnimatedGlowCard
import com.example.ui.components.RefPromptTopBar
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.VioletNeon
import kotlinx.coroutines.delay

@Composable
fun PromptResultScreen(
    analysisResult: ImageAnalysisResult,
    imagePath: String?,
    mode: PromptMode,
    detailLevel: PromptDetailLevel,
    isSaved: Boolean,
    onBack: () -> Unit,
    onRegenerate: () -> Unit,
    onToggleSave: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showCopiedMasterToast by remember { mutableStateOf(false) }
    var showCopiedNegativeToast by remember { mutableStateOf(false) }
    var isBreakdownExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(showCopiedMasterToast) {
        if (showCopiedMasterToast) {
            delay(2000)
            showCopiedMasterToast = false
        }
    }

    LaunchedEffect(showCopiedNegativeToast) {
        if (showCopiedNegativeToast) {
            delay(2000)
            showCopiedNegativeToast = false
        }
    }

    fun copyToClipboard(text: String, isNegative: Boolean = false) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("AI Prompt", text)
        clipboard.setPrimaryClip(clip)
        if (isNegative) {
            showCopiedNegativeToast = true
        } else {
            showCopiedMasterToast = true
        }
    }

    fun sharePrompt(text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share AI Recreation Prompt")
        context.startActivity(shareIntent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        RefPromptTopBar(
            title = "Analysis Result",
            subtitle = null,
            canNavigateBack = true,
            onBackClick = onBack,
            showHistoryAction = false,
            showSettingsAction = false
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header badge & title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyanNeon.copy(alpha = 0.15f))
                                .border(0.8.dp, CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = mode.displayName.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanNeon
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(VioletNeon.copy(alpha = 0.15f))
                                .border(0.8.dp, VioletNeon.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = detailLevel.displayName.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = VioletNeon
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Your AI Prompt Is Ready",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Reference Thumbnail
                if (!imagePath.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imagePath)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Reference Thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Master Prompt Card
            AnimatedGlowCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("master_prompt_card")
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "MASTER RECREATION PROMPT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = CyanNeon
                        )

                        // Animated Copied Pill
                        AnimatedVisibility(
                            visible = showCopiedMasterToast,
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EmeraldSuccess)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Prompt copied ✓",
                                        color = Color.Black,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Master Prompt Text
                    SelectionContainerBox(text = analysisResult.masterPrompt)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Card Actions: Copy, Share, Save, Regenerate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { copyToClipboard(analysisResult.masterPrompt) },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                                .testTag("copy_prompt_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanNeon,
                                contentColor = Color(0xFF001F25)
                            )
                        ) {
                            Icon(
                                imageVector = if (showCopiedMasterToast) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy Prompt",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showCopiedMasterToast) "Copied!" else "Copy Prompt",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { sharePrompt(analysisResult.masterPrompt) },
                            modifier = Modifier
                                .weight(0.9f)
                                .height(46.dp)
                                .testTag("share_prompt_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Share",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        OutlinedButton(
                            onClick = onToggleSave,
                            modifier = Modifier
                                .weight(0.9f)
                                .height(46.dp)
                                .testTag("save_prompt_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSaved) CyanNeon else Color(0xFF334155)
                            )
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Save",
                                modifier = Modifier.size(15.dp),
                                tint = if (isSaved) CyanNeon else MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSaved) "Saved" else "Save",
                                fontSize = 13.sp,
                                color = if (isSaved) CyanNeon else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Regenerate Action
                    OutlinedButton(
                        onClick = onRegenerate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("regenerate_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263352))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Regenerate",
                            modifier = Modifier.size(16.dp),
                            tint = VioletNeon
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Regenerate Analysis",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VioletNeon
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Negative Prompt Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131826)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263352)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("negative_prompt_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "NEGATIVE PROMPT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color(0xFFF43F5E)
                        )

                        AnimatedVisibility(visible = showCopiedNegativeToast) {
                            Text(
                                text = "Negative copied ✓",
                                color = EmeraldSuccess,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = analysisResult.negativePrompt.ifBlank {
                            "distorted anatomy, low resolution, blurry, watermark, bad hands, artifacts"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { copyToClipboard(analysisResult.negativePrompt, isNegative = true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("copy_negative_prompt_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3E1D2A))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Negative Prompt",
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFFDA4AF)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Copy Negative Prompt",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFDA4AF)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Expandable Granular Breakdown Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131826)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263352)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isBreakdownExpanded = !isBreakdownExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(CyanNeon.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewQuilt,
                                    contentDescription = null,
                                    tint = CyanNeon,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Comprehensive Visual Breakdown",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Icon(
                            imageVector = if (isBreakdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isBreakdownExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = isBreakdownExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            BreakdownItem(
                                title = "Subject & Identity",
                                content = analysisResult.subject,
                                icon = Icons.Default.Person,
                                accentColor = CyanNeon
                            )

                            BreakdownItem(
                                title = "Composition & Framing",
                                content = analysisResult.composition,
                                icon = Icons.Default.ViewQuilt,
                                accentColor = VioletNeon
                            )

                            BreakdownItem(
                                title = "Lighting & Shadows",
                                content = analysisResult.lighting,
                                icon = Icons.Default.LightMode,
                                accentColor = Color(0xFFF59E0B)
                            )

                            BreakdownItem(
                                title = "Color Palette & Mood",
                                content = analysisResult.colors,
                                icon = Icons.Default.ColorLens,
                                accentColor = Color(0xFFEC4899)
                            )

                            BreakdownItem(
                                title = "Camera & Optics",
                                content = analysisResult.camera,
                                icon = Icons.Default.CameraAlt,
                                accentColor = EmeraldSuccess
                            )

                            BreakdownItem(
                                title = "Environment & Setting",
                                content = analysisResult.environment,
                                icon = Icons.Default.Public,
                                accentColor = Color(0xFF38BDF8)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SelectionContainerBox(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0B0E18),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E283D)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                lineHeight = 22.sp
            ),
            color = Color(0xFFF1F5F9),
            modifier = Modifier
                .padding(14.dp)
                .testTag("master_prompt_text")
        )
    }
}

@Composable
private fun BreakdownItem(
    title: String,
    content: String,
    icon: ImageVector,
    accentColor: Color
) {
    if (content.isBlank()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F1422))
            .border(1.dp, Color(0xFF1F293D), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
            color = Color(0xFFCBD5E1)
        )
    }
}
