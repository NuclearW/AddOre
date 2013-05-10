package com.nuclearw.addore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;

import org.bukkit.craftbukkit.v1_5_R3.CraftWorld;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import net.minecraft.server.v1_5_R3.WorldGenMinable;

public class AddOre extends JavaPlugin implements Listener, Runnable {
	private static HashMap<String, TreeSet<OreGenerator>> oreGenerators = new HashMap<String, TreeSet<OreGenerator>>();

	private static LinkedBlockingQueue<ChunkLocation> chunksToProcess = new LinkedBlockingQueue<ChunkLocation>();
	private static HashSet<String> processList = new HashSet<String>();
	private static HashSet<String> processedList = new HashSet<String>();

	private static File processedFile;
	private static int processedCount = 0;

	protected final Random random = new Random();

	@Override
	public void onEnable() {
		getDataFolder().mkdir();

		processedFile = new File(getDataFolder(), "addOreProcessed");

		saveDefaultConfig();

		// Load our configs up here

		try {
			Files.touch(processedFile);
			BufferedReader reader = Files.newReader(processedFile, Charsets.UTF_8);

			String line;
			while((line = reader.readLine()) != null) {
				processedList.add(line);
			}

			reader.close();
		} catch(Exception e) {
			e.printStackTrace();
		}

		getServer().getPluginManager().registerEvents(this, this);

		getServer().getScheduler().runTaskTimer(this, this, 20, 20);
	}

	@Override
	public void onDisable() {
		saveProcessed();
	}

	@Override
	public void run() {
		if(chunksToProcess.isEmpty()) return;

		int processed = 0;
		while(!chunksToProcess.isEmpty() && processed < 10) {
			ChunkLocation location = chunksToProcess.poll();

			Set<OreGenerator> generators = oreGenerators.get(location.world).descendingSet();
			for(OreGenerator generator : generators) {
				if(processList.contains(location.toString() + ":" + generator.toString())) {
					continue;
				}

				generator.generate(location);
				processedList.add(location.toString() + ":" + generator.toString());
			}

			processed++;
		}

		if(processed > 0) {
			processedCount++;
		}

		if(processedCount > 10) {
			saveProcessed();
			processedCount = 0;
		}
	}

	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		if(!processList.contains(event.getWorld().getName())) return;

		Set<OreGenerator> generators = oreGenerators.get(event.getWorld().getName());
		String baseLocation = event.getWorld().getName() + ":" + event.getChunk().getX() + ":" + event.getChunk().getZ() + ":";
		for(OreGenerator generator : generators) {
			if(processedList.contains(baseLocation + generator.toString())) {
				continue;
			}

			chunksToProcess.add(new ChunkLocation(event.getWorld().getName(), event.getChunk().getX(), event.getChunk().getZ()));
		}
	}

	private static void saveProcessed() {
		try {
			BufferedWriter writer = Files.newWriter(processedFile, Charsets.UTF_8);
			for(String line : processedList) {
				writer.write(line + "\n");
			}

			writer.flush();
			writer.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private class ChunkLocation {
		protected final int x, z;
		protected final String world;

		protected ChunkLocation(String world, int x, int z) {
			this.x = x;
			this.z = z;
			this.world = world;
		}

		@Override
		public String toString() {
			return world + ":" + x + ":" + z;
		}
	}

	private class OreGenerator implements Comparable<OreGenerator> {
		private final int oreId, spawnableInId, gensPerChunk, maxY, yModifier, maxX, xModifier, maxZ, zModifier, depositSize, runOrder;
		private final WorldGenMinable orePopulator;

		protected OreGenerator(int oreId, int spawnableInId, int gensPerChunk, int maxY, int yModifier, int maxX, int xModifier, int maxZ, int zModifier, int depositSize, int runOrder) {
			this.oreId = oreId;
			this.spawnableInId = spawnableInId;
			this.gensPerChunk = gensPerChunk;
			this.maxY = maxY;
			this.yModifier = yModifier;
			this.maxX = maxX;
			this.xModifier = xModifier;
			this.maxZ = maxZ;
			this.zModifier = zModifier;
			this.depositSize = depositSize;
			this.runOrder = runOrder;

			this.orePopulator = new WorldGenMinable(oreId, depositSize, spawnableInId);
		}

		protected void generate(ChunkLocation location) {
			int cx = location.x * 16;
			int cz = location.z * 16;

			for(int i = 0; i < gensPerChunk; i++) {
	            int x = cx + random.nextInt(maxX) + xModifier;
	            int y = random.nextInt(maxY) + yModifier;
	            int z = cz + random.nextInt(maxZ) + zModifier;
	            orePopulator.a(((CraftWorld) getServer().getWorld(location.world)).getHandle(), random, x, y, z);
	        }
		}

		@Override
		public int compareTo(OreGenerator other) {
			if(runOrder == other.runOrder) {
				return 0;
			} else if(runOrder < other.runOrder) {
				return -10;
			} else {
				return 1;
			}
		}

		@Override
		public String toString() {
			return oreId + ":" + spawnableInId + ":" + gensPerChunk + ":" + maxY + ":" + yModifier + ":" + + maxX + ":" + xModifier + ":" + + maxZ + ":" + zModifier + ":" + depositSize; 
		}
	}
}
