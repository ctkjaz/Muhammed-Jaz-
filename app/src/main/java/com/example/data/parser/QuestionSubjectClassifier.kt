package com.example.data.parser

import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.Question
import com.example.data.model.SubjectCatalog
import java.util.Locale

/**
 * Pure-JVM deterministic subject scanner & auto-distributor for mixed PYQ / mock papers.
 * Scans each question's text, options, and explanation to distribute questions automatically
 * across ClinEdge's 16 canonical subjects (MSN, Pharm, OBG, Peds, CHN, Psych, FON, Anat,
 * Admin, CA-2026, Railways, Health-Prog, Static-GK, Science, Math, Reasoning).
 */
object QuestionSubjectClassifier {
    const val AUTO_SUBJECT_CODE = "AUTO"

    private data class SubjectRule(
        val code: String,
        val strongPhrases: List<String>,
        val keywords: List<String>
    )

    private val explicitSubjectTagRegex = Regex(
        """(?:^|\b)(?:Subject|Subj)\s*[:\-]\s*([A-Za-z0-9\-]+)""",
        RegexOption.IGNORE_CASE
    )

    private val rules: List<SubjectRule> = listOf(
        SubjectRule(
            code = "OBG",
            strongPhrases = listOf(
                "placenta previa", "abruptio placentae", "preeclampsia", "pre-eclampsia", "eclampsia",
                "postpartum hemorrhage", "postpartum haemorrhage", "lochia", "bishop score", "partograph",
                "apgar score", "ectopic pregnancy", "gestational diabetes", "amniotic fluid", "polyhydramnios",
                "oligohydramnios", "umbilical cord", "fundal height", "fetal heart", "leopold",
                "oxytocin", "magnesium sulfate", "mgso4", "hellp syndrome", "cervical dilation",
                "true labor", "false labor", "lochial", "puerperium", "lactation", "mastitis",
                "episiotomy", "cesarean", "caesarean", "hydatidiform mole", "hyperemesis gravidarum",
                "naegele", "gravida", "para", "antenatal", "intranatal", "postnatal", "meconium stained",
                "breech presentation", "pelvic inlet", "uterine atony", "rh incompatibility", "rhogam",
                "menstrual cycle", "endometriosis", "pcos", "fibroid", "cervical cancer", "pap smear"
            ),
            keywords = listOf(
                "pregnancy", "pregnant", "obstetric", "midwifery", "uterus", "uterine", "placenta",
                "fetus", "fetal", "trimester", "ovulation", "labor", "labour", "delivery", "cervix",
                "vaginal", "ovary", "fallopian", "contraception", "iucd", "copper-t", "abortion",
                "miscarriage", "gynecology", "gynaecology", "newborn delivery", "colostrum"
            )
        ),
        SubjectRule(
            code = "Peds",
            strongPhrases = listOf(
                "tetralogy of fallot", "wilms tumor", "kawasaki disease", "hirschsprung", "pyloric stenosis",
                "intussusception", "tracheoesophageal fistula", "neonatal jaundice", "phototherapy",
                "kernicterus", "kangaroo mother care", "kmc", "birth weight", "anterior fontanelle",
                "posterior fontanelle", "developmental milestone", "pincer grasp", "moro reflex",
                "rooting reflex", "babinski reflex", "kwashiorkor", "marasmus", "pem", "protein energy malnutrition",
                "nephrotic syndrome", "rheumatic fever", "jones criteria", "croup", "epiglottitis",
                "bronchiolitis", "hyaline membrane", "respiratory distress syndrome", "surfactant",
                "neural tube defect", "spina bifida", "hydrocephalus", "cleft lip", "cleft palate",
                "down syndrome", "pediatric dose", "clark's rule", "young's rule", "imnci",
                "umbilical stump", "ophthalmic neonatorum", "neonatal sepsis", "patent ductus arteriosus"
            ),
            keywords = listOf(
                "neonate", "neonatal", "infant", "toddler", "pediatric", "paediatric", "child",
                "children", "preterm", "newborn", "milestone", "weaning", "breastfeeding", "fontanelle",
                "congenital", "rickets", "scurvy", "measles", "mumps", "rubella", "pertussis"
            )
        ),
        SubjectRule(
            code = "Pharm",
            strongPhrases = listOf(
                "drug of choice", "antidote", "half-life", "half life", "pharmacokinetics",
                "pharmacodynamics", "first-pass metabolism", "first pass", "therapeutic index",
                "therapeutic range", "digoxin toxicity", "serum lithium", "warfarin", "heparin",
                "protamine sulfate", "vitamin k", "naloxone", "flumazenil", "n-acetylcysteine",
                "atropine", "pralidoxime", "beta blocker", "ace inhibitor", "calcium channel blocker",
                "loop diuretic", "furosemide", "spironolactone", "mannitol", "aminoglycoside",
                "vancomycin", "red man syndrome", "isoniazid", "rifampicin", "ethambutol", "pyrazinamide",
                "streptomycin", "metformin", "insulin", "phenytoin", "valproate", "carbamazepine",
                "adverse drug reaction", "side effect", "contraindication", "teratogenic", "ototoxicity",
                "nephrotoxicity", "extrapyramidal", "neuroleptic malignant", "serotonin syndrome",
                "anaphylaxis", "epinephrine", "adrenaline", "dopamine", "dobutamine", "noradrenaline",
                "nitroglycerin", "morphine", "paracetamol", "acetaminophen", "aspirin", "nsaid"
            ),
            keywords = listOf(
                "drug", "medication", "pharmacology", "antibiotic", "antihypertensive", "diuretic",
                "analgesic", "sedative", "anesthetic", "anaesthetic", "anticoagulant", "antiarrhythmic",
                "bronchodilator", "corticosteroid", "steroid", "agonist", "antagonist", "receptor",
                "dosage", "toxicity", "overdose", "infusion", "bolus", "synergism", "tachyphylaxis"
            )
        ),
        SubjectRule(
            code = "Psych",
            strongPhrases = listOf(
                "schizophrenia", "hallucination", "delusion", "bipolar disorder", "manic episode",
                "major depressive", "electroconvulsive therapy", "ect", "defense mechanism",
                "defence mechanism", "displacement", "projection", "sublimation", "rationalization",
                "reaction formation", "obsessive compulsive", "ocd", "panic disorder", "phobia",
                "post-traumatic stress", "ptsd", "anorexia nervosa", "bulimia nervosa", "alzheimer",
                "dementia", "delirium", "alcohol withdrawal", "delirium tremens", "wernicke",
                "korsakoff", "disulfiram", "suicide precaution", "therapeutic communication",
                "mental status examination", "mse", "flight of ideas", "neologism", "echolalia",
                "echopraxia", "catatonia", "waxy flexibility", "tardive dyskinesia", "akathisia",
                "freud", "erikson", "psychosexual", "psychosocial", "borderline personality",
                "antisocial personality", "somatoform", "conversion disorder", "milieu therapy"
            ),
            keywords = listOf(
                "psychiatric", "psychiatry", "mental health", "psychosis", "neurosis", "anxiety",
                "depression", "mania", "suicidal", "antipsychotic", "antidepressant", "ssri",
                "benzodiazepine", "haloperidol", "clozapine", "lithium", "counseling", "counselling",
                "behavior therapy", "cognitive therapy", "addiction", "substance abuse", "mood disorder"
            )
        ),
        SubjectRule(
            code = "CHN",
            strongPhrases = listOf(
                "primary health centre", "primary health center", "sub-centre", "sub centre", "phc", "chc",
                "asha worker", "anganwadi", "icds", "universal immunization", "cold chain", "vaccine vial monitor",
                "vvm", "ilr", "ice lined refrigerator", "bcg vaccine", "opv", "pentavalent", "rotavirus",
                "epidemiology", "incidence rate", "prevalence rate", "infant mortality rate", "maternal mortality",
                "case fatality rate", "herd immunity", "iceberg phenomenon", "epidemiological triad",
                "primary prevention", "secondary prevention", "tertiary prevention", "primordial prevention",
                "screening test", "sensitivity and specificity", "cohort study", "case control study",
                "randomized controlled", "rntcp", "ntep", "dots", "nvbdcp", "naco", "rmnch", "janani suraksha",
                "biomedical waste", "yellow bag", "red bag", "blue box", "white puncture proof",
                "chlorination", "horrocks", "orthotolidine", "slow sand filter", "sewage", "latrine",
                "demographic cycle", "couple protection rate", "pearl index", "world health organization",
                "alma ata", "bhore committee", "mudaliar committee", "kartar singh", "shrivastav"
            ),
            keywords = listOf(
                "community", "vaccine", "vaccination", "immunization", "immunisation", "epidemic",
                "endemic", "pandemic", "quarantine", "isolation", "surveillance", "vector", "mosquito",
                "anopheles", "aedes", "culex", "malaria", "dengue", "filariasis", "kala azar", "leprosy",
                "tuberculosis", "sanitation", "water purification", "family planning", "demography",
                "census", "nutrition program", "mid day meal", "occupational health", "silicosis", "byssinosis"
            )
        ),
        SubjectRule(
            code = "FON",
            strongPhrases = listOf(
                "hand hygiene", "five moments", "sterile technique", "surgical asepsis", "medical asepsis",
                "foley catheter", "urinary catheter", "nasogastric tube", "ng tube", "ryles tube",
                "gastric lavage", "enema", "pressure ulcer", "bed sore", "braden scale", "norton scale",
                "fowler's position", "semi-fowler", "trendelenburg", "sims position", "lithotomy position",
                "knee-chest position", "dorsal recumbent", "intramuscular injection", "z-track",
                "subcutaneous injection", "intradermal injection", "vastus lateralis", "ventrogluteal",
                "deltoid site", "vital signs", "blood pressure cuff", "korotkoff", "pulse oximetry",
                "cheyne-stokes", "kussmaul", "biot's respiration", "triage", "cpr", "bls",
                "chest compression", "heimlich", "oxygen therapy", "nasal cannula", "venturi mask",
                "non-rebreather", "tracheostomy suctioning", "endotracheal suction", "blood transfusion",
                "transfusion reaction", "iv cannula", "cannula gauge", "infiltration", "phlebitis",
                "nursing process", "nanda", "maslow", "florence nightingale", "virginia henderson",
                "orem", "roy adaptation", "sbar", "documentation", "informed consent", "autoclave",
                "disinfection", "sterilization", "glutaraldehyde", "cidex", "sodium hypochlorite"
            ),
            keywords = listOf(
                "catheterization", "catheter", "dressing", "bandage", "wound care", "position",
                "injection", "syringe", "needle", "gauge", "bed bath", "oral care", "back care",
                "enema", "sitz bath", "hot application", "cold application", "asepsis", "ppe",
                "donning", "doffing", "needle stick", "sharps", "nursing diagnosis", "bedside"
            )
        ),
        SubjectRule(
            code = "Anat",
            strongPhrases = listOf(
                "cranial nerve", "brachial plexus", "circle of willis", "foramen magnum", "skull sutures",
                "coronal suture", "sagittal suture", "lambdoid", "sarcomere", "actin and myosin",
                "neuromuscular junction", "action potential", "sinoatrial node", "sa node", "av node",
                "bundle of his", "purkinje fibers", "cardiac cycle", "frank-starling", "baroreceptor",
                "nephron", "loop of henle", "glomerular filtration", "juxtaglomerular", "raas",
                "renin angiotensin", "alveoli", "tidal volume", "vital capacity", "residual volume",
                "oxygen dissociation", "bohr effect", "anterior pituitary", "posterior pituitary",
                "hypothalamus", "islets of langerhans", "adrenal cortex", "thyroid gland", "parathyroid",
                "portal vein", "hepatic lobule", "biliary tree", "sphincter of oddi", "peyer's patches",
                "erythropoiesis", "clotting cascade", "coagulation factor", "lymphatic drainage",
                "thoracic duct", "synovial joint", "ball and socket", "hinge joint", "mitosis", "meiosis",
                "mitochondria", "ribosome", "golgi apparatus", "endoplasmic reticulum", "dna replication"
            ),
            keywords = listOf(
                "anatomy", "physiology", "bone", "muscle", "nerve", "artery", "vein", "capillary",
                "ventricle", "atrium", "valve", "mitral", "tricuspid", "aorta", "larynx", "pharynx",
                "trachea", "bronchus", "diaphragm", "stomach", "duodenum", "jejunum", "ileum",
                "colon", "pancreas", "spleen", "kidney", "ureter", "bladder", "cerebrum", "cerebellum",
                "medulla", "pons", "spinal cord", "hormone", "enzyme", "tissue", "epithelium", "cartilage"
            )
        ),
        SubjectRule(
            code = "MSN",
            strongPhrases = listOf(
                "myocardial infarction", "angina pectoris", "heart failure", "atrial fibrillation",
                "ventricular fibrillation", "ecg", "st elevation", "troponin", "hypertensive crisis",
                "deep vein thrombosis", "dvt", "pulmonary embolism", "copd", "asthma", "pneumonia",
                "pleural effusion", "pneumothorax", "chest tube", "water seal drainage", "ards",
                "diabetic ketoacidosis", "dka", "hhnk", "hhs", "hypoglycemia", "cushing syndrome",
                "addison disease", "pheochromocytoma", "thyroid storm", "myxedema coma", "siadh",
                "diabetes insipidus", "chronic kidney disease", "ckd", "acute kidney injury", "hemodialysis",
                "peritoneal dialysis", "av fistula", "cirrhosis", "hepatic encephalopathy", "ascites",
                "paracentesis", "esophageal varices", "pancreatitis", "cullen sign", "grey turner",
                "cholecystitis", "murphy sign", "appendicitis", "mcburney", "peptic ulcer", "crohn",
                "ulcerative colitis", "glasgow coma scale", "gcs", "increased icp", "cushing triad",
                "meningitis", "kernig sign", "brudzinski", "guillain barre", "myasthenia gravis",
                "parkinson", "stroke", "cva", "seizure", "status epilepticus", "parkland formula",
                "rule of nines", "burn injury", "compartment syndrome", "fat embolism", "osteomyelitis",
                "rheumatoid arthritis", "gout", "glaucoma", "cataract", "retinal detachment", "meniere",
                "chemotherapy", "tumor lysis", "neutropenia", "blood gas", "abg", "metabolic acidosis",
                "respiratory acidosis", "metabolic alkalosis", "hyperkalemia", "hypokalemia", "hyponatremia",
                "hypocalcemia", "chvostek", "trousseau", "septic shock", "hypovolemic shock", "cardiogenic shock"
            ),
            keywords = listOf(
                "surgery", "postoperative", "post-operative", "preoperative", "patient", "shock",
                "hemorrhage", "haemorrhage", "infarction", "ischemia", "embolism", "thrombosis",
                "fracture", "traction", "cast", "burn", "dialysis", "ventilator", "intubation",
                "anemia", "anaemia", "leukemia", "lymphoma", "cancer", "carcinoma", "ulcer", "hernia",
                "calculi", "stones", "electrolyte", "acidosis", "alkalosis", "edema", "oedema", "cyanosis"
            )
        ),
        SubjectRule(
            code = "Admin",
            strongPhrases = listOf(
                "nursing research", "nursing management", "nursing administration", "literature review",
                "research design", "null hypothesis", "alternative hypothesis", "sampling technique",
                "stratified sampling", "cluster sampling", "snowball sampling", "purposive sampling",
                "validity and reliability", "cronbach", "pilot study", "p-value", "chi-square",
                "anova", "t-test", "standard deviation", "mean median mode", "correlation coefficient",
                "posdcorb", "henri fayol", "max weber", "leadership style", "autocratic", "democratic",
                "laissez-faire", "transformational leadership", "delegation", "span of control",
                "unity of command", "scalar chain", "inventory control", "abc analysis", "ved analysis",
                "nursing audit", "quality assurance", "accreditation", "nabh", "patient ratio",
                "indian nursing council", "inc act", "code of ethics", "negligence", "malpractice",
                "beneficence", "non-maleficence", "autonomy", "fidelity", "veracity", "budgeting",
                "performance appraisal", "gantt chart", "pert", "swot analysis", "in-service education"
            ),
            keywords = listOf(
                "research", "hypothesis", "variable", "sample", "questionnaire", "interview",
                "ethics", "ethical", "legal", "tort", "consent", "management", "supervision",
                "staffing", "budget", "inventory", "audit", "leadership", "curriculum", "pedagogy",
                "lesson plan", "av aids", "microteaching", "evaluation"
            )
        ),
        SubjectRule(
            code = "Health-Prog",
            strongPhrases = listOf(
                "ayushman bharat", "pm-jay", "pmjay", "nhm", "national health mission", "mission indradhanush",
                "poshan abhiyaan", "swachh bharat", "nikshay", "laqshya", "kayakalp", "suman scheme",
                "pradhan mantri surakshit matritva", "pmsma", "janani shishu suraksha", "jssk",
                "rashtriya bal swasthya", "rbsk", "rashtriya kishor", "rksk", "npcdcs", "nbcp",
                "national mental health programme", "national leprosy", "national malaria", "pulse polio",
                "health and wellness centre", "hwc", "ayushman arogya mandir", "e-sanjeevani", "abha id"
            ),
            keywords = listOf(
                "yojana", "abhiyaan", "national programme", "national program", "health scheme",
                "uip", "immunization drive", "ministry of health"
            )
        ),
        SubjectRule(
            code = "Railways",
            strongPhrases = listOf(
                "indian railways", "railway zone", "railway budget", "vande bharat", "kavach system",
                "dfccil", "dedicated freight corridor", "konkan railway", "metro rail", "bullet train",
                "railway board", "locomotive", "chittaranjan", "integral coach factory", "rdso",
                "rrb", "railway minister", "broad gauge", "meter gauge"
            ),
            keywords = listOf(
                "railway", "train", "locomotive", "station", "gauge", "express"
            )
        ),
        SubjectRule(
            code = "CA-2026",
            strongPhrases = listOf(
                "2025", "2026", "union budget", "padma vibhushan", "padma bhushan", "bharat ratna",
                "nobel prize", "g20 summit", "brics summit", "cop29", "cop30", "isro mission",
                "gaganyaan", "chandrayaan", "aditya-l1", "olympics", "world cup", "chief justice",
                "president of india", "prime minister", "who theme", "world health day"
            ),
            keywords = listOf(
                "current affairs", "summit", "award", "launched in", "appointed as", "recently"
            )
        ),
        SubjectRule(
            code = "Static-GK",
            strongPhrases = listOf(
                "constitution of india", "fundamental rights", "fundamental duties", "directive principles",
                "article ", "amendment", "lok sabha", "rajya sabha", "supreme court", "panchayati raj",
                "indus valley", "mughal empire", "indian national congress", "quit india", "dandi march",
                "tropic of cancer", "himalayas", "western ghats", "national park", "biosphere reserve",
                "classical dance", "bharatanatyam", "kathakali", "capital of", "currency of",
                "headquarters of", "united nations", "unesco", "unicef"
            ),
            keywords = listOf(
                "polity", "constitution", "parliament", "governor", "river", "mountain", "dynasty",
                "freedom struggle", "geography", "history", "capitals", "monument", "festival"
            )
        ),
        SubjectRule(
            code = "Science",
            strongPhrases = listOf(
                "newton's law", "ohm's law", "boyle's law", "charles law", "archimedes principle",
                "periodic table", "atomic number", "valency", "covalent bond", "ionic bond",
                "ph value", "acid and base", "photosynthesis", "chlorophyll", "xylem", "phloem",
                "refraction", "reflection", "convex lens", "concave mirror", "focal length",
                "electric current", "resistance", "magnetic field", "sound wave", "si unit",
                "gram positive", "gram negative", "bacteriophage", "virus", "fungi", "protozoa"
            ),
            keywords = listOf(
                "physics", "chemistry", "velocity", "acceleration", "momentum", "wavelength",
                "frequency", "isotope", "catalyst", "oxidation", "reduction", "molecule", "compound",
                "microbiology", "bacteria", "sterilization", "immunity", "antibody", "antigen"
            )
        ),
        SubjectRule(
            code = "Math",
            strongPhrases = listOf(
                "simple interest", "compound interest", "profit and loss", "percentage", "ratio and proportion",
                "time and work", "speed and distance", "upstream", "downstream", "average of",
                "lcm", "hcf", "square root", "cube root", "quadratic", "probability", "permutation",
                "combination", "area of triangle", "volume of cylinder", "drip rate", "drops per minute",
                "ml/hr", "mg/kg", "flow rate"
            ),
            keywords = listOf(
                "calculate", "find the value", "solve for", "sum of", "divided by", "multiplied",
                "fraction", "decimal", "percent", "algebra", "geometry", "trigonometry", "arithmetic"
            )
        ),
        SubjectRule(
            code = "Reasoning",
            strongPhrases = listOf(
                "coding decoding", "blood relation", "number series", "letter series", "missing number",
                "syllogism", "venn diagram", "seating arrangement", "mirror image", "water image",
                "odd one out", "analogy", "direction sense", "statement and conclusion",
                "find the next term", "in a certain code", "pointing to a photograph"
            ),
            keywords = listOf(
                "series", "analogy", "coded as", "brother of", "sister of", "north", "south",
                "east", "west", "clockwise", "anticlockwise", "sequence", "pattern"
            )
        )
    )

