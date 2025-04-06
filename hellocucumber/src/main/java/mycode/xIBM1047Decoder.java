package mycode;

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
    protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
        cd.reset();
        return cd.decode(in, out, false);
    }
}
