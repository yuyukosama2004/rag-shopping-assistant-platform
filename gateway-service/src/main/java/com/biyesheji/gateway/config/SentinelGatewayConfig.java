package com.biyesheji.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;

@Configuration
public class SentinelGatewayConfig {

    @PostConstruct
    public void initSentinelRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();

        // 下单接口限流：QPS 100（演示时可调低验证限流效果）
        GatewayFlowRule orderRule = new GatewayFlowRule();
        orderRule.setResource("order-service");
        orderRule.setResourceMode(com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID);
        orderRule.setCount(100);
        orderRule.setIntervalSec(1);
        rules.add(orderRule);

        GatewayRuleManager.loadRules(rules);

        // 限流降级处理：返回友好 JSON
        BlockRequestHandler handler = (exchange, t) ->
                ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(fromValue(
                                "{\"code\":429,\"message\":\"当前访问人数过多，请稍后再试\",\"data\":null}"
                        ));

        GatewayCallbackManager.setBlockHandler(handler);
    }
}
