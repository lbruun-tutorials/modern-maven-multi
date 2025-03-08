package org.example.core;

import java.time.Instant;
import java.util.Optional;

/** Static information about this library */
public class LibraryInfo {

    // Values '${...}' will be replaced by Maven at build time
    private final String version = "${project.version}";
    private final String buildTimeStr = "${git.build.time}"; // must be in RFC-3339 format
    private final String commitTimeStr  = "${git.commit.time}"; // must be in RFC-3339 format
    private final String commitId = "${git.commit.id}";
    private final String projectUrl  = "${project.url}";
    private final String licenseName  = "${project.licenses[0].name}";
    private final String licenseUrl  = "${project.licenses[0].url}";
    private static final LibraryInfo INSTANCE = new LibraryInfo();

    private LibraryInfo() {
    }

    public static LibraryInfo get() {
        return INSTANCE;
    }

    /**
     * Version of library.
     *
     * @return version as a string or empty string if unknown, never {@code null}.
     */
    public String version() {
        return fixUnresolved(version);
    }

    /**
     * Time the library was build/compiled.
     *
     * @return time of build. Empty if no build time information is available.
     */
    public Optional<Instant> buildTime() {
        String s = fixUnresolved(buildTimeStr);
        if (s != null && (!s.isEmpty())) {
            return Optional.of(Instant.parse(s));
        }
        return Optional.empty();
    }

    /**
     * Time of the commit to version control system (VCS) for the commit from which this library was build.
     *
     * @return time of commit or empty if unknown
     */
    public Optional<Instant> vcsCommitTime() {
        String s = fixUnresolved(commitTimeStr);
        if (s != null && (!s.isEmpty())) {
            return Optional.of(Instant.parse(s));
        }
        return Optional.empty();
    }

    /**
     * Commit identifier in the version control system (VCS) for the commit from which this library was
     * build. A commit identifier is typically an SHA-1 value.
     *
     * <p>Also sometimes referred to as a <i>commit reference</i>.
     *
     * @return commit id or empty string if unknown, never {@code null}.
     */
    public String vcsCommitId() {
        return fixUnresolved(commitId);
    }

    /**
     * URL which is the home page for the project which produced this library.
     *
     * @return URL string for the project's home page or empty string if unknown, never {@code null}.
     */
    public String projectUrl() {
        return fixUnresolved(projectUrl);
    }

    /**
     * Name of license which applies to this library. For example: {@code 'The Apache License, Version 2.0'}.
     * @return license name or empty string if unknown or no license, never {@code null}.
     * @see #licenseUrl()
     */
    public String licenseName() {
        return fixUnresolved(licenseName);
    }

    /**
     * URL from where the full license text can be retrieved for the license which applies to this library. For example: {@code 'https://www.apache.org/licenses/LICENSE-2.0.txt'}.
     * @return license URL or empty string if unknown or no license, never {@code null}.
     * @see #licenseName()
     */
    public String licenseUrl() {
        return fixUnresolved(licenseUrl);
    }


    /**
     * Replaces unresolved placeholders with an empty string.
     */
    private static String fixUnresolved(String str) {
        if (str == null) {
            return null;
        }
        int pos = str.indexOf("${");
        if (pos == -1) {
            return str;
        }
        int pos2 = str.indexOf("}", pos + 3);
        if (pos2 == -1) {
            return str;
        }
        return str.substring(0, pos) + str.substring(pos2 + 1);
    }
}
