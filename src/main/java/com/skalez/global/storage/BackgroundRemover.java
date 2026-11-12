package com.skalez.global.storage;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * 상품 사진의 단색(또는 은은한 그라데이션) 배경을 지워 투명 PNG 로 만든다. 히어로 배너가 '누끼' 사진을 쓰기 때문.
 * 가장자리에서 출발해 배경과 비슷하고 바로 옆 칸과도 비슷한 픽셀만 따라 들어가며 지운다 (상품 안쪽으로 번지지 않게).
 * 사진은 아임웹 CDN 이나 우리 저장소 주소만 받는다 (아무 주소나 받으면 서버가 내부망을 대신 불러 줄 수 있어서)
 */
@Component
@RequiredArgsConstructor
public class BackgroundRemover {

    private static final long MAX_BYTES = 15L * 1024 * 1024;
    private static final long MAX_PIXELS = 25_000_000L;
    /** 가장자리 배경색과 이만큼 넘게 다르면 배경이 아니다 (RGB 거리) */
    private static final int GLOBAL = 45;
    /** 바로 옆 칸과 이만큼 넘게 다르면 경계로 보고 멈춘다 */
    private static final int LOCAL = 9;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private final StorageProperties properties;

    /** 배경을 지운 PNG. 이미 배경이 투명하면 null */
    public byte[] removeFrom(String imageUrl) {
        BufferedImage source = read(fetch(imageUrl));
        BufferedImage result = remove(source);
        if (result == null) return null;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(result, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("배경을 지운 사진을 만들지 못했습니다", e);
        }
    }

    /** 배경 지우기 본체. 가장자리의 60% 넘게 이미 투명하면 null */
    static BufferedImage remove(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int[] pixels = source.getRGB(0, 0, width, height, null, 0, width);

        // 가장자리 픽셀로 배경색(가운데값)을 정한다
        int[] border = borderIndexes(width, height);
        int transparent = 0;
        int[] reds = new int[border.length], greens = new int[border.length], blues = new int[border.length];
        int opaque = 0;
        for (int index : border) {
            int argb = pixels[index];
            if ((argb >>> 24) < 16) {
                transparent++;
                continue;
            }
            reds[opaque] = (argb >> 16) & 0xFF;
            greens[opaque] = (argb >> 8) & 0xFF;
            blues[opaque] = argb & 0xFF;
            opaque++;
        }
        if (transparent * 10 >= border.length * 6 || opaque == 0) return null;
        int background = (median(reds, opaque) << 16) | (median(greens, opaque) << 8) | median(blues, opaque);

        // 가장자리에서 출발해 배경으로 이어진 칸을 찾는다
        boolean[] cleared = new boolean[pixels.length];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int index : border) {
            if (!cleared[index] && isBackground(pixels[index], background)) {
                cleared[index] = true;
                queue.add(index);
            }
        }
        while (!queue.isEmpty()) {
            int index = queue.poll();
            int x = index % width;
            int y = index / width;
            if (x > 0) visit(pixels, cleared, queue, index, index - 1, background);
            if (x < width - 1) visit(pixels, cleared, queue, index, index + 1, background);
            if (y > 0) visit(pixels, cleared, queue, index, index - width, background);
            if (y < height - 1) visit(pixels, cleared, queue, index, index + width, background);
        }

