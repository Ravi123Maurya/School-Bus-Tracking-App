package com.ravi.busmanagementt.presentation.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.launch
import com.ravi.busmanagementt.R
import com.ravi.busmanagementt.presentation.navigation.NavRoutes

// Onboarding Page Data
data class OnboardingPage(
    val title: String,
    val description: String,
    val lottieRes: Int,
    val backgroundColor: Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            title = "Track Your Bus\nin Real-Time",
            description = "Know exactly where your child's bus is at any moment with live GPS tracking.",
            lottieRes = R.raw.realtime_tracking_lottie,
            backgroundColor = Color(0xFFE8E4DC)
        ),
        OnboardingPage(
            title = "Stay Informed with\nNotifications",
            description = "Get instant alerts when the bus approaches your stop or when your child boards.",
            lottieRes = R.raw.stay_informed_lottie,
            backgroundColor = Color(0xFFF5E6E8)
        ),
        OnboardingPage(
            title = "Safe & Secure\nJourney",
            description = "Monitor routes, stops and ensure your child's safe journey every day.",
            lottieRes = R.raw.safe_secure_lottie,
            backgroundColor = Color(0xFFDFF0E8)
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()


    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFF4A4543))
        ) {

            // Main content card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
//                .padding(horizontal = 24.dp, vertical = 48.dp)
                    .align(Alignment.Center),
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = pages[pagerState.currentPage].backgroundColor
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pager content
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f)
                    ) { page ->
                        OnboardingPageContent(
                            page = pages[page]
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Page indicators
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 32.dp)
                    ) {
                        repeat(pages.size) { index ->
                            PageIndicatorAnimated(
                                isActive = pagerState.currentPage == index
                            )
                        }
                    }

                    // Bottom buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Skip button
                        TextButton(
                            onClick = onComplete,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "SKIP",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )
                        }

                        // Next/Start button
                        Button(
                            onClick = {
                                if (pagerState.currentPage == pages.size - 1) {
                                    onComplete()
                                } else {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            modifier = Modifier
                                .width(140.dp)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Text(
                                text = if (pagerState.currentPage == pages.size - 1) "START" else "NEXT",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage
) {
    // Lottie animation
    val lottieComposition = rememberLottieComposition(
        LottieCompositionSpec.RawRes(page.lottieRes)
    )

    val progress by animateLottieCompositionAsState(
        composition = lottieComposition.value,
        isPlaying = true,
        iterations = LottieConstants.IterateForever,
        speed = 0.8f
    )

    // Entry animation
    val visibleState = remember {
        MutableTransitionState(false).apply {
            targetState = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(
                animationSpec = tween(durationMillis = 600)
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Lottie animation - larger illustration area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // White circular background
                    Surface(
                        modifier = Modifier.size(280.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.9f)
                    ) {}

                    LottieAnimation(
                        composition = lottieComposition.value,
                        progress = { progress },
                        modifier = Modifier.size(300.dp)
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Title
                Text(
                    text = page.title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 36.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Description
                Text(
                    text = page.description,
                    fontSize = 16.sp,
                    color = Color.Black.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}


// Alternative version with animated indicator width
@Composable
private fun PageIndicatorAnimated(
    isActive: Boolean
) {
    val width by animateDpAsState(
        targetValue = if (isActive) 24.dp else 8.dp,
        animationSpec = tween(durationMillis = 300)
    )

    Box(
        modifier = Modifier
            .width(width)
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isActive) Color.Black else Color.Black.copy(alpha = 0.3f)
            )
    )
}


@Preview
@Composable
fun OnboardingScreenRoute() {
    OnboardingScreen(
        onComplete = {
            // Save that onboarding is completed (SharedPreferences/DataStore)
            // Navigate to login or home screen
        }
    )
}