package mycode;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public class xIBM1047_1Byte extends Charset {

    public xIBM1047_1Byte(String canonicalName, String[] aliases) {
        super(canonicalName, aliases);
    }

    @Override
    public boolean contains(Charset cs) {
        return cs instanceof xIBM1047_1Byte;
    }

    @Override
    public CharsetDecoder newDecoder() {
        return new xIBM1047Decoder(this, 1, 1);
    }

    @Override
    public CharsetEncoder newEncoder() {
        return new xIBM1047Encoder(this, 1, 1);
    }

}
