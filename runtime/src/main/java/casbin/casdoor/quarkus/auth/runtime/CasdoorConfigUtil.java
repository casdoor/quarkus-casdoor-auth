// Copyright 2025 The Casdoor Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package casbin.casdoor.quarkus.auth.runtime;

import io.quarkus.runtime.configuration.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public final class CasdoorConfigUtil {

    private CasdoorConfigUtil() {
    }

    /**
     * Validates the configuration.
     *
     * @param config the configuration to validate
     * @throws ConfigurationException if the configuration is invalid
     */
    public static void validateConfig(CasdoorConfig config) {
        if (!config.enabled()) {
            throw new ConfigurationException("Casdoor configuration is disabled");
        }

        Optional<String> endpoint = config.endpoint();
        if (endpoint.isPresent()) {
            String endpointUrl = endpoint.get();
            if (!endpointUrl.startsWith("http://") && !endpointUrl.startsWith("https://")) {
                throw new ConfigurationException(
                        "Casdoor endpoint must start with 'http://' or 'https://': " + endpointUrl);
            }
            if (endpointUrl.endsWith("/")) {
                throw new ConfigurationException(
                        "Casdoor endpoint should not end with '/': " + endpointUrl);
            }
        }
    }

    /**
     * Resolves the certificate configuration.
     * If the certificate value is a file path, reads the file content.
     * Supports both classpath resources and file system paths.
     * Otherwise, treats it as the certificate content directly.
     *
     * @param certificateConfig the certificate configuration value
     * @return the certificate content
     * @throws ConfigurationException if the certificate cannot be resolved
     */
    public static String resolveCertificate(String certificateConfig) {
        if (certificateConfig == null || certificateConfig.trim().isEmpty()) {
            throw new ConfigurationException("Certificate configuration cannot be empty");
        }

        if (isFilePath(certificateConfig)) {
            String[] resourcePaths = {
                certificateConfig.startsWith("/") ? certificateConfig.substring(1) : certificateConfig,
                certificateConfig.startsWith("./") ? certificateConfig.substring(2) : certificateConfig,
                certificateConfig
            };
            
            for (String resourcePath : resourcePaths) {
                try (InputStream is = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream(resourcePath)) {
                    if (is != null) {
                        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    }
                } catch (IOException e) {
                }
            }
            
            try {
                Path certificatePath = Paths.get(certificateConfig);
                if (Files.exists(certificatePath)) {
                    return Files.readString(certificatePath);
                }
                
                if (certificateConfig.startsWith("./")) {
                    Path alternativePath = Paths.get(certificateConfig.substring(2));
                    if (Files.exists(alternativePath)) {
                        return Files.readString(alternativePath);
                    }
                }
                
                throw new ConfigurationException(
                        "Certificate file does not exist in classpath or file system: " + certificateConfig);
            } catch (IOException e) {
                throw new ConfigurationException(
                        "Failed to read certificate file: " + certificateConfig, e);
            }
        } else {
            return certificateConfig;
        }
    }

    /**
     * Determines if the given string looks like a file path.
     *
     * @param value the string to check
     * @return true if it looks like a file path
     */
    private static boolean isFilePath(String value) {
        return value.contains("/") || value.contains("\\") || 
               value.endsWith(".pem") || value.endsWith(".crt") || 
               value.endsWith(".cer") || value.endsWith(".cert") ||
               value.startsWith("./") || value.startsWith("../");
    }
}
