package com.suw1labs.worktracker.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
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
import platform.UIKit.UIAccessibilityTraitSelected
import platform.UIKit.UIButton
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIFont
import platform.UIKit.UIFontWeightMedium
import platform.UIKit.UIFontWeightSemibold
import platform.UIKit.UIGlassEffect
import platform.UIKit.UIGlassEffectStyle
import platform.UIKit.UIImage
import platform.UIKit.UIImageSymbolConfiguration
import platform.UIKit.UIImageView
import platform.UIKit.UILabel
import platform.UIKit.UILayoutConstraintAxisHorizontal
import platform.UIKit.UILayoutConstraintAxisVertical
import platform.UIKit.UIStackView
import platform.UIKit.UIStackViewAlignmentCenter
import platform.UIKit.UIStackViewAlignmentFill
import platform.UIKit.UIStackViewDistributionFillEqually
import platform.UIKit.UIView
import platform.UIKit.UIViewContentMode
import platform.UIKit.UIVisualEffectView
import platform.UIKit.setAccessibilityLabel
import platform.UIKit.setAccessibilityTraits
import platform.UIKit.setIsAccessibilityElement
import platform.darwin.NSObject

private const val BarHeightDp = 58
private const val CornerRadius = 26.0
private const val SelectedCornerRadius = 20.0

/**
 * The tab bar as iOS 26 draws it: a Liquid Glass capsule floating above the bottom edge. The bar
 * is a real `UIVisualEffectView` with a `UIGlassEffect`, placed as an interop overlay so the glass
 * samples and refracts the app content scrolling underneath it – which is what Compose cannot
 * paint itself. Icons are SF Symbols, so they match the rest of the system.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
@Composable
internal fun GlassTabBar(tabs: List<ShellTab>, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val selectedColor = scheme.primary.toUIColor()
    val normalColor = scheme.onSurfaceVariant.toUIColor()
    val selectedBackground = scheme.primary.copy(alpha = 0.16f).toUIColor()
    val badgeColor = RoseUrgent.toUIColor()
    val barTint = scheme.surface.copy(alpha = 0.55f).toUIColor()
    val bar = remember { GlassTabBarViews() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        UIKitView(
            factory = { bar.build(tabs.size, barTint) },
            modifier = Modifier.fillMaxWidth().height(BarHeightDp.dp),
            update = { bar.update(tabs, selectedColor, normalColor, selectedBackground, badgeColor) },
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
                placedAsOverlay = true,
            ),
        )
    }
}

/** Holds the native views so [GlassTabBarViews.update] can address them without rebuilding. */
@OptIn(ExperimentalForeignApi::class)
private class GlassTabBarViews {
    private val cells = mutableListOf<TabCell>()

    fun build(count: Int, tint: UIColor): UIVisualEffectView {
        val effect = UIGlassEffect.effectWithStyle(UIGlassEffectStyle.UIGlassEffectStyleRegular)
        effect.setInteractive(true)
        // Without a tint the glass is so clear that content scrolling behind it competes with the
        // labels; the surface colour at low alpha frosts it just enough to stay readable.
        effect.setTintColor(tint)
        val bar = UIVisualEffectView(effect = effect)
        bar.layer.cornerRadius = CornerRadius
        bar.clipsToBounds = true

        val row = UIStackView()
        row.axis = UILayoutConstraintAxisHorizontal
        row.distribution = UIStackViewDistributionFillEqually
        row.alignment = UIStackViewAlignmentFill
        row.translatesAutoresizingMaskIntoConstraints = false
        bar.contentView.addSubview(row)
        NSLayoutConstraint.activateConstraints(
            listOf(
                row.leadingAnchor.constraintEqualToAnchor(bar.contentView.leadingAnchor, constant = 6.0),
                row.trailingAnchor.constraintEqualToAnchor(bar.contentView.trailingAnchor, constant = -6.0),
                row.topAnchor.constraintEqualToAnchor(bar.contentView.topAnchor, constant = 5.0),
                row.bottomAnchor.constraintEqualToAnchor(bar.contentView.bottomAnchor, constant = -5.0),
            )
        )

        cells.clear()
        repeat(count) {
            val cell = TabCell()
            row.addArrangedSubview(cell.container)
            cells += cell
        }
        return bar
    }

