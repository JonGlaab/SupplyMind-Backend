package com.supplymind.platform_core.service.communication;

import com.supplymind.platform_core.dto.intel.email.EmailMessage;
import java.io.File;
import java.util.List;

public interface EmailProvider {


    void sendEmail(String to, String subject, String body, File attachment);
    
}