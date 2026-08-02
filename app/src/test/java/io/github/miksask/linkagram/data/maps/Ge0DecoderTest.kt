package io.github.miksask.linkagram.data.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Ge0DecoderTest {
    @Test
    fun decode_knownShareCode_returnsRoundedCoordinates() {
        // https://omaps.app/04NPTpEAVb/… — Łąkowa, 23/25
        assertEquals(51.7566 to 19.43773, Ge0Decoder.decode("04NPTpEAVb"))
    }

    @Test
    fun decode_loftLodzShareCode_returnsCoordinates() {
        // https://omaps.app/04NPTpGL6Z/LoftLodz
        assertEquals(51.75761 to 19.43783, Ge0Decoder.decode("04NPTpGL6Z"))
    }

    @Test
    fun decode_urlProcessorSample_returnsCoordinates() {
        // Path code from organicmaps/url-processor README examples.
        assertEquals(64.5234 to 12.1234, Ge0Decoder.decode("B4srhdHVVt"))
    }

    @Test
    fun decode_empty_returnsNull() {
        assertNull(Ge0Decoder.decode(""))
    }

    @Test
    fun decode_invalidAlphabet_returnsNull() {
        assertNull(Ge0Decoder.decode("!!!"))
        assertNull(Ge0Decoder.decode("0.1,2.3"))
    }
}
