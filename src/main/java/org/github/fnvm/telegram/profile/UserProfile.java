package org.github.fnvm.telegram.profile;

import org.github.fnvm.data.QualityPreference;

import java.util.Optional;

public class UserProfile {
    private final Long userId;
    private String cookies;
    private QualityPreference defaultQuality = QualityPreference.SD;

    public UserProfile(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public Optional<String> getCookies() {
        return Optional.ofNullable(cookies);
    }

    public void setCookies(String cookies) {
        this.cookies = cookies != null && !cookies.isBlank() ? cookies.trim() : null;
    }

    public void clearCookies() {
        this.cookies = null;
    }

    public QualityPreference getDefaultQuality() {
        return defaultQuality;
    }

    public void setDefaultQuality(QualityPreference quality) {
        this.defaultQuality = quality;
    }
}