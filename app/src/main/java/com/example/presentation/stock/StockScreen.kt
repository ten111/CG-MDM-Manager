package com.example.presentation.stock

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.local.entity.DailyMealRecordEntity
import com.example.data.local.entity.PdsShopEntity
import com.example.data.local.entity.RiceReceiptEntity
import com.example.data.local.entity.StockTransactionEntity
import com.example.data.model.CustomFoodItem
import com.example.data.model.CustomFoodItemParser
import com.example.presentation.common.AppLanguage
import com.example.presentation.common.PoshanDatePickerField
import com.example.presentation.common.PoshanTopAppBar
import com.example.presentation.common.poshanButtonColors
import com.example.presentation.common.poshanFilterChipColors
import com.example.presentation.common.poshanTextFieldColors
import com.example.presentation.viewmodel.PoshanViewModel
import com.example.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// Helper functions to save challan photos in internal app storage
fun saveChallanBitmapToInternal(context: Context, bitmap: Bitmap): String {
    return try {
        val dir = File(context.filesDir, "challan_copies").apply { mkdirs() }
        val file = File(dir, "challan_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        Uri.fromFile(file).toString()
    } catch (e: Exception) {
        ""
    }
}

fun saveChallanUriToInternal(context: Context, sourceUri: Uri): String {
    return try {
        val dir = File(context.filesDir, "challan_copies").apply { mkdirs() }
        val file = File(dir, "challan_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        Uri.fromFile(file).toString()
    } catch (e: Exception) {
        sourceUri.toString()
    }
}

// Helper model to represent commodities in the stock dashboard
data class StockItemInfo(
    val id: String,
    val nameEn: String,
    val nameHi: String,
    val unit: String,
    val icon: ImageVector,
    val normText: String,
    val normPerStudentGrams: Double,
    val currentBalance: Double,
    val isPrimaryPds: Boolean = false
)

// Helper unified receipt model
data class UnifiedReceipt(
    val id: String,
    val itemType: String,
    val itemName: String,
    val shopName: String,
    val challanNumber: String,
    val receiptDate: String,
    val quantity: Double,
    val unit: String,
    val remarks: String = "",
    val photoUri: String = "",
    val originalRiceReceipt: RiceReceiptEntity? = null,
    val originalTransaction: StockTransactionEntity? = null
)

// Helper unified usage model
data class UnifiedUsage(
    val id: String,
    val itemType: String,
    val itemName: String,
    val date: String,
    val quantityUsed: Double,
    val unit: String,
    val studentsServed: Int,
    val originalTransaction: StockTransactionEntity? = null
)

// Item matching and proper display name formatting helpers for Stock Module
fun isMatchingStockItemType(targetItemId: String, txnItemType: String): Boolean {
    if (targetItemId.equals(txnItemType, ignoreCase = true)) return true
    val t = targetItemId.lowercase().trim()
    val raw = txnItemType.lowercase().trim()
    return when {
        t.contains("ifa") || t.contains("folic") || t.contains("iron") -> {
            raw.contains("ifa") || raw.contains("folic") || raw.contains("iron")
        }
        t.contains("dudh") || t.contains("milk") -> {
            raw.contains("dudh") || raw.contains("milk")
        }
        t.contains("badi") || (t.contains("soya") && !t.contains("dudh") && !t.contains("milk")) -> {
            raw.contains("badi") || (raw.contains("soya") && !raw.contains("dudh") && !raw.contains("milk") && !raw.contains("chunk")) || raw.contains("chunk")
        }
        t.contains("deworm") || t.contains("alben") -> {
            raw.contains("deworm") || raw.contains("alben")
        }
        t == "rice" -> raw == "rice" || raw == "chawal"
        t == "pulses" || t == "dal" -> raw == "pulses" || raw == "dal" || raw == "daal"
        t == "oil" -> raw == "oil"
        t == "salt" -> raw == "salt"
        t == "veg" || t == "vegetables" -> raw == "veg" || raw == "vegetables"
        else -> false
    }
}

fun findMatchingStockItem(itemType: String, stockItems: List<StockItemInfo>): StockItemInfo? {
    // 1. Direct id match
    stockItems.firstOrNull { it.id.equals(itemType, ignoreCase = true) }?.let { return it }
    // 2. Name match
    stockItems.firstOrNull { it.nameEn.equals(itemType, ignoreCase = true) || it.nameHi.equals(itemType, ignoreCase = true) }?.let { return it }
    // 3. Known type match
    return stockItems.firstOrNull { isMatchingStockItemType(it.id, itemType) }
}

fun getProperFoodItemName(itemType: String, isHi: Boolean, matchedItem: StockItemInfo?): String {
    if (matchedItem != null) {
        val name = if (isHi) matchedItem.nameHi else matchedItem.nameEn
        return if (name.contains("Soya Dudh", ignoreCase = true)) {
            if (isHi) "सोयादूध (Soya Milk)" else "Soya Milk"
        } else if (name.contains("item_iron_folic_acid", ignoreCase = true) || (itemType.contains("folic", ignoreCase = true) && !name.contains("आयरन"))) {
            if (isHi) "आयरन फोलिक एसिड (IFA)" else "Iron Folic Acid (IFA)"
        } else {
            name
        }
    }
    val lower = itemType.lowercase().trim()
    return when {
        lower.contains("iron") || lower.contains("folic") || lower.contains("ifa") -> {
            if (isHi) "आयरन फोलिक एसिड (IFA)" else "Iron Folic Acid (IFA)"
        }
        lower.contains("dudh") || lower.contains("milk") -> {
            if (isHi) "सोयादूध (Soya Milk)" else "Soya Milk"
        }
        lower.contains("badi") || lower.contains("chunk") -> {
            if (isHi) "सोयाबड़ी (Soyabadi)" else "Soyabadi (Soya Chunks)"
        }
        lower.contains("deworm") || lower.contains("alben") -> {
            if (isHi) "कृमिनाशक (अल्बेंडाजोल)" else "Deworming (Albendazole)"
        }
        lower == "rice" || lower == "chawal" -> {
            if (isHi) "चावल (Rice)" else "Rice"
        }
        lower == "pulses" || lower == "dal" || lower == "daal" -> {
            if (isHi) "दाल (Pulses)" else "Pulses"
        }
        lower == "oil" -> {
            if (isHi) "खाद्य तेल (Oil)" else "Edible Oil"
        }
        lower == "salt" -> {
            if (isHi) "नमक व मसाले (Salt & Spices)" else "Salt & Spices"
        }
        lower == "veg" || lower == "vegetables" -> {
            if (isHi) "सब्जियां (Vegetables)" else "Fresh Vegetables"
        }
        else -> {
            itemType.removePrefix("item_").replace("_", " ").split(" ")
                .joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }
        }
    }
}

// Date formatting helper for Stock Module: Always DD/MM/YYYY or DD-MM-YYYY
private fun formatToDdMmYyyy(dateStr: String): String {
    if (dateStr.isBlank()) return ""
    return try {
        if (dateStr.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
            val parts = dateStr.split("-")
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } else if (dateStr.matches(Regex("""\d{2}-\d{2}-\d{4}"""))) {
            dateStr.replace("-", "/")
        } else if (dateStr.matches(Regex("""\d{2}/\d{2}/\d{4}"""))) {
            dateStr
        } else {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)
            if (parsed != null) SimpleDateFormat("dd/MM/yyyy", Locale.US).format(parsed) else dateStr
        }
    } catch (e: Exception) {
        dateStr
    }
}

