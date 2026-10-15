package com.ferryapps.vitals.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Recorre lo que hace casi todo el mundo al abrir Vitals: el Panel, bajar hasta la red y entrar en
 * Batería y Hardware. Las clases que se cargan aquí se compilan por adelantado al instalar, así
 * que el arranque y el primer scroll no esperan al JIT (lo que más se nota en gama baja).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = PACKAGE,
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()

        scrollMainList()
        openTab("nav_battery")
        scrollMainList()
        openTab("nav_hardware")
        scrollMainList()
        openTab("nav_panel")
    }

    private fun MacrobenchmarkScope.openTab(tag: String) {
        device.wait(Until.hasObject(By.res(tag)), TIMEOUT_MS)
        device.findObject(By.res(tag))?.click()
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.scrollMainList() {
        device.wait(Until.hasObject(By.scrollable(true)), TIMEOUT_MS)
        val list = device.findObject(By.scrollable(true)) ?: return
        // Margen para no abrir el panel de notificaciones ni la barra de gestos.
        list.setGestureMargin(device.displayWidth / 5)
        list.fling(Direction.DOWN)
        device.waitForIdle()
        list.fling(Direction.UP)
        device.waitForIdle()
    }

    private companion object {
        const val PACKAGE = "com.ferryapps.vitals"
        const val TIMEOUT_MS = 5_000L
    }
}
