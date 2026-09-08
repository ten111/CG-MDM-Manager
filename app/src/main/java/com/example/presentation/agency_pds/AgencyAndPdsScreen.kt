package com.example.presentation.agency_pds

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CookingAgencyEntity
import com.example.data.local.entity.PdsShopEntity
import com.example.presentation.common.*
import com.example.presentation.viewmodel.PoshanViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgencyAndPdsScreen(
    viewModel: PoshanViewModel,
    onNavigateBack: () -> Unit
) {
    val agencies by viewModel.allAgencies.collectAsState()
    val pdsShops by viewModel.allPdsShops.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isHi = currentLanguage == AppLanguage.HINDI

    var selectedTab by remember { mutableStateOf(0) }
    var editingAgency by remember { mutableStateOf<CookingAgencyEntity?>(null) }
    var isNewAgency by remember { mutableStateOf(false) }

    var editingPdsShop by remember { mutableStateOf<PdsShopEntity?>(null) }
    var isNewPdsShop by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PoshanTopAppBar(
                title = if (isHi) "स्व-सहायता समूह एवं PDS सेटिंग्स" else "SHG & PDS Settings",
                subtitle = if (isHi) "रसोई एजेंसी (SHG) एवं उचित मूल्य दुकान (FPS) प्रबंधन" else "Cooking Agency (SHG) & PDS Fair Price Shop Settings",
                currentLanguage = currentLanguage,
                onLanguageToggle = { viewModel.toggleLanguage() },
                onSyncClick = {}
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = BluePrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHi) "रसोई एजेंसी / SHG (${agencies.size})" else "Cooking Agency / SHG (${agencies.size})",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) BluePrimary else Color(0xFF64748B)
                            )
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHi) "PDS उचित मूल्य दुकान (${pdsShops.size})" else "PDS Fair Price Shop (${pdsShops.size})",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) BluePrimary else Color(0xFF64748B)
                            )
                        }
                    }
                )
            }

            when (selectedTab) {
                0 -> AgencyTab(
                    agencies = agencies,
                    isHi = isHi,
                    onEditAgency = { agency ->
                        editingAgency = agency
                        isNewAgency = false
                    },
                    onAddNewAgency = {
                        editingAgency = CookingAgencyEntity(
                            agencyId = "SHG-${System.currentTimeMillis() % 10000}",
                            name = "",
                            contactPerson = "",
                            mobile = "",
                            address = "",
                            villageOrCity = "",
                            block = "",
                            district = "",
                            bankName = "",
                            branchName = "",
                            maskedAccountNo = "",
                            ifscCode = "",
                            agreementStartDate = "",
                            agreementEndDate = "",
                            agreementOrderNo = "",
                            associatedSchools = "",
                            status = "ACTIVE"
                        )
                        isNewAgency = true
                    }
                )
                1 -> PdsTab(
                    pdsShops = pdsShops,
                    isHi = isHi,
                    onEditPdsShop = { shop ->
                        editingPdsShop = shop
                        isNewPdsShop = false
                    },
                    onAddNewPdsShop = {
                        editingPdsShop = PdsShopEntity(
                            pdsId = "PDS-${System.currentTimeMillis() % 10000}",
                            fpsNumber = "",
                            shopName = "",
                            dealerName = "",
                            mobile = "",
                            village = "",
                            address = "",
                            block = "",
                            district = "",
                            licenseNumber = "",
                            associatedSchool = "",
                            status = "ACTIVE"
                        )
                        isNewPdsShop = true
                    }
                )
            }
        }
    }

    // EDIT AGENCY DIALOG
    editingAgency?.let { agency ->
        EditAgencyDialog(
            agency = agency,
            isNew = isNewAgency,
            isHi = isHi,
            onDismiss = { editingAgency = null },
            onSave = { updatedAgency ->
                viewModel.saveCookingAgency(updatedAgency)
                editingAgency = null
            }
        )
    }

    // EDIT PDS SHOP DIALOG
    editingPdsShop?.let { shop ->
        EditPdsShopDialog(
            shop = shop,
            isNew = isNewPdsShop,
            isHi = isHi,
            onDismiss = { editingPdsShop = null },
            onSave = { updatedShop ->
                viewModel.savePdsShop(updatedShop)
                editingPdsShop = null
            }
        )
    }
}

