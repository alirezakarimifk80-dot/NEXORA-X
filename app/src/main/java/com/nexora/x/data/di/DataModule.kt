package com.nexora.x.data.di

import com.nexora.x.core.engine.EngineRepository
import com.nexora.x.core.engine.EngineRepositoryImpl
import com.nexora.x.data.repository.NexoraRepositoryImpl
import com.nexora.x.data.repository.ProfileRepositoryImpl
import com.nexora.x.domain.repository.NexoraRepository
import com.nexora.x.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Dependency Injection module binding [NexoraRepositoryImpl], [ProfileRepositoryImpl],
 * and [EngineRepositoryImpl].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindNexoraRepository(
        impl: NexoraRepositoryImpl
    ): NexoraRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: ProfileRepositoryImpl
    ): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindEngineRepository(
        impl: EngineRepositoryImpl
    ): EngineRepository
}
