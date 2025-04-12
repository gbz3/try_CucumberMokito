package tsv;

import org.assertj.core.presentation.HexadecimalRepresentation;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;

public class TsvReaderTest {

    static byte[] toBytes(String hex) {
        return HexFormat.ofDelimiter(" ").parseHex(hex);
    }

    static @NotNull List<byte[]> toList(String @NotNull ... hex) {
        var list = new ArrayList<byte[]>();
        for (String s : hex) {
            list.add(toBytes(s));
        }
        return list;
    }

    @Test
    void testCutoutFields() {
        BiConsumer<String, Optional<List<byte[]>>> ok = (actual, expected) -> {
            var bytes = toBytes(actual);
            var buff = ByteBuffer.allocate(bytes.length);
            buff.put(bytes);
            assertThat(TsvReader.cutoutFields(buff))
                    .usingRecursiveComparison()
                    .withRepresentation(new HexadecimalRepresentation())
                    .isEqualTo(expected);
        };

        // field 1個
        ok.accept("", Optional.empty());
        ok.accept("25", Optional.of(toList("")));
        ok.accept("F0 25", Optional.of(toList("F0")));
        ok.accept("F0 F1 25", Optional.of(toList("F0 F1")));
        ok.accept("F0 F1 25 00", Optional.of(toList("F0 F1")));
        ok.accept("F0 F1 25 00 00", Optional.of(toList("F0 F1")));

        // field 2個
        ok.accept("05 25", Optional.of(toList("", "")));
        ok.accept("F0 05 F0 25", Optional.of(toList("F0", "F0")));
        ok.accept("F0 F1 05 F0 F1 25", Optional.of(toList("F0 F1", "F0 F1")));
        ok.accept("05 F0 F1 25", Optional.of(toList("", "F0 F1")));
        ok.accept("F0 F1 05 25", Optional.of(toList("F0 F1", "")));

        // field 3個
        ok.accept("05 05 25", Optional.of(toList("", "", "")));
        ok.accept("F0 05 F0 05 F0 25", Optional.of(toList("F0", "F0", "F0")));
        ok.accept("F0 F1 05 F0 F1 05 F0 F1 25", Optional.of(toList("F0 F1", "F0 F1", "F0 F1")));
        ok.accept("05 F0 F1 05 F0 F1 25", Optional.of(toList("", "F0 F1", "F0 F1")));
        ok.accept("F0 F1 05 05 F0 F1 25", Optional.of(toList("F0 F1", "", "F0 F1")));
        ok.accept("F0 F1 05 F0 F1 05 25", Optional.of(toList("F0 F1", "F0 F1", "")));
    }
}
