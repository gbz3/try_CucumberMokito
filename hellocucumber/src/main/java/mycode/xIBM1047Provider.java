package mycode;

import java.nio.charset.Charset;
import java.nio.charset.spi.CharsetProvider;
import java.util.Collections;
import java.util.Iterator;

public class xIBM1047Provider extends CharsetProvider {
    static final Charset charset = new xIBM1047_1Byte("xIBM1047", new String[] { "xIBM1047_1Byte" });

    @Override
    public Iterator<Charset> charsets() {
        return Collections.singletonList(charset).iterator();
    }

    @Override
    public Charset charsetForName(String charsetName) {
        if (charset.name().equals(charsetName)) {
            return charset;
        }
        return null;
    }
}
