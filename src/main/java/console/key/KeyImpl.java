package console.key;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import console.common.ConsoleUtils;
import console.common.HelpInfo;
import console.exception.ConsoleMessageException;
import console.key.service.KMSService;
import console.key.tools.AES;
import console.key.tools.Common;
import console.key.tools.ECC;
import io.bretty.console.table.Alignment;
import io.bretty.console.table.ColumnFormatter;
import io.bretty.console.table.Table;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.fisco.bcos.channel.client.P12Manager;
import org.fisco.bcos.channel.client.PEMManager;
import org.fisco.bcos.web3j.crypto.Credentials;
import org.fisco.bcos.web3j.crypto.ECKeyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

public class KeyImpl implements KeyFace {

    private static final Logger logger = LoggerFactory.getLogger(KeyImpl.class);

    private String urlPrefix;
    private String token;
    private String consoleAccountName;
    private String roleName;

    private KMSService kmsService;

    public KeyImpl() throws Exception {
        try {
            kmsService = new KMSService();
        } catch (Exception e) {
            throw new ConsoleMessageException("Init rest template error: " + e);
        }
    }

    @Override
    public void setURLPrefix(String urlPrefix) {
        this.urlPrefix = urlPrefix;
    }

    @Override
    public String login(String[] params) throws Exception {
        String url = urlPrefix + "account/login";
        String account = params[1];
        String accountPwd = params[2];

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        LinkedMultiValueMap paramsMap = new LinkedMultiValueMap();
        paramsMap.add("account", account);
        paramsMap.add("accountPwd", accountPwd);
        HttpEntity entity = new HttpEntity(paramsMap, headers);

        ResponseEntity<String> response =
                kmsService.getRestTemplate().exchange(url, HttpMethod.POST, entity, String.class);
        String strBody = response.getBody();
        logger.info(strBody);

        JSONObject jsonBody = JSONObject.parseObject(strBody);
        JSONObject data = jsonBody.getJSONObject("data");
        token = data.getString("token");
        consoleAccountName = data.getString("account");
        roleName = data.getString("roleName");
        String accountInfo = consoleAccountName + ":" + roleName;

        return accountInfo;
    }

    @Override
    public void addAdminAccount(String[] params) throws Exception {
        if (!roleName.equals(Common.KMS_ROLE_ADMIN)) {
            System.out.println("This command can only be called by admin role.");
            return;
        }

        if (params.length < 2) {
            HelpInfo.promptHelp("addAdminAccount");
            return;
        }
        String param = params[1];
        if ("-h".equals(param) || "--help".equals(param)) {
            HelpInfo.addAdminAccountHelp();
            return;
        }
        if (params.length != 4) {
            HelpInfo.promptHelp("addAdminAccount");
            return;
        }
        String account = params[1];
        String accountPwd = params[2];
        String publicKey = params[3];
        if (!Common.checkUserName(account)) {
            System.out.println("Invalid account. " + Common.ACCOUNT_NAME_FORMAT);
            return;
        }
        if (!Common.checkPassword(accountPwd)) {
            System.out.println("Invalid password. " + Common.PASSWORD_FORMAT);
            return;
        }
        if (publicKey.length() != 128) {
            System.out.println("Invalid public key length, need 128. ");
            return;
        }

        String url = urlPrefix + "account/addAccount";
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("account", account);
        jsonParam.put("accountPwd", accountPwd);
        jsonParam.put("publicKey", publicKey);
        jsonParam.put("roleId", Common.KMS_ROLE_ADMIN_ID);
        HttpHeaders headers = new HttpHeaders();
        headers.add("AuthorizationToken", "Token " + token);
        headers.add("Content-Type", "application/json");
        HttpEntity<String> entity = new HttpEntity<String>(jsonParam.toJSONString(), headers);
        try {
            ResponseEntity<String> response =
                    kmsService
                            .getRestTemplate()
                            .exchange(url, HttpMethod.POST, entity, String.class);
            String strBody = response.getBody();
            logger.info(strBody);
            JSONObject jsonBody = JSONObject.parseObject(strBody);
            JSONObject data = jsonBody.getJSONObject("data");
            String newAccount = data.getString("account");
            System.out.println("Add an admin account \"" + newAccount + "\" successfully.");
        } catch (HttpClientErrorException e) {
            System.out.println("Fail, " + e.getResponseBodyAsString());
        }
    }

