package org.waitlight.simple.jsonql.security;

import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.owasp.esapi.Validator;
import java.util.regex.Pattern;

public class SQLInjectionProtector {
    private static final Logger logger = ESAPI.getLogger(SQLInjectionProtector.class);
    private static final Validator validator = ESAPI.validator();
    private static final Pattern SQL_META_CHARS = Pattern.compile("['\"\\\\().;]");
    private static final Pattern SQL_KEYWORDS = Pattern.compile(
            "(?i)\\b(DROP|DELETE|INSERT|UPDATE|TRUNCATE|ALTER|CREATE|EXEC|UNION|XP_)\\b");

    public static String sanitize(String sql) {
        if (sql == null || sql.isEmpty()) {
            return sql;
        }

        try {
            // 使用ESAPI验证
            if (validator.isValidInput("SQL Input", sql, "SQL", 1000, false)) {
                return sql;
            }
        } catch (Exception e) {
            logger.warning(Logger.SECURITY_FAILURE, "SQL注入检测失败，使用基础验证", e);
        }

        // ESAPI验证失败时使用基础验证
        String sanitized = SQL_KEYWORDS.matcher(sql).replaceAll("");
        sanitized = SQL_META_CHARS.matcher(sanitized).replaceAll("\\\\$0");
        return sanitized.trim();
    }

    public static boolean isSafe(String sql) {
        if (sql == null || sql.isEmpty()) {
            return true;
        }

        try {
            return validator.isValidInput("SQL Input", sql, "SQL", 1000, false);
        } catch (Exception e) {
            logger.warning(Logger.SECURITY_FAILURE, "SQL注入检测失败，使用基础验证", e);
            return !SQL_KEYWORDS.matcher(sql).find() 
                    && !SQL_META_CHARS.matcher(sql).find();
        }
    }

    /**
     * 严格验证SQL语句，发现注入直接抛出异常
     */
    public static void validateSQL(String sql) throws SecurityException {
        if (sql == null || sql.isEmpty()) {
            return;
        }

        try {
            if (!validator.isValidInput("SQL Input", sql, "SQL", 1000, false)) {
                throw new SecurityException("检测到潜在的SQL注入: " + sql);
            }
        } catch (Exception e) {
            throw new SecurityException("SQL验证失败: " + e.getMessage(), e);
        }
    }
}