    fun update(
        tabs: List<ShellTab>,
        selectedColor: UIColor,
        normalColor: UIColor,
        selectedBackground: UIColor,
        badgeColor: UIColor,
    ) {
        tabs.forEachIndexed { index, item ->
            cells.getOrNull(index)?.update(item, selectedColor, normalColor, selectedBackground, badgeColor)
        }
    }
}

/** One tab: SF Symbol over its label, a capsule behind the selected one, and the deadline badge. */
@OptIn(ExperimentalForeignApi::class)
private class TabCell {
    val container = UIView()
    private val icon = UIImageView()
    private val label = UILabel()
    private val badge = UILabel()
    private val button = UIButton()
    private val tapTarget = TapTarget()

    init {
        container.layer.cornerRadius = SelectedCornerRadius
        container.clipsToBounds = true

        icon.contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
        label.font = UIFont.systemFontOfSize(10.0, weight = UIFontWeightMedium)
        label.textAlignment = NSTextAlignmentCenter

        val column = UIStackView(arrangedSubviews = listOf(icon, label))
        column.axis = UILayoutConstraintAxisVertical
        column.alignment = UIStackViewAlignmentCenter
        column.spacing = 2.0
        column.userInteractionEnabled = false
        column.setIsAccessibilityElement(false)
        column.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(column)

        badge.font = UIFont.systemFontOfSize(10.0, weight = UIFontWeightSemibold)
        badge.textColor = UIColor.whiteColor
        badge.textAlignment = NSTextAlignmentCenter
        badge.layer.cornerRadius = 8.0
        badge.clipsToBounds = true
        badge.hidden = true
        badge.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(badge)

        button.translatesAutoresizingMaskIntoConstraints = false
        button.addTarget(tapTarget, action = NSSelectorFromString("tabTapped"), forControlEvents = UIControlEventTouchUpInside)
        container.addSubview(button)

        NSLayoutConstraint.activateConstraints(
            listOf(
                column.centerXAnchor.constraintEqualToAnchor(container.centerXAnchor),
                column.centerYAnchor.constraintEqualToAnchor(container.centerYAnchor),
                icon.heightAnchor.constraintEqualToConstant(22.0),
                badge.leadingAnchor.constraintEqualToAnchor(icon.trailingAnchor, constant = -4.0),
                badge.bottomAnchor.constraintEqualToAnchor(icon.topAnchor, constant = 8.0),
                badge.heightAnchor.constraintEqualToConstant(16.0),
                badge.widthAnchor.constraintGreaterThanOrEqualToConstant(16.0),
                button.leadingAnchor.constraintEqualToAnchor(container.leadingAnchor),
                button.trailingAnchor.constraintEqualToAnchor(container.trailingAnchor),
                button.topAnchor.constraintEqualToAnchor(container.topAnchor),
                button.bottomAnchor.constraintEqualToAnchor(container.bottomAnchor),
            )
        )
    }

    fun update(
        item: ShellTab,
        selectedColor: UIColor,
        normalColor: UIColor,
        selectedBackground: UIColor,
        badgeColor: UIColor,
    ) {
        val tint = if (item.selected) selectedColor else normalColor
        val symbols = UIImageSymbolConfiguration.configurationWithPointSize(18.0)
        icon.image = UIImage.systemImageNamed(item.systemImage, withConfiguration = symbols)
        icon.tintColor = tint
        label.text = item.label
        label.textColor = tint
        label.font = UIFont.systemFontOfSize(10.0, weight = if (item.selected) UIFontWeightSemibold else UIFontWeightMedium)
        val background = if (item.selected) selectedBackground else UIColor.clearColor
        if (container.backgroundColor != background) {
            UIView.animateWithDuration(0.2) { container.backgroundColor = background }
        }
        button.setAccessibilityLabel(item.label)
        button.setAccessibilityTraits(
            if (item.selected) UIAccessibilityTraitButton or UIAccessibilityTraitSelected else UIAccessibilityTraitButton
        )
        badge.hidden = item.badgeCount <= 0
        badge.text = if (item.badgeCount > 0) " ${item.badgeCount} " else null
        badge.backgroundColor = badgeColor
        tapTarget.onTap = item.onClick
    }
}

/** UIKit wants a selector; Kotlin objects reach one through [ObjCAction]. */
private class TapTarget : NSObject() {
    var onTap: () -> Unit = {}

    @ObjCAction
    fun tabTapped() = onTap()
}

private fun Color.toUIColor(): UIColor =
    UIColor(red = red.toDouble(), green = green.toDouble(), blue = blue.toDouble(), alpha = alpha.toDouble())
