package com.fanxin.ksxai.utils;

import java.nio.charset.StandardCharsets;

public class TextCleanerUtils {
    
    /**
     * 清理文本中的非UTF-8字符
     * @param text 原始文本
     * @return 清理后的UTF-8文本
     */
    public static String cleanText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 将文本转换为UTF-8字节数组，再转回字符串，自动去除非法字符
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    /**
     * 移除文本中的控制字符（除了常见的换行符、制表符等）
     * @param text 原始文本
     * @return 清理后的文本
     */
    public static String removeControlCharacters(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        return text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
    }
    
    /**
     * 综合清理文本
     * @param text 原始文本
     * @return 清理后的文本
     */
    public static String cleanTextCompletely(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 先移除控制字符
        text = removeControlCharacters(text);
        // 再确保是有效的UTF-8
        text = cleanText(text);
        return text;
    }
}