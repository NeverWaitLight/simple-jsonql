package org.waitlight.simple.jsonql.metadata;

/**
 * 元数据构建器工厂，用于创建不同类型的MetadataBuilder实例
 */
public class MetadataBuilderFactory {
    /**
     * 创建本地类元数据构建器
     *
     * @param metadataSource 元数据源
     * @return 本地类元数据构建器
     */
    public static MetadataBuilder createLocalBuilder(MetadataSource metadataSource) {
        return new LocalClassMetadataBuilder(metadataSource);
    }

    /**
     * 创建远程类元数据构建器
     *
     * @param metadataSource 元数据源
     * @return 远程类元数据构建器
     */
    public static MetadataBuilder createRemoteBuilder(MetadataSource metadataSource) {
        return new RemoteClassMetadataBuilder(metadataSource);
    }

    public static JsonMetadataBuilder createJsonBuilder(MetadataSource metadataSource) {
        return new JsonMetadataBuilder(metadataSource);
    }
}
