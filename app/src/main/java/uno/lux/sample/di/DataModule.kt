package uno.lux.sample.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import uno.lux.sample.data.InMemoryPostRepository
import uno.lux.sample.data.InMemoryProfileRepository
import uno.lux.sample.data.PostRepository
import uno.lux.sample.data.ProfileRepository
import uno.lux.sample.data.SettingsRepository
import uno.lux.sample.data.SharedPreferencesSettingsRepository
import javax.inject.Singleton

/**
 * Production wiring for the `data` layer: each repository interface gets its concrete
 * implementation here, and nowhere else needs to know which one that is.
 *
 * Everything is [Singleton]-scoped because each repository *is* the app's source of truth —
 * the in-memory ones hold the state itself, so two instances would mean two diverging copies.
 * [provideProfileRepository] takes the bound [PostRepository] so profile pages and the feed
 * share one post store: a like toggled in either place is visible in both.
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
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SharedPreferencesSettingsRepository(
            context.getSharedPreferences("settings", Context.MODE_PRIVATE),
        )
}
