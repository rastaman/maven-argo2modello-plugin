package com.ubikproducts.maven.plugins.argo2modello;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

public class TypesRepository {

    private final Set<String> typesSet = new HashSet<String>();

    private Logger log = Logger.getLogger(TypesRepository.class);

    public void registerTypes(String types) {
        typesSet.clear();
        if (types != null) {
            String[] typesArray = types.split(",");
            for (String s : typesArray) {
                typesSet.add(s);
            }
            log.info("Excluded classes: " + types);
        } else {
            log.info("No excluded classes");
        }
    }

    public boolean isKnown(String className) {
        return ModelloTypesHelper.isBaseType(className) || typesSet.contains(className);
    }

    private TypesRepository() {

    }

    public static class TypesRepositoryBuilder {

        private final TypesRepository typesRepository;

        private TypesRepositoryBuilder() {
            typesRepository = new TypesRepository();
        }

        public static TypesRepositoryBuilder newBuilder() {
            return new TypesRepositoryBuilder();
        }

        public TypesRepositoryBuilder withTypes(String types) {
            typesRepository.registerTypes(types);
            return this;
        }

        public TypesRepositoryBuilder withType(String type) {
            typesRepository.typesSet.add(type);
            return this;
        }

        public TypesRepository build() {
            return typesRepository;
        }
    }
}
