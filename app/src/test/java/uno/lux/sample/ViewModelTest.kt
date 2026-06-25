package uno.lux.sample

import org.junit.Rule

abstract class ViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
}
