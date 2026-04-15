package io.carizmi.testbase;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Typed Java helper for capturing servlet output in tests.
 */

@Getter
public final class ServletCaptureHelper {

    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private final CopyOnWriteArrayList<byte[]> recordedWrites = new CopyOnWriteArrayList<>();

    private final ServletOutputStream servletOutputStream = new ServletOutputStream() {

        @Override
        public void write(int b) {
            byteArrayOutputStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            byte[] copy = new byte[len];
            System.arraycopy(b, off, copy, 0, len);
            recordedWrites.add(copy);
            byteArrayOutputStream.write(b, off, len);
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            // no-op for tests
        }
    };

}