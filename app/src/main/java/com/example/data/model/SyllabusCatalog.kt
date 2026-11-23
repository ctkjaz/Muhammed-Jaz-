package com.example.data.model

import java.util.Locale

data class ChapterMeta(
    val id: String,
    val subjectCode: String,
    val unitNumber: Int,
    val title: String,
    val subtitle: String,
    val topicSlugs: Set<String> = emptySet(),
    val keywords: List<String> = emptyList()
)

data class TopicBucket(
    val id: String,
    val label: String,
    val items: List<QuestionItem>,
    val highYieldCount: Int,
    val masteredCount: Int,
    val attemptedCount: Int
)

data class ChapterBucket(
    val meta: ChapterMeta,
    val items: List<QuestionItem>,
    val topics: List<TopicBucket>,
    val highYieldCount: Int,
    val masteredCount: Int,
    val attemptedCount: Int,
    val accuracyPercent: Int
)

/**
 * Indian Nursing Council (INC) syllabus-aligned Chapter (Unit) & Topic hierarchy
 * across all 16 Nursing & General subjects, with deterministic High-Yield classification.
 */
object SyllabusCatalog {

    private fun ch(
        subject: String,
        unit: Int,
        id: String,
        title: String,
        subtitle: String,
        slugs: List<String>,
        keywords: List<String>
    ) = ChapterMeta(
        id = "${subject.lowercase(Locale.US)}_$id",
        subjectCode = subject,
        unitNumber = unit,
        title = title,
        subtitle = subtitle,
        topicSlugs = slugs.map { normalizeSlug(it) }.toSet(),
        keywords = keywords.map { it.lowercase(Locale.US) }
    )

