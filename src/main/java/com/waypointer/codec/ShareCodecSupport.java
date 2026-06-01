package com.waypointer.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Shared gzip+base64 transport for the share codecs. Each codec keeps its own
 * {@code MalformedCodeException} (it is part of that codec's public surface), so the decode
 * helpers take a factory that wraps a message in the caller's exception type. Guards against
 * gzip bombs with a fixed 1 MiB inflated-size cap.
 */
final class ShareCodecSupport
{
    static final int MAX_INFLATED_BYTES = 1024 * 1024;

    private ShareCodecSupport() {}

    static String stripMagic(String input, String magic, Function<String, RuntimeException> err)
    {
        String trimmed = input.trim();
        if (!trimmed.startsWith(magic))
        {
            throw err.apply("Expected magic " + magic + " at start of code");
        }
        return trimmed.substring(magic.length());
    }

    static String gzipBase64(String input)
    {
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gz = new GZIPOutputStream(baos))
            {
                gz.write(input.getBytes(StandardCharsets.UTF_8));
            }
            return Base64.getEncoder().withoutPadding().encodeToString(baos.toByteArray());
        }
        catch (IOException e)
        {
            throw new RuntimeException("gzip+base64 encode failed", e);
        }
    }

    static String ungzipBase64(String input, Function<String, RuntimeException> err)
    {
        byte[] decoded;
        try { decoded = Base64.getDecoder().decode(input); }
        catch (IllegalArgumentException e) { throw err.apply("Bad base64"); }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(decoded)))
        {
            byte[] buf = new byte[4096];
            int n;
            while ((n = gz.read(buf)) > 0)
            {
                out.write(buf, 0, n);
                if (out.size() > MAX_INFLATED_BYTES)
                {
                    throw err.apply("Share payload exceeds 1 MiB cap");
                }
            }
        }
        catch (IOException e) { throw err.apply("gzip decode failed: " + e.getMessage()); }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
