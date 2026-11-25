package com.roguehockey.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
import com.roguehockey.game.engine.RelicId
import com.roguehockey.game.engine.RunModifiers
import com.roguehockey.game.engine.ShopOffer
import com.roguehockey.game.engine.TableConstants
import com.roguehockey.game.ui.theme.TabletopColors

@Composable
fun ShopScreen(
    coins: Int,
    nextAnte: Int,
    nextRoundInAnte: Int,
    nextTargetScore: Int,
    nextBossModifier: BossModifier,
    modifiers: RunModifiers,
    offers: List<ShopOffer>,
    rerollCost: Int,
    onBuyOffer: (String) -> Unit,
    onSellRelic: (RelicId) -> Unit,
    onReroll: () -> Unit,
    onNextRound: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedOwnedRelic by remember { mutableStateOf<RelicId?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TabletopColors.ChassisDark)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Shop Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "PRO SHOP",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Build combos & synergies for the next table",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
            }

            Surface(
                color = Color(0xFF422006),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.border(2.dp, TabletopColors.GoldCoin, RoundedCornerShape(14.dp))
            ) {
                Text(
                    text = "\$$coins",
                    color = TabletopColors.GoldCoin,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }

        // Next Round Preview Card
        Surface(
            color = if (nextBossModifier != BossModifier.NONE) Color(0xFF2E1065) else TabletopColors.ChassisMid,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (nextRoundInAnte == 3) {
                            "UPCOMING: ANTE $nextAnte BOSS — ${nextBossModifier.title.uppercase()}"
                        } else {
                            "UPCOMING: ANTE $nextAnte • ROUND $nextRoundInAnte/3"
                        },
                        color = if (nextRoundInAnte == 3) Color(0xFFE9D5FF) else TabletopColors.Sky,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (nextBossModifier != BossModifier.NONE) {
                        Text(
                            text = nextBossModifier.description,
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TARGET SCORE",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$nextTargetScore",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Owned Relics Tray
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE RELICS (${modifiers.relics.size}/${TableConstants.MAX_RELIC_SLOTS})",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Flicks: ${modifiers.maxFlicks} • +${modifiers.chipBonusUpgrade} Chips • +${modifiers.multBonusUpgrade} Mult",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (slot in 0 until TableConstants.MAX_RELIC_SLOTS) {
                        val relic = modifiers.relics.getOrNull(slot)
                        val isSelected = relic != null && relic == selectedOwnedRelic
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (relic != null) Color(relic.rarity.accentHex).copy(alpha = 0.25f)
                                    else TabletopColors.ChassisMid.copy(alpha = 0.35f)
                                )
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = when {
                                        isSelected -> TabletopColors.GoldCoin
                                        relic != null -> Color(relic.rarity.accentHex)
                                        else -> Color(0xFF334155)
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable(enabled = relic != null) {
                                    selectedOwnedRelic = if (isSelected) null else relic
                                }
                                .padding(horizontal = 6.dp)
                        ) {
                            if (relic != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = relic.badge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = relic.title,
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                Text(
                                    text = "EMPTY",
                                    color = Color(0xFF475569),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                selectedOwnedRelic?.let { relic ->
                    val refund = (relic.cost / 2).coerceAtLeast(2)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(TabletopColors.ChassisMid)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = relic.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = relic.description,
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onSellRelic(relic)
                                selectedOwnedRelic = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TabletopColors.Coral,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("SELL +\$$refund", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        // 2x2 Grid of Shop Offers
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val rows = offers.chunked(2)
            for (rowOffers in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (offer in rowOffers) {
                        val isRelicFull = offer is ShopOffer.RelicOffer &&
                            modifiers.relics.size >= TableConstants.MAX_RELIC_SLOTS
                        val canBuy = !offer.purchased && coins >= offer.cost && !isRelicFull
                        ShopOfferCard(
                            offer = offer,
                            canBuy = canBuy,
                            isRelicFull = isRelicFull,
                            onBuy = { onBuyOffer(offer.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowOffers.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Bottom Actions: Reroll + Next Table
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onReroll,
                enabled = coins >= rerollCost,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
            ) {
                Text(
                    text = "REROLL (\$$rerollCost)",
                    color = if (coins >= rerollCost) Color.White else Color(0xFF64748B),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Button(
                onClick = onNextRound,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TabletopColors.Emerald,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1.6f)
                    .height(54.dp)
            ) {
                Text(
                    text = "NEXT TABLE  →",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun ShopOfferCard(
    offer: ShopOffer,
    canBuy: Boolean,
    isRelicFull: Boolean,
    onBuy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = when (offer) {
        is ShopOffer.RelicOffer -> Color(offer.relic.rarity.accentHex)
        is ShopOffer.UpgradeOffer -> TabletopColors.Amber
    }
    val typeLabel = when (offer) {
        is ShopOffer.RelicOffer -> "${offer.relic.rarity.label.uppercase()} RELIC"
        is ShopOffer.UpgradeOffer -> "TABLE UPGRADE"
    }

    Surface(
        color = if (offer.purchased) Color(0xFF1E293B) else TabletopColors.SurfaceCard,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .border(
                width = 2.dp,
                color = if (offer.purchased) Color(0xFF334155) else accent.copy(alpha = 0.7f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (offer.purchased) Color(0xFF334155) else accent)
                ) {
                    Text(
                        text = offer.badge,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = typeLabel,
                        color = if (offer.purchased) Color(0xFF64748B) else accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = offer.title,
                        color = if (offer.purchased) Color(0xFF94A3B8) else TabletopColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = offer.description,
                color = if (offer.purchased) Color(0xFF64748B) else TabletopColors.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                minLines = 2
            )

            Button(
                onClick = onBuy,
                enabled = canBuy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFCBD5E1),
                    disabledContentColor = Color(0xFF64748B)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                val btnText = when {
                    offer.purchased -> "SOLD"
                    isRelicFull -> "SLOTS FULL"
                    else -> "BUY  \$${offer.cost}"
                }
                Text(btnText, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
