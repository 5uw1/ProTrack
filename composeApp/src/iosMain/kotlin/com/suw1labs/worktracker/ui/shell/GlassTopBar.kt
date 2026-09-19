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

internal const val TitleRowHeight = 44.0

/**
 * Title bar the way iOS 26 draws it. Two things happen here:
 *
 * * The glass keeps covering the status bar at all times, so content scrolling past the top edge
 *   blurs out instead of running sharp into the clock – the scroll edge effect.
 * * The title row itself slides away as soon as the content is scrolled down and comes back on the
 *   way up, so a long list gets the whole screen.
 *
 * Everything inside the bar is a native view: a `UIGlassEffect` has to sit above the Compose canvas
 * to have anything to refract, and Compose cannot paint on top of an interop overlay.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
@Composable
internal fun GlassTopBar(
    title: String,
    status: String?,
    alertCount: Int,
    titleShown: Boolean,
    hidden: Boolean,
    onAlertClick: () -> Unit,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val titleColor = scheme.onSurface.toUIColor()
    val statusColor = scheme.primary.toUIColor()
    val statusBackground = scheme.primary.copy(alpha = 0.14f).toUIColor()
    val alertColor = RoseUrgent.toUIColor()
    val barTint = scheme.surface.copy(alpha = 0.8f).toUIColor()
    val bar = remember { GlassTopBarViews() }

    Box(modifier = modifier.fillMaxWidth().height(statusBar + TitleRowHeight.dp)) {
        UIKitView(
            factory = { bar.build(statusBar.value.toDouble(), barTint) },
            modifier = Modifier.fillMaxWidth().height(statusBar + TitleRowHeight.dp),
            update = {
                bar.update(
                    title, status, alertCount, titleShown, hidden, onAlertClick,
                    titleColor, statusColor, statusBackground, alertColor,
                )
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
    private var root: UIView? = null
    private var glass: UIVisualEffectView? = null
    private var glassHeight: NSLayoutConstraint? = null
    private val titleRow = UIView()
    private val titleLabel = UILabel()
    private val statusLabel = UILabel()
    private val statusPill = UIView()
    private val alertButton = UIButton()
    private val alertBadge = UILabel()
    private val tapTarget = TopBarTapTarget()
    private var topInset = 0.0
    private var lastTitleShown = true

    fun build(topInset: Double, tint: UIColor): UIView {
        this.topInset = topInset
        val container = UIView()
        container.backgroundColor = UIColor.clearColor
        root = container

        val effect = UIGlassEffect.effectWithStyle(UIGlassEffectStyle.UIGlassEffectStyleRegular)
        // Tinted, or the bar disappears into busy content behind it.
        effect.setTintColor(tint)
        val bar = UIVisualEffectView(effect = effect)
        bar.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(bar)
        glass = bar
        val height = bar.heightAnchor.constraintEqualToConstant(topInset + TitleRowHeight)
        glassHeight = height
        NSLayoutConstraint.activateConstraints(
            listOf(
                bar.leadingAnchor.constraintEqualToAnchor(container.leadingAnchor),
                bar.trailingAnchor.constraintEqualToAnchor(container.trailingAnchor),
                bar.topAnchor.constraintEqualToAnchor(container.topAnchor),
                height,
            )
        )

        val content = bar.contentView
        titleRow.translatesAutoresizingMaskIntoConstraints = false
        content.addSubview(titleRow)
        NSLayoutConstraint.activateConstraints(
            listOf(
                titleRow.leadingAnchor.constraintEqualToAnchor(content.leadingAnchor),
                titleRow.trailingAnchor.constraintEqualToAnchor(content.trailingAnchor),
                titleRow.topAnchor.constraintEqualToAnchor(content.topAnchor, constant = topInset),
                titleRow.heightAnchor.constraintEqualToConstant(TitleRowHeight),
            )
        )

        titleLabel.font = UIFont.systemFontOfSize(17.0, weight = UIFontWeightBold)
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        titleRow.addSubview(titleLabel)

        statusPill.layer.cornerRadius = 8.0
        statusPill.clipsToBounds = true
        statusPill.translatesAutoresizingMaskIntoConstraints = false
        statusLabel.font = UIFont.systemFontOfSize(10.0, weight = UIFontWeightSemibold)
        statusLabel.translatesAutoresizingMaskIntoConstraints = false
        statusPill.addSubview(statusLabel)
        titleRow.addSubview(statusPill)

        alertButton.translatesAutoresizingMaskIntoConstraints = false
        alertButton.addTarget(tapTarget, action = NSSelectorFromString("alertTapped"), forControlEvents = UIControlEventTouchUpInside)
        titleRow.addSubview(alertButton)

        alertBadge.font = UIFont.systemFontOfSize(10.0, weight = UIFontWeightSemibold)
        alertBadge.textColor = UIColor.whiteColor
        alertBadge.textAlignment = NSTextAlignmentCenter
        alertBadge.layer.cornerRadius = 8.0
        alertBadge.clipsToBounds = true
        alertBadge.translatesAutoresizingMaskIntoConstraints = false
        titleRow.addSubview(alertBadge)

        NSLayoutConstraint.activateConstraints(
            listOf(
                titleLabel.leadingAnchor.constraintEqualToAnchor(titleRow.leadingAnchor, constant = 20.0),
                titleLabel.centerYAnchor.constraintEqualToAnchor(titleRow.centerYAnchor),
                statusPill.leadingAnchor.constraintEqualToAnchor(titleLabel.trailingAnchor, constant = 8.0),
                statusPill.centerYAnchor.constraintEqualToAnchor(titleLabel.centerYAnchor),
                statusPill.heightAnchor.constraintEqualToConstant(18.0),
                statusLabel.leadingAnchor.constraintEqualToAnchor(statusPill.leadingAnchor, constant = 7.0),
                statusLabel.trailingAnchor.constraintEqualToAnchor(statusPill.trailingAnchor, constant = -7.0),
                statusLabel.centerYAnchor.constraintEqualToAnchor(statusPill.centerYAnchor),
                alertButton.trailingAnchor.constraintEqualToAnchor(titleRow.trailingAnchor, constant = -16.0),
                alertButton.centerYAnchor.constraintEqualToAnchor(titleRow.centerYAnchor),
                alertButton.widthAnchor.constraintEqualToConstant(32.0),
                alertButton.heightAnchor.constraintEqualToConstant(32.0),
                alertBadge.centerXAnchor.constraintEqualToAnchor(alertButton.trailingAnchor, constant = -4.0),
                alertBadge.centerYAnchor.constraintEqualToAnchor(alertButton.topAnchor, constant = 6.0),
                alertBadge.heightAnchor.constraintEqualToConstant(16.0),
                alertBadge.widthAnchor.constraintGreaterThanOrEqualToConstant(16.0),
            )
        )
        return container
    }

    fun update(
        title: String,
        status: String?,
        alertCount: Int,
        titleShown: Boolean,
        hidden: Boolean,
        onAlertClick: () -> Unit,
        titleColor: UIColor,
        statusColor: UIColor,
        statusBackground: UIColor,
        alertColor: UIColor,
    ) {
        root?.let { view -> UIView.animateWithDuration(0.2) { view.alpha = if (hidden) 0.0 else 1.0 } }

        // Scrolled down: the title row goes and the glass shrinks to the status bar, which keeps
        // blurring whatever passes underneath it.
        if (titleShown != lastTitleShown) {
            lastTitleShown = titleShown
            glassHeight?.constant = if (titleShown) topInset + TitleRowHeight else topInset
            UIView.animateWithDuration(0.25) {
                titleRow.alpha = if (titleShown) 1.0 else 0.0
                root?.layoutIfNeeded()
            }
        }

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
