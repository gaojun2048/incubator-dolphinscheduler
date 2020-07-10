/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dolphinscheduler.plugin.alert.email;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.dolphinscheduler.plugin.alert.email.template.AlertTemplate;
import org.apache.dolphinscheduler.plugin.alert.email.template.DefaultHTMLTemplate;
import org.apache.dolphinscheduler.plugin.alert.email.template.ShowType;
import org.apache.dolphinscheduler.spi.alert.AlertResult;
import org.apache.dolphinscheduler.spi.utils.CollectionUtils;
import org.apache.dolphinscheduler.spi.utils.DSConstants;
import org.apache.dolphinscheduler.spi.utils.ExcelUtils;
import org.apache.dolphinscheduler.spi.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.util.Objects.requireNonNull;


/**
 * mail utils
 */
public class MailSender {

    public static final Logger logger = LoggerFactory.getLogger(MailSender.class);

    private List<String> receivers;
    private List<String> receiverCcs;
    private String mailProtocol = "smtp";
    private String mailSmtpHost;
    private String mailSmtpPort;
    private String mailSender;
    private String enableSmtpAuth;
    private String mailUser;
    private String mailPasswd;
    private String mailUseStartTLS;
    private String mailUseSSL;
    private String xlsFilePath;
    private String sslTrust;
    private String showType;
    private AlertTemplate alertTemplate;

    public MailSender(Map<String, String> config)
    {

        String receiversConfig = config.get(MailParamsConstants.NAME_PLUGIN_DEFAULT_EMAIL_RECEIVERS);
        if(receiversConfig == null || "".equals(receiversConfig)) {
            throw new RuntimeException(MailParamsConstants.PLUGIN_DEFAULT_EMAIL_RECEIVERS + "must not be null");
        }

        receivers = Arrays.asList(receiversConfig.split(","));

        String receiverCcsConfig = config.get(MailParamsConstants.PLUGIN_DEFAULT_EMAIL_RECEIVERCCS);

        receiverCcs = new ArrayList<>();
        if(receiverCcsConfig != null && !"".equals(receiverCcsConfig)) {
            receiverCcs = Arrays.asList(receiverCcsConfig.split(","));
        }

        mailSmtpHost = config.get(MailParamsConstants.NAME_MAIL_SMTP_HOST);
        requireNonNull(mailSmtpHost, MailParamsConstants.MAIL_SMTP_HOST + " must not null");

        mailSmtpPort = config.get(MailParamsConstants.NAME_MAIL_SMTP_PORT);
        requireNonNull(mailSmtpPort, MailParamsConstants.MAIL_SMTP_PORT + " must not null");

        mailSender = config.get(MailParamsConstants.NAME_MAIL_SENDER);
        requireNonNull(mailSender, MailParamsConstants.MAIL_SENDER + " must not null");

        enableSmtpAuth = config.get(MailParamsConstants.NAME_MAIL_SMTP_AUTH);

        mailUser = config.get(MailParamsConstants.NAME_MAIL_USER);
        requireNonNull(mailUser, MailParamsConstants.MAIL_USER + " must not null");

        mailPasswd = config.get(MailParamsConstants.NAME_MAIL_PASSWD);
        requireNonNull(mailPasswd, MailParamsConstants.MAIL_PASSWD + " must not null");

        mailUseStartTLS = config.get(MailParamsConstants.NAME_MAIL_SMTP_STARTTLS_ENABLE);
        requireNonNull(mailUseStartTLS, MailParamsConstants.MAIL_SMTP_STARTTLS_ENABLE + " must not null");

        mailUseSSL = config.get(MailParamsConstants.NAME_MAIL_SMTP_SSL_ENABLE);
        requireNonNull(mailUseSSL, MailParamsConstants.MAIL_SMTP_SSL_ENABLE + " must not null");

        sslTrust = config.get(MailParamsConstants.NAME_MAIL_SMTP_SSL_TRUST);
        requireNonNull(sslTrust, MailParamsConstants.MAIL_SMTP_SSL_TRUST + " must not null");

        showType = config.get(MailParamsConstants.SHOW_TYPE);
        requireNonNull(showType, MailParamsConstants.SHOW_TYPE + " must not null");

        xlsFilePath = config.get(Constants.XLS_FILE_PATH);
        if (StringUtils.isBlank(xlsFilePath)) {
            xlsFilePath = "/tmp/xls";
        }

        alertTemplate = new DefaultHTMLTemplate();
    }

