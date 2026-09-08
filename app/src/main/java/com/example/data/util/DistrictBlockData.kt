package com.example.data.util

data class DistrictInfo(
    val nameEn: String,
    val nameHi: String,
    val blocks: List<BlockInfo>
)

data class BlockInfo(
    val nameEn: String,
    val nameHi: String
)

object LocationData {
    val CHHATTISGARH_DISTRICTS: List<DistrictInfo> = listOf(
        DistrictInfo(
            nameEn = "Kabirdham (Kawardha)",
            nameHi = "कबीरधाम (कवर्धा)",
            blocks = listOf(
                BlockInfo("Bodla", "बोड़ला"),
                BlockInfo("Kawardha", "कवर्धा"),
                BlockInfo("Pandariya", "पंडरिया"),
                BlockInfo("Sahaspur Lohara", "सहसपुर लोहारा")
            )
        ),
        DistrictInfo(
            nameEn = "Raipur",
            nameHi = "रायपुर",
            blocks = listOf(
                BlockInfo("Dharsiwa", "धरसीवां"),
                BlockInfo("Arang", "आरंग"),
                BlockInfo("Abhanpur", "अभनपुर"),
                BlockInfo("Tilda", "तिल्दा")
            )
        ),
        DistrictInfo(
            nameEn = "Durg",
            nameHi = "दुर्ग",
            blocks = listOf(
                BlockInfo("Durg", "दुर्ग"),
                BlockInfo("Dhamdha", "धमधा"),
                BlockInfo("Patan", "पाटन")
            )
        ),
        DistrictInfo(
            nameEn = "Bilaspur",
            nameHi = "बिलासपुर",
            blocks = listOf(
                BlockInfo("Bilha", "बिल्हा"),
                BlockInfo("Kota", "कोटा"),
                BlockInfo("Masturi", "मस्तुरी"),
                BlockInfo("Takhatpur", "तखतपुर"),
                BlockInfo("Belgahna", "बेलगहना")
            )
        ),
        DistrictInfo(
            nameEn = "Rajnandgaon",
            nameHi = "राजनांदगांव",
            blocks = listOf(
                BlockInfo("Rajnandgaon", "राजनांदगांव"),
                BlockInfo("Dongargaon", "डोंगरगांव"),
                BlockInfo("Dongargarh", "डोंगरगढ़"),
                BlockInfo("Chhuria", "छुरिया")
            )
        ),
        DistrictInfo(
            nameEn = "Baloda Bazar-Bhatapara",
            nameHi = "बलौदाबाजार-भाटापारा",
            blocks = listOf(
                BlockInfo("Baloda Bazar", "बलौदाबाजार"),
                BlockInfo("Bhatapara", "भाटापारा"),
                BlockInfo("Kasdol", "कसडोल"),
                BlockInfo("Palari", "पलारी"),
                BlockInfo("Simga", "सिमगा")
            )
        ),
        DistrictInfo(
            nameEn = "Dhamtari",
            nameHi = "धमतरी",
            blocks = listOf(
                BlockInfo("Dhamtari", "धमतरी"),
                BlockInfo("Kurud", "कुरुद"),
                BlockInfo("Magarlod", "मगरलोड"),
                BlockInfo("Nagari", "नगरी")
            )
        ),
        DistrictInfo(
            nameEn = "Mahasamund",
            nameHi = "महासमुंद",
            blocks = listOf(
                BlockInfo("Mahasamund", "महासमुंद"),
                BlockInfo("Bagbahra", "बागबाहरा"),
                BlockInfo("Basna", "बसना"),
                BlockInfo("Pithora", "पिथौरा"),
                BlockInfo("Saraipali", "सरायपाली")
            )
        ),
        DistrictInfo(
            nameEn = "Gariaband",
            nameHi = "गरियाबंद",
            blocks = listOf(
                BlockInfo("Gariaband", "गरियाबंद"),
                BlockInfo("Chhura", "छुरा"),
                BlockInfo("Fingeshwar", "फिंगेश्वर"),
                BlockInfo("Deobhog", "देवभोग"),
                BlockInfo("Mainpur", "मैनपुर")
            )
        ),
        DistrictInfo(
            nameEn = "Bemetara",
            nameHi = "बेमेतरा",
            blocks = listOf(
                BlockInfo("Bemetara", "बेमेतरा"),
                BlockInfo("Berla", "बेरला"),
                BlockInfo("Nawagarh", "नवागढ़"),
                BlockInfo("Saja", "साजा"),
                BlockInfo("Thankhamharia", "थानखम्हरिया")
            )
        ),
        DistrictInfo(
            nameEn = "Balod",
            nameHi = "बालोद",
            blocks = listOf(
                BlockInfo("Balod", "बालोद"),
                BlockInfo("Doundi", "डौंडी"),
                BlockInfo("Doundilohara", "डौंडीलोहारा"),
                BlockInfo("Gunderdehi", "गुण्डरदेही"),
                BlockInfo("Gurur", "गुरूर")
            )
        ),
        DistrictInfo(
            nameEn = "Janjgir-Champa",
            nameHi = "जांजगीर-चांपा",
            blocks = listOf(
                BlockInfo("Janjgir", "जांजगीर"),
                BlockInfo("Akaltara", "अकलतरा"),
                BlockInfo("Baloda", "बलौदा"),
                BlockInfo("Bamhnidih", "बम्हनीडीह"),
                BlockInfo("Champa", "चांपा"),
                BlockInfo("Nawagarh", "नवागढ़"),
                BlockInfo("Pamgarh", "पामगढ़")
            )
        ),
        DistrictInfo(
            nameEn = "Sakti",
            nameHi = "सक्ती",
            blocks = listOf(
                BlockInfo("Sakti", "सक्ती"),
                BlockInfo("Dabhra", "डभरा"),
                BlockInfo("Jaijaipur", "जैजैपुर"),
                BlockInfo("Malkharoda", "मालखरौदा")
            )
        ),
        DistrictInfo(
            nameEn = "Korba",
            nameHi = "कोरबा",
            blocks = listOf(
                BlockInfo("Korba", "कोरबा"),
                BlockInfo("Katghora", "कटघोरा"),
                BlockInfo("Kartala", "करतला"),
                BlockInfo("Pali", "पाली"),
                BlockInfo("Podi Uproda", "पोड़ी उपरोड़ा")
            )
        ),
        DistrictInfo(
            nameEn = "Raigarh",
            nameHi = "रायगढ़",
            blocks = listOf(
                BlockInfo("Raigarh", "रायगढ़"),
                BlockInfo("Dharamjaigarh", "धरमजयगढ़"),
                BlockInfo("Gharghoda", "घरघोड़ा"),
                BlockInfo("Kharsia", "खरसिया"),
                BlockInfo("Lailunga", "लैलूंगा"),
                BlockInfo("Pussore", "पुसौर"),
                BlockInfo("Tamnar", "तमनार")
            )
        ),
        DistrictInfo(
            nameEn = "Sarangarh-Bilaigarh",
            nameHi = "सारंगढ़-बिलाईगढ़",
            blocks = listOf(
                BlockInfo("Sarangarh", "सारंगढ़"),
                BlockInfo("Bilaigarh", "बिलाईगढ़"),
                BlockInfo("Baramkela", "बरमकेला")
            )
        ),
        DistrictInfo(
            nameEn = "Mungeli",
            nameHi = "मुंगेली",
            blocks = listOf(
                BlockInfo("Mungeli", "मुंगेली"),
                BlockInfo("Lormi", "लोरमी"),
                BlockInfo("Pathariya", "पथरिया")
            )
        ),
        DistrictInfo(
            nameEn = "Gaurela-Pendra-Marwahi",
            nameHi = "गौरेला-पेंड्रा-मरवाही",
            blocks = listOf(
                BlockInfo("Gaurela", "गौरेला"),
                BlockInfo("Pendra", "पेंड्रा"),
                BlockInfo("Marwahi", "मरवाही")
            )
        ),
        DistrictInfo(
            nameEn = "Surguja (Ambikapur)",
            nameHi = "सरगुजा (अम्बिकापुर)",
            blocks = listOf(
                BlockInfo("Ambikapur", "अम्बिकापुर"),
                BlockInfo("Batauli", "बतौली"),
                BlockInfo("Lakhanpur", "लखनपुर"),
                BlockInfo("Lundra", "लुण्ड्रा"),
                BlockInfo("Mainpat", "मैनपाट"),
                BlockInfo("Sitapur", "सीतापुर"),
                BlockInfo("Udaipur", "उदयपुर")
            )
        ),
        DistrictInfo(
            nameEn = "Surajpur",
            nameHi = "सूरजपुर",
            blocks = listOf(
                BlockInfo("Surajpur", "सूरजपुर"),
                BlockInfo("Bhaiyathan", "भैयाथान"),
                BlockInfo("Odgi", "ओड़गी"),
                BlockInfo("Pratappur", "प्रतापपुर"),
                BlockInfo("Premnagar", "प्रेमनगर"),
                BlockInfo("Ramanujnagar", "रामानुजनगर")
            )
        ),
        DistrictInfo(
            nameEn = "Balrampur-Ramanujganj",
            nameHi = "बलरामपुर-रामानुजगंज",
            blocks = listOf(
                BlockInfo("Balrampur", "बलरामपुर"),
                BlockInfo("Kusmi", "कुसमी"),
                BlockInfo("Rajpur", "राजपुर"),
                BlockInfo("Ramchandrapur", "रामचन्द्रपुर"),
                BlockInfo("Shankargarh", "शंकरगढ़"),
                BlockInfo("Wadrafnagar", "वाड्राफनगर")
            )
        ),
        DistrictInfo(
            nameEn = "Jashpur",
            nameHi = "जशपुर",
            blocks = listOf(
                BlockInfo("Jashpur", "जशपुर"),
                BlockInfo("Bagicha", "बगीचा"),
                BlockInfo("Duldula", "दुलदुला"),
                BlockInfo("Farsabahar", "फरसाबहार"),
                BlockInfo("Kansabel", "कांसाबेल"),
                BlockInfo("Kunkuri", "कुनकुरी"),
                BlockInfo("Manora", "मनोरा"),
                BlockInfo("Pathalgaon", "पत्थलगांव")
            )
        ),
        DistrictInfo(
            nameEn = "Koriya",
            nameHi = "कोरिया",
            blocks = listOf(
                BlockInfo("Baikunthpur", "बैकुंठपुर"),
                BlockInfo("Sonhat", "सोनहत")
            )
        ),
        DistrictInfo(
            nameEn = "Manendragarh-Chirmiri-Bharatpur",
            nameHi = "मनेन्द्रगढ़-चिरमिरी-भरतपुर",
            blocks = listOf(
                BlockInfo("Bharatpur", "भरतपुर"),
                BlockInfo("Khadgawan", "खड़गवां"),
                BlockInfo("Manendragarh", "मनेन्द्रगढ़")
            )
        ),
        DistrictInfo(
            nameEn = "Bastar (Jagdalpur)",
            nameHi = "बस्तर (जगदलपुर)",
            blocks = listOf(
                BlockInfo("Jagdalpur", "जगदलपुर"),
                BlockInfo("Bakawand", "बकावंड"),
                BlockInfo("Bastar", "बस्तर"),
                BlockInfo("Bastanar", "बास्तानार"),
                BlockInfo("Darbha", "दरभा"),
                BlockInfo("Lohandiguda", "लोहंडीगुड़ा"),
                BlockInfo("Tokapal", "तोकापाल")
            )
        ),
        DistrictInfo(
            nameEn = "Kondagaon",
            nameHi = "कोंडागांव",
            blocks = listOf(
                BlockInfo("Kondagaon", "कोंडागांव"),
                BlockInfo("Keshkal", "केशकाल"),
                BlockInfo("Makdi", "माकड़ी"),
                BlockInfo("Baderajpur", "बड़ेराजपुर"),
                BlockInfo("Pharasgaon", "फरसगांव")
            )
        ),
        DistrictInfo(
            nameEn = "Kanker (North Bastar)",
            nameHi = "कांकेर (उत्तर बस्तर)",
            blocks = listOf(
                BlockInfo("Kanker", "कांकेर"),
                BlockInfo("Antagarh", "अंतागढ़"),
                BlockInfo("Bhanupratappur", "भानुप्रतापपुर"),
                BlockInfo("Charama", "चारमा"),
                BlockInfo("Durgukondal", "दुर्गूकोंदल"),
                BlockInfo("Koylibeda (Pakhanjur)", "कोयलीबेड़ा (पखांजूर)"),
                BlockInfo("Narharpur", "नरहरपुर")
            )
        ),
        DistrictInfo(
            nameEn = "Dantewada (South Bastar)",
            nameHi = "दंतेवाड़ा (दक्षिण बस्तर)",
            blocks = listOf(
                BlockInfo("Dantewada", "दंतेवाड़ा"),
                BlockInfo("Geedam", "गीदम"),
                BlockInfo("Katekalyan", "कटेकल्याण"),
                BlockInfo("Kuakonda", "कुआकोंडा")
            )
        ),
        DistrictInfo(
            nameEn = "Sukma",
            nameHi = "सुकमा",
            blocks = listOf(
                BlockInfo("Sukma", "सुकमा"),
                BlockInfo("Chhindgarh", "छिंदगढ़"),
                BlockInfo("Konta", "कोंटा")
            )
        ),
        DistrictInfo(
            nameEn = "Bijapur",
            nameHi = "बीजापुर",
            blocks = listOf(
                BlockInfo("Bijapur", "बीजापुर"),
                BlockInfo("Bhairamgarh", "भैरमगढ़"),
                BlockInfo("Bhopalpatnam", "भोपालपटनम"),
                BlockInfo("Usur", "उसूर")
            )
        ),
        DistrictInfo(
            nameEn = "Narayanpur",
            nameHi = "नारायणपुर",
            blocks = listOf(
                BlockInfo("Narayanpur", "नारायणपुर"),
                BlockInfo("Orchha", "ओरछा")
            )
        ),
        DistrictInfo(
            nameEn = "Mohla-Manpur-Ambagarh Chowki",
            nameHi = "मोहला-मानपुर-अंबागढ़ चौकी",
            blocks = listOf(
                BlockInfo("Mohla", "मोहला"),
                BlockInfo("Manpur", "मानपुर"),
                BlockInfo("Ambagarh Chowki", "अंबागढ़ चौकी")
            )
        ),
        DistrictInfo(
            nameEn = "Khairagarh-Chhuikhadan-Gandai",
            nameHi = "खैरागढ़-छुईखदान-गंडई",
            blocks = listOf(
                BlockInfo("Khairagarh", "खैरागढ़"),
                BlockInfo("Chhuikhadan", "छुईखदान"),
                BlockInfo("Gandai", "गंडई")
            )
        )
    )

    val SCHOOL_TYPES: List<Pair<String, String>> = listOf(
        Pair("Primary 1-5", "प्राथमिक 1-5 (Primary Class 1-5)"),
        Pair("Middle school 6-8", "पूर्व माध्यमिक 6-8 (Middle School Class 6-8)"),
        Pair("Middle with primary 1-8", "प्राथमिक सह पूर्व माध्यमिक 1-8 (Middle with Primary Class 1-8)")
    )
}