@Composable
private fun AgencyTab(
    agencies: List<CookingAgencyEntity>,
    isHi: Boolean,
    onEditAgency: (CookingAgencyEntity) -> Unit,
    onAddNewAgency: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHi) "पंजीकृत स्व-सहायता समूह (SHG)" else "Registered Cooking Agencies (SHG)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Button(
                    onClick = onAddNewAgency,
                    colors = poshanButtonColors(containerColor = BluePrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isHi) "नया समूह जोड़ें" else "Add Agency", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        items(agencies) { agency ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = agency.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                            Text(
                                text = "अनुबंध आदेश क्र.: ${agency.agreementOrderNo}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusBadge(status = agency.status)
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { onEditAgency(agency) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Agency",
                                    tint = BluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "👤 ${if (isHi) "अध्यक्ष/सचिव" else "Contact"}: ${agency.contactPerson}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "📞 ${agency.mobile}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = BluePrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "पता: ${agency.address}, ${agency.villageOrCity} (${agency.block}, ${agency.district})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bank details
                    MaskedAccountDisplay(
                        maskedAccount = agency.maskedAccountNo,
                        ifsc = agency.ifscCode,
                        bankName = "${agency.bankName} (${agency.branchName})"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "अनुबंध: ${agency.agreementStartDate} से ${agency.agreementEndDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "संबद्ध शाला: ${agency.associatedSchools}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = BluePrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onEditAgency(agency) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = BluePrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isHi) "समूह विवरण संपादित करें (Edit SHG)" else "Edit SHG Details", color = BluePrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun PdsTab(
    pdsShops: List<PdsShopEntity>,
    isHi: Boolean,
    onEditPdsShop: (PdsShopEntity) -> Unit,
    onAddNewPdsShop: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHi) "उचित मूल्य दुकानें (Fair Price Shops)" else "Fair Price PDS Shops",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Button(
                    onClick = onAddNewPdsShop,
                    colors = poshanButtonColors(containerColor = BluePrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isHi) "नया PDS केंद्र जोड़ें" else "Add PDS Shop", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        items(pdsShops) { shop ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = shop.shopName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                            Text(
                                text = "FPS कोड: ${shop.fpsNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusBadge(status = shop.status)
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { onEditPdsShop(shop) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit PDS Shop",
                                    tint = BluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "विक्रेता: ${shop.dealerName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "📞 ${shop.mobile}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = BluePrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "स्थान: ${shop.address}, ग्राम ${shop.village} (${shop.block}, ${shop.district})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "लाइसेंस नंबर: ${shop.licenseNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "संबद्ध शाला: ${shop.associatedSchool}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = BluePrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onEditPdsShop(shop) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = BluePrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isHi) "PDS दुकान विवरण संपादित करें (Edit PDS)" else "Edit PDS Shop Details", color = BluePrimary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditAgencyDialog(
    agency: CookingAgencyEntity,
    isNew: Boolean,
    isHi: Boolean,
    onDismiss: () -> Unit,
    onSave: (CookingAgencyEntity) -> Unit
) {
    var name by remember { mutableStateOf(agency.name) }
    var contactPerson by remember { mutableStateOf(agency.contactPerson) }
    var mobile by remember { mutableStateOf(agency.mobile) }
    var address by remember { mutableStateOf(agency.address) }
    var villageOrCity by remember { mutableStateOf(agency.villageOrCity) }
    var block by remember { mutableStateOf(agency.block) }
    var district by remember { mutableStateOf(agency.district) }
    var bankName by remember { mutableStateOf(agency.bankName) }
    var branchName by remember { mutableStateOf(agency.branchName) }
    var accountNumber by remember { mutableStateOf(agency.maskedAccountNo) }
    var ifscCode by remember { mutableStateOf(agency.ifscCode) }
    var agreementOrderNo by remember { mutableStateOf(agency.agreementOrderNo) }
    var agreementStartDate by remember { mutableStateOf(agency.agreementStartDate) }
    var agreementEndDate by remember { mutableStateOf(agency.agreementEndDate) }
    var associatedSchools by remember { mutableStateOf(agency.associatedSchools) }
    var status by remember { mutableStateOf(agency.status) }
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
                text = if (isNew) (if (isHi) "नया स्व-सहायता समूह जोड़ें" else "Add New Cooking Agency")
                else (if (isHi) "स्व-सहायता समूह (SHG) विवरण संपादित करें" else "Edit SHG Agency Settings"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BluePrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isHi) "समूह का नाम (Agency Name) *" else "Agency / SHG Name *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = contactPerson,
                        onValueChange = { contactPerson = it },
                        label = { Text(if (isHi) "अध्यक्ष/सचिव का नाम *" else "Contact Person *") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text(if (isHi) "मोबाइल नंबर *" else "Mobile *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(if (isHi) "पता (Address)" else "Address") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = villageOrCity,
                        onValueChange = { villageOrCity = it },
                        label = { Text(if (isHi) "ग्राम / शहर *" else "Village / City *") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = block,
                        onValueChange = { block = it },
                        label = { Text(if (isHi) "विकासखंड (Block) *" else "Block *") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text(if (isHi) "जिला (District) *" else "District *") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text(if (isHi) "बैंक का नाम *" else "Bank Name *") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = branchName,
                        onValueChange = { branchName = it },
                        label = { Text(if (isHi) "शाखा (Branch)" else "Branch") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = ifscCode,
                        onValueChange = { ifscCode = it },
                        label = { Text(if (isHi) "IFSC कोड *" else "IFSC Code *") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text(if (isHi) "बैंक खाता नंबर *" else "Bank Account No *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = agreementOrderNo,
                        onValueChange = { agreementOrderNo = it },
                        label = { Text(if (isHi) "अनुबंध आदेश क्र." else "Agreement Order No") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = agreementEndDate,
                        onValueChange = { agreementEndDate = it },
                        label = { Text(if (isHi) "वैधता तिथि (YYYY-MM-DD)" else "End Date") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = associatedSchools,
                    onValueChange = { associatedSchools = it },
                    label = { Text(if (isHi) "संबद्ध शालाएं" else "Associated Schools") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ACTIVE", "INACTIVE", "EXPIRING_SOON").forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s, style = MaterialTheme.typography.labelSmall) }
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
                    val updated = agency.copy(
                        name = name.trim(),
                        contactPerson = contactPerson.trim(),
                        mobile = mobile.trim(),
                        address = address.trim(),
                        villageOrCity = villageOrCity.trim(),
                        block = block.trim(),
                        district = district.trim(),
                        bankName = bankName.trim(),
                        branchName = branchName.trim(),
                        maskedAccountNo = accountNumber.trim(),
                        ifscCode = ifscCode.trim(),
                        agreementOrderNo = agreementOrderNo.trim(),
                        agreementStartDate = agreementStartDate.trim(),
                        agreementEndDate = agreementEndDate.trim(),
                        associatedSchools = associatedSchools.trim(),
                        status = status
                    )
                    onSave(updated)
                },
                colors = poshanButtonColors(containerColor = BluePrimary)
            ) {
                Text(if (isHi) "सुरक्षित करें" else "Save SHG", fontWeight = FontWeight.Bold, color = Color.White)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPdsShopDialog(
    shop: PdsShopEntity,
    isNew: Boolean,
    isHi: Boolean,
    onDismiss: () -> Unit,
    onSave: (PdsShopEntity) -> Unit
) {
    var shopName by remember { mutableStateOf(shop.shopName) }
    var fpsNumber by remember { mutableStateOf(shop.fpsNumber) }
    var dealerName by remember { mutableStateOf(shop.dealerName) }
    var mobile by remember { mutableStateOf(shop.mobile) }
    var address by remember { mutableStateOf(shop.address) }
    var village by remember { mutableStateOf(shop.village) }
    var block by remember { mutableStateOf(shop.block) }
    var district by remember { mutableStateOf(shop.district) }
    var licenseNumber by remember { mutableStateOf(shop.licenseNumber) }
    var associatedSchool by remember { mutableStateOf(shop.associatedSchool) }
    var status by remember { mutableStateOf(shop.status) }
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
                text = if (isNew) (if (isHi) "नया PDS केंद्र जोड़ें" else "Add New PDS Shop")
                else (if (isHi) "PDS उचित मूल्य दुकान सेटिंग्स संपादित करें" else "Edit PDS Shop Settings"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BluePrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text(if (isHi) "दुकान / समिति का नाम *" else "Shop / Society Name *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dealerName,
                        onValueChange = { dealerName = it },
                        label = { Text(if (isHi) "विक्रेता का नाम *" else "Dealer Name *") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text(if (isHi) "मोबाइल नंबर (वैकल्पिक)" else "Mobile (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(if (isHi) "स्थान / पता" else "Address / Location") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = village,
                        onValueChange = { village = it },
                        label = { Text(if (isHi) "ग्राम / शहर *" else "Village *") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = block,
                        onValueChange = { block = it },
                        label = { Text(if (isHi) "विकासखंड (Block) *" else "Block *") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text(if (isHi) "जिला (District) *" else "District *") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = associatedSchool,
                        onValueChange = { associatedSchool = it },
                        label = { Text(if (isHi) "संबद्ध शाला" else "School") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ACTIVE", "INACTIVE").forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s, style = MaterialTheme.typography.labelSmall) }
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
                    val updated = shop.copy(
                        shopName = shopName.trim(),
                        fpsNumber = fpsNumber.trim(),
                        dealerName = dealerName.trim(),
                        mobile = mobile.trim(),
                        address = address.trim(),
                        village = village.trim(),
                        block = block.trim(),
                        district = district.trim(),
                        licenseNumber = licenseNumber.trim(),
                        associatedSchool = associatedSchool.trim(),
                        status = status
                    )
                    onSave(updated)
                },
                colors = poshanButtonColors(containerColor = BluePrimary)
            ) {
                Text(if (isHi) "सुरक्षित करें" else "Save PDS Shop", fontWeight = FontWeight.Bold, color = Color.White)
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
