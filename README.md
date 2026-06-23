# Sample

A portfolio Android app demonstrating modern architecture: single-activity Compose UI, Navigation 3, Hilt DI, MVVM with `StateFlow`, and a Retrofit network layer.

## Data flavors

The app has two product flavors in the `data` dimension that control where repository data comes from.

| Flavor | Data source | Use when |
|--------|------------|----------|
| `local` **(default)** | In-memory, seeded from `SampleData.kt` | Running the app without a backend |
| `network` | Retrofit, hitting the dev server at `http://10.0.2.2:3000/api/` | Testing against a real backend |

### Switching flavors in Android Studio

Open the **Build Variants** panel (View → Tool Windows → Build Variants) and select the variant you want:

- `localDebug` / `localRelease`
- `networkDebug` / `networkRelease`

### Switching flavors on the command line

```powershell
# Install the local flavor (default)
.\gradlew.bat installLocalDebug

# Install the network flavor
.\gradlew.bat installNetworkDebug
```

### Running tests

```powershell
# Local flavor
.\gradlew.bat testLocalDebugUnitTest

# Network flavor
.\gradlew.bat testNetworkDebugUnitTest
```

## Backend setup (network flavor only)

The network flavor expects a server at `http://10.0.2.2:3000/api/` — the standard Android emulator address for `localhost` on the host machine. Start the backend on your machine at port 3000, then launch an emulator and install the `networkDebug` variant.
