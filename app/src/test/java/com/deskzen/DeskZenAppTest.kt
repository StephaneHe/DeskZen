package com.deskzen

import dagger.hilt.android.HiltAndroidApp
import org.junit.Assert.assertNotNull
import org.junit.Test

class DeskZenAppTest {

    @Test
    fun `app class is annotated with HiltAndroidApp`() {
        val annotation = DeskZenApp::class.java.getAnnotation(HiltAndroidApp::class.java)
        assertNotNull("DeskZenApp must be annotated with @HiltAndroidApp", annotation)
    }

    @Test
    fun `app class extends Application`() {
        assert(android.app.Application::class.java.isAssignableFrom(DeskZenApp::class.java))
    }
}
