package io.github.yannici.bedwars.Listener;

import java.util.List;

import io.github.yannici.bedwars.ChatWriter;
import io.github.yannici.bedwars.Main;
import io.github.yannici.bedwars.Utils;
import io.github.yannici.bedwars.Game.Game;
import io.github.yannici.bedwars.Game.GameState;
import io.github.yannici.bedwars.Game.Team;
import io.github.yannici.bedwars.Statistics.PlayerStatistic;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Bed;

import com.google.common.collect.ImmutableMap;

public class BlockListener extends BaseListener {

	public BlockListener() {
		super();
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onBurn(BlockBurnEvent bbe) {
		Block block = bbe.getBlock();
		if(block == null) {
			return;
		}
		
		Game game = Main.getInstance().getGameManager().getGameByLocation(block.getLocation());
		if(game == null) {
			return;
		}
		
		if(game.getState() == GameState.STOPPED) {
			return;
		}
		
		bbe.setCancelled(true);
		return;
	}
	
	@EventHandler
	public void onSpread(BlockSpreadEvent spread) {
	    if(spread.isCancelled()) {
	        return;
	    }
	    
	    Game game = Main.getInstance().getGameManager().getGameByLocation(spread.getBlock().getLocation());
	    if(game == null) {
	        return;
	    }
	    
	    if(game.getState() != GameState.RUNNING) {
	        return;
	    }
	    
	    if(spread.getNewState().getType().equals(Material.FIRE)) {
	    	spread.setCancelled(true);
	    	return;
	    }
	    
	    if(game.getRegion().isPlacedBlock(spread.getSource())) {
	    	game.getRegion().addPlacedBlock(spread.getBlock(), spread.getBlock().getState());
	    } else {
	    	game.getRegion().addPlacedUnbreakableBlock(spread.getBlock(), spread.getBlock().getState());
	    }
	}
	
	@EventHandler
	public void onForm(BlockFormEvent form) {
	    if(form.isCancelled()) {
	        return;
	    }
	    
	    if(form.getNewState().getType() != Material.SNOW) {
	        return;
	    }
	    
	    Game game = Main.getInstance().getGameManager().getGameByLocation(form.getBlock().getLocation());
	    if(game == null) {
	        return;
	    }
	    
	    if(game.getState() == GameState.STOPPED) {
	        return;
	    }
	    
	    form.setCancelled(true);
	}
	
	@EventHandler
	public void onFade(BlockFadeEvent e) {
	    if(e.isCancelled()) {
	        return;
	    }
	    
	    Game game = Main.getInstance().getGameManager().getGameByLocation(e.getBlock().getLocation());
	    if(game == null) {
	        return;
	    }
	    
	    if(game.getState() == GameState.STOPPED) {
	        return;
	    }
	    
	    if(!game.getRegion().isPlacedBlock(e.getBlock())) {
	        e.setCancelled(true);
	    }
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onBreak(BlockBreakEvent e) {
		if(e.isCancelled()) {
			return;
		}
		
		Player p = e.getPlayer();
		if(p == null) {
			Block block = e.getBlock();
			if(block == null) {
				return;
			}
			
			Game game = Main.getInstance().getGameManager().getGameByLocation(block.getLocation());
			if(game == null) {
				return;
			}
			
			if(game.getState() != GameState.RUNNING) {
				return;
			}
			
			e.setCancelled(true);
			return;
		}
		
		Game g = Main.getInstance().getGameManager().getGameOfPlayer(p);
		if (g == null) {
			Block breaked = e.getBlock();
			if (!(breaked.getState() instanceof Sign)) {
				return;
			}
			
			if(!p.hasPermission("bw.setup") || e.isCancelled()) {
			    return;
			}

			Game game = Main.getInstance().getGameManager()
					.getGameBySignLocation(breaked.getLocation());
			if (game == null) {
				return;
			}

			game.removeJoinSign(breaked.getLocation());
			return;
		}

		if (g.getState() != GameState.RUNNING
				&& g.getState() != GameState.WAITING) {
			return;
		}

		if (g.getState() == GameState.WAITING) {
			e.setCancelled(true);
			return;
		}

		if (g.isSpectator(p)) {
			e.setCancelled(true);
			return;
		} 

		if (e.getBlock().getType() == Material.BED_BLOCK) {
			e.setCancelled(true);

			Team team = g.getPlayerTeam(p);
			if (team == null) {
				return;
			}

			Block bedBlock = team.getHeadBed();
			Block breakBlock = e.getBlock();
			Block neighbor = null;
			Bed breakBed = (Bed) breakBlock.getState().getData();

			if (!breakBed.isHeadOfBed()) {
				neighbor = breakBlock;
				breakBlock = Utils.getBedNeighbor(neighbor);
			} else {
				neighbor = Utils.getBedNeighbor(breakBlock);
			}
			
			if (bedBlock.equals(breakBlock)) {
				p.sendMessage(ChatWriter.pluginMessage(ChatColor.RED
						+ Main._l("ingame.blocks.ownbeddestroy")));
				return;
			}

			Team bedDestroyTeam = g.getTeamOfBed(breakBlock);
			if (bedDestroyTeam == null) {
				return;
			}
			
			// set statistics
			if(Main.getInstance().statisticsEnabled()) {
				PlayerStatistic statistic = Main.getInstance().getPlayerStatisticManager().getStatistic(p);
				statistic.setDestroyedBeds(statistic.getDestroyedBeds()+1);
				statistic.addCurrentScore(Main.getInstance().getIntConfig("statistics.scores.bed-destroy", 25));
			}
            
			neighbor.getDrops().clear();
			neighbor.setType(Material.AIR);
			breakBlock.getDrops().clear();
			breakBlock.setType(Material.AIR);

			g.broadcast(ChatColor.RED
					+ Main._l(
							"ingame.blocks.beddestroyed",
							ImmutableMap.of("team",
									bedDestroyTeam.getChatColor()
											+ bedDestroyTeam.getName()
											+ ChatColor.RED,
											"player",
                                            Game.getPlayerWithTeamString(p, team, ChatColor.RED))));
			
			
			g.broadcastSound(Sound.valueOf(Main.getInstance().getStringConfig("bed-sound", "ENDERDRAGON_GROWL").toUpperCase()), 30.0F, 10.0F);
			g.setPlayersScoreboard();
			return;
		}
		
		Block breakedBlock = e.getBlock();

		if (!g.getRegion().isPlacedBlock(breakedBlock)) {
	        e.setCancelled(true); 
		} else {
		    if (e.getBlock().getType() == Material.ENDER_CHEST) {
	            for (Team team : g.getTeams().values()) {
	                List<Block> teamChests = team.getChests();
	                if (teamChests.contains(breakedBlock)) {
	                    team.removeChest(breakedBlock);
	                }
	            }
	            
	            // Drop ender chest
	            ItemStack enderChest = new ItemStack(Material.ENDER_CHEST, 1);
	            ItemMeta meta = enderChest.getItemMeta();
	            meta.setDisplayName(Main._l("ingame.teamchest"));
	            enderChest.setItemMeta(meta);
	            
	            e.setCancelled(true);
	            breakedBlock.getDrops().clear();
	            breakedBlock.setType(Material.AIR);
	            breakedBlock.getWorld().dropItemNaturally(breakedBlock.getLocation(), enderChest);
	        }
		    
		    g.getRegion().removePlacedBlock(breakedBlock);
		}
	}
	
	@EventHandler
	public void onIgnite(BlockIgniteEvent ignite) {
		if(ignite.isCancelled()) {
			return;
		}
		
		if(ignite.getIgnitingBlock() == null
		        && ignite.getIgnitingEntity() == null) {
		    return;
		}
		
		Game game = null;
		if(ignite.getIgnitingBlock() == null) {
		    if(ignite.getIgnitingEntity() instanceof Player) {
		        game = Main.getInstance().getGameManager().getGameOfPlayer((Player)ignite.getIgnitingEntity());
		    } else {
		        game = Main.getInstance().getGameManager().getGameByLocation(ignite.getIgnitingEntity().getLocation());
		    }
		} else {
		    game = Main.getInstance().getGameManager().getGameByLocation(ignite.getIgnitingBlock().getLocation());
		}
		
		if(game == null) {
			return;
		}
		
		if(game.getState() != GameState.RUNNING) {
			return;
		}
		
		if(ignite.getCause() == IgniteCause.ENDER_CRYSTAL
				|| ignite.getCause() == IgniteCause.LIGHTNING
				|| ignite.getCause() == IgniteCause.SPREAD) {
			ignite.setCancelled(true);
			return;
		}
		
		if(ignite.getIgnitingEntity() == null) {
			ignite.setCancelled(true);
			return;
		}
		
		if(!game.getRegion().isPlacedBlock(ignite.getIgnitingBlock())
		        && ignite.getIgnitingBlock() != null) {
		    game.getRegion().addPlacedBlock(ignite.getIgnitingBlock(), ignite.getIgnitingBlock().getState());
		}
	}

	@EventHandler
	public void onPlace(BlockPlaceEvent bpe) {
		Player player = bpe.getPlayer();
		Game game = Main.getInstance().getGameManager().getGameOfPlayer(player);

		if (game == null) {
			return;
		}

		if (game.getState() == GameState.STOPPED) {
			return;
		}

		if (game.getState() == GameState.WAITING) {
			bpe.setCancelled(true);
			bpe.setBuild(false);
			return;
		}

		if (game.getState() == GameState.RUNNING) {
			if (game.isSpectator(player)) {
				bpe.setCancelled(true);
				bpe.setBuild(false);
				return;
			}

			Block placeBlock = bpe.getBlockPlaced();

			if (placeBlock.getType() == Material.BED
					|| placeBlock.getType() == Material.BED_BLOCK) {
				bpe.setCancelled(true);
				bpe.setBuild(false);
				return;
			}

			if (!game.getRegion().isInRegion(placeBlock.getLocation())) {
				bpe.setCancelled(true);
				bpe.setBuild(false);
				return;
			}
			
			if (placeBlock.getType() == Material.ENDER_CHEST) {
				Team playerTeam = game.getPlayerTeam(player);
				if (playerTeam.getInventory() == null) {
					playerTeam.createTeamInventory();
				}

				playerTeam.addChest(placeBlock);
			}
			
			if(!bpe.isCancelled()) {
			    game.getRegion().addPlacedBlock(placeBlock, bpe.getBlockReplacedState());
			}
		}
	}

}
