package uno.lux.sample.app.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import uno.lux.sample.R
import uno.lux.sample.app.navigation.Screen
import uno.lux.sample.app.theme.LocalMosaicColors
import uno.lux.sample.common.ui.DividedNavigationSuiteScaffold
import uno.lux.sample.feed.ui.HomeScreen
import uno.lux.sample.profile.ui.ProfileScreen
import uno.lux.sample.user.data.domain.UserId

/**
 * The tabbed shell: [DividedNavigationSuiteScaffold] adapts the navigation affordance to the
 * window size — bottom bar on phones, navigation rail on larger or unfolded screens — and the
 * selected [AppDestinations] entry chooses which screen fills the content area. Tabs are peer
 * destinations held as plain selection state (switching tabs is not a navigation event and
 * never grows the back stack), so the content cross-fades (fade-through) rather than sliding.
 *
 * An [AppDestinations] entry carrying a [Screen] breaks that rule deliberately: it is an action,
 * not a tab, and selecting it pushes that page over the whole shell through [AppShellViewModel] —
 * covering the navigation bar, leaving the current tab selected underneath, and sliding in like
 * any other page. [AppDestinations.CREATE] opens the composer this way.
 */
@Composable
fun AppShell(
    currentUserId: UserId,
    viewModel: AppShellViewModel = hiltViewModel(),
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val navItemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = LocalMosaicColors.current.textTertiary,
        ),
    )

    DividedNavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            painterResource(destination.icon),
                            contentDescription = stringResource(destination.labelRes),
                        )
                    },
                    label = { Text(stringResource(destination.labelRes)) },
                    // An action entry never reports as selected — it pushes a page and leaves
                    // the current tab highlighted underneath.
                    selected = destination == currentDestination,
                    onClick = {
                        val screen = destination.screen
                        if (screen != null) {
                            viewModel.openDestination(screen)
                        } else {
                            currentDestination = destination
                        }
                    },
                    colors = navItemColors,
                )
            }
        },
    ) {
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                (
                    fadeIn(tween(durationMillis = 210, delayMillis = 90)) +
                        scaleIn(
                            animationSpec = tween(
                                durationMillis = 210,
                                delayMillis = 90,
                            ),
                            initialScale = 0.94f,
                        )
                ) togetherWith
                    fadeOut(tween(durationMillis = 90))
            },
            modifier = Modifier.fillMaxSize(),
            label = "tab",
        ) { destination ->
            when (destination) {
                AppDestinations.HOME -> HomeScreen()

                // The Profile tab is the signed-in user's own profile (no up-affordance, so
                // showBackButton stays false); tapping another author still pushes Screen.Profile.
                AppDestinations.PROFILE -> ProfileScreen(userId = currentUserId)

                // CREATE pushes Screen.CreatePost over the shell rather than filling the
                // content area, so it is never the selected destination and never renders here.
                AppDestinations.CREATE -> Unit
            }
        }
    }
}

/**
 * The top-level entries of the navigation suite; add or change one by editing the enum.
 *
 * Most are **tabs**: selecting one swaps the shell's content area and leaves the item
 * highlighted. An entry carrying a [screen] is instead an **action** — selecting it pushes that
 * page over the entire shell (covering the tab bar) and never becomes the selection, so the tab
 * the user was on stays highlighted underneath. That is how [CREATE] opens the composer, the
 * behaviour a centre "new post" button has in apps like TikTok.
 */
enum class AppDestinations(
    @get:StringRes val labelRes: Int,
    @get:DrawableRes val icon: Int,
    val screen: Screen? = null,
) {
    HOME(
        labelRes = R.string.nav_home,
        icon = R.drawable.ic_home,
    ),
    CREATE(
        labelRes = R.string.nav_create,
        icon = R.drawable.ic_add,
        screen = Screen.CreatePost,
    ),
    PROFILE(
        labelRes = R.string.nav_profile,
        icon = R.drawable.ic_account_box,
    ),
}
