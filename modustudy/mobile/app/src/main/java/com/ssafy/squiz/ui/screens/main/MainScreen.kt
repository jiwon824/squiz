package com.ssafy.squiz.ui.screens.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ssafy.squiz.data.local.AuthManager
import com.ssafy.squiz.ui.navigation.BottomNavItem
import com.ssafy.squiz.ui.navigation.NavRoutes
import com.ssafy.squiz.ui.screens.home.HomeScreen
import com.ssafy.squiz.ui.screens.mystudy.MyStudiesScreen
import com.ssafy.squiz.ui.screens.quiz.QuizHomeScreen
import com.ssafy.squiz.ui.screens.mypage.MyPageScreen
import com.ssafy.squiz.ui.theme.*

@Composable
fun MainScreen(
    onNavigateToStudyDetail: (Long) -> Unit,
    onNavigateToStudySearch: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToDmList: () -> Unit,
    onNavigateToStudyHome: (Long) -> Unit,
    onNavigateToQuizSolve: (Long) -> Unit,
    onNavigateToWrongNotes: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToMyActivity: () -> Unit,
    onNavigateToBookmarkedStudies: () -> Unit,
    onNavigateToMyApplications: () -> Unit,
    onNavigateToStudyTemplates: () -> Unit,
    onNavigateToScheduleList: () -> Unit,
    onNavigateToStudyCreate: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val authManager = remember { AuthManager.getInstance(context) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            SquizBottomBar(
                items = BottomNavItem.items,
                currentRoute = currentDestination?.route,
                onItemClick = { item ->
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(NavRoutes.Home.route) {
                HomeScreen(
                    onStudyClick = onNavigateToStudyDetail,
                    onSearchClick = onNavigateToStudySearch,
                    onNotificationClick = onNavigateToNotifications,
                    onDmClick = onNavigateToDmList,
                    onScheduleClick = onNavigateToScheduleList,
                    onBookmarkedClick = onNavigateToBookmarkedStudies,
                    onMyApplicationsClick = onNavigateToMyApplications,
                    onTemplatesClick = onNavigateToStudyTemplates,
                    onStartReview = {
                        // 퀴즈 탭으로 이동
                        navController.navigate(NavRoutes.QuizHome.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(NavRoutes.MyStudies.route) {
                MyStudiesScreen(
                    onStudyClick = onNavigateToStudyHome
                )
            }

            composable(NavRoutes.QuizHome.route) {
                QuizHomeScreen(
                    onQuizClick = onNavigateToQuizSolve,
                    onWrongNotesClick = onNavigateToWrongNotes
                )
            }

            composable(NavRoutes.MyPage.route) {
                MyPageScreen(
                    onEditProfileClick = onNavigateToEditProfile,
                    onMyActivityClick = onNavigateToMyActivity,
                    onNotificationSettingsClick = { /* TODO: Navigate to notification settings */ },
                    onPrivacySettingsClick = { /* TODO: Navigate to privacy settings */ },
                    onLogoutClick = {
                        // 로그아웃 처리: 토큰 삭제 후 로그인 화면으로 이동
                        authManager.logout()
                        onNavigateToLogin()
                    }
                )
            }
        }
    }
}

@Composable
private fun SquizBottomBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                BottomNavItemView(
                    item = item,
                    selected = selected,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun BottomNavItemView(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) Primary.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(200),
        label = ""
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = ""
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = ""
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.title,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.title,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
