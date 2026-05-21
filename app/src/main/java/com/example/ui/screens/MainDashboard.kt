package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.composed
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.example.data.model.AssetAccount
import com.example.data.model.Budget
import com.example.data.model.FrequentTemplate
import com.example.data.model.Transaction
import com.example.data.model.AppPreference
import com.example.ui.viewmodel.BudgetTrackerViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    viewModel: BudgetTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var isAppUnlocked by remember { mutableStateOf(false) }

    // Display messages via Toast on synchronization status updates
    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearSyncMessage()
        }
    }

    // Security Gate check
    val hasPasscode = preferences.passcode.isNotBlank()
    
    if (hasPasscode && !isAppUnlocked) {
        PasscodeLockOverlay(
            correctPasscode = preferences.passcode,
            onPassed = { isAppUnlocked = true }
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Asset Book",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(26.dp)
                                    .padding(end = 4.dp)
                            )
                            Text(
                                text = "Expensee",
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.SansSerif,
                                letterSpacing = (-0.5).sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pro",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    },
                    actions = {
                        val isDark = when (preferences.darkModeSetting) {
                            1 -> false
                            2 -> true
                            else -> isSystemInDarkTheme()
                        }
                        IconButton(
                            onClick = {
                                viewModel.updateDarkModeSetting(if (isDark) 1 else 2)
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme Mode",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = "Book"
                            )
                        },
                        label = { Text("Book", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_overview")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = "Analytic"
                            )
                        },
                        label = { Text("Analytic", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_budgets")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        },
                        label = { Text("Settings", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_settings")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width / 3 } + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                                .togetherWith(slideOutHorizontally { width -> -width / 3 } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width / 3 } + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                                .togetherWith(slideOutHorizontally { width -> width / 3 } + fadeOut())
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "tabChangeTransition"
                ) { targetState ->
                    when (targetState) {
                        0 -> OverviewTab(viewModel, preferences)
                        1 -> AnalyticTab(viewModel, preferences)
                        2 -> SettingsTab(viewModel, preferences)
                    }
                }
            }
        }
    }
}

// --- CUSTOM DIALOG & OVERRIDE DIALOG BACKPORT FOR EXCELLENT FLUID DESIGN ---

@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    iconContentColor: Color = MaterialTheme.colorScheme.primary,
    titleContentColor: Color = MaterialTheme.colorScheme.onSurface,
    textContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    tonalElevation: androidx.compose.ui.unit.Dp = 8.dp,
    properties: androidx.compose.ui.window.DialogProperties = androidx.compose.ui.window.DialogProperties()
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .wrapContentHeight()
                .shadow(16.dp, shape),
            shape = shape,
            color = containerColor,
            tonalElevation = tonalElevation
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (icon != null || title != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (icon != null) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                icon()
                            }
                        }
                        if (title != null) {
                            CompositionLocalProvider(
                                LocalContentColor provides titleContentColor,
                                LocalTextStyle provides MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            ) {
                                title()
                            }
                        }
                    }
                }

                if (text != null) {
                    CompositionLocalProvider(
                        LocalContentColor provides textContentColor,
                        LocalTextStyle provides MaterialTheme.typography.bodyMedium
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            text()
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (dismissButton != null) {
                        dismissButton()
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    confirmButton()
                }
            }
        }
    }
}

// --- ARITHMETIC EVALUATOR HELPER ---

fun evaluateArithmetic(input: String): Double {
    return try {
        val clean = input.replace(" ", "")
        if (clean.isBlank()) return 0.0
        
        var total = 0.0
        var currentToken = ""
        var lastOp = '+'
        
        for (char in "$clean+") {
            if (char in listOf('+', '-', '*', '/')) {
                val value = currentToken.toDoubleOrNull() ?: 0.0
                total = when (lastOp) {
                    '+' -> total + value
                    '-' -> total - value
                    '*' -> total * value
                    '/' -> if (value != 0.0) total / value else total
                    else -> total
                }
                currentToken = ""
                lastOp = char
            } else {
                currentToken += char
            }
        }
        total
    } catch (e: Exception) {
        input.toDoubleOrNull() ?: 0.0
    }
}