    private val chaptersBySubject: Map<String, List<ChapterMeta>> = mapOf(
        "MSN" to listOf(
            ch("MSN", 1, "cardio_heme", "Cardiovascular & Hematology", "ACS, heart failure, ECG, arrhythmias, anemias & shock",
                listOf("cardiology", "hematology", "immunology"),
                listOf("ecg", "myocardial", "cardiac", "heart", "hypertension", "angina", "anemia", "leukemia", "platelet", "thrombo", "aortic", "mitral", "valve", "shock", "dic", "transfusion")),
            ch("MSN", 2, "resp_abg", "Respiratory, Pulmonology & ABG", "COPD, asthma, pneumonia, ARDS, TB & acid-base balance",
                listOf("pulmonology", "abg_interpretation", "abg_acid_base"),
                listOf("abg", "respiratory", "asthma", "copd", "pneumonia", "tuberculosis", "pleural", "pneumothorax", "ventilator", "arterial blood gas", "alkalosis", "acidosis", "oxygen", "tracheostomy")),
            ch("MSN", 3, "neuro_sensory", "Neurology, ENT & Ophthalmology", "Stroke, ICP, seizures, meningitis, glaucoma & sensory care",
                listOf("neurology", "ophthalmology", "ent"),
                listOf("stroke", "seizure", "meningitis", "icp", "gcs", "glasgow", "parkinson", "myasthenia", "guillain", "glaucoma", "cataract", "retinal", "otitis", "meniere", "cranial nerve")),
            ch("MSN", 4, "fluid_renal", "Fluid, Electrolytes & Nephrology", "AKI, CKD, dialysis, hyperkalemia, sodium & calcium shifts",
                listOf("fluid_electrolytes", "nephrology"),
                listOf("potassium", "sodium", "calcium", "magnesium", "renal", "kidney", "dialysis", "nephrotic", "glomerulo", "creatinine", "oliguria", "hyperkalemia", "hyponatremia", "chvostek", "trousseau")),
            ch("MSN", 5, "gi_endo", "Gastroenterology & Endocrinology", "Cirrhosis, pancreatitis, DKA, thyroid & adrenal crises",
                listOf("gastroenterology", "endocrinology"),
                listOf("liver", "cirrhosis", "pancreatitis", "peptic", "appendicitis", "cholecystitis", "diabetes", "dka", "insulin", "thyroid", "cushing", "addison", "pheochromocytoma", "siadh")),
            ch("MSN", 6, "critical_burns_trauma", "Critical Care, Burns & Trauma", "Parkland formula, triage, sepsis, CPR & shock resuscitation",
                listOf("critical_care", "critical_care_msn", "burns", "trauma"),
                listOf("burn", "parkland", "rule of nine", "sepsis", "cpr", "triage", "trauma", "icu", "code blue", "defibrill", "anaphylaxis", "compartment")),
            ch("MSN", 7, "onco_ortho_inf", "Oncology, Orthopedics, Derm & Infectious", "Chemo safety, fractures, traction, skin & communicable care",
                listOf("oncology", "orthopedics", "dermatology", "infectious_disease", "core_msn"),
                listOf("cancer", "tumor", "chemo", "radiation", "fracture", "traction", "cast", "arthritis", "psoriasis", "pressure ulcer", "hiv", "dengue", "malaria", "typhoid"))
        ),
        "Pharm" to listOf(
            ch("Pharm", 1, "classification", "Drug Classifications & Autonomic", "Receptor pharmacology, CNS, CVS & antimicrobial classes",
                listOf("drug_classification", "core_pharm"),
                listOf("class", "mechanism", "beta blocker", "ace inhibitor", "diuretic", "antibiotic", "cholinergic", "adrenergic", "receptor", "agonist", "antagonist")),
            ch("Pharm", 2, "indications", "Clinical Indications & Drugs of Choice", "First-line therapies across emergency, ICU & chronic care",
                listOf("clinical_indications"),
                listOf("drug of choice", "first-line", "first line", "indicated", "treatment of", "prophylaxis", "therapy for")),
            ch("Pharm", 3, "antidotes_monitoring", "Antidotes & Therapeutic Monitoring", "Toxicity reversal, serum levels, INR, lithium & digoxin",
                listOf("antidotes_monitoring", "high_alert_pharm"),
                listOf("antidote", "toxicity", "overdose", "serum level", "digoxin", "lithium", "warfarin", "heparin", "protamine", "naloxone", "flumazenil", "atropine", "acetylcysteine", "inr", "aptt")),
            ch("Pharm", 4, "adverse_effects", "Adverse Drug Reactions & Contraindications", "Black-box warnings, teratogens & organ toxicities",
                listOf("adverse_effects"),
                listOf("adverse", "side effect", "contraindicat", "ototox", "nephrotox", "red man", "stevens-johnson", "teratogen", "hypokalemia", "extrapyramidal")),
            ch("Pharm", 5, "safe_admin", "Safe Medication Administration", "High-alert IV infusions, routes, dilution & 10 rights",
                listOf("safe_administration"),
                listOf("administer", "infusion", "bolus", "dilut", "subcutaneous", "intramuscular", "z-track", "compatibility", "rights of medication"))
        ),
        "OBG" to listOf(
            ch("OBG", 1, "anatomy_physio", "Reproductive Anatomy & Obstetric Physiology", "Pelvic diameters, placenta, fetal circulation & early pregnancy",
                listOf("obstetric_anatomy", "obstetric_physiology", "reproductive_physiology", "early_pregnancy"),
                listOf("pelvis", "placenta", "umbilical", "fetal", "ductus", "foramen", "hcg", "implantation", "amniotic", "menstrual", "ovulation", "conjugate")),
            ch("OBG", 2, "antenatal", "Antenatal Care & Fetal Surveillance", "ANC visits, Leopold maneuvers, NST/CST, BPP & diagnostics",
                listOf("antenatal_care", "antenatal_assessment", "fetal_surveillance", "prenatal_diagnostics"),
                listOf("antenatal", "leopold", "nst", "non-stress", "biophysical", "fundal", "quickening", "chadwick", "hegar", "amniocentesis", "naegele", "edd")),
            ch("OBG", 3, "high_risk_htn_aph", "High-Risk Pregnancy, PIH & APH", "Preeclampsia, eclampsia, MgSO4, placenta previa & abruption",
                listOf("high_risk_pregnancy", "hypertensive_disorders", "antepartum_hemorrhage", "amniotic_fluid_disorders", "medical_disorders_in_pregnancy"),
                listOf("eclampsia", "preeclampsia", "hellp", "magnesium sulfate", "mgso4", "placenta previa", "abruptio", "polyhydramnios", "oligohydramnios", "gestational diabetes", "rh incompatibility")),
            ch("OBG", 4, "normal_intrapartum", "Normal Labor & Intrapartum Care", "Stages of labor, partograph, cardinal movements & induction",
                listOf("normal_labor", "intrapartum_care", "intrapartum_assessment", "labor_induction", "operative_obstetrics"),
                listOf("labor", "labour", "partograph", "bishop", "oxytocin", "cervical dilation", "effacement", "station", "episiotomy", "cesarean", "forceps", "vacuum", "cardinal")),
            ch("OBG", 5, "obg_emergencies", "Obstetric Emergencies & Malpresentation", "PPH, cord prolapse, shoulder dystocia, uterine rupture & breech",
                listOf("obstetric_emergency", "obstetric_emergencies", "intrapartum_complications", "malpresentation"),
                listOf("pph", "postpartum hemorrhage", "cord prolapse", "shoulder dystocia", "uterine rupture", "breech", "inversion", "atony", "lochia", "emergency")),
            ch("OBG", 6, "postpartum_gyn_fp", "Postpartum, Gynecology, Oncology & Contraception", "Puerperium, lactation, family planning, STIs & gynecologic care",
                listOf("postpartum_care", "gynecology", "gynecologic_oncology", "contraception", "infectious_obg", "obstetric_ethics_law", "core_obg"),
                listOf("postpartum", "puerperium", "lochia", "breastfeed", "mastitis", "contracep", "iucd", "copper-t", "mtp", "pcpndt", "pap smear", "cervical cancer", "fibroid"))
        ),
        "Peds" to listOf(
            ch("Peds", 1, "neonatology", "Neonatology, APGAR & Newborn Resuscitation", "APGAR, Ballard, neonatal reflexes, jaundice, RDS & KMC",
                listOf("neonatology", "apgar_score"),
                listOf("neonate", "newborn", "apgar", "moro", "rooting", "babinski", "phototherapy", "kernicterus", "kmc", "kangaroo", "surfactant", "rds", "meconium", "umbilical")),
            ch("Peds", 2, "growth_uip_nutr", "Growth, Development, Nutrition & UIP", "Milestones, anthropometry, SAM/PEM, IMNCI & immunization",
                listOf("growth_development", "immunization_uip", "pediatric_nutrition", "pediatric_public_health"),
                listOf("milestone", "growth", "fontanelle", "vaccine", "bcg", "opv", "pentavalent", "measles", "rotavirus", "kwashiorkor", "marasmus", "sam", "imnci", "teething")),
            ch("Peds", 3, "cardio_resp_critical", "Pediatric Cardio-Respiratory & Critical Care", "CHD (TOF/PDA/VSD), croup, epiglottitis, bronchiolitis & PALS",
                listOf("pediatric_cardiology", "pediatric_respiratory", "pediatric_critical_care"),
                listOf("tetralogy", "fallot", "pda", "vsd", "asd", "cyanotic", "croup", "epiglottitis", "bronchiolitis", "pals", "pediatric shock", "asthma")),
            ch("Peds", 4, "gi_renal_surg", "Pediatric GI, Renal, Surgery & Fluid Math", "ORS, pyloric stenosis, intussusception, nephrotic & Holliday-Segar",
                listOf("pediatric_gi", "pediatric_renal", "pediatric_surgery", "fluid_calculations", "pediatric_calculations"),
                listOf("pyloric", "intussusception", "hirschsprung", "tracheoesophageal", "ors", "dehydration", "nephrotic", "wilms", "holliday", "segar", "fluid", "dose", "mg/kg")),
            ch("Peds", 5, "neuro_heme_inf", "Pediatric Neuro, Heme-Onco, Infectious & Special", "Febrile seizures, hydrocephalus, thalassemia, exanthems & safety",
                listOf("pediatric_neurology", "pediatric_hematology", "pediatric_oncology", "pediatric_infectious", "pediatric_endocrinology", "pediatric_genetics", "pediatric_orthopedics", "pediatric_psychiatry", "pediatric_safety", "pediatric_toxicology", "pediatric_nursing", "core_pediatrics"),
                listOf("hydrocephalus", "spina bifida", "febrile seizure", "cerebral palsy", "thalassemia", "sickle cell", "hemophilia", "kawasaki", "measles", "mumps", "rubella", "varicella", "diphtheria", "pertussis", "down syndrome"))
        ),
        "CHN" to listOf(
            ch("CHN", 1, "epidemiology_bio", "Epidemiology, Screening & Biostatistics", "Study designs, sensitivity/specificity, incidence, prevalence & triage",
                listOf("epidemiology_prevention", "epidemiological_studies", "biostatistics_epidemiology", "screening_biostatistics", "epidemiology_cold_chain"),
                listOf("epidemiology", "incidence", "prevalence", "cohort", "case-control", "sensitivity", "specificity", "iceberg", "prevention", "primordial", "quarantine", "isolation")),
            ch("CHN", 2, "communicable_uip", "Communicable Diseases, Vectors & Cold Chain", "UIP, VVM, ILR, NTEP, malaria, dengue, rabies & zoonoses",
                listOf("communicable_diseases", "communicable_disease_control", "immunization_cold_chain", "vector_borne_diseases", "water_borne_diseases", "zoonotic_diseases", "entomology", "infection_control"),
                listOf("cold chain", "ilr", "vvm", "vaccine", "aedes", "anopheles", "culex", "malaria", "dengue", "filaria", "rabies", "leprosy", "tuberculosis", "dots", "ntep", "cholera", "polio")),
            ch("CHN", 3, "mch_demography_nutr", "MCH, Demography, Family Planning & Nutrition", "IMR, MMR, Pearl index, RMNCH+A, ICDS & micronutrient drives",
                listOf("maternal_child_health", "demography_family_planning", "demography_biostatistics", "health_indicators", "community_nutrition"),
                listOf("imr", "mmr", "tfr", "pearl index", "demography", "census", "asha", "anganwadi", "icds", "jsy", "jssk", "vitamin a", "iodine", "mid-day meal")),
            ch("CHN", 4, "env_bmw_occ", "Environment, Sanitation, BMW & Occupational", "Color-coded BMW bins, chlorination, Horrocks & pneumoconiosis",
                listOf("biomedical_waste_management", "environmental_health", "environmental_sanitation", "occupational_health"),
                listOf("biomedical waste", "bmw", "yellow bag", "red bag", "chlorin", "horrocks", "orthotolidine", "silicosis", "byssinosis", "bagassosis", "asbestosis", "lead poisoning")),
            ch("CHN", 5, "health_system_ncd", "Health System, Planning, NCDs & Disaster", "SC/PHC/CHC norms, committees, WHO/UNICEF, NP-NCD & disaster triage",
                listOf("healthcare_delivery_system", "health_planning", "international_health", "non_communicable_diseases", "disaster_nursing", "community_emergencies", "community_nursing_practice", "community_specialties", "health_communication", "social_behavioral_health", "social_health_ethics", "core_chn"),
                listOf("phc", "chc", "sub-centre", "bhore", "mudaliar", "kartar", "who", "unicef", "ayushman", "hwc", "disaster", "triage", "ncd", "hypertension"))
        ),
        "Psych" to listOf(
            ch("Psych", 1, "foundations_mse", "Foundations, Neurobiology & MSE", "Defense mechanisms, neurotransmitters, psychopathology & assessment",
                listOf("psychodynamic_foundations", "neurobiology", "psychopathology", "psychiatric_assessment", "therapeutic_relationships"),
                listOf("defense", "freud", "erikson", "dopamine", "serotonin", "gaba", "mse", "hallucination", "delusion", "illusion", "flight of ideas", "neologism", "therapeutic communication")),
            ch("Psych", 2, "psychotic_mood", "Psychotic & Mood Disorders", "Schizophrenia, bipolar disorder, depression & mania",
                listOf("psychotic_disorders", "mood_disorders"),
                listOf("schizophrenia", "bleuler", "schneider", "bipolar", "mania", "depression", "dysthymia", "catatonic", "suicide")),
            ch("Psych", 3, "anxiety_trauma_person", "Anxiety, OCD, Trauma, Somatic & Personality", "Phobias, PTSD, dissociative, somatoform & cluster A/B/C disorders",
                listOf("anxiety_disorders", "anxiety_related_disorders", "trauma_stressor_disorders", "dissociative_disorders", "somatic_disorders", "personality_disorders", "stress_coping", "grief_loss"),
                listOf("anxiety", "panic", "phobia", "ocd", "obsession", "ptsd", "dissociat", "conversion", "somatoform", "borderline", "antisocial", "kubler-ross", "grief")),
            ch("Psych", 4, "substance_neurocog_child", "Substance Use, Neurocognitive, Eating & Child Psych", "Alcohol withdrawal, delirium, dementia, anorexia & ADHD/autism",
                listOf("substance_use_disorders", "neurocognitive_disorders", "eating_disorders", "child_adolescent_psychiatry", "sleep_behavioral_health"),
                listOf("alcohol", "delirium tremens", "wernicke", "korsakoff", "disulfiram", "opioid", "dementia", "alzheimer", "anorexia", "bulimia", "autism", "adhd", "enuresis")),
            ch("Psych", 5, "therapies_emerg_legal", "Psychopharmacology, ECT, Emergencies & MHCA 2017", "Antipsychotics, EPS, NMS, lithium, ECT, CBT & Mental Healthcare Act",
                listOf("psychopharmacology", "somatic_therapies", "psychosocial_therapies", "psychiatric_emergencies", "psychiatric_critical_care", "psychiatric_legal_ethics", "community_psychiatry", "professional_practice", "special_topics", "core_psychiatry"),
                listOf("ect", "electroconvulsive", "lithium", "haloperidol", "clozapine", "ssri", "maoi", "nms", "extrapyramidal", "tardive", "akathisia", "cbt", "mhca", "mental healthcare act"))
        ),
        "FON" to listOf(
            ch("FON", 1, "foundations_ethics_safety", "Nursing Process, Ethics, Legal & Patient Safety", "NANDA, Maslow, tort law, consent, ergonomics & fall prevention",
                listOf("nursing_foundations", "legal_ethical_nursing", "patient_safety", "professional_practice", "end_of_life_care", "core_fon"),
                listOf("nursing process", "maslow", "nightingale", "consent", "negligence", "malpractice", "battery", "assault", "autonomy", "beneficence", "hospice", "rigor mortis")),
            ch("FON", 2, "vitals_assessment_pos", "Vital Signs, Physical Assessment & Positions", "Thermometry, pulse, BP cuff sizing, inspection & therapeutic positions",
                listOf("vital_signs_monitoring", "physical_assessment", "therapeutic_positions", "comfort_mobility", "hygiene_daily_care"),
                listOf("temperature", "pulse", "korotkoff", "blood pressure", "spo2", "fowler", "trendelenburg", "sims", "lithotomy", "knee-chest", "orthopneic", "auscultat")),
            ch("FON", 3, "infection_wound_surg", "Infection Control, Wound Care & Perioperative", "Hand hygiene, autoclaving, isolation, Braden scale & dressings",
                listOf("infection_control", "skin_integrity_wound_care", "surgical_fundamentals", "orthopedic_fundamentals"),
                listOf("autoclave", "steriliz", "hand hygiene", "ppe", "isolation", "airborne", "droplet", "braden", "pressure injury", "bedsore", "dehiscence", "evisceration", "suture")),
            ch("FON", 4, "iv_blood_meds", "IV Therapy, Blood Transfusion & Medication Safety", "Cannula gauges, infiltration, phlebitis, blood reactions & routes",
                listOf("iv_therapy_vascular_access", "blood_fluid_therapy", "medication_safety", "nutrition_fluid_balance"),
                listOf("cannula", "gauge", "phlebitis", "infiltration", "extravasation", "transfusion", "packed rbc", "tpn", "central line", "intradermal", "z-track", "nasogastric")),
            ch("FON", 5, "clinical_procedures", "Airway, GI, Renal, Diagnostics & Emergency", "Suctioning, NG tube, enema, Foley catheter, specimen & CPR",
                listOf("airway_respiratory_care", "gi_procedures", "renal_elimination_procedures", "diagnostic_procedures", "diagnostics_specimens", "cardiac_monitoring", "critical_care_fundamentals", "emergency_fundamentals", "clinical_procedures"),
                listOf("suction", "oxygen", "venturi", "nasogastric", "ryle", "enema", "catheter", "foley", "urine", "sputum", "lumbar puncture", "paracentesis", "thoracentesis", "cpr"))
        ),
        "Anat" to listOf(
            ch("Anat", 1, "cell_hist_msk", "Cell Biology, Histology, Integument & Musculoskeletal", "Organelles, tissues, bones, joints, muscles & skin layers",
                listOf("cellular_physiology", "cellular_immunology", "histology", "integumentary", "musculoskeletal", "general_anatomy", "general_physiology", "developmental_anatomy", "core_anatomy"),
                listOf("mitochondria", "lysosome", "epithelium", "bone", "vertebra", "femur", "cranial", "joint", "sarcomere", "epidermis", "dermis", "muscle")),
            ch("Anat", 2, "cvs_heme_resp", "Cardiovascular, Hematology & Respiratory Anatomy", "Heart valves, conduction system, blood groups, alveoli & spirometry",
                listOf("cardiovascular", "hematology_immunology", "respiratory", "thoracic_anatomy", "respiratory_gi", "applied_anatomy"),
                listOf("heart", "sa node", "av node", "aorta", "coronary", "hemoglobin", "erythrocyte", "platelet", "alveoli", "surfactant", "tidal volume", "vital capacity", "pleura")),
            ch("Anat", 3, "neuro_senses", "Neuroanatomy, Neurophysiology & Special Senses", "Brain lobes, cranial nerves, spinal tracts, eye & ear",
                listOf("neuroanatomy", "neurophysiology", "special_senses", "endocrine_nervous"),
                listOf("cerebellum", "hypothalamus", "medulla", "cranial nerve", "vagus", "trigeminal", "facial nerve", "retina", "cochlea", "vestibular", "synapse", "action potential")),
            ch("Anat", 4, "gi_biochem_renal", "GI, Nutrition/Biochemistry, Renal & Fluid Physiology", "Enzymes, vitamins, nephron, GFR, RAAS & acid-base",
                listOf("gastrointestinal", "nutrition_biochemistry", "renal", "fluid_physiology", "renal_endocrine"),
                listOf("nephron", "glomerul", "gfr", "loop of henle", "aldosterone", "renin", "bile", "pancreas", "amylase", "vitamin", "mineral", "pepsin")),
            ch("Anat", 5, "endo_repro", "Endocrine & Reproductive Systems", "Pituitary, thyroid, adrenal, pancreas & reproductive tracts",
                listOf("endocrine", "endocrine_cellular", "reproductive", "reproductive_musculoskeletal"),
                listOf("pituitary", "thyroid", "parathyroid", "adrenal", "cortisol", "insulin", "glucagon", "ovary", "fallopian", "uterus", "testis", "prostate"))
        ),
        "Admin" to listOf(
            ch("Admin", 1, "mgmt_leadership", "Management Principles, Leadership & Organization", "Fayol, POSDCORB, leadership styles, span of control & change",
                listOf("nursing_management", "motivation_leadership", "organizational_structure", "organizational_change", "communication_conflict", "admin_research"),
                listOf("posdcorb", "fayol", "maslow", "herzberg", "leadership", "autocratic", "democratic", "laissez", "span of control", "scalar chain", "delegation", "conflict")),
            ch("Admin", 2, "hr_finance_hospital", "Hospital Admin, HR, Inventory & Care Delivery", "Staffing norms, ABC/VED, budgeting, PERT & ward management",
                listOf("hospital_administration", "human_resource_management", "financial_material_management", "nursing_care_delivery", "healthcare_technology", "nursing_informatics_management"),
                listOf("abc analysis", "ved", "inventory", "budget", "pert", "gantt", "staffing", "nurse-patient ratio", "primary nursing", "functional nursing", "ehr", "informatics")),
            ch("Admin", 3, "quality_legal", "Quality Assurance, Accreditation & Legal Frameworks", "NABH, nursing audit, INC/TNAI, sentinel events & medico-legal",
                listOf("quality_accreditation", "legal_aspects_of_management", "professional_organizations", "quality_legal_research"),
                listOf("nabh", "audit", "sentinel", "quality", "accreditation", "inc", "tnai", "iccn", "consumer protection", "negligence")),
            ch("Admin", 4, "education", "Nursing Education & Clinical Teaching", "Curriculum, Bloom's taxonomy, AV aids, OSCE & evaluation",
                listOf("nursing_education", "nursing_foundations_education"),
                listOf("curriculum", "bloom", "lesson plan", "osce", "microteaching", "simulation", "evaluation", "formative", "summative", "audio-visual")),
            ch("Admin", 5, "research_biostat", "Nursing Research & Biostatistics", "Research designs, sampling, validity, reliability, p-value & tests",
                listOf("nursing_research", "biostatistics_research"),
                listOf("sampling", "random", "validity", "reliability", "cronbach", "chi-square", "t-test", "anova", "p-value", "null hypothesis", "type i error", "pilot study", "mean", "median", "standard deviation"))
        ),
        "Science" to listOf(
            ch("Science", 1, "biology", "Human Biology & Microbiology", "Cells, genetics, microbiology, blood & physiology basics",
                listOf("biology", "applied_science"),
                listOf("cell", "dna", "rna", "vitamin", "bacteria", "virus", "fungus", "blood", "organ", "enzyme", "photosynthesis", "mitosis")),
            ch("Science", 2, "chemistry", "General & Clinical Chemistry", "pH, acids/bases, isotopes, solutions, organic & inorganic salts",
                listOf("chemistry"),
                listOf("ph", "acid", "base", "atom", "isotope", "molarity", "sodium", "chlorine", "carbon", "oxidation", "catalyst", "periodic")),
            ch("Science", 3, "physics", "General Physics & Medical Biophysics", "SI units, optics, electricity, pressure, sound & radiation",
                listOf("physics"),
                listOf("si unit", "ohm", "watt", "volt", "newton", "pascal", "lens", "refraction", "ultrasound", "x-ray", "velocity", "gravity", "frequency"))
        ),
        "Math" to listOf(
            ch("Math", 1, "dosage_iv", "Drug Dosage, IV Drip Rate & Pediatric Math", "Drops/min, mL/hr, mg/kg dosing, liquid & unit conversions",
                listOf("iv_drip_rate", "infusion_pump", "liquid_dosage", "oral_dosage", "pediatric_dosing", "unit_conversion", "dosage_quant"),
                listOf("drop", "gtt", "ml/hr", "infusion", "dosage", "mg/kg", "mcg", "tablet", "syrup", "dilution", "convert", "iv fluid")),
            ch("Math", 2, "percent_ratio_avg", "Percentages, Ratios, Averages & Ages", "Proportions, mixtures, age word problems & LCM/HCF",
                listOf("percentages", "ratio_proportion", "averages", "ages", "lcm_hcf"),
                listOf("percent", "%", "ratio", "proportion", "average", "mean", "age", "lcm", "hcf")),
            ch("Math", 3, "profit_interest", "Profit & Loss, Simple & Compound Interest", "Discounts, cost price, SI/CI & commercial math",
                listOf("profit_loss", "simple_interest", "compound_interest"),
                listOf("profit", "loss", "discount", "interest", "principal", "amount", "rate of interest")),
            ch("Math", 4, "time_speed_mens", "Time & Work, Pipes, Speed & Mensuration", "Work rates, cisterns, trains, distance & area/volume",
                listOf("time_work", "pipes_cisterns", "trains_speed", "mensuration"),
                listOf("work", "pipe", "cistern", "tank", "train", "speed", "distance", "area", "volume", "perimeter", "cylinder"))
        ),
        "Reasoning" to listOf(
            ch("Reasoning", 1, "coding_series", "Number Series, Coding-Decoding & Odd One Out", "Pattern recognition, letter shifts & classification",
                listOf("number_series", "coding_decoding", "odd_one_out", "analogies", "logical_reasoning"),
                listOf("series", "code", "coded", "odd one", "analogy", "next number", "missing")),
            ch("Reasoning", 2, "blood_direction_rank", "Blood Relations, Direction Sense & Ranking", "Family trees, compass turns & seating/order puzzles",
                listOf("blood_relations", "direction_sense", "ranking_order"),
                listOf("father", "mother", "sister", "brother", "north", "south", "east", "west", "rank", "left", "right", "row")),
            ch("Reasoning", 3, "syllogism_clock_cal", "Syllogisms, Clocks & Calendars", "Deductive logic statements, clock angles & day calculations",
                listOf("syllogisms", "clocks", "calendar"),
                listOf("all ", "some ", "conclusion", "clock", "angle", "minute hand", "calendar", "leap year", "monday", "sunday"))
        ),
        "CA-2026" to listOf(
            ch("CA-2026", 1, "national_policy", "National Policy, Governance & Summits", "Union government initiatives, commissions & national milestones",
                listOf("national_policy", "governance", "summits"),
                listOf("parliament", "ministry", "summit", "g20", "niti", "commission", "policy", "bill", "act")),
            ch("CA-2026", 2, "science_space_tech", "Science, Space, AI & Defence News", "ISRO missions, digital health & defence developments",
                listOf("science_tech", "space", "defence"),
                listOf("isro", "gaganyaan", "chandrayaan", "aditya", "drdo", "satellite", "ai", "digital")),
            ch("CA-2026", 3, "awards_sports_intl", "Awards, Sports, Appointments & Global Events", "Padma/Nobel awards, international bodies & sports",
                listOf("awards_honors", "sports", "international"),
                listOf("award", "nobel", "padma", "olympic", "cricket", "who", "un", "appointed", "chairperson"))
        ),
        "Health-Prog" to listOf(
            ch("Health-Prog", 1, "mch_uip_drives", "RMNCH+A, UIP, Indradhanush & Nutrition Drives", "Maternal-child schemes, immunization & Anaemia Mukt Bharat",
                listOf("mch_schemes", "immunization_drives"),
                listOf("indradhanush", "uip", "rmnch", "jsy", "jssk", "poshan", "anaemia", "suman", "laqshya", "icds")),
            ch("Health-Prog", 2, "disease_elimination", "NTEP (TB), Malaria, Leprosy, Polio & HIV Drives", "Communicable disease elimination targets & protocols",
                listOf("disease_elimination", "ntep", "nvbdcp"),
                listOf("ntep", "nikshay", "tb", "malaria", "kala-azar", "leprosy", "nlep", "naco", "hiv", "polio")),
            ch("Health-Prog", 3, "abdm_nhm_ncd", "Ayushman Bharat, ABDM, NHM & NP-NCD", "PM-JAY, Health & Wellness Centres, e-Sanjeevani & Tele-MANAS",
                listOf("ayushman_bharat", "nhm", "ncd_programs"),
                listOf("ayushman", "pm-jay", "abdm", "hwc", "e-sanjeevani", "tele-manas", "nhm", "ncd", "kayakalp"))
        ),
        "Railways" to listOf(
            ch("Railways", 1, "zones_history", "Railway Zones, HQ & Indian Railways History", "18 zones, production units, gauges & historical firsts",
                listOf("railway_zones", "railway_history"),
                listOf("zone", "headquarter", "gauge", "first train", "icf", "dlw", "clw", "railways")),
            ch("Railways", 2, "budget_modernization", "Railway Budget, Vande Bharat & Kavach Safety", "Dedicated freight corridors, Kavach ATP & modern trains",
                listOf("railway_budget", "modernization"),
                listOf("vande bharat", "kavach", "bullet", "freight", "budget", "electrification", "amrit bharat"))
        ),
        "Static-GK" to listOf(
            ch("Static-GK", 1, "polity_constitution", "Indian Polity, Constitution & Fundamental Rights", "Articles, amendments, Parliament & constitutional bodies",
                listOf("polity", "constitution"),
                listOf("article", "constitution", "amendment", "president", "governor", "supreme court", "fundamental", "directive")),
            ch("Static-GK", 2, "history_geo", "Indian History, Geography & Heritage", "Freedom movement, rivers, national parks & UNESCO sites",
                listOf("history", "geography", "culture"),
                listOf("river", "mountain", "national park", "biosphere", "unesco", "movement", "dynasty", "capital", "state")),
            ch("Static-GK", 3, "orgs_days_awards", "Health Days, Organizations & General Facts", "World Health Day, Nurses Day, UN agencies & national symbols",
                listOf("awards_honors", "important_days", "organizations"),
                listOf("day", "may 12", "april 7", "nightingale", "headquarter", "nobel", "bharat ratna", "currency", "symbol"))
        )
    )

