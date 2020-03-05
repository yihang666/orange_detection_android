package com.example.imagepost;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

class StreamTool {
    public static byte[] readInputStream(InputStream inputStream)throws IOException {
        byte[] buffer = new byte[1024];
        int len = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while ((len = inputStream.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        bos.close();
        return bos.toByteArray();
    }
}
