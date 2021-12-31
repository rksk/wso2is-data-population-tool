package org.wso2.carbon.custom.user.administrator;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.custom.user.administrator.internal.ServiceComponent;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.jdbc.UniqueIDJDBCUserStoreManager;
import org.wso2.carbon.idp.mgt.util.IdPManagementUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;

import static org.wso2.carbon.custom.user.administrator.Constants.*;

public class BulkUserUploadThread {
    private static final Log log = LogFactory.getLog(BulkUserUploadThread.class);
    private Properties properties = null;

    private File[] files = null;
    private ArrayList<String[]> firstLine = new ArrayList<String[]>();
    private Map<Integer, Map<String, Integer>> fileIndexToSpecialColumns = new LinkedHashMap<Integer, Map<String, Integer>>();
    private LinkedHashMap<Integer, LinkedHashSet<String[]>> fileIndexToUserHashMap = new LinkedHashMap<Integer, LinkedHashSet<String[]>>();
    private LinkedHashSet<String[]> userSet = null;

    private String tenantDomain = "carbon.super";
    private int tenantId = -1234;

    private UniqueIDJDBCUserStoreManager store;

    private boolean addRoles;
    private int roleCount = 100;
    private int userCount = 100;
    private int userOffset = 1;

    public void call() throws UserStoreException {

        addRoles = Boolean.parseBoolean(System.getProperty(ADD_ROLES));
        if (System.getProperty(ROLE_COUNT) != null) {
            roleCount = Integer.parseInt(System.getProperty(ROLE_COUNT));;
        }
        if (System.getProperty(USER_COUNT) != null) {
            userCount = Integer.parseInt(System.getProperty(USER_COUNT));;
        }
        if (System.getProperty(USER_OFFSET) != null) {
            userOffset = Integer.parseInt(System.getProperty(USER_OFFSET));;
        }

        long t_init = System.currentTimeMillis();

        store = (UniqueIDJDBCUserStoreManager) ServiceComponent.getRealmService().getBootstrapRealm()
                .getUserStoreManager();
        if (store == null) {
            log.error(BULK_UPLOAD_LOG_PREFIX + "Primary user store was not found. Task aborted.");
            return;
        }

        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(this.tenantDomain);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(this.tenantId);

        log.info(BULK_UPLOAD_LOG_PREFIX + "Starting user provisioning to the user store...");

        if (addRoles) {
            log.info(BULK_UPLOAD_LOG_PREFIX + "Creating " + roleCount + " roles.");
            for (int i=1; i<=roleCount; i++) {
                String role = "group_" + String.format("%03d", i);
                store.doAddRoleWithID(role, null, false);
            }
            log.info(BULK_UPLOAD_LOG_PREFIX + "Role creation is finished.");
        }

        long u_init = System.currentTimeMillis();
        for (int i=userOffset; i<=userCount; i++) {
            Map<String, String> claims = new HashMap();
            String username = "user_" + String.format("%09d", i);
            String password = "password";
            claims.put("http://wso2.org/claims/givenname", "FN" + username);
            claims.put("http://wso2.org/claims/lastname", "LN" + username);
            claims.put("http://wso2.org/claims/emailaddress", username + "@email.com");
            claims.put("http://wso2.org/claims/mobile", "");

            int roleId = ((i-1) % roleCount) + 1;
            String[] roles = {"group_" + String.format("%03d", roleId)};
            try {
                store.doAddUserWithID(username, password, roles, claims, null, false);
            } catch (UserStoreException e) {
                log.error(BULK_UPLOAD_LOG_PREFIX + "Error occurred while adding user with the username : "
                        + username + " | claims : " + claims, e);
            }

            if (i>1&& i%1000 == 0) {
                long u_end = System.currentTimeMillis();
                log.info(BULK_UPLOAD_LOG_PREFIX + "[TIME INDICATOR] Time taken to add users ("+(i-1000)+"-"+i+"): " +
                        + (u_end - u_init) + "ms");
                u_init = System.currentTimeMillis();
            }
        }

        PrivilegedCarbonContext.endTenantFlow();

        long t_final = System.currentTimeMillis();
        log.info(BULK_UPLOAD_LOG_PREFIX + "[TIME INDICATOR] Total time taken to add users to the user store: " +
                + (t_final - t_init)/1000);
    }

    private int getTenantIdFromDomain(String tenantDomain) {
        try {
//            int id = IdPManagementUtil.getTenantIdOfDomain(tenantDomain);
//            int id = IdentityTenantUtil.getTenantId(tenantDomain);
            return -1234;
        } catch (Throwable e) {
            log.error(BULK_UPLOAD_LOG_PREFIX + "Prerequisites were not satisfied. Error occurred while " +
                    "resolving tenant Id from tenant domain :" + tenantDomain, e);
            return -2;
        }
    }
}
