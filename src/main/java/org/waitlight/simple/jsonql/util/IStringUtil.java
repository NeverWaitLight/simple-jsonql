package org.waitlight.simple.jsonql.util;

/**
 * 字符串工具类
 */
public class IStringUtil {

    /**
     * 将驼峰命名转换为下划线风格
     * 例如：UserRole -> user_role
     *
     * @param camelCaseStr 驼峰命名的字符串
     * @return 下划线风格的字符串
     */
    public static String camelToSnake(String camelCaseStr) {
        if (camelCaseStr == null || camelCaseStr.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        // 第一个字符小写
        result.append(Character.toLowerCase(camelCaseStr.charAt(0)));

        // 处理剩余字符
        for (int i = 1; i < camelCaseStr.length(); i++) {
            char ch = camelCaseStr.charAt(i);
            if (Character.isUpperCase(ch)) {
                result.append('_');
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }

        return result.toString();
    }
}