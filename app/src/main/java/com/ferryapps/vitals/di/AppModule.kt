package com.ferryapps.vitals.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule
// App-scoped bindings: add Firebase Remote Config, app-level interceptors, etc.
