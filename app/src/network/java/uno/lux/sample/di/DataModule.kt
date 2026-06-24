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
import uno.lux.sample.data.network.MosaicApi
import uno.lux.sample.data.network.NetworkCommentRepository
import uno.lux.sample.data.network.NetworkFeedRepository
import uno.lux.sample.data.network.NetworkPostRepository
import uno.lux.sample.data.network.NetworkProfileRepository
import uno.lux.sample.data.network.NetworkUserRepository
import uno.lux.sample.data.post.CommentRepository
import uno.lux.sample.data.post.FeedRepository
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.profile.ProfileRepository
import uno.lux.sample.data.settings.DataStoreSettingsRepository
import uno.lux.sample.data.settings.SettingsRepository
import uno.lux.sample.data.user.User
import uno.lux.sample.data.user.UserRepository
import javax.inject.Singleton

/**
 * [NetworkUserRepository] and [NetworkPostRepository] are each provided twice — once as their
 * concrete type (so [NetworkFeedRepository] can call [NetworkUserRepository.ingest] and
 * [PostRepository.ingest] directly) and once as their interface binding consumed by ViewModels.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideNetworkUserRepository(api: MosaicApi): NetworkUserRepository =
        NetworkUserRepository(api)

    @Provides
    @Singleton
    fun provideUserRepository(impl: NetworkUserRepository): UserRepository = impl

    @Provides
    @Singleton
    fun provideNetworkPostRepository(api: MosaicApi): NetworkPostRepository =
        NetworkPostRepository(api)

    @Provides
    @Singleton
    fun providePostRepository(impl: NetworkPostRepository): PostRepository = impl

    @Provides
    @Singleton
    fun provideFeedRepository(
        api: MosaicApi,
        postRepository: PostRepository,
        userRepository: NetworkUserRepository,
    ): FeedRepository = NetworkFeedRepository(api, postRepository, userRepository)

    @Provides
    @Singleton
    fun provideProfileRepository(
        api: MosaicApi,
        postRepository: PostRepository,
    ): ProfileRepository = NetworkProfileRepository(api, postRepository)

    @Provides
    @Singleton
    fun provideCommentRepository(api: MosaicApi): CommentRepository =
        NetworkCommentRepository(api)

    @Provides
    @Singleton
    fun provideSettingsRepository(dataStore: DataStore<Preferences>): SettingsRepository =
        DataStoreSettingsRepository(dataStore)

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

    @Provides
    @CurrentUserId
    fun provideCurrentUserId(): String = LoggedInUserId

    @Provides
    @Singleton
    @CurrentUser
    fun provideCurrentUser(@CurrentUserId currentUserId: String): User =
        SampleUsers.first { it.id == currentUserId }
}
