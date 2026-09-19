package com.suw1labs.worktracker.platform

/**
 * True where the system draws tab bars as a rounded bar floating above the bottom edge (iOS 26
 * and later). Android and desktop keep the Material navigation bar / rail.
 */
expect val usesFloatingTabBar: Boolean
