package pl.granat;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;
import java.util.*;
public final class GranatPlugin extends JavaPlugin implements Listener {
 private NamespacedKey key;
 private final Map<UUID,Long> cooldowns=new HashMap<>();
 public void onEnable(){key=new NamespacedKey(this,"granat");saveDefaultConfig();getServer().getPluginManager().registerEvents(this,this);}
 private ItemStack grenade(int count){
  ItemStack item=new ItemStack(Material.SPLASH_POTION,count);
  PotionMeta meta=(PotionMeta)item.getItemMeta();meta.setBasePotionType(PotionType.SLOW_FALLING);
  meta.setDisplayName(ChatColor.AQUA+"Granat odepchniecia");
  meta.setLore(Arrays.asList(ChatColor.GRAY+"PPM: rzuc granatem",ChatColor.GREEN+"Przedmiot niezuzywalny",ChatColor.YELLOW+"Cooldown: 30 sekund"));
  meta.getPersistentDataContainer().set(key,PersistentDataType.BYTE,(byte)1);item.setItemMeta(meta);return item;
 }
 private boolean isGrenade(ItemStack i){return i!=null&&i.getType()==Material.SPLASH_POTION&&i.hasItemMeta()&&i.getItemMeta().getPersistentDataContainer().has(key,PersistentDataType.BYTE);}
 @Override public boolean onCommand(CommandSender s,Command cmd,String label,String[] args){
  if(!s.hasPermission("granat.admin")){s.sendMessage("Brak uprawnien.");return true;}
  if(args.length<1||args.length>2){s.sendMessage("/granat <gracz> [ilosc 1-64]");return true;}
  Player target=Bukkit.getPlayerExact(args[0]);if(target==null){s.sendMessage("Gracz musi byc online.");return true;}
  int n=1;try{if(args.length==2)n=Integer.parseInt(args[1]);}catch(NumberFormatException e){s.sendMessage("Podaj liczbe 1-64.");return true;}
  if(n<1||n>64){s.sendMessage("Podaj liczbe 1-64.");return true;}
  for(ItemStack rest:target.getInventory().addItem(grenade(n)).values())target.getWorld().dropItemNaturally(target.getLocation(),rest);
  s.sendMessage("Przekazano granat graczowi "+target.getName());return true;
 }
 @EventHandler(priority=EventPriority.HIGH)
 public void onUse(PlayerInteractEvent e){
  if(e.getAction()!=Action.RIGHT_CLICK_AIR&&e.getAction()!=Action.RIGHT_CLICK_BLOCK)return;
  if(!isGrenade(e.getItem()))return;
  if(e.useItemInHand()==Event.Result.DENY)return;
  e.setCancelled(true);
  Player player=e.getPlayer();long now=System.currentTimeMillis();long left=cooldowns.getOrDefault(player.getUniqueId(),0L)-now;
  if(left>0){player.sendMessage(ChatColor.RED+"Granat dostepny za "+((left+999)/1000)+" s.");return;}
  ThrownPotion potion=player.launchProjectile(ThrownPotion.class);
  if(!potion.isValid())return;
  potion.setItem(grenade(1));potion.getPersistentDataContainer().set(key,PersistentDataType.BYTE,(byte)1);
  cooldowns.put(player.getUniqueId(),now+30000L);
 }
 @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
 public void onSplash(PotionSplashEvent e){
  if(!e.getPotion().getPersistentDataContainer().has(key,PersistentDataType.BYTE))return;
  for(LivingEntity entity:e.getAffectedEntities())e.setIntensity(entity,0);
  Location center=e.getPotion().getLocation();
  double radius=Math.max(0.1,getConfig().getDouble("radius",6));
  double horizontal=Math.max(0,getConfig().getDouble("horizontal-force",2.2));
  double vertical=Math.max(0,getConfig().getDouble("vertical-force",0.8));
  center.getWorld().spawnParticle(Particle.EXPLOSION,center,1);center.getWorld().playSound(center,Sound.ENTITY_GENERIC_EXPLODE,1f,1.3f);
  for(Player target:center.getWorld().getPlayers()){
   if(target.isDead()||target.getGameMode()==GameMode.SPECTATOR)continue;
   if(!getConfig().getBoolean("affect-thrower",true)&&target.equals(e.getPotion().getShooter()))continue;
   double distance=target.getLocation().distance(center);if(distance>=radius)continue;
   double scale=1-distance/radius;
   Vector direction=target.getLocation().toVector().subtract(center.toVector()).setY(0);
   if(direction.lengthSquared()<0.0001)direction=target.getLocation().getDirection().setY(0).multiply(-1);
   if(direction.lengthSquared()<0.0001)direction=new Vector(1,0,0);
   Vector velocity=direction.normalize().multiply(horizontal*scale);velocity.setY(vertical*scale);target.setVelocity(velocity);
  }
 }
}
