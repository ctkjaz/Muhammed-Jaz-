package com.example

import com.example.data.model.AbgCompensationStatus
import com.example.data.model.AbgInput
import com.example.data.model.AbgInterpreterLogic
import com.example.data.model.AbgPracticeCatalog
import com.example.data.model.AbgPracticeTier
import com.example.data.model.AbgPrimaryDisorder
import com.example.data.model.EcgStudioCatalog
import com.example.data.model.EcgWaveformEngine
import com.example.data.model.EcgWaveformPattern
import com.example.data.model.OxygenationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase3AbgAndEcgLogicTest {

    @Test
    fun abgInterpreter_classifiesFourPrimaryDisordersAndNormalAccurately() {
        val normal = AbgInterpreterLogic.interpret(AbgInput(7.40, 40.0, 24.0, 95.0))
        assertEquals(AbgPrimaryDisorder.NORMAL, normal.primaryDisorder)
        assertEquals(AbgCompensationStatus.NORMAL, normal.compensationStatus)
        assertEquals(OxygenationStatus.NORMAL, normal.oxygenationStatus)

        val respAcid = AbgInterpreterLogic.interpret(AbgInput(7.24, 60.0, 25.0, 62.0))
        assertEquals(AbgPrimaryDisorder.RESPIRATORY_ACIDOSIS, respAcid.primaryDisorder)
        assertEquals(AbgCompensationStatus.UNCOMPENSATED, respAcid.compensationStatus)
        assertEquals(OxygenationStatus.MILD_HYPOXEMIA, respAcid.oxygenationStatus)

        val respAlk = AbgInterpreterLogic.interpret(AbgInput(7.53, 24.0, 23.0, 98.0))
        assertEquals(AbgPrimaryDisorder.RESPIRATORY_ALKALOSIS, respAlk.primaryDisorder)
        assertEquals(AbgCompensationStatus.UNCOMPENSATED, respAlk.compensationStatus)

        val metAcid = AbgInterpreterLogic.interpret(AbgInput(7.26, 38.0, 16.0, 92.0))
        assertEquals(AbgPrimaryDisorder.METABOLIC_ACIDOSIS, metAcid.primaryDisorder)
        assertEquals(AbgCompensationStatus.MIXED_SUPERIMPOSED, metAcid.compensationStatus) // Winter's expects 30-34 -> CO2 38 flags inadequate ventilation!

        val metAlk = AbgInterpreterLogic.interpret(AbgInput(7.51, 43.0, 34.0, 91.0))
        assertEquals(AbgPrimaryDisorder.METABOLIC_ALKALOSIS, metAlk.primaryDisorder)
        assertEquals(AbgCompensationStatus.UNCOMPENSATED, metAlk.compensationStatus)
    }

    @Test
    fun abgInterpreter_wintersFormulaMixedDisordersAndAnionGap() {
        // Classic DKA with appropriate Winter's compensation: HCO3=10 -> expected CO2 21-25, measured 23
        val dka = AbgInterpreterLogic.interpret(
            AbgInput(ph = 7.22, paco2 = 23.0, hco3 = 10.0, pao2 = 96.0, sodium = 138.0, chloride = 102.0)
        )
        assertEquals(AbgPrimaryDisorder.METABOLIC_ACIDOSIS, dka.primaryDisorder)
        assertEquals(AbgCompensationStatus.PARTIALLY_COMPENSATED, dka.compensationStatus)
        assertNotNull(dka.anionGap)
        assertTrue(dka.anionGap!!.isElevated)
        assertEquals(26.0, dka.anionGap!!.anionGap, 0.01)

        // Salicylate toxicity: HCO3=12 -> Winter's expects 24-28, actual CO2=18 -> superimposed Respiratory Alkalosis
        val salicylate = AbgInterpreterLogic.interpret(AbgInput(ph = 7.31, paco2 = 18.0, hco3 = 12.0, pao2 = 98.0))
        assertEquals(AbgPrimaryDisorder.METABOLIC_ACIDOSIS, salicylate.primaryDisorder)
        assertEquals(AbgCompensationStatus.MIXED_SUPERIMPOSED, salicylate.compensationStatus)
        assertTrue(salicylate.mixedDisorderNote?.contains("Respiratory Alkalosis") == true)

        // Fully compensated chronic COPD retainer (pH 7.37 < 7.40 leans respiratory acidosis)
        val copd = AbgInterpreterLogic.interpret(AbgInput(ph = 7.37, paco2 = 60.0, hco3 = 34.0, pao2 = 54.0))
        assertEquals(AbgPrimaryDisorder.RESPIRATORY_ACIDOSIS, copd.primaryDisorder)
        assertEquals(AbgCompensationStatus.FULLY_COMPENSATED, copd.compensationStatus)
        assertEquals(OxygenationStatus.MODERATE_HYPOXEMIA, copd.oxygenationStatus)

        // Combined respiratory + metabolic acidosis with severe hypoxemia
        val arrest = AbgInterpreterLogic.interpret(AbgInput(ph = 7.10, paco2 = 58.0, hco3 = 16.0, pao2 = 36.0))
        assertEquals(AbgPrimaryDisorder.MIXED_ACIDOSIS, arrest.primaryDisorder)
        assertEquals(OxygenationStatus.SEVERE_HYPOXEMIA, arrest.oxygenationStatus)
    }

    @Test
    fun abgPracticeCatalog_hasAllThreeTiersAndValidInputs() {
        assertEquals(12, AbgPracticeCatalog.ALL_CASES.size)
        AbgPracticeTier.entries.forEach { tier ->
            assertEquals(4, AbgPracticeCatalog.casesForTier(tier).size)
        }
        AbgPracticeCatalog.ALL_CASES.forEach { c ->
            assertNull("Case ${c.id} has invalid ABG input", AbgInterpreterLogic.validateInput(c.input))
            assertEquals(4, c.options.size)
            assertTrue(c.correctIndex in 0..3)
        }
        assertNotNull(AbgInterpreterLogic.validateInput(AbgInput(ph = 5.0, paco2 = 40.0, hco3 = 24.0)))
    }

    @Test
    fun ecgCatalogAndWaveformEngine_generatesCalibratedFiniteStripsForAll14Patterns() {
        assertEquals(14, EcgStudioCatalog.ALL_CASES.size)
        assertEquals(EcgWaveformPattern.entries.size, EcgStudioCatalog.ALL_CASES.size)

        val deck = EcgStudioCatalog.buildPracticeDeck()
        assertEquals(14, deck.size)
        deck.forEach { q ->
            assertEquals(4, q.options.size)
            assertEquals(4, q.options.distinct().size)
            assertEquals(q.caseItem.title, q.options[q.correctIndex])
        }

        EcgWaveformPattern.entries.forEach { pattern ->
            val samples = EcgWaveformEngine.sampleStrip(pattern)
            assertEquals(EcgWaveformEngine.DEFAULT_SAMPLE_COUNT, samples.size)
            var maxMv = Float.NEGATIVE_INFINITY
            var minMv = Float.POSITIVE_INFINITY
            for (v in samples) {
                assertTrue("NaN sample in $pattern", !v.isNaN() && !v.isInfinite())
                if (v > maxMv) maxMv = v
                if (v < minMv) minMv = v
            }
            assertTrue("Waveform $pattern should have meaningful amplitude spread", (maxMv - minMv) > 0.45f)
            assertTrue("Waveform $pattern bounded within paper limits", maxMv <= 1.81f && minMv >= -1.61f)
        }
    }
}
