package com.example.gymlogger.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

public class Animations {
    public val fadeInMenu: EnterTransition = fadeIn(spring(Spring.DampingRatioLowBouncy)) +
            expandVertically(spring(Spring.DampingRatioLowBouncy));
    public val fadeOutMenu: ExitTransition = fadeOut(spring(Spring.DampingRatioLowBouncy)) +
            shrinkVertically(spring(Spring.DampingRatioLowBouncy));
    public val fadeInForm: EnterTransition = fadeIn(spring(Spring.DampingRatioNoBouncy)) +
            expandVertically(spring(Spring.DampingRatioNoBouncy));
    public val slideInVertically: EnterTransition = slideInVertically(spring(Spring.DampingRatioLowBouncy)) + expandVertically(spring(Spring.DampingRatioLowBouncy));
    public val slideOutVertically: ExitTransition = slideOutVertically(spring(Spring.DampingRatioLowBouncy)) +
            shrinkVertically(spring(Spring.DampingRatioLowBouncy));
}