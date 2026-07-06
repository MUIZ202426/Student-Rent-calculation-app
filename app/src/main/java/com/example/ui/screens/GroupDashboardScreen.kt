package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Expense
import com.example.data.model.ExpenseSplit
import com.example.data.model.Group
import com.example.data.model.Profile
import com.example.data.model.Settlement
import com.example.data.repository.StudySplitRepository
import com.example.data.repository.MemberBalance
import com.example.data.repository.SettlementSuggestion
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudySplitViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDashboardScreen(
    group: Group,
    viewModel: StudySplitViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Home, 1: Expenses, 2: Settle, 3: Insights

    val currentProfile by viewModel.currentProfile.collectAsState()
    val groupProfiles by viewModel.groupProfiles.collectAsState()
    val groupExpenses by viewModel.groupExpenses.collectAsState()
    val groupSettlements by viewModel.groupSettlements.collectAsState()
    val groupBalances by viewModel.groupBalances.collectAsState()
    val groupSuggestions by viewModel.groupSuggestions.collectAsState()

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }

    var showConfetti by remember { mutableStateOf(false) }

    // Reactively refresh group details
    LaunchedEffect(group.id) {
        viewModel.selectGroup(group)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = group.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Invite Code: ${group.inviteCode}",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MintPrimary
                            )
                        }
                    },
                    actions = {
                        // Quick switch active profile inside the dashboard
                        IconButton(
                            onClick = {
                                val nextIndex = (groupProfiles.indexOfFirst { it.id == currentProfile?.id } + 1) % groupProfiles.size
                                if (nextIndex >= 0 && nextIndex < groupProfiles.size) {
                                    viewModel.selectProfile(groupProfiles[nextIndex])
                                    Toast.makeText(context, "Swapped perspective to ${groupProfiles[nextIndex].name}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                EmojiAvatar(emoji = currentProfile?.avatarUrl ?: "👤", size = 28.dp, backgroundColor = SlateSurfaceAlt)
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(GreenSuccess)
                                        .border(1.dp, ObsidianBg, CircleShape)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ObsidianBg,
                        titleContentColor = TextPrimary
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = SlateSurface,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    val items = listOf(
                        Triple("Dashboard", Icons.Default.Home, Icons.Outlined.Home),
                        Triple("Expenses", Icons.Default.ReceiptLong, Icons.Outlined.ReceiptLong),
                        Triple("Settle Up", Icons.Default.Payments, Icons.Outlined.Payments),
                        Triple("Insights", Icons.Default.TrendingUp, Icons.Outlined.TrendingUp)
                    )

                    items.forEachIndexed { index, (label, filledIcon, outlinedIcon) ->
                        val isSelected = selectedTab == index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) filledIcon else outlinedIcon,
                                    contentDescription = label,
                                    tint = if (isSelected) MintPrimary else TextMuted
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = if (isSelected) MintPrimary else TextMuted,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = SlateSurfaceAlt
                            )
                        )
                    }
                }
            },
            floatingActionButton = {
                if (selectedTab == 1) { // Only show FAB on Expenses Tab
                    FloatingActionButton(
                        onClick = {
                            editingExpense = null
                            showAddExpenseDialog = true
                        },
                        containerColor = MintPrimary,
                        contentColor = ObsidianBg,
                        shape = CircleShape,
                        modifier = Modifier.testTag("add_expense_fab")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense")
                    }
                }
            },
            containerColor = ObsidianBg
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Crossfade(targetState = selectedTab, label = "TabTransition") { tab ->
                    when (tab) {
                        0 -> DashboardTab(
                            viewModel = viewModel,
                            groupExpenses = groupExpenses,
                            groupBalances = groupBalances,
                            currentProfile = currentProfile,
                            groupProfiles = groupProfiles,
                            onExploreMoreExpenses = { selectedTab = 1 }
                        )
                        1 -> ExpensesTab(
                            viewModel = viewModel,
                            groupExpenses = groupExpenses,
                            groupProfiles = groupProfiles,
                            currentProfile = currentProfile,
                            onEditExpense = { expense ->
                                editingExpense = expense
                                showAddExpenseDialog = true
                            }
                        )
                        2 -> SettleUpTab(
                            viewModel = viewModel,
                            currentProfile = currentProfile,
                            groupSuggestions = groupSuggestions,
                            groupSettlements = groupSettlements,
                            groupProfiles = groupProfiles,
                            onSettled = {
                                showConfetti = true
                            }
                        )
                        3 -> InsightsTab(
                            groupExpenses = groupExpenses,
                            groupProfiles = groupProfiles
                        )
                    }
                }
            }
        }

        // --- Visual Confetti Feedback ---
        ConfettiCelebration(isActive = showConfetti, onFinished = { showConfetti = false })

        // --- Add / Edit Expense Dialog ---
        if (showAddExpenseDialog) {
            AddEditExpenseDialog(
                expense = editingExpense,
                profiles = groupProfiles,
                currentProfile = currentProfile,
                onDismiss = {
                    showAddExpenseDialog = false
                    editingExpense = null
                },
                onSave = { title, amount, paidBy, category, notes, date, splits ->
                    if (editingExpense == null) {
                        viewModel.addExpense(title, amount, paidBy, category, notes, date, splits)
                    } else {
                        viewModel.updateExpense(editingExpense!!.id, title, amount, paidBy, category, notes, date, splits)
                    }
                    showAddExpenseDialog = false
                    editingExpense = null
                }
            )
        }
    }
}

