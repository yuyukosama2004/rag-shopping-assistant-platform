package com.biyesheji.product.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class S3MediaObjectStoreTest {
    @Test
    void writesAndDeletesObjectsBelowConfiguredPrefix() {
        S3Client client = mock(S3Client.class);
        S3MediaObjectStore store = new S3MediaObjectStore(client, "shop-media", "/products/");
        ArgumentCaptor<PutObjectRequest> put = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<DeleteObjectRequest> delete = ArgumentCaptor.forClass(DeleteObjectRequest.class);

        store.put("image.png", new byte[]{1, 2, 3}, "image/png");
        store.delete("image.png");

        verify(client).putObject(put.capture(), any(RequestBody.class));
        verify(client).deleteObject(delete.capture());
        assertEquals("shop-media", put.getValue().bucket());
        assertEquals("products/image.png", put.getValue().key());
        assertEquals("image/png", put.getValue().contentType());
        assertEquals("products/image.png", delete.getValue().key());
    }
}