        // 지운 칸은 투명. 경계 두 줄은 배경색에 가까울수록 투명하게 (배경이 섞인 흰 테두리 없애기)
        boolean[] ring = touchingRing(cleared, cleared, width, height);
        boolean[] secondRing = touchingRing(ring, cleared, width, height);
        int[] out = new int[pixels.length];
        for (int index = 0; index < pixels.length; index++) {
            if (cleared[index]) continue;
            int argb = pixels[index];
            double alpha = argb >>> 24;
            // 배경과 상품이 섞인 경계 픽셀일수록 배경색에 가깝다 → 제곱으로 빠르게 투명하게
            double unlike = Math.min(1.0, distance(argb, background) / (GLOBAL * 4.0));
            if (ring[index]) alpha *= unlike * unlike;
            else if (secondRing[index]) alpha *= unlike;
            out[index] = ((int) Math.round(alpha) << 24) | (argb & 0xFFFFFF);
        }
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, width, height, out, 0, width);
        return result;
    }

    /** 지우지 않은 칸 중 from 칸과 맞닿은 칸 (8방향) */
    private static boolean[] touchingRing(boolean[] from, boolean[] cleared, int width, int height) {
        boolean[] ring = new boolean[from.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if (cleared[index] || from[index]) continue;
                search:
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if ((dx != 0 || dy != 0) && nx >= 0 && ny >= 0 && nx < width && ny < height && from[ny * width + nx]) {
                            ring[index] = true;
                            break search;
                        }
                    }
                }
            }
        }
        return ring;
    }

    private static void visit(int[] pixels, boolean[] cleared, ArrayDeque<Integer> queue, int from, int to, int background) {
        if (cleared[to]) return;
        if (isBackground(pixels[to], background) && distance(pixels[to], pixels[from]) < LOCAL) {
            cleared[to] = true;
            queue.add(to);
        }
    }

    private static boolean isBackground(int argb, int background) {
        return (argb >>> 24) < 16 || distance(argb, background) < GLOBAL;
    }

    private static int distance(int a, int b) {
        int dr = ((a >> 16) & 0xFF) - ((b >> 16) & 0xFF);
        int dg = ((a >> 8) & 0xFF) - ((b >> 8) & 0xFF);
        int db = (a & 0xFF) - (b & 0xFF);
        return (int) Math.sqrt(dr * dr + dg * dg + db * db);
    }

    private static int median(int[] values, int count) {
        int[] copy = Arrays.copyOf(values, count);
        Arrays.sort(copy);
        return copy[count / 2];
    }

    private static int[] borderIndexes(int width, int height) {
        int[] indexes = new int[2 * width + 2 * Math.max(0, height - 2)];
        int n = 0;
        for (int x = 0; x < width; x++) {
            indexes[n++] = x;
            indexes[n++] = (height - 1) * width + x;
        }
        for (int y = 1; y < height - 1; y++) {
            indexes[n++] = y * width;
            indexes[n++] = y * width + width - 1;
        }
        return Arrays.copyOf(indexes, n);
    }

    /** 아임웹 CDN·우리 저장소 주소만, 15MB 까지 받는다 */
    private byte[] fetch(String imageUrl) {
        URI uri;
        try {
            uri = URI.create(imageUrl.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.IMAGE_FETCH_FAILED);
        }
        if (!allowed(uri)) throw new BusinessException(ErrorCode.IMAGE_FETCH_FAILED);
        try {
            HttpResponse<InputStream> response = HTTP.send(
                    HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15)).GET().build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream in = response.body()) {
                if (response.statusCode() != 200) throw new BusinessException(ErrorCode.IMAGE_FETCH_FAILED);
                byte[] bytes = in.readNBytes((int) MAX_BYTES + 1);
                if (bytes.length > MAX_BYTES) throw new BusinessException(ErrorCode.IMAGE_FETCH_FAILED);
                return bytes;
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.IMAGE_FETCH_FAILED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.IMAGE_FETCH_FAILED);
        }
    }

    private boolean allowed(URI uri) {
        String host = uri.getHost();
        if (host == null) return false;
        if ("https".equals(uri.getScheme()) && host.equals("cdn.imweb.me")) return true;
        // 우리 저장소(버킷·로컬 폴더) 주소와 scheme·host·port 가 모두 같을 때만
        URI own = URI.create(properties.publicUrl());
        return host.equals(own.getHost()) && uri.getScheme().equals(own.getScheme()) && uri.getPort() == own.getPort()
                && uri.getPath() != null && uri.getPath().startsWith(own.getPath() == null ? "/" : own.getPath());
    }

    private static BufferedImage read(byte[] bytes) {
        String type = ImageStorage.imageExtension(Arrays.copyOf(bytes, Math.min(bytes.length, 12)));
        if (!"png".equals(type) && !"jpg".equals(type)) throw new BusinessException(ErrorCode.IMAGE_FETCH_FAILED);
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null || (long) image.getWidth() * image.getHeight() > MAX_PIXELS) {
                throw new BusinessException(ErrorCode.IMAGE_FETCH_FAILED);
            }
            return image;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.IMAGE_FETCH_FAILED);
        }
    }
}