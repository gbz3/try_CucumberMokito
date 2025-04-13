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
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

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
        record Expected(int pos, Optional<List<byte[]>> result) {}

        BiConsumer<String, Expected> ok = (actual, expected) -> {
            var bytes = toBytes(actual);
            var buff = ByteBuffer.allocate(bytes.length);
            buff.put(bytes);
            assertThat(TsvReader.cutoutFields(buff))
                    .usingRecursiveComparison()
                    .withRepresentation(new HexadecimalRepresentation())
                    .isEqualTo(expected.result);
            assertThat(buff.position()).as(() -> String.format("pos: [%s]", actual)).isEqualTo(expected.pos);
        };

        // field 1個
        ok.accept("", new Expected(0, Optional.empty()));
        ok.accept("25", new Expected(0, Optional.of(toList(""))));
        ok.accept("F0 25", new Expected(0, Optional.of(toList("F0"))));
        ok.accept("F0 F1 25", new Expected(0, Optional.of(toList("F0 F1"))));
        ok.accept("F0 F1 25 00", new Expected(1, Optional.of(toList("F0 F1"))));
        ok.accept("F0 F1 25 00 00", new Expected(2, Optional.of(toList("F0 F1"))));

        // field 2個
        ok.accept("05 25", new Expected(0, Optional.of(toList("", ""))));
        ok.accept("F0 05 F0 25", new Expected(0, Optional.of(toList("F0", "F0"))));
        ok.accept("F0 F1 05 F0 F1 25", new Expected(0, Optional.of(toList("F0 F1", "F0 F1"))));
        ok.accept("05 F0 F1 25", new Expected(0, Optional.of(toList("", "F0 F1"))));
        ok.accept("F0 F1 05 25", new Expected(0, Optional.of(toList("F0 F1", ""))));

        // field 3個
        ok.accept("05 05 25", new Expected(0, Optional.of(toList("", "", ""))));
        ok.accept("F0 05 F0 05 F0 25", new Expected(0, Optional.of(toList("F0", "F0", "F0"))));
        ok.accept("F0 F1 05 F0 F1 05 F0 F1 25", new Expected(0, Optional.of(toList("F0 F1", "F0 F1", "F0 F1"))));
        ok.accept("05 F0 F1 05 F0 F1 25", new Expected(0, Optional.of(toList("", "F0 F1", "F0 F1"))));
        ok.accept("F0 F1 05 05 F0 F1 25", new Expected(0, Optional.of(toList("F0 F1", "", "F0 F1"))));
        ok.accept("F0 F1 05 F0 F1 05 25", new Expected(0, Optional.of(toList("F0 F1", "F0 F1", ""))));
    }

    @Test
    void testWriteBlock(@TempDir @NotNull Path tempDir) {
        var outFile = tempDir.resolve("out");
        final var BUFFER_SIZE = 4096;

        BiConsumer<String[], Expect> ok = (records, expected) -> {
            try (var outChannel = FileChannel.open(outFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

                // Do
                var buff = ByteBuffer.allocate(BUFFER_SIZE);
                for (var record : records) {
                    var actual = HexFormat.of().parseHex(record);
                    TsvReader.writeBlock(outChannel, buff, actual);
                }

                buff.flip();
                var buffBytes = new byte[buff.remaining()];
                buff.get(buffBytes);
                assertThat(buffBytes)
                        .as(() -> String.format("buff: records.len=%s", Arrays.stream(records).map(String::length).toList()))
                        .withRepresentation(new HexadecimalRepresentation())
                        .isEqualTo(expected.buff);

                var bytes = Files.readAllBytes(outFile);
                assertThat(bytes)
                        .as(() -> String.format("block: records.len=%s", Arrays.stream(records).map(String::length).toList()))
                        .withRepresentation(new HexadecimalRepresentation())
                        .isEqualTo(expected.block);
            } catch (IOException e) {
                fail(e);
            }
        };

        // 1レコード出力
        IntStream.rangeClosed(0, BUFFER_SIZE - Integer.BYTES)
                .forEach(count -> {
                    var record1 = "F1".repeat(count);
                    ok.accept(new String[] {record1}, toExpect(record1));
                });

        // 2レコード出力
//        IntStream.rangeClosed(0, BUFFER_SIZE - Integer.BYTES)
//                .forEach(count -> {
//                    var record1 = "F1".repeat(count);
//                    ok.accept(new String[] {record1, ""}, toExpect(record1, ""));
//                });
        IntStream.rangeClosed(0, BUFFER_SIZE - Integer.BYTES)
                .forEach(count -> {
                    var record1 = "F1".repeat(count);
                    ok.accept(new String[] {record1, "F2"}, toExpect(record1, "F2"));
                });
        IntStream.rangeClosed(0, BUFFER_SIZE - Integer.BYTES)
                .forEach(count -> {
                    var record1 = "F1".repeat(count);
                    ok.accept(new String[] {record1, "F2F3"}, toExpect(record1, "F2F3"));
                });

        // 3レコード出力
        ok.accept(new String[] {"F1", "F2", "F3"}, toExpect("F1", "F2", "F3"));
        ok.accept(new String[] {"F1F1", "F2F2", "F3F3"}, toExpect("F1F1", "F2F2", "F3F3"));
    }

    record Expect(byte[] buff, byte[] block) {}

    static @NotNull Expect toExpect(@NotNull String @NotNull ... recordHex) {
        var hexBuilder = new StringBuilder();
        for (var record : recordHex) {
            var bytes = HexFormat.of().parseHex(record);
            var buff = ByteBuffer.allocate(Integer.BYTES + bytes.length);
            buff.putInt(bytes.length);
            buff.put(bytes);
            hexBuilder.append(HexFormat.of().formatHex(buff.array()));
            if (bytes.length == 0) {
                hexBuilder.append("40".repeat(1012 - (hexBuilder.length() / 2) % 1012));
            }
        }

        var hexData = hexBuilder.toString();
        var hexBlocks = new StringBuilder();
        for (var i = 0; i < hexData.length(); i += (1012 * 2)) {
            if (hexData.length() < i + (1012 * 2)) {
                return new Expect(
                        HexFormat.of().parseHex(hexData.substring(i)),
                        HexFormat.of().parseHex(hexBlocks.toString())
                );
            }
            hexBlocks.append(hexData, i, i + (1012 * 2));
            hexBlocks.append("4040");
        }
        return new Expect(new byte[0], HexFormat.of().parseHex(hexBlocks.toString()));
    }

    @Test
    void testReadTsv(@TempDir @NotNull Path tempDir) {
        var outFile = tempDir.resolve("write01.bin");

        record Actual(String ... hexData) {}
        BiConsumer<Actual, byte[]> ok = (actual, expected) -> {
            try {
                var readFiles = new Path[actual.hexData.length];
                for (var i = 0; i < readFiles.length; i++) {
                    readFiles[i] = tempDir.resolve(String.format("read%02d.bin", i));
                    Files.write(readFiles[i], HexFormat.ofDelimiter(" ").parseHex(actual.hexData[i]));
                }

                try (var outChannel = FileChannel.open(outFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    TsvReader.readTsv(outChannel, readFiles);
                }

                var outBytes = Files.readAllBytes(outFile);
                assertThat(outBytes.length)
                        .as(() -> String.format("hex=%s", Arrays.stream(actual.hexData).toList()))
                        .isEqualTo(expected.length);
                assertThat(outBytes)
                        .as(() -> String.format("hex=%s", Arrays.stream(actual.hexData).toList()))
                        .withRepresentation(new HexadecimalRepresentation())
                        .isEqualTo(expected);
            } catch (IOException e) {
                fail(e);
            }
        };

        // read 1ファイル
        ok.accept(new Actual(""), toBytes("00 00 00 00" + " 40".repeat(1014 - 4)));
        ok.accept(new Actual("25"), toBytes("00 00 00 00" + " 40".repeat(1014 - 4) + " 00 00 00 00" + " 40".repeat(1014 - 4)));
        ok.accept(new Actual("F1 25"), toBytes("00 00 00 01 F1 00 00 00 00" + " 40".repeat(1014 - 9)));
        ok.accept(new Actual("F1 F2 25"), toBytes("00 00 00 02 F1 F2 00 00 00 00" + " 40".repeat(1014 - 10)));
        ok.accept(new Actual("F1 05 F2 25"), toBytes("00 00 00 02 F1 F2 00 00 00 00" + " 40".repeat(1014 - 10)));
        ok.accept(new Actual("F1 F1 05 F2 F2 05 F3 F3 25"),
                toBytes("00 00 00 06 F1 F1 F2 F2 F3 F3 00 00 00 00" + " 40".repeat(1014 - 14)));

        // read 2ファイル
        ok.accept(new Actual("", ""), toBytes("00 00 00 00" + " 40".repeat(1014 - 4)));
        ok.accept(new Actual("25", "25"),
                toBytes("00 00 00 00" + " 40".repeat(1014 - 4)
                        + " 00 00 00 00" + " 40".repeat(1014 - 4)
                        + " 00 00 00 00" + " 40".repeat(1014 - 4)
                ));
        ok.accept(new Actual("F1 25", "F1 25"),
                toBytes("00 00 00 01 F1 00 00 00 01 F1 00 00 00 00" + " 40".repeat(1014 - 14)));
        ok.accept(new Actual("F1 F1 05 F2 F2 05 F3 F3 25", "F1 F1 05 F2 F2 05 F3 F3 25"),
                toBytes("00 00 00 06 F1 F1 F2 F2 F3 F3 00 00 00 06 F1 F1 F2 F2 F3 F3 00 00 00 00" + " 40".repeat(1014 - 24)));

        // read 3ファイル
        ok.accept(new Actual("", "", ""), toBytes("00 00 00 00" + " 40".repeat(1014 - 4)));
        ok.accept(new Actual("25", "25", "25"),
                toBytes("00 00 00 00" + " 40".repeat(1014 - 4)
                        + " 00 00 00 00" + " 40".repeat(1014 - 4)
                        + " 00 00 00 00" + " 40".repeat(1014 - 4)
                        + " 00 00 00 00" + " 40".repeat(1014 - 4)
                ));
        ok.accept(new Actual("F1 25", "F1 25", "F1 25"),
                toBytes("00 00 00 01 F1 00 00 00 01 F1 00 00 00 01 F1 00 00 00 00" + " 40".repeat(1014 - 19)));
        ok.accept(new Actual("F1 F1 05 F2 F2 05 F3 F3 25", "F1 F1 05 F2 F2 05 F3 F3 25", "F1 F1 05 F2 F2 05 F3 F3 25"),
                toBytes("00 00 00 06 F1 F1 F2 F2 F3 F3 00 00 00 06 F1 F1 F2 F2 F3 F3 00 00 00 06 F1 F1 F2 F2 F3 F3 00 00 00 00" + " 40".repeat(1014 - 34)));

        // 1rec + 0rec + 1rec
        ok.accept(new Actual("F1 ".repeat(70) + "25",
                        "",
                        "F3 ".repeat(100) + "25"),
                toBytes("00 00 00 46" + " F1".repeat(70)
                        + " 00 00 00 64" + " F3".repeat(100) + " 00 00 00 00" + " 40".repeat(1014 - 182)));

        // 1rec + 1rec + 1rec
        ok.accept(new Actual("F1 ".repeat(70) + "25",
                        "F2 ".repeat(300) + "25",
                        "F3 ".repeat(100) + "25"),
                toBytes("00 00 00 46" + " F1".repeat(70)
                        + " 00 00 01 2C" + " F2".repeat(300)
                        + " 00 00 00 64" + " F3".repeat(100) + " 00 00 00 00" + " 40".repeat(1014 - 486)));

        // 1rec + 1rec + 1rec = 1 block
        ok.accept(new Actual("F1 ".repeat(70) + "25",
                        "F2 ".repeat(826) + "25",
                        "F3 ".repeat(100) + "25"),
                toBytes("00 00 00 46" + " F1".repeat(70)
                        + " 00 00 03 3A" + " F2".repeat(826)
                        + " 00 00 00 64" + " F3".repeat(100) + " 00 00 00 00" + " 40".repeat(1014 - 1012)));

        // 1rec + 1rec + 1rec = 2 block
        ok.accept(new Actual("F1 ".repeat(70) + "25",
                        "F2 ".repeat(827) + "25",
                        "F3 ".repeat(100) + "25"),
                toBytes("00 00 00 46" + " F1".repeat(70)
                        + " 00 00 03 3B" + " F2".repeat(827)
                        + " 00 00 00 64" + " F3".repeat(100) + " 00 00 00 40 40"
                        + " 00" + " 40".repeat(1014 - 1)));

        // 1rec + 1rec + 1rec = 2 block
        ok.accept(new Actual("F1 ".repeat(70) + "25",
                        "F2 ".repeat(828) + "25",
                        "F3 ".repeat(100) + "25"),
                toBytes("00 00 00 46" + " F1".repeat(70)
                        + " 00 00 03 3C" + " F2".repeat(828)
                        + " 00 00 00 64" + " F3".repeat(100) + " 00 00 40 40"
                        + " 00 00" + " 40".repeat(1014 - 2)));

        // 1rec + 1rec + 1rec = 2 block
        ok.accept(new Actual("F1 ".repeat(70) + "25",
                        "F2 ".repeat(829) + "25",
                        "F3 ".repeat(100) + "25"),
                toBytes("00 00 00 46" + " F1".repeat(70)
                        + " 00 00 03 3D" + " F2".repeat(829)
                        + " 00 00 00 64" + " F3".repeat(100) + " 00 40 40"
                        + " 00 00 00" + " 40".repeat(1014 - 3)));

        // 1rec + 1rec + 1rec = 2 block
        ok.accept(new Actual("F1 ".repeat(70) + "25",
                        "F2 ".repeat(830) + "25",
                        "F3 ".repeat(100) + "25"),
                toBytes("00 00 00 46" + " F1".repeat(70)
                        + " 00 00 03 3E" + " F2".repeat(830)
                        + " 00 00 00 64" + " F3".repeat(100) + " 40 40"
                        + " 00 00 00 00" + " 40".repeat(1014 - 4)));

        // 1rec + 1rec + 1rec = 2 block
        ok.accept(new Actual("F1 ".repeat(70) + "25",
                        "F2 ".repeat(831) + "25",
                        "F3 ".repeat(100) + "25"),
                toBytes("00 00 00 46" + " F1".repeat(70)
                        + " 00 00 03 3F" + " F2".repeat(831)
                        + " 00 00 00 64" + " F3".repeat(99) + " 40 40"
                        + " F3 00 00 00 00" + " 40".repeat(1014 - 5)));

    }

}
