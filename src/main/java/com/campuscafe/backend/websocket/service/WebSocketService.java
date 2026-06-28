package com.campuscafe.backend.websocket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastKotUpdate(Long merchantId, Object payload) {
        String destination = "/topic/merchant/" + merchantId + "/kot";
        log.info("Broadcasting KOT event to {}", destination);
        messagingTemplate.convertAndSend(destination, payload);
    }

    public void broadcastCustomerDisplayUpdate(Long merchantId, Object payload) {
        String destination = "/topic/merchant/" + merchantId + "/customer-display";
        log.info("Broadcasting Customer Display event to {}", destination);
        messagingTemplate.convertAndSend(destination, payload);
    }
}
