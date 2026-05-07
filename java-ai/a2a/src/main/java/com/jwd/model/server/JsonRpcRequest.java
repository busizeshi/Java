package com.jwd.model.server;

import java.util.*;

// JSON-RPC 请求
public record JsonRpcRequest(
    String jsonrpc,
    String method,
    Map<String, Object> params,
    String id
) {}
