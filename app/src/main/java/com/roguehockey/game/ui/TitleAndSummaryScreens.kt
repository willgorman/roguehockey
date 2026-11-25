package com.roguehockey.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roguehockey.game.engine.RelicId
import com.roguehockey.game.engine.RoundClearSummary
import com.roguehockey.game.engine.RunStats
import com.roguehockey.game.ui.theme.TabletopColors

@Composable
fun TitleScreen(
    tiltAvailable: Boolean,
    onStartRun: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), TabletopColors.ChassisDark, Color(0xFF1E3A5F))
                )
            )
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.widthIn(max = 540.dp)
        ) {
            // Tactile Puck Emblem
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(116.dp)
                    .clip(CircleShape)
                    .background(TabletopColors.IceCenter)
                    .border(6.dp, TabletopColors.AluminumRim, CircleShape)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(TabletopColors.ChassisDark)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(TabletopColors.Coral)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ROGUE HOCKEY",
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "TACTILE TABLETOP ROGUE-LITE AIR HOCKEY",
                    color = TabletopColors.Sky,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
            }

            // Frosted Tabletop Quick-Play Card
            Surface(
                color = TabletopColors.IceCenter,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(3.dp, TabletopColors.AluminumRim, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "HOW TO PLAY",
                        color = TabletopColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    RuleRow(
                        badge = "FLICK",
                        badgeColor = TabletopColors.Coral,
                        text = "Flick the puck directly or pull back to slingshot-aim with wall bounce preview."
                    )
                    RuleRow(
                        badge = "TILT",
                        badgeColor = TabletopColors.Sky,
                        text = if (tiltAvailable) {
                            "Tilt your tablet while the puck glides to curve around ice blocks & bank goals."
                        } else {
                            "Tilt steering supported automatically on tablets with gravity/accelerometer sensors."
                        }
                    )
                    RuleRow(
                        badge = "COMBO",
                        badgeColor = TabletopColors.Amber,
                        text = "Chain Bumpers (+Chips), Rings (+Mult), Crates, Cryo-Barrels & Power-Ups to beat the Target Score!"
                    )
                    RuleRow(
                        badge = "SHOP",
                        badgeColor = TabletopColors.Violet,
                        text = "Spend Coins & Interest between rounds on game-breaking Relics and Table Upgrades."
                    )
                }
            }

            Button(
                onClick = onStartRun,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TabletopColors.Coral,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
            ) {
                Text(
                    text = "DROP THE PUCK",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun RuleRow(badge: String, badgeColor: Color, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(badgeColor)
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = badge,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }
        Text(
            text = text,
            color = TabletopColors.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun RoundClearOverlay(
    summary: RoundClearSummary,
    onOpenShop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A).copy(alpha = 0.78f))
            .padding(24.dp)
    ) {
        Surface(
            color = TabletopColors.SurfaceCard,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .border(3.dp, TabletopColors.Emerald, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(TabletopColors.Emerald.copy(alpha = 0.14f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (summary.roundInAnte == 3) "BOSS TABLE CLEARED!" else "TABLE CLEARED!",
                        color = TabletopColors.Emerald,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "${summary.finalScore} / ${summary.targetScore}",
                    color = TabletopColors.TextPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black
                )

                HorizontalDivider(color = Color(0xFFE2E8F0))

                PayoutLine("Table Clear Reward", "+\$${summary.baseReward}")
                PayoutLine("Remaining Flicks (${summary.unusedFlicks})", "+\$${summary.flickBonus}")
                PayoutLine("Interest (+\$1 per \$5 held)", "+\$${summary.interestBonus}")

                HorizontalDivider(color = Color(0xFFE2E8F0))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL PAYOUT",
                        color = TabletopColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "+\$${summary.totalPayout}",
                        color = TabletopColors.GoldCoin,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onOpenShop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TabletopColors.Sky,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = "VISIT PRO SHOP  →",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun PayoutLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TabletopColors.TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text(text = value, color = TabletopColors.Amber, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun GameOverScreen(
    anteReached: Int,
    roundReached: Int,
    finalRoundScore: Int,
    targetScore: Int,
    stats: RunStats,
    relics: List<RelicId>,
    onNewRun: () -> Unit,
    onTitle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A).copy(alpha = 0.90f))
            .padding(24.dp)
    ) {
        Surface(
            color = TabletopColors.SurfaceCard,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .border(3.dp, TabletopColors.Coral, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "OUT OF FLICKS",
                    color = TabletopColors.Coral,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "RUN COMPLETE",
                    color = TabletopColors.TextPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Stopped at Ante $anteReached • Round $roundReached ($finalRoundScore / $targetScore)",
                    color = TabletopColors.TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                HorizontalDivider(color = Color(0xFFE2E8F0))

                StatRow("Rounds Cleared", "${stats.roundsCleared}")
                StatRow("Best Single Shot", "${stats.highestShotScore}")
                StatRow("Total Run Score", "${stats.totalScoreAllRounds}")
                StatRow("Top-Shelf Goals", "${stats.goalsScored}")
                StatRow("Obstacles Shattered", "${stats.obstaclesDestroyed}")

                if (relics.isNotEmpty()) {
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Text(
                        text = "RELICS: ${relics.joinToString(" • ") { it.title }}",
                        color = TabletopColors.Violet,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onTitle,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text("TITLE", fontWeight = FontWeight.ExtraBold)
                    }
                    Button(
                        onClick = onNewRun,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TabletopColors.Coral,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.6f)
                            .height(52.dp)
                    ) {
                        Text("NEW RUN", fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TabletopColors.TextSecondary, fontSize = 14.sp)
        Text(text = value, color = TabletopColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    }
}
