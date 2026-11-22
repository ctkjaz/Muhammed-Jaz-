package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// ClinEdge Design System — Premium Monochrome + Acid Lime
// Pure Black #000000 · Pure White #FFFFFF · Acid Lime #C7FF4D
// ============================================================================

// Primary Brand Colours
val PureBlack = Color(0xFF000000)
val PureWhite = Color(0xFFFFFFFF)
val AcidLime = Color(0xFFC7FF4D)
val AcidLimePressed = Color(0xFFB5EC3B)
val AcidLimeMutedDark = Color(0xFF1E2610)
val AcidLimeMutedLight = Color(0xFFF3FCD9)

// Supporting Monochrome Scale
val Graphite = Color(0xFF171717)
val SecondaryGraphite = Color(0xFF1C1C1C)
val MediumGrey = Color(0xFF858585)
val LightGrey = Color(0xFFEAEAEA)
val BorderGrey = Color(0xFFE5E5E5)
val SoftSurfaceGrey = Color(0xFFF7F7F7)

// Light Mode Tokens
val LightBackground = PureWhite
val LightCardSurface = PureWhite
val LightElevatedSurface = SoftSurfaceGrey
val LightBorder = BorderGrey
val LightTextPrimary = Color(0xFF111111)
val LightTextSecondary = Color(0xFF737373)

// True AMOLED Dark Mode Tokens
val AmoledBackground = PureBlack
val AmoledCardPrimary = Color(0xFF111111)
val AmoledCardElevated = Color(0xFF1A1A1A)
val AmoledBorder = Color(0xFF292929)
val AmoledTextPrimary = Color(0xFFF5F5F5)
val AmoledTextSecondary = Color(0xFF999999)

// Restrained High-Contrast Semantic Feedback Colours (Answer reveal & CBT palette)
val SemanticCorrectFill = Color(0xFF16A34A)
val SemanticCorrectTextLight = Color(0xFF15803D)
val SemanticCorrectTextDark = Color(0xFF4ADE80)
val SemanticCorrectBgLight = Color(0xFFECFDF3)
val SemanticCorrectBgDark = Color(0xFF0B2214)

val SemanticWrongFill = Color(0xFFDC2626)
val SemanticWrongTextLight = Color(0xFFB91C1C)
val SemanticWrongTextDark = Color(0xFFF87171)
val SemanticWrongBgLight = Color(0xFFFEF2F2)
val SemanticWrongBgDark = Color(0xFF2A0F12)

val SemanticWarningFill = Color(0xFFD97706)
val SemanticWarningTextLight = Color(0xFFB45309)
val SemanticWarningTextDark = Color(0xFFFBBF24)

val SemanticReviewFill = Color(0xFF52525B)
val SemanticReviewTextLight = Color(0xFF3F3F46)
val SemanticReviewTextDark = Color(0xFFD4D4D8)

val UnvisitedNeutral = MediumGrey

// Legacy aliases mapped cleanly to Monochrome + Acid Lime tokens for non-UI callers
val CalmSageFill = SemanticCorrectFill
val MutedCrimsonFill = SemanticWrongFill
val WarmOchreFill = SemanticWarningFill
val MutedPlumFill = SemanticReviewFill

// Dark Hero / Score Banner Surface (Monochrome Graphite + Acid Lime KPI Accents)
val EditorialBannerSurface = Color(0xFF111111)
val EditorialBannerTitle = AmoledTextPrimary
val EditorialBannerSubtext = Color(0xFFD4D4D4)
val EditorialBannerMuted = AmoledTextSecondary
val EditorialBannerKpiSuccess = SemanticCorrectTextDark
val EditorialBannerKpiAccent = AcidLime
val EditorialBannerKpiHighlight = AcidLime
val EditorialBannerKpiDanger = SemanticWrongTextDark
