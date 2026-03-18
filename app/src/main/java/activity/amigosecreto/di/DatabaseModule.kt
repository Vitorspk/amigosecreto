package activity.amigosecreto.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import activity.amigosecreto.db.room.AppDatabase
import activity.amigosecreto.db.room.DesejoRoomDao
import activity.amigosecreto.db.room.GrupoRoomDao
import activity.amigosecreto.db.room.ParticipanteRoomDao
import activity.amigosecreto.db.room.SorteioRoomDao
import activity.amigosecreto.repository.DesejoRepository
import activity.amigosecreto.repository.ParticipanteRepository
import activity.amigosecreto.repository.SorteioRepository

/**
 * Hilt module that provides Room DAOs and repository instances.
 *
 * AppDatabase is @Singleton — one instance for the entire app lifetime.
 * Repositories are @Singleton stateless wrappers; Room handles threading via coroutines.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideGrupoDao(db: AppDatabase): GrupoRoomDao = db.grupoDao()

    @Provides
    @Singleton
    fun provideParticipanteDao(db: AppDatabase): ParticipanteRoomDao = db.participanteDao()

    @Provides
    @Singleton
    fun provideDesejoDao(db: AppDatabase): DesejoRoomDao = db.desejoDao()

    @Provides
    @Singleton
    fun provideSorteioDao(db: AppDatabase): SorteioRoomDao = db.sorteioDao()

    @Provides
    @Singleton
    fun provideParticipanteRepository(dao: ParticipanteRoomDao): ParticipanteRepository =
        ParticipanteRepository(dao)

    @Provides
    @Singleton
    fun provideDesejoRepository(dao: DesejoRoomDao): DesejoRepository =
        DesejoRepository(dao)

    @Provides
    @Singleton
    fun provideSorteioRepository(dao: SorteioRoomDao): SorteioRepository =
        SorteioRepository(dao)
}