    private val highYieldKeywords = listOf(
        "priority", "first-line", "first line", "drug of choice", "antidote", "most common",
        "gold standard", "pathognomonic", "contraindicated", "emergency", "immediately",
        "triad", "shock", "eclampsia", "pph", "parkland", "apgar", "abg", "uip",
        "cold chain", "biomedical waste", "cpr", "triage", "mgso4", "magnesium sulfate",
        "digoxin", "lithium", "hyperkalemia", "anaphylax", "suction", "intubat"
    )

    fun normalizeSlug(raw: String): String =
        raw.trim().lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_')

    fun chaptersForSubject(subjectCode: String): List<ChapterMeta> =
        chaptersBySubject[subjectCode] ?: listOf(
            ChapterMeta(
                id = "${normalizeSlug(subjectCode)}_core",
                subjectCode = subjectCode,
                unitNumber = 1,
                title = "Core Syllabus Unit",
                subtitle = "Essential concepts & high-yield practice"
            )
        )

    fun chapterCountForSubject(subjectCode: String): Int =
        chaptersForSubject(subjectCode).size

    fun isHighYield(question: Question): Boolean {
        if (question.isPredictedMock || question.isPyqTagged) return true
        if (question.difficulty == QuestionDifficulty.HARD) return true
        if (question.questionText.contains("[Mains", ignoreCase = true)) return true
        val lower = question.questionText.lowercase(Locale.US)
        return highYieldKeywords.any { lower.contains(it) }
    }

