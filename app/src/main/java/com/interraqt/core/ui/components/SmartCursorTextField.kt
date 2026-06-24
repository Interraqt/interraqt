package com.interraqt.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SmartCursorTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    maxLength: Int,
    focusRequester: FocusRequester,
    onFocusStateChange: (Boolean) -> Unit,
    emptyBoxTapSecondTrigger: Int,
    modifier: Modifier = Modifier,
    placeholderText: String,
    primaryColor: Color,
    surfaceColor: Color,
    textColor: Color,
    subTextColor: Color
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var showHandle by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var forceCursorToEnd by remember { mutableStateOf(false) }

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Function to force scroll so the absolute bottom line is fully visible
    fun forceScrollToEnd() {
        coroutineScope.launch {
            delay(50) // Wait for layout measurement
            onValueChange(value.copy(selection = TextRange(value.text.length)))
            delay(50) // Wait for text selection update
            bringIntoViewRequester.bringIntoView()
        }
    }

    // 🚨 2. SECOND TAP (Focused -> Jumps to end, SHOWS Handle)
    LaunchedEffect(emptyBoxTapSecondTrigger) {
        if (emptyBoxTapSecondTrigger > 0) {
            onValueChange(value.copy(selection = TextRange(value.text.length)))
            showHandle = true
            coroutineScope.launch { delay(100); bringIntoViewRequester.bringIntoView() }
        }
    }

    // 🚨 3. DIRECT TEXT TAP (Places exactly at finger, SHOWS Handle)
    LaunchedEffect(isPressed) {
        if (isPressed && isFocused && !forceCursorToEnd) {
            showHandle = true
            coroutineScope.launch { delay(100); bringIntoViewRequester.bringIntoView() }
        }
    }

    LaunchedEffect(showHandle, value.selection) { if (showHandle) { delay(10000); showHandle = false } }

    val customSelectionColors = TextSelectionColors(
        handleColor = if (showHandle || !value.selection.collapsed) primaryColor else Color.Transparent,
        backgroundColor = primaryColor.copy(alpha = 0.4f)
    )

    // 🚨 INVISIBLE OVERLAY HACK: Intercepts the *very first tap* so the native field doesn't steal the cursor!
    Box(modifier = modifier) {
        CompositionLocalProvider(LocalTextSelectionColors provides customSelectionColors) {
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    if (newValue.text.length <= maxLength) {
                        var finalValue = newValue
                        if (forceCursorToEnd) {
                            finalValue = finalValue.copy(selection = TextRange(finalValue.text.length))
                            forceCursorToEnd = false
                        }
                        if (finalValue.text != value.text) showHandle = false
                        onValueChange(finalValue)
                        coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                    }
                },
                interactionSource = interactionSource,
                maxLines = 6,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        if (state.isFocused && !isFocused) {
                            forceCursorToEnd = true
                            showHandle = false
                            coroutineScope.launch { delay(100); bringIntoViewRequester.bringIntoView() }
                            
                            coroutineScope.launch {
                                delay(50)
                                if (forceCursorToEnd) {
                                    onValueChange(value.copy(selection = TextRange(value.text.length)))
                                    forceCursorToEnd = false
                                }
                            }
                        }
                        if (!state.isFocused) {
                            showHandle = false
                            forceCursorToEnd = false
                        }
                        isFocused = state.isFocused
                        onFocusStateChange(state.isFocused)
                    },
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 24.sp, color = textColor),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = surfaceColor, focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = textColor, unfocusedTextColor = textColor, cursorColor = primaryColor
                ),
                placeholder = { Text(placeholderText, color = subTextColor) }
            )
        }
        
        // This is the magic. It sits invisibly over the text field ONLY when closed.
        if (!isFocused) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Tapping the text field natively triggers the "First Tap" logic!
                        focusRequester.requestFocus()
                        onFocusStateChange(true)
                        // Trigger the jump-to-end logic in the parent
                        onValueChange(value.copy(selection = TextRange(value.text.length)))
                    }
            )
        }
    }
}
