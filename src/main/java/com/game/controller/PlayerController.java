package com.game.controller;

import com.game.entity.Player;
import com.game.service.PlayerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest/players")
public class PlayerController {
    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping()
    public List<Player> getPlayers(@RequestParam(name = "pageNumber", defaultValue = "0") Integer pageNumber,
                                   @RequestParam(name = "pageSize", defaultValue = "3") Integer pageSize,
                                   @RequestParam(name = "order", defaultValue = "id") String order,
                                   @RequestParam(required = false) Map<String, String> params) {
        return playerService.findAll(pageNumber, pageSize, order, params);
    }

    @GetMapping("/{id}")
    public Player getPlayers(@PathVariable("id") String id) {
        return playerService.findById(id).orElse(null);
    }

    @GetMapping("/count")
    public Long getCountPlayers(@RequestParam(required = false) Map<String, String> params) {
        return playerService.getCountPlayers(params);
    }

    @PostMapping()
    public Player createPlayer(@RequestBody Player player) {
        return playerService.createPlayer(player);
    }

    @PostMapping("/{id}")
    public Player updatePlayer(@PathVariable("id") String id, @RequestBody Player player) {
        return playerService.updatePlayer(id, player);
    }

    @DeleteMapping("/{id}")
    public void deletePlayer(@PathVariable("id") String id) {
        playerService.deletePlayer(id);
    }
}