package uno.lux.sample.testing

import org.junit.Rule

abstract class ViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
}
