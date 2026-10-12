package com.example.ui.navigation

sealed class Screen(val route: String, val title: String) {
    data object Dashboard : Screen("dashboard", "Dashboard")
    data object Passbook : Screen("passbook", "Passbook")
    data object Insights : Screen("insights", "Insights")
    data object Payments : Screen("payments", "Payments")
    data object FraudShield : Screen("fraud_shield", "Fraud Shield")
    data object Settings : Screen("settings", "Settings")
    data object SmsSandbox : Screen("sms_sandbox", "SMS Tester")
    data object Onboarding : Screen("onboarding", "Welcome")
}

val BOTTOM_NAV_ITEMS = listOf(
    Screen.Dashboard,
    Screen.Passbook,
    Screen.Insights,
    Screen.Payments,
    Screen.FraudShield
)
