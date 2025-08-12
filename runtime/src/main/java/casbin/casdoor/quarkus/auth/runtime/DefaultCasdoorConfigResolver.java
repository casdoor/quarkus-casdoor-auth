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

import jakarta.enterprise.context.ApplicationScoped;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class DefaultCasdoorConfigResolver implements CasdoorConfigResolver {

    private final CasdoorConfig casdoorConfig;

    public DefaultCasdoorConfigResolver(CasdoorConfig config) {
        this.casdoorConfig = validateAndPrepareConfig(config);
    }

    @Override
    public Uni<CasdoorConfig> resolveCasdoorConfig() {
        return Uni.createFrom().item(casdoorConfig);
    }

    /**
     * Validates and prepares the configuration.
     *
     * @param config the configuration
     * @return the validated Casdoor configuration
     */
    private CasdoorConfig validateAndPrepareConfig(CasdoorConfig config) {
        if (!config.enabled()) {
            throw new IllegalStateException("Casdoor configuration is disabled");
        }
        
        CasdoorConfigUtil.validateConfig(config);
        
        return config;
    }
}
