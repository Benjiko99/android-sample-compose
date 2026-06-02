package uno.lux.sample.data

import java.time.Duration
import java.time.Instant

/**
 * Stand-in content for the in-memory repository and Compose previews. Timestamps are
 * anchored to "now" at first access so the relative labels ("4m", "2h", "3d") stay
 * believable whenever the app is launched.
 */
internal val SampleUsers = listOf(
    User(id = "u1", nickname = "Ada Lovelace", handle = "@countess"),
    User(id = "u2", nickname = "Grace Hopper", handle = "@amazinggrace"),
    User(id = "u3", nickname = "Alan Turing", handle = "@enigma"),
    User(id = "u4", nickname = "Margaret Hamilton", handle = "@mhamilton"),
    User(id = "u5", nickname = "Linus", handle = "@torvalds"),
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