    private val canonicalCodesByLower: Map<String, String> =
        SubjectCatalog.ALL_SUBJS.associate { it.code.lowercase(Locale.US) to it.code } + mapOf(
            "medical-surgical" to "MSN",
            "medical surgical" to "MSN",
            "medsurg" to "MSN",
            "pharmacology" to "Pharm",
            "obgyn" to "OBG",
            "midwifery" to "OBG",
            "pediatrics" to "Peds",
            "paediatrics" to "Peds",
            "child health" to "Peds",
            "community" to "CHN",
            "community health" to "CHN",
            "psychiatry" to "Psych",
            "mental health" to "Psych",
            "psy" to "Psych",
            "fundamentals" to "FON",
            "foundation" to "FON",
            "anatomy" to "Anat",
            "physiology" to "Anat",
            "research" to "Admin",
            "management" to "Admin",
            "administration" to "Admin",
            "gk" to "Static-GK",
            "ca" to "CA-2026",
            "sci" to "Science",
            "maths" to "Math",
            "aptitude" to "Math",
            "reas" to "Reasoning"
        )

    fun resolveSubjectAlias(raw: String?): String? {
        val clean = raw?.trim()?.lowercase(Locale.US).orEmpty()
        if (clean.isBlank()) return null
        return canonicalCodesByLower[clean]
    }