// ==================== TAB 0: DASHBOARD ====================
@Composable
fun DashboardTab(
    viewModel: StudySplitViewModel,
    groupExpenses: List<Expense>,
    groupBalances: List<MemberBalance>,
    currentProfile: Profile?,
    groupProfiles: List<Profile>,
    onExploreMoreExpenses: () -> Unit
) {
    val myBalance = groupBalances.firstOrNull { it.profile.id == currentProfile?.id }
    val totalSpent = groupExpenses.sumOf { it.amount }

    // Slice category spendings
    val categoryTotals = groupExpenses.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Balance Card
        item {
            myBalance?.let { balance ->
                val isCreditor = balance.netBalance >= 0
                val netString = String.format(Locale.US, "$%.2f", Math.abs(balance.netBalance))

                StudySplitCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (isCreditor) GreenSuccess.copy(alpha = 0.3f) else SunsetOrange.copy(alpha = 0.3f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isCreditor) "You are owed" else "You owe",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = netString,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isCreditor) GreenSuccess else SunsetOrange
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = SlateSurfaceAlt)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Paid", fontSize = 10.sp, color = TextMuted)
                                Text(
                                    String.format(Locale.US, "$%.2f", balance.totalPaid),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MintPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Owed", fontSize = 10.sp, color = TextMuted)
                                Text(
                                    String.format(Locale.US, "$%.2f", balance.totalOwed),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SunsetOrange
                                )
                            }
                        }
                    }
                }
            }
        }

        // Spent Insights Card
        item {
            StudySplitCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Group Spent", fontSize = 11.sp, color = TextMuted)
                            Text(
                                String.format(Locale.US, "$%.2f", totalSpent),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SlateSurfaceAlt)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("This Month", fontSize = 10.sp, color = MintPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (categoryTotals.isNotEmpty()) {
                        Divider(color = SlateSurfaceAlt)
                        CustomPieChart(
                            slices = categoryTotals,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Recent Activity / Expenses List Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "See All",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MintPrimary,
                    modifier = Modifier.clickable { onExploreMoreExpenses() }
                )
            }
        }

        if (groupExpenses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No activities recorded", color = TextMuted, fontSize = 12.sp)
                }
            }
        } else {
            items(groupExpenses.take(3)) { expense ->
                val payer = groupProfiles.find { it.id == expense.paidBy }
                ExpenseItemRow(
                    expense = expense,
                    payer = payer,
                    viewModel = viewModel
                )
            }
        }
    }
}

