package org.example.model;

public enum ModuleType {
    BANNER("Banner轮播"),
    IMAGE_TEXT("图文卡片"),
    ARTICLE_LIST("列表模块（文章）"),
    PRODUCT_LIST("列表模块（商品）"),
    CATEGORY_NAV("分类导航模块"),
    RICH_TEXT("富文本区块"),
    VIDEO("视频模块");

    private final String label;

    ModuleType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
