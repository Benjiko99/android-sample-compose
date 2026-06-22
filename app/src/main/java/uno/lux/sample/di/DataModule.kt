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
import uno.lux.sample.data.User
import uno.lux.sample.data.post.CommentRepository
import uno.lux.sample.data.post.InMemoryCommentRepository
import uno.lux.sample.data.post.InMemoryPostRepository
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.profile.InMemoryProfileRepository
import uno.lux.sample.data.profile.ProfileRepository
import uno.lux.sample.data.settings.DataStoreSettingsRepository
import uno.lux.sample.data.settings.SettingsRepository
import javax.inject.Singleton

/**
 * Production wiring for the `data` layer: each repository interface gets its concrete
 * implementation here, and nowhere else needs to know which one that is.
 *
 * Everything is [Singleton]-scoped because each repository *is* the app's source of truth —
 * the in-memory ones hold the state itself, so two instances would mean two diverging copies
 * (and DataStore requires a single instance per file). [provideProfileRepository] takes the
 * bound [PostRepository] so profile pages and the feed share one post store: a like toggled
 * in either place is visible in both.
 *
 * The repositories stay free of DI annotations (constructed here via [Provides] rather than
 * `@Inject`), keeping the data layer plain JVM with no framework dependency.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun providePostRepository(): PostRepository = InMemoryPostRepository()

    @Provides
    @Singleton
    fun provideProfileRepository(postRepository: PostRepository): ProfileRepository =
        InMemoryProfileRepository(postRepository = postRepository)

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

    /** The signed-in [User] object resolved from [provideCurrentUserId]. */
    @Provides
    @CurrentUser
    fun provideCurrentUser(@CurrentUserId currentUserId: String): User =
        SampleUsers.first { it.id == currentUserId }

    @Provides
    @Singleton
    fun provideCommentRepository(@CurrentUser currentUser: User): CommentRepository =
        InMemoryCommentRepository(currentUser = currentUser)
}
