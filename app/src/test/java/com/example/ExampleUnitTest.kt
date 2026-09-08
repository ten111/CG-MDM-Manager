package com.example

import com.example.presentation.stock.CouponOcrParser
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testCouponOcrParser_exactUserCoupon() {
    val sampleCouponText = """
      मध्यान्ह भोजन योजना
      ऑनलाईन मॉनिटरिंग सिस्टम
      (छत्तीसगढ़ शासन)
      चावल आबंटन पत्रक (Rice Allotment Coupon)
      प्रविष्टी माह: जून 2026
      आबंटन माह: जुलाई 2026
      पत्रक क्र. RCS70207260025 दिनांक: 25/06/2026
      संकुल: बोड़ला
      शाला का आईडी/नाम: 22080100308/मा.शा. बोड़ला / BODLA
      शाला का प्रकार: माध्यमिक शाला की दर्ज संख्या: 143
      स्वसहायता समूह का आईडी/नाम: 2208010030801/रिच्छिन माता महिला स्व सहायता समूह
      उ.मु.दु का आईडी/नाम: 572002016/सेवा सहकारी समिति
      जुलाई में शाला दिवस की संख्या: 25 जुलाई का आबंटन: 1.90 क्विटल
      नोट :- आबंटित चावल का उठाव कूपन जारी होने कि तिथि से 15 दिवस के भीतर सुनिश्चित करें।
      
      यहाँ से फाड़े -----------------------
      पत्रक क्र. RC570207260025
      दिनांक : 25/06/2026
      शाला का आईडी/नाम: 22080100308/मा.शा. बोड़ला / BODLA
      उ.मु.दु का आईडी/नाम: 572002016/सेवा सहकारी समिति
      स्वसहायता समूह का आईडी/नाम: 2208010030801/रिच्छिन माता महिला स्व सहायता समूह
      जुलाई का आबंटन:
      1.90 क्विटल
    """.trimIndent()

    val result = CouponOcrParser.extractCouponData(sampleCouponText)
    println("DEBUG testCouponOcrParser_exactUserCoupon: coupon=${result.couponNumber}, date=${result.receiptDateDdMmYyyy}, month=${result.allotmentMonth}, pds=${result.pdsShopName}, qty=${result.quantityKg}")
    assertTrue("Should be success", result.isSuccess)
    assertEquals("RC570207260025", result.couponNumber)
    assertEquals("01-07-2026", result.receiptDateDdMmYyyy)
    assertEquals("सेवा सहकारी समिति", result.pdsShopName)
    assertEquals(190.0, result.quantityKg, 0.001)
  }

  @Test
  fun testCouponOcrParser_separateLines() {
    val sampleText = """
      पत्रक क्र. RC570207260025
      आबंटन माह : जुलाई 2026
      उ.मु.दु: s72002016 सेवा सहकारी समिति
      जुलाई का आबंटन:
      1.90
      क्विटल
    """.trimIndent()

    val result = CouponOcrParser.extractCouponData(sampleText)
    assertTrue(result.isSuccess)
    assertEquals("RC570207260025", result.couponNumber)
    assertEquals("01-07-2026", result.receiptDateDdMmYyyy)
    assertEquals("सेवा सहकारी समिति", result.pdsShopName)
    assertEquals(190.0, result.quantityKg, 0.001)
  }

  @Test
  fun testCouponOcrParser_spacedAndSubstitutedDecimals() {
    // Case 1: Spaced dot "1 . 90 क्विटल"
    val textSpaced = """
      पत्रक क्र. RC570207260025
      आबंटन माह: जुलाई 2026
      शाला दिवस की संख्या: 25 जुलाई का आबंटन: 1 . 90 क्विटल
      उ.मु.दु: 572002016/सेवा सहकारी समिति
    """.trimIndent()
    val resSpaced = CouponOcrParser.extractCouponData(textSpaced)
    assertEquals(190.0, resSpaced.quantityKg, 0.001)
    assertNotEquals(2500.0, resSpaced.quantityKg, 0.001)

    // Case 2: Letter substitution "l.90 क्विटल"
    val textLetter = """
      पत्रक क्र. RC570207260025
      आबंटन माह: जुलाई 2026
      जुलाई में शाला दिवस की संख्या: 25
      जुलाई का आबंटन: l.90 क्विटल
      उ.मु.दु: 572002016/सेवा सहकारी समिति
    """.trimIndent()
    val resLetter = CouponOcrParser.extractCouponData(textLetter)
    assertEquals(190.0, resLetter.quantityKg, 0.001)

    // Case 3: Table cell layout where 25 is adjacent to आबंटन but 1.90 is on next line
    val textAdjacent = """
      पत्रक क्र. RC570207260025
      आबंटन माह: जुलाई 2026
      शाला दिवस की संख्या: 25
      जुलाई का आबंटन:
      1.90 क्विटल
      उ.मु.दु: सेवा सहकारी समिति
    """.trimIndent()
    val resAdjacent = CouponOcrParser.extractCouponData(textAdjacent)
    assertEquals(190.0, resAdjacent.quantityKg, 0.001)
    assertNotEquals(2500.0, resAdjacent.quantityKg, 0.001)
  }

  @Test
  fun testCouponOcrParser_augustCoupon() {
    val augustCouponText = """
      मध्याह्न भोजन योजना
      ऑनलाइन मॉनिटरिंग सिस्टम
      (छत्तीसगढ़ शासन)
      चावल आबंटन पत्रक (Rice Allotment Coupon)
      प्रविष्टी माह: जुलाई 2026 जिला: कवर्धा विकासखण्ड: बोड़ला आबंटन माह: अगस्त 2026
      पत्रक क्र. RC570208260025 दिनांक : 27/07/2026
      संकुल: बोडला
      शाला का आईडी / नाम : 22080100308/मा.शा. बोड़ला / BODLA
      शाला का प्रकार : माध्यमिक शाला की दर्ज संख्या: 143
      स्वसहायता समूह का आईडी/ नाम : 2208010030801/रिच्छिन माता महिला स्व सहायता समूह
      उ.मु.दु का आईडी/ नाम : 572002016/सेवा सहकारी समिति
      अगस्त में शाला दिवस की संख्या: 24 अगस्त का आबंटन: 2.30 क्विंटल
      नोट :- आबंटित चावल का उठाव कूपन जारी होने कि तिथि से 15 दिवस के भीतर सुनिश्चित करें।
      उचित मूल्य की दुकान के संचालनकर्ता के हस्ताक्षर एवं सील
      यहाँ से फाड़े
      पत्रक क्र. RC570208260025 दिनांक : 27/07/2026
      शाला का आईडी/ नाम : 22080100308/मा.शा. बोड़ला / BODLA
      उ.मु.दु का आईडी/ नाम : 572002016/सेवा सहकारी समिति
      स्वसहायता समूह का आईडी/ नाम : 2208010030801/रिच्छिन माता महिला स्व सहायता समूह
      अगस्त का आबंटन: 2.30 क्विंटल
    """.trimIndent()

    val res = CouponOcrParser.extractCouponData(augustCouponText)
    assertTrue(res.isSuccess)
    assertEquals("RC570208260025", res.couponNumber)
    assertEquals("01-08-2026", res.receiptDateDdMmYyyy) // Must be August, NOT September!
    assertEquals("सेवा सहकारी समिति", res.pdsShopName)
    assertEquals(230.0, res.quantityKg, 0.001) // 2.30 quintals = 230 kg
  }
}

