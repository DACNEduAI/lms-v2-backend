package edu.ai.lms.core.quiz3.app.service;

import edu.ai.lms.core.quiz3.app.domain.*;
import edu.ai.lms.core.quiz3.app.service.*;
import edu.ai.lms.core.quiz3.adapter.out.persistence.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import edu.ai.lms.core.quiz3.app.domain.richcontent.RichContentFormat;
import edu.ai.lms.core.quiz3.app.domain.richcontent.RichTextValue;
import edu.ai.lms.core.quiz3.app.exception.RichContentValidationException;
import edu.ai.lms.core.quiz3.app.port.out.RichContentAssetStoragePort;
import lombok.RequiredArgsConstructor;

/** Quiz3-only storage namespace and metadata preparation for embedded images. */
@Service
@RequiredArgsConstructor
public class Quiz3RichContentUploadService {
    private static final String PREFIX = "quiz3/rich-content/";
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final Pattern PENDING_SOURCE = Pattern.compile("pending-image://([0-9a-fA-F-]{8,})");
    private static final Pattern IMAGE_SOURCE = Pattern.compile("<img\\b[^>]*\\bsrc\\s*=\\s*(['\\\"])(.*?)\\1", Pattern.CASE_INSENSITIVE);
    private static final Set<String> EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp", "gif", "svg");

    private final RichContentAssetStoragePort storage;

    public PreparedRichContent prepare(List<RichTextValue> values, List<MultipartFile> files,
            Collection<Quiz3RichContentAsset> existingAssets) {
        List<RichTextValue> safeValues = values == null ? List.of() : values;
        Set<String> pendingIds = pendingIds(safeValues);
        Map<String, MultipartFile> byPendingId = filesByPendingId(files == null ? List.of() : files);
        if (!byPendingId.keySet().equals(pendingIds)) {
            throw new RichContentValidationException("Every uploaded image must be referenced exactly once in rich content");
        }
        Set<String> knownUrls = new LinkedHashSet<>();
        if (existingAssets != null) existingAssets.forEach(asset -> knownUrls.add(storage.publicUrl(asset.storageKey())));
        Map<String, UploadedAsset> uploaded = new HashMap<>();
        try {
            for (String pendingId : pendingIds) {
                UploadedAsset asset = upload(byPendingId.get(pendingId));
                uploaded.put(pendingId, asset);
                knownUrls.add(asset.publicUrl());
            }
            return new PreparedRichContent(safeValues.stream().map(value -> rewrite(value, uploaded, knownUrls)).toList(),
                    List.copyOf(uploaded.values()));
        } catch (RuntimeException ex) {
            cleanup(uploaded.values());
            throw ex;
        }
    }

