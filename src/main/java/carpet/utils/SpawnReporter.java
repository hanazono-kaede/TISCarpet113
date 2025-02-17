
package carpet.utils;

import java.util.List;

import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.Tuple;
import net.minecraft.util.registry.IRegistry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;

import net.minecraft.item.EnumDyeColor;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.biome.Biome;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Entity;

import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.world.gen.Heightmap;

import java.lang.Math;

public class SpawnReporter
{
    public static String [] mob_groups = {"hostile","passive","water","ambient"};
    public static boolean mock_spawns = false;
    
    public static Long track_spawns = 0L;
    public static final HashMap<Integer, HashMap<EnumCreatureType, Tuple<Integer,Integer>>> mobcaps = new HashMap<>();
    public static final HashMap<String, HashMap<String,Long>> spawn_stats = new HashMap<String, HashMap<String,Long>>();
    public static double mobcap_exponent = 0.0D;
    
    public static final HashMap<String, Long> spawn_attempts = new HashMap<String, Long>();
    public static final HashMap<String, Long> overall_spawn_ticks = new HashMap<String, Long>();
    public static final HashMap<String, Long> spawn_ticks_full = new HashMap<String, Long>();
    public static final HashMap<String, Long> spawn_ticks_fail = new HashMap<String, Long>();
    public static final HashMap<String, Long> spawn_ticks_succ = new HashMap<String, Long>();
    public static final HashMap<String, Long> spawn_ticks_spawns = new HashMap<String, Long>();
    public static final HashMap<String, Long> spawn_cap_count = new HashMap<String, Long>();
    public static class SpawnPos
    {
        public String mob;
        public BlockPos pos;
        public SpawnPos(String mob, BlockPos pos)
        {
            this.mob = mob;
            this.pos = pos;
        }
    }
    public static final HashMap<String, EvictingQueue<SpawnPos, Integer>> spawned_mobs = new HashMap<>();
    public static final HashMap<String, Integer> spawn_tries = new HashMap<>();
    public static BlockPos lower_spawning_limit = null;
    public static BlockPos upper_spawning_limit = null;

    static {
        reset_spawn_stats(true);
    }

    public static void registerSpawn(EntityLiving el, String type, String mob, BlockPos pos) { registerSpawn(el, type, mob, pos, 1L);}
    public static void registerSpawn(EntityLiving el, String type, String mob, BlockPos pos, long value)
    {
        if (lower_spawning_limit != null)
        {
            if (!( (lower_spawning_limit.getX() <= pos.getX() && pos.getX() <= upper_spawning_limit.getX()) &&
                 (lower_spawning_limit.getY() <= pos.getY() && pos.getY() <= upper_spawning_limit.getY()) && 
                 (lower_spawning_limit.getZ() <= pos.getZ() && pos.getZ() <= upper_spawning_limit.getZ())
               ))
            {
                return;
            }
        }
        
        long count = spawn_stats.get(type).getOrDefault(mob, 0L);
        spawn_stats.get(type).put(mob, count + value);
        spawned_mobs.get(type).put(new SpawnPos(mob, new BlockPos(el)), 1);
    }


    public static List<ITextComponent> printMobcapsForDimension(int dim)
    {
        String name = DimensionType.getById(dim).toString();
        List<ITextComponent> lst = new ArrayList<>();
        lst.add(Messenger.s(String.format("Mobcaps for %s:",name)));
        for (EnumCreatureType enumcreaturetype : EnumCreatureType.values())
        {
            String type_code = String.format("%s", enumcreaturetype);
            Tuple<Integer, Integer> stat = mobcaps.get(dim).getOrDefault(enumcreaturetype, new Tuple<>(0,0));
            int cur = stat.getA();
            int max = stat.getB();
            int rounds = spawn_tries.get(type_code);
            lst.add( Messenger.c(String.format("w   %s: ",type_code),
                    (cur+max==0)?"g -/-":String.format("%s %d/%d", (cur >= max)?"r":((cur >= 8*max/10)?"y":"l") ,cur, max),
                    (rounds == 1)?"w ":String.format("fi  (%d rounds/tick)",spawn_tries.get(type_code))
            ));
        }
        return lst;
    }
    
