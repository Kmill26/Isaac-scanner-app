package com.example.data.model

object IsaacItemDatabase {

    val items: List<IsaacItem> = listOf(
        // --- QUALITY 4 ITEMS (GOD TIER) ---
        IsaacItem(
            id = 118,
            name = "Brimstone",
            quote = "Blood laser barrage",
            description = "Replaces standard tears with a devastating charged blood laser beam that pierces enemies and obstacles, dealing continuous tick damage.",
            quality = 4,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Devil Room", "Fallen Drop"),
            transformations = listOf("Leviathan"),
            stats = mapOf("Damage" to "13x tick multiplier", "Tears" to "-0.66x delay modifier"),
            iconEmoji = "🩸",
            dlc = "Rebirth",
            synergies = listOf(
                SynergyInfo("Soy Milk", SynergyRating.GOD_TIER, "Infinite Continuous Laser", "Removes charge time completely! You shoot a continuous unending blood beam stream with immense DPS."),
                SynergyInfo("Tech X", SynergyRating.GOD_TIER, "Blood Laser Rings", "Charged shots fire large swirling rings made of red brimstone lasers with double tick damage."),
                SynergyInfo("C Section", SynergyRating.GOD_TIER, "Laser-Shooting Fetuses", "The propelled fetuses shoot their own Brimstone lasers in 4 directions while latching onto enemies."),
                SynergyInfo("Sacred Heart", SynergyRating.GOD_TIER, "Homing Divine Blood Laser", "Brimstone laser turns vibrant purple-crimson and violently curves toward any nearby boss or enemy."),
                SynergyInfo("Ipecac", SynergyRating.GOD_TIER, "Poisonous Explosive Laser", "Adds massive flat +40 DMG to Brimstone and causes green explosive bursts on contact."),
                SynergyInfo("My Reflection", SynergyRating.ANTI_SYNERGY, "Dangerous Curve Back", "Brimstone beam curls tightly backwards, risking self-obscured visibility in bullet hells."),
                SynergyInfo("Mutant Spider", SynergyRating.EXCELLENT, "Quad Laser Barrage", "Fires 4 separate dense Brimstone beams simultaneously."),
                SynergyInfo("Ludovico Technique", SynergyRating.GOD_TIER, "Controllable Giant Laser Ring", "Spawns a massive controllable crimson brimstone ring that deals continuous damage.")
            )
        ),
        IsaacItem(
            id = 182,
            name = "Sacred Heart",
            quote = "Homing shots + DMG up",
            description = "Huge +2.3x Damage multiplier, grants strong homing tears, full red health heal, +1 Max Health container, and decreases shot speed.",
            quality = 4,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Angel Room"),
            transformations = listOf("Seraphim"),
            stats = mapOf("Damage" to "x2.3 Mult + 1.0", "Tears" to "-0.4", "Shot Speed" to "-0.25", "Health" to "+1 Red Heart"),
            iconEmoji = "❤️",
            dlc = "Rebirth",
            synergies = listOf(
                SynergyInfo("Godhead", SynergyRating.GOD_TIER, "Homing Aura of Annihilation", "Massive homing tears surrounded by huge radiant damaging halos that stick onto bosses."),
                SynergyInfo("Brimstone", SynergyRating.GOD_TIER, "Curving Blood Stream", "Brimstone lasers gain high damage boost and actively bend toward enemies."),
                SynergyInfo("C Section", SynergyRating.GOD_TIER, "Homing Ghost Fetuses", "Fetuses aggressively home onto enemies and stay glued to targets."),
                SynergyInfo("Soy Milk", SynergyRating.GOD_TIER, "Tear Gatling Machine", "Mitigates Soy Milk's damage penalty with 2.3x multiplier and ensures 100% accuracy via homing.")
            )
        ),
        IsaacItem(
            id = 678,
            name = "C Section",
            quote = "Fetus whip-out",
            description = "Isaac shoots ghost fetuses that float forward, pierce obstacles, and latch onto enemies to deal continuous rapid tear damage.",
            quality = 4,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Treasure Room"),
            transformations = listOf("Conjoined"),
            stats = mapOf("Damage" to "Continuous Tick", "Range" to "+Special Fetus Trajectory"),
            iconEmoji = "👶",
            dlc = "Repentance",
            synergies = listOf(
                SynergyInfo("Brimstone", SynergyRating.GOD_TIER, "Laser Fetuses", "Fetuses fire their own Brimstone lasers while latched onto bosses."),
                SynergyInfo("Tech X", SynergyRating.GOD_TIER, "Laser Aura Fetuses", "Each fetus is surrounded by an expanding electric laser ring."),
                SynergyInfo("Spoon Bender", SynergyRating.EXCELLENT, "Homing Fetuses", "Fetuses actively track down agile enemies across the room."),
                SynergyInfo("Ipecac", SynergyRating.GOD_TIER, "Poison Bomb Fetuses", "Fetuses deal massive explosion poison damage on impact without self damage.")
            )
        ),
        IsaacItem(
            id = 399,
            name = "Tech X",
            quote = "Laser ring tears",
            description = "Allows Isaac to charge and shoot expanding laser rings that pass through obstacles and enemies, dealing tick damage based on size.",
            quality = 4,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Treasure Room"),
            transformations = emptyList(),
            stats = mapOf("Damage" to "Charged Ring Damage", "Tears" to "Charge Rate Scaling"),
            iconEmoji = "⭕",
            dlc = "Afterbirth",
            synergies = listOf(
                SynergyInfo("Brimstone", SynergyRating.GOD_TIER, "Red Brimstone Swirls", "Laser rings become thick swirling blood loops with double damage."),
                SynergyInfo("Sacred Heart", SynergyRating.GOD_TIER, "Homing Electric Hoops", "Rings slow down and lock onto enemies, hitting them dozens of times."),
                SynergyInfo("Soy Milk", SynergyRating.EXCELLENT, "Instant Rapid Rings", "Allows firing mini rings instantaneously with zero charge delay.")
            )
        ),
        IsaacItem(
            id = 331,
            name = "Godhead",
            quote = "God tears",
            description = "Tears gain a large glowing aura that deals constant contact damage to enemies independent of tear collisions. Grants homing tears.",
            quality = 4,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Angel Room"),
            transformations = listOf("Seraphim"),
            stats = mapOf("Damage" to "+0.5", "Tears" to "-0.3", "Shot Speed" to "-0.3", "Range" to "+1.2"),
            iconEmoji = "👁️",
            dlc = "Rebirth",
            synergies = listOf(
                SynergyInfo("Sacred Heart", SynergyRating.GOD_TIER, "Mega Homing Aura", "Massive overlapping auras that melt bosses within seconds."),
                SynergyInfo("Continuum", SynergyRating.EXCELLENT, "Screen-Wrapping Halo", "Auras travel through room walls, filling the entire room with divine light."),
                SynergyInfo("Tiny Planet", SynergyRating.EXCELLENT, "Orbiting Aura Field", "Creates an impenetrable rotating ring of death around Isaac.")
            )
        ),
        IsaacItem(
            id = 149,
            name = "Ipecac",
            quote = "Explosive spit",
            description = "Fires high-arcing green explosive tears that poison enemies and deal massive +40 base flat damage.",
            quality = 4,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Treasure Room"),
            transformations = listOf("Bob"),
            stats = mapOf("Damage" to "+40.0 Flat", "Tears" to "-66% Rate", "Shot Speed" to "-0.2"),
            iconEmoji = "🤢",
            dlc = "Rebirth",
            synergies = listOf(
                SynergyInfo("Pyromaniac", SynergyRating.GOD_TIER, "Infinite Healing", "Explosions heal Isaac instead of causing damage, making you invincible!"),
                SynergyInfo("Host Hat", SynergyRating.GOD_TIER, "Explosion Immunity", "Grants 100% immunity to your own Ipecac blasts."),
                SynergyInfo("Soy Milk", SynergyRating.GOD_TIER, "Gatling Grenades", "Fires rapid-fire explosive poison shots at max tear rate with high total DPS."),
                SynergyInfo("My Reflection", SynergyRating.ANTI_SYNERGY, "Boomerang Self Destruction", "Explosive tears curve directly back to Isaac's face! Extreme hazard!"),
                SynergyInfo("Cricket's Body", SynergyRating.SITUATIONAL, "Cluster Explosions", "Tears split into 4 smaller explosive shots. High damage but dangerous indoors.")
            )
        ),
        IsaacItem(
            id = 579,
            name = "Rock Bottom",
            quote = "It's only up from here",
            description = "Stats can NEVER decrease for the rest of the run. Any temporary stat boosts become permanent!",
            quality = 4,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Secret Room"),
            transformations = emptyList(),
            stats = mapOf("Special" to "Stat Floor Locking"),
            iconEmoji = "🪨",
            dlc = "Repentance",
            synergies = listOf(
                SynergyInfo("Soy Milk", SynergyRating.GOD_TIER, "Permanent Max Tears Without Damage Loss", "Soy Milk grants max tear rate, then removing or overriding keeps max tears with full base damage!"),
                SynergyInfo("Red Stew", SynergyRating.GOD_TIER, "Permanent +21.6 Damage", "The temporary decaying damage boost from Red Stew never decays!"),
                SynergyInfo("Mega Mush", SynergyRating.GOD_TIER, "Permanent +4x Damage Multiplier", "Locks the gigantic damage multiplier forever."),
                SynergyInfo("Kidney Stone", SynergyRating.GOD_TIER, "Permanent Max Machine Gun Tears", "Burst fire tear rate lock.")
            )
        ),
        IsaacItem(
            id = 636,
            name = "Spindown Dice",
            quote = "-1",
            description = "Active item. Rerolls any pedestal item into the item with an internal ID of (Current ID - 1). Allows guaranteed crafting of Quality 4 items!",
            quality = 4,
            itemType = ItemType.ACTIVE,
            recharge = "6 Rooms",
            itemPools = listOf("Secret Room", "Treasure Room"),
            iconEmoji = "🎲",
            dlc = "Repentance",
            synergies = listOf(
                SynergyInfo("The Battery", SynergyRating.EXCELLENT, "Double Rerolls", "Stores up to two charges to spin down items two steps at once."),
                SynergyInfo("Schoolbag", SynergyRating.GOD_TIER, "Dual Active Hold", "Keep Spindown Dice alongside combat active items like D6 or Book of Revelations.")
            )
        ),
        IsaacItem(
            id = 114,
            name = "Mom's Knife",
            quote = "Stab stab stab",
            description = "Replaces tears with a piercing, controllable knife that deals huge multi-hit damage when stabbed or charged and thrown.",
            quality = 4,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Treasure Room", "Devil Room"),
            transformations = listOf("Mom"),
            stats = mapOf("Damage" to "6x Damage Multiplier per tick"),
            iconEmoji = "🔪",
            dlc = "Rebirth",
            synergies = listOf(
                SynergyInfo("Brimstone", SynergyRating.GOD_TIER, "Knife Barrage Stream", "Throws a barrage of multiple piercing knives in a laser formation."),
                SynergyInfo("C Section", SynergyRating.GOD_TIER, "Fetuses Wielding Knives", "Ghost fetuses hold Mom's Knives and stab enemies continuously!"),
                SynergyInfo("Tiny Planet", SynergyRating.EXCELLENT, "Orbiting Blender", "Knives orbit around Isaac, creating a continuous blender shield.")
            )
        ),
        IsaacItem(
            id = 628,
            name = "Death Certificate",
            quote = "Where are we?",
            description = "Single-use active item that transports Isaac to a grand secret realm containing EVERY single item in the entire game on pedestals to pick one.",
            quality = 4,
            itemType = ItemType.ACTIVE,
            recharge = "Single Use",
            itemPools = listOf("Secret Room"),
            iconEmoji = "📜",
            dlc = "Repentance",
            synergies = listOf(
                SynergyInfo("Rock Bottom", SynergyRating.GOD_TIER, "Pick Missing God Synergy", "Select Sacred Heart, C Section, or Brimstone to instantly win the run."),
                SynergyInfo("R Key", SynergyRating.GOD_TIER, "Restart Victory Lap", "Use R Key after collecting Death Certificate to pick another god item.")
            )
        ),
        IsaacItem(
            id = 420,
            name = "Psy Fly",
            quote = "Swat swat",
            description = "Familiar that flies around Isaac and automatically intercepts, reflects, and neutralizes incoming enemy projectiles into homing tears.",
            quality = 4,
            itemType = ItemType.FAMILIAR,
            itemPools = listOf("Treasure Room"),
            transformations = listOf("Lord of the Flies"),
            iconEmoji = "🪰",
            dlc = "Repentance",
            synergies = listOf(
                SynergyInfo("BFFS!", SynergyRating.GOD_TIER, "Instant Bullet Eraser", "Psy Fly becomes bigger, intercepts twice as fast, and protects against Hush/Delirium attacks entirely.")
            )
        ),
        IsaacItem(
            id = 621,
            name = "Mega Mush",
            quote = "Feel like a monster",
            description = "Active item. Isaac grows gigantic for 30 seconds, gains +4x DMG multiplier, invulnerability, and destroys obstacles/bosses by walking into them.",
            quality = 4,
            itemType = ItemType.ACTIVE,
            recharge = "12 Rooms",
            itemPools = listOf("Treasure Room"),
            transformations = listOf("Fun Guy"),
            iconEmoji = "🍄",
            dlc = "Repentance",
            synergies = listOf(
                SynergyInfo("4.5 Volt", SynergyRating.GOD_TIER, "Endless Mega Form", "Recharges during boss fights simply by stomping the boss!"),
                SynergyInfo("Rock Bottom", SynergyRating.GOD_TIER, "Infinite +4x DMG", "Retains the 4x damage multiplier even when shrinking down.")
            )
        ),
        IsaacItem(
            id = 169,
            name = "Polyphemus",
            quote = "Mega tears",
            description = "Massive damage up (+4 flat + 2x multiplier). Tears become giant and can pierce through enemies if their HP is lower than remaining damage.",
            quality = 4,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Treasure Room"),
            transformations = emptyList(),
            stats = mapOf("Damage" to "+4.0 Flat + 2.0x Mult", "Tears" to "-58% Rate"),
            iconEmoji = "👁️‍🗨️",
            dlc = "Rebirth",
            synergies = listOf(
                SynergyInfo("Soy Milk", SynergyRating.GOD_TIER, "Machine Gun Big Tears", "Neutralizes the heavy tear rate penalty while keeping high base damage."),
                SynergyInfo("Brimstone", SynergyRating.GOD_TIER, "Ultra Wide Laser", "Massive thick blood beam that obliterates entire rooms.")
            )
        ),

        // --- QUALITY 3 ITEMS (TOP TIER) ---
        IsaacItem(
            id = 330,
            name = "Soy Milk",
            quote = "Tears way up + DMG down",
            description = "Increases tear rate by +350% (fire rate cap increased to 120), but decreases individual tear damage by -80%.",
            quality = 2,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Treasure Room"),
            transformations = emptyList(),
            stats = mapOf("Tears" to "x5.5 Fire Rate", "Damage" to "-80%"),
            iconEmoji = "🥛",
            dlc = "Rebirth",
            synergies = listOf(
                SynergyInfo("Brimstone", SynergyRating.GOD_TIER, "Infinite Continuous Laser", "Blood laser charges instantly and stays continuously on screen!"),
                SynergyInfo("Rock Bottom", SynergyRating.GOD_TIER, "Max Fire Rate No Dmg Down", "Get maximum machine-gun fire rate with 100% normal damage!"),
                SynergyInfo("Ipecac", SynergyRating.GOD_TIER, "Grenade Rapid Fire", "Fires rapid explosive poison spit at extreme velocity."),
                SynergyInfo("Jacob's Ladder", SynergyRating.GOD_TIER, "Lightning Tesla Coil", "Spawns continuous electrical storms jumping between all enemies.")
            )
        ),
        IsaacItem(
            id = 12,
            name = "Magic Mushroom",
            quote = "All stats up!",
            description = "+1.5x Damage multiplier, +0.3 Damage, +0.3 Speed, +5.25 Range, +1 Red Heart Container and heals all empty health.",
            quality = 3,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Treasure Room", "Boss Room", "Mushroom Destructible"),
            transformations = listOf("Fun Guy"),
            stats = mapOf("Damage" to "+1.5x Mult", "Speed" to "+0.3", "Range" to "+5.25", "Health" to "+1 Max HP"),
            iconEmoji = "🍄",
            dlc = "Rebirth",
            synergies = listOf(
                SynergyInfo("Cricket's Head", SynergyRating.GOOD, "Damage Stacking Note", "The 1.5x multiplier does not stack with Cricket's Head, but flat damage does.")
            )
        ),
        IsaacItem(
            id = 4,
            name = "Cricket's Head",
            quote = "DMG up",
            description = "+1.5x Damage multiplier and +0.5 flat damage up. Tears gain significant knockback against monsters.",
            quality = 3,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Treasure Room", "Golden Chest"),
            transformations = emptyList(),
            stats = mapOf("Damage" to "+1.5x Mult + 0.5 Flat"),
            iconEmoji = "🐱",
            dlc = "Rebirth",
            synergies = listOf(
                SynergyInfo("Brimstone", SynergyRating.EXCELLENT, "High Damage Beam", "Significantly amplifies tick damage of all lasers.")
            )
        ),
        IsaacItem(
            id = 2,
            name = "The Inner Eye",
            quote = "Triple shot",
            description = "Isaac fires 3 tears at once in a spread, with reduced tear rate.",
            quality = 3,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Treasure Room"),
            transformations = emptyList(),
            stats = mapOf("Tears" to "-49% Rate", "Tears Count" to "3x Spread"),
            iconEmoji = "👁️",
            dlc = "Rebirth",
            synergies = listOf(
                SynergyInfo("Brimstone", SynergyRating.GOD_TIER, "Triple Brimstone", "Fires 3 parallel blood beams across the room.")
            )
        ),
        IsaacItem(
            id = 153,
            name = "Mutant Spider",
            quote = "Quad shot",
            description = "Isaac fires 4 tears at once in a dense spread with decreased tear rate.",
            quality = 3,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Treasure Room"),
            transformations = listOf("Spider Baby"),
            stats = mapOf("Tears" to "-58% Rate", "Tears Count" to "4x Spread"),
            iconEmoji = "🕷️",
            dlc = "Rebirth",
            synergies = listOf(
                SynergyInfo("Brimstone", SynergyRating.GOD_TIER, "Quadruple Laser", "Fires 4 full-power blood lasers simultaneously.")
            )
        ),
        IsaacItem(
            id = 245,
            name = "20/20",
            quote = "Double shot",
            description = "Isaac fires 2 tears simultaneously with slight damage reduction (-20%) and no tear rate penalty.",
            quality = 3,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Treasure Room"),
            transformations = emptyList(),
            stats = mapOf("Tears Count" to "2x Parallel"),
            iconEmoji = "👓",
            dlc = "Rebirth",
            synergies = listOf(
                SynergyInfo("C Section", SynergyRating.GOD_TIER, "Twin Fetuses", "Shoots two fetuses at the same time for double damage.")
            )
        ),
        IsaacItem(
            id = 105,
            name = "The D6",
            quote = "Reroll your destiny",
            description = "Active item. Rerolls any pedestal item in the current room into another random item from the room's item pool.",
            quality = 3,
            itemType = ItemType.ACTIVE,
            recharge = "6 Rooms",
            itemPools = listOf("Treasure Room"),
            iconEmoji = "🎲",
            dlc = "Rebirth",
            synergies = listOf(
                SynergyInfo("The Battery", SynergyRating.EXCELLENT, "Overcharge Rerolls", "Allows holding 2 rerolls at once.")
            )
        ),
        IsaacItem(
            id = 313,
            name = "Holy Mantle",
            quote = "Holy shield",
            description = "Grants a protective holy shield that absorbs the first hit of damage in every single room.",
            quality = 3,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Treasure Room", "Angel Room"),
            transformations = listOf("Seraphim"),
            iconEmoji = "🛡️",
            dlc = "Rebirth",
            synergies = listOf(
                SynergyInfo("Curse Rooms", SynergyRating.GOD_TIER, "Free Entry & Exit", "Enter and leave curse rooms without taking spike damage!")
            )
        ),
        IsaacItem(
            id = 224,
            name = "Cricket's Body",
            quote = "Splash damage",
            description = "+0.5 Tear delay up, -10 Range. Tears split into 4 smaller tears upon hitting enemies or obstacles.",
            quality = 3,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Treasure Room"),
            transformations = emptyList(),
            stats = mapOf("Tears" to "+0.5 Rate", "Range" to "-10.0"),
            iconEmoji = "🦗",
            dlc = "Rebirth",
            synergies = listOf(
                SynergyInfo("Compound Fracture", SynergyRating.GOD_TIER, "Fractal Tear Cascade", "Tears split continuously on every bounce, showering rooms with tears.")
            )
        ),
        IsaacItem(
            id = 374,
            name = "Holy Light",
            quote = "Holy shot",
            description = "Chance to shoot a radiant tear that summons a beam of holy light upon impact dealing 4x Isaac's damage.",
            quality = 3,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Angel Room", "Treasure Room"),
            transformations = listOf("Seraphim"),
            iconEmoji = "⚡",
            dlc = "Afterbirth",
            synergies = listOf(
                SynergyInfo("Soy Milk", SynergyRating.GOD_TIER, "Holy Light Cascade", "Massive fire rate triggers hundreds of holy light pillars per second!")
            )
        ),
        IsaacItem(
            id = 494,
            name = "Jacob's Ladder",
            quote = "Electric tears",
            description = "Tears spark with electricity on impact, releasing arcs of lightning that shock other nearby enemies.",
            quality = 3,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Treasure Room"),
            transformations = emptyList(),
            iconEmoji = "⚡",
            dlc = "Afterbirth+",
            synergies = listOf(
                SynergyInfo("Tech X", SynergyRating.GOD_TIER, "Chain Lightning Rings", "Expanding rings continuously discharge lightning to all enemies.")
            )
        ),
        IsaacItem(
            id = 145,
            name = "Guppy's Head",
            quote = "Reusable fly hive",
            description = "Active item. Spawns 2 to 4 blue attack flies that deal 2x Isaac's damage on impact. Counts 1/3 toward Guppy transformation!",
            quality = 3,
            itemType = ItemType.ACTIVE,
            recharge = "1 Room",
            itemPools = listOf("Devil Room", "Red Chest", "Curse Room"),
            transformations = listOf("Guppy"),
            iconEmoji = "🐱",
            dlc = "Rebirth",
            synergies = listOf(
                SynergyInfo("Hive Mind", SynergyRating.GOD_TIER, "Giant Super Flies", "Doubles the damage and size of all spawned attack flies.")
            )
        ),
        IsaacItem(
            id = 134,
            name = "Guppy's Tail",
            quote = "Cursed?",
            description = "Increases chance of finding Golden Chests and Red Chests to 33%, while decreasing normal room drops. Counts 1/3 toward Guppy!",
            quality = 2,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Devil Room", "Red Chest", "Curse Room"),
            transformations = listOf("Guppy"),
            iconEmoji = "🐈",
            dlc = "Rebirth",
            synergies = listOf(
                SynergyInfo("Left Hand", SynergyRating.GOD_TIER, "All Red Chests", "Converts all chests to Red Chests to quickly complete Guppy!")
            )
        ),
        IsaacItem(
            id = 81,
            name = "Dead Cat",
            quote = "9 lives",
            description = "Sets Isaac to 1 Red Heart Container and grants 9 extra lives. Respawn in previous room upon death. Counts 1/3 toward Guppy!",
            quality = 3,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Devil Room", "Red Chest", "Curse Room"),
            transformations = listOf("Guppy"),
            stats = mapOf("Lives" to "9 Resurrections"),
            iconEmoji = "☠️",
            dlc = "Rebirth",
            synergies = listOf(
                SynergyInfo("Guppy's Head", SynergyRating.GOD_TIER, "Guppy Transformation", "Combine 3 Guppy pieces to transform into flying Guppy spawning flies on every tear hit!")
            )
        ),
        IsaacItem(
            id = 5,
            name = "My Reflection",
            quote = "Boomerang tears",
            description = "Tears travel forward then boomerang back towards Isaac with increased range and shot speed.",
            quality = 1,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Treasure Room"),
            transformations = emptyList(),
            iconEmoji = "🪞",
            dlc = "Rebirth",
            synergies = listOf(
                SynergyInfo("Ipecac", SynergyRating.ANTI_SYNERGY, "Dangerous Self Damage", "Explosive tears boomerang directly back into your character!"),
                SynergyInfo("Tiny Planet", SynergyRating.SITUATIONAL, "Chaotic Whirlwind", "Tears swirl unpredictably around Isaac in erratic loops.")
            )
        ),
        IsaacItem(
            id = 233,
            name = "Tiny Planet",
            quote = "Orbiting tears",
            description = "Tears revolve around Isaac in an orbital path with huge spectral and range buffs.",
            quality = 1,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Treasure Room"),
            transformations = emptyList(),
            iconEmoji = "🪐",
            dlc = "Rebirth",
            synergies = listOf(
                SynergyInfo("Brimstone", SynergyRating.GOD_TIER, "Ring of Blood", "Brimstone laser wraps into a permanent shield ring orbiting Isaac."),
                SynergyInfo("Technology", SynergyRating.GOD_TIER, "Laser Halo", "Continuous laser orbits Isaac.")
            )
        ),
        IsaacItem(
            id = 52,
            name = "Dr. Fetus",
            quote = "Boom!",
            description = "Isaac shoots controllable rolling bombs instead of tears that deal 5x damage and explode after 1.5 seconds.",
            quality = 3,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Treasure Room"),
            transformations = emptyList(),
            iconEmoji = "💣",
            dlc = "Rebirth",
            synergies = listOf(
                SynergyInfo("Pyromaniac", SynergyRating.GOD_TIER, "Immortal Healing", "Bombs explode on Isaac and fully restore your health!"),
                SynergyInfo("Brimstone", SynergyRating.GOD_TIER, "Cross Laser Bombs", "Bombs shoot 4 Brimstone beams in cardinal directions on explosion!")
            )
        ),
        IsaacItem(
            id = 223,
            name = "Pyromaniac",
            quote = "It hurts so good",
            description = "Grants +5 bombs. Any explosion damage heals Isaac for 1 full red heart instead of taking damage.",
            quality = 4,
            itemType = ItemType.PASSIVE,
            itemPools = listOf("Treasure Room"),
            transformations = emptyList(),
            iconEmoji = "❤️‍🔥",
            dlc = "Rebirth",
            synergies = listOf(
                SynergyInfo("Ipecac", SynergyRating.GOD_TIER, "Infinite Self-Heal Weapon", "Every tear you fire can be used to instantly heal your health!"),
                SynergyInfo("Dr. Fetus", SynergyRating.GOD_TIER, "Infinite Bomb Heals", "Drop bombs at your feet to reach full health effortlessly.")
            )
        )
    )

