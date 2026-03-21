package com.deskzen.di

import com.deskzen.domain.usecase.ManageShortcutUseCase
import com.deskzen.domain.usecase.ManageShortcutUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UseCaseModule {

    @Binds
    @Singleton
    abstract fun bindManageShortcutUseCase(impl: ManageShortcutUseCaseImpl): ManageShortcutUseCase
}
