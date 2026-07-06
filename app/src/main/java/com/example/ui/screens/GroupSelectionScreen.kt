package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import com.example.data.model.Group
import com.example.data.model.Profile
import com.example.ui.components.EmojiAvatar
import com.example.ui.components.StudySplitCard
import com.example.ui.components.StudySplitTextField
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudySplitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSelectionScreen(
    viewModel: StudySplitViewModel,
    onGroupSelected: (Group) -> Unit
) {
    val currentProfile by viewModel.currentProfile.collectAsState()
    val allProfiles by viewModel.allProfiles.collectAsState()
    val allGroups by viewModel.allGroups.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    var newGroupName by remember { mutableStateOf("") }
    var inviteCodeInput by remember { mutableStateOf("") }
    var joinError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header with User Info and Profile Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    currentProfile?.let { profile ->
                        EmojiAvatar(
                            emoji = profile.avatarUrl,
                            size = 50.dp,
                            backgroundColor = SlateSurface,
                            onClick = { showProfileDialog = true }
                        )
                        Column {
                            Text(
                                text = "Hey, ${profile.name}! 👋",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (profile.universityHostel.isNotEmpty()) {
                                Text(
                                    text = profile.universityHostel,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                // Profile switcher pill
                Button(
                    onClick = { showProfileDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SlateSurfaceAlt,
                        contentColor = MintPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Swap User", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "Your Shared Groups",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Groups list
            if (allGroups.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(SlateSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = MintPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = "No Shared Groups Yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Create a roommates group or join using a code to start splitting expenses.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(allGroups) { group ->
                        StudySplitCard(
                            onClick = { onGroupSelected(group) },
                            borderColor = SlateSurfaceAlt,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MintPrimary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MeetingRoom,
                                            contentDescription = null,
                                            tint = MintPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = group.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Code: ${group.inviteCode}",
                                            fontSize = 12.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Options Buttons (Create & Join)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MintPrimary,
                        contentColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("create_group_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create Group", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showJoinDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SlateSurface,
                        contentColor = TextPrimary
                    ),
                    border = BorderStroke(1.dp, SlateSurfaceAlt),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("join_group_button")
                ) {
                    Text("Join Group", fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- Create Group Dialog ---
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = {
                    showCreateDialog = false
                    newGroupName = ""
                },
                title = { Text("Create New Group", fontWeight = FontWeight.ExtraBold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Give your roommates group a name (e.g. Dorm 402, Sunset Villa).", fontSize = 12.sp, color = TextSecondary)
                        StudySplitTextField(
                            value = newGroupName,
                            onValueChange = { newGroupName = it },
                            label = "Group Name",
                            modifier = Modifier.testTag("group_name_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newGroupName.isNotBlank()) {
                                viewModel.createGroup(newGroupName) { group ->
                                    onGroupSelected(group)
                                }
                                showCreateDialog = false
                                newGroupName = ""
                            }
                        },
                        enabled = newGroupName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MintPrimary, contentColor = ObsidianBg),
                        modifier = Modifier.testTag("dialog_create_confirm")
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = SlateSurface,
                titleContentColor = TextPrimary
            )
        }

        // --- Join Group Dialog ---
        if (showJoinDialog) {
            AlertDialog(
                onDismissRequest = {
                    showJoinDialog = false
                    inviteCodeInput = ""
                    joinError = null
                },
                title = { Text("Join Group", fontWeight = FontWeight.ExtraBold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Enter the 6-character group invite code.", fontSize = 12.sp, color = TextSecondary)
                        StudySplitTextField(
                            value = inviteCodeInput,
                            onValueChange = {
                                inviteCodeInput = it.take(6)
                                joinError = null
                            },
                            label = "Invite Code",
                            modifier = Modifier.testTag("invite_code_input")
                        )
                        joinError?.let {
                            Text(it, color = SunsetOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (inviteCodeInput.length == 6) {
                                viewModel.joinGroup(inviteCodeInput) { success, group ->
                                    if (success && group != null) {
                                        onGroupSelected(group)
                                        showJoinDialog = false
                                        inviteCodeInput = ""
                                    } else {
                                        joinError = "Invalid group invite code"
                                    }
                                }
                            }
                        },
                        enabled = inviteCodeInput.length == 6,
                        colors = ButtonDefaults.buttonColors(containerColor = MintPrimary, contentColor = ObsidianBg),
                        modifier = Modifier.testTag("dialog_join_confirm")
                    ) {
                        Text("Join")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showJoinDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = SlateSurface,
                titleContentColor = TextPrimary
            )
        }

        // --- Swap Profile Dialog ---
        if (showProfileDialog) {
            Dialog(onDismissRequest = { showProfileDialog = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = BorderStroke(1.dp, SlateSurfaceAlt),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Swap Active Profile",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Select who you are right now. This is perfect for testing splits, debts, and settlements from each member's perspective!",
                            fontSize = 11.sp,
                            color = TextMuted
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.heightIn(max = 250.dp)
                        ) {
                            items(allProfiles) { profile ->
                                val isSelected = currentProfile?.id == profile.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) MintPrimary.copy(alpha = 0.15f) else SlateSurfaceAlt)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.dp,
                                            color = if (isSelected) MintPrimary else Color.Transparent,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            viewModel.selectProfile(profile)
                                            showProfileDialog = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    EmojiAvatar(emoji = profile.avatarUrl, size = 36.dp, backgroundColor = ObsidianBg)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = profile.name + if (isSelected) " (You)" else "",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        if (profile.universityHostel.isNotEmpty()) {
                                            Text(
                                                text = profile.universityHostel,
                                                fontSize = 10.sp,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { showProfileDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateSurfaceAlt, contentColor = TextPrimary),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
