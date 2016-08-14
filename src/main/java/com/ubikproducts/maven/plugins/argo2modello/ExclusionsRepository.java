package com.ubikproducts.maven.plugins.argo2modello;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.ubikproducts.maven.plugins.argo2modello.ArgoUMLDriver.ArgoUMLDriverBuilder;

public class ExclusionsRepository {

    private final Set<String> excludedClassesSet = new HashSet<String>();

    private Logger log = Logger.getLogger(ExclusionsRepository.class);

    public void registerExclusions(String excludedClasses) {
        excludedClassesSet.clear();
        if (excludedClasses != null) {
            String[] excludedClassesArray = excludedClasses.split(",");
            for (String s : excludedClassesArray) {
                excludedClassesSet.add(s);
            }
            log.info("Excluded classes: " + excludedClasses);
        } else {
            log.info("No excluded classes");
        }
    }

    public boolean isExcluded(String className) {
        return excludedClassesSet.contains(className);
    }
    
    private ExclusionsRepository() {
        
    }

    public static class ExclusionsRepositoryBuilder {

        private final ExclusionsRepository exclusionsRepository;

        private ExclusionsRepositoryBuilder() {
            exclusionsRepository = new ExclusionsRepository();
        }

        public static ExclusionsRepositoryBuilder newBuilder() {
            return new ExclusionsRepositoryBuilder();
        }

        public ExclusionsRepositoryBuilder withExclusions(String exclusions) {
            exclusionsRepository.registerExclusions(exclusions);
            return this;
        }

        public ExclusionsRepositoryBuilder withExclusion(String exclusion) {
            exclusionsRepository.excludedClassesSet.add(exclusion);
            return this;
        }

        public ExclusionsRepository build() {
            return exclusionsRepository;
        }
    }
    public ExclusionsRepository newRepository() {
        return new ExclusionsRepository();
    }
}
