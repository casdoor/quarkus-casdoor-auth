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

package casbin.casdoor.quarkus.auth.deployment;

import casbin.casdoor.quarkus.auth.runtime.AuthServiceProducer;
import casbin.casdoor.quarkus.auth.runtime.CasdoorConfig;
import casbin.casdoor.quarkus.auth.runtime.CasdoorConfigResolver;
import casbin.casdoor.quarkus.auth.runtime.CasdoorConfigUtil;
import casbin.casdoor.quarkus.auth.runtime.CasdoorHttpSecurityPolicy;
import casbin.casdoor.quarkus.auth.runtime.DefaultCasdoorConfigResolver;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;

class QuarkusCasdoorAuthProcessor {

    private static final String FEATURE = "quarkus-casdoor-auth";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem registerBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(
                    CasdoorConfig.class,
                    CasdoorConfigResolver.class,
                    CasdoorConfigUtil.class,
                    DefaultCasdoorConfigResolver.class,
                    CasdoorHttpSecurityPolicy.class,
                    AuthServiceProducer.class
                )
                .setUnremovable()
                .build();
    }

    @BuildStep
    ReflectiveClassBuildItem reflectiveClasses() {
        return ReflectiveClassBuildItem.builder(
                CasdoorConfig.class,
                org.casbin.casdoor.config.Config.class,
                org.casbin.casdoor.entity.User.class,
                org.casbin.casdoor.service.AuthService.class
        ).build();
    }

    @BuildStep
    LogCleanupFilterBuildItem logCleanup() {
        return new LogCleanupFilterBuildItem(
                "org.apache.http", 
                "HTTP request failed",
                "Connection reset"
        );
    }
}
