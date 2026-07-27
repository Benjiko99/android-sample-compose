# Architecture

The goal is an improved architecture inspired by Slack's Circuit framework.

Do they not use ViewModels in their architecture at all?

A screen is just an interface. They represent navigable screens in the app.
```kotlin
interface Screen : Parcelable
```

A screen that takes no inputs can be an object, and a screen with inputs can be a data class.
```kotlin
@Stable
interface CircuitUiState

@Immutable
interface CircuitUiEvent

// Useful for stateless screens
object NoState : CircuitUiState

object LoginScreen: Screen

data class CounterScreen(val initialCount: Int): Screen

@Parcelize
object CounterScreen : Screen {
    data class State(
        val count: Int,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState
    
    object Event : CircuitUiEvent
}

// More complex example with a multi-state screen
sealed interface State : CircuitUiState {
    object Loading : State

    data class Count(
        val count: Int,
        val eventSink: (Event) -> Unit,
    ) : State
}
```

```kotlin
interface Ui<UiState> {
    @Composable
    fun content(
        state: UiState,
        modifier: Modifier = Modifier,
    )
}
```

```kotlin
interface Presenter<UiState> {
    @Composable
    fun present(): UiState
}
```

```kotlin
class CounterPresenter @AssistedInject constructor(
    @Assisted private val screen: CounterScreen,
    @Assisted private val navigator: Navigator,
) : Presenter<State> {
    
    @Composable
    override fun present(): State {
        var count by remember { mutableStateOf(0) }
        
        return State(count) { event ->
            when (event) {
                Increment -> count++
                Decrement -> count--
                OpenLogin -> navigator.goTo(LoginScreen)
            }
        }
    }
}
```

## Factories
Things like Presenters should be created by a Factory. I don't know how to do that yet. It should use @AssistedInject.

Navigator and Screen get injected into a Presenter.

## Navigation
Our current solution should be fine.

## CircuitContext
Seems like something we'd need as well; for shared global state and helpers.