private fun getCurrentStockDate(): String {
    return SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(
    viewModel: PoshanViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val riceStockKg by viewModel.currentRiceStockKg.collectAsState()
    val stockTransactions by viewModel.stockTransactions.collectAsState()
    val allReceipts by viewModel.allReceipts.collectAsState()
    val allMealRecords by viewModel.allMealRecords.collectAsState()
    val pdsShops by viewModel.allPdsShops.collectAsState()
    val configNorms by viewModel.configNorms.collectAsState()

    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isHi = currentLanguage == AppLanguage.HINDI

    // Parse custom food items from settings (Only active/enabled items)
    val customFoodItems = remember(configNorms) {
        val parsed = CustomFoodItemParser.parse(configNorms?.customItemsJson)
        val all = if (parsed.isNotEmpty()) parsed else CustomFoodItemParser.getDefaultCustomItems()
        all.filter { it.isEnabled }
    }

    // Build complete list of food and nutrition items
    val stockItems = remember(riceStockKg, stockTransactions, configNorms, customFoodItems) {
        val list = mutableListOf<StockItemInfo>()

        val riceNormGrams = configNorms?.primaryRiceNormGrams ?: 150.0
        val pulseNormGrams = configNorms?.pulseNormGrams ?: 30.0
        val oilNormGrams = configNorms?.oilNormGrams ?: 7.5
        val saltNormGrams = configNorms?.saltNormGrams ?: 5.0
        val vegNormGrams = configNorms?.vegetableNormGrams ?: 75.0

        // 1. Rice
        list.add(
            StockItemInfo(
                id = "RICE",
                nameEn = "Rice",
                nameHi = "चावल (Rice)",
                unit = "kg",
                icon = Icons.Default.Inventory2,
                normText = if (isHi) "${if (riceNormGrams == riceNormGrams.toLong().toDouble()) riceNormGrams.toLong() else riceNormGrams}g प्रति छात्र" else "${if (riceNormGrams == riceNormGrams.toLong().toDouble()) riceNormGrams.toLong() else riceNormGrams}g per student",
                normPerStudentGrams = riceNormGrams,
                currentBalance = riceStockKg,
                isPrimaryPds = true
            )
        )

        // 2. Pulses (दाल)
        val pulseBalance = stockTransactions
            .filter { it.itemType.equals("PULSES", ignoreCase = true) || it.itemType.equals("DAL", ignoreCase = true) }
            .maxByOrNull { it.transactionId }?.runningBalanceKg ?: 0.0
        list.add(
            StockItemInfo(
                id = "PULSES",
                nameEn = "Pulses",
                nameHi = "दाल (Pulses)",
                unit = "kg",
                icon = Icons.Default.SoupKitchen,
                normText = if (isHi) "${if (pulseNormGrams == pulseNormGrams.toLong().toDouble()) pulseNormGrams.toLong() else pulseNormGrams}g प्रति छात्र" else "${if (pulseNormGrams == pulseNormGrams.toLong().toDouble()) pulseNormGrams.toLong() else pulseNormGrams}g per student",
                normPerStudentGrams = pulseNormGrams,
                currentBalance = pulseBalance
            )
        )

        // 3. Fresh Vegetables (सब्जियां) - Placed 3rd as in norms module
        val vegBalance = stockTransactions
            .filter { it.itemType.equals("VEG", ignoreCase = true) || it.itemType.equals("VEGETABLES", ignoreCase = true) }
            .maxByOrNull { it.transactionId }?.runningBalanceKg ?: 0.0
        list.add(
            StockItemInfo(
                id = "VEG",
                nameEn = "Fresh Vegetables",
                nameHi = "सब्जियां (Vegetables)",
                unit = "kg",
                icon = Icons.Default.Eco,
                normText = if (isHi) "${if (vegNormGrams == vegNormGrams.toLong().toDouble()) vegNormGrams.toLong() else vegNormGrams}g प्रति छात्र" else "${if (vegNormGrams == vegNormGrams.toLong().toDouble()) vegNormGrams.toLong() else vegNormGrams}g per student",
                normPerStudentGrams = vegNormGrams,
                currentBalance = vegBalance
            )
        )

        // 4. Edible Cooking Oil (खाद्य तेल)
        val oilBalance = stockTransactions
            .filter { it.itemType.equals("OIL", ignoreCase = true) }
            .maxByOrNull { it.transactionId }?.runningBalanceKg ?: 0.0
        list.add(
            StockItemInfo(
                id = "OIL",
                nameEn = "Edible Oil",
                nameHi = "खाद्य तेल (Oil)",
                unit = "L",
                icon = Icons.Default.WaterDrop,
                normText = if (isHi) "${if (oilNormGrams == oilNormGrams.toLong().toDouble()) oilNormGrams.toLong() else oilNormGrams}g प्रति छात्र" else "${if (oilNormGrams == oilNormGrams.toLong().toDouble()) oilNormGrams.toLong() else oilNormGrams}g per student",
                normPerStudentGrams = oilNormGrams,
                currentBalance = oilBalance
            )
        )

        // 5. Salt & Condiments (नमक व मसाले)
        val saltBalance = stockTransactions
            .filter { it.itemType.equals("SALT", ignoreCase = true) }
            .maxByOrNull { it.transactionId }?.runningBalanceKg ?: 0.0
        list.add(
            StockItemInfo(
                id = "SALT",
                nameEn = "Salt & Spices",
                nameHi = "नमक व मसाले (Salt & Spices)",
                unit = "kg",
                icon = Icons.Default.Grain,
                normText = if (isHi) "${if (saltNormGrams == saltNormGrams.toLong().toDouble()) saltNormGrams.toLong() else saltNormGrams}g प्रति छात्र" else "${if (saltNormGrams == saltNormGrams.toLong().toDouble()) saltNormGrams.toLong() else saltNormGrams}g per student",
                normPerStudentGrams = saltNormGrams,
                currentBalance = saltBalance
            )
        )

        // 6. Custom Items
        customFoodItems.forEach { customItem ->
            val customBal = stockTransactions
                .filter { isMatchingStockItemType(customItem.id, it.itemType) || it.itemType.equals(customItem.name, ignoreCase = true) }
                .maxByOrNull { it.transactionId }?.runningBalanceKg ?: 0.0

            val customIcon = when {
                customItem.id.contains("deworm", ignoreCase = true) || customItem.name.contains("deworm", ignoreCase = true) || customItem.nameHi.contains("कृमिनाशक") || customItem.name.contains("albendazole", ignoreCase = true) -> Icons.Default.Medication
                customItem.id.contains("ifa", ignoreCase = true) || customItem.name.contains("iron", ignoreCase = true) || customItem.name.contains("folic", ignoreCase = true) || customItem.name.contains("tablet", ignoreCase = true) || customItem.nameHi.contains("आयरन") || customItem.nameHi.contains("टेबलेट") -> Icons.Default.HealthAndSafety
                customItem.id.contains("dudh", ignoreCase = true) || customItem.name.contains("milk", ignoreCase = true) || customItem.nameHi.contains("दूध") -> Icons.Default.LocalCafe
                customItem.id.contains("badi", ignoreCase = true) || customItem.id.contains("soya", ignoreCase = true) || customItem.name.contains("soya", ignoreCase = true) || customItem.nameHi.contains("सोया") || customItem.nameHi.contains("बड़ी") || customItem.nameHi.contains("बड़ी") -> Icons.Default.Grain
                customItem.name.contains("Egg", ignoreCase = true) || customItem.nameHi.contains("अंडा") -> Icons.Default.Egg
                customItem.name.contains("Fruit", ignoreCase = true) || customItem.nameHi.contains("फल") -> Icons.Default.LocalFlorist
                else -> Icons.Default.Fastfood
            }

            val properNameEn = when {
                customItem.name.contains("Soya Dudh", ignoreCase = true) -> "Soya Milk"
                customItem.id.contains("ifa") || customItem.id.contains("folic") -> "Iron Folic Acid (IFA)"
                else -> customItem.name
            }
            val properNameHi = when {
                customItem.nameHi.contains("Soya Dudh", ignoreCase = true) -> "सोयादूध (Soya Milk)"
                customItem.id.contains("ifa") || customItem.id.contains("folic") -> "आयरन फोलिक एसिड (IFA)"
                customItem.nameHi.isNotBlank() -> customItem.nameHi
                else -> properNameEn
            }

            list.add(
                StockItemInfo(
                    id = customItem.id,
                    nameEn = properNameEn,
                    nameHi = properNameHi,
                    unit = customItem.unit,
                    icon = customIcon,
                    normText = "${customItem.formattedQuantity()} ${customItem.unit} " + (if (isHi) "प्रति छात्र" else "per child"),
                    normPerStudentGrams = customItem.quantity,
                    currentBalance = customBal
                )
            )
        }

        list
    }

    // 3 Tab System: Stock (Dashboard), Receipts, Usage
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val tabTitles = listOf(
        Pair(if (isHi) "स्टॉक" else "Stock", Icons.Default.Inventory2),
        Pair(if (isHi) "प्राप्तियां" else "Receipts", Icons.Default.ReceiptLong),
        Pair(if (isHi) "खपत" else "Usage", Icons.Default.TrendingDown)
    )

    // Dialog States
    var showReceiveStockDialog by remember { mutableStateOf(false) }
    var autoLaunchCameraOnOpen by remember { mutableStateOf(false) }
    var selectedItemForAdjustment by remember { mutableStateOf<StockItemInfo?>(null) }
    var selectedItemForOpeningStock by remember { mutableStateOf<StockItemInfo?>(null) }
    var selectedItemForDelete by remember { mutableStateOf<StockItemInfo?>(null) }
    var editingReceipt by remember { mutableStateOf<RiceReceiptEntity?>(null) }
    var editingTransaction by remember { mutableStateOf<StockTransactionEntity?>(null) }
    var deletingReceiptId by remember { mutableStateOf<String?>(null) }
    var deletingTransaction by remember { mutableStateOf<StockTransactionEntity?>(null) }

    Scaffold(
        topBar = {
            PoshanTopAppBar(
                title = if (isHi) "स्टॉक प्रबंधन" else "Stock",
                subtitle = if (isHi) "PDS आपूर्ति, स्टॉक डैशबोर्ड व दैनिक उपभोग" else "PDS Supply, Stock Dashboard & Usage",
                currentLanguage = currentLanguage,
                onLanguageToggle = { viewModel.toggleLanguage() },
                onSyncClick = {
                    viewModel.recalculateStockLedger("RICE") {
                        Toast.makeText(
                            context,
                            if (isHi) "✓ स्टॉक शेष की पुनर्गणना पूर्ण हुई" else "✓ Stock balance recalculated",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        },
        bottomBar = {
            // Sticky Bottom Action Bar: + Receive Stock
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Button(
                        onClick = {
                            autoLaunchCameraOnOpen = true
                            showReceiveStockDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.08f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHi) "कूपन स्कैन (OCR)" else "Scan Coupon",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = {
                            autoLaunchCameraOnOpen = false
                            showReceiveStockDialog = true
                        },
                        colors = poshanButtonColors(BluePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHi) "स्टॉक दर्ज करें" else "Receive Stock",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
        ) {
            // 3-TAB BAR
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.White,
                contentColor = BluePrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = BluePrimary,
                        height = 3.dp
                    )
                },
                divider = {
                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                }
            ) {
                tabTitles.forEachIndexed { index, pair ->
                    val selected = pagerState.currentPage == index
                    Tab(
                        selected = selected,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = pair.second,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (selected) BluePrimary else Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = pair.first,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) BluePrimary else Color(0xFF64748B),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    )
                }
            }

            // TAB CONTENT - SWIPABLE WITH HORIZONTAL PAGER
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> {
                        // TAB 1: STOCK DASHBOARD (Commodities overview cards)
                        StockDashboardTab(
                            stockItems = stockItems,
                            transactions = stockTransactions,
                            allReceipts = allReceipts,
                            allMealRecords = allMealRecords,
                            isHi = isHi,
                            onAdjustmentClick = { item -> selectedItemForAdjustment = item },
                            onDeleteClick = { item -> selectedItemForDelete = item },
                            onOpeningStockClick = { item -> selectedItemForOpeningStock = item }
                        )
                    }
                    1 -> {
                        // TAB 2: RECEIPTS (Stock receipts list)
                        StockReceiptsTab(
                            allReceipts = allReceipts,
                            stockTransactions = stockTransactions,
                            stockItems = stockItems,
                            isHi = isHi,
                            onAddReceipt = {
                                autoLaunchCameraOnOpen = false
                                showReceiveStockDialog = true
                            },
                            onScanCoupon = {
                                autoLaunchCameraOnOpen = true
                                showReceiveStockDialog = true
                            },
                            onEditRiceReceipt = { rcp -> editingReceipt = rcp },
                            onEditTransaction = { txn -> editingTransaction = txn },
                            onDeleteRiceReceipt = { id -> deletingReceiptId = id },
                            onDeleteTransaction = { txn -> deletingTransaction = txn }
                        )
                    }
                    2 -> {
                        // TAB 3: USAGE (Daily usage and consumption records)
                        StockUsageTab(
                            mealRecords = allMealRecords,
                            stockTransactions = stockTransactions,
                            stockItems = stockItems,
                            isHi = isHi,
                            onEditTransaction = { txn -> editingTransaction = txn },
                            onDeleteTransaction = { txn -> deletingTransaction = txn }
                        )
                    }
                }
            }
        }
    }

    // ----------------- DIALOGS -----------------

    // 1. Receive Stock Dialog
    if (showReceiveStockDialog) {
        ReceiveStockDialog(
            stockItems = stockItems,
            initialItemId = "RICE",
            pdsShops = pdsShops,
            isHi = isHi,
            autoLaunchCamera = autoLaunchCameraOnOpen,
            onDismiss = {
                showReceiveStockDialog = false
                autoLaunchCameraOnOpen = false
            },
            onSave = { receipt, selectedItemId ->
                showReceiveStockDialog = false
                autoLaunchCameraOnOpen = false
                if (selectedItemId == "RICE") {
                    viewModel.saveRiceReceipt(receipt) {
                        Toast.makeText(context, if (isHi) "✓ चावल प्राप्ति दर्ज हुई" else "✓ Rice receipt recorded", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val item = stockItems.firstOrNull { it.id == selectedItemId } ?: stockItems.first()
                    val photoTag = if (receipt.photoUri.isNotBlank()) " [PHOTO:${receipt.photoUri}]" else ""
                    viewModel.addStockTransaction(
                        StockTransactionEntity(
                            transactionDate = receipt.receiptDate,
                            itemType = item.id,
                            transactionType = "RECEIPT",
                            quantityKg = receipt.quantityKg,
                            runningBalanceKg = item.currentBalance + receipt.quantityKg,
                            referenceId = receipt.receiptId,
                            description = "${item.nameEn} प्राप्त (Challan #${receipt.challanNumber}) - ${receipt.pdsShopName}$photoTag"
                        )
                    ) {
                        Toast.makeText(context, if (isHi) "✓ ${item.nameHi} स्टॉक दर्ज हुआ" else "✓ ${item.nameEn} stock recorded", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // 2. Stock Adjustment Dialog
    selectedItemForAdjustment?.let { item ->
        StockAdjustmentDialog(
            selectedItem = item,
            isHi = isHi,
            onDismiss = { selectedItemForAdjustment = null },
            onSave = { qtyChange, reason ->
                selectedItemForAdjustment = null
                viewModel.addStockAdjustment(
                    date = getCurrentStockDate(),
                    quantityChangeKg = qtyChange,
                    reason = reason,
                    itemType = item.id
                ) {
                    Toast.makeText(
                        context,
                        if (isHi) "✓ ${item.nameHi} स्टॉक समायोजित हुआ" else "✓ ${item.nameEn} stock adjusted",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    // 3. Set Opening Stock Dialog
    selectedItemForOpeningStock?.let { item ->
        SetOpeningStockDialog(
            selectedItem = item,
            isHi = isHi,
            onDismiss = { selectedItemForOpeningStock = null },
            onSave = { date, qty, remarks ->
                selectedItemForOpeningStock = null
                viewModel.setOpeningStock(
                    itemType = item.id,
                    date = date,
                    openingKg = qty,
                    remarks = remarks
                ) {
                    Toast.makeText(
                        context,
                        if (isHi) "✓ ${item.nameHi} प्रारंभिक स्टॉक सेट हुआ" else "✓ Opening stock set for ${item.nameEn}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    // 4. Delete/Reset Item Confirmation Dialog
    selectedItemForDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedItemForDelete = null },
            title = {
                Text(
                    text = if (isHi) "स्टॉक रीसेट करें / हटाएं" else "Reset / Clear Stock",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                )
            },
            text = {
                Text(
                    text = if (isHi)
                        "क्या आप '${item.nameHi}' का स्टॉक विवरण रीसेट / हटाना चाहते हैं? इसके लिए स्टॉक समायोजन द्वारा बैलेंस 0 किया जाएगा।"
                    else
                        "Do you want to reset stock balance for '${item.nameEn}'? This will record an adjustment to zero out the remaining balance.",
                    color = Color(0xFF334155)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDeduct = -item.currentBalance
                        selectedItemForDelete = null
                        if (item.currentBalance != 0.0) {
                            viewModel.addStockAdjustment(
                                date = getCurrentStockDate(),
                                quantityChangeKg = toDeduct,
                                reason = "Manual reset/clear stock to zero",
                                itemType = item.id
                            ) {
                                Toast.makeText(context, if (isHi) "स्टॉक रीसेट हुआ" else "Stock balance reset", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text(if (isHi) "रीसेट करें (Reset)" else "Reset Stock", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedItemForDelete = null }) {
                    Text(if (isHi) "रद्द करें" else "Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }

    // 5. Edit Rice Receipt Dialog
    editingReceipt?.let { rcp ->
        EditRiceReceiptDialog(
            receipt = rcp,
            pdsShops = pdsShops,
            isHi = isHi,
            onDismiss = { editingReceipt = null },
            onSave = { updated ->
                editingReceipt = null
                viewModel.editRiceReceipt(updated) {
                    Toast.makeText(context, if (isHi) "✓ रसीद अपडेट हुई" else "✓ Receipt updated", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // 6. Edit Transaction Dialog
    editingTransaction?.let { txn ->
        EditStockTransactionDialog(
            transaction = txn,
            stockItems = stockItems,
            isHi = isHi,
            onDismiss = { editingTransaction = null },
            onSave = { updated ->
                editingTransaction = null
                viewModel.editStockTransaction(updated) {
                    Toast.makeText(context, if (isHi) "✓ प्रविष्टि अपडेट हुई" else "✓ Entry updated", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // 7. Delete Rice Receipt Dialog
    deletingReceiptId?.let { id ->
        AlertDialog(
            onDismissRequest = { deletingReceiptId = null },
            title = {
                Text(
                    text = if (isHi) "चावल रसीद हटाएं?" else "Delete Receipt?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                )
            },
            text = {
                Text(
                    text = if (isHi)
                        "क्या आप इस PDS चावल रसीद को हटाना चाहते हैं? लेजर बैलेंस स्वतः घट जाएगा।"
                    else
                        "Are you sure you want to delete this receipt? Stock balance will be updated automatically.",
                    color = Color(0xFF334155)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        deletingReceiptId = null
                        viewModel.deleteRiceReceipt(id) {
                            Toast.makeText(context, if (isHi) "रसीद हटाई गई" else "Receipt deleted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text(if (isHi) "हटाएं" else "Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingReceiptId = null }) {
                    Text(if (isHi) "रद्द करें" else "Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }

    // 8. Delete Transaction Dialog
    deletingTransaction?.let { txn ->
        AlertDialog(
            onDismissRequest = { deletingTransaction = null },
            title = {
                Text(
                    text = if (isHi) "प्रविष्टि हटाएं?" else "Delete Entry?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                )
            },
            text = {
                Text(
                    text = if (isHi)
                        "क्या आप इस स्टॉक प्रविष्टि को हटाना चाहते हैं? (${txn.description})"
                    else
                        "Are you sure you want to delete this stock entry? (${txn.description})",
                    color = Color(0xFF334155)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        deletingTransaction = null
                        viewModel.deleteStockTransaction(txn.transactionId) {
                            Toast.makeText(context, if (isHi) "प्रविष्टि हटाई गई" else "Entry deleted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text(if (isHi) "हटाएं" else "Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTransaction = null }) {
                    Text(if (isHi) "रद्द करें" else "Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }
}

// ----------------- TAB 1: STOCK DASHBOARD -----------------

@Composable
private fun StockDashboardTab(
    stockItems: List<StockItemInfo>,
    transactions: List<StockTransactionEntity>,
    allReceipts: List<RiceReceiptEntity>,
    allMealRecords: List<DailyMealRecordEntity>,
    isHi: Boolean,
    onAdjustmentClick: (StockItemInfo) -> Unit,
    onDeleteClick: (StockItemInfo) -> Unit,
    onOpeningStockClick: (StockItemInfo) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(stockItems, key = { it.id }) { item ->
            // Calculate stats for this item
            val itemTxns = remember(transactions, item.id) {
                transactions.filter { isMatchingStockItemType(item.id, it.itemType) || it.itemType.equals(item.nameEn, ignoreCase = true) }
            }

            val totalStock = remember(itemTxns, allReceipts, item.id) {
                if (item.id == "RICE") {
                    val rcpSum = allReceipts.sumOf { it.quantityKg }
                    val opAndAdj = itemTxns.filter { it.transactionType == "OPENING_BALANCE" || (it.transactionType == "ADJUSTMENT" && it.quantityKg > 0) }
                        .sumOf { it.quantityKg }
                    if (rcpSum + opAndAdj > 0) rcpSum + opAndAdj else item.currentBalance
                } else {
                    val inFlow = itemTxns.filter { it.transactionType == "RECEIPT" || it.transactionType == "OPENING_BALANCE" || (it.transactionType == "ADJUSTMENT" && it.quantityKg > 0) }
                        .sumOf { it.quantityKg }
                    if (inFlow > 0) inFlow else item.currentBalance
                }
            }

            val totalUsed = remember(itemTxns, allMealRecords, item.id, item.normPerStudentGrams, item.unit) {
                // Calculate total consumption from meal records dynamically based on the configured rate per student
                val isStandardCommodity = item.id in listOf("RICE", "PULSES", "VEG", "OIL", "SALT")
                val mealUsed = allMealRecords.filter { it.mealServed && it.studentsServed > 0 }.sumOf { record ->
                    if (isStandardCommodity) {
                        (record.studentsServed * item.normPerStudentGrams) / 1000.0
                    } else {
                        // Custom item: usage occurs only when checked for that day
                        val usedSet = com.example.data.model.CustomFoodItemParser.parseUsedItemIds(record.customItemsUsedJson)
                        val isUsedOnDate = if (record.customItemsUsedJson.isNotBlank()) {
                            usedSet.contains(item.id)
                        } else {
                            false
                        }
                        if (isUsedOnDate) {
                            if (item.unit.equals("pcs", ignoreCase = true) || item.unit.equals("nos", ignoreCase = true) || item.unit.equals("duzzen", ignoreCase = true) || item.unit.equals("dozen", ignoreCase = true) || item.unit.equals("दर्जन", ignoreCase = true) || item.unit.equals("pkt", ignoreCase = true) || item.unit.equals("unit", ignoreCase = true)) {
                                record.studentsServed * item.normPerStudentGrams
                            } else if (item.unit.equals("g", ignoreCase = true) || item.unit.equals("ml", ignoreCase = true)) {
                                (record.studentsServed * item.normPerStudentGrams) / 1000.0
                            } else {
                                record.studentsServed * item.normPerStudentGrams
                            }
                        } else {
                            0.0
                        }
                    }
                }
                // Manual/extra usage transactions not tied to daily meal reports
                val directNonMealUsed = itemTxns.filter {
                    it.transactionType == "USAGE" && !it.referenceId.matches(Regex("""\d{4}-\d{2}-\d{2}"""))
                }.sumOf { Math.abs(it.quantityKg) }
                val directAdjUsed = itemTxns.filter { it.transactionType == "ADJUSTMENT" && it.quantityKg < 0 }
                    .sumOf { Math.abs(it.quantityKg) }

                val calculatedTotalUsed = mealUsed + directNonMealUsed + directAdjUsed
                if (calculatedTotalUsed > 0) calculatedTotalUsed else {
                    val fallbackDirectUsed = itemTxns.filter { it.transactionType == "USAGE" || (it.transactionType == "ADJUSTMENT" && it.quantityKg < 0) }
                        .sumOf { Math.abs(it.quantityKg) }
                    fallbackDirectUsed
                }
            }

            val remaining = (totalStock - totalUsed).coerceAtLeast(0.0)

            // Status: Good, Low, Critical
            val statusColor = when {
                remaining >= (if (item.id in listOf("OIL", "SALT")) 10.0 else 50.0) -> Color(0xFF16A34A)
                remaining > 0.0 -> Color(0xFFD97706)
                else -> Color(0xFFDC2626)
            }
            val statusText = when {
                remaining >= (if (item.id in listOf("OIL", "SALT")) 10.0 else 50.0) -> "✔ Good"
                remaining > 0.0 -> if (isHi) "कम (Low)" else "Low"
                else -> if (isHi) "अति कम" else "Critical"
            }

            val isProminentStaple = item.id in listOf("RICE", "PULSES", "VEG")

            if (isProminentStaple) {
                // High-visibility, prominent card for Rice, Pulses, and Vegetables
                val cardBg = when (item.id) {
                    "RICE" -> Color(0xFFF0F9FF)
                    "PULSES" -> Color(0xFFFFFBEB)
                    else -> Color(0xFFF0FDF4)
                }
                val borderColor = when (item.id) {
                    "RICE" -> Color(0xFFBAE6FD)
                    "PULSES" -> Color(0xFFFDE68A)
                    else -> Color(0xFFBBF7D0)
                }
                val iconTint = when (item.id) {
                    "RICE" -> Color(0xFF0284C7)
                    "PULSES" -> Color(0xFFD97706)
                    else -> Color(0xFF16A34A)
                }
                val iconBg = when (item.id) {
                    "RICE" -> Color(0xFFE0F2FE)
                    "PULSES" -> Color(0xFFFEF3C7)
                    else -> Color(0xFFDCFCE7)
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.2.dp, borderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        // Header: Icon, Name, Norm, Status & Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(iconBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = iconTint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isHi) item.nameHi else item.nameEn,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.normText.isNotBlank()) {
                                        Text(
                                            text = item.normText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B),
                                            fontSize = 10.5.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = statusColor.copy(alpha = 0.15f),
                                    border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor,
                                        fontSize = 10.5.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Actions
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFEF3C7),
                                    border = BorderStroke(0.5.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier.clickable { onAdjustmentClick(item) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = "Adjust",
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isHi) "समायोजन" else "Adjust",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD97706)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFEE2E2),
                                    border = BorderStroke(0.5.dp, Color(0xFFFECACA)),
                                    modifier = Modifier.clickable { onDeleteClick(item) }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Large Balance Display & Stats Strip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (isHi) "वर्तमान शेष (Balance)" else "Current Balance",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = String.format(Locale.US, "%.2f", remaining),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = iconTint,
                                        fontSize = 22.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = item.unit,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }

                            // Total Inflow and Used badges
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.95f),
                                border = BorderStroke(1.dp, borderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = if (isHi) "कुल आवक" else "Total Inflow",
                                            fontSize = 9.5.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = "${String.format(Locale.US, "%.2f", totalStock)} ${item.unit}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BluePrimary
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(20.dp)
                                            .background(Color(0xFFCBD5E1))
                                    )

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = if (isHi) "कुल खपत" else "Total Used",
                                            fontSize = 9.5.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = "${String.format(Locale.US, "%.2f", totalUsed)} ${item.unit}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEA580C)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Compact Commodity Card - for Oil, Salt, and custom items
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        // Row 1: Item Name, Icon, Status Pill & Compact Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEFF6FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = BluePrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHi) item.nameHi else item.nameEn,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    fontSize = 13.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = statusColor.copy(alpha = 0.15f),
                                    border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            // Compact Inline Action Buttons
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Adjustment Action Button
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFEF3C7),
                                    border = BorderStroke(0.5.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier.clickable { onAdjustmentClick(item) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = "Adjust",
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (isHi) "समायोजन" else "Adjust",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD97706)
                                        )
                                    }
                                }

                                // Delete / Clear Action Button
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFEE2E2),
                                    border = BorderStroke(0.5.dp, Color(0xFFFECACA)),
                                    modifier = Modifier.clickable { onDeleteClick(item) }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Row 2: Subtle Accent Divider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFE2E8F0))
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        // Row 3: Remaining Balance on Left, Total & Used metrics on Right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Remaining Balance
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier.padding(start = 2.dp)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.2f", remaining),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BluePrimary,
                                    fontSize = 17.5.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${item.unit} ${if (isHi) "शेष" else "Rem."}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(bottom = 1.dp)
                                )
                            }

                            // Compact Stats Surface: Total and Used
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Total
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${if (isHi) "कुल" else "Tot"}: ",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.2f", totalStock),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BluePrimary
                                        )
                                    }

                                    // Divider
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(12.dp)
                                            .background(Color(0xFFCBD5E1))
                                    )

                                    // Used
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${if (isHi) "खपत" else "Used"}: ",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.2f", totalUsed),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEA580C)
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
}

// ----------------- TAB 2: RECEIPTS -----------------

@Composable
private fun StockReceiptsTab(
    allReceipts: List<RiceReceiptEntity>,
    stockTransactions: List<StockTransactionEntity>,
    stockItems: List<StockItemInfo>,
    isHi: Boolean,
    onAddReceipt: () -> Unit,
    onScanCoupon: () -> Unit = {},
    onEditRiceReceipt: (RiceReceiptEntity) -> Unit,
    onEditTransaction: (StockTransactionEntity) -> Unit,
    onDeleteRiceReceipt: (String) -> Unit,
    onDeleteTransaction: (StockTransactionEntity) -> Unit
) {
    // Build unified receipts list
    val unifiedReceipts = remember(allReceipts, stockTransactions, stockItems) {
        val list = mutableListOf<UnifiedReceipt>()

        // 1. All Rice Receipts
        allReceipts.forEach { rcp ->
            list.add(
                UnifiedReceipt(
                    id = "RICE-" + rcp.receiptId,
                    itemType = "RICE",
                    itemName = if (isHi) "चावल (Rice)" else "Rice",
                    shopName = rcp.pdsShopName.ifEmpty { "PDS BODLA" },
                    challanNumber = rcp.challanNumber.ifEmpty { "RCS70207260025" },
                    receiptDate = rcp.receiptDate,
                    quantity = rcp.quantityKg,
                    unit = "kg",
                    remarks = rcp.remarks,
                    photoUri = rcp.photoUri,
                    originalRiceReceipt = rcp
                )
            )
        }

        // 2. Non-rice receipts or opening balance transactions from stockTransactions
        stockTransactions.filter { (it.transactionType == "RECEIPT" && it.itemType != "RICE") || it.transactionType == "OPENING_BALANCE" }
            .forEach { txn ->
                val matched = findMatchingStockItem(txn.itemType, stockItems)
                val displayName = getProperFoodItemName(txn.itemType, isHi, matched)
                val unitStr = matched?.unit ?: when {
                    isMatchingStockItemType("item_ifa", txn.itemType) || isMatchingStockItemType("item_deworming", txn.itemType) -> "Pcs"
                    isMatchingStockItemType("item_soyadudh", txn.itemType) -> "Ltr"
                    else -> "kg"
                }
                val txnPhoto = if (txn.description.contains("[PHOTO:")) {
                    txn.description.substringAfter("[PHOTO:").substringBefore("]")
                } else ""
                val cleanDesc = if (txn.description.contains(" [PHOTO:")) {
                    txn.description.substringBefore(" [PHOTO:")
                } else txn.description
                list.add(
                    UnifiedReceipt(
                        id = "TXN-" + txn.transactionId,
                        itemType = txn.itemType,
                        itemName = displayName,
                        shopName = if (txn.transactionType == "OPENING_BALANCE") "Opening Stock" else cleanDesc.substringAfter(" - ").ifEmpty { "Market Purchase / Supplier" },
                        challanNumber = if (txn.referenceId.isNotBlank()) txn.referenceId else "OP-${txn.transactionId}",
                        receiptDate = txn.transactionDate,
                        quantity = txn.quantityKg,
                        unit = unitStr,
                        remarks = cleanDesc,
                        photoUri = txnPhoto,
                        originalTransaction = txn
                    )
                )
            }

        // Sort by date descending
        list.sortedByDescending { it.receiptDate }
    }

    var previewChallanPhotoUri by remember { mutableStateOf<String?>(null) }

    if (unifiedReceipts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isHi) "कोई स्टॉक रसीद दर्ज नहीं है।" else "No stock receipts recorded.",
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onScanCoupon,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DocumentScanner, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHi) "कूपन स्कैन (OCR)" else "Scan Coupon (OCR)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Button(
                        onClick = onAddReceipt,
                        colors = poshanButtonColors(BluePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHi) "रसीद दर्ज करें" else "Receive Stock",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(unifiedReceipts, key = { it.id }) { rcp ->
                // Receipt Card (Matches Screenshot 2)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Blue Circular Badge with Down Arrow Icon
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(BluePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Middle: Item details (Item Name, From, Bill, Date)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rcp.itemName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "From: ${rcp.shopName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF475569),
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Bill: ${rcp.challanNumber}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatToDdMmYyyy(rcp.receiptDate),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp
                                )
                            }

                            // Right: Quantity in blue + unit
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "+${if (rcp.quantity % 1.0 == 0.0) rcp.quantity.toInt().toString() else String.format(Locale.US, "%.1f", rcp.quantity)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BluePrimary,
                                    fontSize = 22.sp
                                )
                                Text(
                                    text = rcp.unit,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Challan photo indicator if photo attached
                        if (rcp.photoUri.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = Color(0xFFF0FDF4),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { previewChallanPhotoUri = rcp.photoUri }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    ) {
                                        AsyncImage(
                                            model = rcp.photoUri,
                                            contentDescription = "Challan Photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isHi) "चालान / कूपन प्रति संलग्न" else "Challan / Coupon Copy Attached",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF166534),
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = if (isHi) "फोटो देखने के लिए टैप करें" else "Tap to view full photo",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF15803D),
                                            fontSize = 11.sp
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ZoomIn,
                                        contentDescription = null,
                                        tint = Color(0xFF166534),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Divider & Actions Row (Edit & Delete)
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    if (rcp.originalRiceReceipt != null) {
                                        onEditRiceReceipt(rcp.originalRiceReceipt)
                                    } else if (rcp.originalTransaction != null) {
                                        onEditTransaction(rcp.originalTransaction)
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isHi) "संपादित करें" else "Edit",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            TextButton(
                                onClick = {
                                    if (rcp.originalRiceReceipt != null) {
                                        onDeleteRiceReceipt(rcp.originalRiceReceipt.receiptId)
                                    } else if (rcp.originalTransaction != null) {
                                        onDeleteTransaction(rcp.originalTransaction)
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isHi) "हटाएं" else "Delete",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    previewChallanPhotoUri?.let { uri ->
        ChallanPhotoViewerDialog(
            photoUri = uri,
            isHi = isHi,
            onDismiss = { previewChallanPhotoUri = null }
        )
    }
}

// ----------------- TAB 3: USAGE -----------------

@Composable
private fun StockUsageTab(
    mealRecords: List<DailyMealRecordEntity>,
    stockTransactions: List<StockTransactionEntity>,
    stockItems: List<StockItemInfo>,
    isHi: Boolean,
    onEditTransaction: (StockTransactionEntity) -> Unit,
    onDeleteTransaction: (StockTransactionEntity) -> Unit
) {
    // Build unified usage records
    val usageEntries = remember(mealRecords, stockTransactions, stockItems) {
        val list = mutableListOf<UnifiedUsage>()

        // 1. From daily meal reports
        val servedDays = mealRecords.filter { it.mealServed && it.studentsServed > 0 }
        servedDays.forEach { record ->
            // Add entries for primary commodities served
            // Rice
            val riceItem = stockItems.firstOrNull { it.id.equals("RICE", ignoreCase = true) }
            val riceNormGrams = riceItem?.normPerStudentGrams ?: 150.0
            val riceNormKg = (record.studentsServed * riceNormGrams) / 1000.0
            list.add(
                UnifiedUsage(
                    id = "MEAL-RICE-${record.date}",
                    itemType = "RICE",
                    itemName = if (isHi) (riceItem?.nameHi ?: "चावल (Rice)") else (riceItem?.nameEn ?: "Rice"),
                    date = record.date,
                    quantityUsed = riceNormKg,
                    unit = riceItem?.unit ?: "kg",
                    studentsServed = record.studentsServed
                )
            )

            // Pulses
            val pulseItem = stockItems.firstOrNull { it.id.equals("PULSES", ignoreCase = true) }
            val pulseNormGrams = pulseItem?.normPerStudentGrams ?: 30.0
            val pulseNormKg = (record.studentsServed * pulseNormGrams) / 1000.0
            list.add(
                UnifiedUsage(
                    id = "MEAL-PULSES-${record.date}",
                    itemType = "PULSES",
                    itemName = if (isHi) (pulseItem?.nameHi ?: "दाल (Pulses)") else (pulseItem?.nameEn ?: "Pulses"),
                    date = record.date,
                    quantityUsed = pulseNormKg,
                    unit = pulseItem?.unit ?: "kg",
                    studentsServed = record.studentsServed
                )
            )

            // Vegetables
            val vegItem = stockItems.firstOrNull { it.id.equals("VEG", ignoreCase = true) }
            val vegNormGrams = vegItem?.normPerStudentGrams ?: 75.0
            val vegNormKg = (record.studentsServed * vegNormGrams) / 1000.0
            list.add(
                UnifiedUsage(
                    id = "MEAL-VEG-${record.date}",
                    itemType = "VEG",
                    itemName = if (isHi) (vegItem?.nameHi ?: "हरी सब्जियां (Vegetables)") else (vegItem?.nameEn ?: "Vegetables"),
                    date = record.date,
                    quantityUsed = vegNormKg,
                    unit = vegItem?.unit ?: "kg",
                    studentsServed = record.studentsServed
                )
            )

            // Custom Items checked/served for this date
            if (record.customItemsUsedJson.isNotBlank()) {
                val usedIds = com.example.data.model.CustomFoodItemParser.parseUsedItemIds(record.customItemsUsedJson)
                usedIds.forEach { customId ->
                    val customStockItem = stockItems.firstOrNull { it.id.equals(customId, ignoreCase = true) }
                    if (customStockItem != null) {
                        val qtyUsed = if (customStockItem.unit.equals("pcs", ignoreCase = true) || customStockItem.unit.equals("nos", ignoreCase = true) || customStockItem.unit.equals("duzzen", ignoreCase = true) || customStockItem.unit.equals("dozen", ignoreCase = true) || customStockItem.unit.equals("दर्जन", ignoreCase = true) || customStockItem.unit.equals("pkt", ignoreCase = true) || customStockItem.unit.equals("unit", ignoreCase = true)) {
                            record.studentsServed * customStockItem.normPerStudentGrams
                        } else if (customStockItem.unit.equals("g", ignoreCase = true) || customStockItem.unit.equals("ml", ignoreCase = true)) {
                            (record.studentsServed * customStockItem.normPerStudentGrams) / 1000.0
                        } else {
                            record.studentsServed * customStockItem.normPerStudentGrams
                        }
                        list.add(
                            UnifiedUsage(
                                id = "MEAL-$customId-${record.date}",
                                itemType = customId,
                                itemName = if (isHi) customStockItem.nameHi else customStockItem.nameEn,
                                date = record.date,
                                quantityUsed = qtyUsed,
                                unit = customStockItem.unit,
                                studentsServed = record.studentsServed
                            )
                        )
                    }
                }
            }
        }

        // 2. Add any direct manual usages from stockTransactions (if not duplicated)
        stockTransactions.filter { it.transactionType == "USAGE" }.forEach { txn ->
            val alreadyPresent = list.any { it.date == txn.transactionDate && isMatchingStockItemType(it.itemType, txn.itemType) }
            if (!alreadyPresent) {
                val matched = findMatchingStockItem(txn.itemType, stockItems)
                val displayName = getProperFoodItemName(txn.itemType, isHi, matched)
                val studentCount = Regex("""\d+""").find(txn.description)?.value?.toIntOrNull() ?: 0
                val unitStr = matched?.unit ?: when {
                    isMatchingStockItemType("item_ifa", txn.itemType) || isMatchingStockItemType("item_deworming", txn.itemType) -> "Pcs"
                    isMatchingStockItemType("item_soyadudh", txn.itemType) -> "Ltr"
                    else -> "kg"
                }
                list.add(
                    UnifiedUsage(
                        id = "TXN-USAGE-${txn.transactionId}",
                        itemType = txn.itemType,
                        itemName = displayName,
                        date = txn.transactionDate,
                        quantityUsed = Math.abs(txn.quantityKg),
                        unit = unitStr,
                        studentsServed = studentCount,
                        originalTransaction = txn
                    )
                )
            }
        }

        // Sort by date descending
        list.sortedByDescending { it.date }
    }

    if (usageEntries.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(
                    imageVector = Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isHi) "कोई दैनिक भोजन खपत दर्ज नहीं है।" else "No daily usage records found.",
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    } else {
        // Group usage items by date to combine same day items together with a distinct blue border
        val groupedUsageByDate = remember(usageEntries) {
            usageEntries.groupBy { it.date }.toList()
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(groupedUsageByDate, key = { it.first }) { (date, dayItems) ->
                // Combined Day Container with Blue Border to visibly differentiate same-day expenses
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(2.dp, BluePrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Date Header at top of combined group
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = BluePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = formatToDdMmYyyy(date),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    fontSize = 15.sp
                                )
                            }

                            // Badge displaying number of items/commodities consumed on this date
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BluePrimaryContainer
                            ) {
                                Text(
                                    text = if (isHi) "${dayItems.size} सामग्री उपभोग" else "${dayItems.size} items used",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Individual items/commodities consumed on this day
                        dayItems.forEach { usage ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(1.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Top Row: Date on left, Item Name on right
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formatToDdMmYyyy(usage.date),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF64748B),
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = usage.itemName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            fontSize = 15.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Content Row: 2 Warm Cream/Yellow Boxes (Qty Used | Students)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Left Box: Qty Used
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFFEF9C3),
                                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp, horizontal = 10.dp)
                                            ) {
                                                Text(
                                                    text = if (isHi) "उपयोग मात्रा (Qty Used)" else "Qty Used",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF78716C),
                                                    fontSize = 11.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${String.format(Locale.US, "%.3f", usage.quantityUsed)} ${usage.unit}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB45309),
                                                    fontSize = 16.sp
                                                )
                                            }
                                        }

                                        // Right Box: Students
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFFEF9C3),
                                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp, horizontal = 10.dp)
                                            ) {
                                                Text(
                                                    text = if (isHi) "लाभार्थी छात्र (Students)" else "Students",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF78716C),
                                                    fontSize = 11.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (usage.studentsServed > 0) usage.studentsServed.toString() else "-",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB45309),
                                                    fontSize = 16.sp
                                                )
                                            }
                                        }
                                    }

                                    // If it is a manual transaction, allow editing/deletion
                                    if (usage.originalTransaction != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                                        Spacer(modifier = Modifier.height(2.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = { onEditTransaction(usage.originalTransaction) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (isHi) "संपादित करें" else "Edit", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2563EB))
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            TextButton(
                                                onClick = { onDeleteTransaction(usage.originalTransaction) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (isHi) "हटाएं" else "Delete", style = MaterialTheme.typography.labelSmall, color = Color(0xFFDC2626))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------- DIALOG IMPLEMENTATIONS -----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiveStockDialog(
    stockItems: List<StockItemInfo>,
    initialItemId: String,
    pdsShops: List<PdsShopEntity>,
    isHi: Boolean,
    autoLaunchCamera: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (RiceReceiptEntity, String) -> Unit
) {
    val today = getCurrentStockDate()
    var selectedItemType by remember { mutableStateOf(initialItemId) }
    var receiptDate by remember { mutableStateOf(today) }
    var quantityText by remember { mutableStateOf("") }
    var challanNumber by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf("") }

    // OCR States
    var isOcrProcessing by remember { mutableStateOf(false) }
    var ocrResult by remember { mutableStateOf<CouponOcrResult?>(null) }
    var ocrStatusMessage by remember { mutableStateOf<String?>(null) }
    var previewPhotoUri by remember { mutableStateOf<String?>(null) }

    val defaultShop = pdsShops.firstOrNull()
    var selectedShopName by remember {
        mutableStateOf(
            if (defaultShop != null && defaultShop.shopName.isNotBlank() && !defaultShop.shopName.contains("उचित मूल्य दुकान - प्राथमिक कृषि"))
                defaultShop.shopName
            else
                "PDS BODLA"
        )
    }

    val currentItem = stockItems.firstOrNull { it.id == selectedItemType } ?: stockItems.first()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Activity Result Launchers
    val cameraBitmapLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val saved = saveChallanBitmapToInternal(context, bitmap)
            if (saved.isNotBlank()) {
                photoUri = saved
                coroutineScope.launch {
                    isOcrProcessing = true
                    ocrStatusMessage = if (isHi) "चावल आबंटन कूपन से डेटा पढ़ा जा रहा है..." else "Scanning coupon via OCR..."
                    val res = CouponOcrParser.parseCouponFromBitmap(bitmap)
                    isOcrProcessing = false
                    ocrResult = res
                    if (res.isSuccess) {
                        if (res.couponNumber.isNotBlank()) challanNumber = res.couponNumber
                        if (res.receiptDateDdMmYyyy.isNotBlank()) receiptDate = res.receiptDateDdMmYyyy
                        if (res.pdsShopName.isNotBlank()) selectedShopName = res.pdsShopName
                        if (res.quantityKg > 0.0) {
                            quantityText = if (res.quantityKg % 1.0 == 0.0) res.quantityKg.toInt().toString() else String.format(Locale.US, "%.2f", res.quantityKg)
                            selectedItemType = "RICE"
                        }
                        remarks = ""
                        ocrStatusMessage = if (isHi) "✓ कूपन से डेटा स्वतः भरा गया! यदि आवश्यकता हो तो नीचे संपादन करें।" else "✓ Coupon details auto-filled! You can edit fields below if needed."
                        Toast.makeText(context, if (isHi) "✓ कूपन विवरण स्वतः भर दिया गया है" else "✓ Coupon data auto-filled", Toast.LENGTH_SHORT).show()
                    } else {
                        ocrStatusMessage = res.errorMessage ?: if (isHi) "कूपन से पूरा विवरण नहीं पढ़ा जा सका, कृपया विवरण हाथ से भरें।" else "Could not detect coupon details. Please fill manually."
                    }
                }
            }
        }
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val saved = saveChallanUriToInternal(context, uri)
            if (saved.isNotBlank()) {
                photoUri = saved
                coroutineScope.launch {
                    isOcrProcessing = true
                    ocrStatusMessage = if (isHi) "चावल आबंटन कूपन से डेटा पढ़ा जा रहा है..." else "Scanning coupon via OCR..."
                    val res = CouponOcrParser.parseCouponFromUri(context, Uri.parse(saved))
                    isOcrProcessing = false
                    ocrResult = res
                    if (res.isSuccess) {
                        if (res.couponNumber.isNotBlank()) challanNumber = res.couponNumber
                        if (res.receiptDateDdMmYyyy.isNotBlank()) receiptDate = res.receiptDateDdMmYyyy
                        if (res.pdsShopName.isNotBlank()) selectedShopName = res.pdsShopName
                        if (res.quantityKg > 0.0) {
                            quantityText = if (res.quantityKg % 1.0 == 0.0) res.quantityKg.toInt().toString() else String.format(Locale.US, "%.2f", res.quantityKg)
                            selectedItemType = "RICE"
                        }
                        remarks = ""
                        ocrStatusMessage = if (isHi) "✓ कूपन से डेटा स्वतः भरा गया! यदि आवश्यकता हो तो नीचे संपादन करें।" else "✓ Coupon details auto-filled! You can edit fields below if needed."
                        Toast.makeText(context, if (isHi) "✓ कूपन विवरण स्वतः भर दिया गया है" else "✓ Coupon data auto-filled", Toast.LENGTH_SHORT).show()
                    } else {
                        ocrStatusMessage = res.errorMessage ?: if (isHi) "कूपन से पूरा विवरण नहीं पढ़ा जा सका, कृपया विवरण हाथ से भरें।" else "Could not detect coupon details. Please fill manually."
                    }
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraBitmapLauncher.launch(null)
        } else {
            Toast.makeText(
                context,
                if (isHi) "कैमरा अनुमति आवश्यक है" else "Camera permission is required",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(autoLaunchCamera) {
        if (autoLaunchCamera) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraBitmapLauncher.launch(null)
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    AlertDialog(
        onDismissRequest = {
            keyboardController?.hide()
            focusManager.clearFocus()
            onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isHi) "चावल / स्टॉक प्राप्ति दर्ज करें" else "Receive Stock",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                    Text(
                        text = if (isHi) "कूपन फोटो से OCR द्वारा स्वतः भरें" else "Auto-fill with Rice Allotment Coupon OCR",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // STEP 1: UPLOAD / CLICK COUPON IMAGE (FIRST STEP)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = Color(0xFF16A34A),
                                shape = CircleShape,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("1", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isHi) "चरण 1: चावल आबंटन कूपन फोटो लें / अपलोड करें" else "Step 1: Upload / Scan Coupon Image",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF14532D)
                                )
                                Text(
                                    text = if (isHi) "कूपन फोटो अपलोड करते ही कूपन नंबर, दिनांक, दुकान व मात्रा स्वतः भर जाएगी।" else "Click photo to auto-extract coupon #, date, PDS shop & quantity via OCR",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF15803D),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Buttons for Camera and Gallery
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                        cameraBitmapLauncher.launch(null)
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isHi) "कैमरा से फोटो" else "Camera", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            OutlinedButton(
                                onClick = { galleryPickerLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF15803D)),
                                border = BorderStroke(1.dp, Color(0xFF16A34A))
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isHi) "गैलरी से चुनें" else "Gallery", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // OCR Processing Progress Indicator
                        if (isOcrProcessing) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = BluePrimary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isHi) "कूपन से डेटा पढ़ा जा रहा है (OCR Processing)..." else "Extracting data from coupon via OCR...",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = BluePrimary
                                        )
                                        Text(
                                            text = if (isHi) "कूपन क्र., दिनांक, PDS दुकान व मात्रा निकाली जा रही है" else "Reading coupon number, date, PDS shop & allotment",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF64748B),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        // OCR Extraction Success / Notice Card
                        if (ocrStatusMessage != null && !isOcrProcessing) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val isSuccess = ocrResult?.isSuccess == true
                            Surface(
                                color = if (isSuccess) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isSuccess) Color(0xFFA7F3D0) else Color(0xFFFDE68A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Info,
                                            contentDescription = null,
                                            tint = if (isSuccess) Color(0xFF059669) else Color(0xFFD97706),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = ocrStatusMessage ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSuccess) Color(0xFF065F46) else Color(0xFF92400E),
                                            fontSize = 12.sp
                                        )
                                    }
                                    if (isSuccess && ocrResult != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isHi) "💡 नीचे दिए गए फ़ॉर्म में विवरण भर दिया गया है। यदि कोई संख्या या नाम गलत हो, तो आप उसे सीधे एडिट कर सकते हैं।" else "💡 Fields below are populated. If anything is incorrect, you can edit before saving.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF047857),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Attached Photo Preview
                        if (photoUri.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .border(1.dp, Color(0xFF94A3B8), RoundedCornerShape(6.dp))
                                            .clickable { previewPhotoUri = photoUri }
                                    ) {
                                        AsyncImage(
                                            model = photoUri,
                                            contentDescription = "Challan Preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ZoomIn,
                                                contentDescription = "Zoom",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isHi) "✓ कूपन फोटो संलग्न है" else "✓ Coupon Photo Attached",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF166534)
                                        )
                                        Text(
                                            text = if (isHi) "बड़ा देखने के लिए फोटो पर टैप करें" else "Tap photo to preview in full screen",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B),
                                            fontSize = 11.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            photoUri = ""
                                            ocrResult = null
                                            ocrStatusMessage = null
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Remove Photo",
                                            tint = Color(0xFFDC2626)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // STEP 2: REVIEW AND EDIT FORM FIELDS (USER CAN EDIT ANY VALUE)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = BluePrimary,
                                shape = CircleShape,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("2", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isHi) "चरण 2: विवरण जाँचें या संशोधित करें" else "Step 2: Review & Edit Details",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = if (isHi) "आवश्यकतानुसार किसी भी फ़ील्ड को बदल सकते हैं" else "You can modify any field before saving",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        Text(
                            text = if (isHi) "प्राप्त होने वाली सामग्री चुनें:" else "Select Item to Receive:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(stockItems) { item ->
                                val isSelected = selectedItemType == item.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedItemType = item.id },
                                    label = { Text(if (isHi) item.nameHi else item.nameEn, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BluePrimary.copy(alpha = 0.15f),
                                        selectedLabelColor = BluePrimary
                                    )
                                )
                            }
                        }

                        PoshanDatePickerField(
                            value = receiptDate,
                            onValueChange = { receiptDate = it },
                            label = if (isHi) "प्राप्ति दिनांक (DD-MM-YYYY)" else "Receipt Date (DD-MM-YYYY)",
                            isHi = isHi,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column {
                            OutlinedTextField(
                                value = quantityText,
                                onValueChange = { quantityText = it },
                                label = { Text("${if (isHi) "प्राप्त मात्रा" else "Quantity"} (${currentItem.unit})", fontWeight = FontWeight.SemiBold) },
                                colors = poshanTextFieldColors(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            if (ocrResult?.rawAllotmentText?.isNotBlank() == true) {
                                Text(
                                    text = "✓ कूपन आबंटन: ${ocrResult?.rawAllotmentText}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = challanNumber,
                            onValueChange = { challanNumber = it },
                            label = { Text(if (isHi) "कूपन / चालान नंबर (Coupon No.)" else "Coupon / Challan No.", fontWeight = FontWeight.SemiBold) },
                            colors = poshanTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = selectedShopName,
                            onValueChange = { selectedShopName = it },
                            label = { Text(if (isHi) "उचित मूल्य दुकान (PDS Shop) / आपूर्तिकर्ता" else "PDS Shop / Supplier Name", fontWeight = FontWeight.SemiBold) },
                            colors = poshanTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = remarks,
                            onValueChange = { remarks = it },
                            label = { Text(if (isHi) "टिप्पणी (Remarks)" else "Remarks", fontWeight = FontWeight.SemiBold) },
                            colors = poshanTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    val qty = quantityText.toDoubleOrNull() ?: 0.0
                    if (qty > 0) {
                        val receipt = RiceReceiptEntity(
                            receiptId = "RCP-" + System.currentTimeMillis(),
                            pdsId = defaultShop?.pdsId ?: "PDS-DEFAULT",
                            pdsShopName = selectedShopName.ifEmpty { "PDS BODLA" },
                            receiptDate = receiptDate,
                            quantityKg = qty,
                            challanNumber = challanNumber.ifEmpty { "CH-" + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date()) },
                            govtRefNumber = "MDM-STOCK-" + SimpleDateFormat("yyyyMM", Locale.US).format(Date()),
                            remarks = remarks,
                            photoUri = photoUri
                        )
                        onSave(receipt, selectedItemType)
                    } else {
                        Toast.makeText(context, if (isHi) "कृपया सही मात्रा दर्ज करें" else "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = poshanButtonColors(BluePrimary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isHi) "स्टॉक सुरक्षित करें (Save)" else "Save Stock",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                }
            ) {
                Text(if (isHi) "रद्द करें" else "Cancel", fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
            }
        }
    )

    if (previewPhotoUri != null) {
        ChallanPhotoViewerDialog(
            photoUri = previewPhotoUri!!,
            isHi = isHi,
            onDismiss = { previewPhotoUri = null }
        )
    }
}

@Composable
private fun StockAdjustmentDialog(
    selectedItem: StockItemInfo,
    isHi: Boolean,
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit
) {
    var qtyText by remember { mutableStateOf("") }
    var isAddition by remember { mutableStateOf(true) }
    var reason by remember { mutableStateOf("भौतिक सत्यापन अनुसार संशोधन (Physical Verification)") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    DisposableEffect(Unit) {
        onDispose {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    AlertDialog(
        onDismissRequest = {
            keyboardController?.hide()
            focusManager.clearFocus()
            onDismiss()
        },
        title = {
            Text(
                text = "${if (isHi) "स्टॉक समायोजन" else "Stock Adjustment"}: ${if (isHi) selectedItem.nameHi else selectedItem.nameEn}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD97706)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isAddition,
                        onClick = { isAddition = true },
                        label = { Text("+ स्टॉक जोड़ें (Increase)", fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF22C55E).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF15803D)
                        )
                    )
                    FilterChip(
                        selected = !isAddition,
                        onClick = { isAddition = false },
                        label = { Text("- स्टॉक घटाएं (Decrease)", fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFB91C1C)
                        )
                    )
                }

                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("${if (isHi) "समायोजन मात्रा" else "Quantity"} (${selectedItem.unit})", fontWeight = FontWeight.SemiBold) },
                    colors = poshanTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(if (isHi) "समायोजन कारण (Reason)" else "Adjustment Reason", fontWeight = FontWeight.SemiBold) },
                    colors = poshanTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    val rawQty = qtyText.toDoubleOrNull() ?: 0.0
                    val finalQty = if (isAddition) rawQty else -rawQty
                    if (rawQty > 0) {
                        onSave(finalQty, reason)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
            ) {
                Text(if (isHi) "समायोजन दर्ज करें" else "Save Adjustment", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                }
            ) {
                Text(if (isHi) "रद्द करें" else "Cancel", fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
            }
        }
    )
}

@Composable
private fun SetOpeningStockDialog(
    selectedItem: StockItemInfo,
    isHi: Boolean,
    onDismiss: () -> Unit,
    onSave: (date: String, quantity: Double, remarks: String) -> Unit
) {
    val today = getCurrentStockDate()
    var dateText by remember { mutableStateOf(today) }
    var quantityText by remember { mutableStateOf("") }
    var remarksText by remember { mutableStateOf(if (isHi) "प्रारंभिक स्टॉक (${selectedItem.nameHi})" else "Opening Balance (${selectedItem.nameEn})") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    DisposableEffect(Unit) {
        onDispose {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    AlertDialog(
        onDismissRequest = {
            keyboardController?.hide()
            focusManager.clearFocus()
            onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFCCFBF1)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EditCalendar,
                        contentDescription = null,
                        tint = Color(0xFF0F766E),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isHi) "प्रारंभिक स्टॉक दर्ज करें" else "Set Opening Stock",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F766E)
                    )
                    Text(
                        text = if (isHi) selectedItem.nameHi else selectedItem.nameEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PoshanDatePickerField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = if (isHi) "प्रभावी दिनांक (DD-MM-YYYY)" else "Effective Date (DD-MM-YYYY)",
                    isHi = isHi,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text(if (isHi) "प्रारंभिक मात्रा (${selectedItem.unit})" else "Opening Quantity (${selectedItem.unit})", fontWeight = FontWeight.SemiBold) },
                    placeholder = { Text("उदा. 150.0") },
                    colors = poshanTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = remarksText,
                    onValueChange = { remarksText = it },
                    label = { Text(if (isHi) "विवरण / टिप्पणी (Remarks)" else "Description / Remarks", fontWeight = FontWeight.SemiBold) },
                    colors = poshanTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    val qty = quantityText.toDoubleOrNull() ?: 0.0
                    if (qty >= 0 && dateText.isNotBlank()) {
                        onSave(dateText, qty, remarksText)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
            ) {
                Text(if (isHi) "सुरक्षित करें" else "Save Opening Stock", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                }
            ) {
                Text(if (isHi) "रद्द करें" else "Cancel", color = Color(0xFF475569))
            }
        }
    )
}

@Composable
private fun EditRiceReceiptDialog(
    receipt: RiceReceiptEntity,
    pdsShops: List<PdsShopEntity>,
    isHi: Boolean,
    onDismiss: () -> Unit,
    onSave: (RiceReceiptEntity) -> Unit
) {
    var receiptDate by remember { mutableStateOf(formatToDdMmYyyy(receipt.receiptDate)) }
    var quantityText by remember { mutableStateOf(receipt.quantityKg.toString()) }
    var challanNumber by remember { mutableStateOf(receipt.challanNumber) }
    var pdsShopName by remember { mutableStateOf(receipt.pdsShopName.ifEmpty { "PDS BODLA" }) }
    var remarks by remember { mutableStateOf(receipt.remarks) }
    var photoUri by remember { mutableStateOf(receipt.photoUri) }
    var previewPhotoUri by remember { mutableStateOf<String?>(null) }
    var isOcrProcessing by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val cameraBitmapLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val saved = saveChallanBitmapToInternal(context, bitmap)
            if (saved.isNotBlank()) {
                photoUri = saved
                coroutineScope.launch {
                    isOcrProcessing = true
                    val res = CouponOcrParser.parseCouponFromBitmap(bitmap)
                    isOcrProcessing = false
                    if (res.isSuccess) {
                        if (res.couponNumber.isNotBlank()) challanNumber = res.couponNumber
                        if (res.receiptDateDdMmYyyy.isNotBlank()) receiptDate = res.receiptDateDdMmYyyy
                        if (res.pdsShopName.isNotBlank()) pdsShopName = res.pdsShopName
                        if (res.quantityKg > 0.0) {
                            quantityText = if (res.quantityKg % 1.0 == 0.0) res.quantityKg.toInt().toString() else String.format(Locale.US, "%.2f", res.quantityKg)
                        }
                        remarks = ""
                        Toast.makeText(context, if (isHi) "✓ कूपन से विवरण अपडेट हुआ" else "✓ Coupon details updated from photo", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val saved = saveChallanUriToInternal(context, uri)
            if (saved.isNotBlank()) {
                photoUri = saved
                coroutineScope.launch {
                    isOcrProcessing = true
                    val res = CouponOcrParser.parseCouponFromUri(context, Uri.parse(saved))
                    isOcrProcessing = false
                    if (res.isSuccess) {
                        if (res.couponNumber.isNotBlank()) challanNumber = res.couponNumber
                        if (res.receiptDateDdMmYyyy.isNotBlank()) receiptDate = res.receiptDateDdMmYyyy
                        if (res.pdsShopName.isNotBlank()) pdsShopName = res.pdsShopName
                        if (res.quantityKg > 0.0) {
                            quantityText = if (res.quantityKg % 1.0 == 0.0) res.quantityKg.toInt().toString() else String.format(Locale.US, "%.2f", res.quantityKg)
                        }
                        remarks = ""
                        Toast.makeText(context, if (isHi) "✓ कूपन से विवरण अपडेट हुआ" else "✓ Coupon details updated from photo", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraBitmapLauncher.launch(null)
        } else {
            Toast.makeText(
                context,
                if (isHi) "कैमरा अनुमति आवश्यक है" else "Camera permission is required",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    AlertDialog(
        onDismissRequest = {
            keyboardController?.hide()
            focusManager.clearFocus()
            onDismiss()
        },
        title = {
            Text(
                text = if (isHi) "रसीद संपादित करें" else "Edit Receipt",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2563EB)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PoshanDatePickerField(
                    value = receiptDate,
                    onValueChange = { receiptDate = it },
                    label = if (isHi) "प्राप्ति दिनांक (DD-MM-YYYY)" else "Receipt Date (DD-MM-YYYY)",
                    isHi = isHi,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text(if (isHi) "मात्रा कि.ग्रा. (Quantity in Kg)" else "Quantity (kg)", fontWeight = FontWeight.SemiBold) },
                    colors = poshanTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = challanNumber,
                    onValueChange = { challanNumber = it },
                    label = { Text(if (isHi) "चालान नंबर (Challan Number)" else "Challan Number", fontWeight = FontWeight.SemiBold) },
                    colors = poshanTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = pdsShopName,
                    onValueChange = { pdsShopName = it },
                    label = { Text(if (isHi) "उचित मूल्य दुकान (PDS Shop)" else "PDS Shop Name", fontWeight = FontWeight.SemiBold) },
                    colors = poshanTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text(if (isHi) "टिप्पणी (Remarks)" else "Remarks", fontWeight = FontWeight.SemiBold) },
                    colors = poshanTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Challan Photo in Edit Dialog
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHi) "चालान प्रति (Challan Copy)" else "Challan Copy",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }

                        if (isOcrProcessing) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isHi) "OCR द्वारा विवरण पढ़ा जा रहा है..." else "Scanning coupon...",
                                    fontSize = 11.sp,
                                    color = BluePrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (photoUri.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                        .clickable { previewPhotoUri = photoUri }
                                ) {
                                    AsyncImage(
                                        model = photoUri,
                                        contentDescription = "Challan Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ZoomIn,
                                            contentDescription = "Zoom",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isHi) "फोटो संलग्न है" else "Photo Attached",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF166534)
                                    )
                                    Text(
                                        text = if (isHi) "बड़ा देखने के लिए टैप करें" else "Tap to view full screen",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                }
                                IconButton(onClick = { photoUri = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove Photo",
                                        tint = Color(0xFFDC2626)
                                    )
                                }
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                            cameraBitmapLauncher.launch(null)
                                        } else {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB))
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isHi) "कैमरा" else "Camera", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        galleryPickerLauncher.launch("image/*")
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F766E))
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isHi) "गैलरी" else "Gallery", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    val qty = quantityText.toDoubleOrNull() ?: 0.0
                    if (qty > 0) {
                        onSave(
                            receipt.copy(
                                receiptDate = receiptDate,
                                quantityKg = qty,
                                challanNumber = challanNumber,
                                pdsShopName = pdsShopName.ifEmpty { "PDS BODLA" },
                                remarks = remarks,
                                photoUri = photoUri
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text(if (isHi) "अपडेट करें" else "Update Receipt", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                }
            ) {
                Text(if (isHi) "रद्द करें" else "Cancel", color = Color(0xFF475569))
            }
        }
    )

    if (previewPhotoUri != null) {
        ChallanPhotoViewerDialog(
            photoUri = previewPhotoUri!!,
            isHi = isHi,
            onDismiss = { previewPhotoUri = null }
        )
    }
}

@Composable
private fun EditStockTransactionDialog(
    transaction: StockTransactionEntity,
    stockItems: List<StockItemInfo>,
    isHi: Boolean,
    onDismiss: () -> Unit,
    onSave: (StockTransactionEntity) -> Unit
) {
    var dateText by remember { mutableStateOf(formatToDdMmYyyy(transaction.transactionDate)) }
    var qtyText by remember { mutableStateOf(Math.abs(transaction.quantityKg).toString()) }
    var isAddition by remember { mutableStateOf(transaction.quantityKg >= 0) }
    var descriptionText by remember { mutableStateOf(transaction.description) }
    var itemType by remember { mutableStateOf(transaction.itemType.ifEmpty { "RICE" }) }

    val matchedItem = findMatchingStockItem(itemType, stockItems) ?: stockItems.first()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    DisposableEffect(Unit) {
        onDispose {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    AlertDialog(
        onDismissRequest = {
            keyboardController?.hide()
            focusManager.clearFocus()
            onDismiss()
        },
        title = {
            Text(
                text = if (isHi) "प्रविष्टि संपादित करें" else "Edit Stock Entry",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2563EB)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PoshanDatePickerField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = if (isHi) "दिनांक (DD-MM-YYYY)" else "Date (DD-MM-YYYY)",
                    isHi = isHi,
                    modifier = Modifier.fillMaxWidth()
                )

                if (transaction.transactionType == "ADJUSTMENT") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isAddition,
                            onClick = { isAddition = true },
                            label = { Text("+ जोड़ें", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF22C55E).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFF15803D)
                            )
                        )
                        FilterChip(
                            selected = !isAddition,
                            onClick = { isAddition = false },
                            label = { Text("- घटाएं", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFFB91C1C)
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("${if (isHi) "मात्रा" else "Quantity"} (${matchedItem.unit})", fontWeight = FontWeight.SemiBold) },
                    colors = poshanTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text(if (isHi) "विवरण (Description)" else "Description", fontWeight = FontWeight.SemiBold) },
                    colors = poshanTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    val rawQty = qtyText.toDoubleOrNull() ?: 0.0
                    val finalQty = if (transaction.transactionType == "USAGE") {
                        -Math.abs(rawQty)
                    } else if (transaction.transactionType == "ADJUSTMENT") {
                        if (isAddition) Math.abs(rawQty) else -Math.abs(rawQty)
                    } else {
                        Math.abs(rawQty)
                    }

                    if (rawQty > 0) {
                        onSave(
                            transaction.copy(
                                transactionDate = dateText,
                                quantityKg = finalQty,
                                description = descriptionText,
                                itemType = itemType
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text(if (isHi) "सुरक्षित करें" else "Save Changes", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                }
            ) {
                Text(if (isHi) "रद्द करें" else "Cancel", color = Color(0xFF475569))
            }
        }
    )
}

@Composable
fun ChallanPhotoViewerDialog(
    photoUri: String,
    isHi: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHi) "चालान / कूपन प्रति पूर्वावलोकन" else "Challan / Voucher Preview",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                // Image display
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Full Challan Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                // Footer button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = if (isHi) "बंद करें (Close)" else "Close Preview",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