    fun findItemByName(query: String): IsaacItem? {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isEmpty()) return null

        // Exact match
        items.find { it.name.lowercase() == cleanQuery }?.let { return it }

        // Contains match
        items.find { it.name.lowercase().contains(cleanQuery) || cleanQuery.contains(it.name.lowercase()) }?.let { return it }

        // Alias / common query normalization
        val aliasMap = mapOf(
            "brim" to "Brimstone",
            "sacred" to "Sacred Heart",
            "fetus" to "C Section",
            "csection" to "C Section",
            "c-section" to "C Section",
            "tech" to "Tech X",
            "techx" to "Tech X",
            "god" to "Godhead",
            "ipecac" to "Ipecac",
            "rock" to "Rock Bottom",
            "spindown" to "Spindown Dice",
            "spin down" to "Spindown Dice",
            "knife" to "Mom's Knife",
            "moms knife" to "Mom's Knife",
            "fly" to "Psy Fly",
            "psyfly" to "Psy Fly",
            "mush" to "Mega Mush",
            "magic mush" to "Magic Mushroom",
            "mushroom" to "Magic Mushroom",
            "cricket" to "Cricket's Head",
            "crickets head" to "Cricket's Head",
            "poly" to "Polyphemus",
            "soy" to "Soy Milk",
            "soymilk" to "Soy Milk",
            "d6" to "The D6",
            "mantle" to "Holy Mantle",
            "holy" to "Holy Mantle",
            "jacob" to "Jacob's Ladder",
            "dead cat" to "Dead Cat",
            "guppy" to "Guppy's Head",
            "pyro" to "Pyromaniac"
        )

