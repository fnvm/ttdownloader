package org.github.fnvm.telegram.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UserProfileManager {
    private static final Logger log = LoggerFactory.getLogger(UserProfileManager.class);
    private static final Path PROFILES_FILE = Path.of("user_profiles.json");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final Map<Long, UserProfile> profiles = new HashMap<>();

    public UserProfileManager() {
        loadProfiles();
    }

    public UserProfile getOrCreateProfile(Long userId) {
        return profiles.computeIfAbsent(userId, UserProfile::new);
    }

    public void setCookies(Long userId, String cookies) {
        UserProfile profile = getOrCreateProfile(userId);
        profile.setCookies(cookies);
        saveProfiles();
        log.info("Cookies set for user {}", userId);
    }

    public Optional<String> getCookies(Long userId) {
        return Optional.ofNullable(profiles.get(userId))
                .flatMap(UserProfile::getCookies);
    }

    public boolean hasCookies(Long userId) {
        return getCookies(userId).isPresent();
    }

    public void deleteCookies(Long userId) {
        Optional.ofNullable(profiles.get(userId))
                .ifPresent(profile -> {
                    profile.clearCookies();
                    saveProfiles();
                    log.info("Cookies deleted for user {}", userId);
                });
    }

    private void loadProfiles() {
        if (!Files.exists(PROFILES_FILE)) {
            log.info("Profiles file not found, starting with empty profiles");
            return;
        }

        try {
            ProfilesData data = OBJECT_MAPPER.readValue(
                    PROFILES_FILE.toFile(),
                    ProfilesData.class
            );

            if (data != null && data.getProfiles() != null) {
                profiles.putAll(data.getProfiles());
                log.info("Loaded {} user profiles", profiles.size());
            }
        } catch (IOException e) {
            log.error("Failed to load profiles", e);
        }
    }

    private void saveProfiles() {
        try {
            ProfilesData data = new ProfilesData(profiles);
            OBJECT_MAPPER.writeValue(PROFILES_FILE.toFile(), data);
        } catch (IOException e) {
            log.error("Failed to save profiles", e);
        }
    }

    private static class ProfilesData {
        private Map<Long, UserProfile> profiles;

        public ProfilesData() {
        }

        public ProfilesData(Map<Long, UserProfile> profiles) {
            this.profiles = profiles;
        }

        public Map<Long, UserProfile> getProfiles() {
            return profiles;
        }

        public void setProfiles(Map<Long, UserProfile> profiles) {
            this.profiles = profiles;
        }
    }
}