    /**
     * send mail to receivers
     * @param title
     * @param content
     * @return
     */
    public AlertResult sendMails(String title, String content) {
        return sendMails(this.receivers, this.receiverCcs, title, content);
    }

    /**
     * send mail to receivers
     * @param title
     * @param content
     * @return
     */
    public AlertResult sendMailsToReceiverOnly(String title, String content) {
        return sendMails(this.receivers, null, title, content);
    }

    /**
     * send mail
     * @param title
     * @param content
     * @return
     */
    public AlertResult sendMails(List<String> receivers, List<String> receiverCcs, String title, String content) {
        AlertResult alertResult = new AlertResult();
        alertResult.setStatus("false");

        // if there is no receivers && no receiversCc, no need to process
        if (CollectionUtils.isEmpty(receivers) && CollectionUtils.isEmpty(receiverCcs)) {
            return alertResult;
        }

        receivers.removeIf(StringUtils::isEmpty);

        if (showType.equals(ShowType.TABLE.getDescp()) || showType.equals(ShowType.TEXT.getDescp())) {
            // send email
            HtmlEmail email = new HtmlEmail();

            try {
                Session session = getSession();
                email.setMailSession(session);
                email.setFrom(mailSender);
                email.setCharset(DSConstants.UTF_8);
                if (CollectionUtils.isNotEmpty(receivers)){
                    // receivers mail
                    for (String receiver : receivers) {
                        email.addTo(receiver);
                    }
                }

                if (CollectionUtils.isNotEmpty(receiverCcs)){
                    //cc
                    for (String receiverCc : receiverCcs) {
                        email.addCc(receiverCc);
                    }
                }
                // sender mail
                return getStringObjectMap(title, content, alertResult, email);
            } catch (Exception e) {
                handleException(alertResult, e);
            }
        }else if (showType.equals(ShowType.ATTACHMENT.getDescp()) || showType.equals(ShowType.TABLEATTACHMENT.getDescp())) {
            try {

                String partContent = (showType.equals(ShowType.ATTACHMENT.getDescp()) ? "Please see the attachment " + title + DSConstants.EXCEL_SUFFIX_XLS : htmlTable(content,false));

                attachment(title,content,partContent);

                alertResult.setStatus("true");
                return alertResult;
            }catch (Exception e){
                handleException(alertResult, e);
                return alertResult;
            }
        }
        return alertResult;

    }

    /**
     * html table content
     * @param content the content
     * @param showAll if show the whole content
     * @return the html table form
     */
    private String htmlTable(String content, boolean showAll){
        return alertTemplate.getMessageFromTemplate(content,ShowType.TABLE,showAll);
    }

    /**
     * html table content
     * @param content the content
     * @return the html table form
     */
    private String htmlTable(String content){
        return htmlTable(content,true);
    }

    /**
     * html text content
     * @param content the content
     * @return text in html form
     */
    private String htmlText(String content){
        return alertTemplate.getMessageFromTemplate(content,ShowType.TEXT);
    }

    /**
     * send mail as Excel attachment
     * @param title
     * @param content
     * @param partContent
     * @throws Exception
     */
    private void attachment(String title,String content,String partContent)throws Exception{
        MimeMessage msg = getMimeMessage();

        attachContent(title, content, partContent, msg);
    }

    /**
     * get MimeMessage
     * @return
     * @throws MessagingException
     */
    private MimeMessage getMimeMessage() throws MessagingException {

        // 1. The first step in creating mail: creating session
        Session session = getSession();
        // Setting debug mode, can be turned off
        session.setDebug(false);

        // 2. creating mail: Creating a MimeMessage
        MimeMessage msg = new MimeMessage(session);
        // 3. set sender
        msg.setFrom(new InternetAddress(mailSender));
        // 4. set receivers
        for (String receiver : receivers) {
            msg.addRecipients(Message.RecipientType.TO, InternetAddress.parse(receiver));
        }
        return msg;
    }

