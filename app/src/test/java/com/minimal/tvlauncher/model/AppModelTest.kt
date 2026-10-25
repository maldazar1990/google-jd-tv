package com.minimal.tvlauncher.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppModelTest {

    @Test
    fun defaultValues_areAppliedProperly() {
        val app = AppModel(
            packageName = "com.google.android.youtube.tv",
            activityName = "com.google.android.youtube.tv.MainActivity",
            label = "YouTube TV",
            isLeanback = true
        )

        assertThat(app.packageName).isEqualTo("com.google.android.youtube.tv")
        assertThat(app.activityName).isEqualTo("com.google.android.youtube.tv.MainActivity")
        assertThat(app.label).isEqualTo("YouTube TV")
        assertThat(app.isLeanback).isTrue()
        assertThat(app.banner).isNull()
        assertThat(app.icon).isNull()
        assertThat(app.isFavorite).isFalse()
        assertThat(app.isHidden).isFalse()
        assertThat(app.isSystemApp).isFalse()
    }

    @Test
    fun explicitConstructorValues_areAppliedProperly() {
        val app = AppModel(
            packageName = "com.netflix.ninja",
            activityName = "com.netflix.ninja.MainActivity",
            label = "Netflix",
            isLeanback = true,
            banner = null,
            icon = null,
            isFavorite = true,
            isHidden = true,
            isSystemApp = true
        )

        assertThat(app.isFavorite).isTrue()
        assertThat(app.isHidden).isTrue()
        assertThat(app.isSystemApp).isTrue()
    }

    @Test
    fun structuralEquality_identicalInstances_evaluateEqualWithMatchingHashCode() {
        val app1 = AppModel(
            packageName = "com.example.app",
            activityName = "com.example.app.MainActivity",
            label = "Example",
            isLeanback = true,
            isFavorite = false,
            isHidden = false,
            isSystemApp = false
        )
        val app2 = AppModel(
            packageName = "com.example.app",
            activityName = "com.example.app.MainActivity",
            label = "Example",
            isLeanback = true,
            isFavorite = false,
            isHidden = false,
            isSystemApp = false
        )

        assertThat(app1).isEqualTo(app2)
        assertThat(app1.hashCode()).isEqualTo(app2.hashCode())
    }

    @Test
    fun structuralEquality_differentFields_evaluateNotEqual() {
        val base = AppModel(
            packageName = "com.example.app",
            activityName = "com.example.app.MainActivity",
            label = "Example",
            isLeanback = true
        )

        val diffPackage = base.copy(packageName = "com.other.app")
        val diffActivity = base.copy(activityName = "com.example.app.OtherActivity")
        val diffLabel = base.copy(label = "Other")
        val diffLeanback = base.copy(isLeanback = false)
        val diffFavorite = base.copy(isFavorite = true)
        val diffHidden = base.copy(isHidden = true)
        val diffSystem = base.copy(isSystemApp = true)

        assertThat(base).isNotEqualTo(diffPackage)
        assertThat(base).isNotEqualTo(diffActivity)
        assertThat(base).isNotEqualTo(diffLabel)
        assertThat(base).isNotEqualTo(diffLeanback)
        assertThat(base).isNotEqualTo(diffFavorite)
        assertThat(base).isNotEqualTo(diffHidden)
        assertThat(base).isNotEqualTo(diffSystem)
    }

    @Test
    fun copy_togglingFavorite_preservesAllOtherFields() {
        val original = AppModel(
            packageName = "com.spotify.tv",
            activityName = "com.spotify.tv.MainActivity",
            label = "Spotify",
            isLeanback = true,
            isFavorite = false,
            isHidden = false,
            isSystemApp = false
        )

        val modified = original.copy(isFavorite = true)

        assertThat(modified.isFavorite).isTrue()
        assertThat(modified.packageName).isEqualTo(original.packageName)
        assertThat(modified.activityName).isEqualTo(original.activityName)
        assertThat(modified.label).isEqualTo(original.label)
        assertThat(modified.isLeanback).isEqualTo(original.isLeanback)
        assertThat(modified.isHidden).isEqualTo(original.isHidden)
        assertThat(modified.isSystemApp).isEqualTo(original.isSystemApp)
    }

    @Test
    fun copy_togglingHidden_preservesAllOtherFields() {
        val original = AppModel(
            packageName = "com.bloatware.tv",
            activityName = "com.bloatware.tv.MainActivity",
            label = "Bloat",
            isLeanback = false,
            isFavorite = false,
            isHidden = false
        )

        val modified = original.copy(isHidden = true)

        assertThat(modified.isHidden).isTrue()
        assertThat(modified.packageName).isEqualTo(original.packageName)
    }

    @Test
    fun copy_noArguments_producesEqualInstance() {
        val original = AppModel(
            packageName = "com.test.app",
            activityName = "com.test.app.MainActivity",
            label = "Test",
            isLeanback = true
        )

        val copy = original.copy()

        assertThat(copy).isEqualTo(original)
        assertThat(copy.hashCode()).isEqualTo(original.hashCode())
    }

    @Test
    fun toString_containsEssentialFields() {
        val app = AppModel(
            packageName = "com.example.app",
            activityName = "com.example.app.MainActivity",
            label = "Example Title",
            isLeanback = true
        )

        val stringRep = app.toString()

        assertThat(stringRep).contains("packageName=com.example.app")
        assertThat(stringRep).contains("label=Example Title")
        assertThat(stringRep).contains("isLeanback=true")
    }
}
