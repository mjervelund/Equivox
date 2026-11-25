package com.yaskulsky.equivox.emc.mappers;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.List;
import java.util.Map;
import com.yaskulsky.equivox.api.mapper.arithmetic.IValueArithmetic;
import com.yaskulsky.equivox.api.mapper.collector.IExtendedMappingCollector;
import com.yaskulsky.equivox.emc.SimpleGraphMapper;
import com.yaskulsky.equivox.emc.arithmetic.HiddenBigFractionArithmetic;
import com.yaskulsky.equivox.emc.collector.LongToBigFractionCollector;
import com.yaskulsky.equivox.emc.generator.BigFractionToLongGenerator;
import org.apache.commons.math3.fraction.BigFraction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Material forms mapper")
class MaterialFormsMapperTest {

	private BigFractionToLongGenerator<String> valueGenerator;
	private IExtendedMappingCollector<String, Long, IValueArithmetic<BigFraction>> collector;

	@BeforeEach
	void setup() {
		SimpleGraphMapper<String, BigFraction, IValueArithmetic<BigFraction>> mapper = new SimpleGraphMapper<>(new HiddenBigFractionArithmetic());
		valueGenerator = new BigFractionToLongGenerator<>(mapper);
		collector = new LongToBigFractionCollector<>(mapper);
	}

	private static final Map<String, List<String>> TAGS = Map.ofEntries(
			Map.entry("c:ingots/iron", List.of("iron_ingot")),
			Map.entry("c:ingots/tin", List.of("mek_tin_ingot", "rc_tin_ingot")),
			Map.entry("c:dusts/iron", List.of("iron_dust")),
			Map.entry("c:dusts/tin", List.of("tin_dust")),
			Map.entry("c:raw_materials/iron", List.of("raw_iron")),
			Map.entry("c:ores/iron", List.of("iron_ore", "deepslate_iron_ore")),
			Map.entry("c:ores/tin", List.of("tin_ore")),
			Map.entry("c:plates/iron", List.of("iron_plate")),
			Map.entry("mekanism:clumps/iron", List.of("iron_clump")),
			Map.entry("mekanism:crystals/iron", List.of("iron_crystal")),
			Map.entry("c:gems/diamond", List.of("diamond")),
			Map.entry("c:dusts/diamond", List.of("diamond_dust")),
			Map.entry("c:ores/diamond", List.of("diamond_ore")),
			Map.entry("c:dusts/redstone", List.of("redstone")),
			Map.entry("c:ores/redstone", List.of("redstone_ore")),
			Map.entry("c:storage_blocks/coal", List.of("coal_block")),
			Map.entry("c:ores/coal", List.of("coal_ore")),
			Map.entry("c:dusts/coal", List.of("coal_dust")),
			Map.entry("c:dusts/lithium", List.of("lithium_dust")),
			Map.entry("c:dusts", List.of("iron_dust", "tin_dust", "diamond_dust")),
			Map.entry("c:dusts/iron/extra", List.of("odd_dust")),
			Map.entry("c:nuggets/iron", List.of("iron_nugget"))
	);

	private Object2LongMap<String> map() {
		collector.setValueBefore("iron_ingot", 256L);
		collector.setValueBefore("mek_tin_ingot", 256L);
		collector.setValueBefore("rc_tin_ingot", 256L);
		collector.setValueBefore("diamond", 8192L);
		collector.setValueBefore("redstone", 64L);
		collector.setValueBefore("coal_block", 1152L);
		Assertions.assertTrue(MaterialFormsMapper.addConversions(collector, TAGS) > 0);
		return valueGenerator.generateValues();
	}

	@Test
	@DisplayName("Dusts, raw materials, ores, plates and Mekanism stages are worth one ingot")
	void formsWorthOneIngot() {
		Object2LongMap<String> values = map();
		for (String item : List.of("iron_dust", "raw_iron", "iron_ore", "deepslate_iron_ore", "iron_plate", "iron_clump", "iron_crystal",
				"tin_dust", "tin_ore")) {
			Assertions.assertEquals(256, values.getLong(item), item);
		}
	}

	@Test
	@DisplayName("Gems are the unit when there is no ingot, dusts when there is neither (not for dusts), a ninth of the block last")
	void unitFallbacks() {
		Object2LongMap<String> values = map();
		Assertions.assertEquals(8192, values.getLong("diamond_dust"));
		Assertions.assertEquals(8192, values.getLong("diamond_ore"));
		Assertions.assertEquals(64, values.getLong("redstone_ore"));
		Assertions.assertEquals(128, values.getLong("coal_ore"));
		Assertions.assertEquals(128, values.getLong("coal_dust"));
		Assertions.assertFalse(values.containsKey("lithium_dust"), "no unit, no value");
	}

	@Test
	@DisplayName("Other tags are left alone")
	void otherTagsIgnored() {
		Object2LongMap<String> values = map();
		Assertions.assertFalse(values.containsKey("odd_dust"));
		Assertions.assertFalse(values.containsKey("iron_nugget"));
	}

	@Test
	@DisplayName("A cheaper recipe wins over the form conversion")
	void cheaperRecipeWins() {
		collector.addConversion(1, "iron_plate", List.of("raw_iron"));
		collector.setValueBefore("raw_iron", 100L);
		Object2LongMap<String> values = map();
		Assertions.assertEquals(100, values.getLong("iron_plate"));
	}

	@Test
	@DisplayName("A blacklist (value after 0) still zeroes ores")
	void blacklistWins() {
		collector.setValueAfter("iron_ore", 0L);
		Object2LongMap<String> values = map();
		Assertions.assertEquals(0, values.getLong("iron_ore"));
		Assertions.assertEquals(256, values.getLong("iron_dust"));
	}
}