// --- SECURITY CODE LOCK SCREEN SCREEN ---

@Composable
fun PasscodeLockOverlay(
    correctPasscode: String,
    onPassed: () -> Unit
) {
    var enteredText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val shakeOffset by animateDpAsState(
        targetValue = if (isError) 10.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "shakeOffset"
    )

    LaunchedEffect(enteredText) {
        if (enteredText.isNotEmpty()) {
            isError = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier
                .widthIn(max = 340.dp)
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .shadow(12.dp, CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (enteredText.length == 4 && !isError) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = "Security Passcode",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "EXPENSEE SECURE GATE",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Enter 4-Digit Security PIN",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Animated dot display
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .offset(x = shakeOffset)
                    .padding(vertical = 12.dp)
            ) {
                for (i in 0 until 4) {
                    val filled = i < enteredText.length
                    val scale by animateFloatAsState(
                        targetValue = if (filled) 1.25f else 1.0f,
                        animationSpec = spring(stiffness = Spring.StiffnessHigh),
                        label = "dot"
                    )

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .size(18.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = if (isError) MaterialTheme.colorScheme.error 
                                        else if (filled) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                            .background(
                                if (isError) MaterialTheme.colorScheme.error
                                else if (filled) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                    )
                }
            }

            Box(
                modifier = Modifier.height(20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isError) {
                    Text(
                        text = "Incorrect Lock PIN. Try again.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Enhanced Numerical Keyboard using clean custom buttons with scaling on press
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("CLR", "0", "DEL")
                )

                rows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { char ->
                            val isSpecial = char == "CLR" || char == "DEL"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(
                                        if (isSpecial) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .bounceClick {
                                        isError = false
                                        when (char) {
                                            "CLR" -> enteredText = ""
                                            "DEL" -> if (enteredText.isNotEmpty()) enteredText = enteredText.dropLast(1)
                                            else -> {
                                                if (enteredText.length < 4) {
                                                    enteredText += char
                                                }
                                            }
                                        }
                                        if (enteredText.length == 4) {
                                            if (enteredText == correctPasscode) {
                                                onPassed()
                                            } else {
                                                isError = true
                                                enteredText = ""
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (char == "DEL") {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Backspace",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(
                                        text = char,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSpecial) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Format Helper
fun formatLocalCurrency(amount: Double, symbol: String): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    val base = format.format(Math.abs(amount))
    // replace default "$" with the chosen symbol dynamically
    return (if (amount < 0) "-" else "") + base.replace("$", symbol)
}

// --- OVERVIEW TAB PANEL ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OverviewTab(
    viewModel: BudgetTrackerViewModel,
    pref: AppPreference
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    
    val netWorth by viewModel.netWorth.collectAsStateWithLifecycle()
    val totalAssets by viewModel.totalAssets.collectAsStateWithLifecycle()
    val totalLiabilities by viewModel.totalLiabilities.collectAsStateWithLifecycle()

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAddTxDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            // Beautiful cohesive curved gradient background of double-entry ledger balance
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF281C4F),
                                    Color(0xFF4A2E80)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "Net Wealth",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.65f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatLocalCurrency(netWorth, pref.currencySymbol),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 38.sp,
                                letterSpacing = (-1).sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Liquid Assets",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = formatLocalCurrency(totalAssets, pref.currencySymbol),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF81C784)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Card Liabilities",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = formatLocalCurrency(totalLiabilities, pref.currencySymbol),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE57373)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bookmark Templates Grid for 1-Click Entry
        if (templates.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Frequent Bookmarks",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "(1-Click Record)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(templates) { t ->
                            Card(
                                modifier = Modifier
                                    .width(134.dp)
                                    .bounceClick { viewModel.triggerQuickTemplate(t) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = t.templateTitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatLocalCurrency(t.amount, pref.currencySymbol),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "To ${t.category}",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dual Bookkeeping action trigger grids
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val addTxInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = { showAddTxDialog = true },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp)
                        .scaleOnPress(addTxInteraction)
                        .testTag("add_manual_tx_btn"),
                    interactionSource = addTxInteraction,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Record", fontWeight = FontWeight.Bold)
                }

                val addAccInteraction = remember { MutableInteractionSource() }
                FilledTonalButton(
                    onClick = { showAddAccountDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .scaleOnPress(addAccInteraction)
                        .testTag("add_asset_btn"),
                    interactionSource = addAccInteraction,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Wallet", maxLines = 1)
                }
            }
        }

        // Subtitle: Offline accounts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Asset Wallets & Cards",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${accounts.size} Active",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        if (accounts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Asset Wallet Registered",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Register a standard Cash Wallet, Credit Card, or Checking Account to start budgeting offline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        } else {
            items(accounts) { acc ->
                OfflineAccountCard(account = acc, pref = pref, onDisconnect = { viewModel.removeAssetAccount(acc.id) })
            }
        }

        // Historic Ledger Stream records
        item {
            Text(
                text = "Ledger Activity Log",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (transactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recorded transactions yet. Tap Add Record above!",
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(transactions) { tx ->
                val sourceAcc = accounts.firstOrNull { it.id == tx.accountId }
                val destAcc = if (tx.type == "TRANSFER") accounts.firstOrNull { it.id == tx.destAccountId } else null
                
                InteractiveTransactionRow(
                    tx = tx,
                    sourceAsset = sourceAcc,
                    destAsset = destAcc,
                    pref = pref,
                    onDelete = { viewModel.removeTransaction(tx) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showAddAccountDialog) {
        CreateAssetDialog(
            onDismiss = { showAddAccountDialog = false },
            onSave = { name, type, bal, day, col ->
                viewModel.addNewAssetAccount(name, type, bal, day, col)
                showAddAccountDialog = false
            }
        )
    }

    if (showAddTxDialog) {
        AddManualTransactionDialog(
            accounts = accounts,
            templates = templates,
            pref = pref,
            onDismiss = { showAddTxDialog = false },
            onSave = { acc, dst, type, amt, date, name, cat, notes, recur, recInt ->
                viewModel.addNewLocalTransaction(acc, dst, type, amt, date, name, cat, notes, recur, recInt)
                showAddTxDialog = false
            },
            onSaveTemplate = { title, accId, destId, type, amt, cat, name ->
                viewModel.saveBookmarkTemplate(title, accId, destId, type, amt, cat, name)
            }
        )
    }
}

@Composable
fun OfflineAccountCard(
    account: AssetAccount,
    pref: AppPreference,
    onDisconnect: () -> Unit
) {
    var isDeleteExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(android.graphics.Color.parseColor(account.colorHex)).copy(alpha = 0.08f)
        ),
        border = BorderStroke(
            1.dp,
            Color(android.graphics.Color.parseColor(account.colorHex)).copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(android.graphics.Color.parseColor(account.colorHex)).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (account.type) {
                        "Credit Card" -> Icons.Default.CreditCard
                        "Savings" -> Icons.Default.Savings
                        "Wallet" -> Icons.Default.Wallet
                        else -> Icons.Default.AccountBalance
                    },
                    contentDescription = null,
                    tint = Color(android.graphics.Color.parseColor(account.colorHex))
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${account.type} • Settlement Day: ${account.billingDate}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                val isCard = account.type.equals("Credit Card", ignoreCase = true)
                Text(
                    text = formatLocalCurrency(account.balance, pref.currencySymbol),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = if (isCard) Color(0xFFC62828) else Color(0xFF2E7D32)
                )
                Text(
                    text = "Purge Account",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .bounceClick { isDeleteExpanded = true }
                        .padding(top = 2.dp)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }

    if (isDeleteExpanded) {
        AlertDialog(
            onDismissRequest = { isDeleteExpanded = false },
            title = { Text("Delete Asset Account?") },
            text = { Text("Purging '${account.name}' will permanently erase its logs and all associated double-entry ledger listings.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDisconnect()
                        isDeleteExpanded = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Erase")
                }
            },
            dismissButton = {
                TextButton(onClick = { isDeleteExpanded = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun InteractiveTransactionRow(
    tx: Transaction,
    sourceAsset: AssetAccount?,
    destAsset: AssetAccount?,
    pref: AppPreference,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { showDeleteConfirm = true }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon matching category
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when (tx.type) {
                        "INCOME" -> Color(0xFFE8F5E9)
                        "TRANSFER" -> Color(0xFFE3F2FD)
                        else -> Color(0xFFFFEBEE)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (tx.category) {
                    "Food & Dining" -> Icons.Default.LocalPizza
                    "Shopping/Groceries" -> Icons.Default.ShoppingCart
                    "Travel & Transport" -> Icons.Default.DirectionsCar
                    "Entertainment" -> Icons.Default.Tv
                    "Rent & Housing" -> Icons.Default.Home
                    "Salary / Income" -> Icons.Default.Payments
                    "Utilities" -> Icons.Default.Lightbulb
                    "Transfer" -> Icons.Default.CompareArrows
                    else -> Icons.AutoMirrored.Filled.List
                },
                contentDescription = null,
                tint = when (tx.type) {
                    "INCOME" -> Color(0xFF2E7D32)
                    "TRANSFER" -> Color(0xFF1565C0)
                    else -> Color(0xFFC62828)
                },
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val subline = when (tx.type) {
                "TRANSFER" -> "Transfer from ${sourceAsset?.name ?: "Unknown"} ➔ ${destAsset?.name ?: "Unknown"}"
                else -> "${tx.category} • from ${sourceAsset?.name ?: "Unknown"}"
            }
            Text(
                text = "${tx.date} • $subline",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            if (tx.notes.isNotBlank()) {
                Text(
                    text = "“${tx.notes}”",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        Text(
            text = when (tx.type) {
                "INCOME" -> "+${formatLocalCurrency(tx.amount, pref.currencySymbol)}"
                "TRANSFER" -> "⇄ ${formatLocalCurrency(tx.amount, pref.currencySymbol)}"
                else -> "-${formatLocalCurrency(tx.amount, pref.currencySymbol)}"
            },
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            color = when (tx.type) {
                "INCOME" -> Color(0xFF2E7D32)
                "TRANSFER" -> Color(0xFF1565C0)
                else -> Color(0xFFC62828)
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete This Log?") },
            text = { Text("Are you sure you want to delete this log entry and adjust account balances accordingly?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// --- CREATE NEW OFFLINE ASSET ACCOUNT ---

@Composable
fun CreateAssetDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, startingBalance: Double, settlementDay: Int, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Checking/Debit") }
    var startingBalanceStr by remember { mutableStateOf("") }
    var settlementDayStr by remember { mutableStateOf("15") }
    var selectedColor by remember { mutableStateOf("#1E88E5") }

    val accountTypes = listOf("Checking/Debit", "Wallet", "Credit Card", "Savings")
    val colors = listOf("#1E88E5", "#4CAF50", "#E53935", "#8E24AA", "#F4511E", "#00ACC1", "#3949AB")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Asset Account", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("acc_name_input")
                )

                OutlinedTextField(
                    value = startingBalanceStr,
                    onValueChange = { startingBalanceStr = it },
                    label = { Text("Starting / Current Balance ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Asset Subtype", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    accountTypes.forEach { t ->
                        val isSel = t == type
                        FilterChip(
                            selected = isSel,
                            onClick = { type = t },
                            label = { Text(t, fontSize = 11.sp) }
                        )
                    }
                }

                if (type == "Credit Card") {
                    OutlinedTextField(
                        value = settlementDayStr,
                        onValueChange = { settlementDayStr = it },
                        label = { Text("Payment Settlement Day of Month") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("Choose Theme Tint", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { hex ->
                        val isSel = hex == selectedColor
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .border(
                                    if (isSel) 3.dp else 0.dp,
                                    MaterialTheme.colorScheme.onBackground,
                                    CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = startingBalanceStr.toDoubleOrNull() ?: 0.0
                    val day = settlementDayStr.toIntOrNull() ?: 15
                    if (name.isNotBlank()) {
                        onSave(name, type, bal, day, selectedColor)
                    }
                }
            ) {
                Text("Register Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// --- DOUBLE ENTRY BOOKKEEPING TRANSFERS & ENTRIES DIALOG WITH CALCULATOR INTEGRATED ---

@Composable
fun AddManualTransactionDialog(
    accounts: List<AssetAccount>,
    templates: List<FrequentTemplate>,
    pref: AppPreference,
    onDismiss: () -> Unit,
    onSave: (
        accountId: String,
        destAccountId: String?,
        type: String,
        amount: Double,
        date: String,
        name: String,
        category: String,
        notes: String,
        isRecurring: Boolean,
        recurrenceInterval: String
    ) -> Unit,
    onSaveTemplate: (
        title: String,
        accountId: String,
        destAccountId: String?,
        type: String,
        amount: Double,
        category: String,
        name: String
    ) -> Unit
) {
    var type by remember { mutableStateOf("EXPENSE") } // EXPENSE, INCOME, TRANSFER
    var amountInput by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedAccId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var selectedDestAccId by remember { mutableStateOf(accounts.getOrNull(1)?.id ?: "") }
    
    var category by remember { mutableStateOf("Food & Dining") }
    var isRecurring by remember { mutableStateOf(false) }
    var recurrenceInterval by remember { mutableStateOf("None") }

    // Bookmarking helper
    var bookmarkTitle by remember { mutableStateOf("") }
    var isBookmarkEnabled by remember { mutableStateOf(false) }

    val categoriesMap = mapOf(
        "EXPENSE" to listOf("Food & Dining", "Shopping/Groceries", "Travel & Transport", "Entertainment", "Utilities", "Rent & Housing", "Other"),
        "INCOME" to listOf("Salary / Income", "Investments", "Gifts", "Other"),
        "TRANSFER" to listOf("Transfer")
    )

    // Automatically correct category if we switch transaction type
    LaunchedEffect(type) {
        if (type == "TRANSFER") {
            category = "Transfer"
        } else {
            category = categoriesMap[type]?.firstOrNull() ?: "Other"
        }
    }

    // Evaluate live arithmetic result helper
    val evaluatedValue = remember(amountInput) {
        evaluateArithmetic(amountInput)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Activity Record", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Selector Row type
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("EXPENSE", "INCOME", "TRANSFER").forEach { t ->
                            val isSel = t == type
                            Button(
                                onClick = { type = t },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) MaterialTheme.colorScheme.primary 
                                                     else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary 
                                                   else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = t,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Calculator numeric input area
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            label = { Text("Amount / Arithmetic expression") },
                            placeholder = { Text("e.g. 15.5+4.2*2") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), // allows arithmetic operators
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Calculator Engine live evaluation:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = formatLocalCurrency(evaluatedValue, pref.currencySymbol),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Payee / Merchant / Description
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Merchant / Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Double entry source account
                item {
                    Text(
                        text = if (type == "TRANSFER") "Transfer from (Source account)" else "Withdrawal Account",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        accounts.forEach { acc ->
                            val isSel = acc.id == selectedAccId
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedAccId = acc.id },
                                label = { Text("${acc.name} (${acc.balance})", fontSize = 10.sp) }
                            )
                        }
                    }
                }

                // Double entry destination account (ONLY visible whenTRANSFER type)
                if (type == "TRANSFER") {
                    item {
                        Text(
                            text = "Deposit into (Destination account)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            accounts.filter { it.id != selectedAccId }.forEach { acc ->
                                val isSel = acc.id == selectedDestAccId
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedDestAccId = acc.id },
                                    label = { Text("${acc.name} (${acc.balance})", fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }

                // Category selection dropdown
                if (type != "TRANSFER") {
                    item {
                        Text(
                            text = "Select Category",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        var isExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { isExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Text(category)
                            }
                            DropdownMenu(
                                expanded = isExpanded,
                                onDismissRequest = { isExpanded = false }
                            ) {
                                categoriesMap[type]?.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            category = cat
                                            isExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Recurrence Engine
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Set Automated Recurrence?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = isRecurring,
                            onCheckedChange = { isRecurring = it }
                        )
                    }
                }

                if (isRecurring) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Daily", "Weekly", "Monthly").forEach { interval ->
                                val isSel = recurrenceInterval == interval
                                FilterChip(
                                    selected = isSel,
                                    onClick = { recurrenceInterval = interval },
                                    label = { Text(interval) }
                                )
                            }
                        }
                    }
                }

                // Notes Subtitle
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Audit notes (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Bookmark template generation tool
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Save as 1-Click bookmark?",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Checkbox(
                            checked = isBookmarkEnabled,
                            onCheckedChange = { isBookmarkEnabled = it }
                        )
                    }
                }

                if (isBookmarkEnabled) {
                    item {
                        OutlinedTextField(
                            value = bookmarkTitle,
                            onValueChange = { bookmarkTitle = it },
                            label = { Text("Bookmark title (e.g. ☕ Subway lunch)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val dateFormatted = sdf.format(Date())

                    if (evaluatedValue > 0 && selectedAccId.isNotBlank()) {
                        // If user wanted bookmark template, write it first
                        if (isBookmarkEnabled && bookmarkTitle.isNotBlank()) {
                            onSaveTemplate(
                                bookmarkTitle,
                                selectedAccId,
                                if (type == "TRANSFER") selectedDestAccId else null,
                                type,
                                evaluatedValue,
                                category,
                                name
                            )
                        }

                        onSave(
                            selectedAccId,
                            if (type == "TRANSFER") selectedDestAccId else null,
                            type,
                            evaluatedValue,
                            dateFormatted,
                            name,
                            category,
                            notes,
                            isRecurring,
                            if (isRecurring) recurrenceInterval else "None"
                        )
                    }
                }
            ) {
                Text("Confirm Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// --- TAB 2: ANALYTICS & CUSTOM CANVAS BAR CHART GRAPH ---

@Composable
fun AnalyticTab(
    viewModel: BudgetTrackerViewModel,
    pref: AppPreference
) {
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val currentMonthSpending by viewModel.currentMonthSpending.collectAsStateWithLifecycle()
    val currentMonthIncome by viewModel.currentMonthIncome.collectAsStateWithLifecycle()

    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var selectedBudgetToDelete by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Food & Dining", "Shopping/Groceries", "Travel & Transport", "Entertainment", "Utilities", "Rent & Housing", "Other")

    // Map spending per category for current month cutoff dynamically
    val currentMonthCutoffCalendar = viewModel.getStartOfMonthCutoffCalendar(pref.startOfMonthDay)
    val categorySpendings = remember(transactions, currentMonthCutoffCalendar) {
        transactions
            .filter { tx ->
                val txDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(tx.date) ?: Date()
                txDate.after(currentMonthCutoffCalendar.time) && tx.type == "EXPENSE"
            }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Current Month Outgoings",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatLocalCurrency(currentMonthSpending, pref.currencySymbol),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Total Month Inflow", fontSize = 11.sp)
                            Text(
                                text = formatLocalCurrency(currentMonthIncome, pref.currencySymbol),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Unspent Margins", fontSize = 11.sp)
                            val margin = currentMonthIncome - currentMonthSpending
                            Text(
                                text = formatLocalCurrency(margin, pref.currencySymbol),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (margin >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }
                }
            }
        }

        // --- CUSTOM CANVAS Segmented Segment BAR / DONUT REPRESENTATION ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Spending Breakdown Chart",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (categorySpendings.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Chart is empty. Record some expenses first!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        // DrawingSegment representer with Custom Canvas
                        val totalS = categorySpendings.values.sum()
                        val colorsPalette = listOf(
                            Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF4CAF50),
                            Color(0xFF8E24AA), Color(0xFFF4511E), Color(0xFF00ACC1),
                            Color(0xFFFDD835), Color(0xFF7E57C2)
                        )

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        ) {
                            val strokeW = 40f
                            val center = Offset(size.width / 2, size.height / 2)
                            val diameter = size.height - strokeW - 20f
                            val radius = diameter / 2
                            
                            var currentStartAngle = -90f
                            var index = 0
                            
                            categorySpendings.forEach { (_, amt) ->
                                val sweep = ((amt / totalS) * 360f).toFloat()
                                val color = colorsPalette[index % colorsPalette.size]
                                
                                drawArc(
                                    color = color,
                                    startAngle = currentStartAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = Offset(center.x - radius, center.y - radius),
                                    size = Size(diameter, diameter),
                                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                )
                                currentStartAngle += sweep
                                index++
                            }
                        }

                        // Compact Chart Legends
                        Spacer(modifier = Modifier.height(14.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            var idx = 0
                            categorySpendings.forEach { (cat, amt) ->
                                val color = colorsPalette[idx % colorsPalette.size]
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$cat (${((amt / totalS) * 100).toInt()}%)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                idx++
                            }
                        }
                    }
                }
            }
        }

        // Title: Budget targets outline
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Budget Bounds",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                val setBudgetInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = { showAddBudgetDialog = true },
                    modifier = Modifier
                        .scaleOnPress(setBudgetInteraction)
                        .testTag("set_budget_btn"),
                    interactionSource = setBudgetInteraction,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Set Target", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (budgets.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Budgets Active",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Bind monthly budget boundaries to target category spending limit rules offline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        } else {
            items(budgets) { b ->
                val spent = categorySpendings[b.category] ?: 0.0
                val ratio = if (b.limitAmount > 0) spent / b.limitAmount else 0.0
                val ratioFloat = ratio.toFloat()
                
                val progressColor = when {
                    ratio >= 1.0 -> Color(0xFFC62828)
                    ratio >= 0.8 -> Color(0xFFEF6C00)
                    else -> MaterialTheme.colorScheme.primary
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = b.category,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Spent ${formatLocalCurrency(spent, pref.currencySymbol)} of ${formatLocalCurrency(b.limitAmount, pref.currencySymbol)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { selectedBudgetToDelete = b.category }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { ratioFloat.coerceAtMost(1.0f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            strokeCap = StrokeCap.Round
                        )
                        if (ratio >= 1.0) {
                            Text(
                                text = "⚠️ Exceeded limits by ${formatLocalCurrency(spent - b.limitAmount, pref.currencySymbol)}!",
                                color = Color(0xFFC62828),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else if (ratio >= 0.8) {
                            Text(
                                text = "⚠️ Alert: 80%+ budget bounds exhaust warning!",
                                color = Color(0xFFEF6C00),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showAddBudgetDialog) {
        var limitStr by remember { mutableStateOf("") }
        var selectedCat by remember { mutableStateOf(categories.first()) }

        AlertDialog(
            onDismissRequest = { showAddBudgetDialog = false },
            title = { Text("Set Target Boundary", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select Category", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        var expanded by remember { mutableStateOf(false) }
                        Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedCat)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categories.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c) },
                                    onClick = {
                                        selectedCat = c
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = limitStr,
                        onValueChange = { limitStr = it },
                        label = { Text("Budget Limit Amount ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = limitStr.toDoubleOrNull() ?: 0.0
                        if (limit > 0) {
                            viewModel.saveBudgetLimit(selectedCat, limit)
                            showAddBudgetDialog = false
                        }
                    }
                ) {
                    Text("Save Rule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBudgetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (selectedBudgetToDelete != null) {
        AlertDialog(
            onDismissRequest = { selectedBudgetToDelete = null },
            title = { Text("Erase Budget Target?") },
            text = { Text("Delete the target configuration matching '${selectedBudgetToDelete!!}'? Historical transaction records are unaffected.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeBudgetLimit(selectedBudgetToDelete!!)
                        selectedBudgetToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedBudgetToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// FlowRow layout implementation backport
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val layoutWidth = constraints.maxWidth
        
        var currentRowHeights = 0
        var currentRowY = 0
        var currentX = 0
        
        val placements = mutableListOf<Pair<Placeable, Offset>>()

        placeables.forEach { p ->
            if (currentX + p.width > layoutWidth) {
                // Wrap row
                currentRowY += currentRowHeights
                currentRowHeights = 0
                currentX = 0
            }
            placements.add(p to Offset(currentX.toFloat(), currentRowY.toFloat()))
            currentX += p.width + 16 // spacing fallback
            currentRowHeights = maxOf(currentRowHeights, p.height)
        }
        
        layout(layoutWidth, currentRowY + currentRowHeights) {
            placements.forEach { (placeable, offset) ->
                placeable.placeRelative(offset.x.toInt(), offset.y.toInt())
            }
        }
    }
}


// --- TAB 3: SYSTEM SETTINGS, DECIMAL & OFFLINE CSV BACKUPS RESTORE ---

@Composable
fun SettingsTab(
    viewModel: BudgetTrackerViewModel,
    pref: AppPreference
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var passcodeState by remember { mutableStateOf(pref.passcode) }
    var currencyState by remember { mutableStateOf(pref.currencySymbol) }
    var startDayState by remember { mutableStateOf(pref.startOfMonthDay.toString()) }

    var showBackupDialog by remember { mutableStateOf(false) }
    var generatedBackupText by remember { mutableStateOf("") }
    var importBackupText by remember { mutableStateOf("") }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(pref) {
        passcodeState = pref.passcode
        currencyState = pref.currencySymbol
        startDayState = pref.startOfMonthDay.toString()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Preferences Configuration",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Visual Appearance Theme",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("System", 0, Icons.Default.Settings),
                            Triple("Light", 1, Icons.Default.LightMode),
                            Triple("Dark", 2, Icons.Default.DarkMode)
                        ).forEach { (label, value, icon) ->
                            val isSelected = pref.darkModeSetting == value
                            Button(
                                onClick = { viewModel.updateDarkModeSetting(value) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Currency Symbol
                    OutlinedTextField(
                        value = currencyState,
                        onValueChange = { currencyState = it },
                        label = { Text("Default Currency Sign (e.g. $, €, ₹, £)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Passcode
                    OutlinedTextField(
                        value = passcodeState,
                        onValueChange = { passcodeState = it.take(4) }, // max 4 characters
                        label = { Text("4-Digit Security PIN (leave empty to disable)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Start of month
                    OutlinedTextField(
                        value = startDayState,
                        onValueChange = { startDayState = it },
                        label = { Text("Start Day of Month (Budget metrics cutoff)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val day = startDayState.toIntOrNull() ?: 1
                            viewModel.updateSecurityPreferences(
                                passcodeState,
                                currencyState,
                                day,
                                pref.subCategoriesEnabled
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Apply System Variables", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // CSV Local Backup Restoration
        item {
            Text(
                text = "Backup & Data Recovery (Offline)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "MyMoney operates entirely offline. Back up your full checkbook data locally by exporting a structural raw copyable blueprint.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Export
                    Button(
                        onClick = {
                            viewModel.triggerLocalBackup { generated ->
                                generatedBackupText = generated
                                showBackupDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Backup (Raw Copy)", fontWeight = FontWeight.Bold)
                    }

                    // Import
                    FilledTonalButton(
                        onClick = { showRestoreDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore Past Backup String", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Dangerous reset section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Dangerous Zone",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = { showResetConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Wipe All Data & Reload Demo", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Backup Configuration Exported", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Copy this raw backup variable completely and save it securely in notes or files.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = generatedBackupText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(generatedBackupText))
                        Toast.makeText(context, "Copied backup blueprint to clipboard!", Toast.LENGTH_SHORT).show()
                        showBackupDialog = false
                    }
                ) {
                    Text("Copy Entire Code")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore From Backup blueprint", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste the raw structural configuration pattern you exported previously. This replaces all active records.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = importBackupText,
                        onValueChange = { importBackupText = it },
                        placeholder = { Text("Paste Raw blueprint text...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreLocalBackup(importBackupText) { ok ->
                            if (ok) {
                                showRestoreDialog = false
                                importBackupText = ""
                            }
                        }
                    }
                ) {
                    Text("Verify & Overwrite")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Verify Complete System Reset?") },
            text = { Text("This completely purges your offline ledger histories & variables. It will automatically reload default demo data values.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearLocalData()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Erase Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun Modifier.scaleOnPress(interactionSource: MutableInteractionSource): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scaleOnPress"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

fun Modifier.bounceClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    this
        .scaleOnPress(interactionSource)
        .clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.foundation.LocalIndication.current,
            enabled = enabled,
            onClick = onClick
        )
}
