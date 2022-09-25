package com.game.service;

import com.game.entity.Player;
import com.game.entity.Profession;
import com.game.entity.Race;
import com.game.exception.BadRequestException;
import com.game.exception.NotFoundException;
import com.game.repository.PlayerRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Transactional
public class PlayerService {
    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public List<Player> findAll(Integer pageNumber, Integer pageSize, String order, Map<String, String> params) {
        return playerRepository.findAll(getSpecificationPlayer(params), PageRequest.of(pageNumber, pageSize,
                Sort.by(order.toLowerCase()))).getContent();
    }

    public Player createPlayer(Player player) {
        checkPlayerParameters(player);

        int level = calculateLevel(player.getExperience());
        int untilNextLevel = calculateUntilNextLevel(player.getExperience(), level);
        player.setLevel(level);
        player.setUntilNextLevel(untilNextLevel);

        if (player.getBanned() == null) {
            player.setBanned(false);
        }
        return playerRepository.save(player);
    }

    public Player updatePlayer(String id, Player player) {
        long idLong = checkAndGetLongId(id);
        checkExistById(idLong);

        Player playerToUpdate = playerRepository.getOne(idLong);

        if (player.getName() != null) {
            playerToUpdate.setName(player.getName());
        }
        if (player.getTitle() != null) {
            playerToUpdate.setTitle(player.getTitle());
        }

        if (player.getRace() != null) {
            playerToUpdate.setRace(player.getRace());
        }

        if (player.getProfession() != null) {
            playerToUpdate.setProfession(player.getProfession());
        }

        if (player.getExperience() != null) {
            playerToUpdate.setExperience(player.getExperience());
        }

        if (player.getBirthday() != null) {
            playerToUpdate.setBirthday(player.getBirthday());
        }
        if (player.getBanned() != null) {
            playerToUpdate.setBanned(player.getBanned());
        }

        int level = calculateLevel(playerToUpdate.getExperience());
        int untilNextLevel = calculateUntilNextLevel(playerToUpdate.getExperience(), level);
        playerToUpdate.setLevel(level);
        playerToUpdate.setUntilNextLevel(untilNextLevel);
        checkPlayerParameters(playerToUpdate);
        return playerRepository.save(playerToUpdate);
    }

    public Long getCountPlayers(Map<String, String> params) {
        return (long) playerRepository.findAll(getSpecificationPlayer(params)).size();
    }

    public void deletePlayer(String id) {
        long idLong = checkAndGetLongId(id);
        checkExistById(idLong);
        playerRepository.deleteById(idLong);
    }

    public Optional<Player> findById(String id) {
        long idLong = checkAndGetLongId(id);
        if (!playerRepository.existsById(idLong)) {
            throw new NotFoundException();
        }
        return playerRepository.findById(idLong);
    }

    private int calculateLevel(int exp) {
        return (int) ((Math.sqrt(2500 + 200 * exp) - 50) / 100);
    }

    private int calculateUntilNextLevel(int exp, int level) {
        return 50 * (level + 1) * (level + 2) - exp;
    }

    public void checkPlayerParameters(Player player) {
        checkName(player);
        checkTitle(player);
        checkRace(player);
        checkProfession(player);
        checkExperience(player);
        checkBirthday(player);
    }

    public void checkName(Player player) {
        if (player.getName() == null || player.getName().length() > 12) {
            throw new BadRequestException();
        }
    }

    public void checkTitle(Player player) {
        if (player.getTitle() == null || player.getTitle().length() > 30) {
            throw new BadRequestException();
        }
    }

    public void checkRace(Player player) {
        if (player.getRace() == null) {
            throw new BadRequestException();
        }
    }

    public void checkProfession(Player player) {
        if (player.getProfession() == null) {
            throw new BadRequestException();
        }
    }

    public void checkExperience(Player player) {
        if (player.getExperience() == null || player.getExperience() < 0 ||
                player.getExperience() > 10000000) {
            throw new BadRequestException();
        }
    }

    public void checkBirthday(Player player) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        try {
            long milliseconds = dateFormat.parse(player.getBirthday().toString()).getTime();
            Calendar currentDate = Calendar.getInstance();
            currentDate.setTime(dateFormat.parse(player.getBirthday().toString()));
            int currentYear = currentDate.get(Calendar.YEAR);
            if (currentYear < 2000 || currentYear > 3000 || milliseconds < 0) {
                throw new BadRequestException();
            }
        } catch (ParseException e) {
            throw new BadRequestException();
        }
    }

    public Long checkAndGetLongId(String id) {
        long idLong;
        if (id == null || id.equals("") || id.equals("0")) {
            throw new BadRequestException();
        }

        try {
            idLong = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new BadRequestException();
        }

        if (idLong < 0) {
            throw new BadRequestException();
        }
        return idLong;
    }

    public void checkExistById(long id) {
        if (!playerRepository.existsById(id)) {
            throw new NotFoundException();
        }
    }

    private static Specification<Player> getByName(String name) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("name"), "%" + name + "%");
    }

    private static Specification<Player> getByTitle(String title) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(root.get("title"), "%" + title + "%");
    }

    private static Specification<Player> getBirthdayMoreThan(String after) {
        Date finalDate = new Date(Long.parseLong(after));
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("birthday"), finalDate);
    }

    private static Specification<Player> getBirthdayLessThan(String before) {
        Date finalDate = new Date(Long.parseLong(before));
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("birthday"), finalDate);
    }

    private static Specification<Player> getExpMoreThan(String minExperience) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("experience"),
                        Integer.parseInt(minExperience));
    }

    private static Specification<Player> getExpLessThan(String maxExperience) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("experience"),
                        Integer.parseInt(maxExperience));
    }

    private static Specification<Player> getLevelMoreThan(String minLevel) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("level"),
                        Integer.parseInt(minLevel));
    }

    private static Specification<Player> getLevelLessThan(String maxLevel) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("level"),
                        Integer.parseInt(maxLevel));
    }

    private static Specification<Player> getByRace(String race) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("race"),
                Race.valueOf(race));
    }

    private static Specification<Player> getByProfession(String profession) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("profession"),
                Profession.valueOf(profession));
    }

    private static Specification<Player> getByBanned(String banned) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("banned"),
                Boolean.parseBoolean(banned));
    }

    Specification<Player> getSpecificationPlayer(Map<String, String> params) {
        return Specification.where(params.containsKey("name") ?
                        getByName(params.get("name")) : null)
                .and(params.containsKey("title") ?
                        getByTitle(params.get("title")) : null)
                .and(params.containsKey("after") ?
                        getBirthdayMoreThan(params.get("after")) : null)
                .and(params.containsKey("before") ?
                        getBirthdayLessThan(params.get("before")) : null)
                .and(params.containsKey("minExperience") ?
                        getExpMoreThan(params.get("minExperience")) : null)
                .and(params.containsKey("maxExperience") ?
                        getExpLessThan(params.get("maxExperience")) : null)
                .and(params.containsKey("minLevel") ?
                        getLevelMoreThan(params.get("minLevel")) : null)
                .and(params.containsKey("maxLevel") ?
                        getLevelLessThan(params.get("maxLevel")) : null)
                .and(params.containsKey("race") ?
                        getByRace(params.get("race")) : null)
                .and(params.containsKey("profession") ?
                        getByProfession(params.get("profession")) : null)
                .and(params.containsKey("banned") ?
                        getByBanned(params.get("banned")) : null);
    }
}