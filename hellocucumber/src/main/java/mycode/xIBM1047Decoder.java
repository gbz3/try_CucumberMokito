package mycode;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

public class xIBM1047Decoder extends CharsetDecoder {
    private final CharsetDecoder cd = Charset.forName("IBM1047").newDecoder();

    public xIBM1047Decoder(Charset cs, float averageCharsPerByte, float maxCharsPerByte) {
        super(cs, averageCharsPerByte, maxCharsPerByte);
    }

    @Override
    protected CoderResult decodeLoop(@NotNull ByteBuffer in, CharBuffer out) {
        while (in.hasRemaining()) {
            // 出力バッファに空きが無ければオーバーフロー
            if (out.remaining() == 0) return CoderResult.OVERFLOW;
            var b = in.get();
            switch (b) {
                case 0x40 -> out.put(' ');  // TODO
                case 0x5A -> out.put('!');  // TODO
                default -> {
                    cd.reset();
                    var bb = ByteBuffer.wrap(new byte[] {b});
                    cd.decode(bb, out, false);
                }
            }
        }
        // 入力バッファを消費した
        return CoderResult.UNDERFLOW;
    }
}
