package com.costsharing.calculator.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.time.Duration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Component
public class WechatIdentity {
    private final String appId;
    private final String secret;
    private final RestClient client;
    private final ObjectMapper json;
    public WechatIdentity(@Value("${WECHAT_APP_ID:}") String appId, @Value("${WECHAT_APP_SECRET:}") String secret, ObjectMapper json) {
        this.json=json;
        this.appId=appId; this.secret=secret;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(8).toMillis());
        client=RestClient.builder().baseUrl("https://api.weixin.qq.com").requestFactory(factory).build();
    }
    public String exchange(String code) {
        if (appId.isBlank() || secret.isBlank()) throw new AccessException(503, "管理员尚未配置微信登录");
        try {
            String body = client.get().uri(builder -> builder.path("/sns/jscode2session")
                    .queryParam("appid", appId).queryParam("secret", secret)
                    .queryParam("js_code", code).queryParam("grant_type", "authorization_code").build())
                    .retrieve().body(String.class);
            var reply = body == null ? null : json.readValue(body, Reply.class);
            if (reply == null || reply.openid() == null || reply.openid().isBlank() || (reply.errcode()!=null && reply.errcode()!=0))
                throw new AccessException(401, "微信登录失效，请重试");
            return reply.openid();
        } catch (AccessException e) { throw e; }
        catch (Exception e) { throw new AccessException(503, "微信登录暂时不可用，请稍后重试"); }
    }
    // 不接收或向客户端传递 session_key。
    @JsonIgnoreProperties(ignoreUnknown=true)
    private record Reply(String openid, Integer errcode) {}
}
