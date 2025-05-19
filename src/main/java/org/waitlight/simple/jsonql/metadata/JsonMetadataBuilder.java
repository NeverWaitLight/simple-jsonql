package org.waitlight.simple.jsonql.metadata;

public class JsonMetadataBuilder implements MetadataBuilder {
    private final MetadataSource metadataSource;

    public JsonMetadataBuilder(MetadataSource metadataSource) {
        this.metadataSource = metadataSource;
    }

    /**
     * 从 {@link MetadataSource} 获取 json 输入源，解析并构建元数据
     */
    @Override
    public Metadata build() {
        return null;
    }
}
