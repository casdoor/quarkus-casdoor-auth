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

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

@ConfigMapping(prefix = "quarkus.casdoor")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface CasdoorConfig {

    /**
     * The Casdoor server endpoint URL.
     */
    Optional<String> endpoint();

    /**
     * The organization name in Casdoor.
     */
    Optional<String> organizationName();

    /**
     * The client ID of the application in Casdoor.
     */
    Optional<String> clientId();

    /**
     * The client secret of the application in Casdoor.
     */
    Optional<String> clientSecret();

    /**
     * The certificate for JWT token verification.
     */
    Optional<String> certificate();

    /**
     * The application name in Casdoor.
     */
    Optional<String> applicationName();

    /**
     * Whether to enable Casdoor configuration.
     */
    @WithDefault("true")
    boolean enabled();
}
