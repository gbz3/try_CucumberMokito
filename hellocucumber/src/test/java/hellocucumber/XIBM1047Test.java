package hellocucumber;

import mycode.xIBM1047Encoder;
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

    @Test
    void testBytes() {
        assertThat(xIBM1047Encoder.getBytes(0)).isEqualTo(new byte[] { (byte) 0xF0 });
        assertThat(xIBM1047Encoder.getBytes(1)).isEqualTo(new byte[] { (byte) 0xF1 });
        assertThat(xIBM1047Encoder.getBytes(2)).isEqualTo(new byte[] { (byte) 0xF2 });
        assertThat(xIBM1047Encoder.getBytes(3)).isEqualTo(new byte[] { (byte) 0xF3 });
        assertThat(xIBM1047Encoder.getBytes(4)).isEqualTo(new byte[] { (byte) 0xF4 });
        assertThat(xIBM1047Encoder.getBytes(5)).isEqualTo(new byte[] { (byte) 0xF5 });
        assertThat(xIBM1047Encoder.getBytes(6)).isEqualTo(new byte[] { (byte) 0xF6 });
        assertThat(xIBM1047Encoder.getBytes(7)).isEqualTo(new byte[] { (byte) 0xF7 });
        assertThat(xIBM1047Encoder.getBytes(8)).isEqualTo(new byte[] { (byte) 0xF8 });
        assertThat(xIBM1047Encoder.getBytes(9)).isEqualTo(new byte[] { (byte) 0xF9 });
        assertThat(xIBM1047Encoder.getBytes(1234567890)).isEqualTo(new byte[] { (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xF5, (byte) 0xF6, (byte) 0xF7, (byte) 0xF8, (byte) 0xF9, (byte) 0xF0 });
        assertThat(xIBM1047Encoder.getBytes(Integer.MAX_VALUE)).isEqualTo(new byte[] { (byte) 0xF2, (byte) 0xF1, (byte) 0xF4, (byte) 0xF7, (byte) 0xF4, (byte) 0xF8, (byte) 0xF3, (byte) 0xF6, (byte) 0xF4, (byte) 0xF7 });
    }
}
