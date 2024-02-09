package com.ssafy.server.song.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.server.song.model.ReserveModel;
import com.ssafy.server.song.model.entity.SingLog;
import com.ssafy.server.song.model.entity.Song;
import com.ssafy.server.song.model.entity.SongInfo;
import com.ssafy.server.song.service.SongService;
import com.ssafy.server.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/song")
public class SongController {

    private final SongService songService;
    private final UserService userService;
    private final ReserveModel reserveModel;

    @Autowired
    public SongController(SongService songService, UserService userService, ReserveModel reserveModel) {
        this.songService = songService;
        this.userService = userService;
        this.reserveModel = reserveModel;
    }

    @GetMapping("/list")
    public ResponseEntity<?> getSongList() {
        ArrayList<Song> list = (ArrayList<Song>) songService.getSongList();

        return ResponseEntity.ok(list);
    }

    @PostMapping("/reserve")
    public ResponseEntity<?> reserveSong(@RequestBody Map<String, Object> params) {
        System.out.println("Reserve Song : " + params);

        String sessionName = (String) params.get("sessionName");
        String userName = (String) params.get("userName");
        int songId = (Integer) params.get("songId");

        Song song = songService.getSongById(songId);
        String hashString = userName + "&" + song.getTitle() + "&" + song.getSinger();
        reserveModel.getMapSongReserveDeq().get(sessionName).add(hashString);

        return ResponseEntity.ok(reserveModel.getMapSongReserveDeq().get(sessionName));
    }

    @PostMapping("/reserveList")
    public ResponseEntity<?> getReserveList(@RequestBody Map<String, Object> params) {
        String sessionName = (String) params.get("sessionName");
        ArrayDeque<String> reserveDeq = reserveModel.getMapSongReserveDeq().get(sessionName);

        return ResponseEntity.ok(reserveDeq);
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancelReserve(@RequestBody Map<String, Object> params) {
        System.out.println("Reserve Song : " + params);

        String sessionName = (String) params.get("sessionName");
        String hashString = (String) params.get("hashString");

        if (!reserveModel.getMapSongReserveDeq().get(sessionName).contains(hashString)) {
            System.out.println("존재하지 않는 예약");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        reserveModel.getMapSongReserveDeq().get(sessionName).remove(hashString);

        return ResponseEntity.ok(hashString);
    }

    @PostMapping("/start")
    public ResponseEntity<?> startSing(@RequestBody Map<String, Object> params) {
        System.out.println("Start Song : " + params);

        String sessionName = (String) params.get("sessionName");

        if (!reserveModel.getMapSongReserveDeq().containsKey(sessionName)) {
            System.out.println("존재하지 않는 세션");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (reserveModel.getMapSongReserveDeq().get(sessionName).isEmpty()) {
            System.out.println("예약 없음");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String hashString = reserveModel.getMapSongReserveDeq().get(sessionName).pollFirst();
        reserveModel.getMapSongReserveDeq().get(sessionName).remove(hashString);

        return ResponseEntity.ok(hashString);
    }

    @GetMapping("/{songId}")
    public ResponseEntity<Song> getSongById(@PathVariable int songId) {
        Song song = songService.getSongById(songId);

        return ResponseEntity.ok((song));
    }

    @GetMapping("/songInfo/{songId}")
    public ResponseEntity<SongInfo> getSongInfoById(@PathVariable int songId) {
        SongInfo songInfo = songService.getSongInfoById(songId);

        return ResponseEntity.ok((songInfo));
    }

    @PostMapping("/createLog")
    public void createSongLog(@RequestBody JsonNode jsonNode) {
        SingLog singlog = new SingLog();
        int songId = -1, userPk = -1, singScore = -1;
        String singMode = "", singStatus = "";
        UUID userUUID = null;
        try {
            songId = Integer.parseInt(jsonNode.get("songId").asText());
            if (jsonNode.get("uuid") != null) {
                userUUID = UUID.fromString(jsonNode.get("uuid").asText());
                userPk = userService.getUserPk(userUUID);
            } else {
                userPk = Integer.parseInt(jsonNode.get("userPk").asText());
            }
            singMode = jsonNode.get("singMode").asText();
            singStatus = jsonNode.get("singStatus").asText();

            if (jsonNode.get("songId").asText() != null) {
                singScore = Integer.parseInt(jsonNode.get("singScore").asText());
            }
        } catch (Exception e) {
            return;
        }
        singlog.setSongId(songId);
        singlog.setUserPk(userPk);
        singlog.setSingMode(singMode);
        singlog.setSingStatus(singStatus);
        singlog.setSingScore(singScore);

        songService.createSongLog(singlog);
    }

}