    public static List<ITextComponent> recent_spawns(World world, String creature_type_code)
    {
        
        EnumCreatureType creature_type = get_creature_type_from_code(creature_type_code);
        List<ITextComponent> lst = new ArrayList<>();
        if ((track_spawns == 0L))
        {
            lst.add(Messenger.s("Spawn tracking not started"));
            return lst;
        }
        if (creature_type == null)
        {
            lst.add(Messenger.s(String.format("Incorrect creature type: %s",creature_type_code)));
            return lst;
        }

        String type_code = get_type_string(creature_type);
        
        lst.add(Messenger.s(String.format("Recent %s spawns:",type_code)));
        for (SpawnPos entry: spawned_mobs.get(type_code).keySet())
        {
            lst.add( Messenger.c(String.format("w  - %s ",entry.mob), Messenger.tp("wb",entry.pos)));
        }
        
        if (lst.size()==1)
        {
            lst.add(Messenger.s(" - Nothing spawned yet, sorry."));
        }
        return lst;

    }
    
    public static List<ITextComponent> show_mobcaps(BlockPos pos, World worldIn)
    {
        EnumDyeColor under = WoolTool.getWoolColorAtPosition(worldIn, pos.down());
        if (under == null)
        {
            if (track_spawns > 0L)
            {
                return tracking_report(worldIn);
            }
            else
            {
                //return print_general_mobcaps(worldIn);
            }
        }
        String creature_type = get_type_code_from_wool_code(under);
        if (creature_type != null)
        {
            if (track_spawns > 0L)
            {
                return recent_spawns(worldIn, creature_type);
            }
            else
            {
                return printEntitiesByType(creature_type, worldIn);
                
            }
            
        }
        if (track_spawns > 0L)
        {
            return tracking_report(worldIn);
        }
        else
        {
            return tracking_report(worldIn);
            //// TODO
            //return print_general_mobcaps(worldIn);
        }
        
    }
    
    public static String get_type_code_from_wool_code(EnumDyeColor color)
    {
        switch (color)
        {
            case RED:
                return "hostile";
            case GREEN:
                return "passive";
            case BLUE:
                return "water";
            case BROWN:
                return "ambient";
        }
        return null;
    }
    
    public static EnumCreatureType get_creature_type_from_code(String type_code)
    {
        if ("hostile".equalsIgnoreCase(type_code))
        {
            return EnumCreatureType.MONSTER;
        }
        else if ("passive".equalsIgnoreCase(type_code))
        {
            return EnumCreatureType.CREATURE;
        }
        else if ("water".equalsIgnoreCase(type_code))
        {
            return EnumCreatureType.WATER_CREATURE;
        }
        else if ("ambient".equalsIgnoreCase(type_code))
        {
            return EnumCreatureType.AMBIENT;
        }
        return null;
    }
    
    
    public static String get_type_string(EnumCreatureType type)
    {
        return String.format("%s", type);
    }
    
    public static String get_creature_code_from_string(String str)
    {
        return get_type_string(get_creature_type_from_code(str));
    }
    
    public static List<ITextComponent> printEntitiesByType(String creature_type_code, World worldIn) //Class<?> entityType)
    {
        EnumCreatureType typ = get_creature_type_from_code(creature_type_code);
        List<ITextComponent> lst = new ArrayList<>();
        if (typ == null)
        {
            lst.add(Messenger.c(String.format("r Incorrect creature type: %s",creature_type_code)));
            return lst;
        }
        Class<?> cls = typ.getBaseClass();// getCreatureClass();
        lst.add( Messenger.s(String.format("Loaded entities for %s class:", get_type_string(typ))));
        for (Entity entity : worldIn.loadedEntityList)
        {
            if ((!(entity instanceof EntityLiving) || !((EntityLiving)entity).isNoDespawnRequired()) && cls.isAssignableFrom(entity.getClass()))
            {
                lst.add(Messenger.c("w  - ",
                        Messenger.tp("w", entity.posX, entity.posY, entity.posZ),"w  : "+EntityType.getId(entity.getType())));
            }
        }
        if (lst.size()==1)
        {
            lst.add(Messenger.s(" - Empty."));
        }
        return lst;
    }
    