// ==================== TAB 1: EXPENSES LIST ====================
@Composable
fun ExpensesTab(
    viewModel: StudySplitViewModel,
    groupExpenses: List<Expense>,
    groupProfiles: List<Profile>,
    currentProfile: Profile?,
    onEditExpense: (Expense) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedMemberFilter by remember { mutableStateOf<Long?>(null) }

    val categories = listOf("All", "Food", "Utility", "Internet", "Cleaning", "Rent", "Groceries", "Others")

    // Filtered Expenses
    val filteredExpenses = groupExpenses.filter { expense ->
        val matchesSearch = expense.title.contains(searchQuery, ignoreCase = true) ||
                expense.notes.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategoryFilter == "All" || expense.category.lowercase() == selectedCategoryFilter.lowercase()
        val matchesMember = selectedMemberFilter == null || expense.paidBy == selectedMemberFilter

        matchesSearch && matchesCategory && matchesMember
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Search & Filters Box
        Spacer(modifier = Modifier.height(12.dp))
        StudySplitTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = "Search expenses...",
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted) },
            modifier = Modifier.testTag("expense_search_input")
        )

        // Category Filter Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategoryFilter == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MintPrimary else SlateSurface)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MintPrimary else SlateSurfaceAlt,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedCategoryFilter = category }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = category,
                        fontSize = 12.sp,
                        color = if (isSelected) ObsidianBg else TextPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Member Filter Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                val isSelected = selectedMemberFilter == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MintPrimary else SlateSurface)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MintPrimary else SlateSurfaceAlt,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedMemberFilter = null }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Everyone",
                        fontSize = 12.sp,
                        color = if (isSelected) ObsidianBg else TextPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            items(groupProfiles) { profile ->
                val isSelected = selectedMemberFilter == profile.id
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MintPrimary else SlateSurface)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MintPrimary else SlateSurfaceAlt,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedMemberFilter = profile.id }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = profile.avatarUrl, fontSize = 12.sp)
                    Text(
                        text = profile.name,
                        fontSize = 12.sp,
                        color = if (isSelected) ObsidianBg else TextPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Expenses List
        if (filteredExpenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No matching expenses", color = TextMuted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredExpenses) { expense ->
                    val payer = groupProfiles.find { it.id == expense.paidBy }
                    ExpenseItemRow(
                        expense = expense,
                        payer = payer,
                        viewModel = viewModel,
                        onEdit = { onEditExpense(expense) },
                        onDelete = { viewModel.deleteExpense(expense) }
                    )
                }
            }
        }
    }
}

