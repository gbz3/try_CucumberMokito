package mycode;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

public class xIBM1047Encoder extends CharsetEncoder {
    private final CharsetEncoder ce = Charset.forName("IBM1047").newEncoder();

    protected xIBM1047Encoder(Charset cs, float averageCharsPerByte, float maxCharsPerByte) {
        super(cs, averageCharsPerByte, maxCharsPerByte);
    }

    @Override
    protected CoderResult encodeLoop(@NotNull CharBuffer in, ByteBuffer out) {
        while (in.hasRemaining()) {
            // 出力バッファに空きが無ければオーバーフロー
            if (out.remaining() == 0) return CoderResult.OVERFLOW;
            var ch = in.get();
            switch (ch) {
                case ' ' -> out.put((byte) 0x40);   // TODO
                case '!' -> out.put((byte) 0x5A);   // TODO
                default -> {
                    ce.reset();
                    var chBuff = CharBuffer.wrap(new char[] {ch});
                    ce.encode(chBuff, out, false);
                }
            }
        }
        // 入力バッファを消費した
        return CoderResult.UNDERFLOW;
    }

}
