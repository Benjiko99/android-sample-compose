package uno.lux.sample.app.di

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
import uno.lux.sample.app.fixtures.LOGGED_IN_USER_ID
import uno.lux.sample.app.fixtures.SampleUsers
import uno.lux.sample.comment.data.CommentDataSource
import uno.lux.sample.comment.data.CommentRepository
import uno.lux.sample.comment.data.network.CommentApi
import uno.lux.sample.comment.data.network.NetworkCommentDataSource
import uno.lux.sample.common.data.files.AndroidFileLoader
import uno.lux.sample.common.data.files.AndroidVideoMetadataReader
import uno.lux.sample.common.data.files.FileLoader
import uno.lux.sample.common.data.files.VideoMetadataReader
import uno.lux.sample.feed.data.FeedDataSource
import uno.lux.sample.feed.data.FeedRepository
import uno.lux.sample.feed.data.network.FeedApi
import uno.lux.sample.feed.data.network.NetworkFeedDataSource
import uno.lux.sample.post.data.PostDataSource
import uno.lux.sample.post.data.PostRepository
import uno.lux.sample.post.data.network.NetworkPostDataSource
import uno.lux.sample.post.data.network.PostApi
import uno.lux.sample.profile.data.ProfileDataSource
import uno.lux.sample.profile.data.ProfileRepository
import uno.lux.sample.profile.data.network.NetworkProfileDataSource
import uno.lux.sample.profile.data.network.ProfileApi
import uno.lux.sample.settings.data.AppCompatLocaleRepository
import uno.lux.sample.settings.data.AppLocaleRepository
import uno.lux.sample.settings.data.DataStoreSettingsRepository
import uno.lux.sample.settings.data.SettingsRepository
import uno.lux.sample.user.data.UserDataSource
import uno.lux.sample.user.data.UserRepository
import uno.lux.sample.user.data.domain.User
import uno.lux.sample.user.data.domain.UserId
import uno.lux.sample.user.data.network.NetworkUserDataSource
import uno.lux.sample.user.data.network.UserApi
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    fun providePostDataSource(api: PostApi): PostDataSource = NetworkPostDataSource(api)

    @Provides
    @Singleton
    fun providePostRepository(
        dataSource: PostDataSource,
        userRepository: UserRepository,
    ): PostRepository = PostRepository(dataSource, userRepository)

    @Provides
    fun provideFeedDataSource(api: FeedApi): FeedDataSource = NetworkFeedDataSource(api)

    @Provides
    fun provideUserDataSource(api: UserApi): UserDataSource = NetworkUserDataSource(api)

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
    fun provideProfileDataSource(api: ProfileApi): ProfileDataSource = NetworkProfileDataSource(api)

    @Provides
    @Singleton
    fun provideProfileRepository(
        dataSource: ProfileDataSource,
        postRepository: PostRepository,
        userRepository: UserRepository,
        @CurrentUserId currentUserId: UserId,
    ): ProfileRepository =
        ProfileRepository(dataSource, postRepository, userRepository, currentUserId)

    @Provides
    fun provideCommentDataSource(api: CommentApi): CommentDataSource = NetworkCommentDataSource(api)

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
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

    @Provides
    fun provideFileLoader(
        @ApplicationContext context: Context,
    ): FileLoader =
        AndroidFileLoader(context)

    @Provides
    fun provideVideoMetadataReader(
        @ApplicationContext context: Context,
    ): VideoMetadataReader =
        AndroidVideoMetadataReader(context)

    @Provides
    @CurrentUserId
    fun provideCurrentUserId(): String = LOGGED_IN_USER_ID

    @Provides
    @Singleton
    @CurrentUser
    fun provideCurrentUser(
        @CurrentUserId currentUserId: UserId,
    ): User =
        SampleUsers.first { it.id == currentUserId }
}