    @Override
    public void addVisitorAccount(String[] params) throws Exception {
        if (!roleName.equals(Common.KMS_ROLE_ADMIN)) {
            System.out.println("This command can only be called by admin role.");
            return;
        }

        if (params.length < 2) {
            HelpInfo.promptHelp("addVisitorAccount");
            return;
        }
        String param = params[1];
        if ("-h".equals(param) || "--help".equals(param)) {
            HelpInfo.addVisitorAccountHelp();
            return;
        }
        if (params.length != 3) {
            HelpInfo.promptHelp("addVisitorAccount");
            return;
        }
        String account = params[1];
        String accountPwd = params[2];
        if (!Common.checkUserName(account)) {
            System.out.println("Invalid account. " + Common.ACCOUNT_NAME_FORMAT);
            return;
        }
        if (!Common.checkPassword(accountPwd)) {
            System.out.println("Invalid password. " + Common.PASSWORD_FORMAT);
            return;
        }

        String url = urlPrefix + "account/addAccount";
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("account", account);
        jsonParam.put("accountPwd", accountPwd);
        jsonParam.put("roleId", Common.KMS_ROLE_VISITOR_ID);
        HttpHeaders headers = new HttpHeaders();
        headers.add("AuthorizationToken", "Token " + token);
        headers.add("Content-Type", "application/json");
        HttpEntity<String> entity = new HttpEntity<String>(jsonParam.toJSONString(), headers);
        try {
            ResponseEntity<String> response =
                    kmsService
                            .getRestTemplate()
                            .exchange(url, HttpMethod.POST, entity, String.class);
            String strBody = response.getBody();
            logger.info(strBody);
            JSONObject jsonBody = JSONObject.parseObject(strBody);
            JSONObject data = jsonBody.getJSONObject("data");
            String newAccount = data.getString("account");
            System.out.println("Add a visitor account \"" + newAccount + "\" successfully.");
        } catch (HttpClientErrorException e) {
            System.out.println("Fail, " + e.getResponseBodyAsString());
        }
    }

