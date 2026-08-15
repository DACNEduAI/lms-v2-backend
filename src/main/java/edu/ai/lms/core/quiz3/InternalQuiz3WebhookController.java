package edu.ai.lms.core.quiz3;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/internal/webhooks/quiz3-generation")
@RequiredArgsConstructor
public class InternalQuiz3WebhookController {
    private final Quiz3AssessmentService service;
    private final ObjectMapper objectMapper;
    @Value("${quiz.generation.webhook.secret}") private String webhookSecret;

    @PostMapping public ResponseEntity<Void> callback(@RequestHeader(value="X-Webhook-Timestamp",required=false)String timestamp,
            @RequestHeader(value="X-Webhook-Signature",required=false)String signature,@RequestBody String body)throws Exception{
        if(!valid(timestamp,signature,body))return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        service.webhook(objectMapper.readValue(body,Quiz3Models.WebhookPayload.class));return ResponseEntity.ok().build();}
    @PostMapping("/progress") public ResponseEntity<Void> progress(@RequestHeader(value="X-Webhook-Timestamp",required=false)String timestamp,
            @RequestHeader(value="X-Webhook-Signature",required=false)String signature,@RequestBody String body)throws Exception{
        if(!valid(timestamp,signature,body))return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        service.progress(objectMapper.readValue(body,Quiz3Models.ProgressPayload.class));return ResponseEntity.ok().build();}
    private boolean valid(String timestamp,String signature,String body){if(timestamp==null||signature==null)return false;try{long sent=Long.parseLong(timestamp);if(Math.abs(Instant.now().getEpochSecond()-sent)>300)return false;
        Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
        String expected=HexFormat.of().formatHex(mac.doFinal((timestamp+"."+body).getBytes(StandardCharsets.UTF_8)));String supplied=signature.startsWith("v1=")?signature.substring(3):signature;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8));}catch(Exception ex){return false;}}
}
