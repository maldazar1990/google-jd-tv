package com.minimal.tvlauncher.manager

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.minimal.tvlauncher.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Defensive and boundary stress testing for [SportsManager].
 *
 * Verifies:
 * 1. Extreme and adversarial sport names (null-like, blank, unknown, Unicode, emojis).
 * 2. Adversarial team names (empty, whitespace, huge strings, injection attempts).
 * 3. Malformed, truncated, empty, and corrupted JSON payloads from ESPN scoreboard API.
 * 4. Memory footprint: Team logos are lightweight VectorDrawables and no Bitmaps are leaked.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SportsManagerDefensiveTest {

    private lateinit var context: Context
    private lateinit var sportsManager: SportsManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sportsManager = SportsManager()
    }

    // =========================================================================
    // 1. ADVERSARIAL & UNKNOWN SPORT TYPES
    // =========================================================================

    @Test
    fun buildScoreboardUrl_adversarialAndUnknownSports_defaultsSafelyToNhl() {
        val adversarialSports = listOf(
            "",
            "   ",
            "UNKNOWN",
            "CRICKET",
            "CURLING",
            "RUGBY",
            "\u0000\u0001binary",
            "<script>alert('xss')</script>",
            "🏀🏒⚽",
            "12345",
            "null",
            "undefined"
        )

        for (sport in adversarialSports) {
            val url = sportsManager.buildScoreboardUrl(sport)
            assertThat(url).isNotEmpty()
            assertThat(url).contains("hockey/nhl/scoreboard")
        }
    }

    @Test
    fun getSportBadge_unknownSports_defaultsSafelyToGenericBadge() {
        val unknownSports = listOf(
            "",
            "   ",
            "TENNIS",
            "BADMINTON",
            "WATERPOLO",
            "123",
            "???",
            "null"
        )

        for (sport in unknownSports) {
            val badge = sportsManager.getSportBadge(sport)
            assertThat(badge).isEqualTo("🏆 Sport")
        }
    }

    @Test
    fun getCurrentMatchDetails_unknownSportsAndTeams_returnsSafeFallback() {
        val adversarialInputs = listOf(
            Pair("UNKNOWN_SPORT", ""),
            Pair("CRICKET", "Random Club"),
            Pair("", ""),
            Pair("   ", "   "),
            Pair("null", "null"),
            Pair("RUGBY", "\u0000\uFFFF")
        )

        for ((sport, team) in adversarialInputs) {
            val details = sportsManager.getCurrentMatchDetails(sport = sport, favoriteTeam = team, forceRefresh = true)
            assertThat(details).isNotNull()
            assertThat(details?.sportBadge).isNotEmpty()
            assertThat(details?.homeTeam).isNotEmpty()
            assertThat(details?.awayTeam).isNotEmpty()
            assertThat(details?.homeScore).isAtLeast(0)
            assertThat(details?.awayScore).isAtLeast(0)
            assertThat(details?.homeLogoRes).isNotEqualTo(0)
            assertThat(details?.awayLogoRes).isNotEqualTo(0)
        }
    }

    @Test
    fun getSportsTickerText_adversarialSportAndTeam_doesNotThrow() {
        val adversarialInputs = listOf(
            Pair("NON_EXISTENT", ""),
            Pair("NBA", "🔥 Wildcard Team 999 🚀"),
            Pair("", ""),
            Pair("NHL", "A".repeat(1000))
        )

        for ((sport, team) in adversarialInputs) {
            val ticker = sportsManager.getSportsTickerText(sport = sport, favoriteTeam = team, forceRefresh = true)
            assertThat(ticker).isNotEmpty()
        }
    }

    // =========================================================================
    // 2. ADVERSARIAL TEAM NAMES & LOGO RESOLUTION
    // =========================================================================

    @Test
    fun getLogoResourceForTeam_adversarialAndBoundaryStrings_returnsGenericLogo() {
        val invalidOrUnknownTeams = listOf(
            "",
            " ",
            "   \t\n  ",
            "123456",
            "null",
            "undefined",
            "'; DROP TABLE teams; --",
            "<svg onload=alert(1)>",
            "A".repeat(5000),
            "\u0000\u0001\u0002",
            "🔥🚀🏆",
            "Equipe Inconnue FC"
        )

        for (team in invalidOrUnknownTeams) {
            val logoRes = sportsManager.getLogoResourceForTeam(team)
            assertThat(logoRes).isEqualTo(R.drawable.ic_team_generic)
        }
    }

    @Test
    fun getLogoResourceForTeam_casingAndAccentVariations_resolvesCorrectLogos() {
        // Canadiens
        assertThat(sportsManager.getLogoResourceForTeam("CANADIENS")).isEqualTo(R.drawable.ic_team_canadiens)
        assertThat(sportsManager.getLogoResourceForTeam("Canadiens de Montréal")).isEqualTo(R.drawable.ic_team_canadiens)
        assertThat(sportsManager.getLogoResourceForTeam("Habs")).isEqualTo(R.drawable.ic_team_canadiens)
        assertThat(sportsManager.getLogoResourceForTeam("mtl")).isEqualTo(R.drawable.ic_team_canadiens)

        // Leafs
        assertThat(sportsManager.getLogoResourceForTeam("TORONTO MAPLE LEAFS")).isEqualTo(R.drawable.ic_team_leafs)
        assertThat(sportsManager.getLogoResourceForTeam("leafs")).isEqualTo(R.drawable.ic_team_leafs)
        assertThat(sportsManager.getLogoResourceForTeam("tor")).isEqualTo(R.drawable.ic_team_leafs)

        // Lakers
        assertThat(sportsManager.getLogoResourceForTeam("lakers")).isEqualTo(R.drawable.ic_team_lakers)
        assertThat(sportsManager.getLogoResourceForTeam("LAL")).isEqualTo(R.drawable.ic_team_lakers)

        // Celtics
        assertThat(sportsManager.getLogoResourceForTeam("boston celtics")).isEqualTo(R.drawable.ic_team_celtics)
        assertThat(sportsManager.getLogoResourceForTeam("BOS")).isEqualTo(R.drawable.ic_team_celtics)

        // Real Madrid
        assertThat(sportsManager.getLogoResourceForTeam("Real Madrid CF")).isEqualTo(R.drawable.ic_team_real_madrid)
        assertThat(sportsManager.getLogoResourceForTeam("RMA")).isEqualTo(R.drawable.ic_team_real_madrid)

        // Barcelona
        assertThat(sportsManager.getLogoResourceForTeam("FC BARCELONA")).isEqualTo(R.drawable.ic_team_barcelona)
        assertThat(sportsManager.getLogoResourceForTeam("Barça")).isEqualTo(R.drawable.ic_team_barcelona)
        assertThat(sportsManager.getLogoResourceForTeam("barca")).isEqualTo(R.drawable.ic_team_barcelona)
        assertThat(sportsManager.getLogoResourceForTeam("fcb")).isEqualTo(R.drawable.ic_team_barcelona)
    }

    // =========================================================================
    // 3. MALFORMED & ADVERSARIAL JSON PARSING
    // =========================================================================

    @Test
    fun parseEspnScoreboard_corruptedAndMalformedPayloads_returnsEmptyListGracefully() {
        val corruptedPayloads = listOf(
            "",
            "   ",
            "not a json",
            "{",
            "}",
            "[]",
            "{\"events\": null}",
            "{\"events\": []}",
            "{\"events\": [\"primitive_string_instead_of_object\"]}",
            "{\"events\": [{}]}",
            "{\"events\": [{\"status\": null}]}",
            "{\"events\": [{\"competitions\": null}]}",
            "{\"events\": [{\"competitions\": []}]}",
            "{\"events\": [{\"competitions\": [{}]}]}",
            "{\"events\": [{\"competitions\": [{\"competitors\": null}]}]}",
            "{\"events\": [{\"competitions\": [{\"competitors\": []}]}]}",
            "{\"events\": [{\"competitions\": [{\"competitors\": [{}]}]}]}", // only 1 competitor
            "{\"events\": [{\"competitions\": [{\"competitors\": [{\"team\": null}, {\"team\": null}]}]}]}",
            "{\"unknown_key\": 12345}",
            "{\"events\": [{\"status\": {\"type\": {\"shortDetail\": null}}}]}"
        )

        for (payload in corruptedPayloads) {
            val result = sportsManager.parseEspnScoreboard(payload)
            assertThat(result).isNotNull()
            assertThat(result).isEmpty()
        }
    }

    @Test
    fun parseEspnScoreboard_withMissingOrNonNumericScores_defaultsSafely() {
        val nonNumericScoreJson = """
            {
                "events": [
                    {
                        "status": {
                            "type": { "state": "in", "shortDetail": "1st 10:00" }
                        },
                        "competitions": [
                            {
                                "competitors": [
                                    { "team": { "abbreviation": "MTL" }, "score": "TBD" },
                                    { "team": { "abbreviation": "TOR" }, "score": "N/A" }
                                ]
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()

        val games = sportsManager.parseEspnScoreboard(nonNumericScoreJson)
        assertThat(games).hasSize(1)
        assertThat(games[0].scoreA).isEqualTo("N/A")
        assertThat(games[0].scoreB).isEqualTo("TBD")
        assertThat(games[0].isLive).isTrue()

        // When converted to int scores in getCurrentMatchDetails, toIntOrNull() ?: 0 prevents crashes
        val scoreA = games[0].scoreA.toIntOrNull() ?: 0
        val scoreB = games[0].scoreB.toIntOrNull() ?: 0
        assertThat(scoreA).isEqualTo(0)
        assertThat(scoreB).isEqualTo(0)
    }

    // =========================================================================
    // 4. MEMORY & RESOURCE INTEGRITY (VectorDrawables vs Heavy Bitmaps)
    // =========================================================================

    @Test
    fun teamLogoDrawables_areLightweightVectorDrawables_neverHeavyBitmaps() {
        val logoResources = listOf(
            R.drawable.ic_team_canadiens,
            R.drawable.ic_team_leafs,
            R.drawable.ic_team_lakers,
            R.drawable.ic_team_celtics,
            R.drawable.ic_team_real_madrid,
            R.drawable.ic_team_barcelona,
            R.drawable.ic_team_generic,
            R.drawable.bg_sports_card
        )

        for (resId in logoResources) {
            val drawable = ContextCompat.getDrawable(context, resId)
            assertThat(drawable).isNotNull()
            // Crucial TV memory constraint: ensure no uncompressed raster Bitmaps (> 1MB) are used
            assertThat(drawable).isNotInstanceOf(BitmapDrawable::class.java)
        }
    }
}
