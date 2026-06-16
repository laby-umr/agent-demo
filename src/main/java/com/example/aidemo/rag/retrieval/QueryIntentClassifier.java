package com.example.aidemo.rag.retrieval;

import com.example.aidemo.rag.model.QueryIntent;
import java.util.regex.Pattern;

/** 规则优先的查询意图分类。 */
public final class QueryIntentClassifier {

    private static final Pattern SECTION_PATTERN = Pattern.compile("第.*章|条款|slide|Slide|页|章节|幻灯片");
    private static final Pattern TABLE_OVERVIEW_PATTERN = Pattern.compile("总结|概述|有哪些|概况|汇总|讲什么");
    private static final Pattern TABLE_CELL_PATTERN =
            Pattern.compile("多少|几岁|年龄|哪一行|Age|age|Profit|profit|利润|金额|工资|薪资|单价|数量|有哪些列|账面|余额|合计");
    private static final Pattern FINANCIAL_REPORT_PATTERN =
            Pattern.compile("财务报表|财务报告|合并报表|资产负债表|利润表|现金流量|所有者权益|期间报表|\\d{4}年\\d{1,2}月\\d{1,2}日.*期间");
    private static final Pattern ENTITY_PATTERN =
            Pattern.compile("[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*|\\b[A-Z]{2,}\\b|\\d{4,}");

    private QueryIntentClassifier() {}

    public static QueryIntent classify(String query) {
        if (query == null || query.isBlank()) {
            return QueryIntent.GENERAL;
        }
        String text = query.trim();
        if (SECTION_PATTERN.matcher(text).find()) {
            return QueryIntent.SECTION;
        }
        if (TABLE_OVERVIEW_PATTERN.matcher(text).find()) {
            return QueryIntent.TABLE_OVERVIEW;
        }
        if (FINANCIAL_REPORT_PATTERN.matcher(text).find()) {
            return QueryIntent.FINANCIAL_REPORT;
        }
        if (TABLE_CELL_PATTERN.matcher(text).find()) {
            return QueryIntent.TABLE_CELL;
        }
        if (ENTITY_PATTERN.matcher(text).find()) {
            return QueryIntent.ENTITY;
        }
        return QueryIntent.GENERAL;
    }
}
