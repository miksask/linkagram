package io.github.miksask.linkagram.data.extract

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RichLinkHostAllowlistTest {
    @Test
    fun shouldCapture_koleoHosts() {
        assertTrue(RichLinkHostAllowlist.shouldCapture("koleo.pl"))
        assertTrue(RichLinkHostAllowlist.shouldCapture("www.koleo.pl"))
        assertTrue(RichLinkHostAllowlist.shouldCapture("KOLEO.PL"))
    }

    @Test
    fun shouldCapture_rejectsOthers() {
        assertFalse(RichLinkHostAllowlist.shouldCapture("example.com"))
        assertFalse(RichLinkHostAllowlist.shouldCapture("fakecoleo.pl"))
        assertFalse(RichLinkHostAllowlist.shouldCapture(null))
        assertFalse(RichLinkHostAllowlist.shouldCapture(""))
    }
}
