package id.or.kemenag.yasintahlil.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.or.kemenag.yasintahlil.core.data.local.RoomUserDataRepository
import id.or.kemenag.yasintahlil.core.domain.repository.UserDataRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserDataRepository(impl: RoomUserDataRepository): UserDataRepository
}
