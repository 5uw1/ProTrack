package com.suw1labs.worktracker.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.suw1labs.worktracker.ui.theme.RoseUrgent
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSSelectorFromString
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.UIAccessibilityTraitButton
import platform.UIKit.UIButton
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIFont
import platform.UIKit.UIFontWeightBold
import platform.UIKit.UIFontWeightSemibold
import platform.UIKit.UIGlassEffect
import platform.UIKit.UIGlassEffectStyle
import platform.UIKit.UIImage
import platform.UIKit.UIImageSymbolConfiguration
import platform.UIKit.UILabel
import platform.UIKit.UIView
import platform.UIKit.UIVisualEffectView
import platform.UIKit.setAccessibilityLabel
import platform.UIKit.setAccessibilityTraits
import platform.darwin.NSObject

private const val TitleBarHeight = 44

/**
 * Title bar the way iOS 26 draws it: glass across the full width including the status bar, with
 * the content scrolling underneath it. Because a `UIGlassEffect` view has to sit above the Compose
 * canvas to have anything to refract, everything inside it – title, status badge, alert button –
 * is a native view as well.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
@Composable
internal fun GlassTopBar(
    title: String,
    status: String?,
    alertCount: Int,
    onAlertClick: () -> Unit,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val titleColor = scheme.onSurface.toUIColor()
    val statusColor = scheme.primary.toUIColor()
    val statusBackground = scheme.primary.copy(alpha = 0.14f).toUIColor()
    val alertColor = RoseUrgent.toUIColor()
    val barTint = scheme.surface.copy(alpha = 0.55f).toUIColor()
    val bar = remember { GlassTopBarViews() }

    Box(modifier = modifier.fillMaxWidth().height(statusBar + TitleBarHeight.dp)) {
        UIKitView(
            factory = { bar.build(statusBar.value.toDouble(), barTint) },
            modifier = Modifier.fillMaxWidth().height(statusBar + TitleBarHeight.dp),
            update = {
                bar.update(title, status, alertCount, onAlertClick, titleColor, statusColor, statusBackground, alertColor)
            },
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
                placedAsOverlay = true,
            ),
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private class GlassTopBarViews {
    private val titleLabel = UILabel()
    private val statusLabel = UILabel()
    private val statusPill = UIView()
    private val alertButton = UIButton()
    private val alertBadge = UILabel()
    private val tapTarget = TopBarTapTarget()

    fun build(topInset: Double, tint: UIColor): UIVisualEffectView {
        val effect = UIGlassEffect.effectWithStyle(UIGlassEffectStyle.UIGlassEffectStyleRegular)
        // Same frosting as the tab bar, so the title stays readable over scrolled content.
        effect.setTintColor(tint)
        val bar = UIVisualEffectView(effect = effect)
        val content = bar.contentView

        titleLabel.font = UIFont.systemFontOfSize(17.0, weight = UIFontWeightBold)
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        content.addSubview(titleLabel)

        statusPill.layer.cornerRadius = 8.0
        statusPill.clipsToBounds = true
        statusPill.translatesAutoresizingMaskIntoConstraints = false
        statusLabel.font = UIFont.systemFontOfSize(10.0, weight = UIFontWeightSemibold)
        statusLabel.translatesAutoresizingMaskIntoConstraints = false
        statusPill.addSubview(statusLabel)
        content.addSubview(statusPill)

        alertButton.translatesAutoresizingMaskIntoConstraints = false
        alertButton.addTarget(tapTarget, action = NSSelectorFromString("alertTapped"), forControlEvents = UIControlEventTouchUpInside)
        content.addSubview(alertButton)

        alertBadge.font = UIFont.systemFontOfSize(10.0, weight = UIFontWeightSemibold)
        alertBadge.textColor = UIColor.whiteColor
        alertBadge.textAlignment = NSTextAlignmentCenter
        alertBadge.layer.cornerRadius = 8.0
        alertBadge.clipsToBounds = true
        alertBadge.translatesAutoresizingMaskIntoConstraints = false
        content.addSubview(alertBadge)

        val centreY = topInset + TitleBarHeight / 2.0
        NSLayoutConstraint.activateConstraints(
            listOf(
                titleLabel.leadingAnchor.constraintEqualToAnchor(content.leadingAnchor, constant = 20.0),
                titleLabel.topAnchor.constraintEqualToAnchor(content.topAnchor, constant = centreY - 11.0),
                statusPill.leadingAnchor.constraintEqualToAnchor(titleLabel.trailingAnchor, constant = 8.0),
                statusPill.centerYAnchor.constraintEqualToAnchor(titleLabel.centerYAnchor),
                statusPill.heightAnchor.constraintEqualToConstant(18.0),
                statusLabel.leadingAnchor.constraintEqualToAnchor(statusPill.leadingAnchor, constant = 7.0),
                statusLabel.trailingAnchor.constraintEqualToAnchor(statusPill.trailingAnchor, constant = -7.0),
                statusLabel.centerYAnchor.constraintEqualToAnchor(statusPill.centerYAnchor),
                alertButton.trailingAnchor.constraintEqualToAnchor(content.trailingAnchor, constant = -16.0),
                alertButton.centerYAnchor.constraintEqualToAnchor(titleLabel.centerYAnchor),
                alertButton.widthAnchor.constraintEqualToConstant(32.0),
                alertButton.heightAnchor.constraintEqualToConstant(32.0),
                alertBadge.centerXAnchor.constraintEqualToAnchor(alertButton.trailingAnchor, constant = -4.0),
                alertBadge.centerYAnchor.constraintEqualToAnchor(alertButton.topAnchor, constant = 6.0),
                alertBadge.heightAnchor.constraintEqualToConstant(16.0),
                alertBadge.widthAnchor.constraintGreaterThanOrEqualToConstant(16.0),
            )
        )
        return bar
    }

    fun update(
        title: String,
        status: String?,
        alertCount: Int,
        onAlertClick: () -> Unit,
        titleColor: UIColor,
        statusColor: UIColor,
        statusBackground: UIColor,
        alertColor: UIColor,
    ) {
        titleLabel.text = title
        titleLabel.textColor = titleColor

        statusPill.hidden = status == null
        statusLabel.text = status
        statusLabel.textColor = statusColor
        statusPill.backgroundColor = statusBackground

        val symbols = UIImageSymbolConfiguration.configurationWithPointSize(18.0)
        alertButton.setImage(UIImage.systemImageNamed("bell.badge.fill", withConfiguration = symbols), forState = 0u)
        alertButton.tintColor = alertColor
        alertButton.hidden = alertCount <= 0
        alertButton.setAccessibilityLabel(title)
        alertButton.setAccessibilityTraits(UIAccessibilityTraitButton)
        alertBadge.hidden = alertCount <= 0
        alertBadge.text = if (alertCount > 0) " $alertCount " else null
        alertBadge.backgroundColor = alertColor
        tapTarget.onTap = onAlertClick
    }
}

private class TopBarTapTarget : NSObject() {
    var onTap: () -> Unit = {}

    @ObjCAction
    fun alertTapped() = onTap()
}

private fun Color.toUIColor(): UIColor =
    UIColor(red = red.toDouble(), green = green.toDouble(), blue = blue.toDouble(), alpha = alpha.toDouble())
