package org.foobar;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AppTest {

    @Test
    public void testDecodeOpenLR_ValidData() {
        OpenLRProcessor server = new OpenLRProcessor();
        String base64Data = "CCoBEAAmJQm+WSVVfAAJBQQCAxoACgUEAogZAAHtA2UACQUEAgOEADBigj0=";

        Map<String, Object> result = server.decodeOpenLR(base64Data);
        System.out.println(result);

        assertNotNull(result);
        assertFalse(result.toString().contains("error"), "Result should not contain an error");
        System.out.println("✅ Valid OpenLR Data Test Passed!");
    }

    @Test
    public void testDecodeOpenLR_InvalidData() {
        OpenLRProcessor server = new OpenLRProcessor();
        String invalidBase64Data = "INVALIDBASE64===";

        Map<String, Object> result = server.decodeOpenLR(invalidBase64Data);

        assertNotNull(result);
        assertTrue(result.toString().contains("error"), "Result should contain an error");
        System.out.println("✅ Invalid OpenLR Data Test Passed!");
    }

    @Test
    public void testDecodeOpenLR_EmptyData() {
        OpenLRProcessor server = new OpenLRProcessor();
        String emptyBase64Data = "";

        Map<String, Object> result = server.decodeOpenLR(emptyBase64Data);

        assertNotNull(result);
        assertTrue(result.toString().contains("error"), "Result should contain an error");
        System.out.println("✅ Empty OpenLR Data Test Passed!");
    }
}
