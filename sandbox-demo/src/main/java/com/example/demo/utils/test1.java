package com.example.demo.utils;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class test1 {
    public static void main(String[] args) throws UnsupportedEncodingException {
        System.out.println("----------");
        final Base64.Decoder decoder = Base64.getDecoder();
        final Base64.Encoder encoder = Base64.getEncoder();
        final String text = "test010";
        final byte[] textByte = text.getBytes("UTF-8");
//编码
        final String encodedText = encoder.encodeToString(textByte);
        System.out.println(encodedText);
//解码
        System.out.println(new String(decoder.decode(encodedText), "UTF-8"));

    }
}
