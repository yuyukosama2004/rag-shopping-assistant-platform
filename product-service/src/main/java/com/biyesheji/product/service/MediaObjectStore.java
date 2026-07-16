package com.biyesheji.product.service;

import org.springframework.core.io.Resource;

interface MediaObjectStore {
    void put(String filename, byte[] content, String contentType);

    Resource get(String filename);

    void delete(String filename);
}