    @Override
    public void deleteAccount(String[] params) throws Exception {
        if (!roleName.equals(Common.KMS_ROLE_ADMIN)) {
            System.out.println("This command can only be called by admin role.");
            return;
        }

        if (params.length < 2) {
            HelpInfo.promptHelp("deleteAccount");
            return;
        }
        String param = params[1];
        if ("-h".equals(param) || "--help".equals(param)) {
            HelpInfo.deleteAccountHelp();
            return;
        }
        if (params.length != 2) {
            HelpInfo.promptHelp("deleteAccount");
            return;
        }

        String accountName = params[1];
        if (consoleAccountName.equals(accountName)) {
            System.out.println("Cannot delete your own account.");
            return;
        }

        String url = urlPrefix + "account/deleteAccount/" + accountName;
        HttpHeaders headers = new HttpHeaders();
        headers.add("AuthorizationToken", "Token " + token);
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);
        try {
            kmsService.getRestTemplate().exchange(url, HttpMethod.DELETE, entity, String.class);
            System.out.println("Delete an account \"" + accountName + "\" successfully.");
        } catch (HttpClientErrorException e) {
            System.out.println("Fail, " + e.getResponseBodyAsString());
        }
    }

    @Override
    public void listAccount(String[] params) throws Exception {
        if (!roleName.equals(Common.KMS_ROLE_ADMIN)) {
            System.out.println("This command can only be called by admin role.");
            return;
        }

        if (params.length > 1 && ("-h".equals(params[1]) || "--help".equals(params[1]))) {
            HelpInfo.listAccountHelp();
            return;
        }

        int pageNumber = 1;
        int pageSize = 10;
        String url = urlPrefix + "account/accountList/" + pageNumber + "/" + pageSize;
        HttpHeaders headers = new HttpHeaders();
        headers.add("AuthorizationToken", "Token " + token);
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);
        ResponseEntity<String> response;
        try {
            response =
                    kmsService
                            .getRestTemplate()
                            .exchange(url, HttpMethod.GET, entity, String.class);
            String strBody = response.getBody();
            logger.info(strBody);
            JSONObject jsonBody = JSONObject.parseObject(strBody);
            String[] tableHeaders = {"name", "role", "createTime"};
            int accountTotalCount = jsonBody.getIntValue("totalCount");
            String[][] tableData = new String[accountTotalCount][3];
            JSONArray data = jsonBody.getJSONArray("data");
            for (int i = 0; i < data.size(); i++) {
                JSONObject account = data.getJSONObject(i);
                tableData[i][0] = account.getString("account");
                tableData[i][1] = account.getString("roleName");
                tableData[i][2] = account.getString("createTime");
            }
            int pageTotalCount = accountTotalCount / pageSize;
            if (accountTotalCount % pageSize != 0) {
                pageTotalCount += 1;
            }
            for (int pageIdx = 2; pageIdx <= pageTotalCount; pageIdx++) {
                url = urlPrefix + "account/accountList/" + pageIdx + "/" + pageSize;
                response =
                        kmsService
                                .getRestTemplate()
                                .exchange(url, HttpMethod.GET, entity, String.class);
                strBody = response.getBody();
                logger.info(strBody);
                jsonBody = JSONObject.parseObject(strBody);
                if (accountTotalCount != jsonBody.getIntValue("totalCount")) {
                    logger.warn(" the total count has changed");
                    throw new ConsoleMessageException(
                            "The count of accounts has changed, please inquire again.");
                }
                data = jsonBody.getJSONArray("data");
                for (int i = 0; i < data.size(); i++) {
                    JSONObject account = data.getJSONObject(i);
                    int index = (pageIdx - 1) * pageSize + i;
                    tableData[index][0] = account.getString("account");
                    tableData[index][1] = account.getString("roleName");
                    tableData[index][2] = account.getString("createTime");
                }
            }
            System.out.println(
                    "The count of account created by \""
                            + consoleAccountName
                            + "\" is "
                            + accountTotalCount
                            + ".");
            if (0 == accountTotalCount) {
                return;
            }
            ConsoleUtils.singleLine();
            ColumnFormatter<String> cf = ColumnFormatter.text(Alignment.CENTER, 30);
            Table table = Table.of(tableHeaders, tableData, cf);
            System.out.println(table);
            ConsoleUtils.singleLine();
        } catch (HttpClientErrorException e) {
            System.out.println("Fail, " + e.getResponseBodyAsString());
        }
    }

    @Override
    public void updatePwd(String[] params) throws Exception {
        if (params.length < 2) {
            HelpInfo.promptHelp("updatePassword");
            return;
        }
        String param = params[1];
        if ("-h".equals(param) || "--help".equals(param)) {
            HelpInfo.updatePasswordHelp();
            return;
        }
        if (params.length != 3) {
            HelpInfo.promptHelp("updatePassword");
            return;
        }
        String oldPassword = params[1];
        String newPassword = params[2];
        if (oldPassword.equals(newPassword)) {
            System.out.println("Old and new password cannot be repeated.");
            return;
        }
        if (!Common.checkPassword(newPassword)) {
            System.out.println("Invalid new password. " + Common.PASSWORD_FORMAT);
            return;
        }

        String url = urlPrefix + "account/updatePassword";
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("oldAccountPwd", oldPassword);
        jsonParam.put("newAccountPwd", newPassword);
        HttpHeaders headers = new HttpHeaders();
        headers.add("AuthorizationToken", "Token " + token);
        headers.add("Content-Type", "application/json");
        HttpEntity<String> entity = new HttpEntity<String>(jsonParam.toJSONString(), headers);
        try {
            kmsService.getRestTemplate().exchange(url, HttpMethod.PUT, entity, String.class);
            System.out.println("Update password successfully.");
        } catch (HttpClientErrorException e) {
            System.out.println("Fail, " + e.getResponseBodyAsString());
        }
    }

    @Override
    public void uploadPrivateKey(String[] params) throws Exception {
        if (!roleName.equals(Common.KMS_ROLE_VISITOR)) {
            System.out.println("This command can only be called by visitor role.");
            return;
        }

        if (params.length < 2) {
            HelpInfo.promptHelp("uploadPrivateKey");
            return;
        }
        String param = params[1];
        if ("-h".equals(param) || "--help".equals(param)) {
            HelpInfo.uploadPrivateKeyHelp();
            return;
        }
        if (params.length < 3 || params.length > 4) {
            HelpInfo.promptHelp("uploadPrivateKey");
            return;
        }

        ECKeyPair keyPair = getECKeyPair(params);
        if (keyPair == null) {
            System.out.println("Cannot get the private key to be uploaded.");
            return;
        }

        String creatorPublicKey;
        try {
            creatorPublicKey = getCreatorPublicKey();
        } catch (ConsoleMessageException e) {
            System.out.println("Fail, " + e.getMessage());
            return;
        }

        String password = params[2];
        String privateKeyHex = keyPair.getPrivateKey().toString(16);
        String alias;
        if (params.length == 4) {
            alias = params[3];
        } else if (params.length == 3) {
            Credentials credentials = Credentials.create(keyPair);
            alias = credentials.getAddress();
        } else {
            HelpInfo.promptHelp("uploadPrivateKey");
            return;
        }

        String cipherECC = ECC.encrypt(privateKeyHex, creatorPublicKey);
        if (cipherECC == null) {
            System.out.println("encrypt the private key by public key of creator fail.");
            return;
        }
        String cipherAES = AES.encrypt(privateKeyHex, password);
        if (cipherAES == null) {
            System.out.println("encrypt the private key by password of account fail.");
            return;
        }

        String url = urlPrefix + "escrow/addKey";
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("keyAlias", alias);
        jsonParam.put("cipherText", cipherECC);
        jsonParam.put("privateKey", cipherAES);
        HttpHeaders headers = new HttpHeaders();
        headers.add("AuthorizationToken", "Token " + token);
        headers.add("Content-Type", "application/json");
        HttpEntity<String> entity = new HttpEntity<String>(jsonParam.toJSONString(), headers);
        try {
            ResponseEntity<String> response =
                    kmsService
                            .getRestTemplate()
                            .exchange(url, HttpMethod.POST, entity, String.class);
            String strBody = response.getBody();
            logger.info(strBody);
            JSONObject jsonBody = JSONObject.parseObject(strBody);
            JSONObject data = jsonBody.getJSONObject("data");
            alias = data.getString("keyAlias");
            System.out.println("Upload a private key \"" + alias + "\" successfully.");
        } catch (HttpClientErrorException e) {
            System.out.println("Fail, " + e.getResponseBodyAsString());
        }

        return;
    }

    private String getCreatorPublicKey() throws ConsoleMessageException {
        String publicKey;

        String url = urlPrefix + "account/getPublicKey";
        HttpHeaders headers = new HttpHeaders();
        headers.add("AuthorizationToken", "Token " + token);
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);
        try {
            ResponseEntity<String> response =
                    kmsService
                            .getRestTemplate()
                            .exchange(url, HttpMethod.GET, entity, String.class);
            String strBody = response.getBody();
            logger.info(strBody);
            JSONObject jsonBody = JSONObject.parseObject(strBody);
            JSONObject data = jsonBody.getJSONObject("data");
            publicKey = data.getString("publicKey");
        } catch (HttpClientErrorException e) {
            throw new ConsoleMessageException(
                    "getCreatorPublicKey fail, " + e.getResponseBodyAsString());
        }

        return publicKey;
    }

    private ECKeyPair getECKeyPair(String[] params) {
        ECKeyPair keyPair = null;

        String keyFile = Common.FILE_PATH + params[1];
        String password = params[2];
        InputStream in = readAccountFile(keyFile);
        if (null == in) {
            return null;
        }

        try {
            if (keyFile.endsWith("p12")) {
                P12Manager p12Manager = new P12Manager();
                p12Manager.setPassword(password);
                p12Manager.load(in, password);
                keyPair = p12Manager.getECKeyPair();
            } else if (keyFile.endsWith("pem")) {
                PEMManager pem = new PEMManager();
                pem.load(in);
                keyPair = pem.getECKeyPair();
            } else {
                System.out.println(" invalid file format, file name: " + keyFile);
                logger.error(" invalid file format, file name: {}", keyFile);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            logger.error(" message: {}, e: {}", e.getMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                    logger.error(" message: {}, e: {}", e.getMessage(), e);
                }
            }
        }

        return keyPair;
    }

    private InputStream readAccountFile(String fileName) {
        try {
            return Files.newInputStream(Paths.get(fileName));
        } catch (IOException e) {
            System.out.println(
                    "["
                            + Paths.get(fileName).toAbsolutePath()
                            + "]"
                            + " cannot be opened because it does not exist.");
        }
        return null;
    }

    @Override
    public void listPrivateKey(String[] params) throws Exception {
        if (!roleName.equals(Common.KMS_ROLE_VISITOR)) {
            System.out.println("This command can only be called by visitor role.");
            return;
        }

        if (params.length > 1 && ("-h".equals(params[1]) || "--help".equals(params[1]))) {
            HelpInfo.listPrivateKeyHelp();
            return;
        }

        int pageNumber = 1;
        int pageSize = 10;
        String url = urlPrefix + "escrow/keyList/" + pageNumber + "/" + pageSize;
        HttpHeaders headers = new HttpHeaders();
        headers.add("AuthorizationToken", "Token " + token);
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);
        ResponseEntity<String> response;
        try {
            response =
                    kmsService
                            .getRestTemplate()
                            .exchange(url, HttpMethod.GET, entity, String.class);
            String strBody = response.getBody();
            logger.info(strBody);
            JSONObject jsonBody = JSONObject.parseObject(strBody);
            String[] tableHeaders = {"alias", "createTime"};
            int keyTotalCount = jsonBody.getIntValue("totalCount");
            String[][] tableData = new String[keyTotalCount][2];
            JSONArray data = jsonBody.getJSONArray("data");
            for (int i = 0; i < data.size(); i++) {
                JSONObject key = data.getJSONObject(i);
                tableData[i][0] = key.getString("keyAlias");
                tableData[i][1] = key.getString("createTime");
            }
            int pageTotalCount = keyTotalCount / pageSize;
            if (keyTotalCount % pageSize != 0) {
                pageTotalCount += 1;
            }
            for (int pageIdx = 2; pageIdx <= pageTotalCount; pageIdx++) {
                url = urlPrefix + "escrow/keyList/" + pageIdx + "/" + pageSize;
                response =
                        kmsService
                                .getRestTemplate()
                                .exchange(url, HttpMethod.GET, entity, String.class);
                strBody = response.getBody();
                logger.info(strBody);
                jsonBody = JSONObject.parseObject(strBody);
                if (keyTotalCount != jsonBody.getIntValue("totalCount")) {
                    logger.warn(" the total count has changed");
                    throw new ConsoleMessageException(
                            "The count of uploaded keys has changed, please inquire again.");
                }
                data = jsonBody.getJSONArray("data");
                for (int i = 0; i < data.size(); i++) {
                    JSONObject key = data.getJSONObject(i);
                    int index = (pageIdx - 1) * pageSize + i;
                    tableData[index][0] = key.getString("keyAlias");
                    tableData[index][1] = key.getString("createTime");
                }
            }

            System.out.println(
                    "The count of keys uploaded by \""
                            + consoleAccountName
                            + "\" is "
                            + keyTotalCount
                            + ".");
            if (0 == keyTotalCount) {
                return;
            }
            ConsoleUtils.singleLine();
            ColumnFormatter<String> cf = ColumnFormatter.text(Alignment.CENTER, 45);
            Table table = Table.of(tableHeaders, tableData, cf);
            System.out.println(table);
            ConsoleUtils.singleLine();
        } catch (HttpClientErrorException e) {
            System.out.println("Fail, " + e.getResponseBodyAsString());
        }
    }

    @Override
    public void exportPrivateKey(String[] params) throws Exception {
        if (!roleName.equals(Common.KMS_ROLE_VISITOR)) {
            System.out.println("This command can only be called by visitor role.");
            return;
        }

        if (params.length < 2) {
            HelpInfo.promptHelp("exportPrivateKey");
            return;
        }
        String param = params[1];
        if ("-h".equals(param) || "--help".equals(param)) {
            HelpInfo.exportPrivateKeyHelp();
            return;
        }
        if (params.length != 3) {
            HelpInfo.promptHelp("exportPrivateKey");
            return;
        }

        String url = urlPrefix + "escrow/queryKey";
        String keyAlias = params[1];
        url += "/" + consoleAccountName + "/" + keyAlias;
        HttpHeaders headers = new HttpHeaders();
        headers.add("AuthorizationToken", "Token " + token);
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);
        try {
            ResponseEntity<String> response =
                    kmsService
                            .getRestTemplate()
                            .exchange(url, HttpMethod.GET, entity, String.class);
            String strBody = response.getBody();
            logger.info(strBody);
            JSONObject jsonBody = JSONObject.parseObject(strBody);
            JSONObject data = jsonBody.getJSONObject("data");
            if (data == null) {
                System.out.println("The private key not exists.");
                return;
            }
            String cipherText = data.getString("privateKey");
            if (cipherText == null) {
                System.out.println("Get cipher text fail.");
                return;
            }
            String plainText = AES.decrypt(cipherText, params[2]);
            if (plainText == null) {
                System.out.println("Password error.");
                return;
            }
            System.out.println("The private key \"" + keyAlias + "\" is " + plainText + ".");
        } catch (HttpClientErrorException e) {
            System.out.println("queryKey fail, " + e.getResponseBodyAsString());
        }
    }

    @Override
    public void deletePrivateKey(String[] params) throws Exception {
        if (!roleName.equals(Common.KMS_ROLE_VISITOR)) {
            System.out.println("This command can only be called by visitor role.");
            return;
        }

        if (params.length < 2) {
            HelpInfo.promptHelp("deletePrivateKey");
            return;
        }
        String param = params[1];
        if ("-h".equals(param) || "--help".equals(param)) {
            HelpInfo.deletePrivateKeyHelp();
            return;
        }
        if (params.length != 2) {
            HelpInfo.promptHelp("deletePrivateKey");
            return;
        }

        String url = urlPrefix + "escrow/deleteKey";
        String keyAlias = params[1];
        url += "/" + consoleAccountName + "/" + keyAlias;
        HttpHeaders headers = new HttpHeaders();
        headers.add("AuthorizationToken", "Token " + token);
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);
        try {
            kmsService.getRestTemplate().exchange(url, HttpMethod.DELETE, entity, String.class);
            System.out.println("Delete the private key \"" + keyAlias + "\" successfully.");
        } catch (HttpClientErrorException e) {
            System.out.println("Fail, " + e.getResponseBodyAsString());
        }
    }

    @Override
    public void restorePrivateKey(String[] params) throws Exception {
        if (!roleName.equals(Common.KMS_ROLE_ADMIN)) {
            System.out.println("This command can only be called by admin role.");
            return;
        }

        if (params.length < 2) {
            HelpInfo.promptHelp("restorePrivateKey");
            return;
        }
        String param = params[1];
        if ("-h".equals(param) || "--help".equals(param)) {
            HelpInfo.restorePrivateKeyHelp();
            return;
        }
        if (params.length != 4) {
            HelpInfo.promptHelp("restorePrivateKey");
            return;
        }

        String accountName = params[1];
        String keyAlias = params[2];
        String privateKey = params[3];
        if (privateKey.length() != 64) {
            System.out.println("Invalid private key length, need 64. ");
            return;
        }

        String url = urlPrefix + "escrow/queryKey";
        url += "/" + accountName + "/" + keyAlias;
        HttpHeaders headers = new HttpHeaders();
        headers.add("AuthorizationToken", "Token " + token);
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);
        try {
            ResponseEntity<String> response =
                    kmsService
                            .getRestTemplate()
                            .exchange(url, HttpMethod.GET, entity, String.class);
            String strBody = response.getBody();
            logger.info(strBody);
            JSONObject jsonBody = JSONObject.parseObject(strBody);
            JSONObject data = jsonBody.getJSONObject("data");
            if (data == null) {
                System.out.println("The private key not exists.");
                return;
            }
            String cipherText = data.getString("cipherText");
            if (cipherText == null) {
                System.out.println("Get cipher text fail.");
                return;
            }
            String plainText = ECC.decrypt(cipherText, privateKey);
            if (plainText == null) {
                System.out.println("Decrypt fail.");
                return;
            }
            System.out.println(
                    "The private key \""
                            + keyAlias
                            + "\" of account \""
                            + accountName
                            + "\" is "
                            + plainText
                            + ".");
        } catch (HttpClientErrorException e) {
            System.out.println("queryKey fail, " + e.getResponseBodyAsString());
        }
    }
}
