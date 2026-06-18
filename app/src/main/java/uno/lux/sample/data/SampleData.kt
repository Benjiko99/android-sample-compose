package uno.lux.sample.data

import java.time.Duration
import java.time.Instant

/**
 * Stand-in content for the in-memory repository and Compose previews. Timestamps are
 * anchored to "now" at first access so the relative labels ("4m", "2h", "3d") stay
 * believable whenever the app is launched.
 */
internal val SampleUsers = listOf(
    User(
        id = "u1",
        nickname = "Ada Lovelace",
        handle = "@countess",
        age = 36,
        gender = "Woman",
        location = "London, England",
        bio = "Mathematician & writer. The Analytical Engine weaves algebra the way the loom " +
            "weaves flowers. Poetical science, mostly.",
        followerCount = 128_400,
        followingCount = 212,
    ),
    User(
        id = "u2",
        nickname = "Grace Hopper",
        handle = "@amazinggrace",
        age = 85,
        gender = "Woman",
        location = "Arlington, Virginia",
        bio = "Rear Admiral. Compiler pioneer. It's easier to ask forgiveness than permission.",
        followerCount = 342_000,
        followingCount = 180,
    ),
    User(
        id = "u3",
        nickname = "Alan Turing",
        handle = "@enigma",
        age = 41,
        gender = "Man",
        location = "Manchester, England",
        bio = "Asking the only question that matters: can a machine play the imitation game?",
        followerCount = 891_000,
        followingCount = 73,
    ),
    User(
        id = "u4",
        nickname = "Margaret Hamilton",
        handle = "@mhamilton",
        age = 88,
        gender = "Woman",
        location = "Cambridge, Massachusetts",
        bio = "I coined \"software engineering\" so they'd take the code as seriously as the " +
            "hardware. Apollo guidance, priority scheduling.",
        followerCount = 154_300,
        followingCount = 96,
    ),
    User(
        id = "u5",
        nickname = "Linus",
        handle = "@torvalds",
        age = 54,
        gender = "Man",
        location = "Portland, Oregon",
        bio = "Just a hobby, won't be big. Talk is cheap — show me the code.",
        followerCount = 5_200_000,
        followingCount = 12,
    ),
)

internal val SamplePosts: List<Post> = buildSamplePosts(Instant.now())

private fun buildSamplePosts(now: Instant): List<Post> = listOf(
    Post(
        id = "p1",
        author = SampleUsers[0],
        title = "The engine weaves algebraic patterns",
        body = "Just like the Jacquard loom weaves flowers and leaves. A machine need not " +
            "be limited to numbers — give it the right notation and it can compose.",
        createdAt = now.minus(Duration.ofMinutes(4)),
        likeCount = 128,
        commentCount = 17,
        isLiked = true,
    ),
    Post(
        id = "p2",
        author = SampleUsers[1],
        title = "Found the bug",
        body = "It was an actual moth, taped into the logbook at 15:45. First recorded " +
            "case of debugging being literal. Onward to the next nanosecond.",
        createdAt = now.minus(Duration.ofMinutes(38)),
        likeCount = 342,
        commentCount = 51,
        isBookmarked = true,
    ),
    Post(
        id = "p3",
        author = SampleUsers[2],
        title = "Can machines think?",
        body = "The question is too meaningless to deserve discussion. So replace it: can a " +
            "machine play the imitation game well enough that you can't tell?",
        createdAt = now.minus(Duration.ofHours(2)),
        likeCount = 891,
        commentCount = 203,
    ),
    Post(
        id = "p4",
        author = SampleUsers[3],
        title = "Priority scheduling saved the landing",
        body = "Three minutes before touchdown the computer flashed a 1202 alarm. Because we " +
            "designed it to shed low-priority work under overload, it kept the essentials " +
            "running. Apollo 11 landed anyway.",
        createdAt = now.minus(Duration.ofHours(6)),
        likeCount = 1543,
        commentCount = 88,
        isLiked = true,
        isBookmarked = true,
    ),
    Post(
        id = "p5",
        author = SampleUsers[4],
        title = "Just a hobby, won't be big",
        body = "I'm doing a (free) operating system — nothing professional like GNU — for " +
            "386(486) AT clones. It probably never will support anything other than AT " +
            "hard disks, as that's all I have. :)",
        createdAt = now.minus(Duration.ofDays(1)),
        likeCount = 5200,
        commentCount = 612,
    ),
    Post(
        id = "p6",
        author = SampleUsers[0],
        title = "On numbers and music",
        body = "Supposing the relations of pitched sounds could be expressed by the engine, it " +
            "might compose elaborate pieces of music of any degree of complexity.",
        createdAt = now.minus(Duration.ofDays(3)),
        likeCount = 64,
        commentCount = 5,
    ),
)

