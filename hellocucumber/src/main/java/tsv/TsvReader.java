package tsv;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class TsvReader {

    private static final Logger LOGGER = LogManager.getLogger();

    static byte fieldDelimiter = (byte)0x05;
    static byte recordDelimiter = (byte)0x25;

    record cutoutParam(@NotNull ByteBuffer buff, @NotNull Optional<List<byte[]>> result) implements Message {

        @Contract(pure = true)
        @Override
        public @NotNull String getFormattedMessage() {
            return String.format("#### %s: hex=[%s] result=%s",
                    buff, HexFormat.ofDelimiter(" ").formatHex(buff.array()),
                    result.map(cutoutParam::toHex).orElse("EMPTY")
            );
        }

        static @NotNull String toHex(@NotNull List<byte[]> bytesList) {
            var result = new StringBuilder();
            for (var bytes: bytesList) {
                if (!result.isEmpty()) result.append("][");
                result.append(HexFormat.ofDelimiter(" ").formatHex(bytes));
            }
            return "[" + result + "]";
        }

        @Contract(value = " -> new", pure = true)
        @Override
        public Object @NotNull [] getParameters() {
            return new Object[0];
        }

        @Contract(pure = true)
        @Override
        public @Nullable Throwable getThrowable() {
            return null;
        }
    }

    static @NotNull Optional<List<byte[]>> cutoutFields(@NotNull ByteBuffer buffer) {
        //System.out.printf("#### >>> buff=[%s]%n", HexFormat.ofDelimiter(" ").formatHex(buffer.array()));
        LOGGER.debug(() -> new ParameterizedMessage("#### >>> {}: hex=[{}]", buffer, HexFormat.ofDelimiter(" ").formatHex(buffer.array())));
        var recordFound = false;
        var fields = new ArrayList<byte[]>();
        buffer.flip();
        buffer.mark();
        while (buffer.hasRemaining()) {
            var b = buffer.get();
            //System.out.printf("%02X [%s]%n", b, buffer);
            if (b == fieldDelimiter || b == recordDelimiter) {
                var endPos = buffer.position() - 1;
                buffer.reset();
                var size = endPos - buffer.position();
                var bytes = new byte[size];
                buffer.get(bytes);
                //System.out.printf("#### bytes=[%s]%n", HexFormat.ofDelimiter(" ").formatHex(bytes));
                fields.add(bytes);
                buffer.position(buffer.position() + 1);
                buffer.mark();
                if (b == recordDelimiter) {
                    recordFound = true;
                    break;
                }
            }
        }
        if (recordFound) {
            buffer.reset();
        } else {
            buffer.position(0);
            fields.clear();
        }
        buffer.compact();
        Optional<List<byte[]>> result = fields.isEmpty()? Optional.empty(): Optional.of(fields);
        LOGGER.debug(new cutoutParam(buffer, result));
        return result;
    }

    // Dummy
    static byte @NotNull [] fieldsToLine(@NotNull List<byte @NotNull []> fields) {
        var len = fields.stream().mapToInt(e -> e.length).sum();
        var buff = ByteBuffer.allocate(len);
        for (var field : fields) {
            buff.put(field);
        }
        return buff.array();
    }

    static void writeBlock(
            @NotNull FileChannel writeChannel,
            @NotNull ByteBuffer buff,
            byte @NotNull [] line
    ) throws IOException {
            buff.putInt(line.length);
            buff.put(line);

            while (buff.position() >= 1012) {
                var outBytes = new byte[1014];
                outBytes[1012] = (byte)0x40;
                outBytes[1013] = (byte)0x40;
                buff.flip();
                buff.get(outBytes, 0, 1012);
                buff.compact();

                var written = writeChannel.write(ByteBuffer.wrap(outBytes));
                if (written != outBytes.length) {
                    throw new IllegalArgumentException("Written bytes did not match expected");
                }
            }

        if (line.length == 0 && buff.position() > 0) {
            var padding = HexFormat.of().parseHex("40".repeat(1014 - buff.position()));
            buff.put(padding);
            buff.flip();
            var written = writeChannel.write(buff);
            if (written != buff.position()) {
                throw new IllegalArgumentException("Written bytes did not match expected");
            }
            buff.clear();
        }
    }

    static void readTsv(@NotNull FileChannel writeChannel, Path @NotNull ... readPaths) throws IOException {
        var writeBuff = ByteBuffer.allocate(4096);
        for (var path : readPaths) {

            try (var readChannel = Files.newByteChannel(path, StandardOpenOption.READ)) {
                var readBuff = ByteBuffer.allocate(4096);

                // ファイル読み込み
                while (readChannel.read(readBuff) != -1) {

                    // TSV 切り出し
                    Optional<List<byte[]>> fieldsOptional;
                    while((fieldsOptional = cutoutFields(readBuff)).isPresent()) {
                        var fields = fieldsOptional.get();

                        // 1行に変換
                        var line = fieldsToLine(fields);

                        // ブロック化可能ならファイル出力
                        writeBlock(writeChannel, writeBuff, line);
                    }
                }

                // readBuff が空でなければエラー（recordDelimiter で終端していない）
                throwAtNotEmpty(readBuff, path.toString());
            }
        }

        // 最終ブロック出力
        writeBlock(writeChannel, writeBuff, new byte[0]);

        // writeBuff が空でなければエラー
        throwAtNotEmpty(writeBuff, "out");
    }

    static void throwAtNotEmpty(@NotNull ByteBuffer buff, @NotNull String at) {
        if (buff.position() > 0) {
            var bytes = new byte[buff.position()];
            buff.flip();
            buff.get(bytes);
            var bytesHex = HexFormat.ofDelimiter(" ").formatHex(bytes);
            throw new IllegalArgumentException(String.format("invalid data found. %s: [%s]", at, bytesHex));
        }
    }

    static @NotNull Map<@NotNull String, @NotNull String> toMap(@NotNull String icc) {
        final var tagSet = Set.of("9F26", "9A").stream()
                .map(e -> e.getBytes(StandardCharsets.UTF_8))
                .collect(Collectors.toUnmodifiableSet());
        final var result = new HashMap<String, String>();

        var iccBuff = ByteBuffer.wrap(icc.getBytes(StandardCharsets.UTF_8));
        while (iccBuff.hasRemaining()) {
            var tagBytes = cutTag(iccBuff);

            var lenBytes = new byte[2];
            iccBuff.get(lenBytes);
            var len = Integer.parseInt(new String(lenBytes, StandardCharsets.UTF_8));

            var dataBytes = new byte[len * 2];
            iccBuff.get(dataBytes);

            if (tagSet.stream().anyMatch(e -> Arrays.equals(tagBytes, e))) {
                result.put(new String(tagBytes, StandardCharsets.UTF_8), new String(dataBytes, StandardCharsets.UTF_8));
            }
        }
        return result;
    }

    static byte @NotNull [] cutTag(@NotNull ByteBuffer buff) {
        buff.mark();
        var tag2 = new byte[2];
        buff.get(tag2);
        if ((tag2[1] & (byte)0x46) != (byte) 0x46) {
            return tag2;
        }
        buff.reset();
        var tag4 = new byte[4];
        buff.get(tag4);
        return tag4;
    }

}
