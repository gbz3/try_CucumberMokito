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
                if (b == recordDelimiter) break;
                buffer.position(buffer.position() + 1);
                buffer.mark();
            }
        }
        buffer.compact();
        return result.isEmpty()? Optional.empty(): Optional.of(result);
    }

    static byte[] fieldsToLine(@NotNull List<byte @NotNull []> fields) {
        return new byte[0];
    }

    static void writeBlock(
            @NotNull FileChannel writeChannel,
            @NotNull ByteBuffer buff,
            byte[] line
    ) {

    }

    static void readTsv(@NotNull FileChannel writeChannel, Path @NotNull ... readPaths) throws IOException {
        for (var path : readPaths) {
            var writeBuff = ByteBuffer.allocate(4096);

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

            // writeBuff が空でなければ最終ブロック出力
            if (writeBuff.position() > 0) {
                writeBlock(writeChannel, writeBuff, new byte[0]);
            }

            // writeBuff が空でなければエラー
            throwAtNotEmpty(writeBuff, "out");
        }
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
