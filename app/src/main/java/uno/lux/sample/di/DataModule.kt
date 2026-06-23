package uno.lux.sample.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import uno.lux.sample.data.LoggedInUserId
import uno.lux.sample.data.SampleUsers
import uno.lux.sample.data.network.NetworkCommentRepository
import uno.lux.sample.data.network.NetworkPostRepository
import uno.lux.sample.data.network.NetworkProfileRepository
import uno.lux.sample.data.network.NetworkUserRepository
import uno.lux.sample.data.network.SampleApi
import uno.lux.sample.data.post.CommentRepository
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.profile.ProfileRepository
import uno.lux.sample.data.settings.DataStoreSettingsRepository
import uno.lux.sample.data.settings.SettingsRepository
import uno.lux.sample.data.user.User
import uno.lux.sample.data.user.UserRepository
import javax.inject.Singleton

/**
 * Production wiring for the `data` layer: each repository interface gets its concrete
 * implementation here, and nowhere else needs to know which one that is.
 *
 * [NetworkUserRepository] is provided twice — once as its concrete type (so
 * [NetworkPostRepository] can call [NetworkUserRepository.ingest] for feed sideloads) and once
 * as the [UserRepository] binding consumed by ViewModels. Everything is [Singleton]-scoped so
 * the user cache and the feed list are shared across the whole app.
 *
 * The repositories stay free of DI annotations (constructed here via [Provides] rather than
 * `@Inject`), keeping the data layer plain JVM with no framework dependency.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideNetworkUserRepository(api: SampleApi): NetworkUserRepository =
        NetworkUserRepository(api)

    @Provides
    @Singleton
    fun provideUserRepository(impl: NetworkUserRepository): UserRepository = impl

    @Provides
    @Singleton
    fun providePostRepository(
        api: SampleApi,
        userRepository: NetworkUserRepository,
    ): PostRepository = NetworkPostRepository(api, userRepository)

    @Provides
    @Singleton
    fun provideProfileRepository(api: SampleApi): ProfileRepository =
        NetworkProfileRepository(api)

    @Provides
    @Singleton
    fun provideCommentRepository(api: SampleApi): CommentRepository =
        NetworkCommentRepository(api)

    @Provides
    @Singleton
    fun provideSettingsRepository(dataStore: DataStore<Preferences>): SettingsRepository =
        DataStoreSettingsRepository(dataStore)

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

    /** The signed-in user's id. A real app would resolve this from an auth/session source. */
    @Provides
    @CurrentUserId
    fun provideCurrentUserId(): String = LoggedInUserId

    /** The signed-in [User] object. Used for the comment composer; resolved from sample data. */
    @Provides
    @CurrentUser
    fun provideCurrentUser(@CurrentUserId currentUserId: String): User =
        SampleUsers.first { it.id == currentUserId }
}
