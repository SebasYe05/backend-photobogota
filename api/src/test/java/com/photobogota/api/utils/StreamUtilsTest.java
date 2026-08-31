package com.photobogota.api.utils;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StreamUtilsTest {

    @Test
    void lastN_conMasElementosQueN_devuelveLosUltimosNEnOrden() {
        List<Integer> resultado = IntStream.rangeClosed(1, 10).boxed()
                .collect(StreamUtils.lastN(3));

        assertThat(resultado).containsExactly(8, 9, 10);
    }

    @Test
    void lastN_conExactamenteN_devuelveTodo() {
        List<Integer> resultado = IntStream.rangeClosed(1, 3).boxed()
                .collect(StreamUtils.lastN(3));

        assertThat(resultado).containsExactly(1, 2, 3);
    }

    @Test
    void lastN_conMenosElementosQueN_devuelveTodo() {
        List<Integer> resultado = IntStream.rangeClosed(1, 3).boxed()
                .collect(StreamUtils.lastN(10));

        assertThat(resultado).containsExactly(1, 2, 3);
    }

    @Test
    void lastN_conNEnCero_devuelveUnaListaVacia() {
        List<Integer> resultado = IntStream.rangeClosed(1, 3).boxed()
                .collect(StreamUtils.lastN(0));

        assertThat(resultado).isEmpty();
    }

    @Test
    void lastN_streamVacio_devuelveUnaListaVacia() {
        List<Integer> resultado = java.util.stream.Stream.<Integer>empty()
                .collect(StreamUtils.lastN(3));

        assertThat(resultado).isEmpty();
    }

    @Test
    void lastN_conStreamParalelo_preservaLosUltimosNEnOrden() {
        List<Integer> resultado = IntStream.rangeClosed(0, 9_999).boxed().parallel()
                .collect(StreamUtils.lastN(5));

        assertThat(resultado).containsExactly(9_995, 9_996, 9_997, 9_998, 9_999);
    }

    @Test
    void lastN_conDuplicadosMantieneLasInstancias() {
        List<String> resultado = java.util.stream.Stream.of("a", "b", "b", "c", "d").collect(StreamUtils.lastN(3));

        assertThat(resultado).containsExactly("b", "c", "d");
    }
}