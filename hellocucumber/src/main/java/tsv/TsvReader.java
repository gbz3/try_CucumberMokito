package tsv;

import org.jetbrains.annotations.NotNull;
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

public class TsvReader {

    static byte fieldDelimiter = (byte)0x05;
    static byte recordDelimiter = (byte)0x25;

    static @NotNull Optional<List<byte[]>> cutoutFields(@NotNull ByteBuffer buffer) {
        //System.out.printf("#### >>> buff=[%s]%n", HexFormat.ofDelimiter(" ").formatHex(buffer.array()));
        var result = new ArrayList<byte[]>();
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
                result.add(bytes);
                buffer.position(buffer.position() + 1);
                if (b == recordDelimiter) break;
                buffer.mark();
            }
        }
        buffer.compact();
        return result.isEmpty()? Optional.empty(): Optional.of(result);
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

}