    public static void initialize_mocking()
    {
        reset_spawn_stats(false);
        mock_spawns = true;
        
    }
    public static void stop_mocking()
    {
        reset_spawn_stats(false);
        mock_spawns = false;
        
    }
    public static void reset_spawn_stats(boolean full)
    {
        spawn_stats.clear();
        spawned_mobs.clear();
        for (EnumCreatureType enumcreaturetype : EnumCreatureType.values())
        {
            String type_code = String.format("%s", enumcreaturetype);
            if (full)
            {
                spawn_tries.put(type_code, 1);
            }
            for (String suffix: new String[] {""," (N)"," (E)"})
            {
                String code = type_code+suffix;
                overall_spawn_ticks.put(code, 0L);
                spawn_attempts.put(code, 0L);
                spawn_ticks_full.put(code, 0L);
                spawn_ticks_fail.put(code, 0L);
                spawn_ticks_succ.put(code, 0L);
                spawn_ticks_spawns.put(code, 0L);
                spawn_cap_count.put(code, 0L);
            }
            
            spawn_stats.put(type_code, new HashMap<>());
            spawned_mobs.put(type_code, new EvictingQueue<>());
        }
        mobcaps.put(-1,new HashMap<>());
        mobcaps.put(0,new HashMap<>());
        mobcaps.put(1,new HashMap<>());
        track_spawns = 0L;
    }
    
    public static List<ITextComponent> tracking_report(World worldIn)
    {
        List<ITextComponent> report = new ArrayList<>();
        if (track_spawns == 0L)
        {
            report.add(Messenger.c(
                    "w Spawn tracking disabled, type '",
                    "wi /spawn tracking start","/spawn tracking start",
                    "w ' to enable"));
            return report;
        }
        Long duration = (long) worldIn.getServer().getTickCounter() - track_spawns;
        report.add(Messenger.c("bw --------------------"));
        String simulated = mock_spawns?"[SIMULATED] ":"";
        String location = (lower_spawning_limit != null)?String.format("[in (%d, %d, %d)x(%d, %d, %d)]",
                lower_spawning_limit.getX(),lower_spawning_limit.getY(),lower_spawning_limit.getZ(),
                upper_spawning_limit.getX(),upper_spawning_limit.getY(),upper_spawning_limit.getZ() ):"";
        report.add(Messenger.s(String.format("%sSpawn statistics %s: for %.1f min", simulated, location, (duration/72000.0)*60)));
        for (EnumCreatureType enumcreaturetype : EnumCreatureType.values())
        {
            String type_code = String.format("%s", enumcreaturetype);
            boolean there_are_mobs_to_list = false;
            for (String world_code: new String[] {"", " (N)", " (E)"})  
            {
                String code = type_code+world_code;
                if (spawn_ticks_spawns.get(code) > 0L)
                {
                    there_are_mobs_to_list = true;
                    double hours = overall_spawn_ticks.get(code)/72000.0;
                    double full = (100.0D*spawn_ticks_full.get(code))/ spawn_attempts.get(code);
                    double fail = (100.0D*spawn_ticks_fail.get(code))/ spawn_attempts.get(code);
                    double succ = (100.0D*spawn_ticks_succ.get(code))/ spawn_attempts.get(code);
                    report.add(Messenger.c(
                            String.format("w  > %s (%.1f min), %.1f m/t, {", code, 60*hours, (1.0D*spawn_cap_count.get(code))/ spawn_attempts.get(code)),
                            Messenger.c(
                                    Messenger.fancy(null, Messenger.s(String.format("%.1f%%F", full)), Messenger.s(String.format("%.6f%% Full", full)), null),
                                    "w  / ",
                                    Messenger.fancy(null, Messenger.s(String.format("%.1f%%-", fail)), Messenger.s(String.format("%.6f%% Fail", fail)), null),
                                    "w  / ",
                                    Messenger.fancy(null, Messenger.s(String.format("%.1f%%+", succ)), Messenger.s(String.format("%.6f%% Success", succ)), null)
                            ),
                            String.format("w }; %.2f s/att", (1.0D*spawn_ticks_spawns.get(code))/(spawn_ticks_fail.get(code)+spawn_ticks_succ.get(code)))
                    ));
                }
            }
            if (there_are_mobs_to_list)
            {
                for (String creature_name : spawn_stats.get(type_code).keySet())
                {
                    report.add(Messenger.s(String.format("   - %s: %d spawns, %d per hour",
                            creature_name,
                            spawn_stats.get(type_code).get(creature_name),
                            (72000 * spawn_stats.get(type_code).get(creature_name)/duration ))));
                }
            }
        }
        return report;
    }

    

    public static void killEntity(EntityLiving entity)
    {
        if (entity.isPassenger())
        {
            entity.getRidingEntity().remove();
        }
        if (entity.isBeingRidden())
        {
            for (Entity e: entity.getPassengers())
            {
                e.remove();
            }
        }
        if (entity instanceof EntityOcelot)
        {
            for (Entity e: entity.getEntityWorld().getEntitiesWithinAABB(EntityOcelot.class, entity.getBoundingBox()))
            {
                e.remove();
            }
        }
        entity.remove();
    }