    public UploadedAsset uploadAiSvg(String source) {
        byte[] bytes = source == null ? new byte[0] : source.getBytes(StandardCharsets.UTF_8);
        if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES || detect(bytes) != ImageType.SVG) {
            throw new RichContentValidationException("AI SVG is empty, too large, or invalid");
        }
        return store(bytes, ImageType.SVG);
    }

    /** Copies authorized course-asset bytes into question-owned public storage. */
    public UploadedAsset snapshotCourseAsset(byte[] bytes, String mediaType) {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) throw new RichContentValidationException("Course image snapshot is empty or too large");
        ImageType type = detect(bytes);
        if (!type.mediaType().equalsIgnoreCase(mediaType)) throw new RichContentValidationException("Course image MIME type does not match its signature");
        return store(bytes, type);
    }

    public void cleanup(Collection<UploadedAsset> assets) {
        if (assets != null) assets.forEach(asset -> {
            try { storage.delete(asset.storageKey()); } catch (RuntimeException ignored) { }
        });
    }

    public void cleanupAfterRollback(Collection<UploadedAsset> assets) {
        if (assets == null || assets.isEmpty() || !TransactionSynchronizationManager.isSynchronizationActive()) return;
        List<UploadedAsset> snapshot = List.copyOf(assets);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) { if (status != STATUS_COMMITTED) cleanup(snapshot); }
        });
    }

    public List<Quiz3RichContentAsset> assetsForQuestion(UUID questionId, Collection<UploadedAsset> assets) {
        return assets == null ? List.of() : assets.stream().map(asset -> new Quiz3RichContentAsset(UUID.randomUUID(),
                questionId, asset.storageKey(), asset.mediaType(), asset.byteSize(), asset.sha256(), null, null, null, null)).toList();
    }

    public Set<String> referencedStorageKeys(List<RichTextValue> values,
            Collection<Quiz3RichContentAsset> existingAssets, Collection<UploadedAsset> uploadedAssets) {
        Map<String, String> keysByUrl = new HashMap<>();
        if (existingAssets != null) existingAssets.forEach(asset -> keysByUrl.put(storage.publicUrl(asset.storageKey()), asset.storageKey()));
        if (uploadedAssets != null) uploadedAssets.forEach(asset -> keysByUrl.put(asset.publicUrl(), asset.storageKey()));
        Set<String> referenced = new LinkedHashSet<>();
        for (RichTextValue value : values == null ? List.<RichTextValue>of() : values) {
            if (value.contentFormat() != RichContentFormat.TIPTAP_HTML) continue;
            Matcher image = IMAGE_SOURCE.matcher(value.content());
            while (image.find()) {
                String key = keysByUrl.get(image.group(2));
                if (key == null) throw new RichContentValidationException("External or unowned image URLs are not permitted");
                referenced.add(key);
            }
        }
        return referenced;
    }

    private Set<String> pendingIds(List<RichTextValue> values) {
        Set<String> result = new LinkedHashSet<>();
        values.stream().filter(value -> value.contentFormat() == RichContentFormat.TIPTAP_HTML).forEach(value -> {
            Matcher matcher = PENDING_SOURCE.matcher(value.content());
            while (matcher.find()) result.add(matcher.group(1));
        });
        return result;
    }

    private Map<String, MultipartFile> filesByPendingId(List<MultipartFile> files) {
        Map<String, MultipartFile> result = new HashMap<>();
        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename();
            if (filename == null || filename.isBlank()) throw new RichContentValidationException("An uploaded image is missing its filename");
            int dot = filename.lastIndexOf('.');
            String extension = dot < 1 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
            String id = dot < 1 ? filename : filename.substring(0, dot);
            if (!EXTENSIONS.contains(extension) || !id.matches("[0-9a-fA-F-]{8,}")) {
                throw new RichContentValidationException("Image uploads must use the editor-generated UUID filename");
            }
            if (result.putIfAbsent(id, file) != null) throw new RichContentValidationException("Duplicate image upload reference");
        }
        return result;
    }

    private RichTextValue rewrite(RichTextValue value, Map<String, UploadedAsset> uploaded, Set<String> knownUrls) {
        if (value.contentFormat() == RichContentFormat.PLAIN_TEXT) return value;
        Matcher pending = PENDING_SOURCE.matcher(value.content());
        StringBuffer rendered = new StringBuffer();
        while (pending.find()) {
            UploadedAsset asset = uploaded.get(pending.group(1));
            if (asset == null) throw new RichContentValidationException("Rich content refers to an unresolved image");
            pending.appendReplacement(rendered, Matcher.quoteReplacement(asset.publicUrl()));
        }
        pending.appendTail(rendered);
        Matcher image = IMAGE_SOURCE.matcher(rendered.toString());
        while (image.find()) if (!knownUrls.contains(image.group(2))) {
            throw new RichContentValidationException("External or unowned image URLs are not permitted");
        }
        return new RichTextValue(rendered.toString(), RichContentFormat.TIPTAP_HTML);
    }

    private UploadedAsset upload(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_IMAGE_BYTES) {
            throw new RichContentValidationException("Image must be between 1 byte and 10 MB");
        }
        try {
            byte[] bytes = file.getBytes();
            ImageType type = detect(bytes);
            String declared = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
            if (!declared.isBlank() && !declared.equals(type.mediaType())) throw new RichContentValidationException("Image MIME type does not match its file signature");
            String filename = file.getOriginalFilename();
            String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            if (!type.acceptsExtension(extension)) throw new RichContentValidationException("Image extension does not match its file signature");
            return store(bytes, type);
        } catch (IOException ex) {
            throw new RichContentValidationException("Could not read uploaded image");
        }
    }

    private UploadedAsset store(byte[] bytes, ImageType type) {
        String key = PREFIX + UUID.randomUUID() + "." + type.extension();
        storage.upload(key, bytes, type.mediaType());
        return new UploadedAsset(key, storage.publicUrl(key), type.mediaType(), bytes.length, sha256(bytes));
    }

    private ImageType detect(byte[] bytes) {
        if (startsWith(bytes, new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a})) return ImageType.PNG;
        if (startsWith(bytes, new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff})) return ImageType.JPEG;
        if (startsWith(bytes, "GIF87a".getBytes(StandardCharsets.US_ASCII)) || startsWith(bytes, "GIF89a".getBytes(StandardCharsets.US_ASCII))) return ImageType.GIF;
        if (startsWith(bytes, "RIFF".getBytes(StandardCharsets.US_ASCII)) && bytes.length > 12 && Arrays.equals(Arrays.copyOfRange(bytes, 8, 12), "WEBP".getBytes(StandardCharsets.US_ASCII))) return ImageType.WEBP;
        String svg = new String(bytes, StandardCharsets.UTF_8).trim();
        if (svg.startsWith("<svg") || svg.startsWith("<?xml") && svg.toLowerCase(Locale.ROOT).contains("<svg")) {
            String lower = svg.toLowerCase(Locale.ROOT);
            if (lower.contains("<!doctype") || lower.contains("<!entity") || lower.contains("<script") || lower.contains("<foreignobject") || lower.matches("(?s).*\\son[a-z]+\\s*=.*") || lower.contains("javascript:") || lower.matches("(?s).*\\s(?:href|xlink:href)\\s*=.*")) {
                throw new RichContentValidationException("SVG contains unsafe markup");
            }
            return ImageType.SVG;
        }
        throw new RichContentValidationException("Unsupported image type");
    }

    private static boolean startsWith(byte[] source, byte[] prefix) {
        if (source.length < prefix.length) return false;
        for (int index = 0; index < prefix.length; index++) if (source[index] != prefix[index]) return false;
        return true;
    }
    private static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 is unavailable", ex); }
    }

    public record PreparedRichContent(List<RichTextValue> values, List<UploadedAsset> uploadedAssets) { }
    public record UploadedAsset(String storageKey, String publicUrl, String mediaType, long byteSize, String sha256) { }
    private enum ImageType {
        PNG("image/png", "png", Set.of("png")), JPEG("image/jpeg", "jpg", Set.of("jpg", "jpeg")),
        WEBP("image/webp", "webp", Set.of("webp")), GIF("image/gif", "gif", Set.of("gif")), SVG("image/svg+xml", "svg", Set.of("svg"));
        private final String mediaType; private final String extension; private final Set<String> extensions;
        ImageType(String mediaType, String extension, Set<String> extensions) { this.mediaType = mediaType; this.extension = extension; this.extensions = extensions; }
        String mediaType() { return mediaType; } String extension() { return extension; } boolean acceptsExtension(String value) { return extensions.contains(value); }
    }
}
