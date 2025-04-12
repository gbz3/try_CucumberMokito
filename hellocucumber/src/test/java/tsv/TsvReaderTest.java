package tsv;

import org.assertj.core.presentation.HexadecimalRepresentation;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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

    @Test
    void testWriteBlock(@TempDir @NotNull Path tempDir) throws IOException {
        var outFile = tempDir.resolve("out");

        BiConsumer<String, Expect> ok = (actual, expected) -> {
            var actualBytes = HexFormat.of().parseHex(actual);
            try (var outChannel = FileChannel.open(outFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
                var buff = ByteBuffer.allocate(4096);
                TsvReader.writeBlock(outChannel, buff, actualBytes);
                buff.flip();
                var buffBytes = new byte[buff.remaining()];
                buff.get(buffBytes);
                assertThat(buffBytes)
                        .withRepresentation(new HexadecimalRepresentation())
                        .isEqualTo(expected.buff);

                var bytes = Files.readAllBytes(outFile);
                assertThat(bytes)
                        .withRepresentation(new HexadecimalRepresentation())
                        .isEqualTo(expected.block);
            } catch (IOException e) {
                fail(e);
            }
        };

        // 0ブロック出力
        ok.accept("F1".repeat(1), new Expect(toBytes("00 00 00 01 F1"), new byte[0]));
        ok.accept("F1".repeat(1007), new Expect(toBytes("00 00 03 EF" + " F1".repeat(1007)), new byte[0]));

        // 1ブロック出力
        ok.accept("F1".repeat(0), new Expect(toBytes(""), toBlock("")));
        //ok.accept("F1".repeat(1008), new Expect(toBytes("00 00 03 EC" + " F1".repeat(1008)), toBlock("F1".repeat(1008))));
    }

    record Expect(byte[] buff, byte[] block) {}

    static byte[] toBlock(String @NotNull ... recordHex) {
        var hex = new StringBuilder();
        for (var record : recordHex) {
            if (record.isEmpty()) break;
            var bytes = HexFormat.of().parseHex(record);
            var buff = ByteBuffer.allocate(Integer.BYTES + bytes.length);
            buff.putInt(bytes.length);
            buff.put(bytes);
            hex.append(HexFormat.of().formatHex(buff.array()));
        }
        var buff = ByteBuffer.allocate(Integer.BYTES);
        buff.putInt(0);
        hex.append(HexFormat.of().formatHex(buff.array()));
        hex.append("40".repeat(1014 - hex.length() / 2));
        return HexFormat.of().parseHex(hex.toString());
    }

}
