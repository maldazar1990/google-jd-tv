package com.minimal.tvlauncher.manager

import com.google.common.truth.Truth.assertThat
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
}
