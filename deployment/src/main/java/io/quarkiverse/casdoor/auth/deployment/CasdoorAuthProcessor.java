package io.quarkiverse.casdoor.auth.deployment;

import io.quarkiverse.casdoor.auth.runtime.CasdoorEnforcer;
import io.quarkiverse.casdoor.auth.runtime.CasdoorHttpSecurityPolicy;
import io.quarkiverse.casdoor.auth.runtime.CasdoorSecurityIdentityAugmentor;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class CasdoorAuthProcessor {

    private static final String FEATURE = "casdoor-auth";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(CasdoorEnforcer.class, CasdoorSecurityIdentityAugmentor.class,
                        CasdoorHttpSecurityPolicy.class)
                .setUnremovable()
                .build();
    }
}