// ==================== TAB 2: SETTLE UP ====================
@Composable
fun SettleUpTab(
    viewModel: StudySplitViewModel,
    currentProfile: Profile?,
    groupSuggestions: List<SettlementSuggestion>,
    groupSettlements: List<Settlement>,
    groupProfiles: List<Profile>,
    onSettled: () -> Unit
) {
    var showRecordSettleDialog by remember { mutableStateOf<SettlementSuggestion?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Optimized Settlements",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )
        Text(
            text = "StudySplit automatically calculates the minimum cash flow to clear all debts. Zero awkward conversations.",
            fontSize = 11.sp,
            color = TextMuted
        )

        // Suggestion list
        if (groupSuggestions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SlateSurface)
                    .border(1.dp, SlateSurfaceAlt, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎉 All settled up!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MintPrimary)
                    Text("Everyone in the group is fully squared away.", fontSize = 12.sp, color = TextMuted, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(0.5f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groupSuggestions) { sug ->
                    StudySplitCard(
                        borderColor = SlateSurfaceAlt,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(text = sug.fromProfile.avatarUrl, fontSize = 16.sp)
                                    Text(
                                        text = sug.fromProfile.name,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Text("pays", color = TextSecondary, fontSize = 12.sp)
                                    Text(text = sug.toProfile.avatarUrl, fontSize = 16.sp)
                                    Text(
                                        text = sug.toProfile.name,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = String.format(Locale.US, "$%.2f", sug.amount),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MintPrimary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Share WhatsApp
                                IconButton(
                                    onClick = {
                                        val text = "Hey ${sug.fromProfile.name}, let's settle up via StudySplit! You owe ${sug.toProfile.name} \$${String.format(Locale.US, "%.2f", sug.amount)}."
                                        android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_TEXT, text)
                                            context.startActivity(android.content.Intent.createChooser(this, "Share Settlement Details"))
                                        }
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = TextMuted)
                                }

                                // Record settlement button
                                Button(
                                    onClick = { showRecordSettleDialog = sug },
                                    colors = ButtonDefaults.buttonColors(containerColor = MintPrimary, contentColor = ObsidianBg),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Settle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "Settlement History",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // Settlements history list
        if (groupSettlements.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f),
                contentAlignment = Alignment.Center
            ) {
                Text("No payments recorded yet", color = TextMuted, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(0.4f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(groupSettlements) { s ->
                    val payer = groupProfiles.find { it.id == s.paidBy }
                    val receiver = groupProfiles.find { it.id == s.paidTo }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SlateSurface)
                            .border(0.5.dp, SlateSurfaceAlt, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("💸", fontSize = 16.sp)
                            Column {
                                Text(
                                    text = "${payer?.name ?: "Someone"} paid ${receiver?.name ?: "Someone"}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                val formattedDate = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(s.date))
                                Text(text = formattedDate, fontSize = 10.sp, color = TextMuted)
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "$%.2f", s.amount),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = GreenSuccess
                            )
                            IconButton(
                                onClick = { viewModel.deleteSettlement(s) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = SunsetOrange, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Record Settlement Confirmation Dialog ---
    showRecordSettleDialog?.let { sug ->
        AlertDialog(
            onDismissRequest = { showRecordSettleDialog = null },
            title = { Text("Confirm Settlement", fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    text = "Did ${sug.fromProfile.name} pay ${sug.toProfile.name} \$${String.format(Locale.US, "%.2f", sug.amount)}? This will register a payment and update everyone's balances.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addSettlement(sug.fromProfile.id, sug.toProfile.id, sug.amount)
                        showRecordSettleDialog = null
                        onSettled()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess, contentColor = ObsidianBg)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordSettleDialog = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SlateSurface,
            titleContentColor = TextPrimary
        )
    }
}

// ==================== TAB 3: INSIGHTS ====================
@Composable
fun InsightsTab(
    groupExpenses: List<Expense>,
    groupProfiles: List<Profile>
) {
    val totalSpent = groupExpenses.sumOf { it.amount }

    // Badges calculation
    val payerTotals = groupExpenses.groupBy { it.paidBy }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    val biggestSpenderId = payerTotals.maxByOrNull { it.value }?.key
    val biggestSpenderProfile = groupProfiles.find { it.id == biggestSpenderId }

    val totalSpends = payerTotals.values.toList()
    val mostFrugalId = groupProfiles.map { it.id }.find { it !in payerTotals.keys }
        ?: payerTotals.minByOrNull { it.value }?.key
    val mostFrugalProfile = groupProfiles.find { it.id == mostFrugalId }

    // Streak calculation (days with tracked expenses)
    val uniqueDays = groupExpenses.map {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it.date))
    }.toSet().size

    // Category Breakdown
    val categoryTotals = groupExpenses.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                text = "Dorm Insights",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Track patterns, spending habits, and find out who's the ultimate sponsor of Dorm life.",
                fontSize = 11.sp,
                color = TextMuted
            )
        }

        // Streak Card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(SlateSurfaceAlt, SlateSurface)
                        )
                    )
                    .border(0.5.dp, MintPrimary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("🔥", fontSize = 36.sp)
                Column {
                    Text(
                        text = "$uniqueDays Active Days",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MintPrimary
                    )
                    Text(
                        text = "Keeping track of roommate bills with zero delay!",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Badge Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Biggest spender
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = BorderStroke(0.5.dp, SlateSurfaceAlt)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("👑 Spender", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                        EmojiAvatar(emoji = biggestSpenderProfile?.avatarUrl ?: "👤", size = 44.dp, backgroundColor = ObsidianBg)
                        Text(
                            text = biggestSpenderProfile?.name ?: "None yet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = String.format(Locale.US, "$%.2f", payerTotals[biggestSpenderId] ?: 0.0),
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                // Most Frugal
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = BorderStroke(0.5.dp, SlateSurfaceAlt)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("💡 Frugal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonBlue)
                        EmojiAvatar(emoji = mostFrugalProfile?.avatarUrl ?: "👤", size = 44.dp, backgroundColor = ObsidianBg)
                        Text(
                            text = mostFrugalProfile?.name ?: "None yet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = String.format(Locale.US, "$%.2f", payerTotals[mostFrugalId] ?: 0.0),
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // Category breakdown details
        item {
            Text(
                text = "Category Spent",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        if (categoryTotals.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No category records available", color = TextMuted, fontSize = 12.sp)
                }
            }
        } else {
            items(categoryTotals) { (cat, amount) ->
                val percentage = if (totalSpent > 0) ((amount / totalSpent) * 100).toInt() else 0

                StudySplitCard(borderColor = SlateSurfaceAlt) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CategoryIcon(category = cat, size = 36.dp)
                            Column {
                                Text(text = cat, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = "You spent $percentage% on $cat this month", fontSize = 10.sp, color = TextMuted)
                            }
                        }
                        Text(
                            text = String.format(Locale.US, "$%.2f", amount),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MintPrimary
                        )
                    }
                }
            }
        }
    }
}

// ==================== LIST ROWS ====================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpenseItemRow(
    expense: Expense,
    payer: Profile?,
    viewModel: StudySplitViewModel,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    var showEmojiSelection by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SlateSurface)
            .border(0.5.dp, SlateSurfaceAlt, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { onEdit?.invoke() },
                onLongClick = { showEmojiSelection = true }
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            CategoryIcon(category = expense.category, size = 38.dp)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = expense.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (expense.emojiReaction.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SlateSurfaceAlt)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(text = expense.emojiReaction, fontSize = 10.sp)
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Paid by ${payer?.name ?: "someone"}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Text(text = "•", color = TextMuted, fontSize = 10.sp)
                    val formattedDate = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(expense.date))
                    Text(text = formattedDate, fontSize = 11.sp, color = TextMuted)
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = String.format(Locale.US, "$%.2f", expense.amount),
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )

            if (onDelete != null) {
                IconButton(
                    onClick = { onDelete.invoke() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = SunsetOrange, modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    // --- Emoji reaction selector dialog ---
    if (showEmojiSelection) {
        Dialog(onDismissRequest = { showEmojiSelection = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, SlateSurfaceAlt)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("❤️", "😂", "👍", "👎", "😮", "💸").forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SlateSurfaceAlt)
                                .clickable {
                                    viewModel.reactToExpense(expense, emoji)
                                    showEmojiSelection = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==================== SUB-VIEW: ADD / EDIT EXPENSE DIALOG ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseDialog(
    expense: Expense?,
    profiles: List<Profile>,
    currentProfile: Profile?,
    onDismiss: () -> Unit,
    onSave: (String, Double, Long, String, String, Long, List<ExpenseSplit>) -> Unit
) {
    var title by remember { mutableStateOf(expense?.title ?: "") }
    var amountStr by remember { mutableStateOf(expense?.amount?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var paidBy by remember { mutableStateOf(expense?.paidBy ?: currentProfile?.id ?: 0L) }
    var category by remember { mutableStateOf(expense?.category ?: "Food") }
    var notes by remember { mutableStateOf(expense?.notes ?: "") }

    var splitOption by remember { mutableStateOf("Equally") } // Equally, Percentage, Custom, Exclude

    val categories = listOf("Food", "Utility", "Internet", "Cleaning", "Rent", "Groceries", "Others")
    val splitOptions = listOf("Equally", "Percentage", "Custom", "Exclude")

    // Key-value of profileId -> split representation (either % or exact dollar or boolean indicator)
    val splitMap = remember { mutableStateMapOf<Long, String>() }
    val excludeMap = remember { mutableStateMapOf<Long, Boolean>() }

    // Init splits if editing
    LaunchedEffect(expense, profiles) {
        profiles.forEach { p ->
            splitMap[p.id] = ""
            excludeMap[p.id] = true
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            border = BorderStroke(1.dp, SlateSurfaceAlt),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (expense == null) "Add Expense" else "Edit Expense",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                StudySplitTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Title (e.g. Pizza Night, Wi-Fi Bill)",
                    modifier = Modifier.testTag("expense_title_input")
                )

                StudySplitTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = "Amount ($)",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.testTag("expense_amount_input")
                )

                // Paid By Dropdown
                Text("Paid By", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(profiles) { profile ->
                        val isSelected = paidBy == profile.id
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MintPrimary else SlateSurfaceAlt)
                                .clickable { paidBy = profile.id }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = profile.avatarUrl, fontSize = 12.sp)
                            Text(
                                text = profile.name,
                                fontSize = 12.sp,
                                color = if (isSelected) ObsidianBg else TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Category Grid
                Text("Category", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = category.lowercase() == cat.lowercase()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MintPrimary else SlateSurfaceAlt)
                                .clickable { category = cat }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                color = if (isSelected) ObsidianBg else TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Split Options
                Text("Split Options", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    splitOptions.forEach { opt ->
                        val isSelected = splitOption == opt
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MintPrimary else SlateSurfaceAlt)
                                .clickable { splitOption = opt }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = opt,
                                fontSize = 10.sp,
                                color = if (isSelected) ObsidianBg else TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Custom split input depending on option
                val totalAmount = amountStr.toDoubleOrNull() ?: 0.0

                when (splitOption) {
                    "Equally" -> {
                        val share = if (profiles.isNotEmpty()) totalAmount / profiles.size else 0.0
                        Text(
                            text = "Everyone pays an equal share of \$${String.format(Locale.US, "%.2f", share)}.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    "Percentage" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Set percentages for members (must sum to 100%):", fontSize = 10.sp, color = TextMuted)
                            profiles.forEach { p ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = p.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = splitMap[p.id] ?: "",
                                        onValueChange = { splitMap[p.id] = it },
                                        placeholder = { Text("0%") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MintPrimary,
                                            unfocusedBorderColor = SlateSurfaceAlt,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .width(70.dp)
                                            .height(44.dp),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                                    )
                                }
                            }
                        }
                    }
                    "Custom" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Specify exact dollar amounts for each member:", fontSize = 10.sp, color = TextMuted)
                            profiles.forEach { p ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = p.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = splitMap[p.id] ?: "",
                                        onValueChange = { splitMap[p.id] = it },
                                        placeholder = { Text("$0.00") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MintPrimary,
                                            unfocusedBorderColor = SlateSurfaceAlt,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .width(80.dp)
                                            .height(44.dp),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                                    )
                                }
                            }
                        }
                    }
                    "Exclude" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Select who to include in this expense (amount splits equally among checked):", fontSize = 10.sp, color = TextMuted)
                            profiles.forEach { p ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { excludeMap[p.id] = !(excludeMap[p.id] ?: true) }
                                ) {
                                    Text(text = p.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Checkbox(
                                        checked = excludeMap[p.id] ?: true,
                                        onCheckedChange = { excludeMap[p.id] = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MintPrimary,
                                            uncheckedColor = SlateSurfaceAlt,
                                            checkmarkColor = ObsidianBg
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                StudySplitTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Notes (Optional)"
                )

                // Save / Cancel Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank() && totalAmount > 0.0) {
                                // Calculate Splits
                                val finalSplits = mutableListOf<ExpenseSplit>()
                                when (splitOption) {
                                    "Equally" -> {
                                        val share = totalAmount / profiles.size
                                        profiles.forEach { p ->
                                            finalSplits.add(ExpenseSplit(expenseId = 0L, profileId = p.id, amountOwed = share))
                                        }
                                    }
                                    "Percentage" -> {
                                        profiles.forEach { p ->
                                            val pct = (splitMap[p.id] ?: "").toDoubleOrNull() ?: 0.0
                                            val share = (pct / 100.0) * totalAmount
                                            finalSplits.add(ExpenseSplit(expenseId = 0L, profileId = p.id, amountOwed = share))
                                        }
                                    }
                                    "Custom" -> {
                                        profiles.forEach { p ->
                                            val share = (splitMap[p.id] ?: "").toDoubleOrNull() ?: 0.0
                                            finalSplits.add(ExpenseSplit(expenseId = 0L, profileId = p.id, amountOwed = share))
                                        }
                                    }
                                    "Exclude" -> {
                                        val included = profiles.filter { excludeMap[it.id] ?: true }
                                        if (included.isNotEmpty()) {
                                            val share = totalAmount / included.size
                                            included.forEach { p ->
                                                finalSplits.add(ExpenseSplit(expenseId = 0L, profileId = p.id, amountOwed = share))
                                            }
                                        }
                                    }
                                }
                                onSave(title, totalAmount, paidBy, category, notes, System.currentTimeMillis(), finalSplits)
                            }
                        },
                        enabled = title.isNotBlank() && totalAmount > 0.0,
                        colors = ButtonDefaults.buttonColors(containerColor = MintPrimary, contentColor = ObsidianBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("dialog_expense_save")
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