    /**
     * Classifies a single question into one of ClinEdge's 16 canonical subjects.
     * Priority:
     * 1. Explicit inline tag (`Subject: OBG`) if found in stem or explanation
     * 2. Weighted phrase (+4) and keyword (+1.5) match across stem, options, and explanation
     * 3. [fallbackSubject] (defaults to "MSN")
     */
    fun classifyQuestion(
        question: Question,
        fallbackSubject: String = "MSN"
    ): String {
        val combined = buildString {
            append(question.questionText)
            append(' ')
            question.options.forEach { append(it).append(' ') }
            if (question.explanation.isNotBlank()) {
                append(question.explanation)
            }
        }

        // 1. Explicit Subject tag inside text
        explicitSubjectTagRegex.find(combined)?.let { match ->
            resolveSubjectAlias(match.groupValues[1])?.let { return it }
        }

        val lower = combined.lowercase(Locale.US)
        var bestCode = fallbackSubject
        var bestScore = 0.0

        for (rule in rules) {
            var score = 0.0
            for (phrase in rule.strongPhrases) {
                if (lower.contains(phrase)) {
                    score += 4.0
                }
            }
            for (kw in rule.keywords) {
                if (lower.contains(kw)) {
                    score += 1.5
                }
            }
            if (score > bestScore) {
                bestScore = score
                bestCode = rule.code
            }
        }

        return if (bestScore >= 1.5) bestCode else fallbackSubject
    }

    /**
     * Scans every question and updates its [Question.subject] and [Question.topicId]
     * to match the detected clinical/non-tech subject.
     */
    fun autoDistributeBySubject(
        questions: List<Question>,
        fallbackSubject: String = "MSN"
    ): List<Question> {
        return questions.map { q ->
            val detected = classifyQuestion(q, fallbackSubject = fallbackSubject)
            q.copy(
                subject = detected,
                topicId = PlatformCatalogDefaults.defaultTopicIdFor(detected)
            )
        }
    }

    /**
     * Returns subject distribution sorted by count descending for live Admin preview summary.
     */
    fun summarizeSubjectDistribution(questions: List<Question>): List<Pair<String, Int>> {
        return questions
            .groupingBy { it.subject.ifBlank { "MSN" } }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }
    }
}
