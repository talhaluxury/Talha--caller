package com.talhastudio.callerid.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.talhastudio.callerid.ui.theme.*

/** Soft pearl page background shared by every screen. */
@Composable
fun PearlBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Pearl, PearlDeep))),
        content = content
    )
}

/** Frosted glass surface: translucent white, top sheen, thin silver border, soft shadow. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Int = 26,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(corner.dp)
    val base = modifier
        .shadow(14.dp, shape, ambientColor = Color(0x22000000), spotColor = Color(0x33000000))
        .clip(shape)
        .background(Brush.verticalGradient(listOf(Color(0xE6FFFFFF), Color(0x99FFFFFF))))
        .border(1.dp, Brush.verticalGradient(listOf(Color.White, Silver)), shape)
    val withClick = if (onClick != null) base.clickable(onClick = onClick) else base
    Column(withClick.padding(18.dp), content = content)
}

/** Round glass dial key. */
@Composable
fun GlassKey(label: String, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.92f else 1f, label = "key")
    Box(
        Modifier.size(68.dp).scale(s)
            .shadow(8.dp, CircleShape, ambientColor = Color(0x22000000), spotColor = Color(0x33000000))
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xCCF1F0EE))))
            .border(1.dp, Silver, CircleShape)
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(label, style = MaterialTheme.typography.headlineMedium) }
}

/** Raised black call button with a champagne ring. */
@Composable
fun CallButton(onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.94f else 1f, label = "call")
    Box(
        Modifier.size(84.dp).scale(s)
            .shadow(18.dp, CircleShape, ambientColor = Color(0x44000000), spotColor = Color(0x66000000))
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF4A4A4A), Color(0xFF050505))))
            .border(2.dp, Brush.verticalGradient(listOf(Champagne, Color(0xFF8C7444))), CircleShape)
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(Icons.Filled.Call, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(34.dp)) }
}

/** Common screen frame: back arrow, title, scrollable content slot. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenFrame(
    title: String,
    onBack: () -> Unit,
    snackbar: SnackbarHostState? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    PearlBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { snackbar?.let { SnackbarHost(it) } },
            topBar = {
                TopAppBar(
                    title = { Text(title, style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { pad ->
            Column(
                Modifier.padding(pad).padding(horizontal = 20.dp).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content
            )
        }
    }
}

@Composable
fun glassFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Champagne, unfocusedBorderColor = Silver,
    focusedLabelColor = Ink, cursorColor = Ink
)

/** Shows messages from a ViewModel in a snackbar. */
@Composable
fun ShowMessages(messages: kotlinx.coroutines.flow.Flow<String>, snack: SnackbarHostState) {
    LaunchedEffect(messages) { messages.collect { snack.showSnackbar(it) } }
}

@Composable
fun InkButton(text: String, enabled: Boolean = true, modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Color.White,
            disabledContainerColor = Silver, disabledContentColor = Color.White)
    ) { Text(text) }
}
