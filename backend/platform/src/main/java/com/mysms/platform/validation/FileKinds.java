package com.mysms.platform.validation;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The fixed content-type catalogue of platform.file_object.file_kind: which file extensions MySMS accepts by default
 * and the kind each one is (for icons, viewers and processing). Executable and script formats (and SVG, which can
 * carry script) are not in it, so they are refused unless a tenant's upload policy names them explicitly. Images,
 * audio and video must also declare a content type of their own family.
 */
public final class FileKinds {

    private static final Map<String, String> KIND_BY_EXTENSION = kinds(Map.of(
            "pdf", "pdf",
            "document", "doc docx odt rtf txt md",
            "presentation", "ppt pptx odp key",
            "spreadsheet", "xls xlsx ods csv",
            "image", "jpg jpeg png gif webp bmp heic",
            "audio", "mp3 wav m4a ogg aac",
            "video", "mp4 mov webm mkv avi",
            "archive", "zip 7z tar gz rar",
            "package", "h5p imscc",
            "ebook", "epub mobi"));
    private static final Set<String> TYPED_FAMILIES = Set.of("image", "audio", "video");

    private FileKinds() {
    }

    /** The extension of a file name, lower case, without the dot; empty when it has none. */
    public static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 || dot == fileName.length() - 1 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** Whether MySMS accepts the extension when the tenant's policy names none. */
    public static boolean acceptedByDefault(String extension) {
        return KIND_BY_EXTENSION.containsKey(extension);
    }

    /** The file kind of an extension ({@code other} for an extension outside the catalogue a policy allowed). */
    public static String kindOf(String extension) {
        return KIND_BY_EXTENSION.getOrDefault(extension, "other");
    }

    /** Whether the declared content type fits the kind: images, audio and video must say so; the rest may not lie. */
    public static boolean contentTypeFits(String kind, String contentType) {
        String family = contentType.substring(0, contentType.indexOf('/')).toLowerCase(Locale.ROOT);
        if (TYPED_FAMILIES.contains(kind)) {
            return family.equals(kind);
        }
        return !TYPED_FAMILIES.contains(family) || "other".equals(kind);
    }

    /** The extensions a policy allows ("pdf, .DOCX" → {pdf, docx}); empty for no list. */
    public static Optional<Set<String>> allowed(String policyList) {
        if (policyList == null || policyList.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(Arrays.stream(policyList.split(","))
                .map(e -> e.trim().replaceFirst("^\\.", "").toLowerCase(Locale.ROOT))
                .filter(e -> !e.isEmpty()).collect(Collectors.toUnmodifiableSet()));
    }

    private static Map<String, String> kinds(Map<String, String> extensionsByKind) {
        return extensionsByKind.entrySet().stream()
                .flatMap(entry -> Arrays.stream(entry.getValue().split(" ")).map(ext -> Map.entry(ext, entry.getKey())))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
