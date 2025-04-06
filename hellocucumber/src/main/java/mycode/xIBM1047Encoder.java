package mycode;

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
    protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {
        ce.reset();
        return ce.encode(in, out, false);
    }
}
