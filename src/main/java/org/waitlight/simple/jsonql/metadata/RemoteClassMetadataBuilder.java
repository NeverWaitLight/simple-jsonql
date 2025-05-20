package org.waitlight.simple.jsonql.metadata;

public class RemoteClassMetadataBuilder extends MetadataBuilder {

    private final MetadataSource metadataSource;

    public RemoteClassMetadataBuilder(MetadataSource metadataSource) {
        this.metadataSource = metadataSource;
    }

    /**
     * 从 {@link MetadataSource} 获取远程 class 输入源，解析并构建元数据
     */
    @Override
    public Metadata build() {
        return null;
    }
}
