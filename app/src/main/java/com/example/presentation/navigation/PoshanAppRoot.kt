package com.example.presentation.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.presentation.agency_pds.AgencyAndPdsScreen
import com.example.presentation.auth.EmailOtpLoginScreen
import com.example.presentation.auth.LoginPinScreen
import com.example.presentation.auth.RegistrationScreen
import com.example.presentation.auth.StaffRoleManagementScreen
import com.example.presentation.backup.GoogleDriveBackupScreen
import com.example.presentation.calendar.HolidayCalendarScreen
import com.example.presentation.common.AppLanguage
import com.example.presentation.common.AppStrings
import com.example.presentation.common.LocalAppLanguage
import com.example.presentation.cook.CookManagementScreen
import com.example.presentation.dashboard.DashboardScreen
import com.example.presentation.enrollment.MonthlyEnrollmentScreen
import com.example.presentation.meal.DailyMealScreen
import com.example.presentation.meal.MonthlyMealScreen
import com.example.presentation.more.MoreMenuScreen
import com.example.presentation.reports.ReportsScreen
import com.example.presentation.splash.StartSplashScreen
import com.example.presentation.stock.StockScreen
import com.example.presentation.teacher.MonthlyTeacherDataScreen
import com.example.presentation.viewmodel.PoshanViewModel
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.PoshanTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

sealed class Screen(val route: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", Icons.Default.Home)
    object DailyMeal : Screen("daily_meal", Icons.Default.Restaurant)
    object MonthlyMeal : Screen("monthly_meal", Icons.Default.CalendarMonth)
    object Stock : Screen("stock", Icons.Default.Inventory2)
    object Reports : Screen("reports", Icons.Default.Assessment)
    object More : Screen("more", Icons.Default.Menu)

    // Sub-screens
    object Enrollment : Screen("enrollment", Icons.Default.School)
    object TeacherData : Screen("teacher_data", Icons.Default.Badge)
    object Cooks : Screen("cooks", Icons.Default.People)
    object AgencyAndPds : Screen("agency_pds", Icons.Default.Storefront)
    object Calendar : Screen("calendar", Icons.Default.CalendarMonth)
    object SettingsNorms : Screen("settings_norms", Icons.Default.Tune)
    object StaffRoles : Screen("staff_roles", Icons.Default.ManageAccounts)
    object Registration : Screen("registration", Icons.Default.AppRegistration)
    object LoginPin : Screen("login_pin", Icons.Default.Lock)
    object EmailOtpLogin : Screen("email_otp_login", Icons.Default.CloudSync)
    object GoogleDriveBackup : Screen("google_drive_backup", Icons.Default.CloudSync)
    object Splash : Screen("splash", Icons.Default.Home)

    fun getTitle(lang: AppLanguage): String = when (this) {
        Splash -> "CG MDM"
        Dashboard -> AppStrings.navDashboard(lang)
        DailyMeal -> AppStrings.navDailyMeal(lang)
        MonthlyMeal -> AppStrings.navMonthlyMeal(lang)
        Stock -> AppStrings.navStock(lang)
        Reports -> AppStrings.navReports(lang)
        More -> AppStrings.navMore(lang)
        Enrollment -> if (lang == AppLanguage.HINDI) "छात्र नामांकन" else "Student Census"
        TeacherData -> if (lang == AppLanguage.HINDI) "शिक्षक डेटा" else "Teacher Data"
        Cooks -> if (lang == AppLanguage.HINDI) "रसोइया प्रबंधन" else "Cooks"
        AgencyAndPds -> if (lang == AppLanguage.HINDI) "एजेंसी एवं PDS" else "Agency & PDS"
        Calendar -> if (lang == AppLanguage.HINDI) "कैलेंडर" else "Calendar"
        SettingsNorms -> if (lang == AppLanguage.HINDI) "खाद्यान्न मान एवं सेटिंग्स" else "Food Norms & Settings"
        StaffRoles -> if (lang == AppLanguage.HINDI) "स्टाफ एवं रोल (RBAC)" else "Staff Roles (RBAC)"
        Registration -> if (lang == AppLanguage.HINDI) "शाला पंजीकरण (U-DISE)" else "School Registration"
        LoginPin -> if (lang == AppLanguage.HINDI) "सुरक्षा पिन लॉगिन" else "PIN Login"
        EmailOtpLogin -> if (lang == AppLanguage.HINDI) "ईमेल OTP लॉगिन" else "Email OTP Login"
        GoogleDriveBackup -> if (lang == AppLanguage.HINDI) "Google Drive बैकअप" else "Google Drive Backup"
    }
}

