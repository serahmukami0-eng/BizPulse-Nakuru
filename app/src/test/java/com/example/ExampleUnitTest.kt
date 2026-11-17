package com.example

import com.example.data.model.AppModule
import com.example.data.model.ResearchFramework
import com.example.data.model.formatKes
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun formatKes_formatsKenyanShillingsProperly() {
        assertEquals("KES 48,250", formatKes(48250.0))
        assertEquals("KES 0", formatKes(0.0))
        assertEquals("KES 1,250.50", formatKes(1250.5, includeDecimals = true))
    }

    @Test
    fun appModules_alignWithResearchFramework() {
        assertEquals(10, AppModule.entries.size)
        assertEquals("A", AppModule.DASHBOARD.code)
        assertEquals("G", AppModule.REPORTS.code)
        assertEquals("H", AppModule.CUSTOMERS.code)
        assertEquals("I", AppModule.SUPPLIERS.code)
        assertEquals("AI", AppModule.AI_ASSISTANT.code)
        assertEquals(4, ResearchFramework.independentVariables.size)
        assertEquals(4, ResearchFramework.performanceDimensions.size)
    }
}
