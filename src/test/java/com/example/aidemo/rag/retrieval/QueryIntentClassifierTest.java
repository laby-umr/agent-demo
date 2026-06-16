package com.example.aidemo.rag.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.aidemo.rag.model.QueryIntent;
import org.junit.jupiter.api.Test;

class QueryIntentClassifierTest {

    @Test
    void classifiesTableAndSectionIntents() {
        assertEquals(QueryIntent.TABLE_CELL, QueryIntentClassifier.classify("张三工资多少"));
        assertEquals(QueryIntent.TABLE_OVERVIEW, QueryIntentClassifier.classify("这张表讲什么"));
        assertEquals(QueryIntent.SECTION, QueryIntentClassifier.classify("第三章保密义务"));
        assertEquals(QueryIntent.ENTITY, QueryIntentClassifier.classify("GBT 1568-2008"));
        assertEquals(
                QueryIntent.FINANCIAL_REPORT,
                QueryIntentClassifier.classify("2025年1月1日至6月30日止期间财务报表"));
    }
}
