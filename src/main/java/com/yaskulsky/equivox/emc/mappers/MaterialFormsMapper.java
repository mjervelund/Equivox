package com.yaskulsky.equivox.emc.mappers;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.yaskulsky.equivox.api.mapper.EMCMapper;
import com.yaskulsky.equivox.api.mapper.IEMCMapper;
import com.yaskulsky.equivox.api.mapper.collector.IMappingCollector;
import com.yaskulsky.equivox.api.nss.NSSItem;
import com.yaskulsky.equivox.api.nss.NormalizedSimpleStack;
import com.yaskulsky.equivox.config.PEConfigTranslations;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Gives the other forms of a material the value of one unit of that material, using the common tags: an item in
 * {@code c:dusts/<m>}, {@code c:raw_materials/<m>}, {@code c:ores/<m>}, {@code c:plates/<m>} or one of Mekanism's ore
 * processing stages ({@code mekanism:dirty_dusts|clumps|shards|crystals/<m>}) can be made from one item of
 * {@code c:ingots/<m>}, else {@code c:gems/<m>}, else (not for dusts) {@code c:dusts/<m>}, else a ninth of
 * {@code c:storage_blocks/<m>}.
 * <p>
 * These forms usually come from machine recipes the recipe mappers can't read (crushers, enrichment, rolling), so without
 * this they have no value. The conversions only go from the unit to the form, so a form that has a cheaper recipe keeps
 * it. Ores and raw materials stay at 0 while the ore and raw ore blacklist mappers are enabled.
 */
@EMCMapper
public class MaterialFormsMapper implements IEMCMapper<NormalizedSimpleStack, Long> {

	@EMCMapper.Instance
	public static final MaterialFormsMapper INSTANCE = new MaterialFormsMapper();

	/** Tag folders whose items are worth one unit of their material. */
	static final List<String> FORMS = List.of("c:dusts", "c:raw_materials", "c:ores", "c:plates",
			"mekanism:dirty_dusts", "mekanism:clumps", "mekanism:shards", "mekanism:crystals");
	private static final String INGOTS = "c:ingots", GEMS = "c:gems", DUSTS = "c:dusts", STORAGE_BLOCKS = "c:storage_blocks";

	@Override
	public void addMappings(IMappingCollector<NormalizedSimpleStack, Long> mapper, ReloadableServerResources serverResources,
			RegistryAccess registryAccess, ResourceManager resourceManager) {
		Map<String, List<NormalizedSimpleStack>> tags = new HashMap<>();
		registryAccess.lookupOrThrow(Registries.ITEM).listTags().forEach(tag -> {
			String name = tag.key().location().toString();
			if (name.startsWith("c:") || name.startsWith("mekanism:")) {
				tags.put(name, tag.stream().<NormalizedSimpleStack>map(NSSItem::createItem).toList());
			}
		});
		addConversions(mapper, tags);
	}

	/**
	 * Adds the form conversions for the given item tags (tag name to its items).
	 *
	 * @return the number of conversions added
	 */
	static <T> int addConversions(IMappingCollector<T, Long> mapper, Map<String, ? extends Collection<T>> tags) {
		int added = 0;
		for (Map.Entry<String, ? extends Collection<T>> entry : tags.entrySet()) {
			String tag = entry.getKey();
			int slash = tag.lastIndexOf('/');
			if (slash < 0 || !FORMS.contains(tag.substring(0, slash)) || tag.indexOf('/') != slash) {
				//Only <form>/<material>, not the parent tag or deeper ones
				continue;
			}
			String form = tag.substring(0, slash), material = tag.substring(slash + 1);
			int outputCount = 1;
			Collection<T> units = nonEmpty(tags.get(INGOTS + "/" + material));
			if (units == null) {
				units = nonEmpty(tags.get(GEMS + "/" + material));
			}
			if (units == null && !form.equals(DUSTS)) {
				units = nonEmpty(tags.get(DUSTS + "/" + material));
			}
			if (units == null) {
				units = nonEmpty(tags.get(STORAGE_BLOCKS + "/" + material));
				outputCount = 9;
			}
			if (units == null) {
				continue;
			}
			for (T output : entry.getValue()) {
				for (T unit : units) {
					if (!unit.equals(output)) {
						Object2IntMap<T> ingredients = new Object2IntArrayMap<>(1);
						ingredients.put(unit, 1);
						mapper.addConversion(outputCount, output, ingredients);
						added++;
					}
				}
			}
		}
		return added;
	}

	private static <T> Collection<T> nonEmpty(Collection<T> items) {
		return items == null || items.isEmpty() ? null : items;
	}

	@Override
	public String getName() {
		return PEConfigTranslations.MAPPING_MATERIAL_FORMS_MAPPER.title();
	}

	@Override
	public String getTranslationKey() {
		return PEConfigTranslations.MAPPING_MATERIAL_FORMS_MAPPER.getTranslationKey();
	}

	@Override
	public String getDescription() {
		return PEConfigTranslations.MAPPING_MATERIAL_FORMS_MAPPER.tooltip();
	}
}