@Composable
fun PoshanAppRoot(
    viewModel: PoshanViewModel = viewModel(),
    initialNavigateToMeal: Boolean = false,
    initialNavigateToBackup: Boolean = false
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    val isSchoolRegistered by viewModel.isSchoolRegistered.collectAsState()
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()

    val computedStartDestination = remember(isSchoolRegistered, isUserLoggedIn) {
        when {
            !isSchoolRegistered -> Screen.Registration.route
            !isUserLoggedIn -> Screen.LoginPin.route
            else -> Screen.Dashboard.route
        }
    }

    val initialStartDestination = remember(initialNavigateToMeal, initialNavigateToBackup) {
        if (initialNavigateToMeal || initialNavigateToBackup) computedStartDestination else Screen.Splash.route
    }

    LaunchedEffect(initialNavigateToMeal, initialNavigateToBackup) {
        if (initialNavigateToMeal) {
            navController.navigate(Screen.DailyMeal.route)
        } else if (initialNavigateToBackup) {
            navController.navigate(Screen.GoogleDriveBackup.route)
        }
    }

    val bottomNavItems: List<Screen> = remember(currentUserRole) {
        when (currentUserRole) {
            com.example.data.local.entity.UserRole.HEADMASTER,
            com.example.data.local.entity.UserRole.MDM_INCHARGE -> listOf(
                Screen.Dashboard,
                Screen.DailyMeal,
                Screen.MonthlyMeal,
                Screen.Stock,
                Screen.Reports,
                Screen.More
            )
            else -> listOf(
                Screen.Dashboard,
                Screen.DailyMeal,
                Screen.MonthlyMeal,
                Screen.More
            )
        }
    }

    val isTopLevelDestination = bottomNavItems.any { it.route == currentRoute || currentRoute?.startsWith(it.route) == true }

    CompositionLocalProvider(LocalAppLanguage provides currentLanguage) {
        PoshanTheme {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (isTopLevelDestination) {
                        NavigationBar(
                            containerColor = Color.White,
                            tonalElevation = 8.dp
                        ) {
                            bottomNavItems.forEach { screen ->
                                val selected = currentRoute == screen.route || currentRoute?.startsWith(screen.route) == true
                                val title = screen.getTitle(currentLanguage)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = title
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.5.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = GreenPrimary,
                                        selectedTextColor = GreenPrimary,
                                        indicatorColor = GreenPrimary.copy(alpha = 0.15f)
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = initialStartDestination,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
                    composable(Screen.Splash.route) {
                        StartSplashScreen(
                            viewModel = viewModel,
                            onSplashFinished = {
                                navController.navigate(computedStartDestination) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.Dashboard.route) {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToMeal = { navController.navigate(Screen.DailyMeal.route) },
                            onNavigateToStock = { navController.navigate(Screen.Stock.route) },
                            onNavigateToEnrollment = { navController.navigate(Screen.Enrollment.route) },
                            onNavigateToCooks = { navController.navigate(Screen.Cooks.route) },
                            onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) },
                            onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                            onNavigateToGoogleDriveBackup = { navController.navigate(Screen.GoogleDriveBackup.route) },
                            onNavigateToLoginPin = {
                                navController.navigate(Screen.LoginPin.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.DailyMeal.route) {
                        DailyMealScreen(
                            viewModel = viewModel,
                            targetDate = null,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = "${Screen.DailyMeal.route}?date={date}",
                        arguments = listOf(
                            navArgument("date") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val targetDate = backStackEntry.arguments?.getString("date")
                        DailyMealScreen(
                            viewModel = viewModel,
                            targetDate = targetDate,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.MonthlyMeal.route) {
                        MonthlyMealScreen(
                            viewModel = viewModel,
                            onNavigateToDailyMeal = { targetDate ->
                                if (!targetDate.isNullOrBlank()) {
                                    viewModel.setSelectedDate(targetDate)
                                    navController.navigate("${Screen.DailyMeal.route}?date=$targetDate") {
                                        popUpTo(Screen.DailyMeal.route) {
                                            inclusive = true
                                        }
                                        launchSingleTop = true
                                    }
                                } else {
                                    navController.navigate(Screen.DailyMeal.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }

                    composable(Screen.Stock.route) {
                        StockScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Reports.route) {
                        ReportsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.More.route) {
                        MoreMenuScreen(
                            viewModel = viewModel,
                            onNavigateToAgencyAndPds = { navController.navigate(Screen.AgencyAndPds.route) },
                            onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) },
                            onNavigateToEnrollment = { navController.navigate(Screen.Enrollment.route) },
                            onNavigateToTeacherData = { navController.navigate(Screen.TeacherData.route) },
                            onNavigateToCooks = { navController.navigate(Screen.Cooks.route) },
                            onNavigateToSettings = { navController.navigate(Screen.SettingsNorms.route) },
                            onNavigateToStaffRoles = { navController.navigate(Screen.StaffRoles.route) },
                            onNavigateToLoginPin = {
                                navController.navigate(Screen.LoginPin.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onNavigateToGoogleDriveBackup = { navController.navigate(Screen.GoogleDriveBackup.route) }
                        )
                    }

                    composable(Screen.StaffRoles.route) {
                        StaffRoleManagementScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Registration.route) {
                        RegistrationScreen(
                            viewModel = viewModel,
                            onRegistrationSuccess = {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.LoginPin.route) {
                        LoginPinScreen(
                            viewModel = viewModel,
                            onLoginSuccess = {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.LoginPin.route) { inclusive = true }
                                }
                            },
                            onNavigateToRegistration = {
                                navController.navigate(Screen.Registration.route)
                            },
                            onNavigateToEmailOtp = {
                                navController.navigate(Screen.EmailOtpLogin.route)
                            }
                        )
                    }

                    composable(Screen.EmailOtpLogin.route) {
                        EmailOtpLoginScreen(
                            onLoginSuccess = {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onNavigateToRegistration = {
                                navController.navigate(Screen.Registration.route)
                            },
                            onQuickPinFallback = {
                                navController.navigate(Screen.LoginPin.route)
                            }
                        )
                    }

                    composable(Screen.SettingsNorms.route) {
                        com.example.presentation.more.SettingsNormsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.TeacherData.route) {
                        MonthlyTeacherDataScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Enrollment.route) {
                        MonthlyEnrollmentScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Cooks.route) {
                        CookManagementScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.AgencyAndPds.route) {
                        AgencyAndPdsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Calendar.route) {
                        HolidayCalendarScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.GoogleDriveBackup.route) {
                        GoogleDriveBackupScreen(
                            onNavigateBack = { navController.popBackStack() },
                            poshanViewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}
