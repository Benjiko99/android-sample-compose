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
import uno.lux.sample.data.post.CommentDataSource
import uno.lux.sample.data.post.CommentRepository
import uno.lux.sample.data.post.FeedDataSource
import uno.lux.sample.data.post.FeedRepository
import uno.lux.sample.data.post.LocalCommentDataSource
import uno.lux.sample.data.post.LocalFeedDataSource
import uno.lux.sample.data.post.LocalPostDataSource
import uno.lux.sample.data.post.PostDataSource
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.profile.LocalProfileDataSource
import uno.lux.sample.data.profile.ProfileDataSource
import uno.lux.sample.data.profile.ProfileRepository
import uno.lux.sample.data.settings.DataStoreSettingsRepository
import uno.lux.sample.data.settings.SettingsRepository
import uno.lux.sample.data.user.LocalUserDataSource
import uno.lux.sample.data.user.User
import uno.lux.sample.data.user.UserDataSource
import uno.lux.sample.data.user.UserRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    fun providePostDataSource(): PostDataSource = LocalPostDataSource()

    @Provides
    @Singleton
    fun providePostRepository(dataSource: PostDataSource): PostRepository =
        PostRepository(dataSource)

    @Provides
    fun provideFeedDataSource(): FeedDataSource = LocalFeedDataSource()

    @Provides
    fun provideUserDataSource(): UserDataSource = LocalUserDataSource()

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
    fun provideProfileDataSource(): ProfileDataSource = LocalProfileDataSource()

    @Provides
    @Singleton
    fun provideProfileRepository(
        dataSource: ProfileDataSource,
        postRepository: PostRepository,
    ): ProfileRepository = ProfileRepository(dataSource, postRepository)

    @Provides
    fun provideCommentDataSource(@CurrentUser currentUser: User): CommentDataSource =
        LocalCommentDataSource(currentUser)

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
