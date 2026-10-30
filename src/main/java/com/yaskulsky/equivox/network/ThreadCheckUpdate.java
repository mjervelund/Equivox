package com.yaskulsky.equivox.network;

import com.yaskulsky.equivox.PECore;
import com.yaskulsky.equivox.utils.text.PELang;
import com.yaskulsky.equivox.utils.text.TextComponentUtil;
import net.minecraft.client.player.LocalPlayer;
import java.lang.reflect.Method;
import java.net.URI;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.util.TimeUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.fml.loading.FMLConfig.ConfigValue;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforgespi.language.IModInfo;
import org.apache.maven.artifact.versioning.ComparableVersion;

@EventBusSubscriber(modid = PECore.MODID, value = Dist.CLIENT)
public class ThreadCheckUpdate extends Thread {

	private static final String curseURL = "https://github.com/Yaskulsky/projecte-26-port/releases";
	private static volatile ComparableVersion target = null;
	private static volatile boolean hasSentMessage = false;

	public ThreadCheckUpdate() {
		this.setName("Equivox Update Checker Notifier");
	}

	@Override
	public void run() {
		if (!FMLConfig.getBoolConfigValue(ConfigValue.VERSION_CHECK)) {
			//Forge update checker disabled, just exit
			return;
		}
		IModInfo info = PECore.MOD_CONTAINER.getModInfo();
		Object result = null;
		try {
			//VersionChecker moved from net.neoforged.fml to net.neoforged.neoforge.common during 26.3 (and dropped Status.PENDING,
			// returning null while the check runs), so look it up by name to work on either side of the move
			Method getResult = versionChecker().getMethod("getResult", IModInfo.class);
			int tries = 0;
			do {
				Object res = getResult.invoke(null, info);
				if (res != null && !"PENDING".equals(enumName(res, "status"))) {
					result = res;
				}
				try {
					Thread.sleep(TimeUtil.MILLISECONDS_PER_SECOND);
				} catch (InterruptedException ignored) {
				}
				tries++;
			} while (result == null && tries < 10);

			if (result == null) {
				PECore.LOGGER.warn("Update check failed.");
				return;
			}

			if ("OUTDATED".equals(enumName(result, "status"))) {
				target = (ComparableVersion) result.getClass().getMethod("target").invoke(result);
			}
		} catch (ReflectiveOperationException e) {
			PECore.LOGGER.warn("Update check failed.", e);
		}
	}

	private static Class<?> versionChecker() throws ClassNotFoundException {
		try {
			return Class.forName("net.neoforged.neoforge.common.VersionChecker");
		} catch (ClassNotFoundException e) {
			return Class.forName("net.neoforged.fml.VersionChecker");
		}
	}

	private static String enumName(Object record, String component) throws ReflectiveOperationException {
		return ((Enum<?>) record.getClass().getMethod(component).invoke(record)).name();
	}

	@SubscribeEvent
	public static void worldLoad(EntityJoinLevelEvent evt) {
		if (evt.getEntity() instanceof LocalPlayer player && target != null && !hasSentMessage) {
			hasSentMessage = true;
			player.sendSystemMessage(PELang.UPDATE_AVAILABLE.translate(target));
			player.sendSystemMessage(PELang.UPDATE_GET_IT.translate());
			player.sendSystemMessage(TextComponentUtil.build(new ClickEvent.OpenUrl(URI.create(curseURL)), curseURL));
		}
	}
}