    /**
     * Extracts a human-readable topic label from question.topicId (e.g. "msn_abg_interpretation" -> "ABG Interpretation")
     * or falls back to the matched chapter's title.
     */
    fun extractTopicLabel(question: Question, fallbackChapter: ChapterMeta): String {
        val subjPrefix = normalizeSlug(question.subject) + "_"
        val raw = question.topicId.trim().lowercase(Locale.US)
        val stripped = if (raw.startsWith(subjPrefix)) raw.removePrefix(subjPrefix) else raw
        if (stripped.isBlank() || stripped == "general" || stripped.startsWith("core_")) {
            return fallbackChapter.title
        }
        return formatTopicSlug(stripped)
    }

    private fun formatTopicSlug(slug: String): String {
        val acronyms = setOf(
            "abg", "ent", "gi", "iv", "obg", "chn", "fon", "msn", "uip", "cpr",
            "ecg", "icu", "ncd", "nhm", "cho", "bmw", "pih", "aph", "pph",
            "ect", "cbt", "mse", "lcm", "hcf", "gk", "ca", "hr", "who", "tb"
        )
        return slug.split('_')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                if (part in acronyms) part.uppercase(Locale.US)
                else part.replaceFirstChar { it.titlecase(Locale.US) }
            }
    }

    fun classify(question: Question): Pair<ChapterMeta, String> {
        val chapters = chaptersForSubject(question.subject)
        val subjPrefix = normalizeSlug(question.subject) + "_"
        val rawTopic = question.topicId.trim().lowercase(Locale.US)
        val topicSlug = normalizeSlug(if (rawTopic.startsWith(subjPrefix)) rawTopic.removePrefix(subjPrefix) else rawTopic)

        // 1. Direct topic slug match (O(chapters) tiny scan — covers 100% of subject_practice_bank & predicted_mock_bank)
        if (topicSlug.isNotBlank() && topicSlug != "general") {
            for (ch in chapters) {
                if (topicSlug in ch.topicSlugs) {
                    return ch to extractTopicLabel(question, ch)
                }
            }
            for (ch in chapters) {
                if (ch.topicSlugs.any { topicSlug.contains(it) || it.contains(topicSlug) }) {
                    return ch to extractTopicLabel(question, ch)
                }
            }
        }

        // 2. Keyword classification on stem + explanation (handles untagged PYQs cleanly)
        val haystack = "${question.questionText} ${question.explanation}".lowercase(Locale.US)
        var bestChapter: ChapterMeta? = null
        var bestScore = 0
        for (ch in chapters) {
            var score = 0
            for (kw in ch.keywords) {
                if (haystack.contains(kw)) score++
            }
            if (score > bestScore) {
                bestScore = score
                bestChapter = ch
            }
        }

        val resolved = bestChapter ?: chapters.last()
        return resolved to extractTopicLabel(question, resolved)
    }

    /**
     * Groups a subject's question pool into ordered INC syllabus Chapters -> nested Topics,
     * applying optional difficulty and High-Yield filters.
     */
    fun buildChaptersForSubject(
        subjectCode: String,
        items: List<QuestionItem>,
        highYieldOnly: Boolean = false,
        difficultyFilter: DifficultyFilter = DifficultyFilter.ALL
    ): List<ChapterBucket> {
        val targetDiff = difficultyFilter.difficulty
        val chapterOrder = chaptersForSubject(subjectCode)
        val byChapterId = LinkedHashMap<String, ArrayList<Pair<QuestionItem, String>>>(chapterOrder.size * 2)
        for (ch in chapterOrder) {
            byChapterId[ch.id] = ArrayList()
        }

        for (item in items) {
            val q = item.question
            if (q.subject != subjectCode) continue
            if (targetDiff != null && q.difficulty != targetDiff) continue
            if (highYieldOnly && !isHighYield(q)) continue
            val (chapter, topicLabel) = classify(q)
            byChapterId.getOrPut(chapter.id) { ArrayList() }.add(item to topicLabel)
        }

        return chapterOrder.mapNotNull { meta ->
            val pairs = byChapterId[meta.id]
            if (pairs.isNullOrEmpty()) return@mapNotNull null

            val chapterItems = ArrayList<QuestionItem>(pairs.size)
            val topicMap = LinkedHashMap<String, ArrayList<QuestionItem>>()
            var hyCount = 0
            var mastered = 0
            var attempted = 0
            var totalAttempts = 0
            var correctAttempts = 0

            for ((item, topicLabel) in pairs) {
                chapterItems.add(item)
                topicMap.getOrPut(topicLabel) { ArrayList() }.add(item)
                if (isHighYield(item.question)) hyCount++
                val p = item.progress
                if (p.attemptsCount > 0) attempted++
                if (p.lastOutcome == "right") mastered++
                totalAttempts += p.attemptsCount
                correctAttempts += p.correctCount
            }

            val topicBuckets = topicMap.entries
                .sortedByDescending { it.value.size }
                .map { (label, tItems) ->
                    var tHy = 0
                    var tMastered = 0
                    var tAttempted = 0
                    for (it in tItems) {
                        if (isHighYield(it.question)) tHy++
                        if (it.progress.attemptsCount > 0) tAttempted++
                        if (it.progress.lastOutcome == "right") tMastered++
                    }
                    TopicBucket(
                        id = "${meta.id}_${normalizeSlug(label)}",
                        label = label,
                        items = tItems,
                        highYieldCount = tHy,
                        masteredCount = tMastered,
                        attemptedCount = tAttempted
                    )
                }

            val acc = if (totalAttempts > 0) ((correctAttempts.toFloat() / totalAttempts) * 100f).toInt() else 0
            ChapterBucket(
                meta = meta,
                items = chapterItems,
                topics = topicBuckets,
                highYieldCount = hyCount,
                masteredCount = mastered,
                attemptedCount = attempted,
                accuracyPercent = acc
            )
        }
    }
}
