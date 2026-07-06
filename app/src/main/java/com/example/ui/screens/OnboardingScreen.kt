package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EmojiAvatar
import com.example.ui.components.StudySplitTextField
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudySplitViewModel

@Composable
fun OnboardingScreen(
    viewModel: StudySplitViewModel,
    onOnboardingComplete: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var hostel by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🎓") }
    var preAddRoommates by remember { mutableStateOf(true) }

    val emojis = listOf("🎓", "🍕", "💻", "🎸", "🏀", "🎨", "🧪", "🍿", "🎧", "🛹", "☕", "🐱")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(24.dp)
            .navigationBarsPadding()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = "StudySplit",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MintPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Track & split dorm expenses with zero friction",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // Form Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 24.dp)
            ) {
                // Large Avatar Preview
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(MintPrimary.copy(alpha = 0.25f), Color.Transparent)
                            )
                        )
                        .border(2.dp, MintPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = selectedEmoji, fontSize = 44.sp)
                }

                Text(
                    text = "Choose your avatar",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )

                // Emoji Picker
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(emojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (selectedEmoji == emoji) MintPrimary.copy(alpha = 0.2f) else SlateSurface)
                                .border(
                                    width = if (selectedEmoji == emoji) 1.5.dp else 0.5.dp,
                                    color = if (selectedEmoji == emoji) MintPrimary else SlateSurfaceAlt,
                                    shape = CircleShape
                                )
                                .clickable { selectedEmoji = emoji }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 18.sp)
                        }
                    }
                }

                // Inputs
                StudySplitTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Your Name",
                    modifier = Modifier.testTag("username_input")
                )

                StudySplitTextField(
                    value = hostel,
                    onValueChange = { hostel = it },
                    label = "University / Hostel Name (Optional)"
                )

                // Roommate Pre-addition Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SlateSurface)
                        .clickable { preAddRoommates = !preAddRoommates }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Add Roommates for Testing",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Instantly pre-adds Sarah 🎨, Alex 🎸, and Sofia 🧪",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = preAddRoommates,
                        onCheckedChange = { preAddRoommates = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = MintPrimary,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SlateSurfaceAlt
                        )
                    )
                }
            }

            // Footer Button
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        viewModel.createProfile(name, selectedEmoji, hostel) { myProfile ->
                            if (preAddRoommates) {
                                viewModel.createProfile("Sarah", "🎨", hostel)
                                viewModel.createProfile("Alex", "🎸", hostel)
                                viewModel.createProfile("Sofia", "🧪", hostel)
                            }
                            onOnboardingComplete()
                        }
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MintPrimary,
                    contentColor = ObsidianBg,
                    disabledContainerColor = SlateSurfaceAlt,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("submit_button")
            ) {
                Text(
                    text = "Let's Split!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
