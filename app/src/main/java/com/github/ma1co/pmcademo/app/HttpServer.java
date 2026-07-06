package com.github.ma1co.pmcademo.app;

import android.content.Context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import fi.iki.elonen.NanoHTTPD;

public class HttpServer extends NanoHTTPD {
    public static final int PORT = 8080;

    public interface BurstStatusProvider {
        boolean isBursting();
    }
    private Context context;

    private final BurstStatusProvider burstStatusProvider;

    public HttpServer(BurstStatusProvider burstStatusProvider,Context context) {
        super(PORT);
        this.burstStatusProvider = burstStatusProvider;
        this.context = context;
    }

    public HttpServer() {
        super(PORT);
        this.burstStatusProvider = new BurstStatusProvider() {
            @Override
            public boolean isBursting() {
                return false;
            }
        };
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();

        if ("/a".equals(uri)) {
            InputStream is = context.getResources().openRawResource(R.raw.a);
            Scanner s = new Scanner(is).useDelimiter("\\A");
            String html = s.hasNext() ? s.next() : "";
            Logger.info("Serving /a: " + html);
            try {
                is.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return new NanoHTTPD.Response(Response.Status.OK, "text/html", html);
        }

        if ("/b".equals(uri)) {
            String state = String.valueOf(burstStatusProvider != null && burstStatusProvider.isBursting());
            return new NanoHTTPD.Response(Response.Status.OK, MIME_PLAINTEXT, state);
        }
        if("/shutter.mp3".equals(uri)){
            InputStream is = context.getResources().openRawResource(R.raw.shutter);
            return new NanoHTTPD.Response(Response.Status.OK, "audio/wav", is);
        }

        return new NanoHTTPD.Response(Response.Status.OK, MIME_PLAINTEXT, "Not Found");
    }
    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = is.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
        return out.toByteArray();
    }
}
