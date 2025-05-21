package org.waitlight.simple.jsonql.metadata;

public class Metadata {

    public static final DefaultMetadataCache DEFAULT_METADATA_CACHE = new DefaultMetadataCache();
    public static final JOOQMetadataCache JOOQ_METADATA_CACHE = new JOOQMetadataCache();
    public static final CalciteMetadataCache CALCITE_METADATA_CACHE = new CalciteMetadataCache();

    public void add(PersistentClass persistentClass) {
        DEFAULT_METADATA_CACHE.add(persistentClass);
        JOOQ_METADATA_CACHE.add(persistentClass);
        CALCITE_METADATA_CACHE.add(persistentClass);
    }

}