/** Album stand-ins per user id, surfaced on the profile's Albums tab. */
internal val SampleAlbums: Map<String, List<Album>> = mapOf(
    "u1" to listOf(
        Album(id = "a1", title = "Engine Sketches", itemCount = 24),
        Album(id = "a2", title = "Notation Studies", itemCount = 12),
        Album(id = "a3", title = "Loom Patterns", itemCount = 18),
        Album(id = "a4", title = "Letters & Margins", itemCount = 7),
    ),
    "u2" to listOf(
        Album(id = "a5", title = "Mark I Logbook", itemCount = 31),
        Album(id = "a6", title = "The First Bug", itemCount = 4),
        Album(id = "a7", title = "Nanoseconds", itemCount = 9),
    ),
    "u3" to listOf(
        Album(id = "a8", title = "Bombe Rotors", itemCount = 16),
        Album(id = "a9", title = "Morphogenesis", itemCount = 22),
    ),
    "u4" to listOf(
        Album(id = "a10", title = "Rope Memory", itemCount = 14),
        Album(id = "a11", title = "Launch Room", itemCount = 28),
        Album(id = "a12", title = "The Listing", itemCount = 6),
    ),
    "u5" to listOf(
        Album(id = "a13", title = "Build Logs", itemCount = 42),
        Album(id = "a14", title = "Diving Trips", itemCount = 11),
    ),
)

/**
 * The stand-in stream every [Video] points at — a short, freely hosted MP4. Real content would
 * carry a per-video URL; the sample reuses one so the player is exercisable end to end.
 */
const val SampleVideoUrl = "https://samplelib.com/mp4/sample-5s-720p.mp4"

/** Video stand-ins per user id, surfaced on the profile's Videos tab. */
internal val SampleVideos: Map<String, List<Video>> = mapOf(
    "u1" to listOf(
        Video(id = "v1", title = "Weaving algebra on the Analytical Engine", durationSeconds = 222, viewCount = 41_200, videoUrl = SampleVideoUrl),
        Video(id = "v2", title = "Note G, explained", durationSeconds = 615, viewCount = 12_800, videoUrl = SampleVideoUrl),
        Video(id = "v3", title = "Poetical science", durationSeconds = 95, viewCount = 8_400, videoUrl = SampleVideoUrl),
    ),
    "u2" to listOf(
        Video(id = "v4", title = "How a compiler thinks", durationSeconds = 1325, viewCount = 220_000, videoUrl = SampleVideoUrl),
        Video(id = "v5", title = "A nanosecond in your hand", durationSeconds = 184, viewCount = 1_200_000, videoUrl = SampleVideoUrl),
    ),
    "u3" to listOf(
        Video(id = "v6", title = "The imitation game", durationSeconds = 742, viewCount = 980_000, videoUrl = SampleVideoUrl),
        Video(id = "v7", title = "On computable numbers", durationSeconds = 2010, viewCount = 154_000, videoUrl = SampleVideoUrl),
        Video(id = "v8", title = "Breaking Enigma", durationSeconds = 366, viewCount = 512_300, videoUrl = SampleVideoUrl),
    ),
    "u4" to listOf(
        Video(id = "v9", title = "The 1202 alarm", durationSeconds = 488, viewCount = 1_540_000, videoUrl = SampleVideoUrl),
        Video(id = "v10", title = "Software, taken seriously", durationSeconds = 277, viewCount = 96_500, videoUrl = SampleVideoUrl),
    ),
    "u5" to listOf(
        Video(id = "v11", title = "Talk is cheap", durationSeconds = 132, viewCount = 3_100_000, videoUrl = SampleVideoUrl),
        Video(id = "v12", title = "Git in ten minutes", durationSeconds = 631, viewCount = 2_050_000, videoUrl = SampleVideoUrl),
        Video(id = "v13", title = "Why monolithic", durationSeconds = 1442, viewCount = 740_000, videoUrl = SampleVideoUrl),
    ),
)
