package com.minimal.tvlauncher.manager

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NewsManagerTest {

    private lateinit var newsManager: NewsManager

    @Before
    fun setUp() {
        newsManager = NewsManager()
    }

    @Test
    fun parseRssStream_extractsValidHeadlines() {
        val sampleRss = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <title>Google News</title>
                    <item>
                        <title>Mission Artemis : Nouvelle avancée spatiale</title>
                        <link>https://news.google.com/item1</link>
                    </item>
                    <item>
                        <title>Nouvelles découvertes océaniques &amp; environnement</title>
                        <link>https://news.google.com/item2</link>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val stream = ByteArrayInputStream(sampleRss.toByteArray(Charsets.UTF_8))
        val headlines = newsManager.parseRssStream(stream)

        assertThat(headlines).hasSize(2)
        assertThat(headlines[0]).isEqualTo("Mission Artemis : Nouvelle avancée spatiale")
        assertThat(headlines[1]).isEqualTo("Nouvelles découvertes océaniques & environnement")
    }

    @Test
    fun parseRssStream_handlesEmptyOrMalformedXmlGracefully() {
        val malformedXml = "<rss><channel><item><title>Non fermé"
        val stream = ByteArrayInputStream(malformedXml.toByteArray(Charsets.UTF_8))
        val headlines = newsManager.parseRssStream(stream)
        assertThat(headlines).isNotNull()
    }

    @Test
    fun getNewsTickerText_returnsValidDefaultWhenOffline() {
        val ticker = newsManager.getNewsTickerText(forceRefresh = false)
        assertThat(ticker).isNotEmpty()
        assertThat(ticker).startsWith("Google News :")
    }
}
