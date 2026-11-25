package com.roguehockey.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roguehockey.game.engine.BossModifier
import com.roguehockey.game.engine.ControlMode
import com.roguehockey.game.engine.RelicId
import com.roguehockey.game.engine.ShotPhase
import com.roguehockey.game.engine.ShotScoreState
import com.roguehockey.game.engine.TableConstants
import com.roguehockey.game.engine.TiltSensitivity
import com.roguehockey.game.ui.theme.TabletopColors

@Composable
fun TopGameHud(
    ante: Int,
    roundInAnte: Int,
    bossModifier: BossModifier,
    roundScore: Int,
    targetScore: Int,
    coins: Int,
    flicksRemaining: Int,
    maxFlicks: Int,
    shotScore: ShotScoreState,
    relics: List<RelicId>,
    muted: Boolean,
    onToggleMute: () -> Unit,
    onCalibrateTilt: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inspectedRelic by remember { mutableStateOf<RelicId?>(null) }
    val progress = (roundScore.toFloat() / targetScore.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TabletopColors.ChassisDark)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Ante/Round + Score Progress + Coins + Quick Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Ante & Round Badge
            Surface(
                color = if (bossModifier != BossModifier.NONE) TabletopColors.Violet else TabletopColors.ChassisMid,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ANTE $ante",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (roundInAnte == 3) "BOSS" else "RND $roundInAnte/3",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Score Progress Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TabletopColors.ChassisMid)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$roundScore",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "TARGET $targetScore",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (progress >= 1f) TabletopColors.Emerald else TabletopColors.Sky,
                    trackColor = Color(0xFF0F172A)
                )
            }

            // Coins Pill
            Surface(
                color = Color(0xFF422006),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.border(1.5.dp, TabletopColors.GoldCoin, RoundedCornerShape(12.dp))
            ) {
                Text(
                    text = "\$$coins",
                    color = TabletopColors.GoldCoin,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            // Calibrate Tilt & Mute buttons
            IconButton(
                onClick = onCalibrateTilt,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(TabletopColors.ChassisMid)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Calibrate Tilt Zero",
                    tint = TabletopColors.Sky,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(TabletopColors.ChassisMid)
            ) {
                Icon(
                    imageVector = if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Toggle Sound",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Row 2: Balatro-style Chips x Mult + Remaining Flicks + Relics Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Chips x Mult Formula Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TabletopColors.Sky)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${shotScore.chips}",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
                Text(
                    text = " × ",
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TabletopColors.Coral)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${shotScore.mult}",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
                if (shotScore.previewTotal > 0) {
                    Text(
                        text = " = ${shotScore.previewTotal}",
                        color = TabletopColors.Amber,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            // Flicks Remaining Pips
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(TabletopColors.ChassisMid)
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "FLICKS",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(3.dp))
                repeat(maxFlicks) { i ->
                    val active = i < flicksRemaining
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(if (active) TabletopColors.Coral else Color(0xFF0F172A))
                            .border(
                                width = 1.dp,
                                color = if (active) Color.White else Color(0xFF475569),
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Active Relics Mini-Badges (up to 5)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                for (slot in 0 until TableConstants.MAX_RELIC_SLOTS) {
                    val relic = relics.getOrNull(slot)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (relic != null) Color(relic.rarity.accentHex) else Color(0xFF0F172A)
                            )
                            .border(
                                width = 1.dp,
                                color = if (relic != null) Color.White.copy(alpha = 0.6f) else Color(0xFF334155),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = relic != null) {
                                inspectedRelic = if (inspectedRelic == relic) null else relic
                            }
                    ) {
                        Text(
                            text = relic?.badge ?: "·",
                            color = if (relic != null) Color.White else Color(0xFF475569),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // Optional Boss Modifier banner or Relic Tooltip
        if (inspectedRelic != null) {
            val r = inspectedRelic!!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(r.rarity.accentHex).copy(alpha = 0.22f))
                    .border(1.dp, Color(r.rarity.accentHex), RoundedCornerShape(10.dp))
                    .clickable { inspectedRelic = null }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${r.title}: ${r.description}",
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "✕",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        } else if (bossModifier != BossModifier.NONE) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TabletopColors.Violet.copy(alpha = 0.25f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BOSS MODIFIER — ${bossModifier.title}: ",
                    color = Color(0xFFE9D5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = bossModifier.description,
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun BottomControlBar(
    controlMode: ControlMode,
    tiltSensitivity: TiltSensitivity,
    shotPhase: ShotPhase,
    onCycleControlMode: () -> Unit,
    onCycleTiltSensitivity: () -> Unit,
    onRecallPuck: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(TabletopColors.ChassisDark)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Control Mode pill
        Surface(
            color = TabletopColors.ChassisMid,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.clickable { onCycleControlMode() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INPUT: ",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = controlMode.label.uppercase(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Tilt Sensitivity pill
        Surface(
            color = TabletopColors.ChassisMid,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.clickable { onCycleTiltSensitivity() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TILT: ",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tiltSensitivity.label.uppercase(),
                    color = if (tiltSensitivity == TiltSensitivity.OFF) Color(0xFF94A3B8) else TabletopColors.Sky,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Hint or Recall button
        if (shotPhase == ShotPhase.IN_PLAY) {
            Button(
                onClick = onRecallPuck,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TabletopColors.Amber,
                    contentColor = Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "BANK SHOT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        } else {
            Text(
                text = when (controlMode) {
                    ControlMode.AUTO -> "Flick puck or pull back to aim • Tilt tablet to curve"
                    ControlMode.FLICK -> "Swipe fast to flick puck • Tilt tablet to curve"
                    ControlMode.SLINGSHOT -> "Pull back anywhere to aim • Tilt tablet to curve"
                },
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
