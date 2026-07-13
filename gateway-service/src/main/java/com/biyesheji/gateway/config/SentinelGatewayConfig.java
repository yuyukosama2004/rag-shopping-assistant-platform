package com.biyesheji.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;

@Configuration
public class SentinelGatewayConfig {

    @Value("${sentinel.qps.order:100}")
    private int orderQps;

    @Value("${sentinel.qps.product:500}")
    private int productQps;

    @Value("${sentinel.qps.ai:5}")
    private int aiQps;

    @PostConstruct
    public void initSentinelRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();

        GatewayFlowRule orderRule = new GatewayFlowRule();
        orderRule.setResource("order-service");
        orderRule.setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID);
        orderRule.setCount(orderQps);
        orderRule.setIntervalSec(1);
        rules.add(orderRule);

        GatewayFlowRule productRule = new GatewayFlowRule();
        productRule.setResource("product-service");
        productRule.setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID);
        productRule.setCount(productQps);
        productRule.setIntervalSec(1);
        rules.add(productRule);

        GatewayFlowRule aiRule = new GatewayFlowRule();
        aiRule.setResource("ai-service");
        aiRule.setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID);
        aiRule.setCount(aiQps);
        aiRule.setIntervalSec(1);
        rules.add(aiRule);

        GatewayRuleManager.loadRules(rules);

        BlockRequestHandler handler = (exchange, t) ->
                ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(fromValue(
                                "{\"code\":429,\"message\":\"当前访问人数过多，请稍后再试\",\"data\":null}"
                        ));

        GatewayCallbackManager.setBlockHandler(handler);
    }
}
