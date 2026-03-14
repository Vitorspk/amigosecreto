package activity.amigosecreto.di;

import android.content.Context;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

import javax.inject.Singleton;

import activity.amigosecreto.repository.DesejoRepository;
import activity.amigosecreto.repository.ParticipanteRepository;

/**
 * Hilt module that provides repository instances for the entire application lifetime.
 *
 * Repositories are @Singleton because they are stateless wrappers around DAOs — each method
 * opens/closes the DAO internally, so a single shared instance is safe across threads.
 */
@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    public static ParticipanteRepository provideParticipanteRepository(
            @ApplicationContext Context context) {
        return new ParticipanteRepository(context);
    }

    @Provides
    @Singleton
    public static DesejoRepository provideDesejoRepository(
            @ApplicationContext Context context) {
        return new DesejoRepository(context);
    }
}
