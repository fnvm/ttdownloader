package org.github.fnvm.telegram.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.github.fnvm.data.QualityPreference;

import java.util.Optional;

public class UserProfile {
    private Long userId;
    private String cookies;
    private QualityPreference defaultQuality;

    public UserProfile() {
        this.defaultQuality = QualityPreference.SD;
    }

    public UserProfile(Long userId) {
        this.userId = userId;
        this.defaultQuality = QualityPreference.SD;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCookies() {
        return cookies;
    }

    @JsonIgnore
    public Optional<String> getCookiesOptional() {
        return Optional.ofNullable(cookies);
    }

    public void setCookies(String cookies) {
        this.cookies = (cookies != null && !cookies.isBlank()) ? cookies.trim() : null;
    }

    public void clearCookies() {
        this.cookies = null;
    }

    public QualityPreference getDefaultQuality() {
        return defaultQuality != null ? defaultQuality : QualityPreference.SD;
    }

    public void setDefaultQuality(QualityPreference quality) {
        this.defaultQuality = quality;
    }
}
