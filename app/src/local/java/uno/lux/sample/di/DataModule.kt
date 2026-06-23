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
import uno.lux.sample.data.post.CommentRepository
import uno.lux.sample.data.post.InMemoryCommentRepository
import uno.lux.sample.data.post.InMemoryPostRepository
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.profile.InMemoryProfileRepository
import uno.lux.sample.data.profile.ProfileRepository
import uno.lux.sample.data.settings.DataStoreSettingsRepository
import uno.lux.sample.data.settings.SettingsRepository
import uno.lux.sample.data.user.InMemoryUserRepository
import uno.lux.sample.data.user.User
import uno.lux.sample.data.user.UserRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun providePostRepository(): PostRepository = InMemoryPostRepository()

    @Provides
    @Singleton
    fun provideProfileRepository(postRepository: PostRepository): ProfileRepository =
        InMemoryProfileRepository(postRepository)

    @Provides
    @Singleton
    fun provideUserRepository(): UserRepository = InMemoryUserRepository()

    @Provides
    @Singleton
    fun provideCommentRepository(@CurrentUser currentUser: User): CommentRepository =
        InMemoryCommentRepository(currentUser)

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
