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
import uno.lux.sample.data.network.NetworkCommentDataSource
import uno.lux.sample.data.network.NetworkFeedDataSource
import uno.lux.sample.data.network.NetworkPostDataSource
import uno.lux.sample.data.network.NetworkProfileDataSource
import uno.lux.sample.data.network.NetworkUserDataSource
import uno.lux.sample.data.post.CommentDataSource
import uno.lux.sample.data.post.CommentRepository
import uno.lux.sample.data.post.FeedDataSource
import uno.lux.sample.data.post.FeedRepository
import uno.lux.sample.data.post.PostDataSource
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.profile.ProfileDataSource
import uno.lux.sample.data.profile.ProfileRepository
import uno.lux.sample.data.settings.AppCompatLocaleRepository
import uno.lux.sample.data.settings.AppLocaleRepository
import uno.lux.sample.data.settings.DataStoreSettingsRepository
import uno.lux.sample.data.settings.SettingsRepository
import uno.lux.sample.data.user.User
import uno.lux.sample.data.user.UserDataSource
import uno.lux.sample.data.user.UserRepository
import uno.lux.sample.ui.editprofile.AndroidAvatarImageLoader
import uno.lux.sample.ui.editprofile.AvatarImageLoader
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    fun providePostDataSource(api: MosaicApi): PostDataSource = NetworkPostDataSource(api)

    @Provides
    @Singleton
    fun providePostRepository(dataSource: PostDataSource): PostRepository =
        PostRepository(dataSource)

    @Provides
    fun provideFeedDataSource(api: MosaicApi): FeedDataSource = NetworkFeedDataSource(api)

    @Provides
    fun provideUserDataSource(api: MosaicApi): UserDataSource = NetworkUserDataSource(api)

    @Provides
    @Singleton
    fun provideUserRepository(dataSource: UserDataSource): UserRepository =
        UserRepository(dataSource)

    @Provides
    @Singleton
    fun provideFeedRepository(
        dataSource: FeedDataSource,
        postRepository: PostRepository,
        userRepository: UserRepository,
    ): FeedRepository = FeedRepository(dataSource, postRepository, userRepository)

    @Provides
    fun provideProfileDataSource(api: MosaicApi): ProfileDataSource = NetworkProfileDataSource(api)

    @Provides
    @Singleton
    fun provideProfileRepository(
        dataSource: ProfileDataSource,
        postRepository: PostRepository,
    ): ProfileRepository = ProfileRepository(dataSource, postRepository)

    @Provides
    fun provideCommentDataSource(api: MosaicApi): CommentDataSource = NetworkCommentDataSource(api)

    @Provides
    @Singleton
    fun provideCommentRepository(dataSource: CommentDataSource): CommentRepository =
        CommentRepository(dataSource)

    @Provides
    @Singleton
    fun provideSettingsRepository(dataStore: DataStore<Preferences>): SettingsRepository =
        DataStoreSettingsRepository(dataStore)

    @Provides
    @Singleton
    fun provideAppLocaleRepository(): AppLocaleRepository = AppCompatLocaleRepository()

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

    @Provides
    fun provideAvatarImageLoader(@ApplicationContext context: Context): AvatarImageLoader =
        AndroidAvatarImageLoader(context)

    @Provides
    @CurrentUserId
    fun provideCurrentUserId(): String = LoggedInUserId

    @Provides
    @Singleton
    @CurrentUser
    fun provideCurrentUser(@CurrentUserId currentUserId: String): User =
        SampleUsers.first { it.id == currentUserId }
}
