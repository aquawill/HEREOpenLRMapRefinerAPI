package org.foobar;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AppTest {

    @Test
    public void testDecodeOpenLR_ValidData() {
        OpenLRDecoder openLRDecoder = new OpenLRDecoder();
        String base64Data = "CCoBEAAmJQm+WSVVfAAJBQQCAxoACgUEAogZAAHtA2UACQUEAgOEADBigj0=";

        Map<String, Object> result = openLRDecoder.decodeOpenLR(base64Data);
//        System.out.println(result);

        assertNotNull(result);
        assertFalse(result.toString().contains("error"), "Result should not contain an error");
        System.out.println("✅ Valid OpenLR Data Test Passed!");
    }

    @Test
    public void testDecodeOpenLR_InvalidData() {
        OpenLRDecoder server = new OpenLRDecoder();
        String invalidBase64Data = "INVALIDBASE64===";

        Map<String, Object> result = server.decodeOpenLR(invalidBase64Data);

        assertNotNull(result);
        assertTrue(result.toString().contains("error"), "Result should contain an error");
        System.out.println("✅ Invalid OpenLR Data Test Passed!");
    }

    @Test
    public void testDecodeOpenLR_EmptyData() {
        OpenLRDecoder server = new OpenLRDecoder();
        String emptyBase64Data = "";

        Map<String, Object> result = server.decodeOpenLR(emptyBase64Data);

        assertNotNull(result);
        assertTrue(result.toString().contains("error"), "Result should contain an error");
        System.out.println("✅ Empty OpenLR Data Test Passed!");
    }
}
