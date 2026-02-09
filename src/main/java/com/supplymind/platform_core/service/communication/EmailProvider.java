package com.supplymind.platform_core.service.communication;

import java.io.File;

public interface EmailProvider {


    void sendEmail(String to, String subject, String body, File attachment);
    
}