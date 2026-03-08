package com.bitchat.android.ui

data class ScenarioData(
    val id: String,
    val title: String,
    val category: String,
    val categoryColor: Long,
    val call999Script: String,
    val doNots: List<String>,
    val steps: List<ScenarioStep>,
    val equipmentTypes: List<String>,
    val evidenceNote: String,
    val sources: List<SourceRef>,
    val interactiveType: InteractiveType?
)

data class ScenarioStep(
    val title: String,
    val detail: String
)

data class SourceRef(
    val name: String,
    val note: String
)

enum class InteractiveType {
    CPR_METRONOME, FLUSH_TIMER, COOLING_TIMER,
    ELECTRICAL_BRANCH, COOLING_CHECKLIST,
    LONE_WORKER_BRANCH, SPINAL_GATE
}

fun allScenarios(siteAddress: String): List<ScenarioData> {
    val addr = siteAddress.ifBlank { "[site address not configured]" }

    return listOf(
        // 1. Cardiac Arrest
        ScenarioData(
            id = "cardiac",
            title = "Cardiac Arrest",
            category = "Medical",
            categoryColor = 0xFFE5484D,
            call999Script = "Tell them: cardiac arrest, construction site. Address: $addr",
            doNots = listOf(
                "Do not delay compressions to search for AED \u2014 start immediately",
                "Do not stop compressions for more than 10 seconds",
                "Do not press on the ribs \u2014 hands on centre of breastbone only"
            ),
            steps = listOf(
                ScenarioStep("Call 999", "Tell them: cardiac arrest, construction site. Address: $addr"),
                ScenarioStep("Check for response", "Shake shoulders firmly. Shout: Are you all right?"),
                ScenarioStep("Open airway", "One hand on forehead, tilt head back. Lift chin with two fingers."),
                ScenarioStep("Check breathing", "Look, listen, feel for no more than 10 seconds. Not breathing normally \u2014 start CPR."),
                ScenarioStep("30 chest compressions", "Heel of hand on centre of breastbone. Arms straight. Press 5\u20136 cm down, release fully. 100\u2013120 per minute."),
                ScenarioStep("2 rescue breaths", "Pinch nose, seal mouth, watch chest rise. Return to compressions immediately."),
                ScenarioStep("Attach AED on arrival", "Switch on, follow voice prompts. Clear everyone before shock. Restart CPR immediately after shock."),
                ScenarioStep("Continue until paramedics arrive", "Swap with another responder every 2 minutes if possible.")
            ),
            equipmentTypes = listOf("AED", "FIRST_AID_KIT"),
            evidenceNote = "Survival from cardiac arrest with CPR and AED within 3\u20135 minutes can exceed 70%. Without intervention: survival falls 10% per minute (Resuscitation Council UK).",
            sources = listOf(
                SourceRef("HSE INDG347", "First aid at work guidance"),
                SourceRef("Resuscitation Council UK", "Adult basic life support guidelines"),
                SourceRef("BHF 2023", "British Heart Foundation cardiac arrest statistics")
            ),
            interactiveType = InteractiveType.CPR_METRONOME
        ),

        // 2. Fall from Height
        ScenarioData(
            id = "fall",
            title = "Fall from Height",
            category = "Trauma",
            categoryColor = 0xFFE5484D,
            call999Script = "Tell them: fall from height, suspected spinal injury, ambulance only. Address: $addr",
            doNots = listOf(
                "Do not move the casualty unless in immediate danger",
                "Do not remove the helmet without a specific airway reason",
                "Do not allow them to stand even if they say they can",
                "Do not give food, water, or medication"
            ),
            steps = listOf(
                ScenarioStep("Call 999", "Tell them: fall from height, suspected spinal injury, ambulance only. Address: $addr"),
                ScenarioStep("Do not move the casualty", "Unless in immediate danger. Say: Help is coming, try not to move."),
                ScenarioStep("Check airway", "Conscious and breathing: leave as found and monitor. Unconscious, not breathing: minimal head tilt, start CPR \u2014 life takes priority."),
                ScenarioStep("Retrieve spinal board", "Send another person to the equipment location shown in this alert."),
                ScenarioStep("Control visible bleeding", "Firm direct pressure without moving the injured area."),
                ScenarioStep("Keep them warm", "Blanket or clothing over them, not under. Do not move to apply.")
            ),
            equipmentTypes = listOf("SPINAL_BOARD", "FIRST_AID_KIT"),
            evidenceNote = "Falls from height account for 28% of UK workplace fatalities (HSE 2024/25). Improper movement is a documented cause of secondary spinal cord injury.",
            sources = listOf(
                SourceRef("HSE INDG347", "First aid at work guidance"),
                SourceRef("ANZCOR Guideline 9.1.4 (2024)", "Spinal injury management")
            ),
            interactiveType = InteractiveType.SPINAL_GATE
        ),

        // 3. Severe Bleeding
        ScenarioData(
            id = "bleeding",
            title = "Severe Bleeding",
            category = "Trauma",
            categoryColor = 0xFFE5484D,
            call999Script = "Tell them: severe bleeding, construction site. Address: $addr",
            doNots = listOf(
                "Do not remove a blood-soaked dressing \u2014 add more on top",
                "Do not apply tourniquet over a joint",
                "Do not remove a tourniquet once applied \u2014 paramedics only",
                "Do not give food, water, or medication"
            ),
            steps = listOf(
                ScenarioStep("Call 999", "Tell them: severe bleeding, construction site. Address: $addr"),
                ScenarioStep("Protect yourself", "Use gloves if available. If not, a plastic bag or clothing as barrier."),
                ScenarioStep("Apply firm direct pressure", "Press hard with a clean pad. Blood soaks through: add more on top, do not remove."),
                ScenarioStep("Raise the injured part", "Above heart height while maintaining pressure. Not if fracture suspected."),
                ScenarioStep("Hold for minimum 10 minutes", "Do not lift the pad to check."),
                ScenarioStep("Tourniquet \u2014 limb amputation or arterial bleed", "At least 5 cm above wound on single bone. Tighten until bleeding stops. Note exact time."),
                ScenarioStep("Torso, neck, or groin wounds", "Tourniquet cannot be used. Apply haemostatic dressing with sustained firm pressure."),
                ScenarioStep("Treat for shock", "Lay down, raise legs 20 cm unless spinal injury suspected. Keep warm. No fluids.")
            ),
            equipmentTypes = listOf("FIRST_AID_KIT", "TOURNIQUET"),
            evidenceNote = "HSE recommends tourniquets for construction site first aid kits (HSE eBulletin, June 2016). Record time of application and communicate to paramedics.",
            sources = listOf(
                SourceRef("HSE INDG347", "First aid at work guidance"),
                SourceRef("HSE L74 (2024)", "First aid approved code of practice"),
                SourceRef("UK Resuscitation Council (2022)", "Bleeding control guidelines")
            ),
            interactiveType = null
        ),

        // 4. Chemical Splash
        ScenarioData(
            id = "chemical",
            title = "Chemical Splash",
            category = "Chemical",
            categoryColor = 0xFFE8960C,
            call999Script = "Tell them: chemical eye or skin exposure, construction site. Address: $addr",
            doNots = listOf(
                "Do not rub the eyes or skin \u2014 spreads the chemical",
                "Do not apply any cream, butter, or home remedy",
                "Do not remove clothing stuck to burned skin \u2014 flush over it",
                "Do not attempt to neutralise acid or alkali \u2014 flush with water only"
            ),
            steps = listOf(
                ScenarioStep("Flush with water immediately", "Start with any clean water source now, while moving to the eyewash station."),
                ScenarioStep("Get to eyewash station", "Location shown in this alert. Flush continuously for 20 minutes. Use the timer."),
                ScenarioStep("Hold eyelids open throughout", "Use fingers to hold lids apart. Rotate eyeball to flush all surfaces. Remove contact lenses as soon as possible."),
                ScenarioStep("Skin exposure", "Remove contaminated clothing not stuck to skin. Flood skin with cool water for 20 minutes."),
                ScenarioStep("Call 999 or go to A&E", "All chemical eye injuries require A&E. Take the product label or SDS.")
            ),
            equipmentTypes = listOf("EYEWASH", "FIRST_AID_KIT"),
            evidenceNote = "Every second of delay in chemical eye flushing increases risk of permanent corneal damage. Minimum flush time is 20 minutes (BS EN 15154).",
            sources = listOf(
                SourceRef("HSE INDG347", "First aid at work guidance"),
                SourceRef("NHS Burns Treatment", "Chemical burn management"),
                SourceRef("COSHH Regulations 2002", "Control of substances hazardous to health"),
                SourceRef("BS EN 15154", "Emergency safety showers and eye wash equipment")
            ),
            interactiveType = InteractiveType.FLUSH_TIMER
        ),

        // 5. Electrical Contact
        ScenarioData(
            id = "electrical",
            title = "Electrical Contact",
            category = "Electrical",
            categoryColor = 0xFFE8960C,
            call999Script = "Tell them: electrical injury, construction site. Address: $addr",
            doNots = listOf(
                "Do not touch the casualty until power source is confirmed isolated",
                "Do not approach a high-voltage source under any circumstances",
                "Do not use anything wet or metal to move a live source",
                "Do not apply water to electrical burns",
                "Do not allow casualty to leave without hospital assessment \u2014 delayed arrhythmia is a known risk"
            ),
            steps = listOf(
                ScenarioStep("Do not touch the casualty", "Touching before isolation will electrocute you. This is the first rule."),
                ScenarioStep("Call 999", "Tell them: electrical injury, construction site. Address: $addr"),
                ScenarioStep("Select voltage type", "Low voltage and high voltage require completely different responses. Use the selector below."),
                ScenarioStep("Once isolated \u2014 assess", "Check airway and breathing. Not breathing: start CPR and attach AED immediately.")
            ),
            equipmentTypes = listOf("AED", "FIRST_AID_KIT"),
            evidenceNote = "All electrical injuries require hospital assessment regardless of apparent severity. Internal burns from electrical current often require major surgery.",
            sources = listOf(
                SourceRef("HSE Electrical Injuries", "Electrical safety guidance"),
                SourceRef("HSE INDG347", "First aid at work guidance"),
                SourceRef("IEC 60479", "Effects of current on human beings")
            ),
            interactiveType = InteractiveType.ELECTRICAL_BRANCH
        ),

        // 6. Crush Injury
        ScenarioData(
            id = "crush",
            title = "Crush Injury",
            category = "Trauma",
            categoryColor = 0xFFE5484D,
            call999Script = "Tell them: crush injury, person trapped, construction site. Address: $addr. State how long trapped.",
            doNots = listOf(
                "Do not allow casualty to stand or walk after release",
                "Do not assume they are fine because conscious \u2014 crush syndrome can develop after apparent recovery",
                "Do not give food or water",
                "Do not apply tourniquet over the crushed area \u2014 apply above it"
            ),
            steps = listOf(
                ScenarioStep("Call 999 before releasing", "Tell them: crush injury, person trapped. Address: $addr. State how long trapped."),
                ScenarioStep("Assess scene safety", "Structure and plant must be stable before approaching."),
                ScenarioStep("Keep them talking", "No food or water. Communicate throughout."),
                ScenarioStep("Control visible bleeding while trapped", "Direct pressure only. No tourniquet while limb still trapped."),
                ScenarioStep("Release as quickly as safely possible", "Longer crush time means greater crush syndrome risk."),
                ScenarioStep("Tourniquet immediately on release \u2014 limb crush over 15 minutes", "Apply above the crush site at the moment of release."),
                ScenarioStep("Lay flat, treat for shock", "Do not allow to stand. Blankets over body. No food or fluids."),
                ScenarioStep("Monitor closely", "Deterioration can be rapid even if casualty appears to recover. Be ready to start CPR.")
            ),
            equipmentTypes = listOf("FIRST_AID_KIT", "TOURNIQUET"),
            evidenceNote = "Crush syndrome risk rises significantly after 15 minutes of compression (ANZCOR 2026). Paramedics need to be on site at moment of release.",
            sources = listOf(
                SourceRef("ANZCOR Guideline 9.1.7 (2026)", "Crush injury management"),
                SourceRef("HSE INDG347", "First aid at work guidance")
            ),
            interactiveType = null
        ),

        // 7. Burns
        ScenarioData(
            id = "burns",
            title = "Burns",
            category = "Thermal",
            categoryColor = 0xFFE8960C,
            call999Script = "Tell them: burns injury, construction site. Address: $addr",
            doNots = listOf(
                "Do not apply ice, iced water, or cold packs",
                "Do not apply butter, toothpaste, cream, or any home remedy",
                "Do not wrap cling film around a limb \u2014 lay it lengthways",
                "Do not burst blisters",
                "Do not remove clothing stuck to burned skin",
                "Do not apply water to electrical burns \u2014 dry dressings only"
            ),
            steps = listOf(
                ScenarioStep("Stop the burning process", "Remove from source. Clothing on fire: stop, drop, roll. Smother with blanket."),
                ScenarioStep("Call 999 for serious burns", "Burns larger than casualty's hand, face/neck/hands/joints, white or charred skin, any inhalation. Address: $addr"),
                ScenarioStep("Cool with running water for 20 minutes", "Start immediately \u2014 use the timer. Even hours after injury, cooling reduces depth."),
                ScenarioStep("Keep person warm while cooling burn", "Blanket over rest of body. Cooling the burn and warming the person are both required."),
                ScenarioStep("Remove clothing and jewellery", "Carefully remove unless stuck to skin. Never pull off stuck items."),
                ScenarioStep("Cover loosely", "Cling film laid lengthways, clean plastic bag for hands, or sterile non-fluffy dressing.")
            ),
            equipmentTypes = listOf("BURNS_KIT", "FIRST_AID_KIT"),
            evidenceNote = "Electrical burns: A&E regardless of size. Inhalation burns are life-threatening \u2014 call 999 immediately and monitor airway.",
            sources = listOf(
                SourceRef("HSE INDG347", "First aid at work guidance"),
                SourceRef("NHS Burns Treatment", "Burns and scalds treatment")
            ),
            interactiveType = InteractiveType.COOLING_TIMER
        ),

        // 8. Confined Space Rescue
        ScenarioData(
            id = "confined",
            title = "Confined Space Rescue",
            category = "Confined Space",
            categoryColor = 0xFFE5484D,
            call999Script = "Tell them: confined space rescue, person collapsed inside, construction site. Address: $addr. Fire and Rescue required.",
            doNots = listOf(
                "Do not enter the space without breathing apparatus \u2014 you will become a second casualty",
                "Do not lean into the space to reach the casualty",
                "Do not turn off ventilation equipment already running",
                "Do not attempt rescue without the site pre-planned confined space procedure"
            ),
            steps = listOf(
                ScenarioStep("Call 999", "Fire and Rescue attend with breathing apparatus. Address: $addr"),
                ScenarioStep("Do not enter the space", "The same atmosphere that collapsed the worker will incapacitate you within seconds."),
                ScenarioStep("Alert permit holder and supervisor", "Activate the site pre-planned confined space rescue procedure."),
                ScenarioStep("Attempt non-entry rescue first", "If a lifeline is attached, use the retrieval system from outside."),
                ScenarioStep("Increase ventilation if safe", "Force fresh air into the space without entering."),
                ScenarioStep("Entry only by trained BA wearers", "Wait for Fire and Rescue if not available on site."),
                ScenarioStep("On extraction", "Check airway, breathing, circulation. Start CPR if required.")
            ),
            equipmentTypes = listOf("FIRST_AID_KIT"),
            evidenceNote = "60% of confined space fatalities are would-be rescuers who entered without protection (CCOHS). Confined Spaces Regulations 1997 require pre-planned rescue arrangements.",
            sources = listOf(
                SourceRef("Confined Spaces Regulations 1997", "Legal requirements for confined space work"),
                SourceRef("HSE", "Confined space safety guidance"),
                SourceRef("CDM 2015", "Construction design and management regulations")
            ),
            interactiveType = null
        ),

        // 9. Breathing Difficulty
        ScenarioData(
            id = "breathing",
            title = "Breathing Difficulty",
            category = "Respiratory",
            categoryColor = 0xFFE8960C,
            call999Script = "Tell them: breathing difficulty, construction site. Address: $addr",
            doNots = listOf(
                "Do not lay a conscious breathing casualty flat",
                "Do not leave them alone at any point",
                "Do not re-enter an atmosphere that caused fume inhalation"
            ),
            steps = listOf(
                ScenarioStep("Call 999", "Tell them: breathing difficulty, construction site. Address: $addr"),
                ScenarioStep("Sit upright, lean slightly forward", "Easiest breathing position. Do not lay flat."),
                ScenarioStep("Asthma", "Help use their own blue reliever inhaler. One puff every 30\u201360 seconds, up to 10 puffs."),
                ScenarioStep("Choking \u2014 conscious", "Can they cough? Encourage coughing. Cannot cough or speak: 5 back blows between shoulder blades, then 5 abdominal thrusts. Repeat."),
                ScenarioStep("Choking \u2014 becomes unconscious", "Lower to ground. Call 999. Start CPR \u2014 compressions may dislodge the object."),
                ScenarioStep("Carbon monoxide or fume inhalation", "Remove to fresh air immediately. Do not re-enter. Inform 999 of atmospheric hazard."),
                ScenarioStep("Anaphylaxis", "Swelling of throat or tongue, rash, rapid deterioration: administer EpiPen to outer thigh if available. Call 999."),
                ScenarioStep("Casualty stops breathing", "Start CPR immediately \u2014 Cardiac Arrest protocol.")
            ),
            equipmentTypes = listOf("FIRST_AID_KIT", "AED", "OXYGEN"),
            evidenceNote = "Multiple workers becoming unwell simultaneously in an enclosed area indicates carbon monoxide. Evacuate everyone and call 999.",
            sources = listOf(
                SourceRef("HSE INDG347", "First aid at work guidance"),
                SourceRef("NHS", "Breathing emergencies guidance")
            ),
            interactiveType = null
        ),

        // 10. Heat Stroke
        ScenarioData(
            id = "heat",
            title = "Heat Stroke",
            category = "Medical",
            categoryColor = 0xFFE5484D,
            call999Script = "Tell them: heat stroke, construction site. Address: $addr",
            doNots = listOf(
                "Do not give fluids to a confused or unconscious casualty \u2014 aspiration risk",
                "Do not use ice-cold water immersion \u2014 cool water only",
                "Do not allow them to walk it off \u2014 hospital assessment required",
                "Do not leave them unattended"
            ),
            steps = listOf(
                ScenarioStep("Call 999", "Tell them: heat stroke, construction site. Address: $addr"),
                ScenarioStep("Move to shade or cool area", "Out of direct sun and away from hot machinery."),
                ScenarioStep("Cool by all available means", "Use the checklist. Cool water, fanning, ice packs to neck, armpits, and groin."),
                ScenarioStep("Position correctly", "Conscious: sit or lie. Confused: lay on side. Unconscious and breathing: recovery position."),
                ScenarioStep("Monitor closely", "Deterioration can be rapid. Do not leave alone. Be ready to start CPR.")
            ),
            equipmentTypes = listOf("FIRST_AID_KIT"),
            evidenceNote = "Heat stroke vs exhaustion \u2014 Exhaustion: heavy sweating, pale, lucid. Stroke: confused, slurred speech, seizure, or loss of consciousness. If in any doubt, treat as heat stroke.",
            sources = listOf(
                SourceRef("OSHA Heat Illness", "Occupational heat illness prevention"),
                SourceRef("NHS / Mayo Clinic", "Heat stroke treatment guidelines"),
                SourceRef("CCOHS", "Heat stress prevention")
            ),
            interactiveType = InteractiveType.COOLING_CHECKLIST
        ),

        // 11. Lone Worker Emergency
        ScenarioData(
            id = "lone",
            title = "Lone Worker Emergency",
            category = "Unknown",
            categoryColor = 0xFF8A8E96,
            call999Script = "Tell them: lone worker unresponsive, unknown condition, construction site. Address: $addr",
            doNots = listOf(
                "Do not assume the situation is non-serious because the worker is not calling out",
                "Do not search confined or hazardous areas alone"
            ),
            steps = listOf(
                ScenarioStep("Respond immediately", "Treat as potentially life-threatening."),
                ScenarioStep("Call 999 while travelling", "Tell them: lone worker unresponsive, unknown condition. Address: $addr"),
                ScenarioStep("Call out as you approach", "Their response guides your immediate action. Use the condition selector below."),
                ScenarioStep("Stay with them", "Send a second person to meet emergency services at the site entrance and guide them in.")
            ),
            equipmentTypes = listOf("FIRST_AID_KIT", "AED"),
            evidenceNote = "SiteNodes flag when a device has not been detected for an unusual period, enabling a lone worker alert before an SOS is manually sent.",
            sources = listOf(
                SourceRef("HSE INDG73", "Working alone guidance"),
                SourceRef("CDM 2015", "Construction design and management regulations")
            ),
            interactiveType = InteractiveType.LONE_WORKER_BRANCH
        )
    )
}
