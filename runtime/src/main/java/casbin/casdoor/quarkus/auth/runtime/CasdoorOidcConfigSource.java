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

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

public class CasdoorOidcConfigSource implements ConfigSource {
    
    private final Map<String, String> properties;
    private static final int ORDINAL = 250;

    public CasdoorOidcConfigSource(ConfigSourceContext context) {
        this.properties = new HashMap<>();
        
        ConfigValue endpoint = context.getValue("quarkus.casdoor.endpoint");
        ConfigValue clientId = context.getValue("quarkus.casdoor.client-id");
        ConfigValue clientSecret = context.getValue("quarkus.casdoor.client-secret");
        ConfigValue enabled = context.getValue("quarkus.casdoor.enabled");
        
        if (isEnabled(enabled) && endpoint != null && clientId != null && clientSecret != null) {
            properties.put("quarkus.oidc.auth-server-url", endpoint.getValue());
            properties.put("quarkus.oidc.client-id", clientId.getValue());
            properties.put("quarkus.oidc.credentials.secret", clientSecret.getValue());
            properties.put("quarkus.oidc.application-type", "web-app");
        }
    }
    
    private boolean isEnabled(ConfigValue enabled) {
        return enabled == null || enabled.getValue() == null || !"false".equals(enabled.getValue());
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties.keySet();
    }

    @Override
    public String getValue(String propertyName) {
        return properties.get(propertyName);
    }

    @Override
    public String getName() {
        return "CasdoorOidcConfigSource";
    }

    @Override
    public int getOrdinal() {
        return ORDINAL;
    }

    public static class Factory implements ConfigSourceFactory {
        @Override
        public Iterable<ConfigSource> getConfigSources(ConfigSourceContext context) {
            return Collections.singletonList(new CasdoorOidcConfigSource(context));
        }

        @Override
        public OptionalInt getPriority() {
            return OptionalInt.of(ORDINAL);
        }
    }
}
