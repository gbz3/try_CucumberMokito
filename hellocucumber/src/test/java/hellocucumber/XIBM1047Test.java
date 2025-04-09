package hellocucumber;

import mycode.xIBM1047_1Byte;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class XIBM1047Test {

    @Test
    void testCharset() {
        var actualCharset = new xIBM1047_1Byte("xIBM1047", new String[] {});
        var expectedCharset = Charset.forName("IBM1047");

        var actualBytes = IntStream.rangeClosed(0x00, 0x7F)
                .mapToObj(codepoint -> new String(Character.toChars(codepoint)))
                .collect(Collectors.joining())
                .getBytes(actualCharset);
        var expectedBytes = IntStream.rangeClosed(0x00, 0x7F)
                .mapToObj(codepoint -> new String(Character.toChars(codepoint)))
                .collect(Collectors.joining())
                .getBytes(expectedCharset);

        for (var i = 0; i < actualBytes.length; i++) {
            var format = i % 16 == 0? "%n%02X": " %02X";
            System.out.printf(format, actualBytes[i]);
        }

        assertThat(actualBytes).isEqualTo(expectedBytes);
    }
}