        for ((alias, canonical) in aliasMap) {
            if (cleanQuery.contains(alias)) {
                return items.find { it.name == canonical }
            }
        }

        return null
    }

    fun calculateSynergies(candidate: IsaacItem, currentRun: List<IsaacItem>): List<ActiveRunSynergy> {
        val result = mutableListOf<ActiveRunSynergy>()

        for (item in currentRun) {
            // Check candidate synergies with item in inventory
            val candidateSynergy = candidate.synergies.find {
                it.partnerItemName.equals(item.name, ignoreCase = true)
            }
            if (candidateSynergy != null) {
                result.add(
                    ActiveRunSynergy(
                        itemA = candidate.name,
                        itemB = item.name,
                        rating = candidateSynergy.rating,
                        title = candidateSynergy.title,
                        description = candidateSynergy.description
                    )
                )
            }

            // Also check inverse synergies
            val inventorySynergy = item.synergies.find {
                it.partnerItemName.equals(candidate.name, ignoreCase = true)
            }
            if (inventorySynergy != null && result.none { it.itemA == item.name && it.itemB == candidate.name || it.itemA == candidate.name && it.itemB == item.name }) {
                result.add(
                    ActiveRunSynergy(
                        itemA = item.name,
                        itemB = candidate.name,
                        rating = inventorySynergy.rating,
                        title = inventorySynergy.title,
                        description = inventorySynergy.description
                    )
                )
            }
        }

        return result
    }

    fun calculateTransformations(currentRun: List<IsaacItem>): List<TransformationProgress> {
        val transformMap = mapOf(
            "Guppy" to Pair("Transform into Guppy: grants flight and spawns blue flies on every tear hit!", "🐱"),
            "Seraphim" to Pair("Transform into Angel: grants flight and +3 Soul Hearts!", "👼"),
            "Leviathan" to Pair("Transform into Demon: grants flight and +2 Black Hearts!", "😈"),
            "Conjoined" to Pair("Transform into Conjoined: fires 3 tears diagonally for wider spread!", "👶"),
            "Fun Guy" to Pair("Transform into Fun Guy: grants +1 Red Heart Container!", "🍄"),
            "Lord of the Flies" to Pair("Transform into Beelzebub: grants flight and turns enemy flies into friendly blue flies!", "🪰"),
            "Spun" to Pair("Transform into Spun: +2.0 Damage up and +0.15 Speed up!", "💉")
        )

        return transformMap.map { (name, info) ->
            val owned = currentRun.filter { it.transformations.contains(name) }.map { it.name }.distinct()
            TransformationProgress(
                name = name,
                currentCount = owned.size,
                requiredCount = 3,
                itemsOwned = owned,
                rewardEffect = info.first,
                emoji = info.second
            )
        }.filter { it.currentCount > 0 }
    }

    fun getXboxPresets(): List<XboxPresetScreen> = listOf(
        XboxPresetScreen(
            title = "Xbox Devil Deal Pedestal",
            roomType = "Devil Room Offering",
            itemName = "Brimstone",
            consolePrompt = "Pedestal showing glowing black horns and blood sigil on Xbox TV screen.",
            iconEmoji = "🩸",
            hint = "Quality 4 • Blood Laser Beam"
        ),
        XboxPresetScreen(
            title = "Xbox Angel Room Pedestal",
            roomType = "Angel Room Statue Reward",
            itemName = "Sacred Heart",
            consolePrompt = "Pedestal showing glowing bleeding winged heart with divine halo on TV.",
            iconEmoji = "❤️",
            hint = "Quality 4 • x2.3 Homing Tears"
        ),
        XboxPresetScreen(
            title = "Xbox Treasure Room Pedestal",
            roomType = "Treasure Room Item",
            itemName = "C Section",
            consolePrompt = "Pedestal showing umbilical baby fetus icon in glass on console display.",
            iconEmoji = "👶",
            hint = "Quality 4 • Piercing Ghost Fetuses"
        ),
        XboxPresetScreen(
            title = "Xbox Secret Room Pedestal",
            roomType = "Secret Room Discovery",
            itemName = "Rock Bottom",
            consolePrompt = "Pedestal showing gray jagged bottom rock with upward arrow on Xbox screen.",
            iconEmoji = "🪨",
            hint = "Quality 4 • Stat Decreases Locked"
        ),
        XboxPresetScreen(
            title = "Xbox Boss Rush Pedestal",
            roomType = "Boss Rush Choice",
            itemName = "Soy Milk",
            consolePrompt = "Pedestal showing white carton of soy milk with tears aura on TV.",
            iconEmoji = "🥛",
            hint = "Quality 2 • x5.5 Fire Rate"
        ),
        XboxPresetScreen(
            title = "Xbox Item Room Pedestal",
            roomType = "Treasure Room Item",
            itemName = "Ipecac",
            consolePrompt = "Pedestal showing small green bottle of explosive vomit medicine on Xbox.",
            iconEmoji = "🤢",
            hint = "Quality 4 • +40 Explosive Spit"
        )
    )
}