    public static List<ITextComponent> report(BlockPos pos, World worldIn)
    {
        List<ITextComponent> rep = new ArrayList<>();
        int x = pos.getX(); int y = pos.getY(); int z = pos.getZ();
        Chunk chunk = worldIn.getChunk(pos);
        int lc = chunk.getTopBlockY(Heightmap.Type.LIGHT_BLOCKING, x, z) + 1;
        String where = String.format((y >= lc) ? "%d blocks above it." : "%d blocks below it.",  MathHelper.abs(y-lc));
        if (y == lc) where = "right at it.";
        rep.add(Messenger.s(String.format("Maximum spawn Y value for (%+d, %+d) is %d. You are "+where, x, z, lc )));
        rep.add(Messenger.s("Spawns:"));
        for (EnumCreatureType enumcreaturetype : EnumCreatureType.values())
        {
            String type_code = String.format("%s", enumcreaturetype).substring(0, 3);
            List<Biome.SpawnListEntry> lst = ((ChunkProviderServer)worldIn.getChunkProvider()).getPossibleCreatures(enumcreaturetype, pos);
            if (lst != null && !lst.isEmpty())
            {
                for (Biome.SpawnListEntry animal : lst)
                {
                    boolean canspawn = WorldEntitySpawner.canCreatureTypeSpawnAtLocation(EntitySpawnPlacementRegistry.getPlacementType(animal.entityType), worldIn, pos, animal.entityType);
                    int will_spawn = -1;
                    boolean fits = false;
                    boolean fits1 = false;
                    
                    EntityLiving entityliving;
                    try
                    {
                        entityliving = animal.entityType.create(worldIn);
                    }
                    catch (Exception exception)
                    {
                        exception.printStackTrace();
                        return rep;
                    }
                    
                    boolean fits_true = false;
                    boolean fits_false = false;
                    
                    if (canspawn)
                    {
                        will_spawn = 0;
                        for (int attempt = 0; attempt < 50; ++attempt)
                        {
                            float f = (float)x + 0.5F;
                            float f1 = (float)z + 0.5F;
                            entityliving.setLocationAndAngles((double)f, (double)y, (double)f1, worldIn.rand.nextFloat() * 360.0F, 0.0F);
                            fits1 = entityliving.isNotColliding();
                            
                            for (int i = 0; i < 20; ++i)
                            {
                                if (entityliving.canSpawn(worldIn, false))
                                {
                                    will_spawn += 1;
                                }
                            }
                            entityliving.onInitialSpawn(worldIn.getDifficultyForLocation(new BlockPos(entityliving)), null, null);
                            // the code invokes onInitialSpawn after getCanSpawHere
                            fits = fits1 && entityliving.isNotColliding();
                            if (fits)
                            {
                                fits_true = true;
                            }
                            else
                            {
                                fits_false = true;
                            }
                            
                            killEntity(entityliving);
                            
                            try
                            {
                                entityliving = animal.entityType.create(worldIn);
                            }
                            catch (Exception exception)
                            {
                                exception.printStackTrace();
                                return rep;
                            }
                        }
                    }
                    
                    String creature_name = IRegistry.ENTITY_TYPE.getKey(entityliving.getType()).toString().replaceFirst("minecraft:","");
                    String pack_size = String.format("%d", entityliving.getMaxSpawnedInChunk());//String.format("%d-%d", animal.minGroupCount, animal.maxGroupCount);
                    int weight = animal.getItemWeight();
                    if (canspawn)
                    {
                        String c = (fits_true && will_spawn>0)?"e":"gi";
                        rep.add(Messenger.c(
                                String.format("%s %s: %s (%d:%d-%d/%d), can: ",c,type_code,creature_name,weight,animal.minGroupCount, animal.maxGroupCount,  entityliving.getMaxSpawnedInChunk()),
                                "l YES",
                                c+" , fit: ",
                                ((fits_true && fits_false)?"y YES and NO":(fits_true?"l YES":"r NO")),
                                c+" , will: ",
                                ((will_spawn>0)?"l ":"r ")+Math.round((double)will_spawn)/10+"%"
                        ));
                    }
                    else
                    {
                        rep.add(Messenger.c(String.format("gi %s: %s (%d:%d-%d/%d), can: ",type_code,creature_name,weight,animal.minGroupCount, animal.maxGroupCount, entityliving.getMaxSpawnedInChunk()), "n NO"));
                    }
                    killEntity(entityliving);
                }
            }
        }
        return rep;
    }
}