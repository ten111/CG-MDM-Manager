package com.example.presentation.more

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ConfigNormsEntity
import com.example.data.local.entity.CookingAgencyEntity
import com.example.data.model.CustomFoodItem
import com.example.data.model.CustomFoodItemParser
import com.example.presentation.common.AppLanguage
import com.example.presentation.common.PoshanTopAppBar
import com.example.presentation.viewmodel.PoshanViewModel
import com.example.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsNormsScreen(
    viewModel: PoshanViewModel,
    onNavigateBack: () -> Unit
) {
    val configNorms by viewModel.configNorms.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isHi = currentLanguage == AppLanguage.HINDI
    val context = LocalContext.current

    // Initialize state from existing norms (Rice quantity default 0.150 kg per child/day)
    var riceKgText by remember(configNorms) {
        val normGrams = configNorms?.primaryRiceNormGrams?.takeIf { it > 0.0 } ?: 150.0
        val effectiveGrams = if (normGrams == 100.0 || normGrams == 110.0) 150.0 else normGrams
        mutableStateOf(String.format(java.util.Locale.US, "%.3f", effectiveGrams / 1000.0))
    }
    var pulseKgText by remember(configNorms) {
        val pulse = configNorms?.pulseNormGrams ?: 30.0
        mutableStateOf(String.format(java.util.Locale.US, "%.3f", pulse / 1000.0))
    }
    var vegKgText by remember(configNorms) {
        val veg = configNorms?.vegetableNormGrams ?: 75.0
        mutableStateOf(String.format(java.util.Locale.US, "%.3f", veg / 1000.0))
    }
    var oilKgText by remember(configNorms) {
        val oil = configNorms?.oilNormGrams ?: 7.5
        mutableStateOf(String.format(java.util.Locale.US, "%.3f", oil / 1000.0))
    }
    var saltKgText by remember(configNorms) {
        val salt = configNorms?.saltNormGrams ?: 5.0
        mutableStateOf(String.format(java.util.Locale.US, "%.3f", salt / 1000.0))
    }

    // Custom items list
    var customItems by remember(configNorms) {
        val parsed = CustomFoodItemParser.parse(configNorms?.customItemsJson)
        mutableStateOf(
            if (parsed.isNotEmpty()) parsed else CustomFoodItemParser.getDefaultCustomItems()
        )
    }

    var showAddCustomDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<CustomFoodItem?>(null) }

    fun persistCustomItems(newList: List<CustomFoodItem>) {
        customItems = newList
        val riceKg = riceKgText.toDoubleOrNull() ?: 0.150
        val riceGrams = riceKg * 1000.0
        val updated = (configNorms ?: ConfigNormsEntity()).copy(
            primaryRiceNormGrams = riceGrams,
            upperPrimaryRiceNormGrams = riceGrams,
            pulseNormGrams = (pulseKgText.toDoubleOrNull() ?: 0.030) * 1000.0,
            oilNormGrams = (oilKgText.toDoubleOrNull() ?: 0.0075) * 1000.0,
            vegetableNormGrams = (vegKgText.toDoubleOrNull() ?: 0.075) * 1000.0,
            saltNormGrams = (saltKgText.toDoubleOrNull() ?: 0.005) * 1000.0,
            customItemsJson = CustomFoodItemParser.toJson(newList)
        )
        viewModel.saveConfigNorms(updated)
    }

    Scaffold(
        topBar = {
            PoshanTopAppBar(
                title = if (isHi) "खाद्यान्न मान एवं अतिरिक्त सामग्री" else "Food Norms & Custom Items",
                subtitle = if (isHi) "प्रति छात्र खाद्यान्न मान एवं अतिरिक्त सामग्री निर्धारण" else "Per Student Material Norms & Custom Items",
                currentLanguage = currentLanguage,
                onLanguageToggle = { viewModel.toggleLanguage() },
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Button(
                    onClick = {
                        val riceKg = riceKgText.toDoubleOrNull() ?: 0.150
                        val riceGrams = riceKg * 1000.0
                        val updated = (configNorms ?: ConfigNormsEntity()).copy(
                            primaryRiceNormGrams = riceGrams,
                            upperPrimaryRiceNormGrams = riceGrams,
                            pulseNormGrams = (pulseKgText.toDoubleOrNull() ?: 0.030) * 1000.0,
                            oilNormGrams = (oilKgText.toDoubleOrNull() ?: 0.0075) * 1000.0,
                            vegetableNormGrams = (vegKgText.toDoubleOrNull() ?: 0.075) * 1000.0,
                            saltNormGrams = (saltKgText.toDoubleOrNull() ?: 0.005) * 1000.0,
                            customItemsJson = CustomFoodItemParser.toJson(customItems)
                        )
                        viewModel.saveConfigNorms(updated)
                        Toast.makeText(
                            context,
                            if (isHi) "✓ मुख्य खाद्यान्न मान (चावल, दाल, तेल आदि) सुरक्षित किए गए" else "✓ Main food norms (Rice, Pulses, Oil) saved successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BluePrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHi) "मुख्य खाद्यान्न मान सुरक्षित करें (चावल, दाल, तेल)" else "Save Main Food Norms (Rice, Dal, Oil)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // FOOD NORMS & QUANTITIES PER STUDENT CARD
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHi) "खाद्यान्न एवं सामग्री मान (प्रति छात्र/दिवस)" else "Food Norms (Per Student/Day)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Text(
                                text = "PM POSHAN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    // 1. Rice
                    StandardNormRow(
                        label = if (isHi) "चावल (Rice)" else "Rice",
                        icon = "🌾",
                        value = riceKgText,
                        unit = "kg",
                        onValueChange = { riceKgText = it }
                    )

                    // 2. Pulses
                    StandardNormRow(
                        label = if (isHi) "दाल (Pulses)" else "Pulses",
                        icon = "🥣",
                        value = pulseKgText,
                        unit = "kg",
                        onValueChange = { pulseKgText = it }
                    )

                    // 3. Vegetables
                    StandardNormRow(
                        label = if (isHi) "सब्जियां (Vegetables)" else "Vegetables",
                        icon = "🥬",
                        value = vegKgText,
                        unit = "kg",
                        onValueChange = { vegKgText = it }
                    )

                    // 4. Oil
                    StandardNormRow(
                        label = if (isHi) "खाद्य तेल (Oil / Fat)" else "Oil / Fat",
                        icon = "🛢️",
                        value = oilKgText,
                        unit = "kg",
                        onValueChange = { oilKgText = it }
                    )

                    // 5. Salt & Spices
                    StandardNormRow(
                        label = if (isHi) "नमक व मसाले (Salt & Spices)" else "Salt & Spices",
                        icon = "🧂",
                        value = saltKgText,
                        unit = "kg",
                        onValueChange = { saltKgText = it }
                    )

                    // Custom Items in this section
                    if (customItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isHi) "अनुकूलित अतिरिक्त सामग्री (Custom Items):" else "Customisable Items:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )

                        customItems.forEachIndexed { index, item ->
                            CustomItemRow(
                                item = item,
                                isHi = isHi,
                                onToggle = { enabled ->
                                    val list = customItems.toMutableList().also {
                                        it[index] = item.copy(isEnabled = enabled)
                                    }
                                    persistCustomItems(list)
                                },
                                onEdit = {
                                    itemToEdit = item
                                    showAddCustomDialog = true
                                },
                                onDelete = {
                                    val list = customItems.toMutableList().also {
                                        it.removeAt(index)
                                    }
                                    persistCustomItems(list)
                                    Toast.makeText(
                                        context,
                                        if (isHi) "सामग्री हटाई गई" else "Item removed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }

                    // ADD CUSTOM ITEM BUTTON (Themed Blue outline)
                    Surface(
                        onClick = {
                            itemToEdit = null
                            showAddCustomDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.5.dp, Color(0xFF93C5FD)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = null,
                                tint = BluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHi) "+ अतिरिक्त सामग्री जोड़ें (Add Custom Item)" else "+ Add Custom Item",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // ADD / EDIT CUSTOM ITEM DIALOG
    if (showAddCustomDialog) {
        AddEditCustomItemDialog(
            existing = itemToEdit,
            isHi = isHi,
            onDismiss = {
                showAddCustomDialog = false
                itemToEdit = null
            },
            onSave = { newItem ->
                val list = customItems.toMutableList()
                val index = list.indexOfFirst { it.id == newItem.id }
                if (index >= 0) {
                    list[index] = newItem
                } else {
                    list.add(newItem)
                }
                persistCustomItems(list)
                showAddCustomDialog = false
                itemToEdit = null
                Toast.makeText(
                    context,
                    if (isHi) "✓ सामग्री तुरंत सुरक्षित की गई" else "✓ Item saved immediately",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}

@Composable
private fun StandardNormRow(
    label: String,
    icon: String,
    value: String,
    unit: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.width(100.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BluePrimary,
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC)
                )
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                modifier = Modifier.width(28.dp)
            )
        }
    }
}

@Composable
private fun CustomItemRow(
    item: CustomFoodItem,
    isHi: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = if (item.isEnabled) Color(0xFFF8FAFC) else Color(0xFFF1F5F9),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (item.isEnabled) Color(0xFFCBD5E1) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.getDisplayName(isHi),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isEnabled) Color(0xFF0F172A) else Color(0xFF94A3B8)
                )
                Text(
                    text = "${item.formattedQuantity()} ${item.getBilingualUnit()} / ${if (isHi) "छात्र" else "child"}${if (item.rate > 0) " • ₹${item.rate}" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.isEnabled) BluePrimary else Color(0xFF94A3B8),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                }
                Switch(
                    checked = item.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BluePrimary
                    ),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditCustomItemDialog(
    existing: CustomFoodItem?,
    isHi: Boolean,
    onDismiss: () -> Unit,
    onSave: (CustomFoodItem) -> Unit
) {
    var nameEn by remember { mutableStateOf(existing?.name ?: "") }
    var nameHi by remember { mutableStateOf(existing?.nameHi ?: "") }
    var quantityText by remember {
        mutableStateOf(existing?.quantity?.toString() ?: "1.0")
    }
    var selectedUnit by remember {
        mutableStateOf(
            when (existing?.unit?.lowercase()) {
                "pcs", "nos" -> "Pcs"
                "dozen", "duzzen", "दर्जन" -> "Dozen"
                "ltr", "l" -> "Ltr"
                null -> "Pcs"
                else -> existing.unit
            }
        )
    }
    var rateText by remember {
        mutableStateOf(if ((existing?.rate ?: 0.0) > 0) existing!!.rate.toString() else "0")
    }

    val suggestionList = listOf(
        Pair("Boiled Egg", "उबला अंडा"),
        Pair("Banana / Seasonal Fruit", "केला / मौसमी फल"),
        Pair("Soyabadi (Soya Chunks)", "सोयाबड़ी (kg)"),
        Pair("Soya Milk", "सोयादूध (Soya Milk)"),
        Pair("Iron Folic Acid (IFA)", "आयरन फोलिक एसिड (IFA)"),
        Pair("Deworming Tablets (Albendazole)", "कृमिनाशक (अल्बेंडाजोल)"),
        Pair("Peanut Chikki / Gud", "मूंगफली चिक्की / गुड़"),
        Pair("Milk / Kheer", "दूध / खीर"),
        Pair("Sprouts / Chana", "अंकुरित चना / मूंग"),
        Pair("Halwa / Sweet", "हलवा / मिष्ठान")
    )

    data class UnitOption(val code: String, val label: String)
    val row1Units = listOf(
        UnitOption("Pcs", "Pcs / नग"),
        UnitOption("Dozen", "Dozen / दर्जन")
    )
    val row2Units = listOf(
        UnitOption("kg", "kg / किग्रा"),
        UnitOption("g", "g / ग्राम"),
        UnitOption("pkt", "pkt / पैकेट")
    )
    val row3Units = listOf(
        UnitOption("Ltr", "Ltr / लीटर"),
        UnitOption("ml", "ml / मि.ली.")
    )

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
                        .background(Color(0xFFEFF6FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (existing == null) Icons.Default.AddCircle else Icons.Default.Edit,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (existing == null) {
                        if (isHi) "नई अनुकूलित सामग्री जोड़ें" else "Add Custom Food Item"
                    } else {
                        if (isHi) "सामग्री संपादित करें" else "Edit Custom Food Item"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quick suggestions
                Text(
                    text = if (isHi) "त्वरित सुझाव (Quick Presets):" else "Quick Suggestions:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(suggestionList) { (en, hi) ->
                        Surface(
                            onClick = {
                                nameEn = en
                                nameHi = hi
                                when {
                                    en.contains("Egg", ignoreCase = true) -> {
                                        selectedUnit = "Pcs"
                                        quantityText = "1.0"
                                    }
                                    en.contains("Fruit", ignoreCase = true) || en.contains("Banana", ignoreCase = true) || hi.contains("केला") -> {
                                        selectedUnit = "Dozen"
                                        quantityText = "0.083"
                                    }
                                    en.contains("Soyabadi", ignoreCase = true) || en.contains("Soyabean", ignoreCase = true) -> {
                                        selectedUnit = "kg"
                                        quantityText = "0.025"
                                    }
                                    en.contains("Soya Milk", ignoreCase = true) || en.contains("Milk", ignoreCase = true) -> {
                                        selectedUnit = "Ltr"
                                        quantityText = "0.200"
                                    }
                                    en.contains("Iron", ignoreCase = true) || en.contains("IFA", ignoreCase = true) || en.contains("Tablet", ignoreCase = true) -> {
                                        selectedUnit = "Pcs"
                                        quantityText = "1.0"
                                    }
                                    en.contains("Chikki", ignoreCase = true) -> {
                                        selectedUnit = "kg"
                                        quantityText = "0.020"
                                    }
                                    en.contains("Sprouts", ignoreCase = true) -> {
                                        selectedUnit = "kg"
                                        quantityText = "0.025"
                                    }
                                    en.contains("Halwa", ignoreCase = true) -> {
                                        selectedUnit = "kg"
                                        quantityText = "0.050"
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Text(
                                text = if (isHi) hi else en,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Name English
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text(if (isHi) "सामग्री का नाम (English) *" else "Item Name (English) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BluePrimary
                    )
                )

                // Name Hindi
                OutlinedTextField(
                    value = nameHi,
                    onValueChange = { nameHi = it },
                    label = { Text(if (isHi) "सामग्री का नाम (हिंदी)" else "Item Name (Hindi)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BluePrimary
                    )
                )

                // Quantity
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text(if (isHi) "मात्रा प्रति छात्र (Quantity / Student) *" else "Quantity / Child *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BluePrimary
                    )
                )

                // Unit Selection - Solid Blue Color when selected with bilingual labels
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isHi) "इकाई (Unit) चुनें *" else "Select Unit *",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 1: Pcs / नग, Dozen / दर्जन
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row1Units.forEach { u ->
                            val isSelected = selectedUnit.equals(u.code, ignoreCase = true) ||
                                (u.code == "Pcs" && (selectedUnit.equals("pcs", ignoreCase = true) || selectedUnit.equals("nos", ignoreCase = true))) ||
                                (u.code == "Dozen" && (selectedUnit.equals("dozen", ignoreCase = true) || selectedUnit.equals("duzzen", ignoreCase = true) || selectedUnit.equals("दर्जन", ignoreCase = true)))
                            Surface(
                                onClick = { selectedUnit = u.code },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) BluePrimary else Color(0xFFF1F5F9),
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) BluePrimary else Color(0xFFCBD5E1)
                                ),
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = u.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 2: kg / किग्रा, g / ग्राम, pkt / पैकेट
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row2Units.forEach { u ->
                            val isSelected = selectedUnit.equals(u.code, ignoreCase = true)
                            Surface(
                                onClick = { selectedUnit = u.code },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) BluePrimary else Color(0xFFF1F5F9),
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) BluePrimary else Color(0xFFCBD5E1)
                                ),
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = u.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 3: Ltr / लीटर, ml / मि.ली.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row3Units.forEach { u ->
                            val isSelected = selectedUnit.equals(u.code, ignoreCase = true) ||
                                (u.code == "Ltr" && selectedUnit.equals("l", ignoreCase = true))
                            Surface(
                                onClick = { selectedUnit = u.code },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) BluePrimary else Color(0xFFF1F5F9),
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) BluePrimary else Color(0xFFCBD5E1)
                                ),
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = u.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }
                }

                // Rate in ₹
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text(if (isHi) "अनुमानित दर (₹/छात्र - ऐच्छिक)" else "Estimated Rate (₹/child - Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BluePrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    if (nameEn.isNotBlank() || nameHi.isNotBlank()) {
                        val item = CustomFoodItem(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            name = nameEn.ifBlank { nameHi },
                            nameHi = nameHi,
                            quantity = quantityText.toDoubleOrNull() ?: 1.0,
                            unit = selectedUnit,
                            rate = rateText.toDoubleOrNull() ?: 0.0,
                            isEnabled = existing?.isEnabled ?: true
                        )
                        onSave(item)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Text(if (isHi) "सुरक्षित करें" else "Save Item", fontWeight = FontWeight.Bold, color = Color.White)
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
                Text(if (isHi) "रद्द करें" else "Cancel")
            }
        }
    )
}
