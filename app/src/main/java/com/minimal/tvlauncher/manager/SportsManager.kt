package com.minimal.tvlauncher.manager

import com.minimal.tvlauncher.config.AppConfig
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Lightweight, zero-drain Live Sports Scoreboard fetcher.
 * Uses public scoreboard APIs (ESPN public endpoints) strictly over HTTPS without API keys.
 * Caches in-memory to strictly preserve low RAM (< 30 MB).
 */
class SportsManager {

    companion object {
        const val SPORT_NHL = "NHL"
        const val SPORT_NBA = "NBA"
        const val SPORT_NFL = "NFL"
        const val SPORT_SOCCER = "SOCCER"
        const val SPORT_MLB = "MLB"

        const val SOURCE_ESPN = "ESPN"
        const val SOURCE_GOOGLE = "Google Sports"
        const val SOURCE_LIVESCORE = "LiveScore"
    }

    private var cachedSportsString: String? = null
    private var lastFetchTimeMs: Long = 0L
    private var lastQuerySignature: String = ""

    data class GameScore(
        val teamA: String,
        val scoreA: String,
        val teamB: String,
        val scoreB: String,
        val status: String,
        val isLive: Boolean
    )

    fun buildScoreboardUrl(sport: String): String {
        return when (sport.uppercase()) {
            SPORT_NBA -> "${AppConfig.Endpoints.ESPN_SCOREBOARD_BASE_URL}basketball/nba/scoreboard"
            SPORT_NFL -> "${AppConfig.Endpoints.ESPN_SCOREBOARD_BASE_URL}football/nfl/scoreboard"
            SPORT_SOCCER -> "${AppConfig.Endpoints.ESPN_SCOREBOARD_BASE_URL}soccer/eng.1/scoreboard"
            SPORT_MLB -> "${AppConfig.Endpoints.ESPN_SCOREBOARD_BASE_URL}baseball/mlb/scoreboard"
            else -> "${AppConfig.Endpoints.ESPN_SCOREBOARD_BASE_URL}hockey/nhl/scoreboard"
        }
    }

    fun getSportsTickerText(
        sport: String = SPORT_NHL,
        favoriteTeam: String = "",
        forceRefresh: Boolean = false
    ): String {
        val now = System.currentTimeMillis()
        val sig = "$sport|$favoriteTeam"
        if (!forceRefresh && sig == lastQuerySignature && cachedSportsString != null && (now - lastFetchTimeMs) < AppConfig.Cache.SPORTS_CACHE_DURATION_MS) {
            return cachedSportsString!!
        }

        val games = fetchGames(sport)
        val icon = when (sport.uppercase()) {
            SPORT_NHL -> "🏒 LNH"
            SPORT_NBA -> "🏀 NBA"
            SPORT_NFL -> "🏈 NFL"
            SPORT_SOCCER -> "⚽ Foot"
            SPORT_MLB -> "⚾ MLB"
            else -> "🏆 Sport"
        }

        val prioritizedGame = if (favoriteTeam.isNotBlank()) {
            games.find {
                it.teamA.contains(favoriteTeam, ignoreCase = true) ||
                        it.teamB.contains(favoriteTeam, ignoreCase = true)
            } ?: games.firstOrNull()
        } else {
            games.firstOrNull()
        }

        val result = if (prioritizedGame != null) {
            "$icon : ${prioritizedGame.teamA} ${prioritizedGame.scoreA} - ${prioritizedGame.scoreB} ${prioritizedGame.teamB} (${prioritizedGame.status})"
        } else {
            val teamSuffix = if (favoriteTeam.isNotBlank()) " ($favoriteTeam)" else ""
            cachedSportsString ?: "$icon$teamSuffix : Aucun match en cours"
        }

        cachedSportsString = result
        lastFetchTimeMs = now
        lastQuerySignature = sig
        return result
    }

    fun fetchGames(sport: String): List<GameScore> {
        val urlString = buildScoreboardUrl(sport)
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpsURLConnection
            conn.connectTimeout = AppConfig.Timeouts.NETWORK_CONNECT_MS
            conn.readTimeout = AppConfig.Timeouts.NETWORK_READ_MS
            conn.requestMethod = "GET"

            if (conn.responseCode != HttpsURLConnection.HTTP_OK) {
                conn.disconnect()
                return emptyList()
            }

            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            parseEspnScoreboard(text)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun parseEspnScoreboard(jsonText: String): List<GameScore> {
        val list = mutableListOf<GameScore>()
        try {
            val root = JSONObject(jsonText)
            val events = root.optJSONArray("events") ?: return emptyList()

            for (i in 0 until events.length()) {
                val event = events.getJSONObject(i)
                val statusObj = event.optJSONObject("status")
                val typeObj = statusObj?.optJSONObject("type")
                val statusDetail = typeObj?.optString("shortDetail", "Fin") ?: "Fin"
                val state = typeObj?.optString("state", "post") ?: "post"
                val isLive = state == "in"

                val competitions = event.optJSONArray("competitions")
                if (competitions != null && competitions.length() > 0) {
                    val comp = competitions.getJSONObject(0)
                    val competitors = comp.optJSONArray("competitors")
                    if (competitors != null && competitors.length() >= 2) {
                        val comp1 = competitors.getJSONObject(0)
                        val comp2 = competitors.getJSONObject(1)

                        val team1 = comp1.getJSONObject("team").optString("abbreviation", "T1")
                        val score1 = comp1.optString("score", "0")

                        val team2 = comp2.getJSONObject("team").optString("abbreviation", "T2")
                        val score2 = comp2.optString("score", "0")

                        list.add(GameScore(team2, score2, team1, score1, statusDetail, isLive))
                    }
                }
            }
        } catch (_: Exception) {
        }
        return list
    }
}
