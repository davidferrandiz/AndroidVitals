package com.ferryapps.vitals.feature.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.ferryapps.vitals.core.domain.widget.WidgetUpdater
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

class GlanceWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) : WidgetUpdater {
    /** Si no hay ningún widget puesto, no hace nada. */
    override suspend fun updateAll() {
        VitalsWidget().updateAll(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetModule {
    @Binds
    @Singleton
    abstract fun bindWidgetUpdater(impl: GlanceWidgetUpdater): WidgetUpdater
}
