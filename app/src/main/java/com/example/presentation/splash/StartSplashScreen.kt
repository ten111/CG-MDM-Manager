package com.example.presentation.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.auth.AuthSessionManager
import com.example.presentation.viewmodel.PoshanViewModel
import kotlinx.coroutines.delay

/**
 * Reusable watermarked anime school background with a high-key whitening aesthetic.
 * Watermarks the rural school meal scene with a gentle transparency so foreground
 * text and forms remain crisp and distraction-free.
 */
@Composable
fun WatermarkedSchoolWallpaper(
    modifier: Modifier = Modifier,
    transparencyAlpha: Float = 0.22f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFCFDFE))
    ) {
        // High-key whitening background wallpaper with transparency watermark
        Image(
            painter = painterResource(id = R.drawable.img_mdm_school_wallpaper_1788537001237),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(transparencyAlpha),
            contentScale = ContentScale.Crop
        )

        // Soft white gradient veil for high-contrast whitening aesthetic
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.55f),
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.70f)
                        )
                    )
                )
        )

        content()
    }
}

/**
 * Polished startup / splash screen shown when the app opens.
 * Displays the anime school wallpaper with watermarked transparency,
 * prominent central logo, dynamic school identity loaded from the app database,
 * and clean MDM register designations (with no hardcoded government tags).
 */
@Composable
fun StartSplashScreen(
    viewModel: PoshanViewModel? = null,
    onSplashFinished: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val school by (viewModel?.school?.collectAsState() ?: remember { mutableStateOf(null) })

    val prefSchoolName = remember { AuthSessionManager.getSchoolName(context).trim() }
    val prefSchoolUdise = remember { AuthSessionManager.getSchoolUdise(context).trim() }

    // Dynamically resolve school name from database / app preferences (no dummy hardcoded fallbacks)
    val dynamicSchoolName = remember(school, prefSchoolName) {
        val dbName = school?.schoolName?.trim()
        if (!dbName.isNullOrBlank()) dbName else prefSchoolName
    }

    // Dynamically resolve block, district & U-DISE
    val dynamicLocation = remember(school, prefSchoolUdise) {
        val s = school
        if (s != null && (s.blockName.isNotBlank() || s.districtName.isNotBlank() || s.udiseCode.isNotBlank())) {
            val parts = mutableListOf<String>()
            if (s.blockName.isNotBlank()) parts.add("विकासखण्ड: ${s.blockName.trim()}")
            if (s.districtName.isNotBlank()) parts.add("जिला: ${s.districtName.trim()}")
            if (s.udiseCode.isNotBlank()) parts.add("U-DISE: ${s.udiseCode.trim()}")
            parts.joinToString(" • ")
        } else if (prefSchoolUdise.isNotBlank()) {
            "U-DISE: $prefSchoolUdise"
        } else {
            ""
        }
    }

    // The school card must not exist when the app is opened for the first time before setup
    val hasSchoolDetails = remember(dynamicSchoolName) {
        dynamicSchoolName.isNotBlank()
    }

    // Auto-advance after 1.8s or tap immediately
    LaunchedEffect(Unit) {
        isVisible = true
        delay(1800)
        onSplashFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    WatermarkedSchoolWallpaper(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onSplashFinished()
            },
        transparencyAlpha = 0.25f
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top balanced spacing without government header
            Spacer(modifier = Modifier.height(20.dp))

            // Central Hero Block: Logo + Dynamic School Identity
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(600)) + scaleIn(tween(600, easing = FastOutSlowInEasing), initialScale = 0.85f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Central Logo Container with elevated white card and soft halo
                    Surface(
                        modifier = Modifier
                            .size(156.dp)
                            .scale(pulseScale)
                            .shadow(
                                elevation = 12.dp,
                                shape = CircleShape,
                                spotColor = Color(0xFF1E3A8A).copy(alpha = 0.25f)
                            ),
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(3.5.dp, Color.White)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.cg_mdm_final_logo),
                            contentDescription = "CG MDM Manager Logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Dynamic School Name from App Database (only exists if school details are set up)
                    if (hasSchoolDetails) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.88f),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            shadowElevation = 3.dp,
                            modifier = Modifier.fillMaxWidth(0.94f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = dynamicSchoolName,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp,
                                        lineHeight = 26.sp
                                    ),
                                    color = Color(0xFF1E3A8A),
                                    textAlign = TextAlign.Center
                                )

                                if (dynamicLocation.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = dynamicLocation,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.5.sp
                                        ),
                                        color = Color(0xFF475569),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Secondary App Designation
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Text(
                            text = "CG-MDM MANAGER • डिजिटल शाला पंजी",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Bottom Loading / Starting Indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .width(130.dp)
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF16A34A),
                    trackColor = Color(0xFFE2E8F0)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "प्रारंभ हो रहा है...",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
