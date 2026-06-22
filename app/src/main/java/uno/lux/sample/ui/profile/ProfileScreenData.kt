package uno.lux.sample.ui.profile

import uno.lux.sample.data.user.User
import uno.lux.sample.data.profile.Profile

/** ViewModel-level aggregate for the profile screen: the resolved user and their content. */
data class ProfileScreenData(
    val user: User,
    val profile: Profile,
)