    /**
     * get session
     * @return the new Session
     */
    private Session getSession() {
        Properties props = new Properties();
        props.setProperty(MailParamsConstants.MAIL_SMTP_HOST, mailSmtpHost);
        props.setProperty(MailParamsConstants.MAIL_SMTP_PORT, mailSmtpPort);
        props.setProperty(MailParamsConstants.MAIL_SMTP_AUTH, enableSmtpAuth);
        props.setProperty(Constants.MAIL_TRANSPORT_PROTOCOL, mailProtocol);
        props.setProperty(MailParamsConstants.MAIL_SMTP_STARTTLS_ENABLE, mailUseStartTLS);
        props.setProperty(MailParamsConstants.MAIL_SMTP_SSL_ENABLE, mailUseSSL);
        props.setProperty(MailParamsConstants.MAIL_SMTP_SSL_TRUST, sslTrust);

        Authenticator auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                // mail username and password
                return new PasswordAuthentication(mailUser, mailPasswd);
            }
        };

        return Session.getInstance(props, auth);
    }

    /**
     * attach content
     * @param title
     * @param content
     * @param partContent
     * @param msg
     * @throws MessagingException
     * @throws IOException
     */
    private void attachContent(String title, String content, String partContent,MimeMessage msg) throws MessagingException, IOException {
        /**
         * set receiverCc
         */
        if(CollectionUtils.isNotEmpty(receiverCcs)){
            for (String receiverCc : receiverCcs){
                msg.addRecipients(Message.RecipientType.CC, InternetAddress.parse(receiverCc));
            }
        }

        // set subject
        msg.setSubject(title);
        MimeMultipart partList = new MimeMultipart();
        // set signature
        MimeBodyPart part1 = new MimeBodyPart();
        part1.setContent(partContent, Constants.TEXT_HTML_CHARSET_UTF_8);
        // set attach file
        MimeBodyPart part2 = new MimeBodyPart();
        File file = new File(xlsFilePath + DSConstants.SINGLE_SLASH +  title + DSConstants.EXCEL_SUFFIX_XLS);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        // make excel file

        ExcelUtils.genExcelFile(content,title,xlsFilePath);

        part2.attachFile(file);
        part2.setFileName(MimeUtility.encodeText(title + DSConstants.EXCEL_SUFFIX_XLS,DSConstants.UTF_8,"B"));
        // add components to collection
        partList.addBodyPart(part1);
        partList.addBodyPart(part2);
        msg.setContent(partList);
        // 5. send Transport
        Transport.send(msg);
        // 6. delete saved file
        deleteFile(file);
    }

    /**
     * the string object map
     * @param title
     * @param content
     * @param alertResult
     * @param email
     * @return
     * @throws EmailException
     */
    private AlertResult getStringObjectMap(String title, String content, AlertResult alertResult, HtmlEmail email) throws EmailException {

        /**
         * the subject of the message to be sent
         */
        email.setSubject(title);
        /**
         * to send information, you can use HTML tags in mail content because of the use of HtmlEmail
         */
        if (showType.equals(ShowType.TABLE.getDescp())) {
            email.setMsg(htmlTable(content));
        } else if (showType.equals(ShowType.TEXT.getDescp())) {
            email.setMsg(htmlText(content));
        }

        // send
        email.send();

        alertResult.setStatus("true");

        return alertResult;
    }

    /**
     * file delete
     * @param file the file to delete
     */
    public  void deleteFile(File file){
        if(file.exists()){
            if(file.delete()){
                logger.info("delete success: {}",file.getAbsolutePath() + file.getName());
            }else{
                logger.info("delete fail: {}", file.getAbsolutePath() + file.getName());
            }
        }else{
            logger.info("file not exists: {}", file.getAbsolutePath() + file.getName());
        }
    }


    /**
     * handle exception
     * @param alertResult
     * @param e
     */
    private void handleException(AlertResult alertResult, Exception e) {
        logger.error("Send email to {} failed", receivers, e);
        alertResult.setMessage("Send email to {" + String.join(",", receivers) + "} failed，" + e.toString());
    }

}