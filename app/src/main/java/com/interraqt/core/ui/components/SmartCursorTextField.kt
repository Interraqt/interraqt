package com.interraqt.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
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

    // 🚨 NEW: Synchronous flag catches the first touch without blocking scroll gestures!
    var pendingFocusTap by remember { mutableStateOf(false) }

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    // 🚨 SECOND TAP (Focused external tap -> Jumps to end, SHOWS Handle)
    LaunchedEffect(emptyBoxTapSecondTrigger) {
        if (emptyBoxTapSecondTrigger > 0) {
            onValueChange(value.copy(selection = TextRange(value.text.length)))
            showHandle = true
            coroutineScope.launch { delay(100); bringIntoViewRequester.bringIntoView() }
        }
    }

    // 🚨 DIRECT TEXT TAP (Places exactly at finger, SHOWS Handle)
    LaunchedEffect(isPressed) {
        if (isPressed && isFocused && !forceCursorToEnd && !pendingFocusTap) {
            showHandle = true
            coroutineScope.launch { delay(100); bringIntoViewRequester.bringIntoView() }
        }
    }

    LaunchedEffect(showHandle, value.selection) { if (showHandle) { delay(10000); showHandle = false } }

    val customSelectionColors = TextSelectionColors(
        handleColor = if (showHandle || !value.selection.collapsed) primaryColor else Color.Transparent,
        backgroundColor = primaryColor.copy(alpha = 0.4f)
    )

    // 🚨 REMOVED the invisible Box overlay completely. 
    // The text field is now entirely unobstructed, restoring perfect native scrolling.
    CompositionLocalProvider(LocalTextSelectionColors provides customSelectionColors) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                if (newValue.text.length <= maxLength) {
                    var finalValue = newValue
                    
                    // 🚨 SYNCHRONOUS OVERRIDE: Catches the native tap BEFORE it draws to the screen
                    if (pendingFocusTap || forceCursorToEnd) {
                        finalValue = finalValue.copy(selection = TextRange(finalValue.text.length))
                        pendingFocusTap = false
                        forceCursorToEnd = false
                        showHandle = false
                    } else if (finalValue.text != value.text) {
                        showHandle = false
                    }
                    
                    onValueChange(finalValue)
                    coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                }
            },
            interactionSource = interactionSource,
            maxLines = 6,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = modifier
                .bringIntoViewRequester(bringIntoViewRequester)
                .focusRequester(focusRequester)
                // 🚨 NEW LOGIC: Listens silently for the initial touch down without consuming scrolls
                .pointerInput(isFocused) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (!isFocused && event.changes.any { it.pressed }) {
                                pendingFocusTap = true
                            }
                        }
                    }
                }
                .onFocusChanged { state ->
                    if (state.isFocused && !isFocused) {
                        forceCursorToEnd = true
                        showHandle = false
                        coroutineScope.launch { delay(100); bringIntoViewRequester.bringIntoView() }
                        
                        // Fallback override in case the value change is delayed
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
                        pendingFocusTap = false
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
}
