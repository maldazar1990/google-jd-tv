package com.minimal.tvlauncher.manager

import com.google.common.truth.Truth.assertThat
import com.minimal.tvlauncher.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SportsManagerTest {

    private lateinit var sportsManager: SportsManager

    @Before
    fun setUp() {
        sportsManager = SportsManager()
    }

    @Test
    fun buildScoreboardUrl_generatesValidEndpoints() {
        val nhlUrl = sportsManager.buildScoreboardUrl(SportsManager.SPORT_NHL)
        assertThat(nhlUrl).contains("hockey/nhl/scoreboard")

        val nbaUrl = sportsManager.buildScoreboardUrl(SportsManager.SPORT_NBA)
        assertThat(nbaUrl).contains("basketball/nba/scoreboard")

        val soccerUrl = sportsManager.buildScoreboardUrl(SportsManager.SPORT_SOCCER)
        assertThat(soccerUrl).contains("soccer")

        val nflUrl = sportsManager.buildScoreboardUrl(SportsManager.SPORT_NFL)
        assertThat(nflUrl).contains("football/nfl/scoreboard")

        val mlbUrl = sportsManager.buildScoreboardUrl(SportsManager.SPORT_MLB)
        assertThat(mlbUrl).contains("baseball/mlb/scoreboard")
    }

    @Test
    fun parseEspnScoreboard_extractsTeamsAndScores() {
        val sampleJson = """
            {
                "events": [
                    {
                        "status": {
                            "type": {
                                "state": "post",
                                "shortDetail": "Fin"
                            }
                        },
                        "competitions": [
                            {
                                "competitors": [
                                    { "team": { "abbreviation": "BOS" }, "score": "2" },
                                    { "team": { "abbreviation": "MTL" }, "score": "3" }
                                ]
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()

        val games = sportsManager.parseEspnScoreboard(sampleJson)
        assertThat(games).hasSize(1)
        assertThat(games[0].teamA).isEqualTo("MTL")
        assertThat(games[0].scoreA).isEqualTo("3")
        assertThat(games[0].teamB).isEqualTo("BOS")
        assertThat(games[0].scoreB).isEqualTo("2")
        assertThat(games[0].status).isEqualTo("Fin")
    }

    @Test
    fun getSportsTickerText_returnsFallbackWhenNoLiveGames() {
        val ticker = sportsManager.getSportsTickerText(SportsManager.SPORT_NHL, favoriteTeam = "Canadiens")
        assertThat(ticker).isNotEmpty()
        assertThat(ticker).startsWith("🏒 LNH")
    }

    @Test
    fun getLogoResourceForTeam_matchesCanadiensVariations() {
        val variations = listOf("Canadiens", "Habs", "canadiens de montréal", "MTL", "Montreal Canadiens", "montreal")
        for (team in variations) {
            assertThat(sportsManager.getLogoResourceForTeam(team))
                .isEqualTo(R.drawable.ic_team_canadiens)
        }
    }

    @Test
    fun getLogoResourceForTeam_matchesLeafsVariations() {
        val variations = listOf("Maple Leafs", "Leafs", "Toronto Maple Leafs", "tor", "Toronto")
        for (team in variations) {
            assertThat(sportsManager.getLogoResourceForTeam(team))
                .isEqualTo(R.drawable.ic_team_leafs)
        }
    }

    @Test
    fun getLogoResourceForTeam_matchesLakersVariations() {
        val variations = listOf("Lakers", "Los Angeles Lakers", "lal", "LA Lakers")
        for (team in variations) {
            assertThat(sportsManager.getLogoResourceForTeam(team))
                .isEqualTo(R.drawable.ic_team_lakers)
        }
    }

    @Test
    fun getLogoResourceForTeam_matchesCelticsVariations() {
        val variations = listOf("Celtics", "Boston Celtics", "bos")
        for (team in variations) {
            assertThat(sportsManager.getLogoResourceForTeam(team))
                .isEqualTo(R.drawable.ic_team_celtics)
        }
    }

    @Test
    fun getLogoResourceForTeam_matchesRealMadridVariations() {
        val variations = listOf("Real Madrid", "rma", "Real")
        for (team in variations) {
            assertThat(sportsManager.getLogoResourceForTeam(team))
                .isEqualTo(R.drawable.ic_team_real_madrid)
        }
    }

    @Test
    fun getLogoResourceForTeam_matchesBarcelonaVariations() {
        val variations = listOf("Barcelona", "Barça", "barca", "FC Barcelona", "fcb")
        for (team in variations) {
            assertThat(sportsManager.getLogoResourceForTeam(team))
                .isEqualTo(R.drawable.ic_team_barcelona)
        }
    }

    @Test
    fun getLogoResourceForTeam_matchesNewOfficialLogos() {
        // PSG
        assertThat(sportsManager.getLogoResourceForTeam("Paris SG")).isEqualTo(R.drawable.ic_team_psg)
        assertThat(sportsManager.getLogoResourceForTeam("PSG")).isEqualTo(R.drawable.ic_team_psg)

        // Warriors
        assertThat(sportsManager.getLogoResourceForTeam("Golden State Warriors")).isEqualTo(R.drawable.ic_team_warriors)
        assertThat(sportsManager.getLogoResourceForTeam("GSW")).isEqualTo(R.drawable.ic_team_warriors)

        // Bruins
        assertThat(sportsManager.getLogoResourceForTeam("Boston Bruins")).isEqualTo(R.drawable.ic_team_bruins)

        // Oilers
        assertThat(sportsManager.getLogoResourceForTeam("Edmonton Oilers")).isEqualTo(R.drawable.ic_team_oilers)
        assertThat(sportsManager.getLogoResourceForTeam("EDM")).isEqualTo(R.drawable.ic_team_oilers)

        // Rangers
        assertThat(sportsManager.getLogoResourceForTeam("New York Rangers")).isEqualTo(R.drawable.ic_team_rangers)
        assertThat(sportsManager.getLogoResourceForTeam("NYR")).isEqualTo(R.drawable.ic_team_rangers)

        // Raptors
        assertThat(sportsManager.getLogoResourceForTeam("Toronto Raptors")).isEqualTo(R.drawable.ic_team_raptors)
    }

    @Test
    fun getLogoResourceForTeam_returnsGenericForUnknownTeam() {
        val unknowns = listOf("Unknown FC", "Team 123", "", "Random Club")
        for (team in unknowns) {
            assertThat(sportsManager.getLogoResourceForTeam(team))
                .isEqualTo(R.drawable.ic_team_generic)
        }
    }

    @Test
    fun getCurrentMatchDetails_returnsValidNhlDetails() {
        val details = sportsManager.getCurrentMatchDetails(SportsManager.SPORT_NHL, favoriteTeam = "Canadiens")
        assertThat(details).isNotNull()
        assertThat(details?.sportBadge).isEqualTo("🏒 LNH")
        assertThat(details?.homeTeam).isNotEmpty()
        assertThat(details?.awayTeam).isNotEmpty()
        assertThat(details?.homeScore).isAtLeast(0)
        assertThat(details?.awayScore).isAtLeast(0)
        assertThat(details?.status).isNotEmpty()
        assertThat(details?.homeLogoRes).isNotEqualTo(0)
        assertThat(details?.awayLogoRes).isNotEqualTo(0)
    }

    @Test
    fun getCurrentMatchDetails_returnsValidNbaDetails() {
        val details = sportsManager.getCurrentMatchDetails(SportsManager.SPORT_NBA, favoriteTeam = "Lakers")
        assertThat(details).isNotNull()
        assertThat(details?.sportBadge).isEqualTo("🏀 NBA")
        assertThat(details?.homeLogoRes).isEqualTo(R.drawable.ic_team_lakers)
        assertThat(details?.awayLogoRes).isEqualTo(R.drawable.ic_team_celtics)
    }

    @Test
    fun getCurrentMatchDetails_returnsValidSoccerDetails() {
        val details = sportsManager.getCurrentMatchDetails(SportsManager.SPORT_SOCCER, favoriteTeam = "Real Madrid")
        assertThat(details).isNotNull()
        assertThat(details?.sportBadge).isEqualTo("⚽ Foot")
        assertThat(details?.homeLogoRes).isEqualTo(R.drawable.ic_team_real_madrid)
        assertThat(details?.awayLogoRes).isEqualTo(R.drawable.ic_team_barcelona)
    }

    @Test
    fun getCurrentMatchDetails_noArgDefaults() {
        val details = sportsManager.getCurrentMatchDetails()
        assertThat(details).isNotNull()
        assertThat(details?.sportBadge).isEqualTo("🏒 LNH")
    }

    @Test
    fun getSportBadge_mapsCorrectBadges() {
        assertThat(sportsManager.getSportBadge(SportsManager.SPORT_NHL)).isEqualTo("🏒 LNH")
        assertThat(sportsManager.getSportBadge(SportsManager.SPORT_NBA)).isEqualTo("🏀 NBA")
        assertThat(sportsManager.getSportBadge(SportsManager.SPORT_SOCCER)).isEqualTo("⚽ Foot")
        assertThat(sportsManager.getSportBadge(SportsManager.SPORT_NFL)).isEqualTo("🏈 NFL")
        assertThat(sportsManager.getSportBadge(SportsManager.SPORT_MLB)).isEqualTo("⚾ MLB")
    }
}
