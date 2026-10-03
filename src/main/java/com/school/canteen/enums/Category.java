package com.school.canteen.enums;

/** 食材品类 */
public enum Category {
    GRAIN_OIL("米面油"),
    MEAT("肉类"),
    VEGETABLE("蔬菜"),
    FRUIT("水果"),
    DAIRY("乳制品"),
    CONDIMENT("调味品");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
