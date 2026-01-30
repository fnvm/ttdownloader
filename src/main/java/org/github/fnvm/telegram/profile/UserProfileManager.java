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

    public synchronized UserProfile getOrCreateProfile(Long userId) {
        return profiles.computeIfAbsent(userId, UserProfile::new);
    }

    public synchronized void setCookies(Long userId, String cookies) {
        UserProfile profile = getOrCreateProfile(userId);
        profile.setCookies(cookies);
        saveProfiles();
        log.info("Cookies set for user {}", userId);
    }

    public synchronized Optional<String> getCookies(Long userId) {
        return Optional.ofNullable(profiles.get(userId))
                .flatMap(UserProfile::getCookiesOptional);
    }

    public synchronized boolean hasCookies(Long userId) {
        return getCookies(userId).isPresent();
    }

    public synchronized void deleteCookies(Long userId) {
        Optional.ofNullable(profiles.get(userId))
                .ifPresent(profile -> {
                    profile.clearCookies();
                    saveProfiles();
                    log.info("Cookies deleted for user {}", userId);
                });
    }

    private synchronized void loadProfiles() {
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

                profiles.forEach((userId, profile) ->
                        log.debug("Loaded profile for user {}: has cookies = {}",
                                userId, profile.getCookiesOptional().isPresent())
                );
            }
        } catch (IOException e) {
            log.error("Failed to load profiles: {}", e.getMessage(), e);
            backupCorruptedFile();
        }
    }

    private synchronized void saveProfiles() {
        try {
            ProfilesData data = new ProfilesData(new HashMap<>(profiles));

            Path tempFile = Path.of(PROFILES_FILE + ".tmp");
            OBJECT_MAPPER.writeValue(tempFile.toFile(), data);

            Files.move(
                    tempFile,
                    PROFILES_FILE,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE
            );

            log.debug("Saved {} user profiles", profiles.size());
        } catch (IOException e) {
            log.error("Failed to save profiles: {}", e.getMessage(), e);
        }
    }

    private void backupCorruptedFile() {
        try {
            if (Files.exists(PROFILES_FILE)) {
                Path backup = Path.of("user_profiles.json.backup." + System.currentTimeMillis());
                Files.copy(PROFILES_FILE, backup);
                log.info("Created backup of corrupted file: {}", backup);
            }
        } catch (IOException e) {
            log.error("Failed to backup corrupted file: {}", e.getMessage());
        }
    }

    public static class ProfilesData {
        private Map<Long, UserProfile> profiles;

        public ProfilesData() {
            this.profiles = new HashMap<>();
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
